/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.IParameterSplitter;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.util.GuiUtils;
import de.up.ling.tree.ParseException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 *
 * @author groschwitz
 */
public class CommandLineInterface {

    private List<String> parameters = new ArrayList<>();

    @Parameter(names = {"-hostname"}, description = "Name of the host this program is running on")
    private String hostname;

    @Parameter(names = {"-comment"}, description = "Comment on this experiment run", variableArity = true)
    private List<String> comment = new ArrayList<>();

    @Parameter(names = "-taskID", description = "ID of the task (the one to be run) in the database", required = true)
    private int taskID;

    @Parameter(names = {"-varRemapping", "-v"}, description = "Follow with parameters v1 w1 v2 w2 ... where v_i are variable names in the task tree (without '?'), to replace v_i with w_i", variableArity = true, splitter = NullSplitter.class)
    private List<String> varRemapping = new ArrayList<>();

    @Parameter(names = {"-data"}, description = "Follow with the IDs of additional data you want to use in your parse", variableArity = true)
    private List<String> additionalData = new ArrayList<>();
    
    @Parameter(names = {"-threads"}, description = "Number of threads over which the instances should be parallelized", required = false)
    private int numThreads = 1;
    
    private Map<String, String> getVarRemapper() {
        Map<String, String> ret = new HashMap<>();
        if (varRemapping.size() % 2 != 0) {
            throw new com.beust.jcommander.ParameterException("ERROR: uneven amount of parameters for -varRemapping/-v");
        }
        for (int i = 0; i < varRemapping.size(); i += 2) {
            ret.put(varRemapping.get(i), varRemapping.get(i + 1));
        }
        return ret;
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
            System.err.println("Connecting to database...");
            final DBLoader dbLoader = new DBLoader();
            dbLoader.connect();
            System.err.println("Getting task reference...");
            DBLoader.TaskReference taskRef = dbLoader.getTaskReferenceByID(Integer.valueOf(cli.taskID));
            System.err.println("Loading grammar...");
            DBLoader.GrammarReference grammarRef = dbLoader.getGrammarRefForTask(taskRef);
            InterpretedTreeAutomaton irtg = dbLoader.loadGrammarFromDB(grammarRef);
            System.err.println("Loading task...");
            Task task;
            try {
                task = dbLoader.loadTaskFromDBWithIrtg(taskRef, irtg, cli.getVarRemapper(), cli.additionalData);
            } catch (VariableNotDefinedException ex) {
                System.err.println("ERROR: " + ex.toString());
                return -1;
            }
            
            task.setNumThreads(cli.numThreads);
            
            System.err.println("Loading corpus...");
            Corpus corpus = dbLoader.loadCorpusFromDB(task.getCorpus(), irtg);
            
            System.err.println("Setting up experiment...");
            int _experimentID = -1;
            if (!cli.local) {
                _experimentID = dbLoader.uploadExperimentStartData(taskRef, cli.comment.stream().reduce("", (String t, String u) -> t.concat(" ").concat(u)), cli.hostname, cli.getVarRemapper(), cli.additionalData);
                System.err.println("Experiment ID is " + _experimentID);
            }
            final int experimentID = _experimentID;  // for use from lambda expression
            
            System.err.println("Running " + task.getWarmup() + " warmup instances...");
            
            GuiUtils.withConsoleProgressBar(60, System.err, listener -> {
                task.getProgram().run(corpus, new ResultManager.PrintingManager(), i -> listener.accept(i, task.getWarmup(), i + "/" + task.getWarmup()), task.getWarmup());
                return null;
            });
            
//            task.getProgram().run(corpus, new ResultManager.PrintingManager(),
//                    i -> {
//                        System.err.println(i + "/" + task.getWarmup());
//                    }, task.getWarmup());

            System.err.println("Running experiment...");
            if (cli.local) {
                GuiUtils.withConsoleProgressBar(60, System.err, listener -> {
                    task.getProgram().run(corpus, 
                            cli.showResults ? new ResultManager.PrintingManager() : new ResultManager.DummyManager(), 
                            i -> listener.accept(i, corpus.getNumberOfInstances(), i + "/" + corpus.getNumberOfInstances()), 
                            -1);
                    return null;
                });
                
//                task.getProgram().run(corpus, cli.showResults ? new ResultManager.PrintingManager() : new ResultManager.DummyManager(),
//                        i -> {
//                            System.err.println(i + "/" + corpus.getNumberOfInstances());
//                        }, -1);
                
                System.err.println("Done!");
            } else {
                GuiUtils.withConsoleProgressBar(60, System.err, listener -> {
                    task.getProgram().run(corpus, 
                            new DBResultManager(dbLoader, experimentID, ex -> System.err.println("Error when uploading result to database: " + ex.toString()), cli.showResults),
                            i -> listener.accept(i, corpus.getNumberOfInstances(), i + "/" + corpus.getNumberOfInstances()),
                            -1);
                    return null;
                });
                
//                task.getProgram().run(corpus, new DBResultManager(dbLoader, experimentID,
//                        ex -> System.err.println("Error when uploading result to database: " + ex.toString()), cli.showResults),
//                        i -> {
//                            System.err.println(i + "/" + corpus.getNumberOfInstances());
//                        }, -1);
                
                dbLoader.updateExperimentStateFinished(experimentID);
                System.err.println("Done! Experiment ID: " + experimentID);
            }
            return experimentID;
        } catch (SQLException | IOException | ParseException | ParserException ex) {
            System.err.println("Error: " + ex.toString());
            return -1;
        }
    }
    
    public static class NullSplitter implements IParameterSplitter {
        @Override
        public List<String> split(String value) {
          return Collections.singletonList(value);
        }
    }

}
