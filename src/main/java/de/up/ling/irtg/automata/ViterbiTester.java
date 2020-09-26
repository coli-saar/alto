package de.up.ling.irtg.automata;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.saar.basic.StringTools;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.SetAlgebra;
import de.up.ling.irtg.algebra.SubsetAlgebra;
import de.up.ling.irtg.util.FirstOrderModel;
import de.up.ling.tree.Tree;

public class ViterbiTester {
    public static void main(String[] args) throws Exception {
        ViterbiTester t = new ViterbiTester();
        t.testViterbiCaching();
    }

    public void testViterbiCaching() throws Exception {
        String FEATURES = "color+type+x1+x2+y1+y2+z1+z2+to+from+height+width+length+orientation";
        String grammar = StringTools.slurp(new FileReader("grammar.txt"));
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.fromString(grammar);
        Interpretation<Set<List<String>>> refI = (Interpretation<Set<List<String>>>) irtg.getInterpretation("ref");
        SetAlgebra refA = (SetAlgebra) refI.getAlgebra();
        String model = StringTools.slurp(new FileReader("model.txt"));
        refA.setModel(FirstOrderModel.read(new StringReader(model)));
        Interpretation<BitSet> semI = (Interpretation<BitSet>) irtg.getInterpretation("sem");
        SubsetAlgebra semA = (SubsetAlgebra) semI.getAlgebra();
        semA.readOptions(new StringReader(FEATURES));
        
        Set<BitSet> semInputs = new HashSet<>();
        for( String x : semInput.split(",") ) {
            semInputs.add(semA.parseString(x));
        }

        TreeAutomaton<BitSet> ta = semA.decompose(semInputs);
        Intersectable semO = semI.invhom(ta);

        TreeAutomaton<String> automaton = irtg.getAutomaton();
        Intersectable<Set<List<String>>> refO = refI.parse(refA.parseString(refInput));

        //TreeAutomaton<Pair<Pair<String, Set<List<String>>>, BitSet>> ta =
        TreeAutomaton auto = (TreeAutomaton) automaton.intersect(refO).intersect(semO);
        auto.makeAllRulesExplicit();

        TreeAutomaton concreteAuto = auto.asConcreteTreeAutomaton();
        Tree<String> bestTreeConcrete = concreteAuto.viterbi();
        Tree<String> bestTree = auto.viterbi();

        System.err.println("\nAnalysis of original automaton:");
        auto.analyze();

        System.err.println("\nAnalysis of concrete automaton:");
        concreteAuto.analyze();

        System.err.println(auto.accepts(bestTreeConcrete));
        System.err.println(auto.getWeight(bestTreeConcrete));

        System.err.printf("original tree [%f]: %s\n", auto.getWeight(bestTree), bestTree);
        System.err.printf("concrete tree [%f]: %s\n", concreteAuto.getWeight(bestTreeConcrete), bestTreeConcrete);

        assert(concreteAuto.getWeight(bestTreeConcrete) > 0.35);
        assert(auto.getWeight(bestTree) > 0.35);
    }

	private static final String refInput = "{row20-1-2-3-1-2}";
	private static final String semInput = "type+x1+y1+z1+length+orientation,type+x1+x2+y1+z1+z2,type+x2+y2+z2+length+orientation";
}


/**
 * Notes:
 * - One core problem is that the decomp automaton of SubsetAlgebra does not support top-down queries.
 *   This caused an error when InvHomAutomaton#<init> called its makeAllRulesExplicit.
 * - The best tree of the concrete automaton is accepted by the original automaton, with the correct weight.
 *   Thus the problem is somewhere in viterbi.
 */