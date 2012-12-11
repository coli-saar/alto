/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.maxent;

import de.saar.chorus.term.parser.TermParser;
import de.up.ling.irtg.AnnotatedCorpus;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.ParseException;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.PtbTreeAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import de.up.ling.tree.TreeVisitor;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author danilo
 */
public class PTBConverter {

    public static void main(String[] args) throws IOException, ParseException, ParserException {
        PTBConverter lc = new PTBConverter();
        String prefix = (args.length > 0) ? args[0] : "examples/ptb-test";
        
        lc.read(new FileReader(prefix + ".mrg"));
        lc.writeRules(new FileWriter(prefix + "-grammar.irtg"));
        lc.writeCorpus(new FileWriter(prefix + "-corpus.txt"));
        
//        InterpretedTreeAutomaton irtg = IrtgParser.parse(new FileReader(prefix + "-grammar.irtg"));
        
//        System.err.println("Converted rules: " + String.valueOf(lc.getNumOfRules()));
//        int i = irtg.getAutomaton().getRuleSet().size();
//        System.err.println("Parsed rules: " + String.valueOf(i));
    }

    private Map<String, String> ruleMap;
    private List<Tree<String>> ptbTrees;
    private List<Tree<String>> irtgTrees;
    private AnnotatedCorpus corpus;
    private AnnotatedCorpus irtgCorpus;
    private InterpretedTreeAutomaton ita;
    private Homomorphism hIrtg;
    private Homomorphism hPtb;

    public PTBConverter() {
        corpus = new AnnotatedCorpus();
        ptbTrees = new ArrayList<Tree<String>>();
        irtgTrees = new ArrayList<Tree<String>>();
        ruleMap = new HashMap<String, String>();
        ConcreteTreeAutomaton cta = new ConcreteTreeAutomaton();
        ita = new InterpretedTreeAutomaton(cta);
        StringAlgebra stringAlgebra = new StringAlgebra();
        hIrtg = new Homomorphism(ita.getAutomaton().getSignature(), stringAlgebra.getSignature());
        ita.addInterpretation("i", new Interpretation(stringAlgebra, hIrtg));

        PtbTreeAlgebra ptbAlgebra = new PtbTreeAlgebra();
        hPtb = new Homomorphism(ita.getAutomaton().getSignature(), ptbAlgebra.getSignature());
        ita.addInterpretation("ptb", new Interpretation(ptbAlgebra, hPtb));
    }

    public List<Tree<String>> getPtbTrees() {
        return ptbTrees;
    }

    public void read(Reader reader) throws IOException, ParserException {
        PtbTreeAlgebra pta = new PtbTreeAlgebra();
        
        Tree<String> ptbTree = null;
        do {
            ptbTree = pta.parseFromReader(reader);
            if (ptbTree != null) {
                ptbTrees.add(ptbTree);
            }
        } while (ptbTree != null);
        
        ConcreteTreeAutomaton c = (ConcreteTreeAutomaton) ita.getAutomaton();
        for (Tree<String> ptbT : ptbTrees) {
            extractRules(ptbT);
            c.addFinalState(ptbT.getLabel());
            Tree<String> irtgTree = ptb2Irtg(ptbT);
            irtgTrees.add(irtgTree);
            List<String> sentence = ptbT.getLeafLabels();
            List<String> ptbObjects = new ArrayList<String>();
            ptbObjects.add(ptbT.toString());
            Map<String, Object> inputObjectsMap = new HashMap<String, Object>();
            inputObjectsMap.put("i", sentence);
            inputObjectsMap.put("ptb", ptbObjects);
            corpus.getInstances().add(new AnnotatedCorpus.Instance(irtgTree, inputObjectsMap));
        }
        Map<String, Reader> rdrs = new HashMap<String, Reader>();
        rdrs.put("i", new StringReader("QS Yes PERIOD"));
//        ita.decode(new StringReader("ptb"), rdrs);
    }

