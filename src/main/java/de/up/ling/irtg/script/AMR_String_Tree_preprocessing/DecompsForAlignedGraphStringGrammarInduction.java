/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.AMR_String_Tree_preprocessing;

import de.saar.basic.Pair;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.graph.BoundaryRepresentation;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.GraphGrammarInductionAlgebra;
import de.up.ling.irtg.algebra.graph.GraphInfo;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.BinaryPartnerFinder;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 *
 * @author groschwitz
 */
public class DecompsForAlignedGraphStringGrammarInduction {
    
    private final static String combineLabel = "comb";
    private final static String reverseCombineLabel = "rcomb";
    private final static String unalignedLabelPrefix = "unaligned";
    private final static String reverseUnalignedLabelPrefix = "rUnaligned";
    
    public static void main(String[] args) throws FileNotFoundException, IOException, CorpusReadingException, ParserException {
        
        //System.err.println("reading input...");
        
        //setup input
        if (args.length<6) {
            System.out.println("Need 8 arguments (not all found), these are: sourceCount corpusPath targetFolderPath alignmentTargetFolderPath alignmentFilePath nodeNameListFilePath maxAlignmentDistInGraph");
            String defaultArgs = "3 examples/AMRAllCorpusExplicit.txt output/test/ examples/amrHere/AMRExplicit.align examples/amrHere/AMRExplicit.names 3";
            System.out.println("using default arguments instead: "+defaultArgs);
            args = defaultArgs.split(" ");
        }
        int maxSources = Integer.valueOf(args[0]);
        String corpusPath = args[1];
        String targetFolderPath = args[2];
        File alignmentsTarget = new File(targetFolderPath+"graphAlign");
        File allowedCutsTarget = new File(targetFolderPath+"graphCuts");
        File decompsTarget = new File(targetFolderPath+"graphDecomps");
        File ntTarget = new File(targetFolderPath+"graphNTMapping");
        alignmentsTarget.mkdirs();
        allowedCutsTarget.mkdirs();
        decompsTarget.mkdirs();
        ntTarget.mkdirs();
        BufferedReader alignmentReader = new BufferedReader(new FileReader(args[3]));
        BufferedReader nodeNameListReader = new BufferedReader(new FileReader(args[4]));
        int maxAlignmentDistInGraph = Integer.valueOf(args[5]);
        FileWriter filteredAlignmentWriter = new FileWriter(targetFolderPath+"filtered.align");
        
        //setup corpus form input
        Reader corpusReader = new FileReader(corpusPath);
        InterpretedTreeAutomaton irtg4Corpus = new InterpretedTreeAutomaton(null);
        irtg4Corpus.addInterpretation("graph", new Interpretation(new GraphAlgebra(), null));
        irtg4Corpus.addInterpretation("string", new Interpretation(new StringAlgebra(), null));
        irtg4Corpus.addInterpretation("tree", new Interpretation(new MinimalTreeAlgebra(), null));
        Corpus corpus = Corpus.readCorpusLenient(corpusReader, irtg4Corpus);
        
        //int max = 3;
        
        
        int j = 0;
        for (Instance instance : corpus) {
            
            
                //System.err.println("starting instance "+j+"...");
                SGraph graph = (SGraph)instance.getInputObjects().get("graph");

                StringAlgebra stringAlg = new StringAlgebra();

                //alignments
                String alignmentString = alignmentReader.readLine();
                List<String> alignmentsLeft = new ArrayList<>();
                alignmentsLeft.addAll(Arrays.asList(alignmentString.split(" +")));
                int initialAlignmentCount = alignmentsLeft.size();
                String nodeNameString = nodeNameListReader.readLine();
                Pair<Map<String, Set<Integer>>, Int2IntMap> node2AlignAndAlign2StrPos = parseAlignments(alignmentString, nodeNameString);
                Object2IntMap<String> nodeName2Alignment = new Object2IntOpenHashMap<>();
                Int2ObjectMap<String> alignment2NodeName = new Int2ObjectOpenHashMap<>();
                List<Integer> orderedAlignments = new ArrayList<>();
                boolean alignmentsWorkOut = doAlignments(node2AlignAndAlign2StrPos, nodeName2Alignment, alignment2NodeName, orderedAlignments);
                List<String> stringInput = (List<String>)stringAlg.parseString(orderedAlignments.stream()
                    .map(al -> String.valueOf(al)).collect(Collectors.joining(" ")));
                /*for (String nodeName : graph.getAllNodeNames()) {
                    if (!nodeName2Alignment.containsKey(nodeName)) {
                        alignmentsWorkOut = false;
                    }
                }*/
                if (!alignmentsWorkOut) {
                    System.err.println("graph "+j+" ("+graph.getAllNodeNames().size()+" nodes): incompatible alignments");
                    j++;
                    continue;
                }




                int alignmentToRemove = alignmentToRemove(stringInput, graph, alignment2NodeName, maxSources);
                StringJoiner sj = null;
                if (maxAlignmentDistInGraph > 0) {
                    while (alignmentToRemove >= 0) {
                        //if alignmentToRemove is -1, then the dist was less than maxAlignmentDistInGraph

                        //System.out.println(alignmentToRemove);
                        alignmentsLeft.remove(alignmentToRemove);
                        sj = new StringJoiner(" ");
                        for (String alignmentLeft : alignmentsLeft) {
                            sj.add(alignmentLeft);
                        }
                        doAlignments(parseAlignments(sj.toString(), nodeNameString), nodeName2Alignment, alignment2NodeName, orderedAlignments);
                        stringInput = (List<String>)stringAlg.parseString(orderedAlignments.stream()
                            .map(al -> String.valueOf(al)).collect(Collectors.joining(" ")));
                        alignmentToRemove = alignmentToRemove(stringInput, graph, alignment2NodeName, maxAlignmentDistInGraph);
                    }
                }
                filteredAlignmentWriter.write(((sj == null)? alignmentString : sj.toString())+"\n");






                Signature irtgSignature = new Signature();
                irtgSignature.addSymbol(combineLabel, 2);
                irtgSignature.addSymbol(reverseCombineLabel, 2);

                Signature graphSignature = new Signature();
                graphSignature.addSymbol(GraphGrammarInductionAlgebra.OP_COMBINE, 2);
                graphSignature.addSymbol(GraphGrammarInductionAlgebra.OP_EXPLICIT, 1);


                //graph signature, wrapper signature, and maps
                Map<String, String> constLabel2StringConstLabel = new HashMap<>();
                Map<String, String> constLabel2GraphConstLabel = new HashMap<>();
                Map<String, String> nodename2GraphConstLabel = new HashMap<>();
                int i = 0;
                for (String nodeName : graph.getAllNodeNames()) {
                    String constLabel = "const"+i;
                    String graphLabel = "G"+i;
                    graphSignature.addSymbol(graphLabel, 0);
                    nodename2GraphConstLabel.put(nodeName, graphLabel);
                    if (nodeName2Alignment.containsKey(nodeName)) {
                        irtgSignature.addSymbol(constLabel, 0);
                        constLabel2GraphConstLabel.put(constLabel, graphLabel);
                        int alignment = nodeName2Alignment.get(nodeName);
                        constLabel2StringConstLabel.put(constLabel, String.valueOf(alignment));
                    } else {
                        irtgSignature.addSymbol(unalignedLabelPrefix+constLabel, 1);
                        irtgSignature.addSymbol(reverseUnalignedLabelPrefix+constLabel, 1);
                        constLabel2GraphConstLabel.put(unalignedLabelPrefix+constLabel, graphLabel);
                        constLabel2GraphConstLabel.put(reverseUnalignedLabelPrefix+constLabel, graphLabel);
                    }
                    i++;
                }

                //homs --EDIT will do synch parsing with custom automaton, so don't need homs

                //string signature? --EDIT just need to parse the string, doing this by also getting the input string out of it

                //irtg rules --EDIT will do synch parsing with custom automaton, so don't need rules in irtg

                GraphGrammarInductionAlgebra graphInductionAlg = new GraphGrammarInductionAlgebra(graph, maxSources, nodename2GraphConstLabel, graphSignature);
                TreeAutomaton<GraphGrammarInductionAlgebra.BrAndEdges> graphAuto = graphInductionAlg.getAutomaton();

                //System.err.println(stringInput);
                TreeAutomaton<StringAlgebra.Span> stringAuto = stringAlg.decompose(stringInput);

                //System.err.println("automaton setup...");
                //synchr parsing
                
                String fileName = j+"_"+graph.getAllNodeNames().size()+".rtg";
                
                FileWriter alignmentWriter = new FileWriter(alignmentsTarget.getAbsolutePath()+"/"+fileName);
                FileWriter cutsWriter = new FileWriter(allowedCutsTarget.getAbsolutePath()+"/"+fileName);
                CustomSynchParsingAutomaton synchAuto = new CustomSynchParsingAutomaton(irtgSignature, stringAuto, graphAuto, constLabel2StringConstLabel, constLabel2GraphConstLabel, alignmentWriter);
                //System.err.println("parsing...");
                TreeAutomaton<Pair<StringAlgebra.Span, GraphGrammarInductionAlgebra.BrAndEdges>> concAuto = synchAuto.asConcreteTreeAutomatonBottomUp();
                
                
                
                synchAuto.writeAllowedCuts(cutsWriter);
                alignmentWriter.close();
                cutsWriter.close();
                //System.err.println("collecting rules...");
                Set<Rule> rhsRules = new HashSet<>();
                concAuto.processAllRulesTopDown(rule -> {
                    for (Rule helperRule : synchAuto.rule2RhsRules.get(rule)) {
                        for (Rule rhsRule : graphInductionAlg.getDecompRulesForHelperRule(helperRule)) {
                            rhsRules.add(rhsRule);
                        }
                    }
                });

                int alignmentsRemoved = initialAlignmentCount-alignmentsLeft.size();
                System.err.println("graph "+j+" ("+graph.getAllNodeNames().size()+" nodes): "+rhsRules.size()+" rules ------ "+alignmentsRemoved+"/"+initialAlignmentCount+" alignments removed");

                
                if (!rhsRules.isEmpty()) {
                    Map<String, String> allSeenBRsToNT = new HashMap<>();
                    FileWriter writer = new FileWriter(decompsTarget.getAbsolutePath()+"/"+fileName);
                    FileWriter ntWriter = new FileWriter(ntTarget.getAbsolutePath()+"/"+fileName);
                    for (Rule rhsRule : rhsRules) {
                        BoundaryRepresentation parentBR = graphInductionAlg.getDecompAutomaton().getStateForId(rhsRule.getParent());
                        allSeenBRsToNT.put(parentBR.toString(), parentBR.allSourcesToString());
                        writer.write(rhsRule.toString(graphInductionAlg.getDecompAutomaton(), graphInductionAlg.getDecompAutomaton().getStateForId(rhsRule.getParent()).isCompleteGraph())+"\n");
                    }
                    writer.close();
                    
                    for (Entry<String, String> entry : allSeenBRsToNT.entrySet()) {
                        ntWriter.write(entry.getKey()+" ||| " + entry.getValue()+"\n");
                    }
                    ntWriter.close();
                }
            j++;
            /*if (j>= max) {
                break;//for the moment, limit the amount of instances
            }*/
        }
        filteredAlignmentWriter.close();
    }
    
