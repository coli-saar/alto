
package de.up.ling.irtg.script;

import com.google.common.collect.Iterables;
import de.saar.basic.StringTools;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.IrtgParser;
import de.up.ling.irtg.ParseException;
import de.up.ling.irtg.algebra.PtbTreeAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.maxent.ChildOfFeature;
import de.up.ling.irtg.maxent.FeatureFunction;
import de.up.ling.irtg.maxent.MaximumEntropyIrtg;
import de.up.ling.irtg.maxent.RuleNameFeature;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.io.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author Danilo Baumgarten
 */
public class PTBConverter {

    private static final Logger log = Logger.getLogger(PTBConverter.class.getName());
    private static final int TOKEN_SIZE = 15;
    private static final boolean CONVERT = false;
    public static final boolean PARENT_ANNOTATION = true;
    private static final boolean EXTRACT_LEX = true;

    private Map<String, Integer> ruleMap;             // mapping of string representation and name of a rule, i.e. "S/NP/VP" -> "r1"; rule name is encoded according to RTG signature
    private Map<String, FeatureFunction> featureMap; // mapping of names to feature functions
    private List<Tree<String>> ptbTrees;             // list of PTB-trees
//    private List<Tree<String>> irtgTrees;            // list of IRTG-trees
    private Corpus corpus;                           // annotated corpus
    private MaximumEntropyIrtg maxEntIrtg;           // the automaton storing the grammar
    private Homomorphism hStr;                       // homomorphism for StringAlgebra
    private Homomorphism hPtb;                       // homomorphism for PtbTreeAlgebra
    private int maxTerminalsPerSentence;             // max. sentence length (in words)
    
    /**
     * Reads a PTB-file (preset filename or via argument) and depending on settings
     * converts PTB- into Irtg-trees and serialize them as grammar and corpus
     * or serialize them as corpus only
     * 
     * @param args an array of string containing the optional arguments from the call
     * @throws IOException if an error on accessing the files occurs
     */
    public static void main(final String[] args) throws IOException {
        String filename = (args.length > 0) ? args[0] : "examples/ptb-test.mrg";
        String sortByInterpretation = (args.length > 1) ? args[1] : null;
        int tokenSize = (args.length > 2) ? Integer.valueOf(args[2]) : TOKEN_SIZE;
        boolean convert = (args.length > 3) ? (!args[3].equals("noconversion")) : CONVERT;
        
        String prefix = getFilenamePrefix(filename);
        String corpusFilename = prefix + ((convert) ? "-corpus-training.txt" : "-corpus-testing.txt");

        PTBConverter lc = new PTBConverter(tokenSize);

        log.info("Reading PTB data...");
        lc.read(getReaderForFilename(filename));

        if (convert) {
            lc.initGrammar();

            log.info("Converting PTB trees...");
            lc.convert();
            log.log(Level.INFO, "Converted rules: {0}", String.valueOf(lc.ruleMap.size()));

            log.info("Adding features...");
            int numFeatures = lc.addFeatures();
            log.log(Level.INFO, "Features: {0}", String.valueOf(numFeatures));

            log.info("Writing grammar...");
            lc.writeGrammar(new FileWriter(prefix + "-grammar.irtg"));

            log.info("Writing corpus...");
            lc.writeCorpus(new FileWriter(corpusFilename), sortByInterpretation);
        } else {
            log.info("Processing PTB trees...");
            lc.process();
            log.log(Level.INFO, "Processed sentences: {0}", String.valueOf(lc.corpus.getNumberOfInstances()));

            log.info("Writing corpus...");
            lc.writeCorpus(new FileWriter(corpusFilename), sortByInterpretation);
            
            if (EXTRACT_LEX) {
                log.info("Reading grammar...");
                try {
                    lc.readGrammar(new FileReader(prefix + "-grammar.irtg"));
                } catch (FileNotFoundException ex) {
                    log.log(Level.SEVERE, "Reading the grammar was unsuccessful. Create a new one.\n{0}", ex);
                    lc.initGrammar();
                } catch (ParseException ex) {
                    log.log(Level.SEVERE, "Parsing the grammar was unsuccessful. Create a new one.\n{0}", ex);
                    lc.initGrammar();
                }
                log.log(Level.INFO, "Grammar rules at reading: {0}", String.valueOf(lc.ruleMap.size()));

                log.info("Extract lexical rules...");
                lc.extractLexicalRules();
                log.log(Level.INFO, "Grammar rules after extraction: {0}", String.valueOf(lc.ruleMap.size()));

                log.info("Writing grammar...");
                lc.writeGrammar(new FileWriter(prefix + "-grammar-new.irtg"));
            }
        }

        log.info("Done.");
    }

