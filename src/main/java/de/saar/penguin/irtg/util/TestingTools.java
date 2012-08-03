/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.util;

import de.saar.basic.StringOrVariable;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.parser.TermParser;
import de.saar.penguin.irtg.hom.Homomorphism;
import de.saar.penguin.irtg.signature.MapSignature;
import de.saar.penguin.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.util.Map;

/**
 *
 * @author koller
 */
public class TestingTools {
    public static Tree<String> pt(String s) {
        return TreeParser.parse(s);
    }
    
    public static Tree<StringOrVariable> ptv(String s) {
        Term x = TermParser.parse(s);
        return x.toTreeWithVariables();
    }
    
    public static Homomorphism hom(Map<String,String> mappings, Signature sourceSignature) {
        Homomorphism ret = new Homomorphism(sourceSignature, new MapSignature());
        
        for( String sym : mappings.keySet() ) {
            ret.add(sym, ptv(mappings.get(sym)));
        }
        
        return ret;
    }
    
    public static Signature sig(Map<String,Integer> symbols) {
        MapSignature ret = new MapSignature();
        
        for( String sym : symbols.keySet() ) {
            ret.addSymbol(sym, symbols.get(sym));
        }
        
        return ret;
    }
}