    private static int alignmentToRemove(List<String> stringInput, SGraph graph, Int2ObjectMap<String> alignment2NodeName, int maxAllowed) {
        int spanWidth = 1;
        int lookRange = 1;
        GraphInfo graphInfo = new GraphInfo(graph, new GraphAlgebra());
        
        int ret = -1;
        int maxFound = -1;
        
        if (stringInput.size() <= 2) {
            //then we don't want to return anything else
            
            return -1;
        }
        
        for (int i = 0; i <= stringInput.size()-spanWidth; i++) {
            int min = Integer.MAX_VALUE;
            for (int k = 0; k<spanWidth; k++) {
                String baseNodeName = alignment2NodeName.get(Integer.parseInt(stringInput.get(i+k)));
                for (int minus = 1; minus <= lookRange; minus++) {
                    if (i-minus > 0) {
                        String otherNodeName = alignment2NodeName.get(Integer.parseInt(stringInput.get(i-minus)));
                        try {
                            min = Math.min(min, graphInfo.dist(graphInfo.getIntForNode(baseNodeName), graphInfo.getIntForNode(otherNodeName)));
                        } catch (java.lang.NullPointerException ex) {
                            //System.err.println("base node name " + ((baseNodeName == null)? "null" : baseNodeName));
                            //System.err.println("other node name " + ((otherNodeName == null)? "null" : otherNodeName));
                        }
                    }
                }
                for (int plus = 1; plus <= lookRange; plus++) {
                    if (i+spanWidth-1+plus < stringInput.size()) {
                        String otherNodeName = alignment2NodeName.get(Integer.parseInt(stringInput.get(i+spanWidth-1+plus)));
                        try {
                            min = Math.min(min, graphInfo.dist(graphInfo.getIntForNode(baseNodeName), graphInfo.getIntForNode(otherNodeName)));
                        } catch (java.lang.NullPointerException ex) {
                            //System.err.println("base node name " + ((baseNodeName == null)? "null" : baseNodeName));
                            //System.err.println("other node name " + ((otherNodeName == null)? "null" : otherNodeName));
                        }
                    }
                }
            }
            if (min > maxFound) {
                ret = Integer.parseInt(stringInput.get(i));
                maxFound = min;
            }
            //System.err.println(min);
        }
        if (maxFound > maxAllowed) {
            return ret;
        } else {
            return -1;
        }
    }
    
