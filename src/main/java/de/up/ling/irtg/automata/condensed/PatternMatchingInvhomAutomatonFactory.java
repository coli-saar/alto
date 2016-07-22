/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.signature.SignatureMapper;
import de.up.ling.irtg.util.ArrayInt2IntMap;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Produces pattern matcher automata to compute the inverse of decomposition
 * automata under homomorphism.
 * @author koller
 * @param <MatcherState>
 * @param <State>
 */
public abstract class PatternMatchingInvhomAutomatonFactory<MatcherState, State> {

    
    protected IntList startStateIDs;
    protected final Homomorphism hom;
    private Tree<HomomorphismSymbol>[] rightmostVariableForLabelSetID;
    private Int2IntMap arityForLabelSetID;
    
    private IntList epsilonLabelSetIDs;

    //private Map<String, Set<Rule>> matcherConstantRulesByNodeLabel;       idea for later!
    //private Map<String, Set<Rule>> matcherConstantRulesWithoutNodeLabelsByTerm;
    //private Int2ObjectMap<IntSet> rhsState2MatchingStartStates;
    protected boolean computeCompleteMatcher = false;
    public Writer logWriter;
    public String logTitle = "";

    /**
     * Initializes a new {@code PatternMatchingInvhomAutomatonFactory} with
     * respect to the homomorphism {@code hom}.
     * @param hom 
     */
    public PatternMatchingInvhomAutomatonFactory(Homomorphism hom) {
        this.hom = hom;
        initialize(true);

    }

    private void initialize(boolean computeMatcher) {
        rightmostVariableForLabelSetID = new Tree[hom.getMaxLabelSetID() + 1];
        arityForLabelSetID = new ArrayInt2IntMap();
        epsilonLabelSetIDs = new IntArrayList();

//        rhsState2MatchingStartStates = new Int2ObjectOpenHashMap<>();
        for (int labelSetID = 1; labelSetID <= hom.getMaxLabelSetID(); labelSetID++) {
            Tree<HomomorphismSymbol> term = hom.getByLabelSetID(labelSetID);
            if (term.getLabel().isVariable())
            {
                epsilonLabelSetIDs.add(labelSetID);
            }

            int numVariables = (int) term.getLeafLabels().stream().filter(sym -> sym.isVariable()).count();
            arityForLabelSetID.put(labelSetID, numVariables);

            rightmostVariableForLabelSetID[labelSetID] = term.dfs((node, children) -> {
                Tree<HomomorphismSymbol> ret = null;

                if (node.getLabel().isVariable()) {
                    return node;
                } else {
                    for (Tree<HomomorphismSymbol> child : children) {
                        if (child != null) {
                            ret = child;
                        }
                    }

                    return ret;
                }
            });
        }
        
        if (computeMatcher) {
            computeMatcherFromHomomorphism();
        }
    }


    protected abstract void computeMatcherFromHomomorphism();

    





    /*public CondensedTreeAutomaton<State> invhom(TreeAutomaton<State> rhs) {
        
        
        ConcreteCondensedTreeAutomaton<State> ret = new CondensedInvhomAutomaton(rhs);

        SignatureMapper mapper = rhs.getSignature().getMapperTo(matcher.getSignature());
        Int2ObjectMap<IntSet> decorations = decorateStatesWithMatcher(rhs, mapper);

//        for (int rhsState : decorations.keySet()) {
//            System.err.println("dec " + rhs.getStateForId(rhsState) + ": " + Util.mapSet(decorations.get(rhsState), nondetMatcher::getStateForId));
//        }
        FastutilUtils.forEach(decorations.keySet(), rhsState -> {
            IntSet decorationHere = decorations.get(rhsState);

            FastutilUtils.forEach(decorationHere, matcherState -> {
                int labelSetID = startStateIdToLabelSetID.get(matcherState);
                if (labelSetID > 0) {
//                    System.err.println("\n\nrhs=" + rhs.getStateForId(rhsState) + ", labelset=" + hom.getSourceSignature().resolveSymbolIDs(hom.getLabelSetByLabelSetID(labelSetID)));
//                    System.err.println("  matcher state " + nondetMatcher.getStateForId(matcherState));
//                    System.err.println("  rightmost var: " + HomomorphismSymbol.toStringTree(rightmostVariableForLabelSetID[labelSetID], hom.getTargetSignature()));

                    Tree<HomomorphismSymbol> term = hom.getByLabelSetID(labelSetID);
                    int numVariables = arityForLabelSetID.get(labelSetID);

                    if (numVariables == 0) {
                        ret.addRule(new CondensedRule(rhsState, labelSetID, new int[0], 1));
                    } else {
                        int[] childStates = new int[numVariables];

                        // todo - case without variables
                        forAllMatches(matcherState, rhsState, term, rightmostVariableForLabelSetID[labelSetID], childStates, rhs, decorations, cs -> {
//                        System.err.println("match! " + Arrays.stream(cs).mapToObj(rhs::getStateForId).collect(Collectors.toList()));
                            ret.addRule(new CondensedRule(rhsState, labelSetID, cs.clone(), 1));
                        });
                    }
                }
            });

        });

        return ret;
    }*/

    
    protected abstract void adjustMatcher(TreeAutomaton<State> rhs);
   