    public void extractRules(Tree<String> tree) {
        final ConcreteTreeAutomaton c = (ConcreteTreeAutomaton) ita.getAutomaton();
        tree.dfs(new TreeVisitor<String, Void, Boolean>() {
            @Override
            public Boolean combine(Tree<String> node, List<Boolean> childrenValues) {
                if (node.getChildren().isEmpty()) {
                    return true; // we have a leaf node
                }
                String ruleString = nodeToRuleString(node);
                if (!ruleMap.containsKey(ruleString)) {
                    // add new rule
                    String ruleName = "r" + String.valueOf(ruleMap.size()+1);
                    ruleMap.put(ruleString, ruleName);
                    List<String> childStates = new ArrayList<String>();
                    StringBuilder irtgInterp = new StringBuilder();
                    StringBuilder ptbInterp = new StringBuilder();
                    ptbInterp.append(node.getLabel());
                    String label;
                    if (childrenValues.get(0)) {
                        label = node.getChildren().get(0).getLabel();
                        irtgInterp.append(label);
                        ptbInterp.append("(");
                        ptbInterp.append(label);
                        ptbInterp.append(")");
                    } else {
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
                        if (childrenValues.size() == 1) {
                            irtgInterp.append("?1");
                        } else {
                            irtgInterp.append(computeInterpretation(1, childrenValues.size()));
                        }
                    }
                    hIrtg.add(ruleName, TermParser.parse(irtgInterp.toString()).toTreeWithVariables());
                    hPtb.add(ruleName, TermParser.parse(ptbInterp.toString()).toTreeWithVariables());
                    c.addRule(ruleName, childStates, node.getLabel());
                }
                return false;
            }
        });
    }
    
    private String nodeToRuleString(Tree<String> node) {
        StringBuilder ret = new StringBuilder();
        ret.append(node.getLabel());
        for (Tree<String> n : node.getChildren()) {
            ret.append("/");
            ret.append(n.getLabel());
        }
        return ret.toString();
    }

    private String computeInterpretation(int count, int max) {
        StringBuilder ret = new StringBuilder();
        ret.append("*(?");
        if ((count + 1) == max) {
            ret.append(count);
            ret.append(",?");
            ret.append(max);
        } else {
            ret.append(count);
            ret.append(",");
            ret.append(computeInterpretation(count+1,max));
        }
        ret.append(")");
        return ret.toString();
    }

