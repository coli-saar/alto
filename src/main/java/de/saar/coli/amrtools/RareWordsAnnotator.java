/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtools;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.up.ling.tree.Tree;
import de.saar.coli.amrtagging.Alignment;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.TreeWithAritiesAlgebra;
import de.up.ling.irtg.algebra.graph.BlobUtils;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.codec.IsiAmrInputCodec;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.CorpusWriter;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.Counter;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Takes as input an Alto corpus with a string, tree and AMR interpretation as
 * provided by  de.saar.coli.amrtools.datascript.FullProcess, as well as alignments as
 * created by de.saar.coli.amrtools.aligner.Aligner and annotates it
 * with rare word annotations.
 * @author Jonas
 */
public class RareWordsAnnotator {
    
    public final static String NAME_TOKEN = "_NAME_";
    public final static String DATE_TOKEN = "_DATE_";
    public final static String NUMBER_TOKEN = "_NUMBER_";
    public final static String UNK_TOKEN = "_UNK_";
    public final static String SEP = "_";
    public final static String NUMBER_REGEX = "[0-9,.]*[0-9][0-9,.]*";//at least one number, and arbitrary number and commas / dots around it
    
    @Parameter(names = {"--corpus", "-c"}, description = "Path to input corpus", required=true)
    private String corpusPath;
    
    @Parameter(names = {"--alignments", "-a"}, description = "Path to alignment file", required=true)
    private String alignmentPath;
    
    
    @Parameter(names = {"--palignments", "-pa"}, description = "Path to alignment file")
    private String palignmentPath = null;
    
    @Parameter(names = {"--outfile", "-o"}, description = "Path to output corpus ", required=true)
    private String outPath;
    
    
    @Parameter(names = {"--comment", "-cmt"}, description = "Comment to be printed in the file.")
    private String comment = "";
    
    
    @Parameter(names = {"--wordcountfile", "-w"}, description = "Optional path to a file that stores wordcounts")
    private String wordCountPath = null;
    
    @Parameter(names = {"--threshold", "-t"}, description = "Threshold for when a word is considered rare"
            , required = true)
    private int threshold;
    
    @Parameter(names = {"--help", "-?"}, description = "displays help if this is the only command", help = true)
    private boolean help = false;
    
