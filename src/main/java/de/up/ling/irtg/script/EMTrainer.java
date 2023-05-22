package de.up.ling.irtg.script;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.BinaryIrtgInputCodec;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.util.ConsoleProgressBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        // read charts
        // NB: This is memory-wasteful, but TreeAutomaton#trainEM insists on having everything in memory.
        // If this becomes limiting, we might consider generalizing trainEM.
        List<TreeAutomaton<?>> charts = new ArrayList<>();
        List<Map<Rule, Rule>> intersectedRuleToOriginalRule = new ArrayList<>();
        ListMultimap<Rule, Rule> originalRuleToIntersectedRules = ArrayListMultimap.create();

        File chartDirectory = new File(param.chartDirectory);
        ConsoleProgressBar bar = new ConsoleProgressBar(60, System.out);
        File[] chartFiles = chartDirectory.listFiles( (dir, name) -> name.endsWith(".irtb") );
        int x = 0;

        for( File chartFile : chartFiles ) {
            bar.update(x, chartFiles.length, String.format("[%s] Reading chart", chartFile.getName()));
            TreeAutomaton<String> chart = new BinaryIrtgInputCodec().read(new FileInputStream(chartFile)).getAutomaton();
            charts.add(chart);
            irtg.collectRules(chart, intersectedRuleToOriginalRule, originalRuleToIntersectedRules, pairState -> ((String) pairState).split(",")[0] );
        }

        bar.finish();

        bar = new ConsoleProgressBar(60, System.out);
        irtg.getAutomaton().trainEM(charts, intersectedRuleToOriginalRule, originalRuleToIntersectedRules, 10, 0.001, false, bar.createListener());
        bar.finish();
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
