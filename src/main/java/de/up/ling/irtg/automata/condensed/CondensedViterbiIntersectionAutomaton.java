/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import com.google.common.base.Function;
import de.saar.basic.CartesianIterator;
import de.saar.basic.Pair;
import de.up.ling.irtg.AntlrIrtgBuilder;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.*;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.SignatureMapper;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;


/*
 TODO: 
 - collectStatePairs: remove CartesianIterator, use IntCartesianIterator to avoid boxing
 */
/**
 *
 * @author koller
 */
public class CondensedViterbiIntersectionAutomaton<LeftState, RightState> extends TreeAutomaton<Pair<LeftState, RightState>> {

    private TreeAutomaton<LeftState> left;
    private CondensedTreeAutomaton<RightState> right;
    private static final boolean DEBUG = false;
//    private Int2IntMap stateToLeftState;
//    private Int2IntMap stateToRightState;
    private long[] ckyTimestamp = new long[10];
    private final SignatureMapper leftToRightSignatureMapper;
    private final Int2DoubleMap viterbiStateMap;        ///< Maps a state from this automaton to a probability
    private final Int2ObjectMap<Rule> viterbiRuleMap;   ///< Maps a state to its best rule


    private final IntInt2IntMap stateMapping;
//    private final List<Pair<Integer,Integer>> outputStates;
//    private int nextOutputStateId;

    public CondensedViterbiIntersectionAutomaton(TreeAutomaton<LeftState> left, CondensedTreeAutomaton<RightState> right, SignatureMapper sigMapper) {
        super(left.getSignature()); // TODO = should intersect this with the (remapped) right signature

        this.leftToRightSignatureMapper = sigMapper;

        this.left = left;
        this.right = right;

//        stateToLeftState = new Int2IntOpenHashMap();
//        stateToRightState = new Int2IntOpenHashMap();
        finalStates = null;

        stateMapping = new IntInt2IntMap();
        
        viterbiStateMap = new Int2DoubleOpenHashMap();
        viterbiStateMap.defaultReturnValue(0.0); // if a state is not in this map, return 0
        
        viterbiRuleMap = new Int2ObjectOpenHashMap<>();
//        outputStates = new ArrayList<Pair<Integer, Integer>>();
//        outputStates.add(null);
//        nextOutputStateId = 1;
    }

    /**
     * Translates a label ID of the left automaton (= of the intersection
     * automaton) to the label ID of the right automaton for the same label.
     * Returns 0 if the right automaton does not define this label.
     *
     * @param leftLabelId
     * @return
     */
//    private int remapLabel(int leftLabelId) {
//        return leftToRightLabelRemap[leftLabelId];
//    }
    @Override
    public boolean isBottomUpDeterministic() {
        return left.isBottomUpDeterministic() && right.isBottomUpDeterministic();
    }

    public void makeAllRulesExplicitCondensedCKY() {
        if (!isExplicit) {
            isExplicit = true;
            getStateInterner().setTrustingMode(true);

            ckyTimestamp[0] = System.nanoTime();

//            int[] oldLabelRemap = labelRemap;
//            labelRemap = right.getSignature().remap(left.getSignature());
            Int2ObjectMap<IntSet> partners = new Int2ObjectOpenHashMap<IntSet>();

            ckyTimestamp[1] = System.nanoTime();

            // Perform a DFS in the right automaton to find all partner states
            IntSet visited = new IntOpenHashSet();
            for (int q : right.getFinalStates()) {
                ckyDfsForStatesInBottomUpOrder(q, visited, partners);
            }
            
            // Viterbi: Store all rules.
            viterbiRuleMap.values().forEach(this::storeRule);

            // force recomputation of final states
            finalStates = null;

            ckyTimestamp[2] = System.nanoTime();

//            System.err.println("-> " + getAllStates().size());
            if (DEBUG) {
                for (int i = 1; i < ckyTimestamp.length; i++) {
                    if (ckyTimestamp[i] != 0 && ckyTimestamp[i - 1] != 0) {
                        System.err.println("CKY runtime " + (i - 1) + " ??? " + i + ": "
                                + (ckyTimestamp[i] - ckyTimestamp[i - 1]) / 1000000 + "ms");
                    }
                }
                System.err.println("Intersection automaton CKY:\n" + toString() + "\n~~~~~~~~~~~~~~~~~~");
            }
//            labelRemap = oldLabelRemap;
        }
    }

