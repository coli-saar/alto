/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.ImmutableMap;
import de.saar.basic.StringTools;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.TemplateInterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.SetAlgebra;
import de.up.ling.irtg.algebra.SubsetAlgebra;
import static de.up.ling.irtg.algebra.SubsetAlgebra.SEPARATOR;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.TemplateIrtgInputCodec;
import de.up.ling.irtg.util.FirstOrderModel;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author koller
 */
public class SurfaceRealizer {

    private static JCommander jc;
    private static CmdLineParameters param = new CmdLineParameters();

    public static void main(String[] args) throws IOException, Exception {
        jc = JCommander.newBuilder().addObject(param).build();

        try {
            jc.parse(args);
        } catch (ParameterException ex) {
            cmdlineUsage(ex.getMessage());
        }

        if (param.help) {
            cmdlineUsage(null);
        }

        if (param.filenames.size() < 2) {
            cmdlineUsage("Please specify both a grammar and a model.");
        }

        FirstOrderModel model = FirstOrderModel.read(new FileReader(param.filenames.get(1)));
        InterpretedTreeAutomaton irtg = null;

        if (param.filenames.get(0).endsWith(".tirtg")) {
            TemplateInterpretedTreeAutomaton tirtg = new TemplateIrtgInputCodec().read(new FileInputStream(args[0]));
            irtg = tirtg.instantiate(model);

            if (param.debugTirtgFilename != null) {
                Files.write(Paths.get(param.debugTirtgFilename), irtg.toString().getBytes());
            }
        } else {
            irtg = InterpretedTreeAutomaton.read(new FileInputStream(param.filenames.get(0)));
        }

        SubsetAlgebra sem = (SubsetAlgebra) irtg.getInterpretation("sem").getAlgebra();
        Interpretation semI = irtg.getInterpretation("sem");
        
        SetAlgebra ref = (SetAlgebra) irtg.getInterpretation("ref").getAlgebra();
        Interpretation refI = irtg.getInterpretation("ref");
        
        Algebra str = irtg.getInterpretation("string").getAlgebra();

        // put true facts here
        ref.setModel(model);
        List<String> trueAtoms = Util.mapToList(model.getTrueAtoms(), t -> t.toString());
        sem.setOptions(StringTools.join(trueAtoms, SEPARATOR));

        // put inputs here
        Object refInput = ref.parseString(param.ref);
        Object semInput = sem.parseString(param.sem);

        TreeAutomaton<?> chart = null;

        long start = System.nanoTime();
        for (int i = 0; i < param.N; i++) {
            chart = irtg.getAutomaton();
            
            chart = chart.intersect(refI.parse(refInput));
            chart = chart.intersect(semI.parse(semInput));
            
//            chart = irtg.parseInputObjects(ImmutableMap.of("ref", refInput, "sem", semInput));
        }

        System.err.printf("%dx chart construction: %s\n", param.N, Util.formatTimeSince(start));

        if (param.printChartFilename != null) {
            Files.write(Paths.get(param.printChartFilename), chart.toString().getBytes());
        }

        System.out.println();

        int count = 1;
        for (Tree<String> dt : chart.languageIterable()) {
            System.out.printf("[%03d] %s\n", count, str.representAsString(irtg.interpret(dt, "string")));
            if (param.printDerivations) {
                System.out.printf("      <deriv = %s>\n\n", dt);
            }

            count++;
        }
    }

    private static void cmdlineUsage(String errorMessage) {
        if (jc != null) {
            if (errorMessage != null) {
                System.out.println(errorMessage);
            }

            jc.setProgramName("java -cp <alto.jar> de.up.ling.irtg.script.SurfaceRealizer <grammar_filename> <model_filename>");
            jc.usage();
        }

        if (errorMessage != null) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }

    private static class CmdLineParameters {

        @Parameter
        public List<String> filenames = new ArrayList<>();

        @Parameter(names = "--ref", required = true, description = "Set of individual that should be referred to.")
        public String ref = null;

        @Parameter(names = "--sem", required = true, description = "Semantic atoms that should be expressed.")
        public String sem = null;

        @Parameter(names = "-N", description = "Number of times the chart should be computed (for benchmarking).")
        public int N = 1;

        @Parameter(names = "--debug-tirtg", description = "Write instantiated version of the template IRTG to this filename.")
        public String debugTirtgFilename = null;

        @Parameter(names = "--print-chart", description = "Write the chart to this filename.")
        public String printChartFilename = null;

        @Parameter(names = "--print-derivations", description = "Display derivation trees.")
        public boolean printDerivations = false;

        @Parameter(names = "--help", help = true, description = "Prints usage information.")
        private boolean help;
    }

}
