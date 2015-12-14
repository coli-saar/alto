/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.IntersectionAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.ExtractJointTrees;
import de.up.ling.irtg.rule_finding.alignments.AddressAligner;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.create_automaton.CorpusCreator;
import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.Lexicalized;
import de.up.ling.irtg.rule_finding.pruning.intersection.NoEmpty;
import de.up.ling.irtg.rule_finding.pruning.intersection.arities.EnsureMTAArities;
import de.up.ling.irtg.rule_finding.pruning.intersection.arities.FindArities;
import de.up.ling.irtg.rule_finding.pruning.intersection.string.RightBranchingNormalForm;
import de.up.ling.irtg.rule_finding.pruning.intersection.tree.NoLeftIntoRight;
import de.up.ling.irtg.rule_finding.variable_introduction.JustXEveryWhere;
import de.up.ling.irtg.rule_finding.variable_introduction.LeftRightXFromFinite;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
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
public class CreateStringToTreeGeoquery {
    
    /**
     * 
     * @param args
     * @throws IOException
     * @throws ParserException 
     */
    public static void main(String... args) throws IOException, ParserException, Exception{
        Object2ObjectMap<String,IntSet> arities = FindArities.find(new FileInputStream(args[0]), 1);
        int maxArity = FindArities.max(arities);
        
        CorpusCreator.Factory fact = new CorpusCreator.Factory();
        fact.setFirstPruner(new IntersectionPruner<>((TreeAutomaton ta) -> {
            TreeAutomaton a = new RightBranchingNormalForm(ta.getSignature(), ta.getAllLabels());
            a = new IntersectionAutomaton(a, new Lexicalized(ta.getSignature(),ta.getAllLabels()));
            
            return a;
        }));
        fact.setSecondPruner(new IntersectionPruner<>((TreeAutomaton ta) -> {
            TreeAutomaton a = new NoLeftIntoRight(ta.getSignature(), ta.getAllLabels());
            a = new IntersectionAutomaton(a, new NoEmpty(ta.getSignature(),ta.getAllLabels()));
            a = new IntersectionAutomaton(a, new EnsureMTAArities(ta.getSignature(), ta.getAllLabels(), maxArity, arities));
            
            return a;
        }));
        fact.setFirstVariableSource(new LeftRightXFromFinite());
        fact.setSecondVariableSource(new JustXEveryWhere());
        
        Supplier<Algebra<List<String>>> st = () -> new StringAlgebra();
        Supplier<Algebra<Tree<String>>> mta = () -> new MinimalTreeAlgebra();
        
        SpanAligner.Factory ffact = new SpanAligner.Factory();
        AddressAligner.Factory sfact = new AddressAligner.Factory();
        
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
                    Logger.getLogger(CreateStringToTreeGeoquery.class.getName()).log(Level.SEVERE, null, ex);
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
