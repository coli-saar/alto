/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.AMR_String_Tree_preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.stream.Collectors;

/**
 *
 * @author groschwitz
 */
public class RenameFilesWithCSV {
    
    public static void main(String[] args) throws FileNotFoundException {
        
        File folder = new File(args[0]);
        String[] csvLines = new BufferedReader(new FileReader(args[1])).lines().collect(Collectors.toList()).toArray(new String[0]);
        int column = Integer.parseInt(args[2]);
        
        for (File file : folder.listFiles()) {
            int id = Integer.parseInt(file.getName().substring("automaton_".length(), file.getName().length()-".auto".length()))-1;
            String name = String.valueOf(id);
            name += "_"+csvLines[id+1].split(",[ ]*")[column];
            File targetFile = new File(folder.getAbsolutePath()+"/"+name+".rtg");
            file.renameTo(targetFile);
        }
        
    }
    
}
