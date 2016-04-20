/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing.geoquery;

import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.Instance;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author teichmann
 */
public class Reductioner {
    /**
     * 
     */
    private final Object2IntOpenHashMap<String> counts;
    
    /**
     * 
     */
    private final int minCounts;
    
    /**
     * 
     * @param minCounts
     * @param corp
     * @param intName 
     */
    public Reductioner(int minCounts, Corpus corp, String intName) {
        this.minCounts = minCounts;
        counts = new Object2IntOpenHashMap<>();
        
        for(Instance inst : corp) {
            List<String> sent = (List<String>) inst.getInputObjects().get(intName);
            
            for(int i=0;i<sent.size();++i) {
                String s = sent.get(i).toLowerCase().trim();
                
                
                if(s.matches("\\p{Punct}+") || s.isEmpty()) {
                    continue;
                }
                
                counts.addTo(s, 1);
            }
        }
    }
    
    /**
     * 
     * @param inst
     * @param intName
     * @return 
     */
    public Instance reduce(Instance inst, String intName) {
        Map<String,Object> map = new HashMap<>(inst.getInputObjects());
        
        List<String> mapped = new ArrayList<>();
        List<String> old = (List<String>) map.get(intName);
        
        for(int i=0;i<old.size();++i) {
            String s = old.get(i).toLowerCase().trim();
                
                
            if(s.matches("\\p{Punct}+") || s.isEmpty()) {
                continue;
            }
            
            mapped.add(s);
        }
        
        map.put(intName, mapped);
        Instance re = new Instance();
        re.setInputObjects(map);
        
        return re;
    }
}
