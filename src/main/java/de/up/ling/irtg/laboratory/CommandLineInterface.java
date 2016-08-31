/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.IParameterSplitter;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.io.CorpusCache;
import de.up.ling.irtg.io.GrammarCache;
import de.up.ling.irtg.util.GuiUtils;
import de.up.ling.irtg.util.ProgressBarWorker;
import de.up.ling.irtg.util.ProgressListener;
import de.up.ling.tree.ParseException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

/**
 *
 * @author groschwitz
 */
public class CommandLineInterface {

    private static final String DEFAULT_CONFIG_FILENAME = Paths.get(System.getProperty("user.home"), ".alto", "alto.cfg").toString();

    private List<String> parameters = new ArrayList<>();

    @Parameter(names = {"-hostname"}, description = "Name of the host this program is running on")
    private String hostname;

    @Parameter(names = {"-comment"}, description = "Comment on this experiment run", variableArity = true)
    private List<String> comment = new ArrayList<>();

    @Parameter(names = "-taskID", description = "ID of the task (the one to be run) in the database", required = true)
    private int taskID;

    @DynamicParameter(names = {"-V"}) //, description = "Follow with parameters v1 w1 v2 w2 ... where v_i are variable names in the task tree (without '?'), to replace v_i with w_i", variableArity = true, splitter = NullSplitter.class)
    private Map<String, String> varRemapping = new HashMap<>();

    @Parameter(names = {"-data"}, description = "Follow with the IDs of additional data you want to use in your parse", variableArity = true)
    private List<String> additionalData = new ArrayList<>();

    @Parameter(names = {"-threads"}, description = "Number of threads over which the instances should be parallelized", required = false)
    private int numThreads = 1;

    @Parameter(names = {"--verbose", "-v"}, description = "Print detailed measurements and times while parsing. Use multiple -v options to print multiple measurements, or use -v ALL for all measurements.", variableArity = true)
    private List<String> verboseMeasurements = new ArrayList<>();

    @Parameter(names = {"--config"}, description = "Location of configuration file")
    private String configFilename = DEFAULT_CONFIG_FILENAME;

    public boolean isVerbose() {
        return !verboseMeasurements.isEmpty();
    }

    private Map<String, String> getVarRemapper() {
        return varRemapping;
    }

    @Parameter(names = "-local", description = "Local mode, i.e. not uploading results to database")
    private boolean local = false;

    @Parameter(names = {"-show", "-showresults"}, description = "With this flag, results are printed in the console via stderr")
    private boolean showResults = false;

//    @Parameter(names = {"-progressbar", "-pb"}, description="Show progress bar (do not use with -show)")
//    private boolean showProgressBar = false;
    @Parameter(names = {"-h", "-help", "-?"}, description = "displays help if this is the only command", help = true)
    private boolean help = false;

    public static void main(String[] args) throws Exception {
        run(args);
    }

