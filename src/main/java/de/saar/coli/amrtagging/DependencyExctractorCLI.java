/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtagging;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.saar.coli.amrtagging.Alignment.Span;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.graph.AMSignatureBuilder;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.MutableInteger;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import java.io.FileWriter;
import java.util.stream.Collectors;

/**
 * Command line interface for the DependencyExtractor class; call with --help to see options.
 * @author jonas
 */
public class DependencyExctractorCLI {
    @Parameter(names = {"--corpusPath", "-c"}, description = "Path to the input corpus", required = true)
    private String corpusPath;

    @Parameter(names = {"--outPath", "-o"}, description = "Prefix for output files", required = true)
    private String outPath;
    
    @Parameter(names = {"--vocabPath", "-v"}, description = "Prefix for vocab files from a previous run that should be used here (e.g. to training vocab when doing dev/test files)")
    private String vocabPath = null;
    
    @Parameter(names = {"--posPath", "-pos"}, description = "Path to the stanford POS tagger model file english-bidirectional-distsim.tagger", required = true)
    private String posPath;
    
    @Parameter(names = {"--threads", "-t"}, description = "Number of threads over which the instances should be parallelized")
    private int numThreads = 1;
    
    @Parameter(names = {"--limit", "-li"}, description = "Number of minutes after which the process will be terminated")
    private long limit = 999999999;
    
    @Parameter(names = {"--coref", "-cr"}, description = "Set this flag to allow coref sources")
    private boolean coref = false;
    
//    @Parameter(names = {"--joint"}, description = "Set this flag to track alignments jointly (using the alignmentp values)")
//    private boolean joint = false;
//    
//    @Parameter(names = {"--doWrap"}, description = "Set this flag to make the JointAlignmentTrackingAutomaton spread (only does something in conjunction with --joint)")
//    private boolean doWrap = false;
//    
//    @Parameter(names = {"--spread"}, description = "How many words next to a span are allowed to be covered without alignment in")
//    private int maxJointSpread = 2;
    
    @Parameter(names = {"--help", "-?"}, description = "displays help if this is the only command", help = true)
    private boolean help = false;
    
