/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec;

import de.up.ling.tree.Tree;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author koller
 */
public class PtbTreeInputCodec extends InputCodec<Tree<String>> {
    private static final String START_SEQUENCE = "( ";
    private static final Pattern STRIP_PATTERN = Pattern.compile("([^-=]+)([-=])(.+)");
//    private static final Pattern LABELFX_PATTERN = Pattern.compile("(\\D+)(\\d+)(.*)");
//    private static final Pattern CONCAT_PATTERN = Pattern.compile("(.+)(\\^)(.+)");
    public static final String LABEL_PREFIX = "ART-";
    

    /*
    candidates for codec options:
    - strip grammatical functions
    - make all terminal symbols lowercase
    */
    
    
    @Override
    public Tree read(InputStream is) throws ParseException, IOException {
        Reader reader = new InputStreamReader(is);
        
//        numWords = 0;
        String input = "";

        // first we're looking for the beginning of a tree
        // in mrg-files the first symbol starts after two left round brackets
        do {
            int c = reader.read();
            if (c == -1) {
                // out of input, throw EOF exception
                throw new EOFException("At end of input.");
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
            throw new ParseException("PTB tree has no children.");
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
     * @returns the parsed PTB-tree
     * @throws IOException if an error occurs on reading chars from <tt>reader</tt>
     */
    private Tree<String> parseTree(final Reader reader) throws IOException, ParseException {
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
//                        label = stripPosTag(buffer);
                    } else {
                        // 2. the buffer contains a terminal symbol
                        children.add(Tree.create(buffer.toString()));
//                        numWords++;
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
//                    label = stripPosTag(buffer); 
                    label = buffer.toString();
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
        throw new ParseException("Unexpected end of parsed input.");
    }
    
    /**
     * Strips the grammatical function off a nonterminal symbol.
     * That is, NP-SBJ is converted to NP.
     * 
     * @param buf
     * @return 
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
}
