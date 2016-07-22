/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps.geoquery;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.tree.Tree;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author christoph_teichmann
 */
public class ParseFile {
    
    /**
     * 
     * @param args
     * @throws IOException
     * @throws ParserException 
     */
    public static void main(String... args) throws IOException, ParserException {
        String grammarName = args[0];
        String inputFileName = args[1];
        String outputFileName = args[2];
        String lineToParse = args[3];
        String numberOfParses = args[4];
        
        int relevantLine = Integer.parseInt(lineToParse);
        int numParse = Integer.parseInt(numberOfParses);
        
        IrtgInputCodec iic = new IrtgInputCodec();
        InterpretedTreeAutomaton irtg = iic.read(new FileInputStream(grammarName));
        
        List<String> lines = new ArrayList<>();
        try(BufferedReader input = new BufferedReader(new FileReader(inputFileName))) {
            int pos = 0;
            String line;
            
            while((line = input.readLine()) != null) {
                line = line.trim();
                
                if(line.isEmpty()) {
                    pos = 0;
                } else {
                    if(pos == relevantLine) {
                        lines.add(line);
                    }
                    
                    ++pos;
                }
            }
        }
        
        System.out.println("Collected input Strings");
        Map<String,String> todo = new HashMap<>();
        try(BufferedWriter output = new BufferedWriter(new FileWriter(outputFileName))) {
            for(int i=0;i<lines.size();++i) {
                String line = lines.get(i);
                
                todo.clear();
                todo.put("FirstInput", line);
                
                TreeAutomaton ta = irtg.parse(todo);
                
                String finished;
                if(ta.isEmpty()) {
                    output.write(finished = "NULL");
                    output.newLine();
                } else {
                    Iterator<Tree<String>> kBest = ta.languageIterator();
                    
                    for(int k=0;k<numParse && kBest.hasNext();++k) {
                        Object result = irtg.getInterpretation("SecondInput").interpret(kBest.next());
                        output.write(finished = (result == null ? "NULL" : result.toString()));
                        output.newLine();
                    }
                }
                
                output.newLine();
                
                if(i % 100 == 0 && i != 0) {
                    System.out.println("Parsed "+(i+1)+" lines.");
                }
            }
        }
    }
}