    /**
     * Computes the image under inverse homomorphism of the decomposition
     * automaton {@code rhs}.
     * @param rhs
     * @return
     */
    public CondensedTreeAutomaton<State> invhom(TreeAutomaton<State> rhs) {
        if (!computeCompleteMatcher) {
            adjustMatcher(rhs);
        }

        
        ConcreteTreeAutomaton<Pair<String, State>> intersectionAutomaton;

        if (rhs.supportsBottomUpQueries()) {
            intersectionAutomaton = intersectWithMatcherBottomUp(rhs);
        } else {
            intersectionAutomaton = intersectWithMatcherTopDown(rhs);
        }

        
        return getInvhomFromMatchingIntersection(intersectionAutomaton, rhs);
    }

   

    

    protected abstract ConcreteTreeAutomaton<Pair<String, State>> intersectWithMatcherTopDown(TreeAutomaton<State> rhs); 

    protected abstract ConcreteTreeAutomaton<Pair<String, State>> intersectWithMatcherBottomUp(TreeAutomaton<State> rhs);

    

    

    private CondensedTreeAutomaton<State> getInvhomFromMatchingIntersection(ConcreteTreeAutomaton<Pair<String, State>> intersectionAutomaton, TreeAutomaton<State> rhs) {
        return new LazyCondensedInvhomAutomaton(rhs, intersectionAutomaton, intersectionAutomaton.getSignature().getMapperTo(hom.getTargetSignature()));
    }

    
    protected abstract int getLabelSetIDForMatcherStartStateID(int matcherStateID);
    protected abstract MatcherState getMatcherStateForID(int matcherStateID);
    
    
    private class CondensedInvhomAutomaton extends ConcreteCondensedTreeAutomaton<State> {

        public CondensedInvhomAutomaton(TreeAutomaton<State> rhs) {
            signature = hom.getSourceSignature();
            finalStates = rhs.getFinalStates();
            stateInterner = rhs.getStateInterner();
        }

        // Returns the ID for a labelset, but does not add it! Returns 0 if it is not 
        // represented in the interner
        @Override
        protected int getLabelSetID(IntSet labels) {
            return hom.getLabelSetIDByLabelSet(labels);
        }

        // Adds a given labelSet to the interner and returns the int value representing it. 
        // This should be called while creating a rule for this automaton.
        @Override
        public int addLabelSetID(IntSet labels) {
            throw new UnsupportedOperationException("cannot add label set IDs to invhom automaton");
        }

        // Reverse function of getLabelSetID. Shold be used by a CondensedRule Object.
        @Override
        public IntSet getLabelsForID(int labelSetID) {
            return hom.getLabelSetByLabelSetID(labelSetID);
        }
    }

    private class LazyCondensedInvhomAutomaton extends CondensedTreeAutomaton<State> {

        private final TreeAutomaton<Pair<String, State>> intersectionAutomaton;
        private final TreeAutomaton<State> rhs;
        private final SignatureMapper mapperIntersToHom;
        
