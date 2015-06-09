
package de.up.ling.irtg.algebra;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.TreeVisitor;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A tree algebra for trees in the format of the Penn Treebank.
 * The domain of this algebra is the set of all unranked trees,
 * i.e. different nodes with the same label may have different
 * numbers of children. 
 * 
 * @author koller
 * @author Danilo Baumgarten
 */
public class PtbTreeAlgebra extends TreeAlgebra {

    private static final Logger log = Logger.getLogger(PtbTreeAlgebra.class.getName());
    private static final String START_SEQUENCE = "( ";
    private static final Pattern STRIP_PATTERN = Pattern.compile("([^-=]+)([-=])(.+)");
    private static final Pattern LABELFX_PATTERN = Pattern.compile("(\\D+)(\\d+)(.*)");
    private static final Pattern CONCAT_PATTERN = Pattern.compile("(.+)(\\^)(.+)");
    public static final String LABEL_PREFIX = "ART-";

    private int numWords;
    private boolean useParentAnnotation;
    
    /**
     * Constructor
     * 
     * @param useParentAnnotation if true non-terminal symbols
     * get their parent symbol appended
     */
    public PtbTreeAlgebra() {
        this(true);
    }
    
    /**
     * Constructor
     * 
     * @param useParentAnnotation if true non-terminal symbols
     * get their parent symbol appended
     */
    public PtbTreeAlgebra(boolean useParentAnnotation) {
        this.useParentAnnotation = useParentAnnotation;
    }
    
    /**
     * Getter for LABEL_PREFIX
     * 
     * @return String the label prefix
     */
    public static String getArtificialLabelPrefix() {
        return LABEL_PREFIX;
    }

    /**
     * Returns the number of words/token from last parsing
     *
     * @return int number of words/token
     */
    public int getNumWords() {
        return numWords;
    }

    /**
     * Evaluates the given tree
     * i.e. removes the effects of the transformation process (artificial nodes, relabeling, ...)
     * 
     * @param tree the tree, e.g., generated from chart
     * @return the evaluated tree
     */
    @Override
    public Tree<String> evaluate(final Tree<String> tree) {
        return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                List<Tree<String>> newChildValues = new ArrayList<Tree<String>>();
                String labelAsString = node.getLabel();
                
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
                    Matcher match = LABELFX_PATTERN.matcher(labelAsString);
                    label = match.matches() ? match.group(1) : labelAsString;
                } else {
                    label = labelAsString;
                }

                return Tree.create(label,newChildValues);
            }            
        });        
    }

    /**
     * Returns an automaton for the tree
     * In this special case it applies binarization an relabeling first to match
     * the tree with rules in the grammar
     * 
     * @param value the tree
     * @return the automaton for the binarized and relabeled
     * tree <tt>value</tt>
     */
    @Override
    public TreeAutomaton decompose(final Tree<String> value) {
        // the binarization is only for internal handling
        // ptb-like trees therefore must be binarized before further processing
        Tree<String> decomposable = binarizeAndRelabel(value);
//        decomposable.draw();

        return super.decompose(decomposable);
    }

    /**
     * Parses PTB-formatted string
     *
     * @param representation the string containing the data
     * @return the parsed PTB-tree
     * @throws ParserException if an error occurs on parsing the input
     */
    @Override
    public Tree<String> parseString(final String representation) throws ParserException {
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
            try {
                ret = TreeParser.parse(representation);
            } catch (ParseException ex) {
                throw new ParserException(ex);
            }
        }
        
        signature.addAllSymbols(ret);
        
        return ret;
    }

    /**
     * Parses PTB-formatted trees from reader
     *
     * @param reader the reader containing the data
     * @return the parsed PTB-tree
     * @throws IOException if an error occurs on reading chars from <tt>reader</tt>
     */
    public Tree<String> parseFromReader(final Reader reader) throws IOException {
        numWords = 0;
        String input = "";

        // first we're looking for the beginning of a tree
        // in mrg-files the first symbol starts after two left round brackets
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
     * @returnsthe parsed PTB-tree
     * @throws IOException if an error occurs on reading chars from <tt>reader</tt>
     */
    private String stripPosTag(StringBuffer buf) {
        String s = buf.toString();

        // remove trace indices
        Matcher matcher = STRIP_PATTERN.matcher(s);
        s = matcher.matches() ? matcher.group(1) : s;

        // clear the buffer
        buf = new StringBuffer();

        return s;
    }
    
    /**
     * Parses PTB-formatted trees from reader
     *
     * @param reader the reader containing the data
     * @returns the parsed PTB-tree
     * @throws IOException if an error occurs on reading chars from <tt>reader</tt>
     */
    private Tree<String> parseTree(final Reader reader) throws IOException {
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

                // if there's only one child and it has the same symbol return that child
                // this happens when there were removed null elements
                if (children.size() == 1) {
                    Tree<String> child = children.get(0);
                    if (!child.getChildren().isEmpty() && child.getLabel().equals(label)) {
                        return child;
                    }
                }

                return Tree.create(label, children);

            } else if (c == ' ') { // delimiter for symbols (in almost all cases: "(<NONTERMINAL> <TERMINAL>)")

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
    private void skipElement(final Reader reader) throws IOException {
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
     * @return the binarized tree
     */
    public Tree<String> binarizeAndRelabel(final Tree<String> tree) {
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
                if (useParentAnnotation && !childrenValues.get(0).getChildren().isEmpty()) {
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
     * @param children the list of children trees 
     * @param index the current position in the list <tt>children</tt>
     * @return the binarized list
     */
    public Tree<String> binarize(final List<Tree<String>> children, final int index) {
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
     * @return  the concatenated labels
     */
    public String concatLabels(final List<Tree<String>> trees, final int index) {
        StringBuilder ret = new StringBuilder();


        if (useParentAnnotation) {
            // split the first label at the parent annotation marker
            Matcher match = CONCAT_PATTERN.matcher(trees.get(index).getLabel());
            match.matches();

            // add only the stripped label without parent annotation
            ret.append(match.group(1));

            // and keep the parent label for later use
            String suffix = match.group(3);

            // proceed with remaining elements
            for (int i = index+1; i < trees.size(); i++) {
                ret.append("-");
                match = CONCAT_PATTERN.matcher(trees.get(i).getLabel());
                match.matches();
                ret.append(match.group(1));
            }

            // re-add the parent label
            ret.append("^");
            ret.append(suffix);
        } else {
            // add root label
            ret.append(trees.get(index).getLabel());

            // concatenate remaining root labels
            for (int i = index+1; i < trees.size(); i++) {
                ret.append("-");
                ret.append(trees.get(i).getLabel());
            }
        }
       
        return ret.toString();
    }
}
