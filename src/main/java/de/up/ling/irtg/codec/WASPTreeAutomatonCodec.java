/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author teichmann
 */
public class WASPTreeAutomatonCodec {

    /**
     *
     * @param in
     * @return
     * @throws IOException
     */
    public TreeAutomaton<String> read(InputStream in) throws IOException {
        ConcreteTreeAutomaton<String> cta = new ConcreteTreeAutomaton<>();

        int root = cta.addState("Query");
        cta.addFinalState(root);

        try (BufferedReader input = new BufferedReader(new InputStreamReader(in))) {
            String line;
            Set<String> doneLongLabel = new HashSet<>();

            int num = 0;
            String aux = "__AUXILIARY__";
            while ((line = input.readLine()) != null) {
                line = line.trim();

                String[] parts = line.split("\\s+");

                String lhs = parts[0].replaceFirst("\\*[nN]:", "").trim();

                if (!parts[1].equals("->")) {
                    throw new IllegalArgumentException("line: " + line + " does not match rule pattern.");
                }

                String label = null;
                String[] children = null;
                if (line.matches("[^>]*->\\s*\\(\\{\\s*'.*")) {
                    children = new String[0];

                    int start = line.indexOf("'");
                    int end = line.indexOf("'", start + 1);

                    label = "__QUOTE__"+(line.substring(start + 1, end).trim())+"__QUOTE__";
                } else if(line.matches("[^>]*->\\s*\\(\\{\\s*[^\\(\\)]+\\s+\\([^\\(\\)]*\\)\\s*\\}\\).*")) {
                    int from = line.indexOf("{");
                    int to = line.indexOf("(",from+1);
                    
                    label = line.substring(from+1, to).trim();
                    
                    from = to;
                    to = line.indexOf(")",from+1);
                    
                    String options = line.substring(from+1,to).trim();
                    children = transformChildren(children, options, doneLongLabel, cta);
                } else if(line.matches("[^>]*->\\s*\\(\\{\\s*[^\\(\\)]+\\s+\\([^\\(\\)]+\\s+\\([^\\(\\)]+\\)\\s*\\)\\s*\\}\\).*")) {
                    int from = line.indexOf("{");
                    int left1 = line.indexOf("(",from+1);
                    int left2 = line.indexOf("(",left1+1);
                    int right1 = line.indexOf(")",left2+1);
                    
                    label = line.substring(from+1, left1).trim();
                    String child = aux+(num++);
                    
                    String innerChildren = line.substring(left2+1, right1);
                    String innerLabel = line.substring(left1+1, left2).trim();
                    children = transformChildren(children, innerChildren, doneLongLabel, cta);
                    cta.addRule(cta.createRule(child, innerLabel, children));
                    
                    children = new String[] {child};
                } else if(line.matches("[^>]*->\\s*\\(\\s*\\{\\s*\\*t:Num\\s*\\}\\s*\\)\\s*")) {
                    children = new String[0];
                    label = "__NUMBER__";
                } else {
                    throw new IllegalArgumentException("Could not parse the line: "+line);
                }
                
                cta.addRule(cta.createRule(lhs, label, children));
            }
        }

        return cta;
    }

    /**
     * 
     * @param children
     * @param options
     * @param doneLongLabel
     * @param cta
     * @return 
     */
    private String[] transformChildren(String[] children, String options, Set<String> doneLongLabel, ConcreteTreeAutomaton<String> cta) {
        children = options.split(",");
        for(int i=0;i<children.length;++i) {
            children[i] = children[i].trim();
            
            if(children[i].matches("^\\*[nN]:.*")) {
                children[i] = children[i].replaceFirst("^\\*[nN]:", "").trim();
            } else {
                String nt = children[i].toUpperCase()+"__NT";
                
                if(!doneLongLabel.contains(children[i])) {
                    cta.addRule(cta.createRule(nt,children[i],new String[0]));
                    doneLongLabel.add(children[i]);
                }
                
                children[i] = nt;
            }
        }
        return children;
    }
}
