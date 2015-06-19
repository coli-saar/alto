/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec.bolinas_hrg;

import java.util.List;

/**
 * A single nonterminal hyperedge, represented by the
 * nonterminal symbol and its endpoints in the hypergraph.
 * 
 * @author koller
 */
class NonterminalWithHyperedge {
    
    private final String nonterminal;
    
    private List<String> endpoints;

    public NonterminalWithHyperedge(String nonterminal, List<String> endpoints) {
        this.nonterminal = nonterminal;
        this.endpoints = endpoints;
    }

    public String getNonterminal() {
        return nonterminal;
    }

    public List<String> getEndpoints() {
        return endpoints;
    }

    @Override
    public String toString() {
        return nonterminal + endpoints;
    }
}
