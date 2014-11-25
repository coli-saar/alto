/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.collect.Sets;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.NumbersCombine;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author jonas
 */
public class GrammarBRDecompAutoInstruments {

    private SGraphBRDecompositionAutomaton auto;

    private String[] intToGrammarState;
    private IntSet grammarFinalStates;
    private Int2ObjectMap<Int2ObjectMap<IntSet>> unaries; //given a state of the grammar, returns the set of maps that assigns to every applicable unary rule (repr by their label ids) the parent grammar state
    private Int2ObjectMap<Int2ObjectMap<IntSet>> mergePartnersLeft; //given a state of the grammar X, returns a map that assigns to Y all the Zs s.t. Z -> m(X,Y)
    private Int2ObjectMap<Int2ObjectMap<IntSet>> mergePartnersRight; //given a state of the grammar X, returns a map that assigns to Y all the Zs s.t. Z -> m(Y,X)
    private Int2ObjectMap<IntSet> constants; //maps the constants (represented by label Id) to all corresponding grammar states
    private MergePartnerFinder[] mpf;
    private int mergeLabelId;

    public GrammarBRDecompAutoInstruments(String graph, String irtgFilePath) {
        //get auto from graph, and rest from irtg.
    }

    private GrammarBRDecompAutoInstruments() {
        //get auto from graph, and rest from irtg.
    }

    
    //only pass empty (new) graph algebra.
    public static GrammarBRDecompAutoInstruments makeIncompleteDecompositionInstrument(GraphAlgebra alg, SGraph graph, int nrSources) {
        Signature sig = alg.getSignature();
        Set<String> sources = new HashSet<>();
        GrammarBRDecompAutoInstruments res = new GrammarBRDecompAutoInstruments();
        res.intToGrammarState = new String[]{"X", "S"};
        res.grammarFinalStates = new IntOpenHashSet();
        res.grammarFinalStates.add(1);



        sig.addSymbol("merge", 2);
        
        for (int i = 0; i < nrSources; i++) {
            sources.add(String.valueOf(i));
        }

        for (String source1 : sources) {
            sig.addSymbol("f_" + source1, 1);
            for (String source2 : sources) {
                if (!source2.equals(source1)) {
                    sig.addSymbol("r_" + source1 + "_" + source2, 1);
                }
            }
        }

        for (String vName : graph.getAllNodeNames()) {
            sig.addSymbol("(" + vName + "<" + sources.iterator().next() + "> / " + graph.getNode(vName).getLabel() + ")", 0);
        }

        for (String vName1 : graph.getAllNodeNames()) {
            for (String vName2 : graph.getAllNodeNames()) {
                if (!vName1.equals(vName2)) {
                    GraphEdge e = graph.getGraph().getEdge(graph.getNode(vName1), graph.getNode(vName2));
                    if (e != null) {
                        String edgeLabel = e.getLabel();
                        Iterator<String> it = sources.iterator();
                        String s1 = it.next();
                        String s2 = it.next();
                        sig.addSymbol("(" + vName1 + "<" + s1 + "> :" + edgeLabel + " (" + vName2 + "<" + s2 + ">))", 0);
                        sig.addSymbol("(" + vName1 + "<" + s2 + "> :" + edgeLabel + " (" + vName2 + "<" + s1 + ">))", 0);
                    }
                }
            }
        }

        res.auto = (SGraphBRDecompositionAutomatonMPFTrusting)alg.decompose(graph, SGraphBRDecompositionAutomatonMPFTrusting.class);
        
        
        res.unaries = new Int2ObjectOpenHashMap<>();
        res.unaries.put(0, new Int2ObjectOpenHashMap<>());
        res.unaries.put(1, new Int2ObjectOpenHashMap<>());
        res.mergePartnersLeft = new Int2ObjectOpenHashMap<>();
        res.mergePartnersLeft.put(0, new Int2ObjectOpenHashMap<>());
        res.mergePartnersLeft.put(1, new Int2ObjectOpenHashMap<>());
        res.mergePartnersRight = new Int2ObjectOpenHashMap<>();
        res.mergePartnersRight.put(0, new Int2ObjectOpenHashMap<>());
        res.mergePartnersRight.put(1, new Int2ObjectOpenHashMap<>());
        res.mpf = new MergePartnerFinder[2];
        res.mpf[0] = new DynamicMergePartnerFinder(0, nrSources, res.auto.getNumberNodes(), res.auto);
        res.mpf[1] = new DynamicMergePartnerFinder(0, nrSources, res.auto.getNumberNodes(), res.auto);
        res.constants = new Int2ObjectOpenHashMap<>();

        IntSet onlyX = new IntOpenHashSet();
        onlyX.add(0);
        IntSet both = new IntOpenHashSet();
        both.add(0);
        both.add(1);
        
        

        for (String symbol : sig.getSymbols()) {
            int id = sig.getIdForSymbol(symbol);
            int arity = sig.getArity(id);
            
            if (arity == 0){
                res.constants.put(id, both);
            } else if (arity == 1) {
                res.unaries.get(0).put(id, both);
            } else if (arity == 2) {
                res.mergePartnersLeft.get(0).put(0, both);
                //res.mergePartnersRight.get(0).put(0, both);
            }
        }

        res.mergeLabelId = sig.getIdForSymbol("merge");
        
        return res;
    }

    
    
    
    
