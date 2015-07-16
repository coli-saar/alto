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
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
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
    public Pair<TreeAutomaton,Pair<Homomorphism,Homomorphism>> makeInverseIntersection(TreeAutomaton input1,
                                                            TreeAutomaton input2, RuleMarker rlm){
        Signature sig1 = input1.getSignature();
        Signature sig2 = input2.getSignature();
        
        Signature sig = new Signature();
        
        Pair<Homomorphism,Homomorphism> homs = new Pair<>(new Homomorphism(sig, input1.getSignature()),
                new Homomorphism(sig,input2.getSignature()));
        
        List<Tree<String>> xs = new ArrayList<>();
        
        String dummy1 = findDummy(sig1);
        String dummy2 = findDummy(sig2);
        
        Set<String> relevant1 = makeRelevant(input1);
        Set<String> relevant2 = makeRelevant(input2);
        
        this.makeSymbolToSkip(relevant1, sig, sig1, xs, homs.getLeft(), homs.getRight(), dummy1, rlm);
        this.makeSymbolToSkip(relevant2, sig, sig2, xs, homs.getRight(), homs.getLeft(), dummy2, rlm);
        
        for(String sym1 : relevant1){           
            if(rlm.isFrontier(sym1)){
                handleFrontier(rlm, sym1, sig, homs.getLeft(),homs.getRight());
                continue;
            }
            
            int arity1 = sig1.getArityForLabel(sym1);
                        
            for(String sym2 : relevant2){
                if(rlm.isFrontier(sym2)){
                    continue;
                }
                
                int arity2 = sig2.getArityForLabel(sym2);
                
                if(arity1 < arity2){
                    makePairings(xs, arity2, sym2, arity1, sym1, sig, homs.getRight(), homs.getLeft());   
                }else{
                    makePairings(xs, arity1, sym1, arity2, sym2, sig, homs.getLeft(), homs.getRight());
                }
            }
        }
        
        TreeAutomaton inv1 = new InverseHomAutomaton(input1, homs.getLeft()).asConcreteTreeAutomaton();
        TreeAutomaton inv2 = new InverseHomAutomaton(input2, homs.getRight()).asConcreteTreeAutomaton();
        
        return new Pair<>(inv1.intersect(inv2).asConcreteTreeAutomaton(),homs);
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
            xs.add(Tree.create("?"+i));
        }
        Tree<String> expression1 = Tree.create(symLarger, xs);
        
        if(aritySmaller == 0)
        {
            Tree<String> expression2 = Tree.create(symSmaller);
            String label = this.makeLabel(sig, arityLarger);
            
            homLarger.add(label, expression1);
            homSmaller.add(label, expression2);
            
            return;
        }
        
        xs.clear();
        VariableIterator xcomb = new VariableIterator(aritySmaller, arityLarger);
        
        while(xcomb.hasNext()){
            List<String> comb = xcomb.next();
            xs.clear();
            for(int i=0;i<comb.size();++i){
                xs.add(Tree.create(comb.get(i)));
            }
            Tree<String> expression2 = Tree.create(symSmaller, xs);
            
            String label = this.makeLabel(sig, arityLarger);
            
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
    private void handleFrontier(RuleMarker rlm, String sym1, Signature sig, Homomorphism hom1,
            Homomorphism hom2) {
        String sym2 = rlm.getCorresponding(sym1);
        if(!hom2.getTargetSignature().contains(sym2))
        {
            return;
        }
        
        Tree<String> t = Tree.create("?1");
        
        String label = Util.gensym("X");
        sig.addSymbol(label, 1);
        
        hom1.add(label, Tree.create(sym1, t));
        hom2.add(label, Tree.create(sym2, t));
    }

    /**
     * 
     * @param sig
     * @param xs
     * @param hom1
     * @param hom2 
     */
    private void makeSymbolToSkip(Set<String> relevant, Signature sigToAdd, Signature from, List<Tree<String>> xs, Homomorphism hom1,
                                                 Homomorphism hom2, String dummy, RuleMarker rlm) {
        Tree<String> dum = Tree.create(dummy);
        
        for(String sym : relevant){
            if(rlm.isFrontier(sym)){
                continue;
            }
            
            int arity1 = from.getArityForLabel(sym);
            xs.clear();
            for(int i=1;i<=arity1;++i){
                xs.add(Tree.create("?"+i));
            }
            
            Tree<String> expression = Tree.create(sym, xs);
            String label = makeLabel(sigToAdd,arity1);
            
            hom1.add(label, expression);
            hom2.add(label, dum);
            
            if(arity1 < 1){
                continue;
            }
            
            for(int i=0;i<xs.size();++i){
                label = makeLabel(sigToAdd, arity1);
                
                hom2.add(label, xs.get(i));
                hom1.add(label, expression);
            }
        }
    }

    /**
     * 
     * @return 
     */
    private String makeLabel(Signature sig, int arity){
        String s = Util.gensym("l_");
        sig.addSymbol(s, arity);
        
        return s;
    }

    /**
     * 
     * @param sig
     * @return 
     */
    private String findDummy(Signature sig) {
        String dum = "D";
        while(sig.contains("D")){
            sig.addSymbol(dum, 0);
        }
        
        return dum;
    }

    /**
     * 
     * @param input1
     * @return 
     */
    private Set<String> makeRelevant(TreeAutomaton input1) {
        Set<String> ret = new HashSet<>();
        
        IntSet ins = input1.getAllLabels();
        IntIterator iit = ins.iterator();
        while(iit.hasNext()){
            String s = input1.getSignature().resolveSymbolId(iit.nextInt());
            ret.add(s);
        }
        
        return ret;
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
                insert.add("?"+i);
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