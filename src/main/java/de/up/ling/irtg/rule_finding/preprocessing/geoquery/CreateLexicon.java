/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing.geoquery;

import de.saar.basic.Pair;
import de.up.ling.irtg.util.FunctionIterable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author christoph_teichmann
 */
public class CreateLexicon {
    /**
     * 
     */
    private static final Pattern PREFIX_CONTENT = Pattern.compile("([^\\(\\)]+)\\(([^\\(\\)]+)\\).*");

    /**
     * 
     * @param s
     * @return 
     */
    public static boolean isSpecial(String s) {
        return s.matches(".*_____\\d+");
    }

    /**
     * 
     * @param type
     * @param code
     * @return 
     */
    public static String makeSpecial(String type, Integer code) {
        return type+"_____"+code;
    }
    
    /**
     * 
     */
    private final Set<Pair<String,Pair<String,String>>> reducePattern = new HashSet<>();
    
    /**
     * 
     * @param geobase
     * @throws java.io.IOException 
     */
    public CreateLexicon(InputStream geobase) throws IOException {
        boolean isComment = false;
        
        try(BufferedReader input = new BufferedReader(new InputStreamReader(geobase))) {
            
            String line;
            
            while((line = input.readLine()) != null) {
                line = line.toLowerCase().trim();
                if(isComment) {
                    if(line.matches(".*\\*\\/.*")) {
                        isComment = false;
                    }
                    
                    continue;
                }
                
                if(line.matches(".*/\\*.*")) {
                    isComment = true;
                    continue;
                }
                
                if(line.equals("")) {
                    continue;
                }
                
                Matcher match = PREFIX_CONTENT.matcher(line);
                match.matches();
                
                String prefix = match.group(1).trim();
                String content = match.group(2).trim();
                
                switch(prefix) {
                    case "state":
                        addState(content);
                        break;
                    case "city":
                        addCity(content);
                        break;
                    case "river":
                        addRiver(content);
                        break;
                    case "border":
                        addBorder(content);
                        break;
                    case "highlow":
                        addHighLow(content);
                        break;
                    case "mountain":
                        addMountain(content);
                        break;
                    case "road":
                        addRoad(content);
                        break;
                    case "lake":
                        addLake(content);
                        break;
                    case "country":
                        addCountry(content);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown Predicate Type");
                }
            }
        }
    }

    /**
     * 
     * @param trim 
     */
    private void addState(String content) {
        String[] parts = content.split(",");
        
        String state = reduce(parts[0]);
        String abbrev = reduce(parts[1]);
        
        String capital = reduce(parts[2]);
        
        String city1 = reduce(parts[6]);
        String city2 = reduce(parts[7]);
        String city3 = reduce(parts[8]);
        String city4 = reduce(parts[9]);
        
        this.reducePattern.add(new Pair<>(state, new Pair<>(state,"state")));
        this.reducePattern.add(new Pair<>(state, new Pair<>(abbrev,"abbrev")));
        
        this.reducePattern.add(new Pair<>(abbrev, new Pair<>(state,"state")));
        this.reducePattern.add(new Pair<>(abbrev, new Pair<>(abbrev,"abbrev")));
        
        this.reducePattern.add(new Pair<>(capital, new Pair<>(capital,"city")));
        this.reducePattern.add(new Pair<>(city1, new Pair<>(city1,"city")));
        this.reducePattern.add(new Pair<>(city2, new Pair<>(city2,"city")));
        this.reducePattern.add(new Pair<>(city3, new Pair<>(city3,"city")));
        this.reducePattern.add(new Pair<>(city4, new Pair<>(city4,"city")));
    }
    
