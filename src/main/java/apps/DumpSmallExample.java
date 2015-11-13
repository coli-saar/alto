/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps;

import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.AddressAligner;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.create_automaton.CorpusCreator;
import de.up.ling.irtg.rule_finding.pruning.PruneOneSideTerminating;
import de.up.ling.irtg.rule_finding.pruning.Pruner;
import de.up.ling.irtg.rule_finding.variable_introduction.JustXEveryWhere;
import de.up.ling.irtg.rule_finding.variable_introduction.LeftRightXFromFinite;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author christoph_teichmann
 */
public class DumpSmallExample {
   
    public static void main(String... args) throws ParserException, IOException{
                CorpusCreator.Factory fact = new CorpusCreator.Factory();
        fact.setFirstPruner(new PruneOneSideTerminating()).setSecondPruner(Pruner.DEFAULT_PRUNER)
                .setFirstVariableSource(new JustXEveryWhere())
                .setSecondVariableSource(new JustXEveryWhere());
        
        StringAlgebra sal1 = new StringAlgebra();
        StringAlgebra sal2 = new StringAlgebra();
        
        CorpusCreator cc = fact.getInstance(sal1, sal2, new SpanAligner.Factory(), new SpanAligner.Factory());
        
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
        
        List<TreeAutomaton> result = cc.makeRuleTrees(firstInputs, secondInputs, firstAlign, secondAlign);
        
        BufferedWriter bw = new BufferedWriter(new FileWriter("finished"));
        bw.write(result.get(0).toString());
        bw.close();
    }
    
    
}
