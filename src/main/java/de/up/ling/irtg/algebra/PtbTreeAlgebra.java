/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.tree.Tree;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author koller
 */
public class PtbTreeAlgebra extends TreeAlgebra {
    private static final String START_SEQUENCE = "( ";

    public static void main(String[] args) throws ParserException {
        PtbTreeAlgebra pta = new PtbTreeAlgebra();
        Tree<String> tree = pta.parseString("( (`` ``) (INTJ (UH Yes) (. .) ))");
        System.err.println(tree.toString());
    }
    @Override
    public Tree<String> evaluate(Tree<String> t) {
        return super.evaluate(t);
    }    
    @Override
    public Tree<String> parseString(String representation) throws ParserException {
        try {
            StringReader reader = new StringReader(representation);
            return parseFromReader(reader);
        } catch (IOException e) {
            throw new ParserException(e);
        }
    }
    
    public Tree<String> parseFromReader(Reader reader) throws IOException, ParserException {
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
        } while (!input.equals(START_SEQUENCE));
        Tree<String> tree = parseTree(reader);
        List<Tree<String>> children = tree.getChildren();
        if (children.isEmpty()) {
            return null;
        }
        if (children.size() > 1) {
            for (int i = 0; i < children.size(); i++) {
                // a tree containing only a terminal symbol has only one child (the one with the terminal)
                // therefore we're looking for trees with more children or a higher depth
                List<Tree<String>> grandchildren = children.get(i).getChildren();
                if ((grandchildren.size() > 1) || !grandchildren.get(0).getChildren().isEmpty()) {
                    tree = children.get(i);
                    for (int j = 0; j < i; j++) {
                        Tree<String> subTree = children.get(j);
                        tree.getChildren().add(j, subTree);
                    }
                    for (int j = i+1; j < children.size(); j++) {
                        Tree<String> subTree = children.get(j);
                        tree.getChildren().add(subTree);
                    }
                    break;
                }
            }
        } else {
            tree = children.get(0);
        }
        return tree;
    }
    
    private Tree<String> parseTree(Reader reader) throws IOException {
        String label = "";
        String object = "";
        List<Tree<String>> children = new ArrayList<Tree<String>>();
        
        for (int c; (c = reader.read()) != -1;) {

            if (c == '(') {
                Tree<String> subTree = parseTree(reader);
                if (subTree != null) {
                    children.add(subTree);
                }
            } else if (c == ')') {
                if (!object.isEmpty()) {
                    if (label.isEmpty()) {
                        label = object;
                    } else {
                        children.add(Tree.create(object));
                    }
                }
                label += String.valueOf(children.size());
                return (children.isEmpty()) ? null : Tree.create(label, children);
            } else if (c == ' ') {
                if (!object.isEmpty() && label.isEmpty()) {
                    label = object.replaceAll("(-\\d)|(=\\d)", "");
                    object = "";
                }
            } else {
                if((c == '-') && object.isEmpty() && label.isEmpty()) {
                    skipNullElement(reader);
                    return null;
                } else if(c == '[') { // char for [
                    skipNullElement(reader);
                    return null;
                } else if (c > 10) {
                    object += (char) c;
                }
            }
        }

        System.err.println("Unexpected end of parsed input.");
        return null;
    }
    
    private void skipNullElement(Reader reader) throws IOException {
        for (int c; (c = reader.read()) != -1;) {
            if (c == ')') {
                return;
            }
        }
    }
}
