package de.saar.coli.amrtools.datascript;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.codec.AlgebraStringRepresentationOutputCodec;
import de.up.ling.irtg.codec.OutputCodec;
import de.up.ling.irtg.codec.SgraphAmrOutputCodec;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.util.CorpusUtils;

/**
 * I think this class is deprecated --JG.
 * Converts an AMR corpus in Alto format into a corpus in original AMR corpus format (printed to System.out).
 * @author unsure, comments by me --JG
 */
public class JamrDataFromAltoCorpus {

    public static class CommandLineParameters {

        @Parameter(names = {"-c", "--corpus"}, description = "Path to an alto corpus with string and graph interpretations")
        public String pathToCorpus;

        @Parameter(names = {"-trees"}, description = "flag indicating whether the input corpus has an additional tree interpretation")
        public Boolean trees = false;

    }

    private static void usage(String msg, JCommander commander) {
        System.out.println(msg);

        commander.setProgramName("java -cp <path_to_alto_jar_file> de.up.ling.irtg.naive.recombination.JamrDataFromAltoCorpus");
        commander.usage();
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException, CorpusReadingException {
        CommandLineParameters param = new CommandLineParameters();
        JCommander commander = new JCommander(param, args);

        if (param.pathToCorpus == null) {
            usage("no input corpus specified", commander);
            return;
        }

        @SuppressWarnings("rawtypes")
        ArrayList<Pair<String, Algebra>> l = new ArrayList<>(3);

        CorpusUtils.addInt(l, "graph", CorpusUtils.IntTypes.GRAPH);
        CorpusUtils.addInt(l, "string", CorpusUtils.IntTypes.STRING);
        if (param.trees) {
            CorpusUtils.addInt(l, "tree", CorpusUtils.IntTypes.TREE);
        }

        Corpus in = CorpusUtils.retrieveAltoCorpus(param.pathToCorpus, l);

        OutputCodec<SGraph> graphCodec = new SgraphAmrOutputCodec();

        OutputCodec<List<String>> stringCodec = new AlgebraStringRepresentationOutputCodec<List<String>>(l.get(1).getRight());

        for (Instance i : in) {
            System.out.println("# ::snt " + stringCodec.asStringSupplier((List<String>) i.getInputObjects().get("string")).get());
            graphCodec.write((SGraph) i.getInputObjects().get("graph"), System.out);
            System.out.println("\n");
        }

    }

}
