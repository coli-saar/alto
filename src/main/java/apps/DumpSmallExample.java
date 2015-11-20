/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps;

import de.saar.basic.Pair;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.TreeAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.create_automaton.CorpusCreator;
import de.up.ling.irtg.rule_finding.create_automaton.HomomorphismManager;
import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.string.RightBranchingNormalForm;
import de.up.ling.irtg.rule_finding.variable_introduction.JustXEveryWhere;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 *
 * @author christoph_teichmann
 */
public class DumpSmallExample {
    /**
     * 
     * @param args
     * @throws ParserException
     * @throws IOException 
     */
    public static void main(String... args) throws ParserException, IOException{
                CorpusCreator.Factory fact = new CorpusCreator.Factory();
        fact.setFirstPruner(new IntersectionPruner<>((TreeAutomaton ta) -> new RightBranchingNormalForm(ta.getSignature(), ta.getAllLabels())))
                .setSecondPruner(new IntersectionPruner<>((TreeAutomaton ta) -> new RightBranchingNormalForm(ta.getSignature(), ta.getAllLabels())))
                .setFirstVariableSource(new JustXEveryWhere())
                .setSecondVariableSource(new JustXEveryWhere());
        
        Supplier<Algebra<List<String>>> supp = () -> new StringAlgebra();
        
        CorpusCreator cc = fact.getInstance(supp, supp, new SpanAligner.Factory(), new SpanAligner.Factory());
        
        ArrayList firstInputs = new ArrayList<>();
        firstInputs.add("Nightly John goes fishing for herring");
        firstInputs.add("Daily Mary goes fishing for salmon");
        
        ArrayList firstAlign = new ArrayList<>();
        firstAlign.add("0:1:0 0:1:1 1:2:4 2:3:3 3:4:6 4:5:7 5:6:5");
        firstAlign.add("0:1:1 1:2:3 2:3:2 3:4:5 4:5:6 5:6:4");
        
        ArrayList secondInputs = new ArrayList<>();
        secondInputs.add("Jede Nacht geht John Hering angeln");
        secondInputs.add("Täglich geht Mary Lachs angeln");
        
        ArrayList secondAlign = new ArrayList<>();
        secondAlign.add("0:1:0 1:2:1 2:3:3 3:4:4 4:5:5 5:6:6 5:6:7");
        secondAlign.add("0:1:1 1:2:2 2:3:3 3:4:4 4:5:5 4:5:6");
        
        Iterable<Pair<TreeAutomaton,HomomorphismManager>> result = cc.makeRuleTrees(firstInputs, secondInputs, firstAlign, secondAlign);
                
        Pair<TreeAutomaton,HomomorphismManager> one = result.iterator().next();
        
        Interpretation i1 = new Interpretation(new TreeAlgebra(), one.getRight().getHomomorphism1());
        Interpretation i2 = new Interpretation(new TreeAlgebra(), one.getRight().getHomomorphism2());
        
        InterpretedTreeAutomaton ita = new InterpretedTreeAutomaton(one.getLeft());
        ita.addInterpretation("first", i1);
        ita.addInterpretation("second", i2);
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("finished.irtg"))) {
            bw.write(ita.toString());
        }   
    }    
}
