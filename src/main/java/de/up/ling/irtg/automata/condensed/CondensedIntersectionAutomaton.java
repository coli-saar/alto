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
import java.util.*;

/**
 *
 * @author koller
 */
public class CondensedIntersectionAutomaton<LeftState, RightState> extends TreeAutomaton<Pair<LeftState, RightState>> {

    private TreeAutomaton<LeftState> left;
    private CondensedTreeAutomaton<RightState> right;
    private static final boolean DEBUG = false;
    private int[] leftToRightLabelRemap;
    private int[] rightToLeftLabelRemap;
    private Int2IntMap stateToLeftState;
    private Int2IntMap stateToRightState;
    private long[] ckyTimestamp = new long[10];

    public CondensedIntersectionAutomaton(TreeAutomaton<LeftState> left, CondensedTreeAutomaton<RightState> right) {
        super(left.getSignature()); // TODO = should intersect this with the right signature

        leftToRightLabelRemap = left.getSignature().remap(right.getSignature());
        rightToLeftLabelRemap = right.getSignature().remap(left.getSignature());

        this.left = left;
        this.right = right;

//        System.err.println("~~~~~~~~~~~~LEFT~~~~~~~~~~~~~~");
//        System.err.println(left.toString());
//
//        System.err.println("~~~~~~~~~+++RIGHT CONDENSED+++~~~~~~~~~~~~");
//        System.err.println(right.toStringCondensed());
//
//        System.err.println("~~~~~~~~~+++RIGHT ALL RULES+++~~~~~~~~~~~~");
//        System.err.println(right.toString());
//
//        System.err.println("~~~~~~~~~~~~~~~~~~~~~~~~~~");
//
//        System.err.println("right interner: " + right.stateInterner);
//        System.err.println("condensed-right interner: " + condensedRight.stateInterner);
        stateToLeftState = new Int2IntOpenHashMap();
        stateToRightState = new Int2IntOpenHashMap();

        finalStates = null;
//        allStates = new HashMap<Pair<LeftState, RightState>, Pair<LeftState, RightState>>();
    }

    /**
     * Translates a label ID of the left automaton (= of the intersection
     * automaton) to the label ID of the right automaton for the same label.
     * Returns 0 if the right automaton does not define this label.
     *
     * @param leftLabelId
     * @return
     */
    private int remapLabel(int leftLabelId) {
        return leftToRightLabelRemap[leftLabelId];
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return left.isBottomUpDeterministic() && right.isBottomUpDeterministic();
    }

