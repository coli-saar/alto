/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtools;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.graph.BlobUtils;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.codec.IsiAmrInputCodec;
import de.up.ling.irtg.util.Counter;
import de.up.ling.irtg.util.Util;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.data.ILoadPolicy;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import edu.mit.jwi.morph.WordnetStemmer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author Jonas
 */
public class Relabel {
    
    public static final String LEXMARKER = "LEX@";
    
    public static int allNameCount = 0;
    public static int seenNameCount = 0;
    public static Counter<String> seenNETypeCounter = new Counter<>();
    public static Counter<String> unseenNETypeCounter = new Counter<>();
    
    public static void main(String[] args) throws IOException, MalformedURLException, InterruptedException {
        
        if (args.length < 3) {
            System.err.println("Needs three parameters, fourth is optional");
            System.err.println("1) path to parser output, must contain files parserOut.txt (output of parser, including root markers, one AMR per line), sentences.txt, literal.txt, labels.txt (all from the supertagger) " +
                " and a file indices.txt that contains an index for each instance of parserOut that refers to its original index in the sentences.txt etc files. Also goldAMR.txt");
            System.err.println("2) path to lookup folder (nameLookup.txt etc)");
            System.err.println("3) path to wordnet (3.0/dict/ folder)");
            System.err.println("4) (optional) threshold for when to use the backup over the neural predictions");
        }
        
        String parserOutPath = args[0]; // must contain files parserOut.txt (output of parser, including root markers, one AMR per line), sentences.txt, literal.txt, labels.txt (all from the supertagger)
        // and a file indices.txt that contains an index for each instance of parserOut that refers to its original index in the sentences.txt etc files. Also goldAMR.txt
        String lookupPath = args[1];
        String wordnetPath = args[2];
        
        int threshold = 10;
        if (args.length >3) {
            System.err.println("No threshold parameter given, using default "+threshold);
            threshold = Integer.parseInt(args[3]);
        }
        
        Relabel relabel = new Relabel(wordnetPath, lookupPath, threshold, 0);
        
        BufferedReader sentBR = new BufferedReader(new FileReader(parserOutPath+"sentences.txt"));
        List<String> sentences = sentBR.lines().collect(Collectors.toList());
        BufferedReader litBR = new BufferedReader(new FileReader(parserOutPath+"literal.txt"));
        List<String> literals = litBR.lines().collect(Collectors.toList());
        BufferedReader labelBR = new BufferedReader(new FileReader(parserOutPath+"labels.txt"));
        List<String> nnLabels = labelBR.lines().collect(Collectors.toList());
        BufferedReader goldBR = new BufferedReader(new FileReader(parserOutPath+"goldAMR.txt"));
        List<String> golds = goldBR.lines().collect(Collectors.toList());
        
        BufferedReader indicesBR = new BufferedReader(new FileReader(parserOutPath+"indices.txt"));
        BufferedReader parserOutBR = new BufferedReader(new FileReader(parserOutPath+"parserOut.txt"));
        
        FileWriter w = new FileWriter(parserOutPath + "relabeled.txt");
        FileWriter goldW = new FileWriter(parserOutPath + "gold_orderedAsRelabeled.txt");
        
        int orphanDateEdges = 0;
        
        StringAlgebra alg = new StringAlgebra();
        while (parserOutBR.ready() && indicesBR.ready()) {
            SGraph graph = new IsiAmrInputCodec().read(parserOutBR.readLine());
            int index = Integer.parseInt(indicesBR.readLine());
            try {
                relabel.fixGraph(graph, alg.parseString(sentences.get(index)), alg.parseString(literals.get(index)), alg.parseString(nnLabels.get(index)));
            } catch (java.lang.Exception ex) {
                ex.printStackTrace();
            }
            for (GraphEdge e : graph.getGraph().edgeSet()) {
                if ((e.getLabel().equals("day") || e.getLabel().equals("month") || e.getLabel().equals("year"))
                        && !e.getSource().getLabel().equals("date-entity")) {
                    orphanDateEdges++;
                }
            }
            w.write(graph.toIsiAmrString()+"\n\n");
            goldW.write(golds.get(index)+"\n\n");
        }
        
        System.err.println("orphan date edges: "+orphanDateEdges);
        
        w.close();
        goldW.close();
        
//        System.err.println(allNameCount);
//        System.err.println(seenNameCount);
//        System.err.println(seenNameCount/(double)allNameCount);
//        System.err.println("Seen NE types (predicted):");
//        seenNETypeCounter.printAllSorted();
//        System.err.println("Unseen NE types (predicted):");
//        unseenNETypeCounter.printAllSorted();
        
//        List<String> sent = new StringAlgebra().parseString("_name_ runs");
//        List<String> lit = new StringAlgebra().parseString("Obama runs");
//        List<String> nnLabels = new StringAlgebra().parseString("_NAME_ run-09");
        
        
//        relabel.fixGraph(graph, sent, lit, nnLabels);
//        
//        System.err.println(graph.toIsiAmrStringWithSources());
        
//        relabel.getLabel("prefer");
//        relabel.getLabel("dogs");
        
    }
    
    
    private final RAMDictionary dict;
    private final WordnetStemmer stemmer;
    
