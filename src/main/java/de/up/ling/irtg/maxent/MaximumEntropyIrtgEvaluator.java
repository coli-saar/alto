package de.up.ling.irtg.maxent;

import de.up.ling.irtg.IrtgParser;
import de.up.ling.irtg.ParseException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.AnnotatedCorpus;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Danilo Baumgarten
 */
public class MaximumEntropyIrtgEvaluator {
    private static final Logger log = Logger.getLogger(MaximumEntropyIrtgEvaluator.class.getName());
//    private static final boolean READ_CHARTS = false;
    private MaximumEntropyIrtg maxEntIrtg;

    /**
     * Reads a grammar with trained feature weights and a corpus Then computes
     * charts for the test data and compares the best tree with the proper one
     * of the corpus Optionally reads the charts from file to save time
     *
     * @param args an array of string containing the optional arguments from the
     * call
     * @throws IOException if an error occurs on accessing the files
     * @throws ParseException if parsing the grammar fails
     */
    public static void main(String[] args) throws ParseException, IOException {
        String prefix = (args.length > 0) ? args[0] : "ptb-test";
//        boolean readCharts = (args.length > 1) ? (args[1].equals("readcharts")) : READ_CHARTS;

        log.info("Reading grammar...");
        MaximumEntropyIrtg maxEntIrtg = (MaximumEntropyIrtg) IrtgParser.parse(new FileReader(prefix + "-grammar.irtg"));

        // init evaluator - the first bool is for using the parser of InterpretedTreeAutomaton
        // the second one is for pre-computing all f_i
        MaximumEntropyIrtgEvaluator evaluator = new MaximumEntropyIrtgEvaluator(maxEntIrtg);

        log.info("Reading corpus...");
        AnnotatedCorpus anCo = readPtbCorpus(new FileReader(prefix + "-corpus-testing.txt"));

        /*
         if (readCharts) {
         log.info("Reading charts...");
         maxEntIrtg.readCharts(new FileInputStream(prefix + "-testing.charts"));
         }
         */

        log.info("Reading feature weights...");
        maxEntIrtg.readWeights(new FileReader(prefix + "-weights.props"));

        // start evaluation
        evaluator.evaluate(anCo);
    }


    
    public MaximumEntropyIrtgEvaluator(final MaximumEntropyIrtg maxEntIrtg) {
        this.maxEntIrtg = maxEntIrtg;
    }

    /**
     * Evaluates the trained MaximumEntropyIrtg with the testing data
     *
     * @param corpus the testing data containing sentences and their tree for
     * comparison
     */
    public void evaluate(final AnnotatedCorpus corpus) {
        Iterable<AnnotatedCorpus.Instance> instances = corpus.getInstances();
        int successfullyTested = 0;
        int lexicalErrors = 0;
        int grammarErrors = 0;


        for (AnnotatedCorpus.Instance instance : instances) {
            TreeAutomaton chart = maxEntIrtg.parseMaxent(instance.getInputObjects());

            // the chart might be null, e.g. if a lexical item was not found
            if (chart == null) {
                lexicalErrors++;
                continue;
            }

            // there are not the rules to form a proper tree for this instance
            if (chart.getFinalStates().isEmpty()) {
                grammarErrors++;
                continue;
            }

            // get the best tree (viterbi() of the chart or the homomorphism, if
            // the autodetection has found a TreeAlgebra to use
            Tree tree = chart.viterbi();
            if (tree.equals(instance.getTree())) {
                successfullyTested++;
            }
        }

        // announce the results
        log.log(Level.INFO, "Number of tested instances: {0}", corpus.getNumberOfInstances());
        log.log(Level.INFO, "Number of successfully tested instances: {0}", successfullyTested);
        
        if (lexicalErrors > 0) {
            log.log(Level.INFO, "For {0} instances lexical entries are missing.", lexicalErrors);
        }
        
        if (lexicalErrors > 0) {
            log.log(Level.INFO, "For {0} instances there are nor rules leading to at least one tree for that instance", grammarErrors);
        }
    }

    /**
     * Reads an annotated corpus following the condition of only one
     * interpretation, i.e. with StringAlgebra We don't use the reading of
     * AnnotatedCorpus because there is no grammar. The tree is a representation
     * of TreeAlgebra itself
     *
     * @param reader the reader, e.g. file or string reader, containing the data
     * @return AnnotatedCorpus an instance of the corpus class with all the
     * parsed data
     * @throws IOException if an error occurs on reading data from
     * <tt>reader</tt>
     */
    public static AnnotatedCorpus readPtbCorpus(final Reader reader) throws IOException {
        AnnotatedCorpus ret = new AnnotatedCorpus();
        BufferedReader br = new BufferedReader(reader);
        StringAlgebra strAlgebra = new StringAlgebra();
        Map<String, Object> currentInputs = new HashMap<String, Object>();
        String interpName = new String();
        boolean interpretation = true;
        boolean firstLine = true;

        while (true) {
            // read a line from the input
            String line = br.readLine();

            // return the corpus when reaching EOF
            if (line == null) {
                return ret;
            }

            // skip empty lines
            if (line.equals("")) {
                continue;
            }

            // the first line contains the name of the interpretation
            if (firstLine) {
                interpName = line;
                firstLine = false;
            } else {
                // alternate between reading the representation and the tree
                if (interpretation) {
                    // let StringAlgebra parse the representation
                    Object inputObject = strAlgebra.parseString(line);
                    // and map the interpretation name to the parsed iput
                    currentInputs.put(interpName, inputObject);
                    interpretation = false;
                } else {
                    // parse the tree
                    Tree<String> tree = TreeParser.parse(line);
                    // create a corpus instance
                    AnnotatedCorpus.Instance inst = new AnnotatedCorpus.Instance(tree, currentInputs);
                    // and add the instance to the corpus
//                    ret.getInstances().add(inst);   // XXXXXXXX TODO XXXXXXXXX
                    currentInputs = new HashMap<String, Object>();

                    interpretation = true;
                }
            }
        }
    }
}
