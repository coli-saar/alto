/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import com.google.common.collect.Iterables;
import de.saar.basic.StringOrVariable;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.parser.TermParser;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ParseException;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomatonParser;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomatonParser;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.maxent.FeatureFunction;
import de.up.ling.irtg.maxent.MaximumEntropyIrtg;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
        
/**
 *
 * @author koller
 */
public class TestingTools {

    public static Tree<String> pt(String s) {
        return TreeParser.parse(s);
    }

    public static Tree<Integer> pti(String s, Signature sig) {
        return sig.addAllSymbols(pt(s));
    }

    public static Tree<StringOrVariable> ptv(String s) {
        Term x = TermParser.parse(s);
        return x.toTreeWithVariables();
    }

    // sig = target signature of homomorphism
    public static Tree<HomomorphismSymbol> pth(String s, Signature sig) {
        return HomomorphismSymbol.treeFromNames(pt(s), sig);
    }

    public static TreeAutomaton pa(String s) throws ParseException {
        return TreeAutomatonParser.parse(new StringReader(s));
    }
    
    public static InterpretedTreeAutomaton pi(String s) throws IOException, de.up.ling.irtg.codec.ParseException {
        return new IrtgInputCodec().read(s);
    }
    
    public static InterpretedTreeAutomaton pi(InputStream r) throws IOException, de.up.ling.irtg.codec.ParseException {
        return InterpretedTreeAutomaton.read(r);
    }

    public static Homomorphism hom(Map<String, String> mappings, Signature sourceSignature) {
        return hom(mappings, sourceSignature, new Signature());
    }

    public static Homomorphism hom(Map<String, String> mappings, Signature sourceSignature, Signature targetSignature) {
        Homomorphism ret = new Homomorphism(sourceSignature, targetSignature);

        for (String sym : mappings.keySet()) {
            ret.add(sourceSignature.getIdForSymbol(sym), pth(mappings.get(sym), ret.getTargetSignature()));
        }

        return ret;
    }

    public static Homomorphism hom(Map<String, String> mappings) {
        return hom(mappings, new Signature());
    }

    public static Signature sig(Map<String, Integer> symbols) {
        Signature ret = new Signature();

        for (String sym : symbols.keySet()) {
            ret.addSymbol(sym, symbols.get(sym));
        }

        return ret;
    }

    public static void assertAlmostEquals(double x, double y) {
        assert Math.abs(x - y) < 0.0001 : ("expected " + x + ", got " + y);
    }

    public static Rule rule(String parent, String label, List<String> children, TreeAutomaton automaton) {
        return automaton.createRule(parent, label, children, 1);
    }
    
    public static Set<Rule> rbu(String label, List children, TreeAutomaton auto) {
        int[] childStates = new int[children.size()];
        
        for( int i = 0; i < children.size(); i++ ) {
            childStates[i] = auto.getIdForState(children.get(i));
        }
        
        Set<Rule> ret = new HashSet<Rule>();
        Iterables.addAll(ret, auto.getRulesBottomUp(auto.getSignature().getIdForSymbol(label), childStates));
        
        return ret;
    }
    
    // *** Methodes for Condensed Tree Automatons
    
    public static CondensedTreeAutomaton pac(String s) throws ParseException, de.up.ling.irtg.automata.condensed.ParseException {
        return CondensedTreeAutomatonParser.parse(new StringReader(s));
    }
    
    public static FeatureFunction makeTestFeature(final String x) {
        return new FeatureFunction<String>() {
            public double evaluate(Rule rule, TreeAutomaton<String> automaton, MaximumEntropyIrtg irtg) {
                return Double.parseDouble(x);
            }
        };
    }
}