    /**
     * Runs an experiment with command-line style arguments args, and returns
     * the corresponding experimentID. Returns -1 if the process fails.
     *
     * @param args
     * @return
     * @throws java.lang.Exception
     */
    //TODO: better exception handling
    public static int run(String[] args) throws Exception {
        CommandLineInterface cli = new CommandLineInterface();
        JCommander commander = new JCommander(cli);
        try {
            commander.parse(args);
        } catch (com.beust.jcommander.ParameterException ex) {
            System.err.println("An error occured: " + ex.toString());
            System.err.println("\n Available options: ");
            commander.usage();
            return -1;
        }

        if (cli.help) {
            commander.usage();
            return -1;
        }

        Properties props = new Properties();
        try {
            props.load(new FileReader(cli.configFilename));
        } catch (Exception e) {
            System.err.printf("Error while reading config file '%s':\n", cli.configFilename);
            System.err.println(e.getClass());
            System.err.println(e.getMessage());
            System.exit(1);
        }

        Set<String> verboseMeasurementsSet = cli.isVerbose() ? new HashSet<>(cli.verboseMeasurements) : null;

        if (cli.isVerbose()) {
            // sanity checks for verbosity flag
            if (cli.numThreads > 1) {
                System.err.println("Verbose output is only supported when running in a single thread.");
                System.exit(1);
            }
        }

        if (cli.hostname == null) {
            Scanner s = null;
            try {
                s = new Scanner(Runtime.getRuntime().exec("hostname").getInputStream()).useDelimiter("//A");//from http://stackoverflow.com/questions/5711084/java-runtime-getruntime-getting-output-from-executing-a-command-line-program
            } catch (IOException ex) {
                // do nothing here, will check for null below
            }
            if (s != null && s.hasNext()) {
                cli.hostname = s.next();
            } else {
                System.err.println("Could not identify hostname. Please enter the hostname of this machine manually:");
                Scanner reader = new Scanner(System.in);
                cli.hostname = reader.nextLine();
            }
        }

        try {
            Path baseDir = Paths.get(".alto", "cache");

            String altolabBase = props.getProperty("altolab.baseurl");

            if (altolabBase.endsWith("/")) {
                altolabBase = altolabBase + "rest/";
            } else {
                altolabBase = altolabBase + "/rest/";
            }

            URI baseURI = new URI(altolabBase);

            TaskCache tc = new TaskCache(baseDir, baseURI.resolve("task/"));
            UnparsedTask task = tc.get(Integer.toString(cli.taskID));

            GrammarCache gc = new GrammarCache(baseDir, baseURI);
            String gid = String.format("grammar_%d.irtg", task.grammar);
            System.err.printf("Loading grammar #%d (%s) ... ", task.grammar, gc.isInCache(gid) ? "local" : "remote");
            InterpretedTreeAutomaton irtg = gc.get(gid);
            System.err.println("done.");

            CorpusCache cc = new CorpusCache(baseDir, baseURI, irtg);
            String cid = String.format("corpus_%d.txt", task.corpus);
            System.err.printf("Loading corpus #%d (%s) ... ", task.corpus, cc.isInCache(cid) ? "local" : "remote");
            Corpus corpus = cc.get(cid);
            System.err.println("done.");

            AdditionalDataCache ac = new AdditionalDataCache(baseDir, baseURI.resolve("additional_data/"));
            List<String> additionalData = new ArrayList<>();
            for (String ad : cli.additionalData) {
                additionalData.add(ac.get(ad));
            }

            DBLoader dbLoader = cli.local ? null : new DBLoader();

            List<String> unparsedProgram = Arrays.asList(task.getTree().split("\r?\n"));
            Program program = new Program(irtg, additionalData, unparsedProgram, cli.getVarRemapper());
            program.setNumThreads(cli.numThreads);

            System.err.println("Setting up experiment...");
            int _experimentID = -1;
            if (!cli.local) {
                System.err.println("Connecting to database...");
                dbLoader.connect(props);

                _experimentID = dbLoader.uploadExperimentStartData(task.getId(), cli.comment.stream().reduce("", (String t, String u) -> t.concat(" ").concat(u)), cli.hostname, cli.getVarRemapper(), cli.additionalData);
                System.err.println("Experiment ID is " + _experimentID);
            }
            final int experimentID = _experimentID;  // for use from lambda expression

            if (task.getWarmup() > 0) {
                System.err.println("\nRunning " + task.getWarmup() + " warmup instances...");
                withProgressbar(cli.isVerbose(), 60, System.err, listener -> {
                    program.run(corpus, new ResultManager.PrintingManager(), i -> listener.accept(i, task.getWarmup(), i + "/" + task.getWarmup()), task.getWarmup(), true, null);
                    return null;
                });
            }

            System.err.println("\nRunning experiment...");
            if (cli.local) {
                withProgressbar(cli.isVerbose(), 60, System.err, listener -> {
                    program.run(corpus,
                            cli.showResults ? new ResultManager.PrintingManager() : new ResultManager.DummyManager(),
                            i -> listener.accept(i, corpus.getNumberOfInstances(), i + "/" + corpus.getNumberOfInstances()),
                            -1, false, verboseMeasurementsSet);
                    return null;
                });

                System.err.println("Done!");
            } else {
                withProgressbar(cli.isVerbose(), 60, System.err, listener -> {
                    program.run(corpus,
                            new DBResultManager(dbLoader, experimentID, ex -> System.err.println("Error when uploading result to database: " + ex.toString()), cli.showResults),
                            i -> listener.accept(i, corpus.getNumberOfInstances(), i + "/" + corpus.getNumberOfInstances()),
                            -1, false, verboseMeasurementsSet);
                    return null;
                });

                dbLoader.updateExperimentStateFinished(experimentID);
                System.err.println("Done! Experiment ID: " + experimentID);
            }
            return experimentID;
        } catch (SQLException | IOException | ParseException | ParserException ex) {
            System.err.println("Error: " + ex.toString());
            return -1;
        }
    }

    private static final ProgressListener dummyListener = new ProgressListener() {
        @Override
        public void accept(int currentValue, int maxValue, String string) {
        }
    };

    private static <E> void withProgressbar(boolean verbose, int width, PrintStream strm, ProgressBarWorker<E> worker) throws Exception {
        // In verbose mode, a progress bar is not needed -> supply dummy listener.
        if (verbose) {
            worker.compute(dummyListener);
        } else {
            GuiUtils.withConsoleProgressBar(width, strm, worker);
        }
    }

    public static class NullSplitter implements IParameterSplitter {

        @Override
        public List<String> split(String value) {
            return Collections.singletonList(value);
        }
    }

}