    /*
     * Returns the reader appropriate for the file given by <tt>filename</tt>
     * if the file is an archive all the zipped data will be streamed
     * an ordinary file reader is used else
     * 
     * @param filename the name of the file
     * @return Reader the reader to handle the file content
     * @throws FileNotFoundException if the file could not be found
     * @throws IOException in case of an error on reading the zipped stream
     */
    private static Reader getReaderForFilename(final String filename) throws FileNotFoundException, IOException {
        if (filename.endsWith(".gz")) {
            return new InputStreamReader(new GZIPInputStream(new FileInputStream(filename)));
        } else {
            return new FileReader(filename);
        }
    }

    /*
     * Gets the filename without ending to use it as prefix for various output files
     * 
     * @param filenameWithPath the whole filename
     * @return String the filename without ending
     */
    private static String getFilenamePrefix(final String filenameWithPath) {
        String filename = new File(filenameWithPath).getName();
        
        if (filename.endsWith(".mrg")) {
            return filename.substring(0, filename.length() - 4);
        } else if (filename.endsWith(".mrg.gz")) {
            return filename.substring(0, filename.length() - 7);
        } else {
            return filename;
        }
    }

    /**
     * Constructor
     * 
     * @param maxTerminals the max. number of token/words in an instance to use for conversion
     */
    public PTBConverter(final int maxTerminals) {
        maxTerminalsPerSentence = maxTerminals;
        
        // init members
        corpus = new Corpus();
        ptbTrees = new ArrayList<Tree<String>>();
//        irtgTrees = new ArrayList<Tree<String>>();
        ruleMap = new HashMap<String, Integer>();
        featureMap = new HashMap<String, FeatureFunction>();

        log.setLevel(Level.ALL);
    }

    /**
     * Creates a new MaximumEntropyIrtg with interpretations
     * for strings and PTB-trees
     */
    public void initGrammar() {

        // create MaximumEntropyIrtg without features (we have none yet)
        maxEntIrtg = new MaximumEntropyIrtg(new ConcreteTreeAutomaton(), null);

        // setup everthing for interpretation of strings
        StringAlgebra stringAlgebra = new StringAlgebra();
        hStr = new Homomorphism(maxEntIrtg.getAutomaton().getSignature(), stringAlgebra.getSignature());
        maxEntIrtg.addInterpretation("i", new Interpretation(stringAlgebra, hStr));

        // setup everything for interpretation of PTB-trees
        PtbTreeAlgebra ptbAlgebra = new PtbTreeAlgebra(PARENT_ANNOTATION);
        hPtb = new Homomorphism(maxEntIrtg.getAutomaton().getSignature(), ptbAlgebra.getSignature());
        maxEntIrtg.addInterpretation("ptb", new Interpretation(ptbAlgebra, hPtb));
    }

    /**
     * Returns the list of PTB-trees
     *
     * @return List<Tree<String>> the list of PTB-trees
     */
    public List<Tree<String>> getPtbTrees() {
        return ptbTrees;
    }

