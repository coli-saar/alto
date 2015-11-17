/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

/**
 *
 * @author christoph_teichmann
 */
public class MakeSplit {
    
    /**
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String... args) throws IOException{
       InputStream in = new FileInputStream(args[0]);
       OutputStream out1 = new FileOutputStream(args[1]);
       OutputStream out2 = new FileOutputStream(args[2]);
       
        makeSplit(in, out2, out2, args[3]);
    }
    
    
    /**
     * 
     * @param in
     * @param outFirst
     * @param outSecond
     * @param numberFirst
     * @throws IOException 
     */
    public static void makeSplit(InputStream in, OutputStream outFirst,
            OutputStream outSecond, String numberFirst) throws IOException{
        ArrayList<ArrayList<String>> entries = new ArrayList<>();
        try(BufferedReader input = new BufferedReader(new InputStreamReader(in))){
            String line;
            ArrayList<String> current = new ArrayList<>();
            
            while((line = input.readLine()) != null){
                line = line.trim();
                if(line.equals("")){
                    if(!current.isEmpty()){
                        entries.add(current);
                        current = new ArrayList<>();
                    }
                }else{
                    current.add(line);
                }
            }
        }
        
        int size = Integer.parseInt(numberFirst);
        
        int i = 0;
        
        try(BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outFirst))){
            for(;i<size;++i){
                dump(entries.get(i),out);
            }
        }
        
        try(BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outSecond))){
            for(;i<entries.size();++i){
                dump(entries.get(i),out);
            }
        }
    }

    /**
     * 
     * @param lines
     * @param out 
     */
    private static void dump(ArrayList<String> lines, BufferedWriter out) throws IOException {
        for(int i=0;i<lines.size();++i){
            out.write(lines.get(i));
            out.newLine();    
        }
        
        out.newLine();
    }
}
