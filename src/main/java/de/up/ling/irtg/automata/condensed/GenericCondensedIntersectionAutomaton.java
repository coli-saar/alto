/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import com.google.common.collect.Lists;
import de.saar.basic.Pair;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.SignatureMapper;
import de.up.ling.irtg.util.GuiUtils;
import de.up.ling.irtg.util.IntAgenda;
import de.up.ling.irtg.util.IntInt2IntMap;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.ParseException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Computes an automaton for the intersection of an ordinary
 * {@link TreeAutomaton} (left) and a {@link CondensedTreeAutomaton} (right).
 *
 * This class uses a CKY-style algorithm, which queries the right automaton
 * top-down and the left automaton bottom-up. A typical use-case is that the
 * left automaton is the derivation-tree RTG of an IRTG and the right automaton
 * is the inverse-homomorphism image of a decomposition automaton.<p>
 *
 * Note that this automaton will not work correctly if right is recursive except
 * if all recursive rules are of the form 'q -> {...}(q)' which is explicitly
 * handled. Recursive right automata come up, for instance, when parsing with an
 * IRTG that has ?1 on the input interpretation. (See issue #2 on Bitbucket.)
 *
 * @author koller
 * @param <LeftState>
 * @param <RightState>
 */
public abstract class GenericCondensedIntersectionAutomaton<LeftState, RightState> extends TreeAutomaton<Pair<LeftState, RightState>> {

    private final TreeAutomaton<LeftState> left;
    private final CondensedTreeAutomaton<RightState> right;
    public static boolean DEBUG = false;
    private final SignatureMapper leftToRightSignatureMapper;

    private final IntInt2IntMap stateMapping;  // right state -> left state -> output state
    // (index first by right state, then by left state because almost all right states
    // receive corresponding left states, but not vice versa. This keeps outer map very dense,
    // and makes it suitable for a fast ArrayMap)

    abstract protected void collectOutputRule(Rule outputRule);

    abstract protected void addAllOutputRules();

    @FunctionalInterface
    public static interface IntersectionCall {

        public TreeAutomaton intersect(TreeAutomaton left, CondensedTreeAutomaton right);
    }

    public GenericCondensedIntersectionAutomaton(TreeAutomaton<LeftState> left, CondensedTreeAutomaton<RightState> right, SignatureMapper sigMapper) {
        super(left.getSignature()); // TODO = should intersect this with the (remapped) right signature

        this.leftToRightSignatureMapper = sigMapper;

        this.left = left;
        this.right = right;

        finalStates = null;

        stateMapping = new IntInt2IntMap();
        if (!right.supportsTopDownQueries()) {//not sure if this is the right question to ask
            right.makeAllRulesCondensedExplicit();
        }
    }

    // Intersecting the two automatons using a CKY algorithm
    @Override
    public void makeAllRulesExplicit() {
        if (!ruleStore.isExplicit()) {
            ruleStore.setExplicit(true);
            getStateInterner().setTrustingMode(true);

            Int2ObjectMap<IntSet> partners = new Int2ObjectOpenHashMap<>();

            long t1 = System.nanoTime();

            // Perform a DFS in the right automaton to find all partner states
            IntSet visited = new IntOpenHashSet();
            right.getFinalStates().forEach((q) -> {
                // starting the dfs by the final states ensures a topological order
                ckyDfsForStatesInBottomUpOrder(q, visited, partners, 0);
            });

            // transfer all collected rules into the output automaton
            addAllOutputRules();

            // force recomputation of final states
            finalStates = null;

            if (DEBUG) {
                System.err.println("CKY runtime: " + (System.nanoTime() - t1) / 1000000 + "ms");
                System.err.println("Intersection automaton CKY:\n" + toString() + "\n~~~~~~~~~~~~~~~~~~");
            }
        }
    }

    private int progressListenerCount = 0;

    private void D(int depth, Supplier<String> s) {
        if (DEBUG) {
            System.err.println(Util.repeat("  ", depth) + s.get());
        }
    }

    /**
     * Iterate over all states in the right (condensed) automaton to find
     * partner states in the left one.
     *
     * @param q Current state
     * @param visited already visited states
     * @param partners already found partner states
     */
    private void ckyDfsForStatesInBottomUpOrder(int q, IntSet visited, final Int2ObjectMap<IntSet> partners, int depth) {
        D(depth, () -> "cky called: " + right.getStateForId(q));

        if (!visited.contains(q)) {
            visited.add(q);
            D(depth, () -> "-> processing " + right.getStateForId(q));

            final IntList foundPartners = new IntArrayList();
            List<CondensedRule> loopRules = new ArrayList<>();

//            final ArrayList<CondensedRule> selfLoops = new ArrayList<>();
//            final IntList selfToDo = new IntArrayList();
//            final IntSet selfSeen = new IntOpenHashSet();
            for (final CondensedRule rightRule : right.getCondensedRulesByParentState(q)) {
                D(depth, () -> "Right rule: " + rightRule.toString(right, s -> s.contains("asbestos") || s.contains("There")));

                // If the right rule is a "self-loop", i.e. of the form q -> f(q),
                // the normal DFS doesn't work. We give it special treatment by
                // postponing its combination with the right rules until below.
                if (rightRule.isLoop()) {
                    loopRules.add(rightRule);

                    // make sure that all non-loopy children have been explored
                    for (int i = 0; i < rightRule.getArity(); i++) {
                        int ch = rightRule.getChildren()[i];
                        if (ch != rightRule.getParent()) {
                            ckyDfsForStatesInBottomUpOrder(ch, visited, partners, depth + 1);
                        }
                    }

//                    selfLoops.add(rightRule);
                    continue;
                }

                int[] rightChildren = rightRule.getChildren();
                List<IntSet> remappedChildren = new ArrayList<>();

                // iterate over all children in the right rule
                for (int i = 0; i < rightRule.getArity(); ++i) {
                    // go into the recursion first to obtain the topological order that is needed for the CKY algorithm
                    ckyDfsForStatesInBottomUpOrder(rightChildren[i], visited, partners, depth + 1);

                    // only add, if a partner state has been found.
                    if (partners.containsKey(rightChildren[i])) {
                        // take the right-automaton label for each child and get the previously calculated left-automaton label from partners.
                        remappedChildren.add(partners.get(rightChildren[i]));
                    }
                }

                foundPartners.clear();

                if (DEBUG) {
                    List childStates = Util.mapToList(remappedChildren, qs -> Util.mapToList(qs, qq -> left.getStateForId(qq)));
                    D(depth, () -> "found child states: " + childStates);
                }

                // find all rules bottom-up in the left automaton that have the same (remapped) children as the right rule.
                left.foreachRuleBottomUpForSets(rightRule.getLabels(right), remappedChildren, leftToRightSignatureMapper, leftRule -> {
                    // create a new rule
                    Rule rule = combineRules(leftRule, rightRule);

//                    if (!selfSeen.contains(rule.getParent())) {
//                        selfToDo.add(leftRule.getParent());
//
//                        selfSeen.add(rule.getParent());
//                    }
                    D(depth, () -> "Left rule: " + leftRule.toString(left));
                    D(depth, () -> "Combined rule: " + rule.toString(this));
                    D(depth, () -> "");

//                    System.err.println(right.getStateForId(q) + " -> " + rule.toString(this));
                    // transfer rule to staging area for output rules
                    collectOutputRule(rule);

                    // schedule the two parent states for addition
                    // -- they cannot be added to the partners sets here
                    // because we are inside an iteration over these sets
                    assert q == rightRule.getParent();
                    foundPartners.add(rightRule.getParent());
                    foundPartners.add(leftRule.getParent());
                });

                // now go through to-do list and add all state pairs to partner sets
                for (int i = 0; i < foundPartners.size(); i += 2) {
                    addPartner(foundPartners.get(i), foundPartners.get(i + 1), partners);
                }

                if (GuiUtils.getGlobalListener() != null) {
                    GuiUtils.getGlobalListener().accept((progressListenerCount++) % 2000, 2000, "");
                }
            }

            // Now that we have seen all children of q through rules that
            // are not self-loops, go through the self-loops and process them.
            if (partners.get(q) != null) {
                // If q has no partners, that means we have found no non-loopy
                // rules for expanding it. Thus any loopy expansions will be
                // unproductive, so we can skip them.
                
                for (CondensedRule rightRule : loopRules) {
                    int rightParent = rightRule.getParent();
                    int[] rightChildren = rightRule.getChildren();
                    IntSet rightLabelSet = rightRule.getLabels(right);
                    List<IntSet> leftChildStateSets = new ArrayList<>(rightChildren.length);

//                    if (partners.get(q) == null) {
//                        System.err.println("** loopy rule " + rightRule.toString(right));
//                        System.err.println("** but parent q=" + right.getStateForId(q) + " has null partners");
//                    }

                    IntAgenda agenda = new IntAgenda(partners.get(q));
                    while (!agenda.isEmpty()) {
                        int leftState = agenda.pop();

                        for (int i = 0; i < rightRule.getArity(); i++) {
                        // left.foreachRuleBottomUpForSets(rightRule.getLabels(right), remappedChildren, leftToRightSignatureMapper, leftRule -> {
                            // List<IntSet> childStateSets
                            if (rightChildren[i] == rightParent) {
                                // i is a loop position
                                makeLeftChildStateSets(leftChildStateSets, rightChildren, i, leftState, partners);
                                left.foreachRuleBottomUpForSets(rightLabelSet, leftChildStateSets, leftToRightSignatureMapper, leftRule -> {
                                    Rule rule = combineRules(leftRule, rightRule);
                                    collectOutputRule(rule);
                                    addPartner(rightRule.getParent(), leftRule.getParent(), partners);
                                    agenda.enqueue(leftRule.getParent());
                                });
                            }
                        }
                    }
                }
            }

            /*
             int[] children = new int[1];
             for (int i = 0; i < selfToDo.size(); ++i) {
             int leftState = selfToDo.get(i);

             children[0] = leftState;

             for (int k = 0; k < selfLoops.size(); ++k) {
             CondensedRule rightRule = selfLoops.get(k);

             rightRule.getLabels(right).forEach(labelId -> {
             Iterable<Rule> rules = left.getRulesBottomUp(leftToRightSignatureMapper.remapForward(labelId), children);

             rules.forEach(leftRule -> {
             // create a new rule
             Rule rule = combineRules(leftRule, rightRule);

             if (!selfSeen.contains(rule.getParent())) {
             //                                selfToDo.add(rule.getParent());
             selfToDo.add(leftRule.getParent());

             selfSeen.add(rule.getParent());
             }

             D(depth, () -> "Left rule: " + leftRule.toString(left));
             D(depth, () -> "Combined rule (post-hoc): " + rule.toString(this));
             D(depth, () -> "");

             // transfer rule to staging area for output rules
             collectOutputRule(rule);

             addPartner(rightRule.getParent(), leftRule.getParent(), partners);

             });
             });
             }
             }
             */
        }
    }

    private void makeLeftChildStateSets(List<IntSet> leftChildStateSets, int[] rightChildren, int childPos, int leftStateAtPos, Int2ObjectMap<IntSet> partners) {
        leftChildStateSets.clear();

        for (int i = 0; i < rightChildren.length; i++) {
            if (i == childPos) {
                IntSet s = new IntOpenHashSet();
                s.add(leftStateAtPos);
                leftChildStateSets.add(s);
            } else {
                leftChildStateSets.add(partners.get(rightChildren[i]));
            }
        }
    }

    private void addPartner(int rightState, int leftState, Int2ObjectMap<IntSet> partners) {

        // remember the newly found partneres if needed
        IntSet knownPartners = partners.get(rightState);

        if (knownPartners == null) {
            knownPartners = new IntOpenHashSet();
            partners.put(rightState, knownPartners);
        }

        knownPartners.add(leftState);
    }

    private int addStatePair(int leftState, int rightState) {
        int ret = getStateMapping(leftState, rightState);

        if (ret == 0) {
            ret = addState(new Pair(left.getStateForId(leftState), right.getStateForId(rightState)));
            addStateMapping(leftState, rightState, ret);
//            stateMapping.put(rightState, leftState, ret);
        }

        return ret;
    }

    private void addStateMapping(int leftState, int rightState, int combinedState) {
        stateMapping.put(rightState, leftState, combinedState);
    }

    private int getStateMapping(int leftState, int rightState) {
        return stateMapping.get(rightState, leftState);
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
    public boolean isBottomUpDeterministic() {
        return left.isBottomUpDeterministic() && right.isBottomUpDeterministic();
    }

    @Override
    public IntSet getFinalStates() {
        if (finalStates == null) {
            getAllStates(); // initialize data structure for addState

            finalStates = new IntOpenHashSet();
            collectStatePairs(left.getFinalStates(), right.getFinalStates(), finalStates);
        }

        return finalStates;
    }

    // get all states for this automaton, that are the result of the combination of a state in the
    // leftStates set and one in the rightStates set
    private void collectStatePairs(IntSet leftStates, IntSet rightStates, IntSet pairStates) {
        leftStates.forEach((leftState) -> {
            rightStates.stream().map((rightState) -> getStateMapping(leftState, rightState))
                    .filter((state)
                            -> (state != 0)).forEach((state) -> {
                        pairStates.add(state);
                    });
        });
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int label, int[] childStates) {
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

    /**
     * Function to test the efficiency of this intersection algorithm by parsing
     * each sentence in a text file with a given IRTG. Args:
     * /path/to/grammar.irtg, /path/to/sentences, interpretation,
     * /path/to/result_file, "Additional comment"
     *
     * @param args CMD arguments
     * @param showViterbiTrees
     * @param icall what intersection should be used?
     * @throws FileNotFoundException
     * @throws ParseException
     * @throws IOException
     * @throws ParserException
     * @throws AntlrIrtgBuilder.ParseException
     */
    public static void main(String[] args, boolean showViterbiTrees, IntersectionCall icall) throws FileNotFoundException, ParseException, IOException, ParserException, de.up.ling.irtg.codec.CodecParseException {
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
        long totalChartTime = 0;
        long totalViterbiTime = 0;

        // initialize CPU-time benchmarking
        long[] timestamp = new long[10];
        ThreadMXBean benchmarkBean = ManagementFactory.getThreadMXBean();
        boolean useCPUTime = benchmarkBean.isCurrentThreadCpuTimeSupported();
        if (useCPUTime) {
            System.err.println("Using CPU time for measuring the results.");
        }

        System.err.print("Reading the IRTG...");

        updateBenchmark(timestamp, 0, useCPUTime, benchmarkBean);

        InputCodec<InterpretedTreeAutomaton> codec = InputCodec.getInputCodecByExtension(Util.getFilenameExtension(irtgFilename));
        InterpretedTreeAutomaton irtg = codec.read(new FileInputStream(new File(irtgFilename)));
        Interpretation interp = irtg.getInterpretation(interpretation);
        Homomorphism hom = interp.getHomomorphism();
        Algebra alg = irtg.getInterpretation(interpretation).getAlgebra();

        updateBenchmark(timestamp, 1, useCPUTime, benchmarkBean);

//        irtg.getAutomaton().analyze();
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
                    System.err.println("\nSentence #" + sentences);
                    System.err.println("Current sentence: " + sentence);
                    updateBenchmark(timestamp, 2, useCPUTime, benchmarkBean);

                    // intersect
                    TreeAutomaton decomp = alg.decompose(alg.parseString(sentence));
                    CondensedTreeAutomaton inv = decomp.inverseCondensedHomomorphism(hom);

                    TreeAutomaton<String> result = icall.intersect(irtg.getAutomaton(), inv);

                    updateBenchmark(timestamp, 3, useCPUTime, benchmarkBean);

                    long thisChartTime = (timestamp[3] - timestamp[2]);
                    totalChartTime += thisChartTime;
                    System.err.println("-> Chart " + (thisChartTime / 1000000) + "ms, cumulative " + totalChartTime / 1000000 + "ms");
                    out.write("Parsed \n" + sentence + "\nIn " + ((timestamp[3] - timestamp[2]) / 1000000) + "ms.\n\n");
                    out.flush();

                    if (result.getFinalStates().isEmpty()) {
                        System.err.println("**** EMPTY ****\n");
                    } else if (showViterbiTrees) {
                        System.err.println(result.viterbi());
                        updateBenchmark(timestamp, 4, useCPUTime, benchmarkBean);
                        long thisViterbiTime = timestamp[4] - timestamp[3];
                        totalViterbiTime += thisViterbiTime;

                        System.err.println("-> Viterbi " + thisViterbiTime / 1000000 + "ms, cumulative " + totalViterbiTime / 1000000 + "ms");
                    }

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

}