    private void ckyDfsForStatesInBottomUpOrder(int q, IntSet visited, final Int2ObjectMap<IntSet> partners) {
        if (!visited.contains(q)) {
            visited.add(q);
            for (final CondensedRule rightRule : right.getCondensedRulesByParentState(q)) {
                int[] rightChildren = rightRule.getChildren();
                List<IntSet> remappedChildren = new ArrayList<>();

                // iterate over all children in the right rule
                for (int i = 0; i < rightRule.getArity(); ++i) {
                    ckyDfsForStatesInBottomUpOrder(rightChildren[i], visited, partners);

                    // take the right-automaton label for each child and get the previously calculated left-automaton label from partners.
                    remappedChildren.add(partners.get(rightChildren[i]));
                }

                left.foreachRuleBottomUpForSets(rightRule.getLabels(right), remappedChildren, leftToRightSignatureMapper, new Function<Rule, Void>() {
                    @Override
                    public Void apply(final Rule leftRule) {
                        Rule rule = combineRules(leftRule, rightRule);
                        
                        // -- Viterbi
                        int newState = rule.getParent();
                        double ruleWeight = rule.getWeight();
                        
                        int[] children = rule.getChildren();
                        double childWeight = 0.0;
                        
                        // multiply the weight of all childs of the rule
                        for (int i = 0; i < children.length; i++) {
                            childWeight *= viterbiStateMap.get(children[i]);
                        }
                        
                        // write in maps
                        if (viterbiStateMap.get(newState) < ruleWeight * childWeight) {
                            // current rule is better!
                            viterbiRuleMap.put(newState, rule);
                            viterbiStateMap.put(newState, ruleWeight * childWeight);
                        }
                        // -- Viterbi

                        IntSet knownPartners = partners.get(rightRule.getParent());

                        if (knownPartners == null) {
                            knownPartners = new IntOpenHashSet();
                            partners.put(rightRule.getParent(), knownPartners);
                        }

                        knownPartners.add(leftRule.getParent());

                        return null;
                    }
                });

            }
        }
    }

    // bottom-up intersection algorithm
    @Override
    public void makeAllRulesExplicit() {
        makeAllRulesExplicitCondensedCKY();

    }

    private int addStatePair(int leftState, int rightState) {
        int ret = stateMapping.get(leftState, rightState);

        if (ret == 0) {
            ret = addState(new Pair(left.getStateForId(leftState), right.getStateForId(rightState)));
            stateMapping.put(leftState, rightState, ret);
        }

        return ret;
    }

    Rule combineRules(Rule leftRule, CondensedRule rightRule) {
        int[] childStates = new int[leftRule.getArity()];

        for (int i = 0; i < leftRule.getArity(); i++) {
            childStates[i] = addStatePair(leftRule.getChildren()[i], rightRule.getChildren()[i]);
        }

        int parentState = addStatePair(leftRule.getParent(), rightRule.getParent());

        return createRule(parentState, leftRule.getLabel(), childStates, leftRule.getWeight() * rightRule.getWeight());
    }

    @Override
    public IntSet getFinalStates() {
        if (finalStates == null) {
//            System.err.println("compute final states");
            getAllStates(); // initialize data structure for addState

//            System.err.println("left final states: " + left.getFinalStates() + " = " + left.stateInterner.resolveIds(left.getFinalStates()));
//            System.err.println("right final states: " + right.getFinalStates() + " = " + right.stateInterner.resolveIds(right.getFinalStates()));
            finalStates = new IntOpenHashSet();
            collectStatePairs(left.getFinalStates(), right.getFinalStates(), finalStates);
        }

        return finalStates;
    }

    private void collectStatePairs(IntSet leftStates, IntSet rightStates, IntSet pairStates) {
        for (int leftState : leftStates) {
            for (int rightState : rightStates) {
                int state = stateMapping.get(leftState, rightState);

                if (state != 0) {
                    pairStates.add(state);
                }
            }
        }
    }

    @Override
    public Set<Rule> getRulesBottomUp(int label, int[] childStates) {
        makeAllRulesExplicit();

        assert useCachedRuleBottomUp(label, childStates);

        return getRulesBottomUpFromExplicit(label, childStates);
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int label, int parentState) {
        makeAllRulesExplicit();

        assert useCachedRuleTopDown(label, parentState);

        return getRulesTopDownFromExplicit(label, parentState);
    }

    
    private static class IntInt2IntMap {

        private final Int2ObjectMap<Int2IntMap> map;

        public IntInt2IntMap() {
            map = new Int2ObjectOpenHashMap<Int2IntMap>();
        }

        public int get(int x, int y) {
            Int2IntMap m = map.get(x);

            if (m == null) {
                return 0;
            } else {
                return m.get(y);
            }
        }

        public void put(int x, int y, int value) {
            Int2IntMap m = map.get(x);

            if (m == null) {
                m = new Int2IntOpenHashMap();
                map.put(x, m);
            }

            m.put(y, value);
        }
    }

