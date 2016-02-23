/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.RuleFindingIntersectionAutomaton;
import de.up.ling.irtg.automata.TopDownIntersectionAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.pruning.Pruner;
import de.up.ling.irtg.rule_finding.pruning.RemoveDead;
import de.up.ling.irtg.util.BiFunctionParallelIterable;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author christoph_teichmann
 */
public class CorpusCreator {
    
    /**
     * 
     */
    private final Pruner firstPruner;
    
    /**
     * 
     */
    private final Pruner secondPruner;
    
    /**
     * 
     */
    private final int maxThreads;

    /**
     * 
     * @param firstPruner
     * @param secondPruner
     * @param maxThreads 
     */
    public CorpusCreator(Pruner firstPruner, Pruner secondPruner, int maxThreads) {
        this.firstPruner = firstPruner;
        this.secondPruner = secondPruner;
        this.maxThreads = maxThreads;
    }

    /**
     * 
     * @return 
     */
    public Pruner getFirstPruner() {
        return firstPruner;
    }

    /**
     * 
     * @return 
     */
    public Pruner getSecondPruner() {
        return secondPruner;
    }
    
    /**
     * 
     * @param data1
     * @param data2
     * @return 
     */
    public Iterable<Pair<TreeAutomaton<String>,TreeAutomaton<String>>> pushAlignments(
            Iterable<Pair<TreeAutomaton,StateAlignmentMarking>> data1,
            Iterable<Pair<TreeAutomaton,StateAlignmentMarking>> data2) {
        return () -> {
            return new Iterator<Pair<TreeAutomaton<String>, TreeAutomaton<String>>>() {
                /**
                 * 
                 */
                private final Iterator<Pair<TreeAutomaton,StateAlignmentMarking>> itAut1 = data1.iterator();
                
                /**
                 * 
                 */
                private final Iterator<Pair<TreeAutomaton,StateAlignmentMarking>> itAut2 = data2.iterator();
                
                @Override
                public boolean hasNext() {
                    return itAut2.hasNext() && itAut2.hasNext();
                }

                @Override
                public Pair<TreeAutomaton<String>, TreeAutomaton<String>> next() {
                    Pair<TreeAutomaton,StateAlignmentMarking> pa1 = this.itAut1.next();
                    Pair<TreeAutomaton,StateAlignmentMarking> pa2 = this.itAut2.next();
                    
                    TreeAutomaton t1 = pa1.getLeft();
                    TreeAutomaton t2 = pa2.getLeft();
                    
                    StateAlignmentMarking sam1 = pa1.getRight();
                    StateAlignmentMarking sam2 = pa2.getRight();
                    
                    Propagator prop = new Propagator();
                    
                    Int2ObjectMap<IntSortedSet> p1 = prop.propagate(t1, sam1);
                    Int2ObjectMap<IntSortedSet> p2 = prop.propagate(t2, sam2);
                    
                    Set<String> counter1 = Propagator.turnToMarkers(p1.values());
                    Set<String> counter2 = Propagator.turnToMarkers(p2.values());
                    
                    TreeAutomaton r1 = prop.convert(t1, p1, counter2);
                    TreeAutomaton r2 = prop.convert(t2, p2, counter1);
                    
                    return new Pair<>(r1, r2);
                }
            };
        };
    }
    
    /**
     * 
     * @param <State1>
     * @param <State2>
     * @param input
     * @return 
     */
    public <State1,State2> Iterable<Pair<TreeAutomaton,HomomorphismManager>> getSharedAutomata(Iterable<Pair<TreeAutomaton<State1>,TreeAutomaton<State2>>> input) {
        Iterable<Object> pseudoIt = () -> {
            return new Iterator<Object>() {

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public Object next() {
                    return null;
                }
            };
        };
        
        
        BiFunctionParallelIterable<Pair<TreeAutomaton<State1>,TreeAutomaton<State2>>,Object,Pair<TreeAutomaton,HomomorphismManager>> results =
                new BiFunctionParallelIterable<>(input, pseudoIt, maxThreads, 
                (Pair<TreeAutomaton<State1>,TreeAutomaton<State2>> in, Object dummy) -> {
                    TreeAutomaton<String> l = this.firstPruner.apply(in.getLeft());
                    TreeAutomaton<String> r = this.secondPruner.apply(in.getRight());
                    
                    HomomorphismManager hom = new HomomorphismManager(l.getSignature(),r.getSignature());
                    hom.update(l.getAllLabels(), r.getAllLabels());
                    
                    RuleFindingIntersectionAutomaton rfi = new RuleFindingIntersectionAutomaton(l, r, hom.getHomomorphism1(), hom.getHomomorphism2());
                    TopDownIntersectionAutomaton tdi = new TopDownIntersectionAutomaton(rfi, hom.getRestriction());
                    
                    System.out.println("before dead removal");
                    TreeAutomaton finished = RemoveDead.reduce(tdi);
                    System.out.println("after dead removal");
                    
                    finished = hom.reduceToOriginalVariablePairs(finished);
                    
                    return new Pair<>(finished,hom);
                });
        
        return results;
    }
}