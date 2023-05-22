package de.up.ling.irtg.script;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.BinaryIrtgInputCodec;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.util.ConsoleProgressBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

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

        ConsoleProgressBar bar = new ConsoleProgressBar(60, System.out);
        int x = 0;
        irtg.getAutomaton().trainEM(charts, 10, 0.001, false, charts.size(), bar.createListener());
        bar.finish();
    }

    private static class ChartFromFilesIterable implements Iterable<TreeAutomaton.LinkedChart> {
        private File chartDirectory;
        private File[] chartFiles;
        private InterpretedTreeAutomaton irtg;

        public ChartFromFilesIterable(File chartDirectory, InterpretedTreeAutomaton irtg) {
            this.chartDirectory = chartDirectory;
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
                        irtg.collectRules(ret, pairState -> {
                            String s = ((String) pairState).split(",")[0];
//                            System.err.printf("%s -> %s\n", pairState, s);
                            return s;
                        });
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

        @Parameter(names = {"--output-codec", "-oc"}, description = "Encode the output IRTG using the output codec with the given name.")
        public String outputCodecName = "toString";

        @Parameter(names = "--charts", description = "Directory where the chart files are (see save-charts option in ParsingEvaluator).")
        public String chartDirectory = null;

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
