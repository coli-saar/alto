/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author christoph_teichmann
 */
public class JonasToNormal {
    /**
     * 
     * @param args
     * @throws IOException 
     */
    public static void main(String... args) throws IOException {
        File folder = new File(args[0]);
        File[] files = folder.listFiles();
        
        Int2ObjectMap<File> nums = new Int2ObjectOpenHashMap<>();
        
        for(File f : files) {
            if(!f.getName().matches(".*\\.al")) {
                continue;
            }
            
            String name = f.getName();
            
            int num = Integer.parseInt(name.split("_")[0].trim());
            nums.put(num, f);
        }
        
        int pos = 0;
        boolean first = true;
        try(BufferedReader bur = new BufferedReader(new FileReader(args[1]));
                BufferedWriter bw = new BufferedWriter(new FileWriter(args[2]))) {
            String line;
            
            while((line = bur.readLine()) != null) {
                int num = pos++;
                
                File f = nums.get(num);
                
                if(f == null || !f.getName().matches(".*\\.al")) {
                    continue;
                }
                
                String os = f.getAbsolutePath();
                os = os.replaceAll("\\.al$", ".align");
                
                try(BufferedWriter aligns = new BufferedWriter(new FileWriter(os))) {
                    if(first) {
                        first = false;
                    } else {
                        bw.newLine();
                    }
                    line = line.split("\\|\\|\\|")[0].trim();
                    bw.write(line);
                    bw.newLine();
                    int code = 0;
                    boolean ffirst = true;
                    try(BufferedReader in = new BufferedReader(new FileReader(f))) {
                        String oline;
                        
                        while((oline = in.readLine()) != null) {
                            if(ffirst) {
                                ffirst = false;
                            } else {
                                aligns.newLine();
                            }
                            
                            String[] als = oline.split("\\s+");
                            aligns.write(als[0]);
                            
                            for(int i=1;i<als.length;++i) {
                                int align = Integer.parseInt(als[i].trim());
                                bw.write(" ");
                                
                                
                                int cod = code++;
                                aligns.write(" ");
                                aligns.write(Integer.toString(cod));
                                
                                bw.write(""+align+":"+(align+1)+":"+(cod));
                            }
                        }
                    }
                }
            }
        }
    }
}