    /**
     * Reads the list of PTB-trees given by <tt>reader</tt>
     *
     * @param reader the reader containing the data
     * @throws IOException if an error occurs on reading the data
     */
    public void read(final Reader reader) throws IOException {
        PtbTreeAlgebra pta = new PtbTreeAlgebra(PARENT_ANNOTATION);
        Tree<String> ptbTree = null;

        do {
            ptbTree = pta.parseFromReader(reader);
            
            // check if there's a resulting tree and the number of token/words
            if ((ptbTree != null) && (pta.getNumWords() <= maxTerminalsPerSentence)) {
                // store the parsed tree
                ptbTrees.add(ptbTree);
            }
            // break if the parsed tree is null
            // that means whether we read everything or something went wrong
            // anyway there's no use in continuing
        } while (ptbTree != null);
    }

    /**
     * Creates a corpus for all the read PTB-trees
     */
    public void process() {
        for (Tree<String> ptbTree : ptbTrees) {
            // create the representation of the PTB-tree
            Map<String, Object> inputObjectsMap = new HashMap<String, Object>();
            // in form of the actual sentence
            List<String> sentence = ptbTree.getLeafLabels();
            inputObjectsMap.put("i", sentence);

            // add the instance to the corpus
//            corpus.getInstances().add(new AnnotatedCorpus.Instance(ptbTree, inputObjectsMap)); //// XXXXX TODO XXXXX
        }
    }

    /**
     * Converts all PTB-trees to IRTG-trees and collects the rules
     * Also binarizes them, extract the rules to create a grammar
     * and setup the interpretations
     */
    public void convert() {
        ConcreteTreeAutomaton c = (ConcreteTreeAutomaton) maxEntIrtg.getAutomaton();
        PtbTreeAlgebra pta = new PtbTreeAlgebra(PARENT_ANNOTATION);

        for (Tree<String> ptbTree : ptbTrees) {
            // store representation of PTB tree
            List<String> ptbObjects = new ArrayList<String>();
            ptbObjects.add(ptbTree.toString());

            // binarize the PTB-tree
            ptbTree = pta.binarizeAndRelabel(ptbTree);

            // extract and store the rules used in the PTB-tree
            extractRules(ptbTree);

            // add the root label to the final states
            c.addFinalState(c.addState(ptbTree.getLabel()));

            // convert the PTB-Tree to an IRTG-tree
//            Tree<String> irtgTree = ptb2Irtg(ptbTree);
//            irtgTrees.add(irtgTree);

            //create a corpus instance
            List<String> sentence = ptbTree.getLeafLabels();
            Map<String, Object> inputObjectsMap = new HashMap<String, Object>();
            // with the actual sentence
            inputObjectsMap.put("i", sentence);
            // and the PTB-tree as interpretations
            inputObjectsMap.put("ptb", ptbObjects);

            // add the instance to the corpus
//            corpus.getInstances().add(new AnnotatedCorpus.Instance(irtgTree, inputObjectsMap));  /// XXXXXX TODO XXXXXXX
        }

    }

