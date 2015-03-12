/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec.bolinas_hrg;

import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphEdgeFactory;
import de.up.ling.irtg.algebra.graph.GraphNode;
import java.util.ArrayList;
import java.util.List;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;

/**
 *
 * @author koller
 */
public class Rule {
    private NonterminalWithHyperedge lhsNonterminal;
    private DirectedGraph<GraphNode, GraphEdge> rhsGraph;
    private List<NonterminalWithHyperedge> rhsNonterminals;
    private double weight;

    public Rule() {
        rhsGraph = new DefaultDirectedGraph<GraphNode, GraphEdge>(new GraphEdgeFactory());
        rhsNonterminals = new ArrayList<>();
    }

    public NonterminalWithHyperedge getLhsNonterminal() {
        return lhsNonterminal;
    }

    void setLhsNonterminal(NonterminalWithHyperedge lhsNonterminal) {
        this.lhsNonterminal = lhsNonterminal;
    }

    public DirectedGraph<GraphNode, GraphEdge> getRhsGraph() {
        return rhsGraph;
    }

    public List<NonterminalWithHyperedge> getRhsNonterminals() {
        return rhsNonterminals;
    }

    public double getWeight() {
        return weight;
    }

    void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return lhsNonterminal + " -> " + rhsGraph + " " + rhsNonterminals + " {" + weight + "}";
    }
}