    /**
     * Command line interface for the DependencyExtractor class; call with --help to see options.
     * @param args
     * @throws FileNotFoundException
     * @throws IOException
     * @throws CorpusReadingException
     * @throws IllegalArgumentException
     * @throws ParseException
     * @throws InterruptedException 
     */
    public static void main(String[] args) throws FileNotFoundException, IOException, CorpusReadingException, IllegalArgumentException, ParseException, InterruptedException {
        
        DependencyExctractorCLI cli = new DependencyExctractorCLI();
        JCommander commander = new JCommander(cli);
        commander.setProgramName("constraint_extractor");

        try {
            commander.parse(args);
        } catch (com.beust.jcommander.ParameterException ex) {
            System.err.println("An error occured: " + ex.toString());
            System.err.println("\n Available options: ");
            commander.usage();
            return;
        }

        if (cli.help) {
            commander.usage();
            return;
        }
        
        InterpretedTreeAutomaton loaderIRTG = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton());
        Signature dummySig = new Signature();
        loaderIRTG.addInterpretation("repgraph", new Interpretation(new GraphAlgebra(), new Homomorphism(dummySig, dummySig)));
        loaderIRTG.addInterpretation("repstring", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        loaderIRTG.addInterpretation("string", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
        loaderIRTG.addInterpretation("spanmap", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
//        if (cli.joint) {
//            loaderIRTG.addInterpretation("repalignmentp", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
//        } else {
        loaderIRTG.addInterpretation("repalignment", new Interpretation(new StringAlgebra(), new Homomorphism(dummySig, dummySig)));
//        }
        
        Corpus corpus = Corpus.readCorpusWithStrictFormatting(new FileReader(cli.corpusPath), loaderIRTG);
        
        MaxentTagger tagger = new MaxentTagger(cli.posPath);
        
        //BufferedReader alBr = new BufferedReader(new FileReader("examples/toy.align"));
        
        DependencyExtractor extr = (cli.vocabPath == null) ? new DependencyExtractor(cli.outPath) : new DependencyExtractor(cli.outPath, cli.vocabPath);
        FileWriter posWriter = new FileWriter(cli.outPath+"pos.txt");
        Set<String> allPosTags = new HashSet<>();
        FileWriter literalWriter = new FileWriter(cli.outPath+"literal.txt");
        
        MutableInteger nextInstanceID = new MutableInteger(0);
        ForkJoinPool forkJoinPool = new ForkJoinPool(cli.numThreads);
        MutableInteger success = new MutableInteger(0);
        for (Instance inst : corpus) {
            final int i = nextInstanceID.incValue();//returns old value
            SGraph graph = (SGraph)inst.getInputObjects().get("repgraph");
            List<String> sent = (List)inst.getInputObjects().get("repstring");
            List<String> origSent = (List)inst.getInputObjects().get("string");
            List<String> spanmap = (List)inst.getInputObjects().get("spanmap");
//            if (!alBr.ready()) {
//                break;
//            }
            Set<String> lexNodes = new HashSet<>();
            List<String> als;
//            if (cli.joint) {
//                als =(List)inst.getInputObjects().get("repalignmentp");
//            } else {
            als =(List)inst.getInputObjects().get("repalignment");
//            }
            if (als.size() == 1 && als.get(0).equals("")) {
                //System.err.println("Repaired empty alignment!");
                als = new ArrayList<>();
            }
            String[] alStrings = als.toArray(new String[0]);
            for (String alString : alStrings) {
                Alignment al = Alignment.read(alString, 0);
                lexNodes.addAll(al.lexNodes);
            }
            forkJoinPool.execute(() -> {
                try {
                    TreeAutomaton auto;
//                    if (cli.joint) {
//                        auto = JointAlignmentTrackingAutomaton.create(graph, alStrings, sent, cli.doWrap, cli.maxJointSpread);
//                    } else {
                    auto = AlignmentTrackingAutomaton.create(graph, alStrings, sent.size(), cli.coref, (g -> AMSignatureBuilder.scoreGraphPassiveSpecial(g)));
//                    }
                    auto.processAllRulesBottomUp(null);

                    Tree<String> vit = auto.viterbi();
                    //System.err.println(vit);
                    if (vit != null) {
                        synchronized (success) {
                            success.incValue();
                        }
                        
                        //make POS and literal output, from original sentence, using span map
                        List<TaggedWord> origPosTags = tagger.apply(origSent.stream().map(word -> new Word(word)).collect(Collectors.toList()));
                        origPosTags.stream().forEach(t -> allPosTags.add(t.tag()));
                        List<String> posTags = new ArrayList<>();
                        List<String> literals = new ArrayList<>();
                        for (String spanString : spanmap) {
                            Span span = new Span(spanString);
                            List<String> origWords = new ArrayList<>();
                            for (int l = span.start; l<span.end; l++) {
                                origWords.add(origSent.get(l));
                            }
                            literals.add(origWords.stream().collect(Collectors.joining("_")));
                            posTags.add(origPosTags.get(span.start).tag());
                        }
                        
                        synchronized (extr) {
                            //TODO maybe preserve order of corpus? -- Edit: not necessary, working around that elsewhere
                            List<String> constraints = extr.tree2constraints(auto.viterbi(), lexNodes);
                            extr.writeTrainingdataFromConstraints(constraints, sent);
                            posWriter.write(posTags.stream().collect(Collectors.joining(" "))+"\n");
                            literalWriter.write(literals.stream().collect(Collectors.joining(" "))+"\n");
                        }
                    }
                    if ((i+1) % 500 == 0) {
                        synchronized (success) {
                                System.err.println("Successes: "+success.getValue()+"/"+i);
                        }
                    }
                } catch (IllegalArgumentException ex) {
                    System.err.println(i);
                    System.err.println(graph.toIsiAmrStringWithSources());
                    System.err.println(ex.toString());
                } catch (Exception ex) {
                    System.err.println(i);
                    System.err.println(graph.toIsiAmrStringWithSources());
                    ex.printStackTrace();
                }
            });
            
        }
        
        
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(cli.limit, TimeUnit.MINUTES);
        
        posWriter.close();
        literalWriter.close();
        FileWriter posVocabW = new FileWriter(cli.outPath+"vocabPos.txt");
        for (String tag : allPosTags) {
            posVocabW.write(tag+"\n");
        }
        posVocabW.close();
        extr.writeVocab();
        extr.close();
        
    }
    
    
    
}
