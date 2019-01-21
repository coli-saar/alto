/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import de.saar.basic.Pair;
import de.saar.basic.StringTools;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.TemplateInterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.SetAlgebra;
import de.up.ling.irtg.algebra.SubsetAlgebra;
import static de.up.ling.irtg.algebra.SubsetAlgebra.SEPARATOR;
import de.up.ling.irtg.automata.MultiIntersectionAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedRule;
import de.up.ling.irtg.automata.pruning.FOM;
import de.up.ling.irtg.automata.pruning.MultiFOM;
import de.up.ling.irtg.codec.TemplateIrtgInputCodec;
import de.up.ling.irtg.semiring.Semiring;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import de.up.ling.irtg.util.FirstOrderModel;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jgrapht.GraphPath;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.FloydWarshallShortestPaths;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

/**
 *
 * @author koller
 */
public class SurfaceRealizer {

    private static JCommander jc;
    private static CmdLineParameters param = new CmdLineParameters();
    private static final boolean DEBUG = false;

    public static void main(String[] args) throws Exception {
        jc = JCommander.newBuilder().addObject(param).build();

        try {
            jc.parse(args);
        } catch (ParameterException ex) {
            cmdlineUsage(ex.getMessage());
        }

        if (param.help) {
            cmdlineUsage(null);
        }

        if (param.filenames.size() < 2) {
            cmdlineUsage("Please specify both a grammar and a model.");
        }

        FirstOrderModel model = FirstOrderModel.read(new FileReader(param.filenames.get(1)));
        InterpretedTreeAutomaton irtg = null;

        if (param.filenames.get(0).endsWith(".tirtg")) {
            TemplateInterpretedTreeAutomaton tirtg = new TemplateIrtgInputCodec().read(new FileInputStream(args[0]));
            irtg = tirtg.instantiate(model);

            if (param.debugTirtgFilename != null) {
                Files.write(Paths.get(param.debugTirtgFilename), irtg.toString().getBytes());
            }
        } else {
            irtg = InterpretedTreeAutomaton.read(new FileInputStream(param.filenames.get(0)));
        }

        SubsetAlgebra<String> sem = (SubsetAlgebra) irtg.getInterpretation("sem").getAlgebra();
        Interpretation semI = irtg.getInterpretation("sem");

        SetAlgebra ref = (SetAlgebra) irtg.getInterpretation("ref").getAlgebra();
        Interpretation refI = irtg.getInterpretation("ref");

        Algebra str = irtg.getInterpretation("string").getAlgebra();

        // put true facts here
        ref.setModel(model);
        List<String> trueAtoms = Util.mapToList(model.getTrueAtoms(), t -> t.toString());
        sem.setOptions(StringTools.join(trueAtoms, SEPARATOR));

        // put inputs here
        Set<List<String>> refInput = ref.parseString(param.ref);
        BitSet semInput = sem.parseString(param.sem);

        // for FOM calculation
        FloydWarshallShortestPaths fw = computeShortestPaths(model);
        Object2IntMap<String> numDistractors = computeNumReasonableDistractors(model);
//        System.err.printf("numDistractors: %s\n", numDistractors);

        TreeAutomaton<?> chart = null;

        CpuTimeStopwatch totalTime = new CpuTimeStopwatch();

        for (int i = 0; i < param.warmupIterations + param.avgIterations; i++) {
            CpuTimeStopwatch sw = new CpuTimeStopwatch();
            sw.record();

            // start recording total runtime after warmup iterations
            if (i == param.warmupIterations) {
                totalTime.record();
                System.err.println("Finished warming up.");
            }

            // with multi intersection
            TreeAutomaton<BitSet> semInvhom = (TreeAutomaton) semI.parse(semInput);
            TreeAutomaton<Set<List<String>>> refInvhom = (TreeAutomaton) refI.parse(refInput);
            MultiFOM fom = makeFom(semInvhom, refInvhom, semInput, refInput, sem, fw, numDistractors, irtg.getAutomaton());

            List<TreeAutomaton> rightAutomata = Lists.newArrayList(refInvhom, semInvhom);
            MultiIntersectionAutomaton c = new MultiIntersectionAutomaton(irtg.getAutomaton(), rightAutomata, fom);
            c.setStopWhenFinalStateFound(true);
            c.makeAllRulesExplicit();

            chart = c;

            /*
            // with regular intersection
            
            TreeAutomaton<Pair<String, Set<List<String>>>> afterRef = irtg.getAutomaton().intersect(refI.parse(refInput));

            sw.record();

            afterRef = afterRef.reduceTopDown();
            sw.record();

            Map<Integer, Integer> refDistance = afterRef.evaluateInSemiringTopDown(new DistanceSemiring(), new RuleEvaluatorTopDown<Integer>() {
                                                                               public Integer initialValue() {
                                                                                   return 0;
                                                                               }

                                                                               public Integer evaluateRule(Rule rule, int i) {
                                                                                   return 1;
                                                                               }
                                                                           });

            sw.record();

//            Files.write(Paths.get("after-ref.auto"), afterRef.toString().getBytes());
//            System.err.printf("ref done, %s\n", Util.formatTimeSince(start));
            TreeAutomaton<BitSet> invhom = (TreeAutomaton) semI.parse(semInput);
            FOM fom = makeFom(invhom, afterRef, semInput, refDistance, sem);
            IntersectionAutomaton afterSem = new IntersectionAutomaton(afterRef, invhom, fom);
            afterSem.setStopWhenFinalStateFound(true);
            afterSem.makeAllRulesExplicit();

            sw.record();
//            sw.printMilliseconds("ref chart", "ref reduce", "ref distance", "sem chart");

            chart = afterSem;
             */
        }

        totalTime.record();

        System.err.printf("chart construction time, averaged over %d iterations: %s\n", param.avgIterations, Util.formatTime(totalTime.getTimeBefore(1) / param.avgIterations));

        if (param.printChartFilename != null) {
            Files.write(Paths.get(param.printChartFilename), chart.toString().getBytes());
        }

        System.out.println();

        int count = 1;
        for (Tree<String> dt : chart.languageIterable()) {
            System.out.printf("[%03d] %s\n", count, str.representAsString(irtg.interpret(dt, "string")));
            if (param.printDerivations) {
                System.out.printf("      <deriv = %s>\n\n", dt);
            }

            count++;
        }
    }

