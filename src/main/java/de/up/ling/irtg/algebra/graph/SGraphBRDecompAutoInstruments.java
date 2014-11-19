/*
 * To change auto license header, choose License Headers in Project Properties.
 * To change auto template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.collect.Sets;
import de.up.ling.irtg.automata.Rule;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntHeapPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author jonas
 */
public class SGraphBRDecompAutoInstruments {
    
    private final SGraphBRDecompositionAutomaton auto;
    private final MergePartnerFinder mpFinder;
    private Map<String, String> constantAbbreviations;
    private int nrParses;
    
    public SGraphBRDecompAutoInstruments(SGraphBRDecompositionAutomaton auto, int sourceCount, int nodeCount){
        this.auto = auto;
        mpFinder = new DynamicMergePartnerFinder(0, sourceCount, nodeCount, auto);
    }
    
    
    public void iterateThroughRulesBottomUpNaive(GraphAlgebra alg, boolean printSteps) {//old
        Map<String, Integer> symbols = auto.getSignature().getSymbolsWithArities();
        Set<String> constants = new HashSet<>();
        Set<String> unisymbols = new HashSet<>();
        Set<String> bisymbols = new HashSet<>();
        for (String s : symbols.keySet()) {
            if (symbols.get(s) == 0) {
                constants.add(s);
            } else if (symbols.get(s) == 1) {
                unisymbols.add(s);
            } else if (symbols.get(s) == 2) {
                bisymbols.add(s);
            }
        }
        List<BoundaryRepresentation> agenda = new ArrayList<>();
        for (String c : constants) {
            try {
                Iterator<Rule> it = auto.getRulesBottomUpMPF(auto.getSignature().getIdForSymbol(c), new int[]{}).iterator();
                while (it.hasNext()) {
                    BoundaryRepresentation newBR = auto.getStateForId(it.next().getParent());
                    agenda.add(newBR);//assuming here that no (or at least not too many) constants appear multiple times. Otherwise should check for duplicates
                    if (printSteps) {
                        System.out.println("Added constant " + newBR.toString());
                    }
                }
            } catch (java.lang.Exception e) {

            }
        }
        List<BoundaryRepresentation> done = new ArrayList<>();
        Set<BoundaryRepresentation> seen = new HashSet<>();
        seen.addAll(Sets.newHashSet(agenda));
        for (int i = 0; i < agenda.size(); i++) {
            BoundaryRepresentation a = agenda.get(i);
            if (printSteps) {
                System.out.println("Checking " + a.toString());
            }
            int id = auto.getIdForState(a);
            if (auto.getFinalStates().contains(id)) {
                System.out.println("Found final state!  " + a.toString());//always print auto, i guess
            }
            for (String u : unisymbols) {
                Iterator<Rule> it = auto.getRulesBottomUpMPF(auto.getSignature().getIdForSymbol(u), new int[]{auto.getIdForState(a)}).iterator();
                if (it.hasNext()) {
                    Rule rule = it.next();
                    BoundaryRepresentation newBR = auto.getStateForId(rule.getParent());
                    if (!seen.contains(newBR)) {
                        agenda.add(newBR);
                        seen.add(newBR);
                        if (printSteps) {
                            System.out.println("Result of " + rule.getLabel(auto) + " is: " + newBR.toString());
                        }
                    }
                }
            }
            for (String b : bisymbols) {
                for (BoundaryRepresentation d : done) {
                    Iterator<Rule> it = auto.getRulesBottomUpMPF(auto.getSignature().getIdForSymbol(b), new int[]{auto.getIdForState(a), auto.getIdForState(d)}).iterator();
                    if (it.hasNext()) {
                        BoundaryRepresentation newBR = auto.getStateForId(it.next().getParent());
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
        InitTuple iT = initAgenda(makeRulesTopDown, printSteps);
        IntList agenda = iT.agenda;
        IntSet seen = iT.seen;
        int nrMergeChecks = 0;
        int nrMerges = 0;
        
        for (int i = 0; i < agenda.size(); i++) {
            int a = agenda.get(i);
            
            
            if (printSteps) {
                System.out.println("Checking " + auto.getStateForId(a).toString());
            }
            
            if (auto.getFinalStates().contains(a)) {
                System.out.println("Found final state!  " + auto.getStateForId(a).toString());//always print this, i guess
            }
            
            for (String u : iT.unisymbols) {
                Iterator<Rule> it = auto.getRulesBottomUpMPF(auto.getSignature().getIdForSymbol(u), new int[]{a}).iterator();
                
                addRuleResults(it, agenda, seen, -1, printSteps, makeRulesTopDown);
                
            }
            
            for (String b : iT.bisymbols) {
                IntList partners = mpFinder.getAllMergePartners(a);
                
                for (int d: partners) {
                    nrMergeChecks++;
                    Iterator<Rule> it = auto.getRulesBottomUpMPF(auto.getSignature().getIdForSymbol(b), new int[]{a, d}).iterator();
                    
                    nrMerges += addRuleResults(it, agenda, seen, d, printSteps, makeRulesTopDown);
                }
            }
            
            mpFinder.insert(a);
        }
        mpFinder.print("MPF: ",0);
        System.out.println("Number of Merge Checks: " + String.valueOf(nrMergeChecks));
        System.out.println("Number of Merges: " + String.valueOf(nrMerges));
        System.out.println("Number of Parses: " + String.valueOf(nrParses));
    }
    
    

    public boolean iterateThroughRulesBottomUp1Clean(GraphAlgebra alg)//looks up potential merges with tree structure in MergePartnerFinder
    {
        boolean ret = false;
        InitTuple iT = initAgenda(false, false);
        IntList agenda = iT.agenda;
        IntSet seen = iT.seen;
        int nrMerges = 0;
       
        for (int i = 0; i < agenda.size(); i++) {
            int a = agenda.get(i);
            
            //if (auto.getFinalStates().contains(a)) {
                //System.out.println("Found final state!  " + auto.getStateForId(a).toString(auto));//always print this, i guess
            //    nrParses++;
            //}
            if (auto.getFinalStates().contains(a)){
                ret = true;
            }
            
            iT.unisymbols.stream().map((u) -> auto.getRulesBottomUpMPF(auto.getSignature().getIdForSymbol(u), new int[]{a}).iterator()).forEach((it) -> {
                addRuleResults(it, agenda, seen, -1, false, false);
            });
            
            for (String b : iT.bisymbols) {
                IntList partners = mpFinder.getAllMergePartners(a);
                
                for (int d: partners) {
                    Iterator<Rule> it = auto.getRulesBottomUpMPF(auto.getSignature().getIdForSymbol(b), new int[]{a, d}).iterator();
                    
                    nrMerges += addRuleResults(it, agenda, seen, d, false, false);
                }
            }
            mpFinder.insert(a);
        }
        System.out.println("Number of Parses: " + nrParses + ";   Number of Merges: " + nrMerges);
        return ret;
    }
    
    
    public boolean doesAccept(GraphAlgebra alg)
    {
        InitTuplePriority iT = initAgendaPriority(false, false);
        IntPriorityQueue agenda = iT.agenda;
        IntSet seen = iT.seen;
        
        
        while (!agenda.isEmpty()) {
            int a = agenda.dequeueInt();
            
            
            //auto.getStateForId(a).printSources();//for debugging/understanding
            
            if (auto.getFinalStates().contains(a)) {
                return true;
            }
            
            for (String u : iT.unisymbols) {
                Iterator<Rule> it = auto.getRulesBottomUpMPF(auto.getSignature().getIdForSymbol(u), new int[]{a}).iterator();
                
                
                
                addRuleResults(it, agenda, seen, -1, false, false);
                
            }
            
            for (String b : iT.bisymbols) {
                IntList partners = mpFinder.getAllMergePartners(a);
                
                for (int d: partners) {
                    Iterator<Rule> it = auto.getRulesBottomUpMPF(auto.getSignature().getIdForSymbol(b), new int[]{a, d}).iterator();
                    
                    addRuleResults(it, agenda, seen, -1, false, false);
                }
            }
            
            mpFinder.insert(a);
        }
        return false;
    }
    
    
    private InitTuple initAgenda(boolean makeRulesTopDown, boolean printSteps)
    {
        nrParses = 0;
        InitTuple iT = new InitTuple();
        if (makeRulesTopDown) {
            auto.rulesTopDown = new HashMap<>();
            constantAbbreviations = new HashMap<>();
        }
        Map<String, Integer> symbols = auto.getSignature().getSymbolsWithArities();
        int j = 0;
        for (String s : symbols.keySet()) {
            if (symbols.get(s) == 0) {
                iT.constants.add(s);
                if (makeRulesTopDown) {
                    constantAbbreviations.put(s, "C" + String.valueOf(j));
                    System.out.println("C" + String.valueOf(j) + " represents " + s);
                    j++;
                }
            } else if (symbols.get(s) == 1) {
                iT.unisymbols.add(s);
            } else if (symbols.get(s) == 2) {
                iT.bisymbols.add(s);
            }
        }
        for (String c : iT.constants) {
            try {
                Iterator<Rule> it = auto.getRulesBottomUpMPF(auto.getSignature().getIdForSymbol(c), new int[]{}).iterator();
                while (it.hasNext()) {
                    Rule rule = it.next();
                    int parent = rule.getParent();
                    
                    if (!iT.agenda.contains(parent))
                        iT.agenda.add(parent);//assuming here that no (or at least not too many) constants appear multiple times. Otherwise should check for duplicates
                    
                    if (printSteps) {
                        System.out.println("Added constant " + auto.getStateForId(rule.getParent()).toString());
                    }
                    
                    if (makeRulesTopDown) {
                        addRuleTopDown(rule);
                    }
                }
            } catch (java.lang.Exception e) {

            }
        }
        iT.seen.addAll(Sets.newHashSet(iT.agenda));
        return iT;
    }
    
    private InitTuplePriority initAgendaPriority(boolean makeRulesTopDown, boolean printSteps)
    {
        InitTuplePriority iT = new InitTuplePriority(auto);
        if (makeRulesTopDown) {
            auto.rulesTopDown = new HashMap<>();
            constantAbbreviations = new HashMap<>();
        }
        Map<String, Integer> symbols = auto.getSignature().getSymbolsWithArities();
        int j = 0;
        for (String s : symbols.keySet()) {
            if (symbols.get(s) == 0) {
                iT.constants.add(s);
                if (makeRulesTopDown) {
                    constantAbbreviations.put(s, "C" + String.valueOf(j));
                    System.out.println("C" + String.valueOf(j) + " represents " + s);
                    j++;
                }
            } else if (symbols.get(s) == 1) {
                iT.unisymbols.add(s);
            } else if (symbols.get(s) == 2) {
                iT.bisymbols.add(s);
            }
        }
        for (String c : iT.constants) {
            try {
                Iterator<Rule> it = auto.getRulesBottomUpMPF(auto.getSignature().getIdForSymbol(c), new int[]{}).iterator();
                while (it.hasNext()) {
                    Rule rule = it.next();
                    int parent = rule.getParent();
                    
                    if (!iT.seen.contains(parent))
                    {
                        iT.agenda.enqueue(parent);
                        iT.seen.add(parent);
                    }
                    
                    if (printSteps) {
                        System.out.println("Added constant " + auto.getStateForId(rule.getParent()).toString());
                    }
                    
                    if (makeRulesTopDown) {
                        addRuleTopDown(rule);
                    }
                }
            } catch (java.lang.Exception e) {

            }
        }
        return iT;
    }
    
    private int addRuleResults(Iterator<Rule> it, IntList agenda, IntSet seen, int partner, boolean printSteps, boolean makeRulesTopDown)
    {
        if (it.hasNext()) {
            Rule rule = it.next();
            int newBR = rule.getParent();
            if (auto.getFinalStates().contains(newBR))
                nrParses++;

            addBR(seen, agenda, newBR, partner, printSteps);

            if (makeRulesTopDown) {
                addRuleTopDown(rule);
            }
            return 1;
        } else {
            return 0;
        }
    }
    
    private int addRuleResults(Iterator<Rule> it, IntPriorityQueue agenda, IntSet seen, int partner, boolean printSteps, boolean makeRulesTopDown)
    {
        if (it.hasNext()) {
            Rule rule = it.next();
            int newBR = rule.getParent();

            
            //if (rule.getLabel(auto).startsWith("f"))
            //    System.out.println("Forget: "+rule.getLabel(auto));
            
            addBR(seen, agenda, newBR, partner, printSteps);

            if (makeRulesTopDown) {
                addRuleTopDown(rule);
            }
            return 1;
        } else {
            return 0;
        }
    }

    private void addBR(IntSet seen, IntList agenda, int newBR, int partner, boolean printSteps) {
        
        if (printSteps && partner >= 0) {
            System.out.println("Result of merge with " + auto.getStateForId(partner).toString() + " is: " + auto.getStateForId(newBR).toString());
        }
        
        if (!seen.contains(newBR)) {
            agenda.add(newBR);
            seen.add(newBR);
        }
        
    }
    
    
    private void addBR(IntSet seen, IntPriorityQueue agenda, int newBR, int partner, boolean printSteps) {
        if (!seen.contains(newBR)) {
            agenda.enqueue(newBR);
            seen.add(newBR);
            if (printSteps && partner >= 0) {
                System.out.println("Result of merge with " + auto.getStateForId(partner).toString() + " is: " + auto.getStateForId(newBR).toString());
            }
        }
    }

    private void addRuleTopDown(Rule rule) {
        BoundaryRepresentation rep = auto.getStateForId(rule.getParent());
        if (auto.rulesTopDown.containsKey(rep)) {
            Set<Rule> set = auto.rulesTopDown.get(rep);
            set.add(rule);
        } else {
            Set<Rule> set = new HashSet<>();
            set.add(rule);
            auto.rulesTopDown.put(rep, set);
        }
    }

    public void printDecompositionsTopDown() {
        for (int finalState : auto.getFinalStates()) {
            BoundaryRepresentation finalRep = auto.getStateForId(finalState);
            auto.decompLengths = new HashMap<>();
            Set<String> possibleDecompositions = getPossibleDecompositionsTopDown(finalRep, new HashSet<>());
            for (String decomp : possibleDecompositions) {
                System.out.println(decomp);
            }
        }
    }

    public void printShortestDecompositionsTopDown() {
        for (int finalState : auto.getFinalStates()) {
            BoundaryRepresentation finalRep = auto.getStateForId(finalState);
            if (!auto.rulesTopDown.containsKey(finalRep)) {
                System.out.println("no parse for " + finalRep.toString());
            } else {
                auto.decompLengths = new HashMap<>();
                Set<String> possibleDecompositions = getPossibleDecompositionsTopDown(finalRep, new HashSet<>());
                if (!possibleDecompositions.isEmpty()) {
                    int shortest = auto.decompLengths.get(possibleDecompositions.iterator().next());
                    for (String decomp : possibleDecompositions) {
                        shortest = Math.min(shortest, auto.decompLengths.get(decomp));
                    }
                    for (String decomp : possibleDecompositions) {
                        if (auto.decompLengths.get(decomp) == shortest) {
                            System.out.println(decomp);
                        }
                    }
                }
            }
        }
    }

    private Set<String> getPossibleDecompositionsTopDown(BoundaryRepresentation rep, Set<BoundaryRepresentation> alreadySeen) {
        Set<Rule> applicableRules = auto.rulesTopDown.get(rep);
        Set<String> res = new HashSet<>();
        for (Rule rule : applicableRules) {
            if (rule.getArity() == 0) {
                String resString = constantAbbreviations.get(rule.getLabel(auto));//rule.getLabel(auto);
                res.add(resString);
                auto.decompLengths.put(resString, 1);
            } else if (rule.getArity() == 1) {
                int childId = rule.getChildren()[0];
                BoundaryRepresentation child = auto.getStateForId(childId);
                if (!alreadySeen.contains(child)) {
                    Set<BoundaryRepresentation> newSeen = new HashSet<>();
                    newSeen.addAll(alreadySeen);
                    newSeen.add(rep);
                    for (String childDecomp : getPossibleDecompositionsTopDown(child, newSeen)) {
                        String resString = rule.getLabel(auto) + "(" + childDecomp + ")";
                        res.add(resString);
                        auto.decompLengths.put(resString, auto.decompLengths.get(childDecomp) + 1);
                    }
                }
            } else if (rule.getArity() == 2) {
                int childLeftId = rule.getChildren()[0];
                int childRightId = rule.getChildren()[1];
                BoundaryRepresentation childLeft = auto.getStateForId(childLeftId);
                BoundaryRepresentation childRight = auto.getStateForId(childRightId);
                if (!(alreadySeen.contains(childLeft) || alreadySeen.contains(childRight))) {
                    Set<BoundaryRepresentation> newSeen = new HashSet<>();
                    newSeen.addAll(alreadySeen);
                    newSeen.add(rep);
                    for (String childLeftDecomp : getPossibleDecompositionsTopDown(childLeft, newSeen)) {
                        for (String childRightDecomp : getPossibleDecompositionsTopDown(childRight, newSeen)) {
                            //String resString = "(" + childLeftDecomp + "||" + childRightDecomp + ")";
                            String resString = " " + childLeftDecomp + "||" + childRightDecomp + " ";
                            res.add(resString);
                            auto.decompLengths.put(resString, auto.decompLengths.get(childLeftDecomp) + auto.decompLengths.get(childRightDecomp) + 1);
                        }
                    }
                }
            }
        }
        return res;
    }

    public void printAllRulesTopDown() {
        int counter = 0;
        for (BoundaryRepresentation rep : auto.rulesTopDown.keySet()) {
            for (Rule rule : auto.rulesTopDown.get(rep)) {
                counter++;
                String children = "";
                if (rule.getArity() == 0) {
                    children = "";
                } else if (rule.getArity() == 1) {
                    children = auto.getStateForId(rule.getChildren()[0]).toString();
                } else if (rule.getArity() == 2) {
                    children = auto.getStateForId(rule.getChildren()[0]).toString() + " , " + auto.getStateForId(rule.getChildren()[1]).toString();
                }
                System.out.println(auto.getStateForId(rule.getParent()).toString() + " -> " + rule.getLabel(auto) + "(" + children + ")");
            }
        }
        System.out.println("That is a total of " + String.valueOf(counter) + " Rules.");
    }

    
    
    private class InitTuple{
        public IntList agenda = new IntArrayList();
        public IntSet seen = new IntOpenHashSet();
        public Set<String> constants = new HashSet<>();
        public Set<String> unisymbols = new HashSet<>();
        public Set<String> bisymbols = new HashSet<>();
    }
    
    private class InitTuplePriority{
        public IntPriorityQueue agenda = new IntHeapPriorityQueue();
        public IntSet seen = new IntOpenHashSet();
        public Set<String> constants = new HashSet<>();
        public Set<String> unisymbols = new HashSet<>();
        public Set<String> bisymbols = new HashSet<>();
        public InitTuplePriority(SGraphBRDecompositionAutomaton auto){
            agenda = new IntHeapPriorityQueue(new BRComparator(auto));
        }
    }
}
