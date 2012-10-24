/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.automata;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 *
 * @author koller
 */
public class HuangKbest<State> {
    private int K;
    private Map<State, PriorityQueue<Derivation>> cand;
    private ListMultimap<State, Derivation> trees;
    private TreeAutomaton<State> auto;
    private ListMultimap<State, Rule<State>> rulesTopDown;

    public HuangKbest(TreeAutomaton<State> auto, int k) {
        this.K = k;
        this.auto = auto;
        
        cand = new HashMap<State, PriorityQueue<Derivation>>();
        trees = ArrayListMultimap.create();
        
        rulesTopDown = ArrayListMultimap.create();        
        for( Rule<State> rule : auto.getRuleSet() ) {
            rulesTopDown.put(rule.getParent(), rule);
        }
    }
    
    private void lazyKthBest(State q, int k) {        
        if( ! cand.containsKey(q) ) {
            getCandidates(q);
        }
        
        List<Derivation> treesForQ = trees.get(q);
        PriorityQueue<Derivation> candForQ = cand.get(q);
        
        while( treesForQ.size() < K ) {
            if( ! treesForQ.isEmpty() ) {
                Derivation d = treesForQ.get(treesForQ.size()-1);
//                lazyNext();
            }
            
            if( ! candForQ.isEmpty() ) {
                treesForQ.add(candForQ.poll());
            } else {
                break;
            }
        }
    }

    private void getCandidates(State q) {
        PriorityQueue<Derivation> pq = new PriorityQueue<Derivation>();
        List<Derivation> temp = new ArrayList<Derivation>();
        
        for( Rule<State> rule : rulesTopDown.get(q) ) {
            double weight = rule.getWeight();
            List<Tree<String>> subtrees = new ArrayList<Tree<String>>();
            
            for( int i = 0; i < rule.getArity(); i++ ) {
                // ASSUMPTION - each child already has a 1-best derivation
                WeightedTree wt = trees.get(rule.getChildren()[i]).get(0).tree;
                subtrees.add(wt.getTree());
                weight *= wt.getWeight();
            }
            
            WeightedTree wt = new WeightedTree(Tree.create(rule.getLabel(), subtrees), weight);
            Derivation d = new Derivation(rule, zeroes(rule.getArity()), wt);
            temp.add(d);
        }
        
        Collections.sort(temp);
        
        for( int i = 0; i < K; i++ ) {
            pq.offer(temp.get(i));
        }
        
        cand.put(q, pq);        
    }
    
    private class Derivation implements Comparable<Derivation> {
        public Rule<State> rule;
        public int[] positionsInChildLists;
        public WeightedTree tree;

        public Derivation(Rule<State> rule, int[] positionsInChildLists, WeightedTree tree) {
            this.rule = rule;
            this.positionsInChildLists = positionsInChildLists;
            this.tree = tree;
        }

        @Override
        public int compareTo(Derivation o) {
            return - Double.compare(tree.getWeight(), o.tree.getWeight());
        }
    }
    
    private int[] zeroes(int n) {
        int[] ret = new int[n];
        return ret;
    }
    
    private int[] oneOne(int n, int positionOfOne) {
        int[] ret = zeroes(n);
        ret[positionOfOne] = 1;
        return ret;
    }
    
    private static int[] addPositionLists(int[] l1, int[] l2) {
        if( l1.length != l2.length ) {
            return null;
        }
        
        for( int i = 0; i < l1.length; i++ ) {
            l1[i] += l2[i];
        }
        
        return l1;
    }
}
