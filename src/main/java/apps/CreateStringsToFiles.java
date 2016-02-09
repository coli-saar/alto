/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps;

import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;
import de.up.ling.irtg.rule_finding.ExtractJointTrees;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.alignments.SpecifiedAligner;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.create_automaton.CorpusCreator;
import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.string.RightBranchingNormalForm;
import de.up.ling.irtg.rule_finding.variable_introduction.JustXEveryWhere;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author christoph_teichmann
 */
public class CreateStringsToFiles {
    /**
     * 
     * @param args
     * @throws IOException
     * @throws ParserException 
     */
    public static void main(String... args) throws IOException, ParserException{
        CorpusCreator.Factory fact = new CorpusCreator.Factory();
        fact.setFirstPruner(new IntersectionPruner<>((TreeAutomaton ta) -> {
            return new RightBranchingNormalForm(ta.getSignature(), ta.getAllLabels());
        }));

        fact.setFirstVariableSource(new JustXEveryWhere());
        fact.setSecondVariableSource(new JustXEveryWhere());
        
        CorpusCreator cc = fact.getInstance(null, null, null, null);
        ExtractJointTrees et = new ExtractJointTrees(cc);
        
        final List<File> files = new ArrayList<>();
        Object2IntMap<File> nums = new Object2IntOpenHashMap<>();
        
        
        File f = new File(args[1]);
        File[] fs = f.listFiles();
        for(File k : fs) {
            if(k.isFile() && k.getAbsolutePath().endsWith(".rtg")) {
                String s = k.getName();
                nums.put(k, Integer.parseInt(s.trim().split("_")[0].trim()));
                
                files.add(k);
            }
        }
        files.sort((File o1, File o2) -> Integer.compare(nums.getInt(o1), nums.getInt(o2)));
        
        List<Pair<String,String>> leff = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
            String line;
            
            while((line = br.readLine()) != null) {
                line = line.trim();
                String aline = br.readLine().trim();
                
                leff.add(new Pair<>(line,aline));
            }
        }
        
        Iterable<AlignedTrees> left = () -> {
            return new Iterator<AlignedTrees>() {
                private int pos = 0;
                
                @Override
                public boolean hasNext() {
                    return pos < leff.size();
                }

                @Override
                public AlignedTrees next() {
                    Pair<String,String> pa = leff.get(pos++);
                    StringAlgebra sal = new StringAlgebra();
                    
                    TreeAutomaton<StringAlgebra.Span> ta = sal.decompose(sal.parseString(pa.getLeft()));
                    SpanAligner span = new SpanAligner(pa.getRight(), ta);
                    
                    return new AlignedTrees(ta, span);
                }
            };
        };
        
        Iterable<AlignedTrees> right = () -> {
            return new Iterator<AlignedTrees>() {
                
                private int pos = 0;

                @Override
                public boolean hasNext() {
                    return pos < files.size();
                }

                @Override
                public AlignedTrees next() {
                    TreeAutomatonInputCodec tic = new TreeAutomatonInputCodec();
                    
                    TreeAutomaton ta;
                    File f = files.get(pos++);
                    
                    try (InputStream in = new FileInputStream(f)) {
                        ta = tic.read(in);
                    } catch (IOException ex) {
                        Logger.getLogger(CreateStringsToFiles.class.getName()).log(Level.SEVERE, null, ex);
                        return null;
                    }
                    
                    SpecifiedAligner<String> spec = new SpecifiedAligner<>(ta);
                    String s = f.getAbsolutePath().replaceAll("\\.rtg$", ".align");
                    
                    try(BufferedReader br = new BufferedReader(new FileReader(s))) {
                        String line;
                        while((line = br.readLine()) != null) {
                            String[] parts = line.split(" ");
                            String state = parts[0].trim();
                            
                            IntSet aligns = new IntOpenHashSet();
                            for(int i=1;i<parts.length;++i) {
                                aligns.add(Integer.parseInt(parts[i].trim()));
                            }
                            
                            spec.put(state, aligns);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(CreateStringsToFiles.class.getName()).log(Level.SEVERE, null, ex);
                        return null;
                    }
                    
                    AlignedTrees at = new AlignedTrees(ta, spec);
                    return at;
                }
            };
        };
        
        Supplier<OutputStream> sup = new Supplier<OutputStream>(){
            /**
             * 
             */
            private int count = 0;
            
            /**
             * 
             */
            private final String name = args[2];
            
            
            @Override
            public OutputStream get() {
                OutputStream out = null;
                try {
                    out = new FileOutputStream(name+"_"+(++count)+".irtg");
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(CreateStringToString.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                return out;
            }            
        };
        
        double[] res = et.getAutomataAndMakeStatistics(left, right, sup);
        System.out.println("average: "+res[0]);
        System.out.println("min: "+res[1]);
        System.out.println("max: "+res[2]);
    }
}
