package de.up.ling.irtg.algebra;

import de.up.ling.tree.TreeVisitor;
import de.up.ling.tree.Tree;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
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
    private static Pattern TRACE_PATTERN = Pattern.compile("([^-=]+)([-=])(.+)");
    private static Pattern TMP_PATTERN = Pattern.compile("(.+)(\\^)(.+)");
    private static Map<String, String> LABELS;
    public static final String LABEL_PREFIX = "ART-";
    private static final String UN_LABEL_PREFIX = LABEL_PREFIX + "UN";
    private static final String BIN_LABEL_PREFIX = LABEL_PREFIX + "BIN";
    
    private int numWords;

    /**
     * For testing only
     */
    public static void main(String[] args) throws ParserException {
        PtbTreeAlgebra pta = new PtbTreeAlgebra();
        Tree<String> tree = pta.parseString("( (`` ``) (INTJ (UH Yes) (. .) ))");
        System.err.println(tree.toString());
        tree = binarize(tree);
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
                    } else if (child.getLabel().startsWith(UN_LABEL_PREFIX)) {
                        newChildValues.add(child.getChildren().get(0));
                    } else {
                        newChildValues.add(child);
                    }
                }
                return Tree.create(node.getLabel(),newChildValues);
            }            
        });        
    }

    /**
     * Returns the number of words/token from last parsing
     *
     * @returns int number of words/token
     */
    public int getNumWords() {
        return numWords;
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
        numWords = 0;
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
    private String stripPosTag(StringBuffer buf) {
        String s = buf.toString();
        // remove trace indices
        Matcher matcher = TRACE_PATTERN.matcher(s);
        s = matcher.find() ? matcher.group(1) : s;
        // clear the buffer
        return s;
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
                        label = stripPosTag(buffer);
                    } else {
                        // 2. the buffer contains a terminal symbol
                        children.add(Tree.create(buffer.toString()));
                        numWords++;
                    }
                }
                
                // no children indicates no valid tree -> returning null
                if (children.isEmpty()) {
                    return null;
                }
                if (children.size() == 1) {
                    Tree<String> child = children.get(0);
                    if (!child.getChildren().isEmpty() && child.getLabel().equals(label)) {
                        return child;
                    }
                }
                return Tree.create(label, children);
            } else if (c == ' ') { // delimiter for symbols (e.g. "(<NONTERMINAL> <TERMINAL>)")
                // check the buffer
                if ((buffer.length() > 0) && label.isEmpty()) {
                    label = stripPosTag(buffer);
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

    public static void binarizeInit() {
        LABELS = new HashMap<String, String>();
    }
    /**
     * Binarizes a PTB tree (as in Matsukaki et al.,2005)
     *
     * @param tree the PTB tree
     * @return Tree<String> the binarized tree
     */
    public static Tree<String> binarize(Tree<String> tree) {
        return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                if (childrenValues.isEmpty()) {
                    return node;
                }
/*                if (!childrenValues.get(0).getChildren().isEmpty()) {
                    for (Tree<String> child : childrenValues) {
                        child.setLabel(child.getLabel() + "^" + node.getLabel());
                    }
                }
*/                if (childrenValues.size() == 1) {
                    return Tree.create(node.getLabel(), childrenValues);
                }
                if (childrenValues.size() == 2) {
                    return Tree.create(node.getLabel(), childrenValues);
                }
                List<Tree<String>> newChildrenValues = new ArrayList<Tree<String>>();
                newChildrenValues.add(childrenValues.get(0));
                newChildrenValues.add(binarize(childrenValues, 1));
                return Tree.create(node.getLabel(), newChildrenValues);
            }            
        });        
    }

    /**
     * Binarizes a list of trees (as in Mazukaki et al 2005)
     *
     * @param list the list of trees 
     * @return List<Tree<String>> the binarized list
     */
    public static Tree<String> binarize(List<Tree<String>> children, int index) {
        List<Tree<String>> newChildren = new ArrayList<Tree<String>>();
        newChildren.add(children.get(index));
        String label;
        String labelKey = newChildren.get(0).getLabel();
        if (children.size() > index+1) {
            newChildren.add(binarize(children, index+1));
            label = BIN_LABEL_PREFIX;
            labelKey += "/" + newChildren.get(1).getLabel();
        } else {
            label = UN_LABEL_PREFIX;
        }
        if (LABELS != null) {
            if (LABELS.containsKey(labelKey)) {
                label = LABELS.get(labelKey);
            } else {
                label += String.valueOf(LABELS.size()+1);
                LABELS.put(labelKey, label);
            }
        }
        return Tree.create(label, newChildren);
    }
}
