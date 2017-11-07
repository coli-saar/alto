/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.util.MutableInteger;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An (annotated or unannotated) corpus of input objects. You may attach a
 * collection of parse charts for these input objects to the chart using the
 * {@link Charts} class. See the examples to see the exact file format for
 * corpora.<p>
 *
 * Blank lines in a corpus file are ignored. Furthermore, lines that start with
 * the <i>comment prefix</i> are ignored as well. The comment prefix is taken
 * from the non-blank line of the corpus, which needs to be
 * <p>
 * <code> [ccc] IRTG unannotated corpus file, v1.0</code>
 * <p>
 * or<p>
 * <code> [ccc] IRTG annotated corpus file, v1.0</code> respectively. Whatever
 * you specify as <code>[ccc]</code> is used as the comment pattern, and all
 * lines that start with the same pattern are ignored as comments. So if you use
 * "# IRTG annotated ...", then all lines starting with "#" are comments, and if
 * you use "// IRTG unannotated ...", then all lines starting with "//" are
 * comments. You can freely choose your own comment prefix to suit the needs of
 * your corpus.
 *
 * @author koller
 */
public class Corpus implements Iterable<Instance> {

    static String CORPUS_VERSION = "1.0";
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s*");
    private static final Pattern UNANNOTATED_CORPUS_DECLARATION_PATTERN = Pattern.compile("\\s*(\\S+)\\s*IRTG unannotated corpus file, v(\\S+).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANNOTATED_CORPUS_DECLARATION_PATTERN = Pattern.compile("\\s*(\\S+)\\s*IRTG annotated corpus file, v(\\S+).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern INTERPRETATION_DECLARATION_PATTERN = Pattern.compile("\\s*interpretation\\s+([^: ]+)\\s*:\\s*(\\S+).*", Pattern.CASE_INSENSITIVE);

    private final List<Instance> instances;
    private ChartAttacher charts;
    private boolean isAnnotated;
    private static final boolean DEBUG = false;
    private String source; // explains where this corpus came from

    /**
     * This creates a new empty corpus.
     * By default, the new corpus is set to "annotated".
     * If any instance without a derivation tree is added
     * to the corpus, the corpus changes its state
     * to "unannotated".
     * 
     */
    public Corpus() {
        instances = new ArrayList<>();
        charts = null;
        isAnnotated = true;
    }

    /**
     * Returns true if there are gold annotations for each instance.
     * @return 
     */
    public boolean isAnnotated() {
        return isAnnotated;
    }

    /**
     * Returns true if the instances in this corpus are associated with parse
     * charts.
     * @return 
     */
    public boolean hasCharts() {
        return charts != null;
    }

    /**
     * This attaches parse charts to the instances of the corpus.
     * 
     * Different ChartAttachers may have different strategies for this. A
     * OnTheFlyCharts attacher will compute the parse charts when they are requested.
     * 
     * @param charts 
     */
    public void attachCharts(ChartAttacher charts) {
        this.charts = charts;
    }

    /**
     * Reads charts from a file and attaches them to this corpus.
     *
     * @param filename
     * @throws IOException
     */
    public void attachCharts(String filename) throws IOException {
        attachCharts(new Charts(new FileInputStreamSupplier(new File(filename))));
    }

    /**
     * Returns the number of instances contained in this corpus.
     * @return 
     */
    public int getNumberOfInstances() {
        return instances.size();
    }

    @Override
    public Iterator<Instance> iterator() {
        if (hasCharts()) {
            return this.charts.attach(this.instances.iterator());
        } else {
            return this.instances.iterator();
        }
    }

    /**
     * Adds a new instance to the corpus.
     * 
     * If the instance is not annotated, it will change set the whole corpus to
     * being unAnnotated.
     * @param instance 
     */
    public void addInstance(Instance instance) {
        instances.add(instance);
        
        if( instance.getDerivationTree() == null ) {
            isAnnotated = false;
        }
        
        // NB Changed this in April 2017, check if it caused
        // any problems. - AK
    }

    /**
     * Returns a string describing the source of the corpus, if this was passed
     * to the corpus at some point.
     * @return 
     */
    public String getSource() {
        return source;
    }

    /**
     * This sets a value for the source of the corpus.
     * @param source 
     */
    public void setSource(String source) {
        this.source = source;
    }

//    public void writeCorpus(Writer writer, InterpretedTreeAutomaton irtg) throws IOException {
//        CorpusWriter cw = new CorpusWriter(irtg, null, writer);
//        cw.setAnnotated(isAnnotated);
//
//        for (Instance inst : instances) {
//            cw.writeInstance(inst);
//        }
//    }

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

    /**
     * Reads a corpus from a string format available via a reader. Loads all interpretations
     * shared by corpus and grammar. There must be at least one such interpretation,
     * or an error is thrown.
     * @param reader
     * @param irtg
     * @return
     * @throws IOException
     * @throws CorpusReadingException 
     */
    public static Corpus readCorpus(Reader reader, InterpretedTreeAutomaton irtg) throws IOException, CorpusReadingException {
        return readCorpusWrapper(reader, irtg, br -> lineNumber -> interpIndex -> {
            try {
                return readNextLine(br, lineNumber);
            } catch (IOException ex) {
                throw new RuntimeException(ex);//TODO: this is not pretty. Check e.g. http://www.baeldung.com/java-lambda-exceptions section 3.1 for a cleaner solution?
            }
        });
    }
    
    /**
     * Reads a corpus from a string format available via a reader, assuming strict
     * ordering of lines to allow empty entries to be read properly.
     * The assumed format is: header, one empty line, instance lines, one empty line,
     * instance lines, one empty line, instance lines, and so on. This matches the
     * default output format of the CorpusWriter (last checked 11/2017 -- JG).
     * Loads all interpretations
     * shared by corpus and grammar. There must be at least one such interpretation,
     * or an error is thrown.
     * @param reader
     * @param irtg
     * @return
     * @throws IOException
     * @throws CorpusReadingException 
     */
    public static Corpus readCorpusWithStrictFormatting(Reader reader, InterpretedTreeAutomaton irtg) throws IOException, CorpusReadingException {
        return readCorpusWrapper(reader, irtg, br -> lineNumber -> interpIndex -> {
            try {
                return readNextLineStrict(br, lineNumber, interpIndex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);//TODO: this is not pretty. Check e.g. http://www.baeldung.com/java-lambda-exceptions section 3.1 for a cleaner solution?
            }
        });
    }
    
    private static Corpus readCorpusWrapper(Reader reader, InterpretedTreeAutomaton irtg,
            Function<BufferedReader, Function<MutableInteger, Function<Integer, String>>> readLine) throws IOException, CorpusReadingException {
        Corpus ret = new Corpus();
        boolean annotated = false;

        BufferedReader br = new BufferedReader(reader);
        List<String> interpretationOrder = new ArrayList<>();
        Map<String, Object> currentInputs = new HashMap<>();
        int currentInterpretationIndex = 0;
        MutableInteger lineNumber = new MutableInteger(0);
        String commentPrefix = null;

        // first non-blank line is declaration of annotated or unannotated corpus
        String line = readNextLine(br, lineNumber);

        Matcher unannoMatcher = UNANNOTATED_CORPUS_DECLARATION_PATTERN.matcher(line);
        if (unannoMatcher.matches()) {
            annotated = false;
            commentPrefix = unannoMatcher.group(1);
            if (!CORPUS_VERSION.equals(unannoMatcher.group(2))) {
                throw new CorpusReadingException("Expecting corpus file format version " + CORPUS_VERSION + ", but file is version " + unannoMatcher.group(1));
            }
        } else {
            Matcher annoMatcher = ANNOTATED_CORPUS_DECLARATION_PATTERN.matcher(line);
            if (annoMatcher.matches()) {
                annotated = true;
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
        boolean foundInterpretation = false;
        Set<String> ungrammaticalInterpretations = new HashSet<>();
        while (true) {
            line = readLine.apply(br).apply(lineNumber).apply(-1);//do not know interpretations yet, pass -1 as interpretation counter

            if (line == null) {
                return ret;
            }

            String stripped = readAsComment(line, commentPrefix);
            if (stripped == null) {
                // first non-comment, non-empty, non-metadata-declaring line => finished reading metadata
                break;
            } else {
                Matcher interpretationMatcher = INTERPRETATION_DECLARATION_PATTERN.matcher(stripped);
                if (interpretationMatcher.matches()) {
                    String interpretationName = interpretationMatcher.group(1);

                    if (irtg.getInterpretations().containsKey(interpretationName)) {
                        foundInterpretation = true;
                    } else {
                        ungrammaticalInterpretations.add(interpretationName);
                    }

                    interpretationOrder.add(interpretationName);
                }
            }
        }

        if (!foundInterpretation) {
            throw new CorpusReadingException("Corpus and grammar share no common interpretation");
        }
        
        if (interpretationOrder.isEmpty()) {
            throw new CorpusReadingException("Corpus defined no interpretations");
        }

        ret.isAnnotated = annotated;

        // read actual corpus
        while (true) {
            if (line == null) {
                return ret;
            }

            String stripped = readAsComment(line, commentPrefix);
            if (stripped != null) {
                line = readLine.apply(br).apply(lineNumber).apply(currentInterpretationIndex);
                continue;
            }

            String current = interpretationOrder.get(currentInterpretationIndex++);

            if (!ungrammaticalInterpretations.contains(current)) {
                try {
                    Object inputObject = irtg.parseString(current, line);
                    currentInputs.put(current, inputObject);
                } catch (Throwable ex) {
                    throw new CorpusReadingException("An error occurred while parsing " + reader + ", line " + lineNumber + ", expected interpretation " + current + ": " + ex.getMessage(), ex);
                }
            }

            if (currentInterpretationIndex == interpretationOrder.size()) {
                Instance inst = new Instance();
                inst.setInputObjects(currentInputs);

                if (annotated) {
                    String annoLine = readLine.apply(br).apply(lineNumber).apply(currentInterpretationIndex);

                    if (annoLine == null) {
                        throw new CorpusReadingException("Expected a derivation tree in line " + lineNumber);
                    }

                    try {
                        Tree<String> derivationTree = TreeParser.parse(annoLine);
                        inst.setDerivationTree(irtg.getAutomaton().getSignature().addAllSymbols(derivationTree));
                    } catch (Throwable ex) {  // TreeParser#parse can throw weird Errors, hence Throwable
                        throw new CorpusReadingException("An error occurred while reading the derivation tree in line " + lineNumber + ": " + ex.getMessage(), ex);
                    }
                }

                ret.instances.add(inst);
                currentInputs = new HashMap<>();
                currentInterpretationIndex = 0;
            }

            line = readLine.apply(br).apply(lineNumber).apply(currentInterpretationIndex);
        }
    }
    
    
    private static String readNextLine(BufferedReader br, MutableInteger lineNumber) throws IOException {
        String ret = null;

        do {
            ret = br.readLine();
            lineNumber.incValue();
        } while (ret != null && WHITESPACE_PATTERN.matcher(ret).matches());

        if (DEBUG) {
            System.err.println("**read line: " + ret);
        }

        return ret;
    }
    
    private static String readNextLineStrict(BufferedReader br, MutableInteger lineNumber, int currentInterpretationIndex) throws IOException {
        String ret = br.readLine();
        lineNumber.incValue();
        
        if (currentInterpretationIndex == 0) {
            //skip exactly one whitespace line in between instance (triggers after(!) each instance)
            if (ret != null && !WHITESPACE_PATTERN.matcher(ret).matches()) {
                System.err.println("***WARNING*** found nonempty line in between instances!");
                System.err.println(ret);
            }
            ret = br.readLine();
            lineNumber.incValue();
        } else if (currentInterpretationIndex < 0 && ret != null && WHITESPACE_PATTERN.matcher(ret).matches()) {
            //skip exactly one whitespace line after header
            ret = br.readLine();
            lineNumber.incValue();
        }

        if (DEBUG) {
            System.err.println("**read line: " + ret);
        }

        return ret;
    }

    /**
     * Re-orders the instances in this corpus according to the order induced by
     * the comparator.
     * @param comparator 
     */
    public void sort(Comparator<Instance> comparator) {
        instances.sort(comparator);
    }
}
