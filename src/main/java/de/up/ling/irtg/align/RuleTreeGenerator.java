/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.InverseHomAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * This class takes two decomposition automata (with alignment annotation) and creates
 * an intersection automaton over an alphabet that can be mapped into the original alphabets
 * of the given automata.
 * 
 * Alignments are respected by querying the given rule marker and the resulting automaton
 * only generates trees from the input automata under the homomorphisms.
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
        Signature sig = new Signature();
        
        // first we generate two homomorphism that allow us to arbitrarily combine trees
        Pair<Homomorphism,Homomorphism> homs = this.getHomomorphisms(sig, input1, input2, rlm);
        
        // then we compute the inverse
        TreeAutomaton inv1 = new InverseHomAutomaton(input1, homs.getLeft()).asConcreteTreeAutomaton();
        TreeAutomaton inv2 = new InverseHomAutomaton(input2, homs.getRight()).asConcreteTreeAutomaton();
        
        // finally we restrict the result so that variables are always generated in both trees and 
        TreeAutomaton restriction = makeRestriction(sig,rlm, homs.getLeft(), homs.getRight());
        
        // then we simply return the intersection and the homomorphisms
        return new Pair<>(inv1.intersect(inv2).intersect(restriction),homs);
    }

    /**
     * This method constructs a pair of homomorphisms from a generated alphabet into the input alphabets.
     * 
     * These homomorphisms allow for aligning anything with anything under any permutation of the variables, the
     * only exception are the alignment labels. Those can only be aligned with other alignment nodes that are an
     * exact match.
     * 
     * @param sig The signature into which the shared labels will be written.
     * @param input1 The first target language.
     * @param input2 The second target language.
     * @param rlm The rule marker that gets to decide which labels indicate alignment.
     * @return A pair of homomorphisms for grammar inference.
     */
    public Pair<Homomorphism,Homomorphism> getHomomorphisms(Signature sig, TreeAutomaton input1,
                                                            TreeAutomaton input2, RuleMarker rlm){
        Signature sig1 = input1.getSignature();
        Signature sig2 = input2.getSignature();
        
        Pair<Homomorphism,Homomorphism> homs = new Pair<>(new Homomorphism(sig, input1.getSignature()),
                new Homomorphism(sig,input2.getSignature()));
        
        // use to store variable permutations
        List<Tree<String>> xs = new ArrayList<>();
        // use to store variable permutations
        List<Tree<String>> otherXs = new ArrayList<>();
        
        // this extracts all the symbols that are actually used in the two automata,
        // if the automata offer a better implementation than simply returning their signature
        Set<String> relevant1 = makeRelevant(input1);
        Set<String> relevant2 = makeRelevant(input2);
        
        // here we combine symbols with just single variables.
        this.makeSymbolToSkip(relevant1, sig, sig1, xs, homs.getLeft(), homs.getRight(), rlm);
        this.makeSymbolToSkip(relevant2, sig, sig2, xs, homs.getRight(), homs.getLeft(), rlm);
        
        // now we pair labels
        for(String sym1 : relevant1){
            // if we have an alingment label, we simply pair it up with its match (if it exists)
            if(rlm.isFrontier(sym1)){
                handleFrontier(rlm, sym1, sig, homs.getLeft(),homs.getRight());
                continue;
            }
            
            // otherwise we compare arities in order to decide for which one we need to iterate over
            // variable permutations
            int arity1 = sig1.getArityForLabel(sym1);
                        
            for(String sym2 : relevant2){
                // skip frontier nodes, we already dealt with them
                if(rlm.isFrontier(sym2)){
                    continue;
                }
                
                // get the second arity so we can compare
                int arity2 = sig2.getArityForLabel(sym2);
                
                if(arity1 < arity2){
                    makePairings(xs, otherXs, arity2, sym2, arity1, sym1, sig, homs.getRight(), homs.getLeft());   
                }else{
                    makePairings(xs, otherXs, arity1, sym1, arity2, sym2, sig, homs.getLeft(), homs.getRight());
                }
            }
        }
        
        return homs;
    }
    
    /**
     * This method computes all the possible variable permutations for the label with the smaller arity
     * and then combines them with the label with the larger arity.
     * 
     * @param xs Used to store variable permutations for the smaller arity.
     * @param other Used to store variables for the larger arity.
     * @param arityLarger The arity of the label for which it is largest.
     * @param symLarger The label with the larger arity.
     * @param aritySmaller The arity for the label for which it is smallest.
     * @param symSmaller The label with the smaller arity.
     * @param sig The signature of the shared language.
     * @param homLarger The homomorphism that interprets into the language of symLarger.
     * @param homSmaller The homomorphism that interprets into the language of symSmaller.
     */
    private void makePairings(List<Tree<String>> xs, List<Tree<String>> other, int arityLarger, String symLarger,
            int aritySmaller, String symSmaller, Signature sig,
            Homomorphism homLarger, Homomorphism homSmaller) {
        other.clear();
        // for the larger one, we simply fill all its variable slots.
        for(int i=1;i<=arityLarger;++i){
            other.add(Tree.create("?"+i));
        }
        
        Tree<String> expression1 = Tree.create(symLarger, other);
        
        // if the smaller one takes no variables, then we do not need to iterate
        // over options.
        if(aritySmaller == 0)
        {
            Tree<String> expression2 = Tree.create(symSmaller);
            String label = this.makeLabel(sig, arityLarger);
            
            homLarger.add(label, expression1);
            homSmaller.add(label, expression2);
            
            return;
        }
        
        // otherwise we iterate over all ways of picking variables from the list
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
     * This method pairs up alignment labels in the homomorphisms.
     * 
     * @param rlm The rule marker that understands the alignment labels.
     * @param sym1 The frontier label that we have already found.
     * @param sig The signature of the shared language.
     * @param hom1 First interpretation.
     * @param hom2 Second interpretation.
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
     * Pairs up labels with single variables.
     * 
     * @param sig Signature of the shared language.
     * @param xs Used to hold the variables.
     * @param hom1 First interpretation.
     * @param hom2 Second interpretation.
     */
    private void makeSymbolToSkip(Set<String> relevant, Signature sigToAdd, Signature from,
                                                        List<Tree<String>> xs, Homomorphism hom1,
                                                 Homomorphism hom2, RuleMarker rlm) {
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
            if(arity1 < 1){
                continue;
            }
            
            for(int i=0;i<xs.size();++i){
                String label = makeLabel(sigToAdd, arity1);
                
                hom2.add(label, xs.get(i));
                hom1.add(label, expression);
            }
        }
    }

    /**
     * Returns a new label for the given arity.
     * @return 
     */
    private String makeLabel(Signature sig, int arity){
        String s = Util.gensym("l_");
        sig.addSymbol(s, arity);
        
        return s;
    }

    /**
     * Returns the set of all the labels that are included in input1.getAllLabels().
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
     * Creates an automaton that forces frontier labels to be realized in both
     * homomorphic images of any rule tree that uses them.
     * 
     * Also enforces that there are no frontier loops and no pairs of frontiers
     * at the root.
     * 
     * @param sig Shared language signature.
     * @param rlm Marks the alignment labels.
     * @param h1 First mapping.
     * @param h2 Second mapping.
     * @return 
     */
    private TreeAutomaton makeRestriction(Signature sig, RuleMarker rlm,
            Homomorphism h1, Homomorphism h2) {
        ConcreteTreeAutomaton<String> cta = new ConcreteTreeAutomaton<>(sig);
        
        // start state necessary to ensure no frontier at root
        int start = cta.addState(findFree("s", sig));
        // state that indicates that we have not yet entered the useless part
        // of any interpretation
        int general = cta.addState(findFree("a",sig));
        // indicates that we have entered the useless part.
        int addmissible = cta.addState(findFree("b",sig));
        
        cta.addFinalState(start);
        
        IntSet s1 = new IntOpenHashSet();
        IntSet s2 = new IntOpenHashSet();
        
        IntArrayList il = new IntArrayList();
        
        Set<String> fronts = new ObjectOpenHashSet<>();
        Set<String> others = new ObjectOpenHashSet<>();
        
        for(String sym : sig.getSymbols()){
            String l1 = h1.get(sym).getLabel();
            String l2 = h2.get(sym).getLabel();
            
            // frontier labels get their own states and they MUST be followed
            // by something else
            if(rlm.isFrontier(l1) || rlm.isFrontier(l2)){
                int label = sig.getIdForSymbol(sym);
                int goal = cta.addState(sym);
                
                il.clear();
                fronts.add(sym);
                il.add(goal);
                cta.addRule(cta.createRule(general, label, il, 1));
            }else{
                others.add(sym);
            }
        }
        
        for(String sym : others){
            int label = sig.getIdForSymbol(sym);
            
            Tree<String> t1 = h1.get(sym);
            Tree<String> t2 = h2.get(sym);
            
            String l1 = t1.getLabel();
            String l2 = t2.getLabel();    
            
            // addmissible can do anything
            il.clear();
            for(int i=0;i<sig.getArityForLabel(sym);++i){
                il.add(addmissible);
            }
            
            cta.addRule(cta.createRule(addmissible, label, il, 1));
            
            s1.clear();
            s2.clear();
            fillWithConversion(s1,t1);
            fillWithConversion(s2,t2);
            
            // filter out positions that will turn into nothing.
            il.clear();
            for(int i=1;i<=sig.getArityForLabel(sym);++i){
                il.add((s1.contains(i) && s2.contains(i)) ? general : addmissible);
            }
            
            cta.addRule(cta.createRule(general, label, il, 1));
            cta.addRule(cta.createRule(start, label, il, 1));
            
            // the frontier states behave like the general state when it comes
            // to non-frontier transitions.
            for(String s : fronts){
                int state = cta.getIdForState(s);
                cta.addRule(cta.createRule(state, label, il, 1));
            }
        }
        
        return cta;
    }

    /**
     * Reads the variables in the tree and adds their numbers to the set.
     * 
     * @param is
     * @param children 
     */
    private void fillWithConversion(IntSet is, Tree<String> t) {
        String label = t.getLabel();
        if(label.matches("\\?.*")){
            is.add(Integer.parseInt(label.substring(1)));
            return;
        }
        
        for(Tree<String> ts : t.getChildren()){
            is.add(Integer.parseInt(ts.getLabel().substring(1)));
        }
    }

    /**
     * Finds an unused code in the signature.
     * 
     * @param code
     * @param sig
     * @return 
     */
    private String findFree(String code, Signature sig) {
        String ret = code;
        while(sig.contains(ret)){
            ret += code;
        }
        
        return ret;
    }
    
    /**
     * This is an utility class that lets us iterate over all ways of selecting
     * n options from the given list.
     * 
     * Will always return the same list, just different values in it.
     */
    private class VariableIterator implements Iterator<List<String>>{
        /**
         * Are we in the first step of the iteration?
         */
        private boolean first = true;
        
        /**
         * The returned list.
         */
        private final List<String> ret = new ArrayList<>();
        
        /**
         * The variable sets from which we draw.
         */
        private final List<Set<String>> sources = new ArrayList<>();
        
        /**
         * The current iterators.
         */
        private final List<Iterator<String>> main = new ArrayList<>();
        
        /**
         * Construct a new instance that assumes that there are from variables
         * to fill pick slots.
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
            //we have more while we are at the start or some iterator has not
            // finished.
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
            // find the last iterator that still has elements left
            for(;cut >= 0;--cut){
                if(this.main.get(cut).hasNext()){
                   break; 
                }
            }
            
            if(cut < 0){
                throw new NoSuchElementException();
            }
            //update the elements from there on
            this.ret.set(cut, main.get(cut).next());
            
            for(int i=cut+1;i<main.size();++i){
                // the new sets that follow it can not contain the elements we already picked.
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