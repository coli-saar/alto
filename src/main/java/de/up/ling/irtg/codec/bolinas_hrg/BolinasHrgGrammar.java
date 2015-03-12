/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec.bolinas_hrg;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author koller
 */
public class BolinasHrgGrammar {
    private List<Rule> rules;

    public BolinasHrgGrammar() {
        rules = new ArrayList<>();
    }

    public List<Rule> getRules() {
        return rules;
    }
    
    void addRule(Rule rule) {
        rules.add(rule);
    }
}