    public Tree<String> ptb2Irtg(Tree<String> tree) {
        return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                if (node.getChildren().isEmpty()) {
                    return null; // we have a leaf node
                }
                String ruleString = nodeToRuleString(node);
                String ruleName = ruleMap.get(ruleString);
                if (ruleName == null) {
                    System.err.println("Rule not found for: " + node.toString());
                    return null;
                }
                if ((childrenValues.size() == 1) && (childrenValues.get(0) == null)) {
                    return Tree.create(ruleName);
                }
                return Tree.create(ruleName, childrenValues);
            }            
        });        
    }

    public void writeRules(Writer writer) throws IOException {
        writer.write(ita.toString());
        writer.close();
    }

    public void writeCorpus(Writer writer) throws IOException {
        String nl = System.getProperty("line.separator");
        if (corpus.getInstances().isEmpty()) {
            writer.close();
            return;
        }
        for (String interp : corpus.getInstances().get(0).inputObjects.keySet()) {
            writer.write(interp + nl);
        }
        for (AnnotatedCorpus.Instance instance : corpus.getInstances()) {
            for (String interp : corpus.getInstances().get(0).inputObjects.keySet()) {
                String interpretation = MaximumEntropyIrtg.join((List<String>)instance.inputObjects.get(interp));
                writer.write(interpretation + nl);
            }
            writer.write(instance.tree.toString() + nl);
        }
        writer.close();
    }

    
    @Deprecated
    private Tree addTree(Reader reader, PTBRule parentRule, List<String> inputObjects) throws IOException {
        String object = "";
        PTBRule rule = new PTBRule();
        Tree<String> tree = Tree.create(new String());

        for (int c; (c = reader.read()) != -1;) {

            if (c == '(') {
                Tree<String> subTree = addTree(reader, rule, inputObjects);
                if (subTree != null) {
                    tree = tree.addSubtree(subTree);
                }
            } else if (c == ')') {
                if (((String) tree.getLabel()).isEmpty()) {
                    if (!object.isEmpty()) {
                        object = object.toLowerCase();
                        inputObjects.add(object);
                        if (object.contains("'")) {
                            object = '"' + object + '"';
                        } else if (startsWithInt(object) ||
                                object.contains("-") || object.contains(",") || object.contains(".")) {
                            object = "'" + object + "'";
                        }
                        rule.interpretation = object;
                    }
                    if ((rule.right.size() > 0) || (!rule.interpretation.isEmpty())) {
                        int delim = rule.left.indexOf("|");
                        if (delim > 0) {
                            rule.left = rule.left.substring(0, delim);
                        }
                        PTBRule r = findRule(rule);
                        if (r == null) {
                        } else {
                            rule = r;
                        }
                        if (parentRule != null) {
                            parentRule.right.add(rule.left);
                        }
                        tree.setLabel(rule.name);
                    }
                }
                return (tree.getLabel().isEmpty() && tree.getChildren().isEmpty()) ? null : tree;
            } else if (c == 32) {
                if (!object.isEmpty() && rule.left.isEmpty()) {
                    if (object.equals(",")) {
                        object = "SEP-COM";
                    } else if (object.equals(".")) {
                        object = "SEP-PER";
                    } else if (object.equals(":")) {
                        object = "SEP-COL";
                    }
                    rule.left = object.replaceAll("(-\\d)|(=\\d)", "");
                    object = "";
                }
            } else if ((c == 96) && rule.left.isEmpty()) {
                reader.skip(5);
                inputObjects.add("''");
            } else if ((c == 39) && rule.left.isEmpty()) {
                reader.skip(5);
                inputObjects.add("''");
            } else {
                if((c == 45) && object.isEmpty() && rule.left.isEmpty()) {
                    skipNullElement(reader);
                    return null;
                } else if(c == 91) { // char for [
                    skipNullElement(reader);
                    return null;
                } else if (c > 10) {
                    object += (char) c;
                }
            }
        }

        System.err.println("Unexpected end of parsed tree.");
        return null;
    }
    
    @Deprecated
    private void skipNullElement(Reader reader) throws IOException {
        for (int c; (c = reader.read()) != -1;) {
            if (c == 41) {
                return;
            }
        }
    }
    
    @Deprecated
    private PTBRule findRule(PTBRule rule) {
/*        for (PTBRule r : rules) {
            if(r.equals(rule)) {
                return r;
            }
        }
*/        return null;
    }
    
    @Deprecated
    private PTBRule findRule(String label) {
/*        for (PTBRule r : rules) {
            if(r.name.equals(label)) {
                return r;
            }
        }
*/        return null;
    }
    
    @Deprecated
    private class PTBRule {
        public String left;
        public List<String> right;
        public String interpretation;
        public String name;
        public boolean possibleStart;
        
        public PTBRule() {
            left = "";
            right = new ArrayList<String>();
            interpretation = "";
            name = "";
            possibleStart = false;
        }
        
        public boolean equals(PTBRule lr) {
            if (left.equals(lr.left) || left.equals(lr.left + "!")) {
                if (right.size() > 0) {
                    if (right.size() == lr.right.size()) {
                        for (int i = 0; i < right.size(); i++) {
                            if (!right.get(i).equals(lr.right.get(i))) {
                                return false;
                            }
                        }
                        return true;
                    }
                } else {
                    return (interpretation.equals(lr.interpretation));
                }
            }
            return false;
        }
        
        @Override
        public String toString() {
            String out = (possibleStart) ? left + "!" : left;
            out += " -> " + name;
            if (!right.isEmpty()) {
                out += "(" + right.get(0);
                for (int i = 1; i < right.size(); i++) {
                    out += "," + right.get(i);
                }
                out += ")";
            }
            out += "\n    [i] ";
            if (interpretation.isEmpty()) {
                if (right.size() == 1) {
                    out += "?1";
                } else {
                    out += computeInterpretation(1, right.size());
                }
            } else {
                out += interpretation;
            }
            return out;
        }
        
        public String computeInterpretation(int count, int max) {
            if ((count + 1) == max) {
                return "*(?" + String.valueOf(count) + ",?" + String.valueOf(max) + ")";
            }
            return "*(?" + String.valueOf(count) + "," + computeInterpretation(count+1,max) + ")";
        }
    }
    
    @Deprecated
    public static boolean startsWithInt(String s) {
        char start = s.charAt(0);
        try {  
            Integer.parseInt(String.valueOf(start));  
            return true;  
        } catch( Exception e) {  
            return false;  
        }  
    }  

}