/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.data_creation;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.CodecParseException;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;
import de.up.ling.irtg.util.FunctionIterable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 *
 * @author christoph_teichmann
 */
public class MakeAutomata {    
    /**
     * 
     * @param data
     * @param target
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ParserException 
     */
    public static void create(InputStream data, Supplier<OutputStream> target) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, ParserException {
        try(BufferedReader input = new BufferedReader(new InputStreamReader(data))) {
            String className = input.readLine();
            
            Class cl = Class.forName(className);
            Algebra algeb = (Algebra) cl.newInstance();
            
            String line;
            while((line = input.readLine()) != null) {
                line = line.trim();
                
                if(line.isEmpty()) {
                    continue;
                }
                
                TreeAutomaton ta = algeb.decompose(algeb.parseString(line));
                try(BufferedWriter output = new BufferedWriter(new OutputStreamWriter(target.get()))) {
                    output.write(ta.toString());
                }
            }
        }
    }
    
    /**
     * 
     * @param data
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ParserException 
     */
    public static Iterable<String> create(InputStream data) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, ParserException {
        List<String> result = new ArrayList<>();
        
        try(BufferedReader input = new BufferedReader(new InputStreamReader(data))) {
            String className = input.readLine();
            Class cl = Class.forName(className);
            Algebra algeb = (Algebra) cl.newInstance();
            
            String line;
            while((line = input.readLine()) != null) {
                line = line.trim();
                
                if(line.isEmpty()) {
                    continue;
                }
                
                TreeAutomaton ta = algeb.decompose(algeb.parseString(line));
                result.add(ta.toString());
            }
        }
        
        return result;
    }
    
    /**
     * 
     * @param in
     * @return 
     */
    public static Iterable<TreeAutomaton> reconstruct(Iterable<InputStream> in) {
        TreeAutomatonInputCodec taic = new TreeAutomatonInputCodec();
        
        return new FunctionIterable<>(in,(InputStream ip) -> {
            try {
                return taic.read(ip);
            } catch (CodecParseException | IOException ex) {
                throw new RuntimeException("Could not successfully construct automaton.");
            }
        });
    }
}
