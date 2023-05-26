package de.up.ling.irtg.script;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.BinaryIrtgInputCodec;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.codec.OutputCodec;
import de.up.ling.irtg.codec.ToStringOutputCodec;
import de.up.ling.irtg.util.ConsoleProgressBar;
import de.up.ling.irtg.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Performs EM training on a grammar. The script assumes that the parse charts were already
 * computed and have been stored to *.irtb files. This can be done e.g. with the ParsingEvaluator script.
 */
public class EMTrainer {
    private static JCommander jc;
    public static void main(String[] args) throws IOException {
        // parse command-line parameters
        CmdLineParameters param = new CmdLineParameters();
        jc = new JCommander(param);
        jc.parse(args);

        if (param.help) {
            usage(null);
        }

        if (param.grammarName == null) {
            usage("No grammar specified.");
        }

        if( param.chartDirectory == null ) {
            usage("No chart directory specified.");
        }

        // read IRTG
        InterpretedTreeAutomaton irtg = new IrtgInputCodec().read(new FileInputStream(param.grammarName));
        ChartFromFilesIterable charts = new ChartFromFilesIterable(new File(param.chartDirectory), irtg);

        // perform EM training
        ConsoleProgressBar bar = new ConsoleProgressBar(60, System.out);
        irtg.getAutomaton().trainEM(charts, param.maxIterations, param.threshold, false, charts.size(), bar.createListener());
        bar.finish();

        // write IRTG
        OutputCodec oc = OutputCodec.getOutputCodecByExtension(Util.getFilenameExtension(param.outGrammarName));
        if( oc == null ) {
            // fallback: if no specific output codec is defined, simply use toString
            oc = new ToStringOutputCodec();
        }

        oc.write(irtg, new FileOutputStream(param.outGrammarName));
    }

    private static class ChartFromFilesIterable implements Iterable<TreeAutomaton.LinkedChart> {
        private final File[] chartFiles;
        private final InterpretedTreeAutomaton irtg;

        public ChartFromFilesIterable(File chartDirectory, InterpretedTreeAutomaton irtg) {
            this.chartFiles = chartDirectory.listFiles( (dir, name) -> name.endsWith(".irtb") );
            this.irtg = irtg;
        }

        public int size() {
            return chartFiles.length;
        }

        @Override
        public Iterator<TreeAutomaton.LinkedChart> iterator() {
            return new Iterator<>() {
                private int i = 0;

                @Override
                public boolean hasNext() {
                    return i < chartFiles.length;
                }

                @Override
                public TreeAutomaton.LinkedChart next() throws NoSuchElementException {
                    try {
                        TreeAutomaton<String> chart = new BinaryIrtgInputCodec().read(new FileInputStream(chartFiles[i++])).getAutomaton();
                        TreeAutomaton.LinkedChart ret = new TreeAutomaton.LinkedChart(chart);
                        irtg.collectRules(ret, pairState -> ((String) pairState).split(",")[0]);
                        return ret;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
    }

    private static class CmdLineParameters {
        @Parameter(names = {"--grammar", "-g"}, description = "IRTG whose parameters we want to estimate.")
        public String grammarName = null;

        @Parameter(names = {"--out-grammar", "-o"}, description = "Filename in which the trained IRTG will be stored.")
        public String outGrammarName = null;

        @Parameter(names = "--charts", description = "Directory where the chart files are (see save-charts option in ParsingEvaluator).")
        public String chartDirectory = null;

        @Parameter(names = "--iterations", description = "Maximum number of EM iterations (default 10).")
        public int maxIterations = 10;

        @Parameter(names = "--threshold", description = "Finish EM if the log-likelihood improves by less than this (default 0.001).")
        public double threshold = 0.001;

        @Parameter(names = "--help", help = true, description = "Prints usage information.")
        private boolean help;
    }

    private static void usage(String errorMessage) {
        if (jc != null) {
            if (errorMessage != null) {
                System.out.println(errorMessage);
            }

            jc.setProgramName("java -cp <alto.jar> de.up.ling.irtg.script.ParsingEvaluator <inputfiles>");
            jc.usage();

            if (errorMessage != null) {
                System.exit(1);
            } else {
                System.exit(0);
            }
        }
    }
}
