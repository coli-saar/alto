/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.siblingfinder

import org.junit.Test
import static org.junit.Assert.*
import de.up.ling.irtg.automata.ConcreteTreeAutomaton
import de.up.ling.irtg.automata.Rule
import de.up.ling.irtg.hom.Homomorphism
import de.up.ling.irtg.signature.Signature
import de.up.ling.tree.TreeParser
/**
 *
 * @author Jonas
 */
class SiblingFinderInvhomTest {

    
    @Test
    public void testBasicAutomaton() {
    
    
        Signature srcSig = new Signature();
        srcSig.addSymbol("a", 2);
        srcSig.addSymbol("aPrime", 2);
        srcSig.addSymbol("aSingle", 1);
        srcSig.addSymbol("bOne", 0);
        srcSig.addSymbol("bTwo", 0);
        srcSig.addSymbol("bThree", 0);
        
        Signature sig = new Signature();
        sig.addSymbol("one", 0);
        sig.addSymbol("two", 0);
        sig.addSymbol("three", 0);
        sig.addSymbol("conc", 2);
        ConcreteTreeAutomaton<String> auto = new ConcreteTreeAutomaton(sig);
        
        Homomorphism hom = new Homomorphism(srcSig, sig);
        hom.add("a", TreeParser.parse("conc(one, conc(?1, ?2))"));
        hom.add("aPrime", TreeParser.parse("conc(?1, conc(two, ?2))"));
        hom.add("aSingle", TreeParser.parse("conc(one, conc(two, ?1))"));
        hom.add("bThree", TreeParser.parse("three"));
        hom.add("bTwo", TreeParser.parse("two"));
        hom.add("bOne", TreeParser.parse("one"));
        
        auto.addRule(auto.createRule("One", "one", new String[0]));
        auto.addRule(auto.createRule("Two", "two", new String[0]));
        auto.addRule(auto.createRule("Three", "three", new String[0]));
        auto.addRule(auto.createRule("TwoThree", "conc", ["Two", "Three"]));
        auto.addRule(auto.createRule("OneTwoThree", "conc", ["One", "TwoThree"]));
        auto.addFinalState(auto.getIdForState("OneTwoThree"));
        
        
        SiblingFinderInvhom invhom = new SiblingFinderInvhom(auto, hom);
        Iterable<Rule> res = invhom.getConstantBottomUp(srcSig.getIdForSymbol("bOne"));
        assert res.iterator().hasNext();
        res = invhom.getConstantBottomUp(srcSig.getIdForSymbol("bTwo"));
        assert res.iterator().hasNext();
        res = invhom.getConstantBottomUp(srcSig.getIdForSymbol("bThree"));
        assert res.iterator().hasNext();
        res = invhom.getRulesBottomUp(auto.getIdForState("Two"), 0, srcSig.getIdForSymbol("a"));
        assert !res.iterator().hasNext();
        res = invhom.getRulesBottomUp(auto.getIdForState("Three"), 1, srcSig.getIdForSymbol("a"));
        assert res.iterator().hasNext();
        res = invhom.getRulesBottomUp(auto.getIdForState("One"), 0, srcSig.getIdForSymbol("aPrime"));
        assert !res.iterator().hasNext();
        res = invhom.getRulesBottomUp(auto.getIdForState("Three"), 1, srcSig.getIdForSymbol("aPrime"));
        assert res.iterator().hasNext();
        res = invhom.getRulesBottomUp(auto.getIdForState("Three"), 0, srcSig.getIdForSymbol("aSingle"));
        assert res.iterator().hasNext();
        
        
    }
}
