/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.pruning;

import de.up.ling.irtg.align.HomomorphismManager;
import de.up.ling.irtg.align.Pruner;
import de.up.ling.irtg.align.StateAlignmentMarking;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Set;

/**
 *
 * @author christoph_teichmann
 * @param <State>
 */
public class TightPruner<State> implements Pruner<State> {

    /**
     * 
     */
    private Set<String> admissible;

    /**
     * 
     */
    private int pruningNumber = 5;

    /**
     * 
     * @param pruningNumber 
     */
    public void setPruningNumber(int pruningNumber) {
        this.pruningNumber = Math.max(pruningNumber,2);
    }
    
    /**
     * 
     * @param admissible 
     */
    public void setAdmissible(Set<String> admissible) {
        this.admissible = admissible;
    }
    
    
    @Override
    public TreeAutomaton<State> prePrune(TreeAutomaton<State> automaton, StateAlignmentMarking<State> stateMarkers) {
        return automaton;
    }

    @Override
    public TreeAutomaton<State> postPrune(TreeAutomaton<State> automaton, StateAlignmentMarking<State> stateMarkers) {
        Int2ObjectMap tight = new Int2ObjectOpenHashMap();
        Int2IntMap numberConsistent = new Int2IntOpenHashMap();
        Object2IntMap<Rule> ruleConsistent = new Object2IntOpenHashMap<>();
        
        Visitor<State> vis = new Visitor<>(automaton, stateMarkers, tight, numberConsistent, ruleConsistent);
        automaton.foreachStateInBottomUpOrder(vis);
        
        ConcreteTreeAutomaton<State> cta = new ConcreteTreeAutomaton<>();
        
        IntSet seen = new IntOpenHashSet();
        IntArrayList todo = new IntArrayList();
        IntIterator iit = automaton.getFinalStates().iterator();
        PriorityQueue<Rule> pq = new PriorityQueue<>(new Comparison(ruleConsistent, tight));
        while(iit.hasNext()){
            todo.add(iit.nextInt());
        }
        
        for(int i=0;i<todo.size();++i){
            int state = todo.getInt(i);
            Iterable<Rule> it = automaton.getRulesTopDown(state);
            
            for(Rule r : it){
                pq.offer(r);
            }
            int added = 0;
            
            while(added < this.pruningNumber && !pq.isEmpty()){
                Rule r = pq.poll();
                addRule(cta,r,automaton);
                for(int child : r.getChildren()){
                    if(!seen.contains(child)){
                        seen.add(child);
                        todo.add(child);
                    }
                }
                
                ++added;
            }
        }
        
        return cta;
    }

    /**
     * 
     * @param cta
     * @param r
     * @param original 
     */
    private void addRule(ConcreteTreeAutomaton<State> cta, Rule r, TreeAutomaton<State> original) {
        State par = original.getStateForId(r.getParent());
        int parent = cta.addState(par);
        
        if(original.getFinalStates().contains(r.getParent())){
            cta.addFinalState(parent);
        }
        
        int label = cta.getSignature().addSymbol(r.getLabel(original), r.getChildren().length);
        
        int[] children = new int[r.getChildren().length];
        for(int i=0;i<r.getChildren().length;++i){
            children[i] = cta.addState(original.getStateForId(r.getChildren()[i]));
        }
        
        cta.addRule(cta.createRule(parent, label, children, r.getWeight()));
    }
    
    /**
     * 
     * @param <State> 
     */
    private class Visitor<State> implements TreeAutomaton.BottomUpStateVisitor{

        /**
         * 
         */
        private final TreeAutomaton<State> base;
        
        /**
         * 
         */
        private final StateAlignmentMarking<State> markers;
        
        /**
         * 
         */
        private final Int2ObjectMap<Tightness> tight;
        
        /**
         * 
         */
        private final Int2IntMap numberConsistent;
        
        /**
         * 
         */
        private final Object2IntMap ruleConsistent;

        /**
         * 
         * @param base
         * @param markers
         * @param tight
         * @param numberConsistent 
         */
        public Visitor(TreeAutomaton<State> base, StateAlignmentMarking<State> markers,
                Int2ObjectMap<Tightness> tight, Int2IntMap numberConsistent, Object2IntMap ruleConsistent) {
            this.base = base;
            this.markers = markers;
            this.tight = tight;
            this.numberConsistent = numberConsistent;
            this.numberConsistent.defaultReturnValue(0);
            this.ruleConsistent = ruleConsistent;
        }
        