        public LazyCondensedInvhomAutomaton(TreeAutomaton<State> rhs, TreeAutomaton<Pair<String, State>> intersectionAutomaton, SignatureMapper mapperIntersToHom) {
            super(rhs.getSignature());
            signature = hom.getSourceSignature();
            finalStates = rhs.getFinalStates();
            stateInterner = rhs.getStateInterner();
            this.rhs = rhs;
            this.intersectionAutomaton = intersectionAutomaton;
            this.mapperIntersToHom = mapperIntersToHom;
        }

        @Override
        public Iterable<CondensedRule> getCondensedRulesByParentState(int parentState) {

            List<CondensedRule> ret = new ArrayList<>();
            for (int matcherStateIDUnprocessed : startStateIDs) {
                Pair<String, State> intersState = new Pair(getMatcherStateForID(matcherStateIDUnprocessed), rhs.getStateForId(parentState));
                int intersStateID = intersectionAutomaton.getIdForState(intersState);
                if (intersStateID > 0 ) {
                    //int innerIntersStateID = intersectionAutomaton.getIdForState(new ImmutablePair(restrictiveMatcher.getStateForId(matcherStateIDUnprocessed), intersState.getRight()));
                    if (intersectionAutomaton.getRulesTopDown(intersStateID).iterator().hasNext()) {//this seems inefficient. But maybe not so bad since intersectionAutomaton is explicit?

                        int labelSetID = getLabelSetIDForMatcherStartStateID(matcherStateIDUnprocessed);
                        if (labelSetID >= 1) {
                            Tree<HomomorphismSymbol> term = hom.getByLabelSetID(labelSetID);
                            int numVariables = arityForLabelSetID.get(labelSetID);

                            if (numVariables == 0) {
                                ret.add(new CondensedRule(parentState, labelSetID, new int[0], 1));
                            } else {
                                /*int[] childStates = new int[numVariables];
                                 forAllMatchesRestrictive(intersStateID, term, rightmostVariableForLabelSetID[labelSetID], childStates, rhs, intersectionAutomaton, mapperIntersToHom, cs -> {
                                 //                        System.err.println("match! " + Arrays.stream(cs).mapToObj(rhs::getStateForId).collect(Collectors.toList()));
                                 ret.addRule(new CondensedRule(rhsStateID, labelSetID, cs.clone(), 1));
                                 });*/
                                int[] seed = new int[numVariables];
                                List<int[]> seedList = new ArrayList<>();
                                seedList.add(seed);
                                forAllMatches(seedList, intersStateID, term, rightmostVariableForLabelSetID[labelSetID], rhs, intersectionAutomaton, mapperIntersToHom,
                                                            childStates -> {
                                                                ret.add(new CondensedRule(parentState, labelSetID, childStates, 1));
                                                                    });

    //                            forAllMatchesRestrictive2(intersStateID, term, null, seed, rhs, intersectionAutomaton, mapperIntersToHom, 
    //                                                        childStates -> ret.addRule(new CondensedRule(rhsStateID, labelSetID, childStates, 1)));

                                /*StringJoiner sj = new StringJoiner(", ", "{", "}");
                                 for (int[] array : res) {
                                 sj.add(Arrays.toString(array));
                                 }
                                 System.err.println(sj);*/
                                //System.err.println(res.size());
                            }
                        }
                    }
                }
            }
            
            epsilonLabelSetIDs.stream().forEach(labelSetID -> ret.add(new CondensedRule(parentState, labelSetID, new int[]{parentState}, 1)));
            //System.err.println(ret);
            return ret;
        }
        

        // Returns the ID for a labelset, but does not add it! Returns 0 if it is not 
        // represented in the interner
        @Override
        protected int getLabelSetID(IntSet labels) {
            return hom.getLabelSetIDByLabelSet(labels);
        }

        // Adds a given labelSet to the interner and returns the int value representing it. 
        // This should be called while creating a rule for this automaton.
        @Override
        public int addLabelSetID(IntSet labels) {
            throw new UnsupportedOperationException("cannot add label set IDs to invhom automaton");
        }

        // Reverse function of getLabelSetID. Shold be used by a CondensedRule Object.
        @Override
        public IntSet getLabelsForID(int labelSetID) {
            return hom.getLabelSetByLabelSetID(labelSetID);
        }