    public void iterateThroughRulesBottomUp(boolean printSteps)//looks up potential merges with tree structure in MergePartnerFinder
    {
        InitTuple iT = initAgenda(printSteps);
        LongList agenda = iT.agenda;
        LongSet seen = iT.seen;
        int nrMergeChecks = 0;
        int nrMerges = 0;

        for (int i = 0; i < agenda.size(); i++) {
            long next = agenda.getLong(i);
            int gSymb = NumbersCombine.getFirst(next);//the grammar state
            int a = NumbersCombine.getSecond(next);//the boundary representation (i.e. the subgraph state)

            if (printSteps) {
                System.out.println("Checking " + auto.getStateForId(a).toString() + " / " + intToGrammarState[gSymb] + ": ");
            }

            if (auto.getFinalStates().contains(a) && grammarFinalStates.contains(gSymb)) {
                System.out.println("Found final state!  " + auto.getStateForId(a).toString()+ " / " + intToGrammarState[gSymb]);//always print this, i guess
            }

            for (int u : unaries.get(gSymb).keySet()) {
                Iterable<Rule> rules = auto.getRulesBottomUp(u, new int[]{a});

                IntSet parentGStates = unaries.get(gSymb).get(u);
                for (int p : parentGStates) {
                    addRuleResults(rules.iterator(), agenda, seen, p, -1, printSteps);
                }

            }

            Int2ObjectMap<IntSet> hereMPLeft = mergePartnersLeft.get(gSymb);
            for (int b : hereMPLeft.keySet()) {
                IntList partners = mpf[b].getAllMergePartners(a);

                for (int d : partners) {
                    nrMergeChecks++;
                    Iterable<Rule> rules = auto.getRulesBottomUp(mergeLabelId, new int[]{a, d});

                    int doMerge = 0;
                    IntSet parentGStates = hereMPLeft.get(b);
                    for (int p : parentGStates) {
                        doMerge = Math.max(doMerge, addRuleResults(rules.iterator(), agenda, seen, p, d, printSteps));
                    }
                    nrMerges += doMerge;
                }
            }

            Int2ObjectMap<IntSet> hereMPRight = mergePartnersRight.get(gSymb);
            for (int b : hereMPRight.keySet()) {
                IntList partners = mpf[b].getAllMergePartners(a);

                for (int d : partners) {
                    nrMergeChecks++;
                    Iterable<Rule> rules = auto.getRulesBottomUp(mergeLabelId, new int[]{d, a});

                    int doMerge = 0;
                    IntSet parentGStates = hereMPRight.get(b);
                    for (int p : parentGStates) {
                        doMerge = Math.max(doMerge, addRuleResults(rules.iterator(), agenda, seen, p, d, printSteps));
                    }
                    nrMerges += doMerge;
                }
            }

            mpf[gSymb].insert(a);
            if (printSteps) {
                System.out.println("----------------");
            }
        }
        //mpFinder.print("MPF: ",0);
        System.out.println("Number of Merge Checks: " + String.valueOf(nrMergeChecks));
        System.out.println("Number of Merges: " + String.valueOf(nrMerges));
    }

    private int addRuleResults(Iterator<Rule> it, LongList agenda, LongSet seen, int parentGState, int partner, boolean printSteps) {
        if (it.hasNext()) {
            Rule rule = it.next();
            int newBR = rule.getParent();

            addBR(seen, agenda, parentGState, newBR, partner, printSteps);

            return 1;
        } else {
            return 0;
        }
    }

    private void addBR(LongSet seen, LongList agenda, int parentGState, int newBR, int partner, boolean printSteps) {

        if (printSteps && partner >= 0) {
            System.out.println("Result of merge with " + auto.getStateForId(partner).toString() + " is: " + auto.getStateForId(newBR).toString());
        }
        
        long res = NumbersCombine.combine(parentGState, newBR);
        if (!seen.contains(res)) {
            agenda.add(res);
            seen.add(res);
            if (printSteps) {
                System.out.println("added " + auto.getStateForId(newBR).toString() + " / " + intToGrammarState[parentGState]);
            }
        }

    }

