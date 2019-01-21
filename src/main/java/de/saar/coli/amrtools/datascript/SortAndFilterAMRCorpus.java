/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtools.datascript;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.MutableInteger;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sorts a corpus by node count of the graph entry, optionally removing graphs
 * over a certain size.
 * @author Jonas
 */
public class SortAndFilterAMRCorpus {
    
    static String CORPUS_VERSION = "1.0";
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s*");
    private static final Pattern UNANNOTATED_CORPUS_DECLARATION_PATTERN = Pattern.compile("\\s*(\\S+)\\s*IRTG unannotated corpus file, v(\\S+).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANNOTATED_CORPUS_DECLARATION_PATTERN = Pattern.compile("\\s*(\\S+)\\s*IRTG annotated corpus file, v(\\S+).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern INTERPRETATION_DECLARATION_PATTERN = Pattern.compile("\\s*interpretation\\s+([^: ]+)\\s*:\\s*(\\S+).*", Pattern.CASE_INSENSITIVE);

    
    @Parameter(description = "<path to corpus>")
    private List<String> corpusPath;
    
    @Parameter(names = {"--alignmentfile", "-a"}, description = "Path to alignment file")
    private String alignmentPath = null;
    
    @Parameter(names = {"--graphinterpretation", "-gi"}, description = "Name of graph interpretation in the corpus, by which the filtering and sorting should occur. Default is 'graph'.")
    private String graphInterp = "graph";
    
    @Parameter(names = {"--comment", "-c"}, description = "Path to alignment file")
    private String comment = "";
    
    @Parameter(names = {"--maxnodes", "-m"}, description = "Keep only instances with at most this many nodes. Use -1 to keep all instances.")
    private int max = -1;
    
        
    @Parameter(names = {"--help", "-?"}, description = "displays help if this is the only command", help = true)
    private boolean help = false;
    
    
    /**
     * Sorts a corpus by AMR size and removes all instances over a threshold size.
     * Also sorts an alignment file in parallel, if it exists. First argument is
     * folder path that contains a finalAlto.corpus file, and, optionally, a
     * AlignmentsTranslated.keep file. Second argument is max number of AMR nodes
     * (use -1 to keep all). Third argument is a comment to be added to the
     * resulting corpus, e.g. original corpus version.
     * @param args
     * @throws IOException
     * @throws ParserException 
     */
    public static void main(String[] args) throws IOException, ParserException, CorpusReadingException {
        
        SortAndFilterAMRCorpus sorter = new SortAndFilterAMRCorpus();
        
        JCommander commander = new JCommander(sorter);
        commander.setProgramName("SortAndFilterAMRCorpus");
        
        try {
            commander.parse(args);
        } catch (com.beust.jcommander.ParameterException ex) {
            System.err.println("An error occured: " + ex.toString());
            System.err.println("\n Available options: ");
            commander.usage();
            return;
        }

        if (sorter.help) {
            commander.usage();
            return;
        }
        
        BufferedReader br = new BufferedReader(new FileReader(sorter.corpusPath.get(0)));
        
        BufferedReader alBR = null;
        if (sorter.alignmentPath != null) {
            alBR = new BufferedReader(new FileReader(sorter.alignmentPath));
        }
        
        //skip header of corpus file, but remember it for later
        String suffix = (sorter.max == -1) ? "sorted" : "filtered_"+sorter.max;
        String header = "";
        String line = br.readLine();
        int l = 0;
        while (!line.trim().equals("")) {
            header += line+"\n";
            if (l == 1) {
                header += line.split(" ")[0] + " " + sorter.comment + " "+suffix+"\n";//at comment marker at start of line
            }
            l++;
            line = br.readLine();
        }
        while (line.trim().equals("")) {
            header += "\n";
            line = br.readLine();
        }
        //at this point, line is the first line of the first instance.//TODO handle comments in corpus
        
        
        InterpretedTreeAutomaton loaderIRTG = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton<>());
        Signature dummySig = new Signature();
        loaderIRTG.addInterpretation(new Interpretation(new GraphAlgebra(), new Homomorphism(dummySig, dummySig), sorter.graphInterp));
        Corpus corpus = Corpus.readCorpusWithStrictFormatting(new FileReader(sorter.corpusPath.get(0)), loaderIRTG);
        
        int interpCount = countCorpusInterpretations(sorter.corpusPath.get(0));
        
        
        List<Instance> ret = new ArrayList<>();
        
        int j = 0;
        for (de.up.ling.irtg.corpus.Instance inst : corpus) {
            //System.err.println(j);
            j++;
            SGraph graph = (SGraph)inst.getInputObjects().get(sorter.graphInterp);
            //System.err.println(graph.toIsiAmrStringWithSources());
            String corpusEntry = "";
            for (int i = 0; i<interpCount; i++) {
                corpusEntry += line+"\n";
                if (br.ready()) {
                    line = br.readLine();
                } else {
                    break;
                }
            }
            while (line.trim().equals("")) {
                if (br.ready()) {
                    line = br.readLine();
                } else {
                    break;
                }
            }
            //at this point, line is the first of the next instance.
            String al = (alBR==null) ? null : alBR.readLine();
            Instance instToStore = new Instance(corpusEntry, graph.getGraph().vertexSet().size(), al);//new Instance(graph, tree, string, alBR.readLine());
            //System.err.println(corpusEntry);
            if (sorter.max < 0 || instToStore.size<=sorter.max) {
                ret.add(instToStore);
            }
        }
        
        ret.sort(Comparator.comparingInt(inst -> inst.size));
        
        String prefix = sorter.corpusPath.get(0).endsWith(".corpus") ? sorter.corpusPath.get(0).substring(0, sorter.corpusPath.get(0).length()-".corpus".length()) : sorter.corpusPath.get(0);
        
        FileWriter w = new FileWriter(prefix+"_"+suffix+".corpus");
        String alPrefix = null;
        if (sorter.alignmentPath != null) {
            alPrefix = sorter.alignmentPath.endsWith(".align") ? sorter.alignmentPath.substring(0, sorter.alignmentPath.length()-".align".length()) : sorter.alignmentPath;
        }
        FileWriter alW = (alBR==null) ? null : new FileWriter(alPrefix+"_"+suffix+".align");
        w.write(header);
        for (Instance inst : ret) {
            w.write(inst.corpusEntry+"\n");
            if (alW != null) {
                alW.write(inst.align+"\n");
            }
        }
        w.close();
        if (alW != null) {
            alW.close();
        }
    }
    
