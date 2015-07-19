/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.alignment_algebras;

import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.align.AlignmentAlgebra;
import de.up.ling.irtg.align.RuleMarker;
import de.up.ling.irtg.align.rule_markers.SimpleRuleMarker;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author christoph_teichmann
 */
public class StringAlignmentAlgebra extends StringAlgebra implements AlignmentAlgebra {

    @Override
    public Pair<RuleMarker,Pair<TreeAutomaton,TreeAutomaton>> decomposePair(String one, String two) {
        String[] pOne = one.split("\\s+");
        String[] pTwo = two.split("\\s+");
        
        List<String> input = new ArrayList<>();
        Int2IntMap oneMarkerToPosition = new Int2IntOpenHashMap();
        int pos = 0;
        for(String s : pOne)
        {
            String[] k = s.split(":");
            input.add(k[0]);
            getSignature().addSymbol(k[0], 0);
            
            for(int i=1;i<k.length;++i){
                oneMarkerToPosition.put(Integer.parseInt(k[i].trim()), pos);
            }
            ++pos;
        }
        
        TreeAutomaton<Span> ta1 = decompose(input);
        
        input.clear();
        Int2IntMap twoMarkerToPosition = new Int2IntOpenHashMap();
        pos = 0;
        for(String s : pTwo)
        {
            String[] k = s.split(":");
            input.add(k[0]);
            getSignature().addSymbol(k[0], 0);
            
            for(int i=1;i<k.length;++i){
                twoMarkerToPosition.put(Integer.parseInt(k[i].trim()), pos);
            }
            ++pos;
        }
        
        TreeAutomaton<Span> ta2 = decompose(input);
        
        Int2ObjectMap<Rule> rulesOne = new Int2ObjectOpenHashMap<>();
        Int2ObjectMap<Rule> rulesTwo = new Int2ObjectOpenHashMap<>();
        
        
        for(Rule r : ta1.getRuleSet()){
            Span s = ta1.getStateForId(r.getParent());
            if(s.end - s.start == 1){
                rulesOne.put(s.start, r);
            }
        }
        for(Rule r : ta2.getRuleSet()){
            Span s = ta2.getStateForId(r.getParent());
            if(s.end - s.start == 1){
                rulesTwo.put(s.start, r);
            }
        }
        
        SimpleRuleMarker srm = new SimpleRuleMarker(ensure("X",ta1.getSignature(),ta2.getSignature()));
        Iterator<Entry> it = oneMarkerToPosition.int2IntEntrySet().iterator();
        
        while(it.hasNext()){
            Entry e = it.next();
            int marker = e.getIntKey();
            int firstPos = e.getIntValue();
            if(twoMarkerToPosition.containsKey(marker)){
                int secondPos = twoMarkerToPosition.get(marker);
                
                srm.addPair(rulesOne.get(firstPos), rulesTwo.get(secondPos));
            }
        }
        
        return new Pair<>(srm,new Pair<>(ta1,ta2));
    }

    /**
     * 
     * @param x
     * @param signature
     * @param signature0
     * @return 
     */
    private String ensure(String x, Signature signature, Signature signature0) {
        String code = x;
        while(signature.contains(code) || signature0.contains(code)){
            code += x;
        }
        
        return code;
    }
}