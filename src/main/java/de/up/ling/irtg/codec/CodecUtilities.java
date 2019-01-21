/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.maxent.FeatureFunction;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;

/**
 * Shared methods that are useful in implementing codecs.
 * 
 * @author koller
 */
public class CodecUtilities {
    private int gensymNext = 1;
    
    /**
     * Removes the first and last character from a string.
     * 
     * @param s
     * @return 
     */
    public static String stripOuterChars(String s) {
        assert s.length() >= 2 : "string -" + s + "- should have length at least 2";
        return s.substring(1, s.length() - 1);
    }
    
    /**
     * Extracts a name from a given ANTLR rule context.
     * If the name is surrounded by quotes, the quotes are stripped off.
     * Otherwise, the name is returned verbatim.
     * 
     * @param context
     * @param isQuoted
     * @return 
     */
    public static String extractName(RuleContext context, boolean isQuoted) {
        if (isQuoted) {
            String s = context.getText();
            return stripOuterChars(s);
        } else {
            return context.getText();
        }
    }
    /**
     * Normalizes a list of strings by introducing new anonymous states
     * where necessary. For each element of "children", the method checks
     * whether the element is a member of the "states" set. If yes, the element
     * is added to the returned state list. Otherwise, a new anonymous state
     * is added to the automaton "auto", along with a rule that rewrites this state
     * into the element string, and the anonymous state is added to the returned
     * state list.
     * 
     * @param auto
     * @param children
     * @param states
     * @return 
     */
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
    
    
    /**
     * Generate a new string with the given prefix. The string is guaranteed
     * to be different form all other gensym'ed strings that came from the
     * same instance of CodecUtilities.
     * @param prefix
     * @return 
     */
    public String gensym(String prefix) {
        return prefix + (gensymNext++);
    }
    
    /**
     * Extracts a list of values from an ANTLR context representing a list.
     * The method first applies the "extractList" function to the given
     * "context", and then applies the "map" function to each value in
     * this list. The method returns the list of values the "map" function
     * returned.
     * 
     * @param <I>
     * @param <C>
     * @param <O>
     * @param context
     * @param extractList
     * @param map
     * @return 
     */
    public static <I,C extends ParserRuleContext, O> List<O> processList(C context, Function<C,List<I>> extractList, Function<I,O> map) {
        List<O> ret = new ArrayList<>();
        
        if( context != null ) {
            for( I x : extractList.apply(context)) {
                ret.add(map.apply(x));
            }
        }
        
        return ret;
    }
    
    /**
     * Extracts a weight from a string specifying the weight.
     * If "weight" is non-null, the method applies the "exactStr" function
     * to extract a string from the "weight" object (e.g., an ANTLR
     * context), strips off the first and last character, and returns
     * the double value represented by the remaining string. Otherwise,
     * the method returns 1.
     * 
     * @param <C>
     * @param weight
     * @param extractStr
     * @return 
     */
    public static <C extends ParserRuleContext> double weight(C weight, Function<C,String> extractStr) {
        if( weight == null ) {
            return 1;
        } else {
            return Double.parseDouble(CodecUtilities.stripOuterChars(extractStr.apply(weight)));
        }
    }
    
    /**
     * Extracts a tree of values from an ANTLR context. The method assumes that
     * the class C of contexts has a tree structure; and that the "children" function
     * extracts the children of a context (as a list of C's) and the "label" function
     * extracts the node label for the context. These methods are then applied recursively
     * to produce a Tree representation of the context.
     * 
     * @param <O>
     * @param <C>
     * @param context
     * @param label
     * @param children
     * @return 
     */
    public static <O, C extends ParserRuleContext> Tree<O> processTree(C context, Function<C,O> label, Function<C, List<C>> children) {
        List<C> cChildren = children.apply(context);
        List<Tree<O>> retChildren = Collections.EMPTY_LIST;
        
        if( cChildren != null ) {
            retChildren = Util.mapToList(children.apply(context), x -> processTree(x, label, children));
        }
         
        return Tree.create(label.apply(context), retChildren);
    }
    
    /**
     * Retrieves a constructor for a {@link FeatureFunction}. The "className"
     * argument specifies the fully qualified name of a subclass of FeatureFunction.
     * The method then looks for a constructor for this class with "n" parameters
     * of type String.
     * 
     * @param className
     * @param n
     * @return
     * @throws ClassNotFoundException no subclass of FeatureFunction could be found
     * @throws NoSuchMethodException  no constructor with n String parameters could be found
     */
    public static Constructor<FeatureFunction> findFeatureConstructor(String className, int n) throws ClassNotFoundException, NoSuchMethodException {
        Class<FeatureFunction> cl = (Class<FeatureFunction>) Class.forName(className);

        Class[] args = new Class[n];
        Arrays.fill(args, String.class);

        Constructor<FeatureFunction> con = cl.getConstructor(args);
        return con;
    }
    
    /**
     * Retrieves a static factory method by name and number of arguments.
     * The method looks for a static method className#methodName with
     * n parameters of type String, and returns it.
     * 
     * @param className
     * @param methodName
     * @param n
     * @return
     * @throws ClassNotFoundException the class could not be found
     * @throws NoSuchMethodException  no method with this name of parameter list could be found
     */
    public static Method findStaticFeatureFactory(String className, String methodName, int n) throws ClassNotFoundException, NoSuchMethodException {
        Class cl = Class.forName(className);
        
        Class[] args = new Class[n];
        Arrays.fill(args, String.class);
        
        return cl.getMethod(methodName, args);
    }

}