    private final Map<String, String> lit2name;
    private final Map<String, String> lit2wiki;
    private final Map<String, String> lit2type;
    private final Object2IntMap<String> word2count;
    private final Map<String, String> word2label;
    private final int nnThreshold;
    private final int lookupThreshold;
    
    
    public Relabel(String wordnetPath, String mapsPath, int nnThreshold, int lookupThreshold)
            throws IOException, MalformedURLException, InterruptedException {
        
        URL url = new URL("file", null, wordnetPath);
        
        dict = new RAMDictionary(url, ILoadPolicy.BACKGROUND_LOAD);
        dict.open();
        //dict.load(true);//do this when aligning full corpus
        
        stemmer = new WordnetStemmer(dict);
        
        lit2name = readMap(mapsPath+"nameLookup.txt");
        lit2wiki = readMap(mapsPath+"wikiLookup.txt");
        lit2type = readMap(mapsPath+"nameTypeLookup.txt");
        word2label = readMap(mapsPath+"words2labelsLookup.txt");
        word2count = readCounts(mapsPath+"words2labelsLookup.txt");
        this.nnThreshold = nnThreshold;
        this.lookupThreshold = lookupThreshold;
    }
    
    
    public void fixGraph(SGraph graph, List<String> sent, List<String> lit, List<String> nnLabels) {
        
        for (GraphNode node : new HashSet<>(graph.getGraph().vertexSet())) {
            if (node.getLabel().matches(LEXMARKER+"[0-9]+")) {
                int i = Integer.parseInt(node.getLabel().substring(LEXMARKER.length()));
                String nextWord = (i+1 < sent.size()) ? sent.get(i+1) : null;
                if (i >= sent.size() || i >= nnLabels.size()) {
                    System.err.println(sent);
                    System.err.println(nnLabels);
                    System.err.println(graph);
                }
                fixLabel(sent.get(i), lit.get(i), nextWord, nnLabels.get(i), graph, node);
            }
        }
        
    }
    
    
    public void fixLabel(String word, String lit, String nextWord,
            String nnLabel, SGraph graph, GraphNode lexNode) {
        
        if (word.equals(RareWordsAnnotator.NAME_TOKEN.toLowerCase())) {
            
            allNameCount++;
            
            lexNode.setLabel("name");
            String lookupName = lit2name.get(lit);
            String[] ops;
            boolean seen = false;
            if (lookupName != null && !lookupName.equals("")) {
                seenNameCount++;
                seen = true;
                ops = lookupName.split(":");
            } else {
                ops = lit.split("_");
            }
            int i = 1;
            for (String op : ops) {
                GraphNode opNode = graph.addNode(Util.gensym("explicitanon"), op);
                graph.addEdge(lexNode, opNode, "op"+i);
                i++;
            }
            
            //add wiki node
            GraphEdge nameEdge = null;
            for (GraphEdge edge : graph.getGraph().incomingEdgesOf(lexNode)) {
                if (edge.getLabel().equals("name")) {
                    nameEdge = edge;
                    break;
                }
            }
            if (nameEdge != null) {
                String wikiEntry = lit2wiki.containsKey(lit) ? lit2wiki.get(lit) : "-";
                GraphNode wikiNode = graph.addNode(Util.gensym("explicitanon"), wikiEntry);
                GraphNode neNode = BlobUtils.otherNode(lexNode, nameEdge);
                if (lit2type.containsKey(lit) && !neNode.getLabel().startsWith("LEX@")) {
                    neNode.setLabel(lit2type.get(lit));
                }
                graph.addEdge(neNode, wikiNode, "wiki");
            }
            
            
        } else if (word.equals(RareWordsAnnotator.DATE_TOKEN.toLowerCase())) {
            
            lexNode.setLabel("date-entity");
            
            Integer year;
            Integer month;
            Integer day;
            
            if (lit.contains("-")) {
                //yyyy-MM-dd
                year = Integer.parseInt(lit.substring(0, 4));
                month = Integer.parseInt(lit.substring(5, 7));
                day = Integer.parseInt(lit.substring(8, 10));
            } else if (lit.contains("/")) {
                //MM/dd/yy
                year = fixYear(Integer.parseInt(lit.substring(6, 8)));
                month = Integer.parseInt(lit.substring(0, 2));
                day = Integer.parseInt(lit.substring(3, 5));
            } else {
                //yyMMdd
                year = fixYear(Integer.parseInt(lit.substring(0, 2)));
                month = Integer.parseInt(lit.substring(2, 4));
                day = Integer.parseInt(lit.substring(4, 6));
            }
            
            if (year > 0) {
                GraphNode node = graph.addNode(Util.gensym("explicitanon"), String.valueOf(year));
                graph.addEdge(lexNode, node, "year");
            }
            if (month > 0) {
                GraphNode node = graph.addNode(Util.gensym("explicitanon"), String.valueOf(month));
                graph.addEdge(lexNode, node, "month");
            }
            if (day > 0) {
                GraphNode node = graph.addNode(Util.gensym("explicitanon"), String.valueOf(day));
                graph.addEdge(lexNode, node, "day");
            }
            
        } else if (word.equals(RareWordsAnnotator.NUMBER_TOKEN.toLowerCase())) {
            
            //recover large numbers
            if (nextWord == null) {
                lexNode.setLabel(lit);
            } else {
                if (nextWord.toLowerCase().startsWith("hundred")) {
                    lexNode.setLabel(shiftZero(lit, 2));
                } else if (nextWord.toLowerCase().startsWith("thousand")) {
                    lexNode.setLabel(shiftZero(lit, 3));
                } else if (nextWord.toLowerCase().startsWith("million")) {
                    lexNode.setLabel(shiftZero(lit, 6));
                } else if (nextWord.toLowerCase().startsWith("billion")) {
                    lexNode.setLabel(shiftZero(lit, 9));
                } else if (nextWord.toLowerCase().startsWith("trillion")) {
                    lexNode.setLabel(shiftZero(lit, 12));
                } else {
                    //else use number directly
                    lexNode.setLabel(lit.replaceAll("[,.]", ""));
                }
            }
            
        } else {
            // find verb stems
            
            int count = word2count.getInt(word);
            
            if (count >= nnThreshold) {
                lexNode.setLabel(nnLabel);
            } else if (count > lookupThreshold && word2label.containsKey(word)) {
                lexNode.setLabel(word2label.get(word));
            } else {
                boolean hasArgs = false;
                for (GraphEdge edge : graph.getGraph().outgoingEdgesOf(lexNode)) {
                    if (edge.getLabel().startsWith("ARG")) {
                        hasArgs = true;
                        break;
                    }
                }
                
                if (hasArgs) {
                    boolean foundWithStemming = false;
                    try {
                        List<String> verbStems = stemmer.findStems(word, POS.VERB);
                        if (!verbStems.isEmpty()) {
                            IIndexWord idxWord = dict.getIndexWord(verbStems.iterator().next(), POS.VERB);
                            try {
                                lexNode.setLabel(dict.getWord(idxWord.getWordIDs().iterator().next()).getLemma()+"-01");
                                foundWithStemming = true;
                            } catch (java.lang.Exception ex) {
                                //ex.printStackTrace();
                            }
                        }
                    } catch (IllegalArgumentException ex) {
                        System.err.println(word);
//                        ex.printStackTrace();
                    }
                        
                    if (!foundWithStemming) {
                    
                        //find related verbs
                        Set<IWord> iWords = new HashSet<>();
                        for (POS pos : POS.values()) {
                            try {
                                for (String stem : stemmer.findStems(word, pos)) {
                                    IIndexWord idxWord = dict.getIndexWord(stem, pos);
                                    if (idxWord != null) {
                                        for (IWordID wordID : idxWord.getWordIDs()) {
                                            iWords.add(dict.getWord(wordID));
                                        }
                                    }
                                }
                            } catch (java.lang.IllegalArgumentException ex) {
                                System.err.println("*** WARNING *** "
                                        + de.up.ling.irtg.util.Util.getStackTrace(ex));
                            }
                        }
                        Set<IWord> seen = new HashSet<>(iWords);
                        boolean changed = true;
                        while (changed) {
                            changed = false;
                            for (IWord iW : new HashSet<>(seen)) {
                                List<IWord> newHere = iW.getRelatedWords(Pointer.DERIVATIONALLY_RELATED)
                                        .stream().map(id -> dict.getWord(id)).collect(Collectors.toList());
                                for (IWord newIW : newHere) {
                                    if (!seen.contains(newIW)) {
                                        changed = true;
                                        seen.add(newIW);
                                    }
                                }
                            }
                        }
                        seen.removeIf(iW -> !iW.getPOS().equals(POS.VERB));
                        if (!seen.isEmpty()) {
                            lexNode.setLabel(seen.iterator().next().getLemma()+"-01");
                        } else {
                            lexNode.setLabel(word+"-01");
                        }
                    }
                    
                } else {
                    boolean foundWithStemming = false;
                    try {
                        List<String> nounStems = stemmer.findStems(word, POS.NOUN);
                        if (!nounStems.isEmpty()) {
                            IIndexWord idxWord = dict.getIndexWord(nounStems.iterator().next(), POS.NOUN);
                            try {
                                lexNode.setLabel(dict.getWord(idxWord.getWordIDs().iterator().next()).getLemma());
                                foundWithStemming = true;
                            } catch (java.lang.Exception ex) {
                                //ex.printStackTrace();
                            }
                        }
                    } catch (IllegalArgumentException ex) {
//                        ex.printStackTrace();
                    }
                    
                    if (!foundWithStemming) {
                        lexNode.setLabel(word);
                    }
                }
            }

        }
    }
    