    private static boolean doAlignments(Pair<Map<String, Set<Integer>>, Int2IntMap> node2AlignAndAlign2StrPos,
            Object2IntMap<String> nodeName2Alignment,
            Int2ObjectMap<String> alignment2NodeName,
            List<Integer> orderedAlignments) {
        
        
        Int2IntMap align2StrPos = node2AlignAndAlign2StrPos.right;
        
        boolean ret = true;
        
        nodeName2Alignment.clear();
        alignment2NodeName.clear();
        orderedAlignments.clear();
        
        for (Entry<String, Set<Integer>> entry : node2AlignAndAlign2StrPos.left.entrySet()) {
            if (entry.getValue().size() > 1) {
                ret = false;
            } else if (entry.getValue().size() == 1) {
                orderedAlignments.add(entry.getValue().iterator().next());
                nodeName2Alignment.put(entry.getKey(), entry.getValue().iterator().next());
                alignment2NodeName.put(entry.getValue().iterator().next(), entry.getKey());
            }
        }
        Collections.sort(orderedAlignments, (s1, s2) -> align2StrPos.get(s1)-align2StrPos.get(s2));
        return ret;
    }
    
    private static class CustomSynchParsingAutomaton extends TreeAutomaton<Pair<StringAlgebra.Span, GraphGrammarInductionAlgebra.BrAndEdges>> {