    private InitTuple initAgenda(boolean printSteps) {
        InitTuple iT = new InitTuple();
        for (int c : constants.keySet()) {
            IntSet parentGStates = constants.get(c);
            for (int p : parentGStates) {
                Iterator<Rule> it = auto.getRulesBottomUp(c, new int[]{}).iterator();
                while (it.hasNext()) {
                    Rule rule = it.next();
                    int parent = rule.getParent();

                    long res = NumbersCombine.combine(p, parent);

                    if (!iT.agenda.contains(res)) {
                        iT.agenda.add(res);
                    }

                    if (printSteps) {
                        System.out.println("Added constant " + auto.getStateForId(rule.getParent()).toString());
                    }

                }
            }
        }
        iT.seen.addAll(Sets.newHashSet(iT.agenda));
        return iT;
    }

    private class InitTuple {

        public LongList agenda = new LongArrayList();
        public LongSet seen = new LongOpenHashSet();
    }
    
    public static void main(String[] args) throws Exception {
        
        GraphAlgebra alg = new GraphAlgebra();
        SGraph graph = alg.parseString(testString5);
        
        for (int i = 0; i<10; i++) {
            long startTime = System.currentTimeMillis();

            GrammarBRDecompAutoInstruments instr = makeIncompleteDecompositionInstrument(alg, graph, 4);
            instr.iterateThroughRulesBottomUp(false);

            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;
            System.out.println("Parse time is " + elapsedTime + "ms");
        }
        
    }
    
    
    
    
    private static final String testString1 = "(a / gamma  :alpha (b / beta))";
    private static final String testString2
            = "(n / need-01\n"
            + "      :ARG0 (t / they)\n"
            + "      :ARG1 (e / explain-01)\n"
            + "      :time (a / always))";
    private static final String testString3 = "(p / picture :domain (i / it) :topic (b2 / boa :mod (c2 / constrictor) :ARG0-of (s / swallow-01 :ARG1 (a / animal))))";
    private static final String testString4 = "(bel / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG0 (g / girl)  :ARG1 (l / like  :ARG0 g :ARG1 b)))";//the boy believes that the girl wants to like him.
    private static final String testString5 = "(bel1 / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG0 (g / girl)  :ARG1 (bel2 / believe  :ARG0 b  :ARG1 (l / like  :ARG0 g :ARG1 b))))";//the boy believes that the girl wants him to believe that she likes him.
    private static final String testString5sub1 = "(bel1 / believe  :ARG0 (b / boy)  :ARG1 (w / want  :ARG1 (bel2 / believe  :ARG0 b  )))";//kleines beispiel für graph der 3 sources braucht
    private static final String testString6 = "(s / see-01\n"
            + "      :ARG0 (i / i)\n"
            + "      :ARG1 (p / picture\n"
            + "            :mod (m / magnificent)\n"
            + "            :location (b2 / book\n"
            + "                  :name (n / name :op1 \"True\" :op2 \"Stories\" :op3 \"from\" :op4 \"Nature\")\n"
            + "                  :topic (f / forest\n"
            + "                        :mod (p2 / primeval))))\n"
            + "      :mod (o / once)\n"
            + "      :time (a / age-01\n"
            + "            :ARG1 i\n"
            + "            :ARG2 (t / temporal-quantity :quant 6\n"
            + "                  :unit (y / year))))";
    
    private static final String testString7 = "(a6 / and\n" +
"      :op1 (l / look-02\n" +
"            :ARG0 (p / picture\n" +
"                  :name (n / name :op1 \"Drawing\" :op2 \"Number\" :op3 \"Two\")\n" +
"                  :poss i)\n" +
"            :ARG1 (t2 / this))\n" +
"      :op2 (r / respond-01\n" +
"            :ARG0 (g / grown-up)\n" +
"            :ARG1 (i / i)\n" +
"            :ARG2 (a / advise-01\n" +
"                  :ARG0 g\n" +
"                  :ARG1 i\n" +
"                  :ARG2 (a3 / and\n" +
"                        :op1 (l2 / lay-01\n" +
"                              :ARG0 i\n" +
"                              :ARG1 (t3 / thing\n" +
"                                    :ARG1-of (d2 / draw-01\n" +
"                                          :ARG0 i)\n" +
"                                    :topic (b2 / boa\n" +
"                                          :mod (c2 / constrictor)\n" +
"                                          :mod (o / or\n" +
"                                                :op1 (i2 / inside)\n" +
"                                                :op2 (o2 / outside))))\n" +
"                              :ARG2 (a2 / aside))\n" +
"                        :op2 (d3 / devote-01\n" +
"                              :ARG0 i\n" +
"                              :ARG1 i\n" +
"                              :ARG2 (a4 / and\n" +
"                                    :op1 (g2 / geography)\n" +
"                                    :op2 (h / history)\n" +
"                                    :op3 (a5 / arithmetic)\n" +
"                                    :op4 (g3 / grammar))\n" +
"                              :mod (i3 / instead))))\n" +
"            :time (t4 / time\n" +
"                  :mod (t5 / this))))";//n = 31, sources needed = 3

}
