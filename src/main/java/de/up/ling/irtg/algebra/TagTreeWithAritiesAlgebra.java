/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import de.up.ling.tree.TreeVisitor;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extends the TagTreeAlgebra with arity annotation.
 * 
 * This similar is to {@link  TreeWithAritiesAlgebra} but using the tag operations.
 * 
 * Warning: this class is written to work with Grammar 35 in Alto Lab, and is
 * not annotating constants with _0.
 * @author Jonas
 */
public class TagTreeWithAritiesAlgebra extends TagTreeAlgebra {
    
    private static final Pattern ARITY_STRIPPING_PATTERN = Pattern.compile("(.+)_(\\d+)");
    private boolean permissive = true;

    @Override
    public Tree<String> evaluate(Tree<String> t) {
        return super.evaluate(stripArities(t));
    }

    @Override
    public TreeAutomaton decompose(Tree<String> value) {
        return super.decompose(addArities(value));
    }

    @Override
    public Tree<String> parseString(String representation) throws ParserException {
        try {
            Tree<String> ret = TreeParser.parse(representation);
            signature.addAllSymbols(addArities(ret));
            return ret;
        } catch (de.up.ling.tree.ParseException ex) {
            throw new ParserException(ex);
        }
    }    
    
    /**
     * Decorates the nodes in a tree with arities.
     * 
     * @param tree
     * @return 
     */
    public static Tree<String> addArities(Tree<String> tree) {
        return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                String label = node.getLabel();
                
                if( ! HomomorphismSymbol.isVariableSymbol(label) && !childrenValues.isEmpty()) {
                    label = label + "_" + childrenValues.size();
                }
                
                return Tree.create(label, childrenValues);
            }           
        });
    }
    
    protected Tree<String> stripArities(Tree<String> tree) {
        return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                Matcher m = ARITY_STRIPPING_PATTERN.matcher(node.getLabel());
                
                if( m.matches() ) {
                    int arity = Integer.parseInt(m.group(2));
                    
                    if( permissive || arity == childrenValues.size() ) {
                        return Tree.create(m.group(1), childrenValues);
                    } else {
                        String msg = String.format("Node with label '%s' should have %d children, but has %d: %s", node.getLabel(), arity, childrenValues.size(), childrenValues.toString());
                        throw new IllegalArgumentException(msg);
                    }
                } else {
                    return Tree.create(node.getLabel(), childrenValues);
                    //throw new IllegalArgumentException("Node label " + node.getLabel() + " is not of the form label_arity");
                }
            }           
        });
    }

    /**
     * Permissiveness controls whether matching between the arity annotation and the actual arities is
     * enforced, this method can be used to check the current setting for this parameter.
     * 
     * @return 
     */
    public boolean isPermissive() {
        return permissive;
    }

    /**
     * Permissiveness controls whether matching between the arity annotation and the actual arities is
     * enforced, this method can be used to set this parameter.
     * 
     * @param permissive 
     */
    public void setPermissive(boolean permissive) {
        this.permissive = permissive;
    }
    
    /*
    // this should be moved into a test case; for now I commented it out - christoph
    public static void main(String[] args) throws IOException, ParserException {
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.fromPath("examples/grammar_35.irtg");
        Object input = irtg.getInterpretation("string").getAlgebra().parseString("There is no asbestos in our products now . ''");
        InterpretedTreeAutomaton filtered = irtg.filterBinarizedForAppearingConstants("string", input);
        irtg = irtg.filterForAppearingConstants("string", input);
        //System.err.println(filtered);
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("string", input);
        System.err.println(irtg.parseInputObjects(inputs).viterbi());
        System.err.println(filtered.parseInputObjects(inputs).viterbi());
    }*/
    
}
