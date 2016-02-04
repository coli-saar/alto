/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.handle_unknown;

import de.up.ling.irtg.rule_finding.preprocessing.geoquery.CreateLexicon;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Iterator;
import java.util.function.Function;

/**
 *
 * @author christoph
 */
public class ReduceAndDrop {
    
    /**
     * 
     */
    private final int minNumber;
    
    /**
     * 
     */
    private final Function<String,String> unknown;
    
    /**
     * 
     */
    private final Function<String,String> known;

    /**
     * 
     * @param minNumber
     * @param unknown
     * @param known 
     */
    public ReduceAndDrop(int minNumber,
            Function<String, String> unknown, Function<String, String> known) {
        this.minNumber = minNumber;
        this.unknown = unknown;
        this.known = known;
    }
    
    /**
     * 
     * @param training
     * @return
     */
    public Function<String[],String> getReduction(Iterator<String[]> training) {
        Object2IntOpenHashMap<String> counts = new Object2IntOpenHashMap<>();
        training.forEachRemaining((line) -> {
            for(String s : line) {
                counts.addTo(s, 1);
            }
        });
        
        return (String[] input) -> {
          StringBuilder sb = new StringBuilder();
          
          for(int i=0;i<input.length;++i) {
              if(i != 0) {
                  sb.append(" ");
              }
              
              String s = input[i];
              
              if(CreateLexicon.isSpecial(s)) {
                  sb.append(s);
              } else {
                  int count = counts.getInt(s);
                  
                  if(count < this.minNumber) {
                      sb.append(this.unknown.apply(s));
                  } else {
                      sb.append(this.known.apply(s));
                  }
              }
          }
          
          return sb.toString();  
        };
    }

    /**
     * 
     * @param statStream
     * @param check
     * @return 
     */
    public Function<String[], String> getCheckedReduction(Iterable<String[]> statStream,
                                                            CreateLexicon.Check check) {
        Object2IntOpenHashMap<String> counts = new Object2IntOpenHashMap<>();
        statStream.forEach((line) -> {
            for(String s : line) {
                counts.addTo(s, 1);
            }
        });
        
        return (String[] input) -> {
          StringBuilder sb = new StringBuilder();
          
          for(int i=0;i<input.length;++i) {
              if(i != 0) {
                  sb.append(" ");
              }
              
              String s = input[i];
              
              int l = check.knownPattern(i, input);
              
              if(l > 0) {
                  for(int add=0;add<l;++add) {
                      if(add != 0) {
                          sb.append(" ");
                      }
                      
                      sb.append(input[i+add]);
                  }
                  
                  i += l;
              } else {
                  int count = counts.getInt(s);
                  
                  if(count < this.minNumber) {
                      sb.append(this.unknown.apply(s));
                  } else {
                      sb.append(this.known.apply(s));
                  }
              }
          }
          
          return sb.toString();  
        };
    }
}
