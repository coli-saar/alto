package de.up.ling.irtg.script;

import de.saar.basic.StringOrVariable;
import de.saar.basic.StringTools;
import de.up.ling.irtg.corpus.AnnotatedCorpus;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.ParseException;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.PtbTreeAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.corpus.AnnotatedCorpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.maxent.ChildOfFeature;
import de.up.ling.irtg.maxent.FeatureFunction;
import de.up.ling.irtg.maxent.MaximumEntropyIrtg;
import de.up.ling.irtg.maxent.RuleNameFeature;
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

    private static final Logger log = Logger.getLogger(MaximumEntropyIrtg.class.getName());

    /**
     * 
     */
    public static void main(String[] args) throws IOException, ParseException, ParserException {
        String filename = (args.length > 0) ? args[0] : "examples/ptb-test.mrg";
        String sortByInterpretation = (args.length > 1) ? args[1] : null;
        int tokenSize = (args.length > 2) ? Integer.valueOf(args[2]) : 15;

        PTBConverter lc = new PTBConverter(tokenSize);

        String prefix = getFilenamePrefix(filename);

        PTBConverter.log.info("Reading PTB data...");
        lc.read(getReaderForFilename(filename));

        PTBConverter.log.info("Converting PTB trees...");
        lc.convert();
        PTBConverter.log.log(Level.INFO, "Converted rules: {0}", String.valueOf(lc.ruleMap.size()));

        PTBConverter.log.info("Adding features...");
        int numFeatures = lc.addFeatures();
        PTBConverter.log.log(Level.INFO, "Features: {0}", String.valueOf(numFeatures));

        PTBConverter.log.info("Writing grammar...");
        lc.writeGrammar(new FileWriter(prefix + "-grammar.irtg"));

        PTBConverter.log.info("Writing corpus...");
        lc.writeCorpus(new FileWriter(prefix + "-corpus.txt"), sortByInterpretation);


        lc = null;

        PTBConverter.log.info("Parsing grammar...");
        MaximumEntropyIrtg irtg = (MaximumEntropyIrtg) de.up.ling.irtg.IrtgParser.parse(new FileReader(prefix + "-grammar.irtg"));
        int i = irtg.getAutomaton().getRuleSet().size();
        PTBConverter.log.log(Level.INFO, "Parsed rules: {0}", String.valueOf(i));
        PTBConverter.log.info("Checking for loops... ");
        irtg.checkForLoops();
        irtg = null;
        PTBConverter.log.info("Done");
    }

    private static Reader getReaderForFilename(String filename) throws FileNotFoundException, IOException {
        if (filename.endsWith(".gz")) {
            return new InputStreamReader(new GZIPInputStream(new FileInputStream(filename)));
        } else {
            return new FileReader(filename);
        }
    }

    private static String getFilenamePrefix(String filenameWithPath) {
        String filename = new File(filenameWithPath).getName();
        
        if (filename.endsWith(".mrg")) {
            return filename.substring(0, filename.length() - 4);
        } else if (filename.endsWith(".mrg.gz")) {
            return filename.substring(0, filename.length() - 7);
        } else {
            return filename;
        }
    }
    private Map<String, String> ruleMap;             // mapping of string representation and name of a rule, i.e. "S/NP/VP" -> "r1"
    private Map<String, FeatureFunction> featureMap; // mapping of names to feature functions
    private List<Tree<String>> ptbTrees;             // list of PTB-trees
    private List<Tree<String>> irtgTrees;            // list of IRTG-trees
    private AnnotatedCorpus corpus;                  // annotated corpus
    private MaximumEntropyIrtg maxEntIrtg;           // the automaton storing the grammar
    private Homomorphism hStr;                       // homomorphism for StringAlgebra
    private Homomorphism hPtb;                       // homomorphism for PtbTreeAlgebra
    private int maxTerminalsPerSentence;             // max. sentence length (in words)

    public PTBConverter(int maxTerminals) {
        maxTerminalsPerSentence = maxTerminals;
        corpus = new AnnotatedCorpus();
        ptbTrees = new ArrayList<Tree<String>>();
        irtgTrees = new ArrayList<Tree<String>>();
        ruleMap = new HashMap<String, String>();
        featureMap = new HashMap<String, FeatureFunction>();
        ConcreteTreeAutomaton cta = new ConcreteTreeAutomaton();
        maxEntIrtg = new MaximumEntropyIrtg(cta, null);
        StringAlgebra stringAlgebra = new StringAlgebra();
        hStr = new Homomorphism(maxEntIrtg.getAutomaton().getSignature(), stringAlgebra.getSignature());
        maxEntIrtg.addInterpretation("i", new Interpretation(stringAlgebra, hStr));

        PtbTreeAlgebra ptbAlgebra = new PtbTreeAlgebra();
        hPtb = new Homomorphism(maxEntIrtg.getAutomaton().getSignature(), ptbAlgebra.getSignature());
        maxEntIrtg.addInterpretation("ptb", new Interpretation(ptbAlgebra, hPtb));

        log.setLevel(Level.ALL);
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
    public void read(Reader reader) throws IOException {
        PtbTreeAlgebra pta = new PtbTreeAlgebra();
        Tree<String> ptbTree = null;

        do {
            ptbTree = pta.parseFromReader(reader);
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
     * Converts all PTB-trees to IRTG-trees and collects the rules
     */
    public void convert() {
        ConcreteTreeAutomaton c = (ConcreteTreeAutomaton) maxEntIrtg.getAutomaton();
        PtbTreeAlgebra.binarizeInit();
        for (Tree<String> ptbTree : ptbTrees) {
            // store representation of PTB tree
            List<String> ptbObjects = new ArrayList<String>();
            ptbObjects.add(ptbTree.toString());
            // binarize the PTB-tree
            ptbTree = PtbTreeAlgebra.binarize(ptbTree);
            // extract and store the rules used in the PTB-tree
            extractRules(ptbTree);
            // add the root label to the final states
            c.addFinalState(ptbTree.getLabel());
            // convert the PTB-Tree to an IRTG-tree
            Tree<String> irtgTree = ptb2Irtg(ptbTree);
            irtgTrees.add(irtgTree);

            //create a corpus instance
            List<String> sentence = ptbTree.getLeafLabels();
            Map<String, Object> inputObjectsMap = new HashMap<String, Object>();
            // with the actual sentence
            inputObjectsMap.put("i", sentence);
            // and the PTB-tree as interpretations
            inputObjectsMap.put("ptb", ptbObjects);
            // and add the instance to the corpus
            corpus.getInstances().add(new AnnotatedCorpus.Instance(irtgTree, inputObjectsMap));
        }

    }

    /**
     * Extracts all rules from the given PTB-tree
     *
     * @param tree the PTB-tree
     */
    public void extractRules(Tree<String> tree) {
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
                    // create rule name (rXXX)
                    String ruleName = "r" + String.valueOf(ruleMap.size() + 1);
                    // remember that we have the rule already
                    ruleMap.put(ruleString, ruleName);
                    List<String> childStates = new ArrayList<String>(); // list of states on the right side
                    List<Tree<StringOrVariable>> ptbChildren = new ArrayList<Tree<StringOrVariable>>();
                    String label;
                    if (childrenValues.get(0)) { // the node's child is a leaf node --> terminal symbol
                        label = nodeChildren.get(0).getLabel(); // terminal symbol
                        hStr.add(ruleName, Tree.create(new StringOrVariable(label, false)));
                        ptbChildren.add(Tree.create(new StringOrVariable(label, false)));
                    } else {
                        // collect the child states and create the PTB interpretation
                        // the PTB interpretation is not nested but straight forward
                        for (int i = 0; i < childrenValues.size(); i++) {
                            label = nodeChildren.get(i).getLabel();
                            childStates.add(label);
                            String ptbLabel = "?" + String.valueOf(i + 1);
                            ptbChildren.add(Tree.create(new StringOrVariable(ptbLabel, true)));
                        }
                        // create the nested intepretation for the StringAlgebra
                        Tree<StringOrVariable> strInterp = computeInterpretation(1, childrenValues.size());
                        hStr.add(ruleName, strInterp);
                    }
                    // add interpretations the the homomorphisms
                    hPtb.add(ruleName, Tree.create(new StringOrVariable(node.getLabel(), false), ptbChildren));
                    // add the rule to the automaton
                    c.addRule(ruleName, childStates, node.getLabel());
                }
                return false;
            }
        });
    }

    /**
     * Creates a string representation for the rule at the PTB-node
     *
     * @param node the PTB-(sub)tree
     * @return String the rule representation
     */
    private String nodeToRuleString(Tree<String> node) {
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
    private Tree<StringOrVariable> computeInterpretation(int start, int max) {
        if (max == 1) {
            return Tree.create(new StringOrVariable("?1", true));
        }
        if (max < start) {
            // that's not right
            return null;
        }
        List<Tree<StringOrVariable>> strChildren = new ArrayList<Tree<StringOrVariable>>();
        String startLabel = "?" + String.valueOf(start);
        strChildren.add(Tree.create(new StringOrVariable(startLabel, true)));
        if ((start + 1) == max) {
            // last element to compute
            String maxLabel = "?" + String.valueOf(max);
            strChildren.add(Tree.create(new StringOrVariable(maxLabel, true)));
        } else {
            strChildren.add(computeInterpretation(start + 1, max));
        }
        return Tree.create(new StringOrVariable("*", false), strChildren);
    }

    /**
     * Converts a PTB-tree to an IRTG-tree
     *
     * @param tree the PTB-tree
     * @return Tree<String> the converted IRTG-tree
     */
    public Tree<String> ptb2Irtg(Tree<String> tree) {
        if (tree == null) {
            return null;
        }
        return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            /**
             * Returns the IRTG-(sub)tree corresponding to <tt>node</tt>
             *
             * @param node the PTB-(sub)tree
             * @param childrenValues the already converted child nodes of
             * <tt>node</tt>
             * @return Tree<String> the IRTG-(sub)tree
             */
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
                    System.err.println("Rule not found for: " + node.toString());
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

    /**
     * Writes the grammar rules to a writer, e.g., string or file
     *
     * @param writer the writer to store the data into
     * @throws IOException if the writer cannot store the data properly
     */
    public void writeGrammar(Writer writer) throws IOException {
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
     * @throws IOException if the writer cannot store the data properly
     */
    public void writeCorpus(Writer writer, final String sortByInterpretation) throws IOException {
        String nl = System.getProperty("line.separator");
        List<Instance> instances = corpus.getInstances();


        if (instances.isEmpty()) {
            // if the corpus is empty we have nothing to do
            writer.close();
            return;
        }

        Set<String> interpretations = instances.get(0).inputObjects.keySet();

        // write the indices for all interpretations
        for (String interp : interpretations) {
            writer.write(interp + nl);
        }

        // if requested, sort instances by length in interpretation sortByInterpretation
        if (sortByInterpretation != null) {
            Collections.sort(instances, new Comparator<Instance>() {
                public int compare(Instance t, Instance t1) {
                    return t.inputObjects.get(sortByInterpretation).toString().length() - t1.inputObjects.get(sortByInterpretation).toString().length();
                }
            });
        }

        // write every instance
        for (AnnotatedCorpus.Instance instance : corpus.getInstances()) {
            // for every instance write their interpretations
            for (String interp : interpretations) {
                String interpretation = StringTools.join((List<String>) instance.inputObjects.get(interp), " ");
                writer.write(interpretation + nl);
            }
            // and their tree
            writer.write(instance.tree.toString() + nl);
        }
        writer.close();
    }

    /**
     * Creates a list of feature functions used with the MaximumEntropyIrtg.
     * There are endless ways to create such a list. This is a wrapper function.
     * Choose a specific function or a combination of them for the actual
     * creation.
     *
     */
    public int addFeatures() {
        Set<String> featuredRules = new HashSet<String>();
        addAllRuleFeatures();
//        addParentRelatedFeatures(featuredRules);
//        addTerminalRelatedFeatures(featuredRules);
//        addChildRelatedFeatures();

        maxEntIrtg.setFeatures(featureMap);
        return featureMap.size();
    }

    public void addAllRuleFeatures() {
        int num = featureMap.size();
        String featureName;
        Collection<String> rules = ruleMap.values();
        for (String r : rules) {
            featureName = "f" + String.valueOf(++num);
            featureMap.put(featureName, new RuleNameFeature(r));
        }
    }

    public void addChildRelatedFeatures() {
        Map<String, Set<String>> stateRules = new HashMap<String, Set<String>>();
        Set<Rule<String>> ruleSet = maxEntIrtg.getAutomaton().getRuleSet();
        for (Rule<String> r : ruleSet) {
            String parentState = r.getParent();
            Object[] children = r.getChildren();
            for (Object child : children) {
                String state = (String) child;
                if (stateRules.containsKey(state) && !stateRules.get(state).contains(parentState)) {
                    stateRules.get(state).add(parentState);
                } else {
                    Set<String> rules = new HashSet<String>();
                    rules.add(parentState);
                    stateRules.put(state, rules);
                }
            }
        }
        int num = featureMap.size();
        String featureName;
        Set<String> states = stateRules.keySet();
        for (String state : states) {
            Set<String> parentStates = stateRules.get(state);
            if (parentStates.size() > 1) {
                for (String p : parentStates) {
                    featureName = "f" + String.valueOf(++num);
                    featureMap.put(featureName, new ChildOfFeature(p, state));
                }
            }
        }
    }

    private void addRuleFeatures(final Map<String, Set<String>> stateRules, Set<String> featuredRules) {
        int num = featureMap.size();
        String featureName;
        Collection<Set<String>> rules = stateRules.values();
        for (Set<String> rulesSet : rules) {
            if (rulesSet.size() > 1) {
                for (String r : rulesSet) {
                    featureName = "f" + String.valueOf(++num);
                    featureMap.put(featureName, new RuleNameFeature(r));
                    featuredRules.add(r);
                }
            }
        }
    }

    public void addParentRelatedFeatures(Set<String> featuredRules) {
        Map<String, Set<String>> stateRules = new HashMap<String, Set<String>>();
        Set<Rule<String>> ruleSet = maxEntIrtg.getAutomaton().getRuleSet();
        for (Rule<String> r : ruleSet) {
            String ruleName = r.getLabel();
            if (!featuredRules.contains(ruleName)) {
                String state = r.getParent();
                if (stateRules.containsKey(state)) {
                    stateRules.get(state).add(ruleName);
                } else {
                    Set<String> rules = new HashSet<String>();
                    rules.add(ruleName);
                    stateRules.put(state, rules);
                }
            }
        }
        addRuleFeatures(stateRules, featuredRules);
    }

    public void addTerminalRelatedFeatures(Set<String> featuredRules) {
        Map<String, Set<String>> interpRules = new HashMap<String, Set<String>>();
        Set<Rule<String>> ruleSet = maxEntIrtg.getAutomaton().getRuleSet();
        for (Rule<String> r : ruleSet) {
            String ruleName = r.getLabel();
            if (!featuredRules.contains(ruleName)) {
                Object[] children = r.getChildren();
                if (children.length == 0) {
                    String interp = hStr.get(ruleName).toString();
                    if (interpRules.containsKey(interp)) {
                        interpRules.get(interp).add(ruleName);
                    } else {
                        Set<String> rules = new HashSet<String>();
                        rules.add(ruleName);
                        interpRules.put(interp, rules);
                    }
                }
            }
        }
        addRuleFeatures(interpRules, featuredRules);
    }
}