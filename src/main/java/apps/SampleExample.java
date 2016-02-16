/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497b;

/**
 *
 * @author christoph_teichmann
 */
public class SampleExample {
    private final static int INNER_LOOP_STEPS = 200;
    private final static int OUTER_LOOP_STEPS = 1000;
    
    
    /**
     * 
     */
    private static final RandomGenerator rg = new Well44497b(92338429483298723L);
    
    
    public static void main(String... ars) {
        // CREATE THE AUTOMATON FROM WHICH WE ARE SAMPLING
        StringAlgebra sal = new StringAlgebra();
        List<String> input = new ArrayList<>();
        for(int i=0;i<120;++i) {
            input.add("a");
        }
        
        TreeAutomaton t = sal.decompose(input).asConcreteTreeAutomaton();
        
        //CREATE THE CACHE
        Int2ObjectMap<List<Rule>> cache = new Int2ObjectOpenHashMap<>();
        IntIterator iit = t.getAllStates().iterator();
        while(iit.hasNext()) {
            int state = iit.nextInt();
            List<Rule> list = new ArrayList<>();
            cache.put(state, list);
            
            Iterable<Rule> rules = t.getRulesTopDown(state);
            rules.forEach(list::add);
        }
        
        /// STATE TO START SAMPLING FROM
        int start = t.getFinalStates().iterator().nextInt();
        
        /// EVALUATION
        for(int i=0;i<OUTER_LOOP_STEPS;++i) {
            sampleCacheForEach(t,cache,start);
            
            sampleCacheIndex(t,cache,start);
            
            sampleNoCache(t,start);
        }
    }

    private static void sampleCacheIndex(TreeAutomaton t, Int2ObjectMap<List<Rule>> cache, int start) {
        for(int i=0;i<INNER_LOOP_STEPS;++i) {
            sampleLocalCacheIndex(t,cache,start);
        }
    }

    private static void sampleLocalCacheIndex(TreeAutomaton t, Int2ObjectMap<List<Rule>> cache, int state) {
        List<Rule> options = cache.get(state);
        int numRules = 0;
        for(int i=0;i<options.size();++i) {
            ++numRules;
        }
       
       int choice = rg.nextInt(numRules);
       
       Rule r = null;
       for(int i=0;i<numRules;++i) {
           Rule k = options.get(i);
           
           if(choice == 0) {
              r = k;
              break;
           }
           
           --choice;
        }
       
       for(int i=0;i<r.getArity();++i) {
           sampleLocalCacheIndex(t, cache, r.getChildren()[i]);
       }
    }

    /// HERE THE NO CACHE VERSION STARTS
    private static void sampleNoCache(TreeAutomaton t, int start) {
        for(int i=0;i<INNER_LOOP_STEPS;++i) {
            sampleLocalNoCache(t,start);
        }
    }
    private static void sampleLocalNoCache(TreeAutomaton t, int state) {
        Iterable<Rule> it = t.getRulesTopDown(state);
        Iterator<Rule> iter = it.iterator();
        
        int size = 0;
        while(iter.hasNext()) {
            iter.next();
            ++size;
        }
        
        int choice = rg.nextInt(size);
        
        iter = it.iterator();
        Rule r = null;
        while(iter.hasNext()) {
            Rule k = iter.next();
            if(choice == 0) {
                r = k;
                break;
            }
            
            --choice;
        }
        
        for(int i=0;i<r.getArity();++i) {
            sampleLocalNoCache(t, r.getChildren()[i]);
        }
    }
    
    ///HERE THE FOREACH PORTION STARTS
    private static void sampleCacheForEach(TreeAutomaton t, Int2ObjectMap<List<Rule>> cache, int start) {
        for(int i=0;i<INNER_LOOP_STEPS;++i) {
            sampleLocalCacheForEach(t,cache,start);
        }
    }

    private static class Counter implements Consumer<Rule> {
        public int count;

        @Override
        public void accept(Rule t) {
            ++count;
        }
    }
    private static Counter counter = new Counter();
    
    private static class MainCode implements Consumer<Rule> {
        public int pos;
        
        public Rule pick;
        

        @Override
        public void accept(Rule t) {
            if(pos == 0) {
                pick = t;
            }
            
            --pos;
        }
    }
    private static MainCode block = new MainCode();
    
    private static void sampleLocalCacheForEach(TreeAutomaton t, Int2ObjectMap<List<Rule>> cache, int state) {      
       List<Rule> options = cache.get(state);
       counter.count = 0;
       options.forEach(counter);
       
       int choice = rg.nextInt(counter.count);
       block.pos = choice;
        
       options.forEach(block);
        
       Rule r = block.pick;
       for(int i=0;i<r.getArity();++i) {
           sampleLocalCacheForEach(t, cache, r.getChildren()[i]);
       }
    }
}