    /**
     * 
     * @param trim 
     */
    private void addCity(String content) {
        String[] parts = content.split(",");
        
        String state = reduce(parts[0]);
        String abbrev = reduce(parts[1]);
        
        String name = reduce(parts[2]);
        
        this.reducePattern.add(new Pair<>(state, new Pair<>(state,"state")));
        this.reducePattern.add(new Pair<>(state, new Pair<>(abbrev,"abbrev")));
        
        this.reducePattern.add(new Pair<>(abbrev, new Pair<>(state,"state")));
        this.reducePattern.add(new Pair<>(abbrev, new Pair<>(abbrev,"abbrev")));
        
        this.reducePattern.add(new Pair<>(name, new Pair<>(name,"city")));
    }

    /**
     * 
     * @param content 
     */
    private void addRiver(String content) {
        String[] parts = content.split(",");
        
        String name = reduce(parts[0]);
        this.reducePattern.add(new Pair<>(name, new Pair<>(name,"river")));
        
        for(int i=2;i<parts.length;++i) {
            String state = reduce(parts[i]);
            
            this.reducePattern.add(new Pair<>(state, new Pair<>(state,"state")));
        }
    }

    /**
     * 
     * @param content 
     */
    private void addBorder(String content) {
        String[] parts = content.split(",");
        
        String state = reduce(parts[0]);
        String abbrev = reduce(parts[1]);
        
        this.reducePattern.add(new Pair<>(state, new Pair<>(state,"state")));
        this.reducePattern.add(new Pair<>(state, new Pair<>(abbrev,"abbrev")));
        
        this.reducePattern.add(new Pair<>(abbrev, new Pair<>(state,"state")));
        this.reducePattern.add(new Pair<>(abbrev, new Pair<>(abbrev,"abbrev")));
        
        for(int i=2;i<parts.length;++i) {
            String stat = reduce(parts[i]);
            
            this.reducePattern.add(new Pair<>(stat, new Pair<>(stat,"state")));
        }
    }

    /**
     * 
     * @param content 
     */
    private void addHighLow(String content) {
        String[] parts = content.split(",");
        
        String state = reduce(parts[0]);
        String abbrev = reduce(parts[1]);
        
        this.reducePattern.add(new Pair<>(state, new Pair<>(state,"state")));
        this.reducePattern.add(new Pair<>(state, new Pair<>(abbrev,"abbrev")));
        
        this.reducePattern.add(new Pair<>(abbrev, new Pair<>(state,"state")));
        this.reducePattern.add(new Pair<>(abbrev, new Pair<>(abbrev,"abbrev")));
    }

    /**
     * 
     * @param content 
     */
    private void addMountain(String content) {
        String[] parts = content.split(",");
        
        String state = reduce(parts[0]);
        String abbrev = reduce(parts[1]);
        
        String mountain = reduce(parts[2]);
        
        this.reducePattern.add(new Pair<>(state, new Pair<>(state,"state")));
        this.reducePattern.add(new Pair<>(state, new Pair<>(abbrev,"abbrev")));
        
        this.reducePattern.add(new Pair<>(abbrev, new Pair<>(state,"state")));
        this.reducePattern.add(new Pair<>(abbrev, new Pair<>(abbrev,"abbrev")));
        
        this.reducePattern.add(new Pair<>(mountain, new Pair<>(mountain,"mount")));
    }

    /**
     * 
     * @param content 
     */
    private void addRoad(String content) {
        String[] parts = content.split(",");
        
        for(int i=1;i<parts.length;++i) {
            String stat = reduce(parts[i]);
            
            this.reducePattern.add(new Pair<>(stat, new Pair<>(stat,"state")));
        }
    }

    /**
     * 
     * @param content 
     */
    private void addLake(String content) {
        String[] parts = content.split(",");
        
        String name = reduce(parts[0]);
        this.reducePattern.add(new Pair<>(name, new Pair<>(name,"lake")));
        
        for(int i=2;i<parts.length;++i) {
            String stat = reduce(parts[i]);
            
            this.reducePattern.add(new Pair<>(stat, new Pair<>(stat,"state")));
        }
    }

    /**
     * 
     * @param part
     * @return 
     */
    private String reduce(String part) {
        part = part.replaceAll("\\[|\\]", "");
        part = part.trim();
        
        part = part.replaceAll("(^')|('$)", "");
        return part.trim();
    }
    
