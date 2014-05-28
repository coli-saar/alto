/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.antlr.v4.runtime.RuleContext;

/**
 *
 * @author koller
 */
public class CodecUtilities {
    private int gensymNext = 1;
    
    public static String stripOuterChars(String s) {
        assert s.length() >= 2 : "string -" + s + "- should have length at least 2";
        return s.substring(1, s.length() - 1);
    }
    
    public static String extractName(RuleContext context, boolean isQuoted) {
        if (isQuoted) {
            String s = context.getText();
            return stripOuterChars(s);
        } else {
            return context.getText();
        }
    }
    
    public List<String> introduceAnonymousStates(ConcreteTreeAutomaton<String> auto, List<String> children, Set<String> states) {
        List<String> ret = new ArrayList<>();
        
        for( String s : children ) {
            if( states.contains(s) ) {
                ret.add(s);
            } else {
                String newState = gensym("_q_");
                auto.addRule(auto.createRule(newState, s, new ArrayList<>()));
                ret.add(newState);
            }
        }
            
        return ret;
    }
    
    
    
    public String gensym(String prefix) {
        return prefix + (gensymNext++);
    }
}