        private final Int2IntMap state2LhsState = new Int2IntOpenHashMap();
        private final Int2IntMap state2RhsState = new Int2IntOpenHashMap();
        private final Int2IntMap lhsState2State = new Int2IntOpenHashMap();
        private final int concatID;
        private final int combineID;
        private final int explicitID;
        private final TreeAutomaton<StringAlgebra.Span> lhs;
        private final TreeAutomaton<GraphGrammarInductionAlgebra.BrAndEdges> rhs;
        private final Map<Rule, List<Rule>> rule2RhsRules;
        private final Map<String, String> constLabel2StringConstLabel;
        private final Map<String, String> constLabel2GraphConstLabel;
        private final Writer alignmentWriter;
        private final Set<String> allowedCuts;
        
        public CustomSynchParsingAutomaton(Signature signature, TreeAutomaton<StringAlgebra.Span> lhs,
                TreeAutomaton<GraphGrammarInductionAlgebra.BrAndEdges> rhs, 
                Map<String, String> constLabel2StringConstLabel,
                Map<String, String> constLabel2GraphConstLabel,
                Writer alignmentWriter) {
            super(signature);
            this.lhs = lhs;
            this.rhs = rhs;
            rule2RhsRules = new HashMap<>();
            concatID = lhs.getSignature().getIdForSymbol(StringAlgebra.CONCAT);
            combineID = rhs.getSignature().getIdForSymbol(GraphGrammarInductionAlgebra.OP_COMBINE);
            explicitID = rhs.getSignature().getIdForSymbol(GraphGrammarInductionAlgebra.OP_EXPLICIT);
            this.constLabel2StringConstLabel = constLabel2StringConstLabel;
            this.constLabel2GraphConstLabel = constLabel2GraphConstLabel;
            this.alignmentWriter = alignmentWriter;
            allowedCuts = new HashSet<>();
        }

        @Override
        public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
            String label = signature.resolveSymbolId(labelId);
            
            List<Rule> ret = new ArrayList<>();
            List<Rule> allRhsRules = new ArrayList<>();
            
