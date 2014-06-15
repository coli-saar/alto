/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec;

import static de.up.ling.irtg.algebra.TagTreeAlgebra.C;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.antlr.v4.runtime.ParserRuleContext;
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
    
    public static <I,C extends ParserRuleContext, O> List<O> processList(C context, Function<C,List<I>> extractList, Function<I,O> map) {
        List<O> ret = new ArrayList<>();
        
        if( context != null ) {
            for( I x : extractList.apply(context)) {
                ret.add(map.apply(x));
            }
        }
        
        return ret;
    }
    
    public static <C extends ParserRuleContext> double weight(C weight, Function<C,String> extractStr) {
        if( weight == null ) {
            return 1;
        } else {
            return Double.parseDouble(CodecUtilities.stripOuterChars(extractStr.apply(weight)));
        }
    }
    
    public static <O, C extends ParserRuleContext> Tree<O> processTree(C context, Function<C,O> label, Function<C, List<C>> children) {
        List<C> cChildren = children.apply(context);
        List<Tree<O>> retChildren = Collections.EMPTY_LIST;
        
        if( cChildren != null ) {
            retChildren = Util.mapList(children.apply(context), x -> processTree(x, label, children));
        }
         
        return Tree.create(label.apply(context), retChildren);
    }
}
