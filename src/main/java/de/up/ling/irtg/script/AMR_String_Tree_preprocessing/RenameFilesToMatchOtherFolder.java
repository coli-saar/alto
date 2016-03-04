/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.AMR_String_Tree_preprocessing;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author groschwitz
 */
public class RenameFilesToMatchOtherFolder {
    
    public static void main(String[] args) throws FileNotFoundException {
        
        File folder = new File(args[0]);
        File namesFolder = new File(args[1]);
        
        Map<Integer, String> id2Name = new HashMap<>();
        
        for (File file : namesFolder.listFiles()) {
            String name = file.getName();
            id2Name.put(Integer.parseInt(name.split("_")[0]), name);
        }
        
        for (File file : folder.listFiles()) {
            int id = Integer.parseInt(file.getName().substring("automaton_".length(), file.getName().length()-".auto".length()))-1;
            File targetFile = new File(folder.getAbsolutePath()+"/"+id2Name.get(id));
            file.renameTo(targetFile);
        }
        
    }
    
}
