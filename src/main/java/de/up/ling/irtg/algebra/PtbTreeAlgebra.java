package de.up.ling.irtg.algebra;

import de.up.ling.tree.TreeVisitor;
import de.up.ling.tree.Tree;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author koller
 * @author Danilo Baumgarten
 */
public class PtbTreeAlgebra extends TreeAlgebra {
    private static final String START_SEQUENCE = "( ";
    private static Pattern LABEL_PATTERN = Pattern.compile("([a-zA-Z-]+)([0-9]+)");
    private static Map<String, String> BINARY_LABELS;
    private static final String BIN_LABEL_PREFIX = "ART-BIN";

    /**
     * For testing only
     */
    public static void main(String[] args) throws ParserException {
        PtbTreeAlgebra pta = new PtbTreeAlgebra();
        Tree<String> tree = pta.parseString("( (`` ``) (INTJ (UH Yes) (. .) ))");
        System.err.println(tree.toString());
        Map<String, String> map = new HashMap<String, String>();
        map.put("``4/UH1","~XXX");
        tree = binarize(tree, map);
        System.err.println(tree.toString());
    }

    /**
     * Parses PTB-formatted string
     *
     * @param representation the string containing the data
     * @returns Tree<String> containing the parsed PTB-tree
     * @throws ParserException if an error occurs on parsing the input
     */
    @Override
    public Tree<String> evaluate(Tree<String> tree) {
        return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childValues) {
                List<Tree<String>> newChildValues = new ArrayList<Tree<String>>();
                for (int i = 0; i < childValues.size(); i++) {
                    Tree<String> child = childValues.get(i);
                    if (child.getLabel().startsWith(BIN_LABEL_PREFIX)) {
                        newChildValues.addAll(child.getChildren());
                    } else {
                        newChildValues.add(child);
                    }
                }
                return Tree.create(node.getLabel(),newChildValues);
            }            
        });        
    }

    /**
     * Parses PTB-formatted string
     *
     * @param representation the string containing the data
     * @returns Tree<String> containing the parsed PTB-tree
     * @throws ParserException if an error occurs on parsing the input
     */
    @Override
    public Tree<String> parseString(String representation) throws ParserException {
        try {
            StringReader reader = new StringReader(representation);
            return parseFromReader(reader);
        } catch (IOException e) {
            throw new ParserException(e);
        }
    }
    
    /**
     * Parses PTB-formatted trees from reader
     *
     * @param reader the reader containing the data
     * @returns Tree<String> containing the parsed PTB-tree
     * @throws IOException if an error occurs on reading chars from <tt>reader</tt>
     */
    public Tree<String> parseFromReader(Reader reader) throws IOException {
        // first we're looking for the beginning of a tree
        String input = "";
        do {
            int c = reader.read();
            if (c == -1) {
                return null;
            } else if (c == '(') {
                input = "(";
            } else {
                input += (char) c;
            }
            // skip every input until reaching the sequence indicating a new tree
        } while (!input.equals(START_SEQUENCE));
        // parse the tree
        Tree<String> tree = parseTree(reader);
        // a PTB-tree consists of a "wrapper"-tree its children representing the actual sentence
        // therefore we take a look at these children
        List<Tree<String>> children = tree.getChildren();
        // without any child nodes we have no tree
        if (children.isEmpty()) {
            return null;
        }
        // there are occurrences in which we have more than on tree representing the sentence
        if (children.size() > 1) {
            // for unification we select one tree and insert the others into that one
            for (int i = 0; i < children.size(); i++) {
                // a tree containing only a terminal symbol has only one child (the one with the terminal)
                // therefore we're looking for trees with more children or more depth
                List<Tree<String>> grandchildren = children.get(i).getChildren();
                if ((grandchildren.size() > 1) || !grandchildren.get(0).getChildren().isEmpty()) {
                    tree = children.get(i);
                    // prepend the trees coming before the new master tree
                    for (int j = 0; j < i; j++) {
                        Tree<String> subTree = children.get(j);
                        tree.getChildren().add(j, subTree);
                    }
                    // append the trees coming after the new master tree
                    for (int j = i+1; j < children.size(); j++) {
                        Tree<String> subTree = children.get(j);
                        tree.getChildren().add(subTree);
                    }
                    String label = tree.getLabel();
                    label = label.substring(0, label.length()-1) + String.valueOf(tree.getChildren().size());
                    tree.setLabel(label);
                    break;
                }
            }
        } else {
            // if the "wrapper" contains only one child node: that's our new master tree
            tree = children.get(0);
        }
        return tree;
    }
    
    /**
     * Parses PTB-formatted trees from reader
     *
     * @param reader the reader containing the data
     * @returns Tree<String> containing the parsed PTB-tree
     * @throws IOException if an error occurs on reading chars from <tt>reader</tt>
     */
    private Tree<String> parseTree(Reader reader) throws IOException {
        String label = "";
        StringBuffer buffer = new StringBuffer();
        List<Tree<String>> children = new ArrayList<Tree<String>>();
        
        for (int c; (c = reader.read()) != -1;) {

            if (c == '(') { // start of a new element
                // recursive call to get the sub tree
                Tree<String> subTree = parseTree(reader);
                
                if (subTree != null) {
                    children.add(subTree);
                }
            } else if (c == ')') { // end of an element
                // if the buffer is not empty we take care of that char sequence
                if (buffer.length() > 0) {
                    // there are 2 possibilities:
                    if (label.isEmpty()) {
                        // 1. the buffer contains a non-terminal symbol
                        label = buffer.toString();
                    } else {
                        // 2. the buffer contains a terminal symbol
                        children.add(Tree.create(buffer.toString()));
                    }
                }

                // add the #ofChildNodes to the label
                // at a later point the label (state) get a corresponding list of rules but only the right arity
                label += String.valueOf(children.size());
                // no children indicates no valid tree -> returning null
                return (children.isEmpty()) ? null : Tree.create(label, children);
            } else if (c == ' ') { // delimiter for symbols (e.g. "(<NONTERMINAL> <TERMINAL>)")
                // check the buffer
                if ((buffer.length() > 0) && label.isEmpty()) {
                    label = buffer.toString();
                    // remove trace indices
                    label = label.replaceAll("(-\\d)|(=\\d)", "");
                    // clear the buffer
                    buffer = new StringBuffer();
                }
            } else {
                
                if((c == '-') && (buffer.length() == 0) && label.isEmpty()) {
                    // - indcates a null element but only if we are at the start of a symbol (buffer is empty)
                    // and we have no label for the tree (a non terminal symbol) yet
                    // then we skip the current element
                    skipElement(reader);
                    return null;
                } else if(c == '[') {
                    // we skip elements containing '['
                    skipElement(reader);
                    return null;
                } else if (c > 32) {
                    // every not yet handled char - except control chars - is stored in the buffer
                    buffer.append((char) c);
                }

            }
        }

        // we reached the end of the reader without reading the closing char ')' for this tree
        System.err.println("Unexpected end of parsed input.");
        return null;
    }
    

    /**
     * Skips all char up to the end of the current element
     *
     * @param reader the reader containing the data
     */
    private void skipElement(Reader reader) throws IOException {
        for (int c; (c = reader.read()) != -1;) {
            if (c == ')') {
                return;
            }
        }
    }
    
    /**
     * Binarizes a PTB tree
     *
     * @param tree the PTB tree
     * @return Tree<String> the binarized tree
     */
    public static Tree<String> binarize(Tree<String> tree, Map<String,String> binaryLabels) {
        BINARY_LABELS = binaryLabels;
        return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                if (childrenValues.isEmpty()) {
                    return node;
                } else if (childrenValues.size() <= 2) {
                    return Tree.create(node.getLabel(), childrenValues);
                }
                do {
                    childrenValues = binarize(childrenValues);
                } while (childrenValues.size() > 2);
                Matcher matcher = LABEL_PATTERN.matcher(node.getLabel());
                String label = (matcher.find()) ? matcher.group(1) + "2" : node.getLabel() + "2";
                return Tree.create(label, childrenValues);
            }            
        });        
    }
    
    /**
     * Binarizes a list of trees
     *
     * @param list the list of trees 
     * @return List<Tree<String>> the binarized list
     */
    public static List<Tree<String>> binarize(List<Tree<String>> oldList) {
        List<Tree<String>> newList = new ArrayList<Tree<String>>();
        Iterator<Tree<String>> iter = oldList.iterator();
        do {
            Tree<String> node1 = iter.next();
            if (iter.hasNext()) {
                Tree<String> node2 = iter.next();
                String binLabelKey = node1.getLabel() + "/" + node2.getLabel();
                String label;
                if (BINARY_LABELS == null) {
                    label = "~";
                } else {
                    if (BINARY_LABELS.containsKey(binLabelKey)) {
                        label = BINARY_LABELS.get(binLabelKey);
                    } else {
                        label = BIN_LABEL_PREFIX + String.valueOf(BINARY_LABELS.size()+1);
                        BINARY_LABELS.put(binLabelKey, label);
                    }
                }
                List<Tree<String>> children = new ArrayList<Tree<String>>();
                children.add(node1);
                children.add(node2);
                newList.add(Tree.create(label, children));
            } else {
                newList.add(node1);
            }
        } while (iter.hasNext());
        return newList;
    }
}