            if (label.equals(combineLabel) || label.equals(reverseCombineLabel)) {
                
                
                //lhs
                int[] lhsChildren = new int[2];
                lhsChildren[0] = state2LhsState.get(childStates[0]);
                lhsChildren[1] = state2LhsState.get(childStates[1]);
                Iterable<Rule> lhsRules = lhs.getRulesBottomUp(concatID, lhsChildren);
                
                //rhs
                int[] rhsChildren = new int[2];
                if (label.equals(combineLabel)) {
                    rhsChildren[0] = state2RhsState.get(childStates[0]);
                    rhsChildren[1] = state2RhsState.get(childStates[1]);
                } else {
                    rhsChildren[1] = state2RhsState.get(childStates[0]);
                    rhsChildren[0] = state2RhsState.get(childStates[1]);
                }
                //the next iteration is over max one rule since lhs is deterministic for concat, so no loss of efficiency here
                //basically a shortcut for checking if this is empty
                for (Rule lhsRule : lhsRules) {
                    Iterable<Rule> explicitRules = rhs.getRulesBottomUp(explicitID, new int[]{rhsChildren[1]});
                    //the next iteration is over max one rule since rhs is deterministic for explicit, so no loss of efficiency here
                    //basically a shortcut for checking if this is empty
                    for (Rule explicitRule : explicitRules) {
                        rhsChildren[1] = explicitRule.getParent();
                        allRhsRules.add(explicitRule);
                        Iterable<Rule> combineRules = rhs.getRulesBottomUp(combineID, rhsChildren);
                        //the next iteration is over max one rule since rhs is deterministic for combine, so no loss of efficiency here
                        //basically a shortcut for checking if this is empty
                        for (Rule combineRule : combineRules) {
                            allRhsRules.add(combineRule);
                            ret.add(createRule(addState(new Pair(lhs.getStateForId(lhsRule.getParent()), rhs.getStateForId(combineRule.getParent()))),
                                    labelId, childStates, 1.0));
                        }
                    }
                }
            } else if (label.startsWith(unalignedLabelPrefix) || label.startsWith(reverseUnalignedLabelPrefix)) {
                
                //get constant for unaligned node
                int rhsChild = state2RhsState.get(childStates[0]);
                Rule rhsConstRule = rhs.getRulesBottomUp(rhs.getSignature().getIdForSymbol(constLabel2GraphConstLabel.get(label)),
                        new int[0]).iterator().next();
                allRhsRules.add(rhsConstRule);
                
                //setup child array
                int[] rhsChildren = new int[2];
                if (label.startsWith(unalignedLabelPrefix)) {
                    rhsChildren[0] = rhsChild;
                    rhsChildren[1] = rhsConstRule.getParent();
                } else {
                    rhsChildren[1] = rhsChild;
                    rhsChildren[0] = rhsConstRule.getParent();
                }
                
                //make right side explicit
                Iterable<Rule> explicitRules = rhs.getRulesBottomUp(explicitID, new int[]{rhsChildren[1]});
                //the next iteration is over max one rule since rhs is deterministic for explicit, so no loss of efficiency here
                //basically a shortcut for checking if this is empty
                for (Rule explicitRule : explicitRules) {
                    rhsChildren[1] = explicitRule.getParent();
                    allRhsRules.add(explicitRule);
                    
                    //combine
                    Iterable<Rule> combineRules = rhs.getRulesBottomUp(combineID, rhsChildren);
                    //the next iteration is over max one rule since rhs is deterministic for combine, so no loss of efficiency here
                    //basically a shortcut for checking if this is empty
                    for (Rule combineRule : combineRules) {
                        allRhsRules.add(combineRule);
                        ret.add(createRule(addState(new Pair(lhs.getStateForId(state2LhsState.get(childStates[0])), rhs.getStateForId(combineRule.getParent()))),
                                labelId, childStates, 1.0));
                    }
                }
                
                
            } else {
                //then we have a constant
                
                StringAlgebra.Span lhsState = lhs.getStateForId(lhs.getRulesBottomUp(lhs.getSignature().getIdForSymbol(constLabel2StringConstLabel.get(label)),
                        new int[0]).iterator().next().getParent());
                Rule rhsRule = rhs.getRulesBottomUp(rhs.getSignature().getIdForSymbol(constLabel2GraphConstLabel.get(label)),
                        new int[0]).iterator().next();
                allRhsRules.add(rhsRule);
                GraphGrammarInductionAlgebra.BrAndEdges rhsState = rhs.getStateForId(rhsRule.getParent());
                
                ret.add(createRule(addState(new Pair(lhsState, rhsState)), labelId, childStates, 1.0));
                
                
                try {
                    //write entry into alignment file
                    alignmentWriter.write("'"+rhs.getStateForId(rhsRule.getParent()).getBr().toString()+"' ||| "+String.valueOf(Integer.parseInt(constLabel2StringConstLabel.get(label))+1)+"\n");
                } catch (IOException ex) {
                    System.err.println("error writing alignment: "+ex);
                }
                
            }
            