    public static void main(String[] args) throws IOException, CorpusReadingException, MalformedURLException, InterruptedException {
        
        RareWordsAnnotator annotator = new RareWordsAnnotator();
        
        JCommander commander = new JCommander(annotator);
        commander.setProgramName("rareWords");
        
        try {
            commander.parse(args);
        } catch (com.beust.jcommander.ParameterException ex) {
            System.err.println("An error occured: " + ex.toString());
            System.err.println("\n Available options: ");
            commander.usage();
            return;
        }

        if (annotator.help) {
            commander.usage();
            return;
        }
        
        InterpretedTreeAutomaton loaderIRTG = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton<>());
        Signature dummySig = new Signature();
        loaderIRTG.addInterpretation("graph", new Interpretation(new GraphAlgebra(), new Homomorphism(dummySig, dummySig)));
        loaderIRTG.addInterpretation("string", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        loaderIRTG.addInterpretation("tree", new Interpretation(new TreeWithAritiesAlgebra(), new Homomorphism(dummySig, dummySig)));
        Corpus corpus = Corpus.readCorpus(new FileReader(annotator.corpusPath), loaderIRTG);
        
        BufferedReader alBr = new BufferedReader(new FileReader(annotator.alignmentPath));
        BufferedReader palBr = null;
        if (annotator.palignmentPath != null) {
            palBr = new BufferedReader(new FileReader(annotator.palignmentPath));
        }
        
        Corpus outC = new Corpus();
        
        //count words
        Counter<String> wordCounts = new Counter<>();
        for (Instance inst : corpus) {
            List<String> sent = (List)inst.getInputObjects().get("string");
            for (String word : sent) {
                wordCounts.add(word.toLowerCase());
            }
        }
        
        //collect data for later label replacement (for the names)
        Map<String, Counter<String>> lit2name = new HashMap<>();
        Map<String, Counter<String>> lit2wiki = new HashMap<>();
        int noWiki = 0;
        
        if (annotator.wordCountPath != null) {
            try (FileWriter wcw = new FileWriter(annotator.wordCountPath)) {
                wordCounts.writeAllSorted(wcw);
            }
        }
        
        StringAlgebra parsingAlg = new StringAlgebra();
        int i = 0;
        for (Instance inst : corpus) {
            
            if (i%500 == 0) {
                System.out.println("Doing instance "+i);
            }
            
            //new instance, copy old objects over
            Instance newI = new Instance();
            newI.setInputObjects(new HashMap<>());
            newI.getInputObjects().put("string", inst.getInputObjects().get("string"));
            newI.getInputObjects().put("tree", inst.getInputObjects().get("tree"));
            SGraph graph = (SGraph)inst.getInputObjects().get("graph");
            graph.setWriteAsAMR(true);
            newI.getInputObjects().put("graph", graph);
            
            List<String> origSent = (List)inst.getInputObjects().get("string");
            List<String> repSent = new ArrayList(origSent);
            Tree<String> origTree = (Tree)inst.getInputObjects().get("tree");
            Tree<String> repTree = (Tree)origTree.clone();
            
            String alString = alBr.readLine();
            newI.getInputObjects().put("alignment", parsingAlg.parseString(alString));
            
            Set<Alignment> als = new HashSet<>();
            if (!alString.equals("")) {
                for (String al : alString.split(" ")) {
                    Alignment alToAdd = Alignment.read(al);
                    if (alToAdd != null) {
                        als.add(alToAdd);
                    } else {
                        System.err.println("***WARNING*** Alignments broken!");
                        System.err.println(alString);
                    }
                }
            }
            
            Set<Alignment> pals = null;
            if (annotator.palignmentPath != null) {
                String palString = palBr.readLine();
                newI.getInputObjects().put("alignmentp", parsingAlg.parseString(palString));

                pals = new HashSet<>();
                if (!palString.equals("")) {
                    for (String al : palString.split(" ")) {
                        Alignment alToAdd = Alignment.read(al);
                        if (alToAdd != null) {
                            pals.add(alToAdd);
                        } else {
                            System.err.println("***WARNING*** p-Alignments broken!");
                            System.err.println(palString);
                        }
                    }
                }
            }
            
            //Create a copy of the graph, that will have some things replaced (rep for replace)
            Map<String, GraphNode> repNodes = new HashMap<>();
            Map<String, String> repS2n = new HashMap<>();
            Set<GraphEdge> repEdges = new HashSet<>();
            for (GraphNode v : graph.getGraph().vertexSet()) {
                repNodes.put(v.getName(), new GraphNode(v.getName(), v.getLabel()));
            }
            for (GraphEdge e : graph.getGraph().edgeSet()) {
                repEdges.add(new GraphEdge(repNodes.get(e.getSource().getName()), repNodes.get(e.getTarget().getName()), e.getLabel()));
            }
            for (String s : graph.getAllSources()) {
                repS2n.put(s, graph.getNodeForSource(s));
            }
            
            //collect name and date replacement spans, and replace the relevant node clusters in the graph
            Set<Alignment.Span> nameSpans = new HashSet<>();
            Set<Alignment.Span> dateSpans = new HashSet<>();
            for (Alignment al : als) {
                boolean replaced = false;
                if (!al.lexNodes.isEmpty() && al.nodes.size() > 1) {
                    GraphNode lexNode = graph.getNode(al.lexNodes.iterator().next());
                    if (lexNode == null) {
                        System.err.println(i);
                        System.err.println("***WARNING*** lexNode not found! Skipping alignment.");
                        System.err.println(al);
                        System.err.println(graph.toIsiAmrStringWithSources());
                        continue;
                    }
                    String label = lexNode.getLabel();
                    if (label.equals("name")) {
                        boolean repName = false;
                        //replace the name node and all opx nodes.
                        //assume at this point that all lexNodes are name nodes; but print a warning if there are multiple, because it seems strange
                        if (al.lexNodes.size() > 1) {
                            System.err.println("***WARNING*** Multiple name lexNodes in graph "+i+": "+al.lexNodes);
                        }
                        for (String lexN : al.lexNodes) {
                            Set<GraphEdge> opEdges = new HashSet<>();
                            for (GraphEdge e : graph.getGraph().outgoingEdgesOf(graph.getNode(lexN))) {
                                if (e.getLabel().matches("op[0-9]+") && al.nodes.contains(e.getTarget().getName())) {
                                    opEdges.add(e);
                                }
                            }
                            if (!opEdges.isEmpty()) {
                                for (GraphEdge e : opEdges) {
                                    repEdges.remove(e);
                                    repNodes.remove(e.getTarget().getName());
                                }
                                repNodes.get(lexN).setLabel(NAME_TOKEN);
                                repName = true;
                            }
                        }
                        if (repName) {
                            //collect name and wiki stats
                            GraphNode nameNode = graph.getNode(al.lexNodes.iterator().next());
                            GraphNode wikiNode = getWikiNodeForNameNode(nameNode, graph);
                            String name = encodeName(nameNode, graph);
                            String literalName = origSent.subList(al.span.start, al.span.end)
                                    .stream().collect(Collectors.joining("_"));
                            
                            count(lit2name, literalName, name);
                            if (wikiNode != null) {
                                count(lit2wiki, literalName, wikiNode.getLabel());
                            } else {
                                noWiki++;
                            }
                            
                            nameSpans.add(al.span);
                            replaced = true;
                        }
                    } else if (label.equals("date-entity")) {
                        //lets for assume that we want to replace all of these that have multiple nodes,
                            //and replace all outgoing edges labelled year, month or day, and their targets
                        //assume at this point that all lexNodes are date-entity nodes; but print a warning if there are multiple, because it seems strange
                        if (al.lexNodes.size() > 1) {
                            System.err.println("***WARNING*** Multiple date lexNodes in graph "+i+": "+al.lexNodes);
                        }
                        for (String lexN : al.lexNodes) {
                            for (GraphEdge e : graph.getGraph().outgoingEdgesOf(graph.getNode(lexN))) {
                                if ((e.getLabel().equals("day") || e.getLabel().equals("month") || e.getLabel().equals("year"))
                                        && al.nodes.contains(e.getTarget().getName())) {
                                    repEdges.remove(e);//note that e and the object in repEdges are not the same object, but equal
                                    repNodes.remove(e.getTarget().getName());
                                }
                            }
                            repNodes.get(lexN).setLabel(DATE_TOKEN);
                        }
                        dateSpans.add(al.span);
                        replaced = true;
                    }
                }
                
                //replace everything in the graph that is aligned to a number, will replace words later
                if (!replaced && al.span.isSingleton() && origSent.get(al.span.start).matches(NUMBER_REGEX)) {
                    //then we have a number
                    repNodes.get(al.lexNodes.iterator().next()).setLabel(NUMBER_TOKEN);
                }
            }
            
            // now simply replace ALL numbers in the string and tree      
            //this also replaces some dates, but they will be later re-replaced with the DATE_TOKEN
            List<String> paths2leaves = (List)repTree.getAllPathsToLeavesWithSeparator(SEP); //note that in this Tree class,
                //this indeed returns a list with the leaves in proper order.
            for (int k = 0; k<repSent.size(); k++) {
                if (repSent.get(k).matches(NUMBER_REGEX)) {
                    repSent.set(k, NUMBER_TOKEN);
                    repTree.selectWithSeparators(paths2leaves.get(k), 0, SEP).setLabel(NUMBER_TOKEN);
                }
            }
            
            SGraph repGraph = new SGraph();
            repGraph.setWriteAsAMR(true);
            for (GraphNode n : repNodes.values()) {
                repGraph.addNode(n.getName(), n.getLabel());
            }
            for (GraphEdge e : repEdges) {
                repGraph.addEdge(repGraph.getNode(e.getSource().getName()), repGraph.getNode(e.getTarget().getName()), e.getLabel());
            }
            for (String s : repS2n.keySet()) {
                repGraph.addSource(s, repS2n.get(s));
            }
            
            //Old: re-label all wiki-edge targets with label "-"
            //New: remove all wiki edges
            for (GraphEdge e : new HashSet<>(repGraph.getGraph().edgeSet())) {
                if (e.getLabel().equals("wiki")) {
                    //e.getTarget().setLabel("-");
                    repGraph.getGraph().removeEdge(e);
                    repGraph.removeNode(e.getTarget().getName());
                }
            }
            
            List<Alignment.Span> allLargeSpans = new ArrayList<>();
            allLargeSpans.addAll(nameSpans);
            allLargeSpans.addAll(dateSpans);
            
            //replace all date and name spans in tree and string
            allLargeSpans.sort((Alignment.Span o1, Alignment.Span o2) -> {
                return o2.start-o1.start;//gives us reverse order, later spans first!
            });
            for (Alignment.Span span : allLargeSpans) {
                String replacement = nameSpans.contains(span) ? NAME_TOKEN : DATE_TOKEN;

                //replace in string
                for (int k = span.end-1; k>span.start; k--) {
                    repSent.remove(k);
                }
                repSent.set(span.start, replacement);

                //replace in tree
                if (!origTree.getChildren().isEmpty()) {
                    //i.e. if we have a non-trivial tree
                    
                    for (int k = span.end-1; k>span.start; k--) {
                        if (k >= paths2leaves.size()) {
                            System.err.println("***WARNING*** Node not found in tree!");
                            System.err.println(als);
                            System.err.println(origSent);
                            System.err.println(repSent);
                            System.err.println(origTree);
                            System.err.println(repTree);
                            System.err.println(graph.toString());
                            System.err.println(repGraph.toString());
                            System.err.println(repGraph.toIsiAmrString());
                        }
                        removeNodeAndEmptyParents(repTree, paths2leaves.get(k));
                    }
                    repTree.selectWithSeparators(paths2leaves.get(span.start), 0, SEP).setLabel(replacement);
                }

            }

            
            //do the map that remembers which tokens in repSent and repTree came from which original span
            List<String> spanMap = new ArrayList<>();
            allLargeSpans.sort((Alignment.Span o1, Alignment.Span o2) -> {
                return o1.start-o2.start;//now sort in natural order
            });
            int largeSpanIndex = 0;
            for (int k = 0; k<origSent.size(); k++) {
                if (allLargeSpans.size() <= largeSpanIndex) {
                    spanMap.add(k+"-"+(k+1));
                } else {
                    Alignment.Span span = allLargeSpans.get(largeSpanIndex);
                    if (span.start == k) {
                        spanMap.add(span.start+"-"+span.end);
                        k = span.end-1;//such that after the k++ of the for-loop, we are at span.end
                        largeSpanIndex++;
                    } else {
                        spanMap.add(k+"-"+(k+1));
                    }
                }
            }            
            newI.getInputObjects().put("spanmap", spanMap);

            //compute new alignments
            List<Alignment> newAls = new ArrayList<>();
            for (int k = 0; k<spanMap.size(); k++) {
                Alignment.Span spanHere = new Alignment.Span(spanMap.get(k));
                for (Alignment al : als) {
                    if (al.span.equals(spanHere)) {
                        Set<String> nodes = new HashSet<>();
                        for (String nn : al.nodes) {
                            if (repGraph.containsNode(nn)) {
                                nodes.add(nn);
                            }
                        }
                        Set<String> lexNodes = new HashSet<>();
                        for (String nn : al.lexNodes) {
                            if (repGraph.containsNode(nn)) {
                                lexNodes.add(nn);
                            }
                        }
                        if (!nodes.isEmpty()) {
                            Alignment newAl = new Alignment(nodes, new Alignment.Span(k, k+1), lexNodes, 0);
                            newAls.add(newAl);
                        }
                    }
                }
            }
            newI.getInputObjects().put("repalignment", newAls.stream().map(al -> al.toString()).collect(Collectors.toList()));
            
            //compute new palignments, if they exist
            if (annotator.palignmentPath != null) {
                List<Alignment> newPAls = new ArrayList<>();
                for (Alignment al : pals) {
                    for (int k = 0; k<spanMap.size(); k++) {
                        Alignment.Span spanHere = new Alignment.Span(spanMap.get(k));
                        if (al.span.start >= spanHere.start && al.span.end <= spanHere.end) {
                            //if al.span is contained in spanHere, we need to replace it
                            Set<String> nodes = new HashSet<>();
                            for (String nn : al.nodes) {
                                if (repGraph.containsNode(nn)) {
                                    nodes.add(nn);
                                }
                            }
                            Set<String> lexNodes = new HashSet<>();
                            for (String nn : al.lexNodes) {
                                if (repGraph.containsNode(nn)) {
                                    lexNodes.add(nn);
                                }
                            }
                            if (!nodes.isEmpty()) {
                                Alignment newAl = new Alignment(nodes, new Alignment.Span(k, k+1), lexNodes, 0, al.getWeight());//important to keep weight here!
                                newPAls.add(newAl);
                                //then al is sorted and we are good.
                                break;
                            }
                        } else {
                            if ((al.span.start < spanHere.start && al.span.end > spanHere.start)
                                    || (al.span.start < spanHere.end && al.span.end > spanHere.end)) {
                                //System.err.println("***WARNING*** weirdly overlapping alignments in instance "+i+": "+al.span+" and "+spanHere);//this should actually be fine I think, given the permissive nature of the p alignments
                                //then al is a name or date we decided against, and we don't want to keep it!
                                break;
                            }
                        }
                    }
                }
                newI.getInputObjects().put("repalignmentp", newPAls.stream().map(al -> al.toString()).collect(Collectors.toList()));
            }
            
            
            //replace the rare words
            for (int k = 0; k<repSent.size(); k++) {
                String word = repSent.get(k);
                if (!word.equals(NAME_TOKEN) && !word.equals(DATE_TOKEN) && !word.equals(NUMBER_TOKEN)
                        && wordCounts.get(word.toLowerCase()) <= annotator.threshold) {
                    boolean treeNontrivial = (!origTree.getChildren().isEmpty());
                    String posTag;
                    if (treeNontrivial) {
                        String path2leaf = paths2leaves.get(k);
                        String path2parent = path2leaf.substring(0, path2leaf.lastIndexOf(SEP));
                        posTag = repTree.selectWithSeparators(path2parent, 0, SEP).getLabel();
                        repTree.selectWithSeparators(path2leaf, 0, SEP).setLabel(posTag);
                    } else {
                        posTag = UNK_TOKEN;
                    }
                    
                    repSent.set(k, posTag);
                    int origK = Integer.valueOf(spanMap.get(k).split("-")[0]);
                    for (Alignment al : als) {
                        if (al.span.start == origK) {
                            if (al.span.end != origK+1) {
                                System.err.println("***WARNING*** found span "+al.span+" in graph "+i+" for a rare word that is not a singleton.");
                                System.err.println(als);
                                System.err.println(al);
                                System.err.println(origSent);
                                System.err.println(repSent);
                                System.err.println(spanMap);
                                System.err.println(graph.toString());
                                System.err.println(repGraph.toString());
                            }
                            for (String lexN : al.lexNodes) {
                                if (repGraph.containsNode(lexN)) {
                                    repGraph.getNode(lexN).setLabel(posTag);
                                } else {
                                    System.err.println("***WARNING*** lexNode "+lexN+" not present int repGraph  "+i+".");
                                    System.err.println(als);
                                    System.err.println(al);
                                    System.err.println(origSent);
                                    System.err.println(repSent);
                                    System.err.println(spanMap);
                                    System.err.println(graph.toString());
                                    System.err.println(repGraph.toString());
                                }
                            }
                        }
                    }
                }
            }
            
            try {
                repGraph.toIsiAmrString();
            } catch (java.lang.Exception ex) {
                System.err.println(de.up.ling.irtg.util.Util.getStackTrace(ex));
                try {
                   graph.toIsiAmrString();
                } catch (java.lang.Exception ex2) {
                    System.err.println("Orig fails as well.");
                }
                repGraph = new IsiAmrInputCodec().read("(d<root> / disconnected)");
            }
            newI.getInputObjects().put("repgraph", repGraph);
            newI.getInputObjects().put("reptree", repTree);
            newI.getInputObjects().put("repstring", repSent);
            
            outC.addInstance(newI);
            
            i++;
        }
        
        //write corpus
        InterpretedTreeAutomaton writerIRTG = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton<>());
        writerIRTG.addInterpretation("graph", new Interpretation(new GraphAlgebra(), new Homomorphism(dummySig, dummySig)));
        writerIRTG.addInterpretation("string", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        writerIRTG.addInterpretation("tree", new Interpretation(new TreeWithAritiesAlgebra(), new Homomorphism(dummySig, dummySig)));
        writerIRTG.addInterpretation("repgraph", new Interpretation(new GraphAlgebra(), new Homomorphism(dummySig, dummySig)));
        writerIRTG.addInterpretation("repstring", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        writerIRTG.addInterpretation("reptree", new Interpretation(new TreeWithAritiesAlgebra(), new Homomorphism(dummySig, dummySig)));
        writerIRTG.addInterpretation("spanmap", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        writerIRTG.addInterpretation("alignment", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        writerIRTG.addInterpretation("repalignment", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        if (annotator.palignmentPath != null) {
            writerIRTG.addInterpretation("alignmentp", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
            writerIRTG.addInterpretation("repalignmentp", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        }
        try (FileWriter w = new FileWriter(annotator.outPath)) {
            new CorpusWriter(writerIRTG, annotator.comment + " Replaced all words with frequency <= "+annotator.threshold, "///###", w).writeCorpus(outC);
        }
        
        //write and print stats
        System.err.println("no wiki: "+noWiki);
        String pathPrefix = new File(annotator.outPath).getParent()+"/";
        List<String> sortedNames = new ArrayList<>(lit2name.keySet());
        sortedNames.sort((String o1, String o2) -> -Integer.compare(lit2name.get(o1).sum(), lit2name.get(o2).sum()));
        FileWriter wikiStatsW = new FileWriter(pathPrefix+"wikiStats.txt");
        FileWriter wikiLookupW = new FileWriter(pathPrefix+"wikiLookup.txt");
        for (String lit : sortedNames) {
            if (lit2wiki.containsKey(lit)) {
                Counter<String> c = lit2wiki.get(lit);
                wikiLookupW.write(lit+"\t"+c.argMax()+"\n");
                wikiStatsW.write(lit+": ");
                for (Object2IntMap.Entry<String> e : c.getAllSorted()) {
                    wikiStatsW.write(e.getKey()+"/"+e.getIntValue()+" ");
                }
                wikiStatsW.write("\n");
            }
        }
        wikiStatsW.close();
        wikiLookupW.close();
        
        FileWriter nameStatsW = new FileWriter(pathPrefix+"nameStats.txt");
        FileWriter nameLookupW = new FileWriter(pathPrefix+"nameLookup.txt");
        for (String lit : sortedNames) {
            if (lit2name.containsKey(lit)) {
                Counter<String> c = lit2name.get(lit);
                nameLookupW.write(lit+"\t"+c.argMax()+"\n");
                nameStatsW.write(lit+": ");
                for (Object2IntMap.Entry<String> e : c.getAllSorted()) {
                    nameStatsW.write(e.getKey()+"/"+e.getIntValue()+" ");
                }
                nameStatsW.write("\n");
            }
        }
        nameStatsW.close();
        nameLookupW.close();
    }
    
    private static void removeNodeAndEmptyParents(Tree tree, String path) {
        String path2parent = path.substring(0, path.lastIndexOf(SEP));
        int index = Integer.valueOf(path.substring(path.lastIndexOf(SEP)+1));
        Tree parent = tree.selectWithSeparators(path2parent, 0, SEP);
        parent.getChildren().remove(index);
        if (parent.getChildren().isEmpty()) {
            removeNodeAndEmptyParents(tree, path2parent);
        }
    }
    
    private static GraphNode getWikiNodeForNameNode(GraphNode nameNode, SGraph graph) {
        GraphEdge nameEdge = null;
        for (GraphEdge edge : graph.getGraph().incomingEdgesOf(nameNode)) {
            if (edge.getLabel().equals("name")) {
                nameEdge = edge;
                break;
            }
        }
        if (nameEdge != null) {
            GraphNode neNode = BlobUtils.otherNode(nameNode, nameEdge);
            GraphEdge wikiEdge = null;
            for (GraphEdge edge : graph.getGraph().outgoingEdgesOf(neNode)) {
                if (edge.getLabel().equals("wiki")) {
                    wikiEdge = edge;
                    break;
                }
            }
            if (wikiEdge != null) {
                return BlobUtils.otherNode(neNode, wikiEdge);
            }
        }
        return null;
    }
    
    private static String encodeName(GraphNode nameNode, SGraph graph) {
        Map<Integer, String> op2label = new HashMap<>();
        for (GraphEdge edge : graph.getGraph().outgoingEdgesOf(nameNode)) {
            if (edge.getLabel().matches("op[0-9]+")) {
                op2label.put(Integer.parseInt(edge.getLabel().substring(2)),
                        BlobUtils.otherNode(nameNode, edge).getLabel());
            }
        }
        
        //catching both 0-based and 1-based ops
        List<String> name = new ArrayList<>();
        for (int i = 0; i<op2label.size()+1; i++) {
            if (op2label.containsKey(i)) {
                name.add(op2label.get(i));
            }
        }
        return name.stream().collect(Collectors.joining(":"));
    }
    
    private static void count(Map<String, Counter<String>> map, String key, String value) {
        Counter<String> c = map.get(key);
        if (c == null) {
            c = new Counter<>();
            map.put(key, c);
        }
        c.add(value);
    }
    
}
