/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.evaluation;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;
import de.up.ling.irtg.learning_rates.AdaGrad;
import de.up.ling.irtg.rule_finding.sampling.AdaptiveSampler;
import de.up.ling.irtg.rule_finding.sampling.rule_weighters.AutomatonWeighted;
import de.up.ling.irtg.rule_finding.sampling.RuleWeighting;
import de.up.ling.irtg.rule_finding.sampling.TreeSample;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.LongSupplier;
import org.apache.commons.math3.random.Well44497b;

/**
 *
 * @author christoph
 */
public class EvaluateSamplingFromRules {
   
   /**
    * 
    * @param args
    * @throws IOException 
    */
    public static void main(String... args) throws IOException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);

        String automataFolder = props.getProperty("automataFolder");
        String baseRate = props.getProperty("baseLearningRate");
        String resultFolder = props.getProperty("resultFolder");
        String resultPrefix = props.getProperty("resultPrefix");
        String normExp = props.getProperty("normalizationExponent");
        String normDiv = props.getProperty("normalizationDivisor");
        String populationSize = props.getProperty("populationSize");
        String rounds = props.getProperty("rounds");
        String seed = props.getProperty("seed");
        String repetitions = props.getProperty("repetitions");
        
        Well44497b seeds = new Well44497b(Long.parseLong(seed));
        LongSupplier seeder = () -> {
            return seeds.nextLong();
        };
        
        File baseFold = new File(resultFolder);
        baseFold.mkdirs();
        
        File sources = new File(automataFolder);
        File[] auts = sources.listFiles();
        
        TreeAutomatonInputCodec taic = new TreeAutomatonInputCodec();
        for(int automaton=0;automaton<auts.length;++automaton) {
            File aut = auts[automaton];
            if(!aut.getName().matches(".*\\.auto")) {
                continue;
            }
            
            TreeAutomaton<String> ta;
            try(FileInputStream fis = new FileInputStream(aut)) {
                ta = taic.read(fis);
            }
            
            Function<TreeAutomaton,RuleWeighting> f = (TreeAutomaton q) -> {
                AdaGrad ada = new AdaGrad(Double.parseDouble(baseRate));
                return new AutomatonWeighted(q, Integer.parseInt(normExp), Double.parseDouble(normDiv), ada);
            };
            
            AdaptiveSampler.Configuration config = new AdaptiveSampler.Configuration(f);
            config.setDeterministic(true);
            config.setPopulationSize(Integer.parseInt(populationSize));
            config.setRounds(Integer.parseInt(rounds));
            config.setSeeds(seeder);
            
            Pair<DoubleList,List<DoubleList>> results = makeInside(ta, config, Integer.parseInt(repetitions));
            
            String fileName = baseFold.getAbsolutePath()+File.separator+resultPrefix+"_"+aut.getName()+".stats";
            try(BufferedWriter out = new BufferedWriter(new FileWriter(fileName))) {
                DoubleList average = results.getLeft();
                List<DoubleList> single = results.getRight();
                
                for(int round=0;round<average.size();++round) {
                    if(round != 0) {
                        out.write(";");
                    }
                    out.write("round"+round);
                }
                
                
                for(int rep=0;rep<average.size();++rep) {
                    out.newLine();
                    for(int round=0;round<average.size();++round) {
                        if(round != 0) {
                            out.write(";");
                        }
                        out.write(""+single.get(rep).get(round));
                    }
                }
            }
        }
    }
    
    
    /**
     * 
     * @param <Type>
     * @param ta
     * @param config
     * @return 
     */
    public static <Type> DoubleList makeInside(TreeAutomaton<Type> ta, AdaptiveSampler.Configuration config) {
        DoubleArrayList dal = new DoubleArrayList();
        
        List<TreeSample<Rule>> lt = config.run(ta);
        
        for(TreeSample ts : lt) {
            double seen = 0.0;
            
            for(int i=0;i<ts.populationSize();++i) {
                seen += Math.exp(ts.getLogTargetWeight(i)-ts.getLogPropWeight(i));
            }
            
            dal.add(seen / ts.populationSize());
        }
        
        return dal;
    }
    
    /**
     * 
     * @param <Type>
     * @param ta
     * @param config
     * @param repetitions
     * @return 
     */
    public static <Type> Pair<DoubleList,List<DoubleList>> makeInside(TreeAutomaton<Type> ta, AdaptiveSampler.Configuration config, int repetitions) {
        List<DoubleList> r1 = new ArrayList<>();
        DoubleArrayList r2 = new DoubleArrayList();
        
        for(int rep=0;rep<repetitions;++rep) {
            DoubleList dl = makeInside(ta, config);
            
            r1.add(dl);
            for(int round=0;round<dl.size();++round) {
                if(rep==0) {
                    r2.add(dl.getDouble(round));
                } else {
                    r2.set(round, dl.getDouble(round)+r2.getDouble(round));
                }
            }
            
            System.out.println("finished repetition: "+rep);
        }
        
        for(int i=0;i<r2.size();++i) {
            r2.set(i, r2.getDouble(i) / r1.size());
        }
        
        return new Pair<>(r2,r1);
    }
}
