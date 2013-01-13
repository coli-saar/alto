package de.up.ling.irtg.algebra;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.AnnotatedCorpus;
import de.up.ling.tree.TreeVisitor;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author koller
 * @author Danilo Baumgarten
 */
public class PtbTreeAlgebra extends TreeAlgebra {
    private static final Logger log = Logger.getLogger(PtbTreeAlgebra.class.getName());
    private static final String START_SEQUENCE = "( ";
    private static Pattern TRACE_PATTERN = Pattern.compile("([^-=]+)([-=])(.+)");
    private static Pattern PARENT_PATTERN = Pattern.compile("(\\D+)(\\d+)(.*)");
    public static final String LABEL_PREFIX = "ART-";
    private int numWords;

    /**
     * For testing only
     */
    public static void main(String[] args) throws ParserException {
        PtbTreeAlgebra pta = new PtbTreeAlgebra();
        Tree<String> tree = pta.parseString("( (`` ``) (INTJ (UH Yes) (. .) ))");
        PtbTreeAlgebra.log.log(Level.INFO, tree.toString());
        tree = binarizeAndRelabel(tree);
        PtbTreeAlgebra.log.log(Level.INFO, tree.toString());
    }

    /**
     * Evaluates the given tree
     * i.e. removes the effects of the transformation process (artificial nodes, relabeling, ...)
     * 
     * @param tree the tree, e.g., generated from chart
     * @return Tree<String> the evaluated tree
     */
    @Override
    public Tree<String> evaluate(Tree<String> tree) {
        tree.draw();
        return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                List<Tree<String>> newChildValues = new ArrayList<Tree<String>>();
                // remove the effects of the binarization
                // i.e. replace artificial nodes with their children
                for (int i = 0; i < childrenValues.size(); i++) {
                    Tree<String> child = childrenValues.get(i);
                    if (child.getLabel().startsWith(LABEL_PREFIX)) {
                        newChildValues.addAll(child.getChildren());
                    } else {
                        newChildValues.add(child);
                    }
                }
                
                // remove effects of relabeling
                // i.e. the parent annotation and the appended size of children
                String label;
                if (!childrenValues.isEmpty()) {
                    Matcher match = PARENT_PATTERN.matcher(node.getLabel());
                    label = match.matches() ? match.group(1) : node.getLabel();
                } else {
                    label = node.getLabel();
                }
                return Tree.create(label,newChildValues);
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
     * Reads an annotated corpus following the condition of only one
     * interpretation, i.e. with StringAlgebra
     *
     * @param reader the reader, e.g. file or string reader, containing the data
     * @returns AnnotatedCorpus an instance of the corpus class with all the parsed data
     * @throws IOException if an error occurs on reading data from <tt>reader</tt>
     */
    public static AnnotatedCorpus readPtbCorpus(Reader reader) throws IOException {
        AnnotatedCorpus ret = new AnnotatedCorpus();
        BufferedReader br = new BufferedReader(reader);
        StringAlgebra strAlgebra = new StringAlgebra();
        Map<String, Object> currentInputs = new HashMap<String, Object>();
        String interpName = new String();
        boolean interpretation = true;
        boolean firstLine = true;

        while (true) {
            // read a line from the input
            String line = br.readLine();

            // return the corpus when reaching EOF
            if (line == null) {
                return ret;
            }

            // skip empty lines
            if (line.equals("")) {
                continue;
            }

            // the first line contains the name of the interpretation
            if (firstLine) {
                interpName = line;
                firstLine = false;
            } else {
                // alternate between reading the representation and the tree
                if (interpretation) {
                    // let StringAlgebra parse the representation
                    Object inputObject = strAlgebra.parseString(line);
                    // and map the interpretation name to the parsed iput
                    currentInputs.put(interpName, inputObject);
                    interpretation = false;
                } else {
                    // parse the tree
                    Tree<String> tree = TreeParser.parse(line);
                    // create a corpus instance
                    AnnotatedCorpus.Instance inst = new AnnotatedCorpus.Instance(tree, currentInputs);
                    // and add the instance to the corpus
                    ret.getInstances().add(inst);
                    currentInputs = new HashMap<String, Object>();
                    
                    interpretation = true;
                }
            }
        }
    }

    @Override
    public TreeAutomaton decompose(Tree<String> value) {
        // the binarization is only for internal handling
        // ptb-like trees therefore must be binarized before further processing
        value = binarizeAndRelabel(value);
//        value.draw();
        return super.decompose(value);
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
        Tree<String> ret;
        
        // first try to the actual parser
        try {
            StringReader reader = new StringReader(representation);
            ret = parseFromReader(reader);
        } catch (IOException e) {
            throw new ParserException(e);
        }
        
        // if there's neither an exception nor a tree,
        // the input is possibly a tree as a string
        if (ret == null) {
            ret = TreeParser.parse(representation);
        }
        
        return ret;
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
        // in mrg-files the first symbol starts after two left round brackets
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
        s = matcher.matches() ? matcher.group(1) : s;
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
                        children.add(Tree.create(buffer.toString().toLowerCase()));
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
        log.log(Level.SEVERE, "Unexpected end of parsed input.");
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
     * Binarizes a PTB tree (as in Matsukaki et al.,2005)
     * and re-labels the nodes
     *
     * @param tree the PTB tree
     * @return Tree<String> the binarized tree
     */
    public static Tree<String> binarizeAndRelabel(Tree<String> tree) {
        return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                // leaf node: nothing to do
                if (childrenValues.isEmpty()) {
                    return node;
                }

                // Re-Labeling:
                // 1. strip symbols to the main POS and add the #children due to the constraint
                //    every rule with the same parent state must have the same arity
                String label = node.getLabel() + String.valueOf(childrenValues.size());
                // 2. parent annotation (as in Johnson, 1998)
                if (!childrenValues.get(0).getChildren().isEmpty()) {
                    for (Tree<String> child : childrenValues) {
                        child.setLabel(child.getLabel() + "^" + label);
                    }
                }
                
                // unary nodes need no binarization
                if (childrenValues.size() == 1) {
                    return Tree.create(label, childrenValues);
                }
                // already binary nodes need no binarization
                if (childrenValues.size() == 2) {
                    return Tree.create(label, childrenValues);
                }
                // binarize nodes with children > 2
                List<Tree<String>> newChildrenValues = new ArrayList<Tree<String>>();
                newChildrenValues.add(childrenValues.get(0));
                newChildrenValues.add(binarize(childrenValues, 1));
                
                return Tree.create(label, newChildrenValues);
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
        // add the first child to the list
        newChildren.add(children.get(index));
        // create an artificial label
        String label = LABEL_PREFIX + concatLabels(children, index);
        
        if (children.size() > index+1) {
            // binarize remaining children and add the resulting tree to the list
            newChildren.add(binarize(children, index+1));
        }
        
        return Tree.create(label, newChildren);
    }

    /**
     * Concatenates all root-labels of a given list of trees from an index on
     *
     * @param labels the list of trees 
     * @param index the from where on the root labels will be concatenated
     * @return String the concatenated labels
     */
    public static String concatLabels(List<Tree<String>> labels, int index) {
        Pattern parentPattern = Pattern.compile("(.+)(\\^)(.+)");
        StringBuilder ret = new StringBuilder();
        Matcher match = parentPattern.matcher(labels.get(index).getLabel());
        match.matches();
        // add only the stripped label without parent annotation
        ret.append(match.group(1));
        // and keep the parent label for later use
        String suffix = match.group(3);
        
        // proceed with remaining elements
        for (int i = index+1; i < labels.size(); i++) {
            ret.append("-");
            match = parentPattern.matcher(labels.get(i).getLabel());
            match.matches();
            ret.append(match.group(1));
        }
        
        // re-add the parent label
        ret.append("^");
        ret.append(suffix);
        
        return ret.toString();
    }

    /**
     * Binarizes a list of trees (as in Mazukaki et al 2005)
     *
     * @param list the list of trees 
     * @return List<Tree<String>> the binarized list
     */
/*    public static Tree<String> binarize(List<Tree<String>> children, int index) {
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
    }*/
}
