/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.pruning;

import de.up.ling.irtg.align.Pruner;
import de.up.ling.irtg.align.StateAlignmentMarking;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Comparator;

/**
 *
 * @author christoph_teichmann
 * @param <State>
 */
public class TightPruner<State> implements Pruner<State> {

    @Override
    public TreeAutomaton<State> postPrune(TreeAutomaton<State> automaton, StateAlignmentMarking<State> stateMarkers) {
        return automaton;
    }

    @Override
    public TreeAutomaton<State> prePrune(TreeAutomaton<State> automaton, StateAlignmentMarking<State> stateMarkers) {
        Int2ObjectMap tight = new Int2ObjectOpenHashMap();
        Int2IntMap height = new Int2IntOpenHashMap();
        
        Visitor<State> vis = new Visitor<>(automaton, stateMarkers, tight, height);
        automaton.foreachStateInBottomUpOrder(vis);
        
        
        
        
        ConcreteTreeAutomaton<State> cta = new ConcreteTreeAutomaton<State>();
        
        IntSet done = new IntOpenHashSet();
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
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
        private final Int2IntMap height;

        /**
         * 
         * @param base
         * @param markers
         * @param tight
         * @param height 
         */
        public Visitor(TreeAutomaton<State> base, StateAlignmentMarking<State> markers, Int2ObjectMap<Tightness> tight, Int2IntMap height) {
            this.base = base;
            this.markers = markers;
            this.tight = tight;
            this.height = height;
            this.height.defaultReturnValue(Integer.MAX_VALUE-1);
        }
        
        @Override
        public void visit(int state, Iterable<Rule> rulesTopDown) {
            Tightness t = Tightness.NONE;
            int depth = Integer.MAX_VALUE;
            
            for(Rule r : rulesTopDown){
                Tightness o = Tightness.getTightness(r, tight, markers, base);
                if(o.compareTo(t) < 0){
                    t = o;
                }
                
                if(r.getChildren().length < 1){
                    depth = 1;
                }else{
                    int h = this.height.get(r.getChildren()[0])+1;
                    depth = Math.max(depth, h);
                }
            }
            
            this.height.put(state, depth);
            this.tight.put(state, t);
        }        
    }
    
    /**
     * 
     */
    public static enum Tightness{
        TIGHT,
        RIGHT,
        LEFT,
        NONE;
        
        
        /**
         * 
         * @param <State>
         * @param r
         * @param alreadyProcessed
         * @param alignments
         * @param t
         * @return 
         */
        public static <State> Tightness getTightness(Rule r, Int2ObjectMap<Tightness> alreadyProcessed,
                StateAlignmentMarking<State> alignments, TreeAutomaton<State> t){
            IntSet align = alignments.getAlignmentMarkers(t.getStateForId(r.getParent()));
            if(!align.isEmpty()){
                return TIGHT;
            }
            
            if(r.getChildren().length < 2){
                return NONE;
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
                else{
                    return NONE;
                }
            }
        }
    }
    
    private class Comparison implements Comparator<Rule>{

        
        
        @Override
        public int compare(Rule o1, Rule o2) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
}