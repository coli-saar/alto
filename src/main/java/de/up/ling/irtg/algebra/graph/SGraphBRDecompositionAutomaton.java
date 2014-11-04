/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.IsiAmrParser;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.IntTrie;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.Util;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class SGraphBRDecompositionAutomaton extends TreeAutomaton<BoundaryRepresentation> {

    private final GraphAlgebra algebra;
    private final SGraph completeGraph;
    //private IntTrie<Int2ObjectMap<Iterable<Rule>>> storedRules;
    private final PairwiseShortestPaths pwsp;
    private final MergePartnerFinder mpFinder;
    private Map<BoundaryRepresentation, Set<Rule>> rulesTopDown;
    private Map<String, Integer> decompLengths;
    private Map<String, String> constantAbbreviations;
    private final Map<String, Integer> sourcenameToInt;
    private final Map<String, Integer> nodenameToInt;
    private final String[] intToSourcename;
    private final String[] intToNodename;

    SGraphBRDecompositionAutomaton(SGraph completeGraph, GraphAlgebra algebra, Signature signature) {
        super(signature);

        this.algebra = algebra;

        //find all sources used in algebra:
        Set<String> sources = new HashSet<>();
        sourcenameToInt = new HashMap<>();
        nodenameToInt = new HashMap<>();
        for (String symbol : signature.getSymbols())//this adds all sources from the signature, but be careful, this is kind of a hack. Maybe better just give this a list of sources directly?
        {
            if (symbol.startsWith(GraphAlgebra.OP_FORGET)) {
                String[] parts = symbol.split("_");
                sources.add(parts[1]);
            } else if (symbol.startsWith(GraphAlgebra.OP_RENAME)) {
                String[] parts = symbol.split("_");
                if (parts.length == 2) {
                    sources.add("root");
                }
                for (int i = 1; i < parts.length; i++) {
                    sources.add(parts[i]);
                }
            }
        }

        intToSourcename = new String[sources.size()];
        int i = 0;
        for (String source : sources) {
            sourcenameToInt.put(source, i);
            intToSourcename[i] = source;
            i++;
        }
        intToNodename = new String[completeGraph.getAllNodeNames().size()];
        i = 0;
        for (String nodename : completeGraph.getAllNodeNames()) {
            nodenameToInt.put(nodename, i);
            intToNodename[i] = nodename;
            i++;
        }

        mpFinder = new DynamicMergePartnerFinder(0, sources.size(), completeGraph.getAllNodeNames().size(), this);

        //storedRules = new IntTrie<>();
        this.completeGraph = completeGraph;
        int x = addState(new BoundaryRepresentation(completeGraph, this));
        finalStates.add(x);

        pwsp = new PairwiseShortestPaths(completeGraph, this);
    }

    private Rule makeRule(BoundaryRepresentation parent, int labelId, int[] childStates) {
        int parentState = addState(parent);
        return createRule(parentState, labelId, childStates, 1);
    }

    private static <E> Iterable<E> sing(E object) {
        return Collections.singletonList(object);
    }

    private Iterable<Rule> sing(BoundaryRepresentation parent, int labelId, int[] childStates) {
//        System.err.println("-> make rule, parent= " + parent);
        return sing(makeRule(parent, labelId, childStates));
    }

    /*private Iterable<Rule> memoize(Iterable<Rule> rules, int labelId, int[] childStates) {
     // memoize rule
     Int2ObjectMap<Iterable<Rule>> rulesHere = storedRules.get(childStates);

     if (rulesHere == null) {
     rulesHere = new Int2ObjectOpenHashMap<>();
     storedRules.put(childStates, rulesHere);
     }

     rulesHere.put(labelId, rules);
        
     // add final state if needed
     /*for( Rule rule : rules ) {
     BoundaryRepresentation parent = getStateForId(rule.getParent());
            
     if( parent.isIdenticalExceptSources(completeGraph, completeGraph)) {
     finalStates.add(rule.getParent());
     }
     }*/
    //    return rules;
    // }
    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        /*Int2ObjectMap<Iterable<Rule>> rulesHere = storedRules.get(childStates);

         // check stored rules
         if (rulesHere != null) {
         Iterable<Rule> rules = rulesHere.get(labelId);
         if (rules != null) {
         return rules;
         }
         }*/

        String label = signature.resolveSymbolId(labelId);
        //List<BoundaryRepresentation> children = Arrays.stream(childStates).mapToObj(q -> getStateForId(q)).collect(Collectors.toList());
        List<BoundaryRepresentation> children = new ArrayList<>();
        for (int i = 0; i < childStates.length; i++) {
            children.add(getStateForId(childStates[i]));
        }

        try {
            if (label == null) {
                return Collections.EMPTY_LIST;
            } else if (label.equals(GraphAlgebra.OP_MERGE)) {
                if (!children.get(0).isMergeable(pwsp, children.get(1))) { // ensure result is connected
                    return Collections.EMPTY_LIST;//memoize(Collections.EMPTY_LIST, labelId, childStates);
                } else {
                    BoundaryRepresentation result = children.get(0).merge(children.get(1));

                    if (result == null) {
//                        System.err.println("merge returned null: " + children.get(0) + " with " + children.get(1));
                        return Collections.EMPTY_LIST;//memoize(Collections.EMPTY_LIST, labelId, childStates);
                    } else {
                        //result.setEqualsMeansIsomorphy(false);//is this a problem??
                        return sing(result, labelId, childStates);//memoize(sing(result, labelId, childStates), labelId, childStates);
                    }
                }
            } else if (label.startsWith(GraphAlgebra.OP_RENAME)
                    || label.startsWith(GraphAlgebra.OP_FORGET)
                    || label.startsWith(GraphAlgebra.OP_FORGET_ALL)
                    || label.startsWith(GraphAlgebra.OP_FORGET_ALL_BUT_ROOT)
                    || label.startsWith(GraphAlgebra.OP_FORGET_EXCEPT)) {

                BoundaryRepresentation arg = children.get(0);

                for (Integer sourceToForget : arg.getForgottenSources(label, this))//check if we may forget.
                {
                    if (!arg.isForgetAllowed(sourceToForget, completeGraph, this)) {
                        return Collections.EMPTY_LIST;//memoize(Collections.EMPTY_LIST, labelId, childStates);
                    }
                }

                // now we can apply the operation.
                BoundaryRepresentation result = arg.applyForgetRename(label, this);// maybe do the above check in here? might be more efficient.

                if (result == null) {
//                    System.err.println(label + " returned null: " + children.get(0));
                    return Collections.EMPTY_LIST;//memoize(Collections.EMPTY_LIST, labelId, childStates);
                } else {
                    //result.setEqualsMeansIsomorphy(false);//is this a problem??
                    return sing(result, labelId, childStates);//memoize(sing(result, labelId, childStates), labelId, childStates);
                }
            } else {
                List<Rule> rules = new ArrayList<>();
                SGraph sgraph = IsiAmrParser.parse(new StringReader(label));

                if (sgraph == null) {
//                    System.err.println("Unparsable operation: " + label);
                    return Collections.EMPTY_LIST;//memoize(Collections.EMPTY_LIST, labelId, childStates);
                }

//                System.err.println(" - looking for matches of " + sgraph + " in " + completeGraph);
                completeGraph.foreachMatchingSubgraph(sgraph, matchedSubgraph -> {
//                    System.err.println(" -> make terminal rule, parent = " + matchedSubgraph);
                    if (!hasCrossingEdgesFromNodes(matchedSubgraph.getAllNonSourceNodenames(), matchedSubgraph)) {
                        matchedSubgraph.setEqualsMeansIsomorphy(false);
                        rules.add(makeRule(new BoundaryRepresentation(matchedSubgraph, this), labelId, childStates));
                    } else {
//                        System.err.println("match " + matchedSubgraph + " has crossing edges from nodes");
                    }
                });

                return rules;//memoize(rules, labelId, childStates);
            }
        } catch (de.up.ling.irtg.algebra.graph.ParseException ex) {
            throw new IllegalArgumentException("Could not parse operation \"" + label + "\": " + ex.getMessage());
        }
    }

    private boolean hasCrossingEdgesFromNodes(Iterable<String> nodenames, SGraph subgraph) {
        for (String nodename : nodenames) {
            if (!subgraph.isSourceNode(nodename)) {
                GraphNode node = completeGraph.getNode(nodename);

                if (!completeGraph.getGraph().containsVertex(node)) {
                    System.err.println("*** TERRIBLE ERROR ***");
                    System.err.println(" int graph: " + completeGraph);
                    System.err.println("can't find node " + node);
                    System.err.println(" - node name: " + nodename);
                    assert false;
                }

                for (GraphEdge edge : completeGraph.getGraph().incomingEdgesOf(node)) {
                    if (subgraph.getNode(edge.getSource().getName()) == null) {
                        return true;
                    }
                }

                for (GraphEdge edge : completeGraph.getGraph().outgoingEdgesOf(node)) {
                    if (subgraph.getNode(edge.getTarget().getName()) == null) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return false;
    }

    @Override
    public boolean supportsTopDownQueries() {
        return false;
    }

    @Override
    public boolean supportsBottomUpQueries() {
        return true;
    }

    public void iterateThroughRulesBottomUpNaive(GraphAlgebra alg, boolean printSteps) {
        Map<String, Integer> symbols = signature.getSymbolsWithArities();
        Set<String> constants = new HashSet<String>();
        Set<String> unisymbols = new HashSet<String>();
        Set<String> bisymbols = new HashSet<String>();
        for (String s : symbols.keySet()) {
            if (symbols.get(s) == 0) {
                constants.add(s);
            } else if (symbols.get(s) == 1) {
                unisymbols.add(s);
            } else if (symbols.get(s) == 2) {
                bisymbols.add(s);
            }
        }
        List<BoundaryRepresentation> agenda = new ArrayList<BoundaryRepresentation>();
        for (String c : constants) {
            try {
                Iterator<Rule> it = getRulesBottomUp(signature.getIdForSymbol(c), new int[]{}).iterator();
                while (it.hasNext()) {
                    BoundaryRepresentation newBR = getStateForId(it.next().getParent());
                    agenda.add(newBR);//assuming here that no (or at least not too many) constants appear multiple times. Otherwise should check for duplicates
                    if (printSteps) {
                        System.out.println("Added constant " + newBR.toString());
                    }
                }
            } catch (java.lang.Exception e) {

            }
        }
        List<BoundaryRepresentation> done = new ArrayList<BoundaryRepresentation>();
        Set<BoundaryRepresentation> seen = new HashSet<BoundaryRepresentation>();
        seen.addAll(Sets.newHashSet(agenda));
        for (int i = 0; i < agenda.size(); i++) {
            BoundaryRepresentation a = agenda.get(i);
            if (printSteps) {
                System.out.println("Checking " + a.toString());
            }
            int id = getIdForState(a);
            if (finalStates.contains(id)) {
                System.out.println("Found final state!  " + a.toString());//always print this, i guess
            }
            for (String u : unisymbols) {
                Iterator<Rule> it = getRulesBottomUp(signature.getIdForSymbol(u), new int[]{getIdForState(a)}).iterator();
                if (it.hasNext()) {
                    Rule rule = it.next();
                    BoundaryRepresentation newBR = getStateForId(rule.getParent());
                    if (!seen.contains(newBR)) {
                        agenda.add(newBR);
                        seen.add(newBR);
                        if (printSteps) {
                            System.out.println("Result of " + rule.getLabel(this) + " is: " + newBR.toString());
                        }
                    }
                }
            }
            for (String b : bisymbols) {
                for (BoundaryRepresentation d : done) {
                    Iterator<Rule> it = getRulesBottomUp(signature.getIdForSymbol(b), new int[]{getIdForState(a), getIdForState(d)}).iterator();
                    if (it.hasNext()) {
                        BoundaryRepresentation newBR = getStateForId(it.next().getParent());
                        if (!seen.contains(newBR)) {
                            agenda.add(newBR);
                            seen.add(newBR);
                            if (printSteps) {
                                System.out.println("Result of merge with " + d.toString() + " is: " + newBR.toString());
                            }
                        }
                    }
                }
            }
            done.add(a);
        }
    }

    public void iterateThroughRulesBottomUp1(GraphAlgebra alg, boolean printSteps, boolean makeRulesTopDown)//looks up potential merges with tree structure in MergePartnerFinder
    {
        if (makeRulesTopDown) {
            rulesTopDown = new HashMap<>();
            constantAbbreviations = new HashMap<>();
        }
        Map<String, Integer> symbols = signature.getSymbolsWithArities();
        Set<String> constants = new HashSet<>();
        Set<String> unisymbols = new HashSet<>();
        Set<String> bisymbols = new HashSet<>();
        int j = 0;
        for (String s : symbols.keySet()) {
            if (symbols.get(s) == 0) {
                constants.add(s);
                if (makeRulesTopDown) {
                    constantAbbreviations.put(s, "C" + String.valueOf(j));
                    System.out.println("C" + String.valueOf(j) + " represents " + s);
                    j++;
                }
            } else if (symbols.get(s) == 1) {
                unisymbols.add(s);
            } else if (symbols.get(s) == 2) {
                bisymbols.add(s);
            }
        }
        IntList agenda = new IntArrayList();
        for (String c : constants) {
            try {
                Iterator<Rule> it = getRulesBottomUp(signature.getIdForSymbol(c), new int[]{}).iterator();
                while (it.hasNext()) {
                    Rule rule = it.next();
                    agenda.add(rule.getParent());//assuming here that no (or at least not too many) constants appear multiple times. Otherwise should check for duplicates
                    if (printSteps) {
                        System.out.println("Added constant " + getStateForId(rule.getParent()).toString(this));
                    }
                    if (makeRulesTopDown) {
                        addRuleTopDown(rule);
                    }
                }
            } catch (java.lang.Exception e) {

            }
        }
        IntSet seen = new IntOpenHashSet();
        seen.addAll(Sets.newHashSet(agenda));
        int nrMergeChecks = 0;
        int nrMerges = 0;
        for (int i = 0; i < agenda.size(); i++) {
            int a = agenda.get(i);
            
            if (printSteps) {
                System.out.println("Checking " + getStateForId(a).toString(this));
            }
            
            if (finalStates.contains(a)) {
                System.out.println("Found final state!  " + getStateForId(a).toString(this));//always print this, i guess
            }
            
            for (String u : unisymbols) {
                Iterator<Rule> it = getRulesBottomUp(signature.getIdForSymbol(u), new int[]{a}).iterator();
                if (it.hasNext()) {
                    Rule rule = it.next();
                    int newBR = rule.getParent();
                    if (!seen.contains(newBR)) {
                        agenda.add(newBR);
                        seen.add(newBR);
                        if (printSteps) {
                            System.out.println("Result of " + rule.getLabel(this) + " is: " + getStateForId(newBR).toString(this));
                        }
                    }
                    
                    if (makeRulesTopDown) {
                        addRuleTopDown(rule);
                    }

                }
            }
            
            for (String b : bisymbols) {
                IntSet partners = mpFinder.getAllMergePartners(a);
                //IntListIterator pIt = partners.listIterator();
                
                for (int d: partners) {
                    //int d = pIt.nextInt();
                    nrMergeChecks++;
                    Iterator<Rule> it = getRulesBottomUp(signature.getIdForSymbol(b), new int[]{a, d}).iterator();
                    
                    if (it.hasNext()) {
                        nrMerges++;
                        Rule rule = it.next();
                        int newBR = rule.getParent();
                        
                        if (!seen.contains(newBR)) {
                            agenda.add(newBR);
                            seen.add(newBR);
                            if (printSteps) {
                                System.out.println("Result of merge with " + getStateForId(d).toString(this) + " is: " + getStateForId(newBR).toString(this));
                            }
                        }
                        
                        if (makeRulesTopDown) {
                            addRuleTopDown(rule);
                        }
                    }
                }
            }
            mpFinder.insert(a);
        }
        System.out.println("Number of Merge Checks: " + String.valueOf(nrMergeChecks));
        System.out.println("Number of Merges: " + String.valueOf(nrMerges));
    }

    public void iterateThroughRulesBottomUp1Clean(GraphAlgebra alg)//looks up potential merges with tree structure in MergePartnerFinder
    {
        Map<String, Integer> symbols = signature.getSymbolsWithArities();
        Set<String> constants = new HashSet<>();
        Set<String> unisymbols = new HashSet<>();
        Set<String> bisymbols = new HashSet<>();
        int j = 0;
        symbols.keySet().stream().forEach((s) -> {
            if (symbols.get(s) == 0) {
                constants.add(s);
            } else if (symbols.get(s) == 1) {
                unisymbols.add(s);
            } else if (symbols.get(s) == 2) {
                bisymbols.add(s);
            }
        });
        IntList agenda = new IntArrayList();
        constants.stream().forEach((c) -> {
            try {
                Iterator<Rule> it = getRulesBottomUp(signature.getIdForSymbol(c), new int[]{}).iterator();
                while (it.hasNext()) {
                    Rule rule = it.next();
                    agenda.add(rule.getParent());//assuming here that no (or at least not too many) constants appear multiple times. Otherwise should check for duplicates
                }
            } catch (java.lang.Exception e) {

            }
        });
        IntSet seen = new IntOpenHashSet();
        seen.addAll(Sets.newHashSet(agenda));
        for (int i = 0; i < agenda.size(); i++) {
            int a = agenda.get(i);
            unisymbols.stream().map((u) -> getRulesBottomUp(signature.getIdForSymbol(u), new int[]{a}).iterator()).filter((it) -> (it.hasNext())).map((it) -> it.next()).map((rule) -> rule.getParent()).filter((newBR) -> (!seen.contains(newBR))).map((newBR) -> {
                agenda.add(newBR);
                return newBR;
            }).forEach((newBR) -> {
                seen.add(newBR);
            });
            bisymbols.stream().forEach((b) -> {
                IntSet partners = mpFinder.getAllMergePartners(a);
                for (int d : partners) {
                    Iterator<Rule> it = getRulesBottomUp(signature.getIdForSymbol(b), new int[]{a, d}).iterator();

                    if (it.hasNext()) {
                        Rule rule = it.next();
                        int newBR = rule.getParent();
                        addBR(seen, agenda, newBR);
                    }
                }
            });
            mpFinder.insert(a);
        }
    }

    private void addBR(IntSet seen, IntList agenda, int newBR) {
        if (!seen.contains(newBR)) {
            agenda.add(newBR);
            seen.add(newBR);
        }
    }

    private void addRuleTopDown(Rule rule) {
        BoundaryRepresentation rep = getStateForId(rule.getParent());
        if (rulesTopDown.containsKey(rep)) {
            Set<Rule> set = rulesTopDown.get(rep);
            set.add(rule);
        } else {
            Set<Rule> set = new HashSet<>();
            set.add(rule);
            rulesTopDown.put(rep, set);
        }
    }

    public void printDecompositionsTopDown() {
        for (int finalState : finalStates) {
            BoundaryRepresentation finalRep = getStateForId(finalState);
            decompLengths = new HashMap<>();
            Set<String> possibleDecompositions = getPossibleDecompositionsTopDown(finalRep, new HashSet<>());
            for (String decomp : possibleDecompositions) {
                System.out.println(decomp);
            }
        }
    }

    public void printShortestDecompositionsTopDown() {
        for (int finalState : finalStates) {
            BoundaryRepresentation finalRep = getStateForId(finalState);
            if (!rulesTopDown.containsKey(finalRep)) {
                System.out.println("no parse for " + finalRep.toString());
            } else {
                decompLengths = new HashMap<>();
                Set<String> possibleDecompositions = getPossibleDecompositionsTopDown(finalRep, new HashSet<>());
                if (!possibleDecompositions.isEmpty()) {
                    int shortest = decompLengths.get(possibleDecompositions.iterator().next());
                    for (String decomp : possibleDecompositions) {
                        shortest = Math.min(shortest, decompLengths.get(decomp));
                    }
                    for (String decomp : possibleDecompositions) {
                        if (decompLengths.get(decomp) == shortest) {
                            System.out.println(decomp);
                        }
                    }
                }
            }
        }
    }

    private Set<String> getPossibleDecompositionsTopDown(BoundaryRepresentation rep, Set<BoundaryRepresentation> alreadySeen) {
        Set<Rule> applicableRules = rulesTopDown.get(rep);
        Set<String> res = new HashSet<>();
        for (Rule rule : applicableRules) {
            if (rule.getArity() == 0) {
                String resString = constantAbbreviations.get(rule.getLabel(this));//rule.getLabel(this);
                res.add(resString);
                decompLengths.put(resString, 1);
            } else if (rule.getArity() == 1) {
                int childId = rule.getChildren()[0];
                BoundaryRepresentation child = getStateForId(childId);
                if (!alreadySeen.contains(child)) {
                    Set<BoundaryRepresentation> newSeen = new HashSet<>();
                    newSeen.addAll(alreadySeen);
                    newSeen.add(rep);
                    for (String childDecomp : getPossibleDecompositionsTopDown(child, newSeen)) {
                        String resString = rule.getLabel(this) + "(" + childDecomp + ")";
                        res.add(resString);
                        decompLengths.put(resString, decompLengths.get(childDecomp) + 1);
                    }
                }
            } else if (rule.getArity() == 2) {
                int childLeftId = rule.getChildren()[0];
                int childRightId = rule.getChildren()[1];
                BoundaryRepresentation childLeft = getStateForId(childLeftId);
                BoundaryRepresentation childRight = getStateForId(childRightId);
                if (!(alreadySeen.contains(childLeft) || alreadySeen.contains(childRight))) {
                    Set<BoundaryRepresentation> newSeen = new HashSet<>();
                    newSeen.addAll(alreadySeen);
                    newSeen.add(rep);
                    for (String childLeftDecomp : getPossibleDecompositionsTopDown(childLeft, newSeen)) {
                        for (String childRightDecomp : getPossibleDecompositionsTopDown(childRight, newSeen)) {
                            //String resString = "(" + childLeftDecomp + "||" + childRightDecomp + ")";
                            String resString = " " + childLeftDecomp + "||" + childRightDecomp + " ";
                            res.add(resString);
                            decompLengths.put(resString, decompLengths.get(childLeftDecomp) + decompLengths.get(childRightDecomp) + 1);
                        }
                    }
                }
            }
        }
        return res;
    }

    public void printAllRulesTopDown() {
        int counter = 0;
        for (BoundaryRepresentation rep : rulesTopDown.keySet()) {
            for (Rule rule : rulesTopDown.get(rep)) {
                counter++;
                String children = "";
                if (rule.getArity() == 0) {
                    children = "";
                } else if (rule.getArity() == 1) {
                    children = getStateForId(rule.getChildren()[0]).toString(this);
                } else if (rule.getArity() == 2) {
                    children = getStateForId(rule.getChildren()[0]).toString(this) + " , " + getStateForId(rule.getChildren()[1]).toString(this);
                }
                System.out.println(getStateForId(rule.getParent()).toString(this) + " -> " + rule.getLabel(this) + "(" + children + ")");
            }
        }
        System.out.println("That is a total of " + String.valueOf(counter) + " Rules.");
    }

    public int getIntForSource(String source) {
        return sourcenameToInt.get(source);
    }

    public int getIntForNode(String nodename) {
        return nodenameToInt.get(nodename);
    }

    public String getSourceForInt(int source) {
        return intToSourcename[source];
    }

    public String getNodeForInt(int node) {
        return intToNodename[node];
    }

    public int getNrSources() {
        return intToSourcename.length;
    }

}