        @Override
        public void visit(int state, Iterable<Rule> rulesTopDown) {
            Tightness t = Tightness.NONE;
            int variablesDominated = 0;
            
            for(Rule r : rulesTopDown){
                Tightness o = Tightness.getTightness(r, tight, markers, base, admissible);
                if(o.compareTo(t) < 0){
                    t = o;
                }
                
                String label = r.getLabel(base);
                int sum = HomomorphismManager.VARIABLE_PATTERN.test(label) &&
                        (admissible == null || admissible.contains(label)) ? 1 : 0;
                for(int child : r.getChildren()){
                    sum += this.numberConsistent.get(child);
                }
                
                this.ruleConsistent.put(r, sum);
                
                variablesDominated = Math.max(sum, variablesDominated);
            }
            
            this.numberConsistent.put(state, variablesDominated);
            this.tight.put(state, t);
        }        
    }
    
    /**
     * 
     */
    public enum Tightness{
        TIGHT,
        RIGHT,
        LEFT,
        DISTANT,
        NONE;
        
        
        /**
         * 
         * @param <State>
         * @param r
         * @param alreadyProcessed
         * @param alignments
         * @param t
         * @param admissible
         * @return 
         */
        public static <State> Tightness getTightness(Rule r, Int2ObjectMap<Tightness> alreadyProcessed,
                StateAlignmentMarking<State> alignments, TreeAutomaton<State> t, Set<String> admissible){
            IntSet align = alignments.getAlignmentMarkers(t.getStateForId(r.getParent()));
            if(!align.isEmpty()){
                return TIGHT;
            }
            
            if(r.getChildren().length < 2){
                if(r.getChildren().length < 1){
                    return NONE;
                }else{
                    String label = r.getLabel(t);
                    if(admissible != null && ! admissible.contains(label)){
                        return NONE;
                    }
                    
                    if(HomomorphismManager.VARIABLE_PATTERN.test(label)){
                        Tightness q = alreadyProcessed.get(r.getChildren()[0]);
                        if(q != null){
                            return q;
                        }else{
                            return null;
                        }
                    }
                    
                    Tightness q = alreadyProcessed.get(r.getChildren()[0]);
                    if(q != null && q != NONE){
                        return DISTANT;
                    }else{
                        return NONE;
                    }
                }
            }
            
            Tightness rt = alreadyProcessed.get(r.getChildren()[0]);
            Tightness lt = alreadyProcessed.get(r.getChildren()[r.getChildren().length-1]);
            
            if(rt == TIGHT || rt == RIGHT){
                if(lt == TIGHT || lt == LEFT){
                    return TIGHT;
                }
                else{
                    return RIGHT;
                }
            }else{
                if(lt == TIGHT || lt == LEFT){
                    return LEFT;
                }
            }
            
            for(int state : r.getChildren()){
                Tightness q = alreadyProcessed.get(state);
                
                if(q != NONE && q != null){
                    return DISTANT;
                }
            }
            
            return NONE;
        }
    }
    
    /**
     * 
     */
    private class Comparison implements Comparator<Rule>{
        
        /**
         * 
         */
        private final Object2IntMap<Rule> ruleVariables;
        
        /**
         * 
         */
        private final Int2ObjectMap<Tightness> tight;

        /**
         * 
         * @param ruleVariables
         * @param tight 
         */
        public Comparison(Object2IntMap<Rule> ruleVariables, Int2ObjectMap<Tightness> tight) {
            this.ruleVariables = ruleVariables;
            this.tight = tight;
        }
        
        @Override
        public int compare(Rule o1, Rule o2) {
            int comp = Integer.compare(this.ruleVariables.get(o1), this.ruleVariables.get(o2));
            if(comp != 0){
                return comp;
            }
            
            if(o1.getChildren().length > 0){
                if(o2.getChildren().length > 0){
                    return comp(o1.getChildren(),o2.getChildren());
                }else{
                    return -1;
                }
            }else{
                if(o2.getChildren().length > 0){
                    return 1;
                }else{
                    return comp(o1.getChildren(),o2.getChildren());
                }
            }
        }

        /**
         * 
         * @param children
         * @param children0
         * @return 
         */
        private int comp(int[] children, int[] children0) {
            for(int i=0;i<children.length && i < children0.length;++i){
                int comp = this.tight.get(children[i]).compareTo(this.tight.get(children0[i]));
                if(comp != 0){
                    return comp;
                }
            }
            
            return Integer.compare(children.length, children0.length);
        }
    }
}