        @Override
        public Iterable<CondensedRule> getCondensedRulesBottomUp(IntSet labelId, int[] childStates) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Iterable<CondensedRule> getCondensedRulesTopDown(IntSet labelId, int parentState) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void makeAllRulesCondensedExplicit() {
            Queue<Integer> agenda = new LinkedList<>();
            IntSet seen = new IntOpenHashSet();
            agenda.addAll(finalStates);
            seen.addAll(finalStates);
            while (!agenda.isEmpty()) {
                int stateID = agenda.poll();
                Iterable<CondensedRule> res = getCondensedRulesByParentState(stateID);
                for (CondensedRule rule : res) {
                    storeRule(rule);
                    for (int child : rule.getChildren()) {
                        if (!seen.contains(child)) {
                            seen.add(child);
                            agenda.add(child);
                        }
                    }
                }
            }
        }
    }
    
    protected abstract List<int[]> forAllMatches(List<int[]> prevList, int intersState, Tree<HomomorphismSymbol> term, Tree<HomomorphismSymbol> rightmostVariable, TreeAutomaton<State> rhsAuto, TreeAutomaton<Pair<String, State>> intersectionAuto, SignatureMapper mapperintersToHom, Consumer<int[]> fn);

    
    
    
    
    
    
    
    
    
    
    


    

    
    /*public static void main(String[] args) throws Exception {

        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream("examples/hrgTestingCleanS.irtg"));
        Homomorphism hom = irtg.getInterpretation("graph").getHomomorphism();
        GraphAlgebra alg = (GraphAlgebra) irtg.getInterpretation("graph").getAlgebra();

        PatternMatchingInvhomAutomatonFactory f = new PMFactoryRestrictive(hom, alg);
        f.computeCompleteMatcher = true;
        f.computeMatcherFromHomomorphism();

        System.err.println(alg.getSignature());
        for (int labelSetID = 1; labelSetID <= hom.getMaxLabelSetID(); labelSetID++) {
            System.err.println(hom.getByLabelSetID(labelSetID));
        }
        String ex0 = "(g<root >/ go-01 :ARG0 (b / boy))";
        String ex1 = "(w<root> / want-01  :ARG0 (b / boy)  :ARG1 (g<vcomp> / go-01 :ARG0 b))";
        String ex2 = "(w<root> / want-01 :ARG0 (b / boy) :ARG1 (bel / believe-01 :ARG0 (g / girl) :ARG1 (l / like-01 :ARG0 (b2 / boy) :ARG1 (g2 / girl))) :dummy g)";
        String ex3 = "(w<root> / want-01 :ARG0 (b / boy) :ARG1 (go / go-01 :ARG0 (g / girl)) :dummy g)";
        String input = ex2;
        SGraph sgraph = alg.parseString(input);

        TreeAutomaton<BoundaryRepresentation> rhs = alg.decompose(alg.parseString(input), SGraphBRDecompositionAutomatonBottomUp.class);

        /*System.err.println(rhs);
         int ruleCount = 0;
         Iterator it = rhs.getRuleSet().iterator();
         while (it.hasNext()) {
         it.next();
         ruleCount++;
         }
         System.err.println("rule count: " + ruleCount);*/
        /*CondensedTreeAutomaton<BoundaryRepresentation> invhom = f.invhom(rhs);
        //System.err.println(f.restrictiveMatcher);
        //System.err.println(f.matcherChild2Rule);
        TreeAutomaton finalIntAut = new CondensedIntersectionAutomaton<String, BoundaryRepresentation>(irtg.getAutomaton(), invhom, irtg.getAutomaton().getSignature().getIdentityMapper());
        //new IntersectionAutomaton(irtg.getAutomaton(), invhom); 

        System.err.println("INVHOM:\n" + invhom);
        System.err.println("FINALINTERSECTION:\n" + finalIntAut);

        Map<String, String> map = new HashMap<>();
        map.put("graph", input);
        TreeAutomaton chart = irtg.parse(map);
        System.err.println("IRTG parse:\n" + chart);

        int warmupIterations = 10000;
        for (int i = 0; i < warmupIterations; i++) {
            f = new PMFactoryRestrictive(hom, alg);
            f.computeCompleteMatcher = true;
            f.computeMatcherFromHomomorphism();
        }

        f.computeCompleteMatcher = false;
        f.initialize(true);
        for (int i = 0; i < warmupIterations; i++) {
            rhs = alg.decompose(alg.parseString(input), SGraphBRDecompositionAutomatonBottomUp.class);
            invhom = f.invhom(rhs);
            finalIntAut = new CondensedIntersectionAutomaton<String, BoundaryRepresentation>(irtg.getAutomaton(), invhom, irtg.getAutomaton().getSignature().getIdentityMapper());
        }

        f.computeCompleteMatcher = true;
        f.initialize(true);
        for (int i = 0; i < warmupIterations; i++) {
            rhs = alg.decompose(alg.parseString(input), SGraphBRDecompositionAutomatonBottomUp.class);
            invhom = f.invhom(rhs);
            finalIntAut = new CondensedIntersectionAutomaton<String, BoundaryRepresentation>(irtg.getAutomaton(), invhom, irtg.getAutomaton().getSignature().getIdentityMapper());
        }

        CpuTimeStopwatch sw = new CpuTimeStopwatch();
        int setupIterations = 0;
        int iterations = 100000;
        int standardIterations = 100000;

        sw.record(0);
        for (int i = 0; i < setupIterations; i++) {
            f = new PMFactoryRestrictive(hom, alg);
            f.computeCompleteMatcher = true;
            f.initialize(true);
        }

        sw.record(1);
        f.computeCompleteMatcher = false;
        f.initialize(true);
        for (int i = 0; i < iterations; i++) {
            rhs = alg.decompose(sgraph, SGraphBRDecompositionAutomatonBottomUp.class);
            invhom = f.invhom(rhs);
            finalIntAut = new CondensedIntersectionAutomaton<String, BoundaryRepresentation>(irtg.getAutomaton(), invhom, irtg.getAutomaton().getSignature().getIdentityMapper());
        }

        sw.record(2);
        f.computeCompleteMatcher = true;
        f.initialize(true);
        for (int i = 0; i < standardIterations; i++) {
            rhs = alg.decompose(sgraph, SGraphBRDecompositionAutomatonBottomUp.class);
            invhom = f.invhom(rhs);
            finalIntAut = new CondensedIntersectionAutomaton<String, BoundaryRepresentation>(irtg.getAutomaton(), invhom, irtg.getAutomaton().getSignature().getIdentityMapper());
        }
        sw.record(3);
        System.err.println(iterations + "/" + standardIterations + " iterations:");
        sw.printMilliseconds("create pattern matching automaton (" + setupIterations + " iterations)", "invhom via pattern matching lazy (" + iterations + " iterations)", "invhom via pattern matching complete (" + standardIterations + " iterations)");

        /*CpuTimeStopwatch sw = new CpuTimeStopwatch();
         sw.record(0);

         InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream(args[0]));
         Homomorphism hom = irtg.getInterpretation("string").getHomomorphism();
         Algebra<List<String>> alg = irtg.getInterpretation("string").getAlgebra();

         sw.record(1);

         PatternMatchingInvhomAutomatonFactory f = new PatternMatchingInvhomAutomatonFactory(hom);
         f.computeMatcherFromHomomorphism();

         sw.record(2);
         sw.printMilliseconds("load", "prepare");

         int numSent = 0;
         BufferedReader buf = new BufferedReader(new FileReader(args[1]));
         do {
         String line = buf.readLine();

         if (line == null) {
         break;
         }

         List<String> sent = alg.parseString(line);
         TreeAutomaton decomp = alg.decompose(sent);

         System.err.println("\n" + (numSent + 1) + " - " + sent.size() + " words");

         CpuTimeStopwatch w2 = new CpuTimeStopwatch();
         w2.record(0);

         CondensedTreeAutomaton invhom = f.invhom(decomp);
         w2.record(1);

         TreeAutomaton chart = new CondensedViterbiIntersectionAutomaton(irtg.getAutomaton(), invhom, new IdentitySignatureMapper(invhom.getSignature()));
         chart.makeAllRulesExplicit();

         w2.record(2);
            
         System.err.println(chart.viterbi());
            
         w2.record(3);

         w2.printMilliseconds("invhom", "intersect", "viterbi");

         numSent++;
         } while (true);*/
    //}
}
