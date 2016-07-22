/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps;

import com.google.common.io.Files;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author christoph_teichmann
 */
public class RemoveEmpty {

    /**
     *
     * @param args
     * @throws java.io.IOException
     */
    public static void main(String... args) throws IOException{
        File files = new File(args[0]);
        TreeAutomatonInputCodec tic = new TreeAutomatonInputCodec();

        List<File> nonEmpty = new ArrayList<>();
        for (File file : files.listFiles()) {
            if (!file.getName().endsWith(".rtg")) {
                continue;
            }
            
            TreeAutomaton ta = tic.read(new FileInputStream(file));
            if(ta.countTrees() > 0) {
                nonEmpty.add(file);
            }
        }
        
        String prefix = args[1];
        
        for(File file : nonEmpty) {
            File cfile = new File(prefix+file.getName());
            
            Files.copy(file, cfile);
            
            File align = new File(file.getAbsolutePath().replaceAll("\\.rtg$", ".al"));
            File calign = new File(prefix+align.getName());
            
            Files.copy(align, calign);
        }
    }
}
