package de.up.ling.irtg.maxent;

import de.saar.basic.StringOrVariable;
import de.saar.basic.StringTools;
import de.saar.chorus.term.parser.TermParser;
import de.up.ling.irtg.AnnotatedCorpus;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.ParseException;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.PtbTreeAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Danilo Baumgarten
 */
public class PTBConverter {

    /**
     * For testing
     */
    public static void main(String[] args) throws IOException, ParseException, ParserException {
        PTBConverter lc = new PTBConverter();
        String prefix = (args.length > 0) ? args[0] : "examples/ptb-test";
        
        lc.read(new FileReader(prefix + ".mrg"));                   // read the data
        lc.convert();                                               // convert the trees
        lc.writeGrammar(new FileWriter(prefix + "-grammar.irtg"));    // write the grammar
        lc.writeCorpus(new FileWriter(prefix + "-corpus.txt"));     // write the corpus
        
        de.up.ling.irtg.InterpretedTreeAutomaton irtg = de.up.ling.irtg.IrtgParser.parse(new FileReader(prefix + "-grammar.irtg"));
        System.err.println("Converted rules: " + String.valueOf(lc.ruleMap.size()));
        int i = irtg.getAutomaton().getRuleSet().size();
        System.err.println("Parsed rules: " + String.valueOf(i));
    }

    public Map<String, String> ruleMap;             // mapping of string representation and name of a rule, i.e. "S/NP/VP" -> "r1"
    private Map<String, FeatureFunction> featureMap; // mapping of names to feature functions
    private List<Tree<String>> ptbTrees;             // list of PTB-trees
    private List<Tree<String>> irtgTrees;            // list of IRTG-trees
    private AnnotatedCorpus corpus;                  // annotated corpus
    private MaximumEntropyIrtg maxEntIrtg;           // the automaton storing the grammar
    private Homomorphism hStr;                       // homomorphism for StringAlgebra
    private Homomorphism hPtb;                       // homomorphism for PtbTreeAlgebra

