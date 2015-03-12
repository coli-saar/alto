/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec.bolinas_hrg;

import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import java.util.List;
import org.jgrapht.DirectedGraph;

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

    void setRhsGraph(DirectedGraph<GraphNode, GraphEdge> rhsGraph) {
        this.rhsGraph = rhsGraph;
    }

    public List<NonterminalWithHyperedge> getRhsNonterminals() {
        return rhsNonterminals;
    }

    void setRhsNonterminals(List<NonterminalWithHyperedge> rhsNonterminals) {
        this.rhsNonterminals = rhsNonterminals;
    }

    public double getWeight() {
        return weight;
    }

    void setWeight(double weight) {
        this.weight = weight;
    }
}