    private static MultiFOM makeFom(TreeAutomaton<BitSet> semInvhom, TreeAutomaton<Set<List<String>>> refInvhom,
            BitSet targetSem, Set<List<String>> targetRef,
            SubsetAlgebra<String> sem,
            FloydWarshallShortestPaths fw, Object2IntMap<String> numDistractors,
            TreeAutomaton<String> drtg) {
        // right[0] = ref
        // right[1] = sem

        return new MultiFOM() {
            @Override
            public double evaluate(Rule left, CondensedRule right) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public double evaluateStates(int leftStateId, int[] rightStates) {
                BitSet rightBitset = semInvhom.getStateForId(rightStates[1]);

                BitSet unrealized = new BitSet();
                unrealized.or(targetSem);
                unrealized.andNot(rightBitset);
                int numUnrealized = unrealized.cardinality();

                String leftState = drtg.getStateForId(leftStateId);
                String intendedReferent = leftState.substring(leftState.lastIndexOf('_') + 1);
                int distance = 0;
                for (List<String> tr : targetRef) {
                    for (String t : tr) {
                        distance += getDistance(t, intendedReferent, fw);
                    }
                }

                Set<List<String>> ref = refInvhom.getStateForId(rightStates[0]);
                int numReasonableDistractors = numDistractors.getInt(intendedReferent);
                int numDistractors = Math.min(numReasonableDistractors, ref.size()) - 1;
                
                double value = 100000 * numUnrealized + distance + numDistractors; // lexicographic sort by (numUnrealized, numExtra)

                if (DEBUG) {
                    System.err.printf("evaluate %s + %s + %s -> %f\n", drtg.getStateForId(leftStateId), refInvhom.getStateForId(rightStates[0]), sem.toSet(rightBitset), value);
                }

                return value;
            }
        };
    }