    /**
     * 
     * @param input
     * @return 
     */
    public Iterable<Pair<String,String>> replace(Iterable<Pair<String,String>> input) {
        List<Pair<String,Pair<String,String>>> list = new ArrayList<>();
        
        this.reducePattern.stream().forEach((p) -> {
            list.add(p);
        });
        
        list.sort((Pair<String,Pair<String,String>> i1,Pair<String,Pair<String,String>> i2) -> {
            int l1 = i1.getLeft().length();
            int l2 = i2.getLeft().length();
            
            int comp = -Integer.compare(l1, l2);
            if(comp != 0) {
                return comp;
            }
            
            l1 = i1.getRight().getLeft().length();
            l2 = i2.getRight().getLeft().length();
            
            return -Integer.compare(l1, l2);
        });
        
        return new FunctionIterable<>(input,(Pair<String,String> original) -> {
            String from = " "+original.getLeft().toLowerCase()+" ";
            String to = (" "+original.getRight().toLowerCase()+" ");
            
            from = from.replaceAll("\\s+", " ");
            to = to.replaceAll("\\s+", " ");
            
            
            Map<String,Integer> counter = new HashMap<>();
            
            for(Pair<String,Pair<String,String>> pattern : list) {
                String lPatt = ".*[ ']"+pattern.getLeft()+"[ '].*";
                String rPatt = ".*[ ']"+pattern.getRight().getLeft()+"[ '].*";
                
                if(from.matches(lPatt) && to.matches(rPatt)) {
                    String type = pattern.getRight().getRight();
                    Integer code = counter.get(type);
                    
                    if(code == null) {
                        code = 1;
                    } else {
                        code += 1;
                    }
                    counter.put(type, code);
                    
                    String encoding = makeSpecial(type, code);
                    
                    String fromPattern = "([ '])("+pattern.getLeft()+")([ '])";
                    String toPattern = "([ '])("+pattern.getRight().getLeft()+")([ '])";
                    
                    from = from.replaceAll(fromPattern, "$1"+encoding+"$3");
                    to = to.replaceFirst(toPattern, "$1"+encoding+"$3");
                }
            }
            
            from = from.replaceAll("\\s+", " ").trim();
            to = to.replaceAll("\\s+", " ").trim();
            
            return new Pair<>(from,to);
        });
    }

    /**
     * 
     * @param content 
     */
    private void addCountry(String content) {
        String[] parts = content.split(",");
        
        String name = reduce(parts[0]);
        this.reducePattern.add(new Pair<>(name, new Pair<>(name,"country")));
    }

    /**
     * 
     * @return 
     */
    public Set<Pair<String, Pair<String, String>>> getReducePattern() {
        return reducePattern;
    }
    
    /**
     * 
     * @return 
     */
    public SimpleCheck getCheck() {
        return new SimpleCheck();
    }
    
    
    /**
     * 
     */
    public class SimpleCheck implements Check {
        /**
         * 
         */
        private final List<String[]> patts;
        
        /**
         * 
         */
        public SimpleCheck() {
            Set<String> container = new HashSet<>();
            reducePattern.forEach((pair) -> container.add(pair.getLeft()));
            
            patts = new ArrayList<>();
            
            container.forEach((entry) -> {
                patts.add(entry.trim().split("\\s+"));
            });
            
            patts.sort((String[] patt1, String[] patt2) -> -Integer.compare(patt1.length, patt2.length));
        }
        
        /**
         * 
         * @param pos
         * @param arr
         * @return 
         */
        @Override
        public int knownPattern(int pos, String[] arr) {
            main : for(int i=0;i<this.patts.size();++i) {
                String[] patt = patts.get(i);
                
                if(patt.length+pos > arr.length) {
                    continue;
                }
                
                for(int j=0;j<patt.length;++j) {
                    if(!patt[j].equals(arr[pos+j])) {
                        continue main;
                    }
                }
                
                return patt.length;
            }
            
            return 0;
        }
    }
}
