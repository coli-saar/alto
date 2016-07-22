/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.nonterminals;

import java.util.HashMap;
import java.util.Map;
/**
 * 
 * @author christoph
 */
public class IdentityNonterminals extends LookUpMTA {
    @Override
    protected Map<String, String> createConvertedMapping(Map<String, String> stateToType,
                                                String defAult, Map<String, String> internal) {
        Map<String,String> result = new HashMap<>();
        for(Map.Entry<String,String> ent : stateToType.entrySet()) {
            String name = ent.getKey();
            String val = ent.getValue();
            if(val == null) {
                result.put(name, defAult);
            } else {
                String mapped = internal.get(val);
                
                mapped = mapped == null ? val : mapped;
                
                result.put(name, mapped);
            }
        }
        return result;
    }
}