    private static FOM makeFom(TreeAutomaton<BitSet> semInvhom, TreeAutomaton<Pair<String, Set<List<String>>>> chartAfterRef, BitSet targetSem, Map<Integer, Integer> refDistance, SubsetAlgebra<String> sem) {
        return new FOM() {
            @Override
            public double evaluate(Rule left, CondensedRule right) {
                return 0; // not needed
            }

            @Override
            public double evaluateStates(int leftState, int rightState) {
                BitSet rightBitset = semInvhom.getStateForId(rightState);

                BitSet unrealized = new BitSet();
                unrealized.or(targetSem);
                unrealized.andNot(rightBitset);
                int numUnrealized = unrealized.cardinality();

                BitSet extraSem = new BitSet();
                extraSem.or(rightBitset);
                extraSem.andNot(targetSem);
                int numExtra = extraSem.cardinality();

//                Set<List<String>> ref = chartAfterRef.getStateForId(leftState).getRight();
//                int numDistractors = ref.size() - 1;
                int dist = refDistance.get(leftState);

                double value = 100000 * numUnrealized + 1000 * dist + numExtra; // lexicographic sort by (numUnrealized, numExtra)

//                System.err.printf("evaluate %s + %s -> %f\n", chartAfterRef.getStateForId(leftState), sem.toSet(rightBitset), value);
                return value;
            }
        };
    }

    private static class DistanceSemiring implements Semiring<Integer> {

        @Override
        public Integer add(Integer x, Integer y) {
            return Math.min(x, y);
        }

        @Override
        public Integer multiply(Integer x, Integer y) {
            return x + y;
        }

        @Override
        public Integer zero() {
            return Integer.MAX_VALUE;
        }

    }

    private static void cmdlineUsage(String errorMessage) {
        if (jc != null) {
            if (errorMessage != null) {
                System.out.println(errorMessage);
            }

            jc.setProgramName("java -cp <alto.jar> de.up.ling.irtg.script.SurfaceRealizer <grammar_filename> <model_filename>");
            jc.usage();
        }

        if (errorMessage != null) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }

    private static class CmdLineParameters {

        @Parameter
        public List<String> filenames = new ArrayList<>();

        @Parameter(names = "--ref", required = true, description = "Set of individual that should be referred to.")
        public String ref = null;

        @Parameter(names = "--sem", required = true, description = "Semantic atoms that should be expressed.")
        public String sem = null;

        @Parameter(names = "--warmup", description = "Number of warmup iterations.")
        public int warmupIterations = 0;

        @Parameter(names = "--avg", description = "Number of iterations over which runtime is averaged.")
        public int avgIterations = 1;

        @Parameter(names = "--debug-tirtg", description = "Write instantiated version of the template IRTG to this filename.")
        public String debugTirtgFilename = null;

        @Parameter(names = "--print-chart", description = "Write the chart to this filename.")
        public String printChartFilename = null;

        @Parameter(names = "--print-derivations", description = "Display derivation trees.")
        public boolean printDerivations = false;

        @Parameter(names = "--help", help = true, description = "Prints usage information.")
        private boolean help;
    }

    private static FloydWarshallShortestPaths computeShortestPaths(FirstOrderModel model) {
        UndirectedGraph<String, DefaultEdge> g = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);

        for (String a : model.getUniverse()) {
            g.addVertex(a);
        }

        for (Tree<String> atom : model.getTrueAtoms()) {
            if (atom.getChildren().size() == 2) {
                String s = atom.getChildren().get(0).getLabel();
                String t = atom.getChildren().get(1).getLabel();

                g.addEdge(s, t);
            }
        }

        System.err.println(g);

        return new FloydWarshallShortestPaths(g);
    }

    private static int getDistance(String s, String t, FloydWarshallShortestPaths fw) {
//        

        if (s.equals(t)) {
//            System.err.println("   (= 0)");
            return 0;
        } else {
            GraphPath shortestPath = fw.getShortestPath(s, t);
            int ret = 1000000; // very high number

            if (shortestPath != null) {
                return shortestPath.getEdgeList().size();
            }

            if (DEBUG) {
                System.err.printf("distance(%s -> %s) = %d\n", s, t, ret);
            }
            return ret;
        }
    }
    
    private static Object2IntMap<String> computeNumReasonableDistractors(FirstOrderModel model) {
        Object2IntMap<String> ret = new Object2IntOpenHashMap<>();
        Multiset<String> atomCounts = HashMultiset.create();
        
        for( String x : model.getUniverse() ) {
            ret.put(x, model.getUniverse().size());
        }
        
        for( Tree<String> atom : model.getTrueAtoms() ) {
            atomCounts.add(atom.getLabel());
        }
        
        for( Tree<String> atom : model.getTrueAtoms() ) {
            if( atom.getChildren().size() == 1 ) {
                int count = atomCounts.count(atom.getLabel());
                String x = atom.getChildren().get(0).getLabel();
                if( count < ret.get(x) ) {
                    ret.put(x, count);
                }
            }
        }
        
        return ret;
    }

}