    /**
     * Extracts all rules from the given PTB-tree
     *
     * @param tree the PTB-tree
     */
    public void extractRules(final Tree<String> tree) {
        final ConcreteTreeAutomaton c = (ConcreteTreeAutomaton) maxEntIrtg.getAutomaton();
        tree.dfs(new TreeVisitor<String, Void, Boolean>() {
            /**
             * Extracts and stores the rule in <tt>node</tt>
             *
             * @param node the PTB-(sub)tree
             * @param childrenValues a list of flags whether the child node is a
             * leaf node or not
             * @return Boolean whether <tt>node</tt> is a leaf node or not
             */
            @Override
            public Boolean combine(Tree<String> node, List<Boolean> childrenValues) {
                if (childrenValues.isEmpty()) {
                    return true; // we have a leaf node
                }
                // create a string representation for the rule, i.e. 'S/NP/VP'
                String ruleString = nodeToRuleString(node);
                // create and store the rule if it not exists
                if (!ruleMap.containsKey(ruleString)) {
                    List<Tree<String>> nodeChildren = node.getChildren();
                    List<String> childStates = new ArrayList<String>(); // list of states on the right side
                    List<Tree<HomomorphismSymbol>> ptbChildren = new ArrayList<Tree<HomomorphismSymbol>>();
                    String label;

                    // create rule name (rXXX)
                    int ruleName = hStr.getSourceSignature().addSymbol("r" + String.valueOf(ruleMap.size() + 1), childrenValues.size());
                    // remember that we have the rule already
                    ruleMap.put(ruleString, ruleName);

                    if (childrenValues.get(0)) { // the node's child is a leaf node --> terminal symbol
                        label = nodeChildren.get(0).getLabel(); // terminal symbol
                        Tree<HomomorphismSymbol> th = Tree.create(HomomorphismSymbol.createConstant(label, hStr.getTargetSignature(), 0));
                        hStr.add(ruleName, th);
                        ptbChildren.add(th);
                    } else {
                        // collect the child states and create the PTB interpretation
                        // the PTB interpretation is not nested but straight forward
                        for (int i = 0; i < childrenValues.size(); i++) {
                            label = nodeChildren.get(i).getLabel();
                            childStates.add(label);
                            String ptbLabel = "?" + String.valueOf(i + 1);
                            ptbChildren.add(Tree.create(HomomorphismSymbol.createVariable(ptbLabel))); // XXX this was createConstant -- but shouldn't it be createVariable? AK 23.7.13
                        }

                        // create the nested intepretation for the StringAlgebra
                        Tree<HomomorphismSymbol> strInterp = computeInterpretation(1, childrenValues.size(), hStr.getTargetSignature());
                        hStr.add(ruleName, strInterp);
                    }

                    // add interpretations the the homomorphisms
                    hPtb.add(ruleName, Tree.create(HomomorphismSymbol.createConstant(node.getLabel(), hPtb.getTargetSignature(), 0), ptbChildren));

                    // add the rule to the automaton
                    c.addRule(c.createRule(node.getLabel(), hStr.getSourceSignature().resolveSymbolId(ruleName), childStates));
                }

                return false;
            }
        });
    }

    public void extractLexicalRules() {
        for (Tree<String> ptbTree : ptbTrees) {

            List<Tree<String>> ptbTerminals = getTerminalSubTrees(ptbTree);

            for (Tree<String> ptbTerminal : ptbTerminals) {
                // extract and store the rules used in the PTB-tree
                extractRules(ptbTerminal);
            }
        }
    }