    public PTBConverter() {
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
            if (ptbTree != null) {
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
        for (Tree<String> ptbTree : ptbTrees) {
            // extract and store the rules used in the PTB-tree
            extractRules(ptbTree);
            // add the root label to the final states
            c.addFinalState(ptbTree.getLabel());
            // convert the PTB-Tree to an IRTG-tree
            Tree<String> irtgTree = ptb2Irtg(ptbTree);
            irtgTrees.add(irtgTree);
            
            //create a corpus instance
            List<String> sentence = ptbTree.getLeafLabels();
            List<String> ptbObjects = new ArrayList<String>();
            ptbObjects.add(ptbTree.toString());
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
             * @param childrenValues a list of flags whether the child node is a leaf node or not
             * @return Boolean whether <tt>node</tt> is a leaf node or not
             */
            @Override
            public Boolean combine(Tree<String> node, List<Boolean> childrenValues) {
                if (node.getChildren().isEmpty()) {
                    return true; // we have a leaf node
                }
                // create a string representation for the rule, i.e. 'S/NP/VP'
                String ruleString = nodeToRuleString(node);
                // create and store the rule if it not exists
                if (!ruleMap.containsKey(ruleString)) {
                    // create rule name (rXXX)
                    String ruleName = "r" + String.valueOf(ruleMap.size()+1);
                    // remember that we have the rule already
                    ruleMap.put(ruleString, ruleName);
                    List<String> childStates = new ArrayList<String>(); // list of states on the right side
                    StringBuilder strInterp = new StringBuilder(); // interpretation for StringAlgebra
                    StringBuilder ptbInterp = new StringBuilder(); // interpretation for PtbTreeAlgebra
                    ptbInterp.append(node.getLabel()); // the PTB interpretation starts with the parent state / node label
                    String label;
                    boolean stringOrVariable = false;
                    if (childrenValues.get(0)) { // the node's child is a leaf node --> terminal symbol
                        label = node.getChildren().get(0).getLabel(); // terminal symbol
                        strInterp.append(label);
                        ptbInterp.append("(");
                        ptbInterp.append(label);
                        ptbInterp.append(")");
                    } else {
                        // collect the child states and create the PTB interpretation
                        // the PTB interpretation is not nested but straight forward
                        for (int i = 0; i < childrenValues.size(); i++) {
                            label = node.getChildren().get(i).getLabel();
                            childStates.add(label);
                            if (i == 0) {
                                ptbInterp.append("(");
                            } else {
                                ptbInterp.append(",");
                            }
                            ptbInterp.append("?");
                            ptbInterp.append(i+1);
                        }
                        ptbInterp.append(")");
                        // create the nested intepretation for the StringAlgebra
                        strInterp.append(computeInterpretation(1, childrenValues.size()));
                        stringOrVariable = true;
                    }
                    // add interpretations the the homomorphisms
                    String parsable = strInterp.toString();
                    if (parsable.equals("`")) {
                        parsable = "'" + parsable + "'";
                    }
                    hStr.add(ruleName, Tree.create(new StringOrVariable(parsable, false)));
                    hPtb.add(ruleName, Tree.create(new StringOrVariable(ptbInterp.toString(), stringOrVariable)));
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
     * Computes the (nested) interpretation for rules containing no terminal symbol
     * 
     * @param start the number of the first element of the interpretation, e.g., 1
     * @param max the number of elements of the interpretation
     * @return String the interpretation
     */
    private String computeInterpretation(int start, int max) {
        if (max == 1) {
            // only one element
            return "?1";
        } else if (max < start) {
            // that's not right
            return "";
        }
        StringBuilder ret = new StringBuilder();
        ret.append("*(?");
        if ((start + 1) == max) {
            // these are the last 2 elements resulting in something like "*(?X, ?Y)"
            ret.append(start);
            ret.append(",?");
            ret.append(max);
        } else {
            ret.append(start);
            ret.append(",");
            // recursive call
            ret.append(computeInterpretation(start+1,max));
        }
        ret.append(")");
        return ret.toString();
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
             * @param childrenValues the already converted child nodes of <tt>node</tt>
             * @return Tree<String> the IRTG-(sub)tree
             */
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                if ((node== null) || node.getChildren().isEmpty()) {
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
    public void writeCorpus(Writer writer) throws IOException {
        String nl = System.getProperty("line.separator");
        if (corpus.getInstances().isEmpty()) {
            // if the corpus is empty we have nothing to do
            writer.close();
            return;
        }
        
        // write the indices for all interpretations
        for (String interp : corpus.getInstances().get(0).inputObjects.keySet()) {
            writer.write(interp + nl);
        }
        
        // write every instance
        for (AnnotatedCorpus.Instance instance : corpus.getInstances()) {
            // for every instance write their interpretations
            for (String interp : corpus.getInstances().get(0).inputObjects.keySet()) {
                String interpretation = StringTools.join((List<String>)instance.inputObjects.get(interp), " ");
                writer.write(interpretation + nl);
            }
            // and their tree
            writer.write(instance.tree.toString() + nl);
        }
        writer.close();
    }

    /**
     * Creates a list of feature functions used with the MaximumEntropyIrtg.
     * There are endless ways to create such a list. This is a wrapper
     * function. Choose a specific function or a combination of them for the
     * actual creation.
     * 
     */
    public void addFeatures() {
        addManualFeatures();
        addAllRuleFeatures();
        
        maxEntIrtg.setFeatures(featureMap);
    }

    public void addManualFeatures() {
        int num = featureMap.size() + 1;
        String featureName = "f" + String.valueOf(num);
        featureMap.put(featureName, new ChildOfFeature("N","PP"));
        featureName = "f" + String.valueOf(++num);
        featureMap.put(featureName, new ChildOfFeature("VP","PP"));
    }

    public void addAllRuleFeatures() {
        int num = featureMap.size();
        String featureName;
        for (String r : ruleMap.values()) {
            featureName = "f" + String.valueOf(++num);
            featureMap.put(featureName, new TestFeature(r));
        }
    }

}