    public static Map<String, String> readMap(String filePath) throws IOException {
        Map<String, String> ret = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        while (br.ready()) {
            String[] parts = br.readLine().split("\t");
            if (parts.length > 1) {
                ret.put(parts[0], parts[1]);
            }
        }
        return ret;
    }
    
    private static Object2IntMap<String> readCounts(String filePath) throws IOException {
        Object2IntMap<String> ret = new Object2IntOpenHashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        while (br.ready()) {
            String[] parts = br.readLine().split("\t");
            if (parts.length > 2) {
                ret.put(parts[0], Integer.parseInt(parts[2]));
            }
        }
        return ret;
    }
    
    private static int fixYear(int twoDigitYear) {
        return (twoDigitYear < 20) ? twoDigitYear+2000 : twoDigitYear+1900;
    }
    
    private static String shiftZero(String number, int amount) {
        for (int i = 0; i<amount; i++) {
            int dotIndex = number.indexOf(".");
            if (dotIndex == -1) {
                number = number+"0";
            } else {
                number = number.replace(".", "");
                if (dotIndex < number.length() -1) {
                    //re-insert dot one further
                    number = number.substring(0, dotIndex+1)+"."+number.substring(dotIndex+1);
                }
                //else keep it as is
            }
        }
        return number;
    }
    
}