    public void makeAllRulesExplicitCondensedCKY() {
        if (!isExplicit) {
            isExplicit = true;
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

            // force recomputation of final states
            finalStates = null;

            ckyTimestamp[2] = System.nanoTime();

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
                List<IntSet> remappedChildren = new ArrayList<IntSet>();

                // iterate over all children in the right rule
                for (int i = 0; i < rightRule.getArity(); ++i) {
                    ckyDfsForStatesInBottomUpOrder(rightChildren[i], visited, partners);

                    // take the right-automaton label for each child and get the previously calculated left-automaton label from partners.
                    remappedChildren.add(partners.get(rightChildren[i]));
                }

                left.foreachRuleBottomUpForSets(rightRule.getLabels(right), remappedChildren, leftToRightLabelRemap, new Function<Rule, Void>() {
                    public Void apply(Rule leftRule) {
                        Rule rule = combineRules(leftRule, rightRule);
                        storeRule(rule);

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
        int ret = addState(new Pair(left.getStateForId(leftState), right.getStateForId(rightState)));

        stateToLeftState.put(ret, leftState);
        stateToRightState.put(ret, rightState);

        return ret;
    }

    private Rule combineRules(Rule leftRule, Rule rightRule) {
        int[] childStates = new int[leftRule.getArity()];

        for (int i = 0; i < leftRule.getArity(); i++) {
            childStates[i] = addStatePair(leftRule.getChildren()[i], rightRule.getChildren()[i]);
        }

        int parentState = addStatePair(leftRule.getParent(), rightRule.getParent());

        return createRule(parentState, leftRule.getLabel(), childStates, leftRule.getWeight() * rightRule.getWeight());
    }

    private Rule combineRules(Rule leftRule, CondensedRule rightRule) {
        int[] childStates = new int[leftRule.getArity()];

        for (int i = 0; i < leftRule.getArity(); i++) {
            childStates[i] = addStatePair(leftRule.getChildren()[i], rightRule.getChildren()[i]);
        }

        int parentState = addStatePair(leftRule.getParent(), rightRule.getParent());

        return createRule(parentState, leftRule.getLabel(), childStates, leftRule.getWeight() * rightRule.getWeight());
    }

    @Override
    public Set<Integer> getFinalStates() {
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

    private void collectStatePairs(Collection<Integer> leftStates, Collection<Integer> rightStates, Collection<Integer> pairStates) {
        List<Collection> stateSets = new ArrayList<Collection>();
        stateSets.add(leftStates);
        stateSets.add(rightStates);

//        System.err.println("known states: " + stateInterner.getKnownObjects());
        CartesianIterator<Integer> it = new CartesianIterator(stateSets);
        while (it.hasNext()) {
            List<Integer> states = it.next();

            Pair<LeftState, RightState> statePair = new Pair(left.getStateForId(states.get(0)), right.getStateForId(states.get(1)));
//            System.err.println("consider pair for final state: " + statePair);

            int state = stateInterner.resolveObject(statePair);

            if (state != 0) {
//                System.err.println(" -> state pair exists");
                pairStates.add(state);
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
    public Set<Rule> getRulesTopDown(int label, int parentState) {
        makeAllRulesExplicit();

        assert useCachedRuleTopDown(label, parentState);

        return getRulesTopDownFromExplicit(label, parentState);
    }

    @Override
    public Set<Integer> getAllStates() {
        makeAllRulesExplicit();
        return super.getAllStates();
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
        long[] timestamp = new long[5];

        System.err.print("Reading the IRTG...");
        timestamp[0] = System.nanoTime();

        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileReader(new File(irtgFilename)));
        Interpretation interp = irtg.getInterpretation(interpretation);
        Homomorphism hom = interp.getHomomorphism();
        Algebra alg = irtg.getInterpretation(interpretation).getAlgebra();

        timestamp[1] = System.nanoTime();
        System.err.println(" Done in " + ((timestamp[1] - timestamp[0]) / 1000000) + "ms");
        try {
            File oFile = new File(outputFile);
//            System.err.println("Can write to file? " + oFile.canWrite());
            FileWriter outstream = new FileWriter(oFile);
            BufferedWriter out = new BufferedWriter(outstream);
            out.write("Testing IntersectionAutomaton with condensed intersectio ...\n"
                    + "IRTG-File  : " + irtgFilename + "\n"
                    + "Input-File : " + sentencesFilename + "\n"
                    + "Output-File: " + outputFile + "\n"
                    + "Comments   : " + comments + "\n\n");
            //  out.flush();
//            System.err.println("Can write to file? " + oFile.canWrite());

            try {

                FileInputStream instream = new FileInputStream(new File(sentencesFilename));
                DataInputStream in = new DataInputStream(instream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String sentence;
                int times = 0;
                int sentences = 0;

                while ((sentence = br.readLine()) != null) {
                    ++sentences;
                    System.err.println("Current sentence: " + sentence);
                    timestamp[2] = System.nanoTime();

                    TreeAutomaton decomp = alg.decompose(alg.parseString(sentence));
                    CondensedTreeAutomaton inv = decomp.inverseCondensedHomomorphism(hom);
                    TreeAutomaton<String> result = irtg.getAutomaton().intersectCondensed(inv);


                    timestamp[3] = System.nanoTime();

                    System.err.println("Done in " + ((timestamp[3] - timestamp[2]) / 1000000) + "ms \n");
                    outstream.write("Parsed \n" + sentence + "\nIn " + ((timestamp[3] - timestamp[2]) / 1000000) + "ms.\n\n");


//                    if (!result.getFinalStates().isEmpty()) {
//                        System.err.println("Language:\n" + result.language().toString());
//                        System.err.println("\nViterbi:\n" + result.viterbi() + "\n");
//                    }

//                    out.flush();
                    times += (timestamp[3] - timestamp[2]) / 1000000;
                }
                outstream.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n Parsed " + sentences + " sentences in " + times + "ms. \n");
//                out.flush();
            } catch (IOException ex) {
                System.err.println("Error while reading the Sentences-file: " + ex.getMessage());
            }
        } catch (Exception ex) {
            System.out.println("Error while writing to file:" + ex.getMessage() + "\n" + stackTraceToString(ex.getStackTrace()));
        }

    }

    public static String stackTraceToString(StackTraceElement[] elements) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : elements) {
            sb.append(element.toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}
