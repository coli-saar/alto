/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.nonterminals;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author christoph_teichmann
 */
public abstract class StateValueComputation {
    /**
     * 
     * @param mtaAutomaton
     * @return 
     */
    public Map<String, String> getStatesToTypes(TreeAutomaton mtaAutomaton) {
        IntList states = mtaAutomaton.getStatesInBottomUpOrder();
        Map<String, String> labels = new HashMap<>();
        IntIterator iit = states.iterator();
        while (iit.hasNext()) {
            int state = iit.nextInt();
            Iterable<Rule> rules = mtaAutomaton.getRulesTopDown(state);
            for (Rule r : rules) {
                String s = computeValue(r, labels, mtaAutomaton);
                if (s != null) {
                    labels.put(mtaAutomaton.getStateForId(state).toString(), s);
                    break;
                }
            }
        }
        return labels;
    }

    /**
     * 
     * @param r
     * @param labels
     * @param mtaAutomaton
     * @return 
     */
    protected abstract String computeValue(Rule r, Map<String, String> labels, TreeAutomaton mtaAutomaton);    
    
    /**
     * 
     * @param ta
     * @param internal
     * @param defAult
     * @return 
     */
    public Map<String,String> mapToNonterminal(TreeAutomaton ta, Map<String,String> internal, String defAult) {
        Map<String,String> result = new HashMap<>();
        Map<String,String> states = this.getStatesToTypes(ta);
        
        for(Map.Entry<String,String> ent : states.entrySet()) {
            String name = ent.getKey();
            String val = states.get(name);
            if(val == null) {
                result.put(name, defAult);
            } else {
                val = internal.get(val);
                
                val = val == null ? defAult : val;
                
                result.put(name, val);
            }
        }
        
        return result;
    }
    
    
    /**
     * 
     * @param inputs
     * @param outputs
     * @param map
     * @param defAult
     * @throws IOException 
     */
    public void transferAutomata(Iterable<InputStream> inputs, Iterable<OutputStream> outputs,
                                    InputStream map, String defAult) throws IOException {
        Map<String,String> information = makeInfo(map);
        
        Iterator<InputStream> ins = inputs.iterator();
        Iterator<OutputStream> outs = outputs.iterator();
        
        TreeAutomatonInputCodec taic = new TreeAutomatonInputCodec();
        while(ins.hasNext()) {
            InputStream in = ins.next();
            
            
            OutputStream ous = outs.next();
            try(BufferedWriter store = new BufferedWriter(new OutputStreamWriter(ous))) {
                TreeAutomaton tai = taic.read(in);
                Map<String,String> internal = this.mapToNonterminal(tai, information, defAult);
                
                for(Map.Entry<String,String> ent : internal.entrySet()) {
                    store.write(ent.getKey());
                    store.write(ReplaceNonterminal.DIVIDER);
                    store.write(ent.getValue());
                    store.newLine();
                }
            } catch (IOException oex) {
                System.err.println("-----------");
                System.err.println(oex);
                System.err.println("-----------");
                System.err.println("Error processing entry");
            }
        }
    }
    
    /**
     * 
     * @param map
     * @return 
     */
    private Map<String, String> makeInfo(InputStream map) throws IOException {
        Map<String,String> assignments = new HashMap<>();
        
        try(BufferedReader input = new BufferedReader(new InputStreamReader(map))) {
            String line;
            
            while((line = input.readLine()) != null) {
                line = line.trim();
                if(line.isEmpty()) {
                    continue;
                }
                
                String[] parts = line.split(ReplaceNonterminal.DIVIDER_REGEX);
                
                String label = parts[0].trim();
                String type = parts[1].trim();
                
                if(label.startsWith("'")) {
                    label = label.substring(1);
                }
                if(label.endsWith("'")) {
                    label = label.substring(0,label.length()-1);
                }
                
                assignments.put(label, type);
            }
        }
        
        return assignments;
    }
}
 