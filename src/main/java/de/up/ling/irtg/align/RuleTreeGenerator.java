/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.InverseHomAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 *
 * @author christoph
 */
public class RuleTreeGenerator {
    
    /**
     * 
     * @param input1
     * @param input2
     * @param rlm
     * @return 
     */
    public Pair<TreeAutomaton,List<Homomorphism>> makeInverseIntersection(TreeAutomaton input1,
                                                            TreeAutomaton input2, RuleMarker rlm){
        Signature sig1 = input1.getSignature();
        Signature sig2 = input2.getSignature();
        
        List<Homomorphism> homs = new ArrayList<>();
        Signature sig = new Signature();
        homs.add(new Homomorphism(sig, input1.getSignature()));
        homs.add(new Homomorphism(sig,input2.getSignature()));
        
        List<Tree<String>> xs = new ArrayList<>();
        
        this.makeSymbolToSkip(sig1, xs, homs.get(0), homs.get(1), sig);
        this.makeSymbolToSkip(sig2, xs, homs.get(1), homs.get(0), sig);
        
        for(String sym1 : sig1.getSymbols()){
            
            if(rlm.isFrontier(sym1)){
                handleFrontier(rlm, sym1, sig, homs);
            }
            
            int arity1 = sig1.getArityForLabel(sym1);
            
            for(String sym2 : sig2.getSymbols()){
                int arity2 = sig2.getArityForLabel(sym2);
                
                if(arity1 < arity2){
                    makePairings(xs, arity2, sym2, arity1, sym1, sig, homs.get(1), homs.get(2));   
                }else{
                    makePairings(xs, arity1, sym1, arity2, sym2, sig, homs.get(0), homs.get(1));
                }
            }
        }
        
        TreeAutomaton inv1 = new InverseHomAutomaton(input1, homs.get(0));
        TreeAutomaton inv2 = new InverseHomAutomaton(input2, homs.get(1));
        
        return new Pair<>(inv1.intersect(inv2),homs);
    }

    /**
     * 
     * @param xs
     * @param arityLarger
     * @param symLarger
     * @param aritySmaller
     * @param symSmaller
     * @param sig
     * @param homLarger
     * @param homSmaller 
     */
    private void makePairings(List<Tree<String>> xs, int arityLarger, String symLarger,
            int aritySmaller, String symSmaller, Signature sig,
            Homomorphism homLarger, Homomorphism homSmaller) {
        xs.clear();
        for(int i=1;i<=arityLarger;++i){
            xs.add(Tree.create("x_"+i));
        }
        Tree<String> expression1 = Tree.create(symLarger, xs);
        
        if(aritySmaller == 0)
        {
            Tree<String> expression2 = Tree.create(symSmaller);
            String label = makeLabel(sig,arityLarger);
            
            homLarger.add(label, expression1);
            homSmaller.add(label, expression2);
            return;
        }
        
        xs.clear();
        VariableIterator xcomb = new VariableIterator(aritySmaller, arityLarger);
        
        while(xcomb.hasNext()){
            List<String> comb = xcomb.next();
            xs.clear();
            for(int i=1;i<=comb.size();++i){
                xs.add(Tree.create(comb.get(i)));
            }
            Tree<String> expression2 = Tree.create(symSmaller, xs);
            
            String label = makeLabel(sig,arityLarger);
            
            homLarger.add(label, expression1);
            homSmaller.add(label, expression2);
        }
    }

    /**
     * 
     * @param rlm
     * @param sym1
     * @param sig
     * @param homs 
     */
    private void handleFrontier(RuleMarker rlm, String sym1, Signature sig, List<Homomorphism> homs) {
        String sym2 = rlm.getCorresponding(sym1);
        
        Tree<String> t = Tree.create("x_1");
        
        String label = Util.gensym("X");
        sig.addSymbol(label, 1);
        
        homs.get(0).add(label, Tree.create(sym1, t));
        homs.get(1).add(label, Tree.create(sym2, t));
    }

    /**
     * 
     * @param sig
     * @param xs
     * @param hom1
     * @param hom2 
     */
    private void makeSymbolToSkip(Signature sig, List<Tree<String>> xs, Homomorphism hom1,
                                                 Homomorphism hom2, Signature add) {
        for(String sym : sig.getSymbols()){
            int arity1 = sig.getArityForLabel(sym);
            xs.clear();
            for(int i=1;i<=arity1;++i){
                xs.add(Tree.create("x_"+i));
            }
            Tree<String> expression = Tree.create(sym, xs);
            for(int i=0;i<xs.size();++i){
                String label = makeLabel(add,arity1);
                
                hom2.add(label, xs.get(i));
                hom1.add(label, expression);
            }
        }
    }

    /**
     * 
     * @return 
     */
    private String makeLabel(Signature sig, int arity) {
        String s = Util.gensym("l_");
        
        sig.addSymbol(s, arity);
        
        return s;
    }
    
    /**
     * 
     */
    private class VariableIterator implements Iterator<List<String>>{
        /**
         * 
         */
        private boolean first = true;
        
        /**
         * 
         */
        private final List<String> ret = new ArrayList<>();
        
        /**
         * 
         */
        private final List<Set<String>> sources = new ArrayList<>();
        
        /**
         * 
         */
        private final List<Iterator<String>> main = new ArrayList<>();
        
        /**
         * 
         * @param pick
         * @param from 
         */
        private VariableIterator(int pick, int from){
            
            Set<String> insert = new HashSet<>();
            for(int i=1;i<=from;++i){
                insert.add("x_"+i);
            }
            
            sources.add(insert);
            main.add(insert.iterator());
            ret.add(main.get(0).next());
            
            for(int i=1;i<pick;++i){
                insert = new HashSet<>(insert);
                insert.remove(ret.get(i-1));
                sources.add(insert);
                main.add(insert.iterator());
                ret.add(main.get(i).next());
            }
        }

        @Override
        public boolean hasNext() {
            if(first){
                return true;
            }
            
            for(int i=0;i<this.main.size();++i){
                if(this.main.get(i).hasNext()){
                    return true;
                }
            }
            
            return false;
        }

        @Override
        public List<String> next() {
            if(first){
                this.first = false;
                return this.ret;
            }
            
            int cut = this.main.size()-1;
            
            for(;cut >= 0;--cut){
                if(this.main.get(cut).hasNext()){
                   break; 
                }
            }
            if(cut < 0){
                throw new NoSuchElementException();
            }
            
            this.ret.set(cut, main.get(cut).next());
            for(int i=cut+1;i<main.size();++i){
                Set<String> set = new HashSet<>(this.sources.get(i-1));
                set.remove(ret.get(i-1));
                this.sources.set(i, set);
                this.main.set(i, set.iterator());
                this.ret.set(i, this.main.get(i).next());
            }
            
            return this.ret;
        }
    }
}