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

//    private static final Pattern COMMENT_PATTERN = Pattern.compile("\\s*#.*");
//    private static final Pattern INTERPRETATION_DECLARATION_PATTERN = Pattern.compile("\\s*#\\s*interpretation\\s+([^: ]+)\\s*:\\s*(\\S+).*", Pattern.CASE_INSENSITIVE);
    private final List<Instance> instances;
    private ChartAttacher charts;
    private boolean isAnnotated;
    private static final boolean DEBUG = false;
    private String source; // explains where this corpus came from

    public Corpus() {
        instances = new ArrayList<>();
        charts = null;
        isAnnotated = false;
    }

    public boolean isAnnotated() {
        return isAnnotated;
    }

    public boolean hasCharts() {
        return charts != null;
    }

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

    public void addInstance(Instance instance) {
        instances.add(instance);

        //is this the intended behaviour??? jonas
        if (instance.getDerivationTree() != null) {
            isAnnotated = true;
        }
    }

    public String getSource() {
        return source;
    }

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

    public static Corpus readCorpus(Reader reader, InterpretedTreeAutomaton irtg) throws IOException, CorpusReadingException {
        Corpus ret = new Corpus();
        boolean annotated = false;

        BufferedReader br = new BufferedReader(reader);
        List<String> interpretationOrder = new ArrayList<String>();
        Map<String, Object> currentInputs = new HashMap<String, Object>();
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
        while (true) {
            line = readNextLine(br, lineNumber);

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

                    if (!irtg.getInterpretations().containsKey(interpretationName)) {
                        throw new CorpusReadingException("Corpus file specified interpretation '" + interpretationName + "', which is not declared in IRTG");
                    }

                    interpretationOrder.add(interpretationName);
                }
            }
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
                line = readNextLine(br, lineNumber);
                continue;
            }

            String current = interpretationOrder.get(currentInterpretationIndex++);

            try {
                Object inputObject = irtg.parseString(current, line);
                currentInputs.put(current, inputObject);
            } catch (Throwable ex) {
                throw new CorpusReadingException("An error occurred while parsing " + reader + ", line " + lineNumber + ", expected interpretation " + current + ": " + ex.getMessage(), ex);
            }

            if (currentInterpretationIndex == interpretationOrder.size()) {
                Instance inst = new Instance();
                inst.setInputObjects(currentInputs);

                if (annotated) {
                    String annoLine = readNextLine(br, lineNumber);

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
                currentInputs = new HashMap<String, Object>();
                currentInterpretationIndex = 0;
            }

            line = readNextLine(br, lineNumber);
        }
    }
    
    /**
     * A version of readCorpus that allows interpretations in the corpus file to not be declared in the grammar.
     * These interpretations are omitted in the returned corpus. Returns an error if none of the interpretations
     * are declared in the grammar.
     * @param reader
     * @param irtg
     * @return
     * @throws IOException
     * @throws CorpusReadingException 
     */
    public static Corpus readCorpusLenient(Reader reader, InterpretedTreeAutomaton irtg) throws IOException, CorpusReadingException {
        Corpus ret = new Corpus();
        boolean annotated = false;

        BufferedReader br = new BufferedReader(reader);
        List<String> interpretationOrder = new ArrayList<String>();
        Map<String, Object> currentInputs = new HashMap<String, Object>();
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
            line = readNextLine(br, lineNumber);

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
                line = readNextLine(br, lineNumber);
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
                    String annoLine = readNextLine(br, lineNumber);

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
                currentInputs = new HashMap<String, Object>();
                currentInterpretationIndex = 0;
            }

            line = readNextLine(br, lineNumber);
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

    public void sort(Comparator<Instance> comparator) {
        instances.sort(comparator);
    }
}