    /**
     * Arg1: IRTG Grammar Arg2: List of Sentences Arg3: Interpretation to parse
     * Arg4: Outputfile Arg5: Comments
     *
     * @param args
     */
    public static void main(String[] args) throws FileNotFoundException, ParseException, IOException, ParserException, AntlrIrtgBuilder.ParseException {
        if (args.length != 5) {
            System.err.println("1. IRTG\n"
                    + "2. Sentences\n"
                    + "3. Interpretation\n"
                    + "4. Output file\n"
                    + "5. Comments");
            System.exit(1);
        }

        String irtgFilename = args[0];
        String sentencesFilename = args[1];
        String interpretation = args[2];
        String outputFile = args[3];
        String comments = args[4];

        // initialize CPU-time benchmarking
        long[] timestamp = new long[4];
        ThreadMXBean benchmarkBean = ManagementFactory.getThreadMXBean();
        boolean useCPUTime = benchmarkBean.isCurrentThreadCpuTimeSupported();
        if (useCPUTime) {
            System.err.println("Using CPU time for measuring the results.");
        }

        System.err.print("Reading the IRTG...");

        updateBenchmark(timestamp, 0, useCPUTime, benchmarkBean);

//        timestamp[0] = System.nanoTime();
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileReader(new File(irtgFilename)));
        Interpretation interp = irtg.getInterpretation(interpretation);
        Homomorphism hom = interp.getHomomorphism();
        Algebra alg = irtg.getInterpretation(interpretation).getAlgebra();

        updateBenchmark(timestamp, 1, useCPUTime, benchmarkBean);

        System.err.println(" Done in " + ((timestamp[1] - timestamp[0]) / 1000000) + "ms");
        try {
            File oFile = new File(outputFile);
            FileWriter outstream = new FileWriter(oFile);
            BufferedWriter out = new BufferedWriter(outstream);
            out.write("Testing IntersectionAutomaton with condensed intersection ...\n"
                    + "IRTG-File  : " + irtgFilename + "\n"
                    + "Input-File : " + sentencesFilename + "\n"
                    + "Output-File: " + outputFile + "\n"
                    + "Comments   : " + comments + "\n"
                    + "CPU-Time   : " + useCPUTime + "\n\n");
            out.flush();

            try {
                // setting up inputstream for the sentences
                FileInputStream instream = new FileInputStream(new File(sentencesFilename));
                DataInputStream in = new DataInputStream(instream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String sentence;
                int times = 0;
                int sentences = 0;

                while ((sentence = br.readLine()) != null) {
                    ++sentences;
                    System.err.println("Sentence #" + sentences);
                    System.err.println("Current sentence: " + sentence);
                    updateBenchmark(timestamp, 2, useCPUTime, benchmarkBean);

                    // intersect
                    TreeAutomaton decomp = alg.decompose(alg.parseString(sentence));
                    CondensedTreeAutomaton inv = decomp.inverseCondensedHomomorphism(hom);

                    TreeAutomaton<String> result = irtg.getAutomaton().intersectViterbi(inv);

                    updateBenchmark(timestamp, 3, useCPUTime, benchmarkBean);

                    if (result.getFinalStates().isEmpty()) {
                        System.err.println("\n**** EMPTY ****\n");
                    }
//                    else {
//                        System.err.println("\nViterbi:\n" + result.viterbi() + "\n");
//                    }

                    // try to trigger gc
                    result = null;
                    System.gc();

                    System.err.println("Done in " + ((timestamp[3] - timestamp[2]) / 1000000) + "ms \n");
                    out.write("Parsed \n" + sentence + "\nIn " + ((timestamp[3] - timestamp[2]) / 1000000) + "ms.\n\n");

                    out.flush();
                    times += (timestamp[3] - timestamp[2]) / 1000000;
                }
                out.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n Parsed " + sentences + " sentences in " + times + "ms. \n");
                out.flush();
            } catch (IOException ex) {
                System.err.println("Error while reading the Sentences-file: " + ex.getMessage());
            }
        } catch (Exception ex) {
            System.out.println("Error while writing to file:" + ex.getMessage());
            ex.printStackTrace(System.err);
        }

    }

    // Saves the current time / CPU time in the timestamp-variable
    private static void updateBenchmark(long[] timestamp, int index, boolean useCPU, ThreadMXBean bean) {
        if (useCPU) {
            timestamp[index] = bean.getCurrentThreadCpuTime();
        } else {
            timestamp[index] = System.nanoTime();
        }
    }

    private static String stackTraceToString(StackTraceElement[] elements) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : elements) {
            sb.append(element.toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}
