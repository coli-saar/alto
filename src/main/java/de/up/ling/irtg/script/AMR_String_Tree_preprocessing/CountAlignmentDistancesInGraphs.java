/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.AMR_String_Tree_preprocessing;

import de.saar.basic.Pair;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.GraphInfo;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import static de.up.ling.irtg.script.AMR_String_Tree_preprocessing.DecompsForAlignedGraphStringGrammarInduction.parseAlignments;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Counts how far apart nodes, that are aligned to neighboring
 * words in a sentence, are in a graph. Runs on a corpus.
 * @author groschwitz
 */
public class CountAlignmentDistancesInGraphs {
    
    public static void main(String[] args) throws FileNotFoundException, IOException, CorpusReadingException {
        //setup input
        if (args.length<3) {
            System.out.println("Need 3 arguments (not all found), these are: corpusPath alignmentFilePath nodeNameListFilePath spanWidth lookRange");
            String defaultArgs = "examples/AMRAllCorpusExplicit.txt examples/amrHere/AMRExplicit.align examples/amrHere/AMRExplicit.names 1 1";
            System.out.println("using default arguments instead: "+defaultArgs);
            args = defaultArgs.split(" ");
        }
        String corpusPath = args[0];
        BufferedReader alignmentReader = new BufferedReader(new FileReader(args[1]));
        BufferedReader nodeNameListReader = new BufferedReader(new FileReader(args[2]));
        int spanWidth = Integer.parseInt(args[3]);
        int lookRange = Integer.parseInt(args[4]);
        
        
        //setup corpus form input
        Reader corpusReader = new FileReader(corpusPath);
        InterpretedTreeAutomaton irtg4Corpus = new InterpretedTreeAutomaton(null);
        irtg4Corpus.addInterpretation("graph", new Interpretation(new GraphAlgebra(), null));
        irtg4Corpus.addInterpretation("string", new Interpretation(new StringAlgebra(), null));
        Corpus corpus = Corpus.readCorpus(corpusReader, irtg4Corpus);
        
        //int max = 3;
        
        int j = 0;
        for (Instance instance : corpus) {
            System.err.println("-----------------instance "+j+"----------------------");
            SGraph graph = (SGraph)instance.getInputObjects().get("graph");
            
            StringAlgebra stringAlg = new StringAlgebra();
            
            //alignments
            String alignmentString = alignmentReader.readLine();
            String nodeNameString = nodeNameListReader.readLine();
            Pair<Map<String, Set<Integer>>, Int2IntMap> node2AlignAndAlign2StrPos = parseAlignments(alignmentString, nodeNameString);
            Int2IntMap align2StrPos = node2AlignAndAlign2StrPos.right;
            Object2IntMap<String> nodeName2Alignment = new Object2IntOpenHashMap<>();
            Int2ObjectMap<String> alignment2NodeName = new Int2ObjectOpenHashMap<>();
            List<Integer> orderedAlignments = new ArrayList<>();
            boolean alignmentsWorkOut = true;
            for (Map.Entry<String, Set<Integer>> entry : node2AlignAndAlign2StrPos.left.entrySet()) {
                if (entry.getValue().size() > 1) {
                    alignmentsWorkOut = false;
                } else if (entry.getValue().size() == 1) {
                    orderedAlignments.add(entry.getValue().iterator().next());
                    nodeName2Alignment.put(entry.getKey(), entry.getValue().iterator().next());
                    alignment2NodeName.put(entry.getValue().iterator().next(), entry.getKey());
                }
            }
            if (!alignmentsWorkOut) {
                System.err.println("graph "+j+" ("+graph.getAllNodeNames().size()+" nodes): incompatible alignments");
                j++;
                continue;
            }
            Collections.sort(orderedAlignments, (s1, s2) -> align2StrPos.get(s1)-align2StrPos.get(s2));
            List<String> stringInput = (List<String>)stringAlg.parseString(orderedAlignments.stream()
                .map(al -> String.valueOf(al)).collect(Collectors.joining(" ")));
            
            
            GraphInfo graphInfo = new GraphInfo(graph, new GraphAlgebra());
            
            for (int i = 0; i < stringInput.size()-spanWidth; i++) {
                int min = Integer.MAX_VALUE;
                for (int k = 0; k<spanWidth; k++) {
                    String baseNodeName = alignment2NodeName.get(Integer.parseInt(stringInput.get(i+k)));
                    for (int minus = 1; minus <= lookRange; minus++) {
                        if (i-minus > 0) {
                            String otherNodeName = alignment2NodeName.get(Integer.parseInt(stringInput.get(i-minus)));
                            try {
                                min = Math.min(min, graphInfo.dist(graphInfo.getIntForNode(baseNodeName), graphInfo.getIntForNode(otherNodeName)));
                            } catch (java.lang.NullPointerException ex) {
                                System.err.println("base node name" + ((baseNodeName == null)? "null" : baseNodeName));
                                System.err.println("other node name" + ((otherNodeName == null)? "null" : otherNodeName));
                            }
                        }
                    }
                    for (int plus = 1; plus <= lookRange; plus++) {
                        if (i+spanWidth-1+plus < stringInput.size()) {
                            String otherNodeName = alignment2NodeName.get(Integer.parseInt(stringInput.get(i+spanWidth-1+plus)));
                            try {
                                min = Math.min(min, graphInfo.dist(graphInfo.getIntForNode(baseNodeName), graphInfo.getIntForNode(otherNodeName)));
                            } catch (java.lang.NullPointerException ex) {
                                System.err.println("base node name" + ((baseNodeName == null)? "null" : baseNodeName));
                                System.err.println("other node name" + ((otherNodeName == null)? "null" : otherNodeName));
                            }
                        }
                    }
                }
                System.err.println(min);
            }
            
            
            
            
            j++;
            /*if (j>= max) {
                break;//for the moment, limit the amount of instances
            }*/
        }
    }
}
