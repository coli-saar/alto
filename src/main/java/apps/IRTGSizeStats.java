/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

/**
 *
 * @author christoph_teichmann
 */
public class IRTGSizeStats {
    /**
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String... args) throws IOException {
        File folder = new File(args[0]);
        File[] inputs = folder.listFiles();
        
        IrtgInputCodec iic = new IrtgInputCodec();
        LongList sizes = new LongArrayList();
        
        File min = null;
        long minSize = Long.MAX_VALUE;
        File max = null;
        long maxSize = Long.MIN_VALUE;
        
        for(File file : inputs){
            InputStream ins = new FileInputStream(file);
            InterpretedTreeAutomaton ita = iic.read(ins);
            
            long size = ita.getAutomaton().countTrees();
            sizes.add(size);
            
            if(size > maxSize){
                maxSize = size;
                max = file;
            }
            
            if(size < minSize){
                minSize = size;
                min = file;
            }
        }
        
        Collections.sort(sizes);
        
        System.out.println("min: "+sizes.get(0));
        System.out.println("max: "+sizes.get(sizes.size()-1));
        
        int middle = sizes.size()/2;
        int firstQuartile = sizes.size()/4;
        int thirdQuartile = (sizes.size()*3)/4;
        
        System.out.println("median: "+sizes.get(middle));
        System.out.println("firstQuartile: "+sizes.get(firstQuartile));
        System.out.println("thirdQuartile: "+sizes.get(thirdQuartile));
        
        System.out.println("smallest: "+min);
        System.out.println("largest: "+max);
    }
}
