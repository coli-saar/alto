/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph.ordered;

import com.google.common.collect.Sets;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.SemEvalDependencyFormat;
import it.unimi.dsi.fastutil.Function;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author christoph_teichmann
 */
public class OrderedDependencyGraphAlgebra extends Algebra<OrderedDependencyGraph> {

    /**
     *
     */
    private final SemEvalDependencyFormat sedf = new SemEvalDependencyFormat();

    /**
     *
     */
    private final Function<GraphNode, String> sourceNamer;

    /**
     *
     * @param sourceNamer
     */
    public OrderedDependencyGraphAlgebra(Function<GraphNode, String> sourceNamer) {
        this.getSignature().addSymbol(GraphAlgebra.OP_MERGE, 2);

        this.sourceNamer = sourceNamer;
    }

    @Override
    protected OrderedDependencyGraph evaluate(String label, List<OrderedDependencyGraph> childrenValues) {
        if (label.equals(GraphAlgebra.OP_MERGE)) {
            if (childrenValues.size() != 2) {
                return null;
            }

            return childrenValues.get(0).merge(childrenValues.get(1));
        } else if (label.startsWith(GraphAlgebra.OP_FORGET)) {
            if (childrenValues.size() != 1) {
                return null;
            }

            String[] parts = label.split("_");

            if (parts.length < 2) {
                return null;
            }

            OrderedDependencyGraph odg = childrenValues.get(0);
            Set<String> removed = new HashSet<>();

            this.getSignature().addSymbol(label, 1);
            for (int i = 1; i < parts.length; ++i) {
                removed.add(parts[i]);
            }

            return odg.forgetSourcesExcept(Sets.difference(odg.getAllSources(), removed));
        } else if (label.startsWith(GraphAlgebra.OP_RENAME)) {
            if (childrenValues.size() != 1) {
                return null;
            }

            String[] parts = label.split("_");

            if (parts.length != 3) {
                return null;
            }

            OrderedDependencyGraph odg = childrenValues.get(0);

            this.getSignature().addSymbol(label, 1);
            return odg.renameSource(parts[1], parts[2]);
        } else if (label.startsWith(GraphAlgebra.OP_SWAP)) {
            if (childrenValues.size() != 1) {
                return null;
            }

            String[] parts = label.split("_");

            if (parts.length != 3) {
                return null;
            }

            OrderedDependencyGraph odg = childrenValues.get(0);

            this.getSignature().addSymbol(label, 1);
            return odg.renameSource(parts[1], parts[2]);
        } else {
            if (!childrenValues.isEmpty()) {
                return null;
            }

            OrderedDependencyGraph odg = new OrderedDependencyGraph();

            String[] parts = label.split("\\s+");

            String[] combo = makeNode(parts, 0);
            GraphNode node1 = odg.addNode(combo[0], combo[2]);
            if (combo[1] == null) {
                odg.addSource(combo[1], combo[0]);
            }

            if (parts.length > 1) {
                if (parts.length != 3) {
                    return null;
                }

                combo = makeNode(parts, 2);
                GraphNode node2 = odg.addNode(combo[0], combo[2]);

                String edgeLabel = parts[1];
                if (edgeLabel.startsWith(":")) {
                    edgeLabel = edgeLabel.substring(1);
                }

                odg.addEdge(node1, node2, edgeLabel);
            }

            this.signature.addSymbol(label, 0);
            return odg;
        }
    }

    /**
     *
     * @param parts
     * @return
     */
    private String[] makeNode(String[] parts, int pos) {
        String[] n1 = parts[pos].split("<");
        String nodeName;
        if (n1.length == 1) {
            n1 = n1[0].split("/");
        }
        nodeName = n1[0];
        String source = null;
        if (n1.length > 1) {
            n1 = n1[1].split(">?/");

            source = n1[0];
            if (source.endsWith(">")) {
                source = source.substring(0, source.length() - 1);
            }
        }
        String nodeLabel = null;
        if (n1.length > 1) {
            nodeLabel = n1[1];
        }
        String[] combo = new String[]{nodeName, source, nodeLabel};
        return combo;
    }

    @Override
    public OrderedDependencyGraph parseString(String representation) throws ParserException {
        //TODO

        return null;
    }

    @Override
    public TreeAutomaton decompose(OrderedDependencyGraph value) {
        return super.decompose(value); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
    
    
}
