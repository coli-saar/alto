/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec.tag;

/**
 *
 * @author koller
 */
public enum NodeType {
    DEFAULT(""), SUBSTITUTION("!"), FOOT("*"), HEAD("<>"), SECONDARY_LEX("[]");
    
    public String mark(String x) {
        return x + marker;
    }
    
    private final String marker;
    
    private NodeType(String m) {
        this.marker = m;
    }
}
