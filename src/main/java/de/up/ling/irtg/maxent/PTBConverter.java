/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.maxent;

import de.up.ling.irtg.AnnotatedCorpus;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.IrtgParser;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.io.*;

/**
 *
 * @author danilo
 */
public class PTBConverter {

    public static void main(String[] args) throws IOException, de.up.ling.irtg.ParseException {
        PTBConverter lc = new PTBConverter();
        String prefix = args[0];
        
        lc.read(new FileReader(prefix + ".mrg"));
        lc.writeRules(new FileWriter(prefix + "-grammar.irtg"));
        lc.writeCorpus(new FileWriter(prefix + "-corpus.txt"));
        
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new FileReader(prefix + "-grammar.irtg"));
        
        System.err.println("Converted rules: " + String.valueOf(lc.getNumOfRules()));
        int i = irtg.getAutomaton().getRuleSet().size();
        System.err.println("Parsed rules: " + String.valueOf(i));
    }

    private static List<String> specialTerminals;
    private AnnotatedCorpus corpus;
    private List<PTBRule> rules;
    private PTBRule quoteStartRule;
    private PTBRule quoteEndRule;

    public PTBConverter() {
        specialTerminals = new ArrayList<String>();
        Collections.addAll(specialTerminals, new String[] {";",":","!","?","&","%","/","\\","interpretation","feature"});
        corpus = new AnnotatedCorpus();
        rules = new ArrayList<PTBRule>();
        quoteStartRule = new PTBRule();
        quoteStartRule.left = "QUOTE-S";
        quoteStartRule.interpretation = "\"''\"";
        quoteStartRule.name = "r1";
        rules.add(quoteStartRule);
        quoteEndRule = new PTBRule();
        quoteEndRule.left = "QUOTE-E";
        quoteEndRule.interpretation = "\"''\"";
        quoteEndRule.name = "r2";
        rules.add(quoteEndRule);
    }

    public int getNumOfRules() {
        return rules.size();
    }

    public void read(Reader reader) throws IOException {
        for (int c; (c = reader.read()) != -1;) {
            
            if (c == 40) {
                List<String> inputObjects = new ArrayList<String>();
                PTBRule parent = new PTBRule();
                Tree<String> tree = addTree(reader, parent, inputObjects);

                if (tree != null) {
                    PTBRule rule = null;
                    if (tree.getLabel().isEmpty() && !tree.getChildren().isEmpty()) {
                        List<Tree<String>> children = tree.getChildren();
                        if (children.size() == 1) {
                            tree = children.get(0);
                        } else {
                            for (int i = 0; i < children.size(); i++) {
                                if (!children.get(i).getChildren().isEmpty()) {
                                    tree = children.get(i);
                                    rule = findRule(tree.getLabel());
                                    for (int j = i-1; j >= 0; j--) {
                                        Tree<String> subTree = children.get(j);
                                        tree.getChildren().add(0, subTree);
                                        PTBRule subRule = findRule(subTree.getLabel());
                                        rule.right.add(0, subRule.left);
                                    }
                                    for (int j = i+1; j < children.size(); j++) {
                                        Tree<String> subTree = children.get(j);
                                        tree.getChildren().add(subTree);
                                        PTBRule subRule = findRule(subTree.getLabel());
                                        rule.right.add(subRule.left);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    if (rule == null) {
                        rule = findRule(tree.getLabel());
                    }
                    rule.possibleStart = true;
                    Map<String, Object> inputObj = new HashMap<String, Object>();
                    inputObj.put("i", inputObjects);
                    AnnotatedCorpus.Instance instance = new AnnotatedCorpus.Instance(tree, inputObj);
                    corpus.getInstances().add(instance);
                }

            }

        }

    }

    public void writeRules(Writer writer) throws IOException {
        String nl = System.getProperty("line.separator");
        writer.write("interpretation i: de.up.ling.irtg.algebra.StringAlgebra" + nl + nl);

        for (PTBRule r : rules) {
            writer.write(r.toString() + nl);
        }

        writer.close();
    }

    public void writeCorpus(Writer writer) throws IOException {
        // TODO: write the corpus
        String nl = System.getProperty("line.separator");
        writer.write("i" + nl);

        for (AnnotatedCorpus.Instance instance : corpus.getInstances()) {
            String sentence = MaximumEntropyIrtg.join((List<String>)instance.inputObjects.get("i"));
            writer.write(sentence + nl);
            writer.write(instance.tree + nl);
        }

        writer.close();
    }

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
                        } else if (specialTerminals.contains(object) || startsWithInt(object) ||
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
                            if (!rule.left.isEmpty()) {
                                rule.name = "r" + String.valueOf(rules.size() + 1);
                                rules.add(rule);
                            }
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
                parentRule.right.add(quoteStartRule.left);
                inputObjects.add("''");
                return Tree.create(quoteStartRule.name);
            } else if ((c == 39) && rule.left.isEmpty()) {
                reader.skip(5);
                parentRule.right.add(quoteEndRule.left);
                inputObjects.add("''");
                return Tree.create(quoteEndRule.name);
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
    
    private void skipNullElement(Reader reader) throws IOException {
        for (int c; (c = reader.read()) != -1;) {
            if (c == 41) {
                return;
            }
        }
    }
    
    private PTBRule findRule(PTBRule rule) {
        for (PTBRule r : rules) {
            if(r.equals(rule)) {
                return r;
            }
        }
        return null;
    }
    
    private PTBRule findRule(String label) {
        for (PTBRule r : rules) {
            if(r.name.equals(label)) {
                return r;
            }
        }
        return null;
    }
    
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