            //writing allowed cuts
            for (Rule finalRhsRule : allRhsRules) {
                GraphGrammarInductionAlgebra.BrAndEdges brAndEdges = rhs.getStateForId(finalRhsRule.getParent());
                if (brAndEdges.hasNoUnassignedEdges()) {
                    allowedCuts.add(brAndEdges.getBr().toString());
                }
            }
            
            if (ret.size() == 1) {
                //System.err.println(ret.get(0).toString(this));
                rule2RhsRules.put(ret.get(0), allRhsRules);
            } else if (ret.size() > 1) {
                System.err.println("Unexpectedly found multiple rules bottom up!");
            }
            return ret;
            
        }
        
        public void writeAllowedCuts(Writer cutsWriter) {
            try {
                for (String allowedCut : allowedCuts) {
                    cutsWriter.write(allowedCut+"\n");
                }
            } catch (IOException ex) {
                System.err.println("error writing allowed cuts: "+ex.toString());
            }
        }

        @Override
        protected int addState(Pair<StringAlgebra.Span, GraphGrammarInductionAlgebra.BrAndEdges> state) {
            int ret = super.addState(state);
            int leftID = lhs.getIdForState(state.left);
            int rightID = rhs.getIdForState(state.right);
            lhsState2State.put(leftID, ret);
            state2LhsState.put(ret, leftID);
            state2RhsState.put(ret, rightID);
            if (lhs.getFinalStates().contains(leftID) && rhs.getFinalStates().contains(rightID)) {
                //System.err.println("final state found: "+state.toString());
                addFinalState(ret);
            }
            return ret;
        }

        
        @Override
        public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean supportsBottomUpQueries() {
            return true;
        }

        @Override
        public boolean supportsTopDownQueries() {
            return false;
        }

        @Override
        public BinaryPartnerFinder makeNewBinaryPartnerFinder() {
            return new BinaryPartnerFinder() {

                private BinaryPartnerFinder lhsBPF = lhs.makeNewBinaryPartnerFinder();
                
                @Override
                public IntCollection getPartners(int labelID, int stateID) {
                    return lhsBPF.getPartners(lhs.getSignature().getIdForSymbol(StringAlgebra.CONCAT), state2LhsState.get(stateID))
                            .stream().map(lhsID -> lhsState2State.get(lhsID)).collect(Collector.of(
                                () -> new IntArrayList(),
                                (IntList list, Integer val) -> list.add(val),
                                (IntList list, IntList list2) -> {
                                    IntList ret = new IntArrayList();
                                    ret.addAll(list);
                                    ret.addAll(list2);
                                    return ret;
                                }));
                }

                @Override
                public void addState(int stateID) {
                    lhsBPF.addState(state2LhsState.get(stateID));
                }
            };
        }
        
        

        @Override
        public boolean isBottomUpDeterministic() {
            return true; //true for the current incarnation, could change in future
        }
        
    }
    
    public static Pair<Map<String, Set<Integer>>, Int2IntMap> parseAlignments(String alignmentString, String nodeNameString) {
        //setup
        Map<String, Set<Integer>> nodeName2Alignments = new HashMap<>();
        Int2IntMap alignment2StringPos = new Int2IntOpenHashMap();
        
        
        if (alignmentString.length() < 3) {
            //then it cannot describe an alignment
            return new Pair(nodeName2Alignments, alignment2StringPos);
        }
        
        String[] nodeNames = nodeNameString.split(" +");
        String[] alignments = alignmentString.split(" +");
        
        //check for duplicate names, and add empty Sets
        for (int i = 0; i < nodeNames.length; i++) {
            for (int j = i+1; j<nodeNames.length; j++) {
                if (nodeNames[i].equals(nodeNames[j])) {
                    System.err.println("WARNING: Node name '"+nodeNames[i]+"' appears twice in '"+nodeNameString+"'!");
                }
            }
            nodeName2Alignments.put(nodeNames[i], new HashSet<>());
        }
        
        
        //parse actual alignments
        for (int i = 0; i < alignments.length; i++) {
            String[] stringAndGraph = alignments[i].split("-");
            String nodeName = nodeNames[Integer.parseInt(stringAndGraph[1])];
            nodeName2Alignments.get(nodeName).add(i);
            alignment2StringPos.put(i, Integer.parseInt(stringAndGraph[0]));
        }
        
        return new Pair(nodeName2Alignments, alignment2StringPos);
    }
    
}
