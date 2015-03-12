/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec.bolinas_hrg;

import java.util.List;

/**
 *
 * @author koller
 */
public class NonterminalWithHyperedge {
    private String nonterminal;
    private List<String> endpoints;

    public NonterminalWithHyperedge(String nonterminal, List<String> endpoints) {
        this.nonterminal = nonterminal;
        this.endpoints = endpoints;
    }

    public String getNonterminal() {
        return nonterminal;
    }

    void setNonterminal(String nonterminal) {
        this.nonterminal = nonterminal;
    }

    public List<String> getEndpoints() {
        return endpoints;
    }

    void setEndpoints(List<String> endpoints) {
        this.endpoints = endpoints;
    }

    @Override
    public String toString() {
        return nonterminal + endpoints;
    }
}