    private static class Instance {
        String corpusEntry;
        String align;
        int size;
        
        public Instance(String corpusEntry, int size, String align) {
            this.corpusEntry = corpusEntry;
            this.size = size;
            this.align = align;
        }
        
    }
    
    private static int countCorpusInterpretations(String path) throws CorpusReadingException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        List<String> interpretationOrder = new ArrayList<>();
        Map<String, Object> currentInputs = new HashMap<>();
        int currentInterpretationIndex = 0;
        MutableInteger lineNumber = new MutableInteger(0);
        String commentPrefix = null;

        // first non-blank line is declaration of annotated or unannotated corpus
        String line = readNextLine(br, lineNumber);

        Matcher unannoMatcher = UNANNOTATED_CORPUS_DECLARATION_PATTERN.matcher(line);
        if (unannoMatcher.matches()) {
            commentPrefix = unannoMatcher.group(1);
            if (!CORPUS_VERSION.equals(unannoMatcher.group(2))) {
                throw new CorpusReadingException("Expecting corpus file format version " + CORPUS_VERSION + ", but file is version " + unannoMatcher.group(1));
            }
        } else {
            Matcher annoMatcher = ANNOTATED_CORPUS_DECLARATION_PATTERN.matcher(line);
            if (annoMatcher.matches()) {
                commentPrefix = annoMatcher.group(1);
                if (!CORPUS_VERSION.equals(annoMatcher.group(2))) {
                    throw new CorpusReadingException("Expecting corpus file format version " + CORPUS_VERSION + ", but file is version " + annoMatcher.group(1));
                }
            }
        }

        if (commentPrefix == null) {
            throw new CorpusReadingException("First non-blank line of corpus must be corpus declaration, but was " + line);
        }

//        System.err.println("comment pattern: |" + commentPrefix + "|");
        // read and check header
        while (true) {
            line = readNextLine(br, lineNumber);

            if (line == null) {
                break;
            }

            String stripped = readAsComment(line, commentPrefix);
            if (stripped == null) {
                // first non-comment, non-empty, non-metadata-declaring line => finished reading metadata
                break;
            } else {
                Matcher interpretationMatcher = INTERPRETATION_DECLARATION_PATTERN.matcher(stripped);
                if (interpretationMatcher.matches()) {
                    String interpretationName = interpretationMatcher.group(1);

                    interpretationOrder.add(interpretationName);
                }
            }
        }
        return interpretationOrder.size();
    }
    
    private static String readNextLine(BufferedReader br, MutableInteger lineNumber) throws IOException {
        String ret = null;

        do {
            ret = br.readLine();
            lineNumber.incValue();
        } while (ret != null && WHITESPACE_PATTERN.matcher(ret).matches());


        return ret;
    }
    
    // Returns the line with leading whitespace + commentPrefix removed.
    // If the line does not start with whitespace + commentPrefix, returns null.
    private static String readAsComment(String line, String commentPrefix) {
        int pos = line.indexOf(commentPrefix);

        if (pos < 0) {
            return null;
        } else {
            for (int i = 0; i < pos; i++) {
                if (!Character.isWhitespace(line.charAt(i))) {
                    return null;
                }
            }

            return line.substring(pos + commentPrefix.length());
        }
    }
}
