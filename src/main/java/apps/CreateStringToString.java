/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.ExtractJointTrees;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.create_automaton.CorpusCreator;
import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.string.RightBranchingNormalForm;
import de.up.ling.irtg.rule_finding.variable_introduction.LeftRightXFromFinite;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author christoph_teichmann
 */
public class CreateStringToString {
    
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
        fact.setSecondPruner(new IntersectionPruner<>((TreeAutomaton ta) -> {
            return new RightBranchingNormalForm(ta.getSignature(), ta.getAllLabels());
        }));
        fact.setFirstVariableSource(new LeftRightXFromFinite());
        fact.setSecondVariableSource(new LeftRightXFromFinite());
        
        Supplier<Algebra<List<String>>> st = () -> new StringAlgebra();
        Supplier<Algebra<List<String>>> mta = () -> new StringAlgebra();
        
        SpanAligner.Factory ffact = new SpanAligner.Factory();
        SpanAligner.Factory sfact = new SpanAligner.Factory();
        
        CorpusCreator cc = fact.getInstance(st, mta, ffact, sfact);
        ExtractJointTrees et = new ExtractJointTrees(cc);
        
        InputStream in = new FileInputStream(args[0]);
        Supplier<OutputStream> sup = new Supplier<OutputStream>(){
            /**
             * 
             */
            private int count = 0;
            
            /**
             * 
             */
            private final String name = args[1];
            
            
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
        
        double[] res = et.getAutomataAndMakeStatistics(in, sup);
        System.out.println("average: "+res[0]);
        System.out.println("min: "+res[1]);
        System.out.println("max: "+res[2]);
    }
}