    public List<Tree<String>> getTerminalSubTrees(final Tree<String> tree) {
//        final ConcreteTreeAutomaton c = (ConcreteTreeAutomaton) maxEntIrtg.getAutomaton();
        final List<Tree<String>> ret = new ArrayList<Tree<String>>();
        tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                if (childrenValues.isEmpty()) {
                    return node; // we have a leaf node
                }
                for (Tree<String> child : childrenValues) {
                    if (child != null) {
                        if (child.getChildren().isEmpty()) {
                            node.setLabel(node.getLabel() + "1");
                            if (PARENT_ANNOTATION) {
                                return node;
                            } else {
                                ret.add(node);
                            }
                        } else {
                            Tree<String> firstGrandChild = child.getChildren().get(0);
                            if (firstGrandChild.getChildren().isEmpty()) {
                                String label = child.getLabel() + "^" + node.getLabel() + String.valueOf(childrenValues.size());
                                child.setLabel(label);
                                ret.add(child);
                            }
                        }
                    }
                }
                return null;
            }
        });
        return ret;
    }

    /**
     * Creates a string representation for the rule at the PTB-node
     *
     * @param node the PTB-(sub)tree
     * @return String the rule representation
     */
    private String nodeToRuleString(final Tree<String> node) {
        StringBuilder ret = new StringBuilder();

        // the representation starts with the left side of the rule
        ret.append(node.getLabel());

        // and adds all elements of the right side all delimited by '/'
        for (Tree<String> n : node.getChildren()) {
            ret.append("/");
            ret.append(n.getLabel());
        }
        return ret.toString();
    }

    /**
     * Computes the (nested) interpretation for rules containing no terminal
     * symbol
     *
     * @param start the number of the first element of the interpretation, e.g.,
     * 1
     * @param max the number of elements of the interpretation
     * @return String the interpretation
     */
    private Tree<HomomorphismSymbol> computeInterpretation(final int start, final int max, Signature signature) {
        // if there is only one element create the tree and return it
        if (max == 1) {
            return Tree.create(HomomorphismSymbol.createVariable("?1"));
        }

        // if (max < start) becomes true this functions is missused
        if (max < start) {
            return null;
        }
        
        List<Tree<HomomorphismSymbol>> strChildren = new ArrayList<Tree<HomomorphismSymbol>>();
        
        // create a variable name
        String startLabel = "?" + String.valueOf(start);
        // and add the variable to the list
        strChildren.add(Tree.create(HomomorphismSymbol.createVariable(startLabel)));
        
        // proceed with the remaining elements
        if ((start + 1) == max) { // last element to compute
            // create variable name
            String maxLabel = "?" + String.valueOf(max);
            // and add the variable to the list
            strChildren.add(Tree.create(HomomorphismSymbol.createVariable(maxLabel)));
        } else { // more than one remaining element
            // recursive step
            strChildren.add(computeInterpretation(start + 1, max, signature));
        }

        return Tree.create(HomomorphismSymbol.createConstant("*", signature, strChildren.size()), strChildren);
    }

    /**  ** unused **
     * Converts a PTB-tree to an IRTG-tree
     *
     * @param tree the PTB-tree
     * @return Tree<String> the converted IRTG-tree
     */
    /*
    public Tree<String> ptb2Irtg(final Tree<String> tree) {
        if (tree == null) {
            return null;
        }
        return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
          
             * Returns the IRTG-(sub)tree corresponding to <tt>node</tt>
             *
             * @param node the PTB-(sub)tree
             * @param childrenValues the already converted child nodes of
             * <tt>node</tt>
             * @return Tree<String> the IRTG-(sub)tree

            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                if ((node == null) || node.getChildren().isEmpty()) {
                    return null; // we have a leaf node. nothing to do here
                }

                // create a string representation for the rule, i.e. 'S/NP/VP'
                String ruleString = nodeToRuleString(node);
                // and try to find the corresponding rule
                String ruleName = ruleMap.get(ruleString);

                if (ruleName == null) {
                    // the tree contains a rule we have not stored. something went wrong
                    log.log(Level.SEVERE, "Rule not found for: {0}", node.toString());
                    return null;
                }

                if ((childrenValues.size() == 1) && (childrenValues.get(0) == null)) {
                    // the childrenValues indicating this rule leads to a terminal symbol
                    return Tree.create(ruleName);
                }

                // create a IRTG-styled tree with the rule name as label and childrenValues as child nodes
                return Tree.create(ruleName, childrenValues);
            }
        });
    }
    */

    /**
     * Reads the grammar from a reader, e.g., string or file
     *
     * @param reader the reader containing the data
     * @throws IOException if error occurs on reading the data
     * @throws ParseException if error occurs on parsing the data
     */
    public void readGrammar(final Reader reader) throws IOException, ParseException {
        maxEntIrtg = (MaximumEntropyIrtg) IrtgParser.parse(reader);

        Collection<Interpretation> interpretations = maxEntIrtg.getInterpretations().values();

        // check every entry of the set of interpretations
        for (Interpretation i : interpretations) {

            // for now only StringAlgebra is used for input strings to compute charts
            if (i.getAlgebra() instanceof StringAlgebra) {
                hStr = i.getHomomorphism();
            } else if (i.getAlgebra() instanceof PtbTreeAlgebra) {
                hPtb = i.getHomomorphism();
            }
        }
        
        maxEntIrtg.setFeatures(null);

        Set<Rule> ruleSet = maxEntIrtg.getAutomaton().getRuleSet();
        for (Rule rule : ruleSet) {
            StringBuilder ruleStringBuilder = new StringBuilder();

            // the representation starts with the left side of the rule
            ruleStringBuilder.append(rule.getParent());

            // and adds all elements of the right side delimited by '/'
            int[] children = rule.getChildren();
            if (children.length > 0) {
                for (Object child : children) {
                    ruleStringBuilder.append("/");
                    ruleStringBuilder.append((String) child);
                }
            } else {
                Tree<HomomorphismSymbol> t = hStr.get(rule.getLabel());
                ruleStringBuilder.append("/");
                ruleStringBuilder.append(t.getLabel().getValue());
            }

            String ruleString = ruleStringBuilder.toString();

            ruleMap.put(ruleString, rule.getLabel());
        }
    }

    /**
     * Writes the grammar rules to a writer, e.g., string or file
     *
     * @param writer the writer to store the data into
     * @throws IOException if the writer cannot store the data properly
     */
    public void writeGrammar(final Writer writer) throws IOException {
        // if not already done add the features
        if (featureMap.isEmpty()) {
            addFeatures();
        }

        writer.write(maxEntIrtg.toString());
        writer.close();
    }

    /**
     * Writes the annotated corpus to a writer, e.g., string or file
     *
     * @param writer the writer to store the data into
     * @param sortByInterpretation a string referring to an interpretation
     * which shall be used to sort the instances by length
     * @throws IOException if the writer cannot store the data properly
     */
    public void writeCorpus(final Writer writer, final String sortByInterpretation) throws IOException {
        String nl = System.getProperty("line.separator");
        
        // this copying is annoying, but probably unavoidable given the sorting by interpretation.
        // but this code should be moved into AnnotatedCorpus anyway. TODO
        List<Instance> instances = new ArrayList<Instance>();
        Iterables.addAll(instances, corpus);

        if (instances.isEmpty()) {
            // if the corpus is empty we have nothing to do
            writer.close();
            return;
        }

        Set<String> interpretations = instances.get(0).getInputObjects().keySet();

        // write the indices for all interpretations
        for (String interp : interpretations) {
            writer.write(interp + nl);
        }

        // if requested, sort instances by length in interpretation sortByInterpretation
        if (sortByInterpretation != null) {
            Collections.sort(instances, new Comparator<Instance>() {
                public int compare(Instance t, Instance t1) {
                    return t.getInputObjects().get(sortByInterpretation).toString().length() - t1.getInputObjects().get(sortByInterpretation).toString().length();
                }
            });
        }

        // write every instance
        for (Instance instance : instances) {
            // for every instance write their interpretations
            for (String interp : interpretations) {
                String interpretation = StringTools.join((List<String>) instance.getInputObjects().get(interp), " ");
                writer.write(interpretation + nl);
            }

            // and their tree
            writer.write(instance.getDerivationTree().toString() + nl);
        }

        writer.close();
    }

    /**
     * Creates a list of feature functions used with the MaximumEntropyIrtg.
     * There are endless ways to create such a list. This is a wrapper function.
     * Choose a specific function or a combination of them for the actual
     * creation.
     *
     * @return int the number of all added features 
     */
    public int addFeatures() {
        addFeatures4AllRules();
//        addParentRelatedFeatures();
//        addTerminalRelatedFeatures();
        addChildRelatedFeatures();

        maxEntIrtg.setFeatures(featureMap);
        return featureMap.size();
    }

    /**
     * Creates for every rule a feature function featuring exactly that rule
     */
    public void addFeatures4AllRules() {
        int num = featureMap.size();
        String featureName;
        Collection<Integer> rules = ruleMap.values();

        for (Integer r : rules) {
            featureName = "f" + String.valueOf(++num);
            featureMap.put(featureName, new RuleNameFeature(maxEntIrtg.getAutomaton().getSignature().resolveSymbolId(r)));
        }
    }

    /**
     * Creates feature functions for every rule when it has a non-terminal symbol
     * on the right side which appears in more than one rule
     */
    public void addChildRelatedFeatures() {
        Map<String, Set<String>> states = new HashMap<String, Set<String>>();
        TreeAutomaton auto = maxEntIrtg.getAutomaton();
        Set<Rule> ruleSet = auto.getRuleSet();

        for (Rule r : ruleSet) {
            int parentState = r.getParent();
            String parentAsString = auto.getStateForId(parentState).toString();
            int[] children = r.getChildren();

            for (int child : children) {
                String state = auto.getStateForId(child).toString();

                // get already gathered parent states for state
                Set<String> parentStates = states.get(state);

                // create new set if none present
                if (parentStates == null) {
                    parentStates = new HashSet<String>();
                    states.put(state, parentStates);
                }
                
                // add parent state
                parentStates.add(parentAsString);

            }
        }
        
        int num = featureMap.size();
        String featureName;
        Set<String> childStates = states.keySet();

        
        for (String state : childStates) {
            Set<String> parentStates = states.get(state);
            
            // check if there are more than one parent state for state
            if (parentStates.size() > 1) {

                // add a feature function for all pairs of state and parent state
                for (String p : parentStates) {
                    featureName = "f" + String.valueOf(++num);
                    featureMap.put(featureName, new ChildOfFeature(p, state));
                }
            }
        }
    }

    /**
     * For every set in the given collection containing more than one item
     * create a proper feature function for these items
     * 
     * @param stateRules the collection of sets; every set contains
     * the rules corresponding to one state
     */
    private void addFeaturesToMap(final Collection<Set<Integer>> stateRules) {
        int num = featureMap.size();
        String featureName;

        for (Set<Integer> rulesSet : stateRules) {

            // check if the set contains more than one item
            if (rulesSet.size() > 1) {

                // add a feature function for every rule in the set
                for (Integer r : rulesSet) {
                    featureName = "f" + String.valueOf(++num);
                    featureMap.put(featureName, new RuleNameFeature(maxEntIrtg.getAutomaton().getSignature().resolveSymbolId(r)));
                }
            }
        }
    }

    /**
     * Creates feature functions for every rule when it's left non-terminal
     * symbol appears in more than one rule
     */
    public void addParentRelatedFeatures() {
        Map<String, Set<Integer>> stateRules = new HashMap<String, Set<Integer>>();
        TreeAutomaton auto = maxEntIrtg.getAutomaton();
        Set<Rule> ruleSet = auto.getRuleSet();
        

        for (Rule r : ruleSet) {
            int ruleName = r.getLabel();
            String state = auto.getStateForId(r.getParent()).toString();


            // get already gathered rules for state
            Set<Integer> rules = stateRules.get(state);

            // create new set if none present
            if (rules == null) {
                rules = new HashSet<Integer>();
                stateRules.put(state, rules);
            }

            // add rule
            rules.add(ruleName);
        }

        addFeaturesToMap(stateRules.values());
    }

    /**
     * Creates feature functions for every rule when it has a terminal symbol
     * which appears in more than one rule
     */
    public void addTerminalRelatedFeatures() {
        Map<String, Set<Integer>> interpRules = new HashMap<String, Set<Integer>>();
        Set<Rule> ruleSet = maxEntIrtg.getAutomaton().getRuleSet();

        for (Rule r : ruleSet) {
            int ruleName = r.getLabel();
            int[] children = r.getChildren();

            if (children.length == 0) {
                String interp = hStr.get(ruleName).toString();

                // get already gathered rules for terminal symbol
                Set<Integer> rules = interpRules.get(interp);

                // create new set if none present
                if (rules == null) {
                    rules = new HashSet<Integer>();
                    interpRules.put(interp, rules);
                }

                // add rule
                rules.add(ruleName);
            }
        }

        addFeaturesToMap(interpRules.values());
    }
}