package de.up.ling.irtg.script;

import de.saar.basic.StringTools;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.TreeAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.PtbTreeInputCodec;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusWriter;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import de.up.ling.tree.TreeVisitor;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/*
 * Note: This class is outdated, but I keep it around (as non-public)
 * because it contains much more fine-grained functionality than the
 * CorpusConverter, for sorting and filtering and such. This functionality
 * should be merged into CC and generalized. - AK
 */


/**
 * This reads a file of the Penn Treebank and converts it into a corpus that is
 * suitable for working with the IRTG tool, along with an IRTG grammar read off
 * from the treebank.<p>
 *
 * Usage: java PTBConverter &lt;wsjxxx.mrg&gt; [&lt;sortByInterpretation&gt;]
 * <p>
 *
 * The resulting IRTG will have two interpretations, "i" and "ptb". "i" is a
 * string interpretation, see {@link StringAlgebra}; "ptb" is an interpretation
 * for the original parse trees, using a {@link PtbTreeAlgebra}. You may
 * optionally specify that the IRTG corpus is sorted in ascending order by the
 * size of the input objects of one of these interpretations, by passing the
 * interpretation as the second argument on the command line.
 *
 * @author Danilo Baumgarten
 */
@Deprecated
class PTBConverter {

    private static final Logger log = Logger.getLogger(PTBConverter.class.getName());
    private static final int TOKEN_SIZE = 15;
    private static final boolean CONVERT = false;
    public static final boolean PARENT_ANNOTATION = true;
    private Map<String, Integer> ruleMap;             // mapping of string representation and name of a rule, i.e. "S/NP/VP" -> "r1"; rule name is encoded according to RTG signature
    private List<Tree<String>> ptbTrees;             // list of PTB-trees
    private Corpus corpus;                           // annotated corpus
    private InterpretedTreeAutomaton irtg;           // the automaton storing the grammar
    private Homomorphism hStr;                       // homomorphism for StringAlgebra
    private Homomorphism hPtb;                       // homomorphism for PtbTreeAlgebra
    private int maxTerminalsPerSentence;             // max. sentence length (in words)

    /**
     * Reads a PTB-file (preset filename or via argument) and depending on
     * settings converts PTB- into Irtg-trees and serialize them as grammar and
     * corpus or serialize them as corpus only
     *
     * @param args an array of string containing the optional arguments from the
     * call
     * @throws IOException if an error on accessing the files occurs
     */
    public static void main(final String[] args) throws IOException {
        String filename = (args.length > 0) ? args[0] : "examples/ptb-test.mrg";
        String sortByInterpretation = (args.length > 1) ? args[1] : null;
        int tokenSize = (args.length > 2) ? Integer.valueOf(args[2]) : TOKEN_SIZE;
        boolean convert = (args.length > 3) ? (!args[3].equals("noconversion")) : CONVERT;

        List<String> interpretationOrder = new ArrayList<String>();
        interpretationOrder.add("i");
        interpretationOrder.add("ptb");

        if ((sortByInterpretation != null) && !interpretationOrder.contains(sortByInterpretation)) {
            System.err.println("Ignore sorting request on interpretation '" + sortByInterpretation + "'.");
            System.err.println("Permitted interpretations: " + interpretationOrder);
            sortByInterpretation = null;
        }

        String prefix = getFilenamePrefix(filename);
        String corpusFilename = prefix + ((convert) ? "-corpus-training.txt" : "-corpus-testing.txt");

        PTBConverter lc = new PTBConverter(tokenSize);

        log.info("Reading PTB data...");
        lc.read(getReaderForFilename(filename));

        lc.initGrammar();

        log.info("Converting PTB trees...");
        lc.convert(sortByInterpretation);
        log.log(Level.INFO, "Converted rules: {0}", String.valueOf(lc.ruleMap.size()));

        log.info("Writing grammar...");
        lc.writeGrammar(new FileWriter(prefix + "-grammar.irtg"));

        log.info("Writing corpus...");
        FileWriter fw = new FileWriter(corpusFilename);
        CorpusWriter cw = new CorpusWriter(lc.irtg, null, fw);
        lc.corpus.forEach(cw);
        fw.close();

        log.info("Done.");
    }

    /*
     * Returns the reader appropriate for the file given by <tt>filename</tt>
     * if the file is an archive all the zipped data will be streamed
     * an ordinary file reader is used else
     * 
     * @param filename the name of the file
     * @return Reader the reader to handle the file content
     * @throws FileNotFoundException if the file could not be found
     * @throws IOException in case of an error on reading the zipped stream
     */
    private static Reader getReaderForFilename(final String filename) throws FileNotFoundException, IOException {
        if (filename.endsWith(".gz")) {
            return new InputStreamReader(new GZIPInputStream(new FileInputStream(filename)));
        } else {
            return new FileReader(filename);
        }
    }

    /*
     * Gets the filename without ending to use it as prefix for various output files
     * 
     * @param filenameWithPath the whole filename
     * @return String the filename without ending
     */
    private static String getFilenamePrefix(final String filenameWithPath) {
        String filename = new File(filenameWithPath).getName();

        if (filename.endsWith(".mrg")) {
            return filename.substring(0, filename.length() - 4);
        } else if (filename.endsWith(".mrg.gz")) {
            return filename.substring(0, filename.length() - 7);
        } else {
            return filename;
        }
    }

    /**
     * Constructor
     *
     * @param maxTerminals the max. number of token/words in an instance to use
     * for conversion
     */
    public PTBConverter(final int maxTerminals) {
        maxTerminalsPerSentence = maxTerminals;

        // init members
        corpus = new Corpus();
        ptbTrees = new ArrayList<Tree<String>>();
        ruleMap = new HashMap<String, Integer>();

        log.setLevel(Level.ALL);
    }

    /**
     * Creates a new MaximumEntropyIrtg with interpretations for strings and
     * PTB-trees
     */
    public void initGrammar() {
        // create IRTG 
        irtg = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton());

        // set up string interpretation
        StringAlgebra stringAlgebra = new StringAlgebra();
        hStr = new Homomorphism(irtg.getAutomaton().getSignature(), stringAlgebra.getSignature());
        irtg.addInterpretation("i", new Interpretation(stringAlgebra, hStr));

        // set up PTB-tree interpretation
        PtbTreeAlgebra ptbAlgebra = new PtbTreeAlgebra(PARENT_ANNOTATION);
        hPtb = new Homomorphism(irtg.getAutomaton().getSignature(), ptbAlgebra.getSignature());
        irtg.addInterpretation("ptb", new Interpretation(ptbAlgebra, hPtb));
    }

    /**
     * Reads the list of PTB-trees given by <tt>reader</tt>
     *
     * @param reader the reader containing the data
     * @throws IOException if an error occurs on reading the data
     */
    private void read(final Reader reader) throws IOException {
        PtbTreeAlgebra pta = new PtbTreeAlgebra(PARENT_ANNOTATION);
        Tree<String> ptbTree = null;

        do {
            ptbTree = pta.parseFromReader(reader);

            // check if there's a resulting tree and the number of token/words
            if ((ptbTree != null) && (pta.getNumWords() <= maxTerminalsPerSentence)) {
                // store the parsed tree
                ptbTrees.add(ptbTree);
            }
            // break if the parsed tree is null
            // that means whether we read everything or something went wrong
            // anyway there's no use in continuing
        } while (ptbTree != null);
    }

    /**
     * Converts all PTB-trees to IRTG-trees and collects the rules Also
     * binarizes them, extract the rules to create a grammar and setup the
     * interpretations
     */
    private void convert(final String sortByInterpretation) {
        ConcreteTreeAutomaton c = (ConcreteTreeAutomaton) irtg.getAutomaton();
        PtbTreeAlgebra pta = new PtbTreeAlgebra(PARENT_ANNOTATION);
        List<Instance> instances = new ArrayList<Instance>();

        for (Tree<String> ptbTree : ptbTrees) {
            Map<String, Object> inputObjectsMap = new HashMap<String, Object>();
            inputObjectsMap.put("ptb", ptbTree);

            // extract grammar rules and remember derivation tree
            Tree<Integer> derivationTree = extractRules(pta.binarizeAndRelabel(ptbTree));
            c.addFinalState(c.addState(ptbTree.getLabel()));

            inputObjectsMap.put("i", StringTools.join(ptbTree.getLeafLabels(), " "));

            // create instance and schedule it for addition to corpus
            Instance inst = new Instance();
            inst.setInputObjects(inputObjectsMap);
            inst.setDerivationTree(derivationTree);

            // schedule the instance for addition to corpus
            instances.add(inst);
        }

        // if requested, sort instances by length in interpretation sortByInterpretation
        if (sortByInterpretation != null) {
            Collections.sort(instances, new Comparator<Instance>() {
                public int compare(Instance t, Instance t1) {
                    return t.getInputObjects().get(sortByInterpretation).toString().length() - t1.getInputObjects().get(sortByInterpretation).toString().length();
                }
            });
        }

        // add instance to corpus
        for (Instance inst : instances) {
            corpus.addInstance(inst);
        }
    }

    /**
     * Extracts all rules from the given PTB-tree
     *
     * @param tree the PTB-tree
     */
    private Tree<Integer> extractRules(final Tree<String> tree) {
        final ConcreteTreeAutomaton c = (ConcreteTreeAutomaton) irtg.getAutomaton();

        return tree.dfs(new TreeVisitor<String, Void, Tree<Integer>>() {
            @Override
            public Tree<Integer> combine(Tree<String> node, List<Tree<Integer>> childrenValues) {
                if (childrenValues.isEmpty()) {
                    return null;
                }

                String ruleString = nodeToRuleString(node);       // string representation for the rule's states, e.g. "S/NP/VP"
                int ruleNameId = 0;                               // label ID to be used in the rule and the derivation tree

                // obtain label ID for rule and derivation tree
                boolean ruleWasKnown = ruleMap.containsKey(ruleString);
                if (ruleWasKnown) {
                    ruleNameId = ruleMap.get(ruleString);
                } else {
                    ruleNameId = hStr.getSourceSignature().addSymbol("r" + String.valueOf(ruleMap.size() + 1), childrenValues.size());
                    ruleMap.put(ruleString, ruleNameId);
                }

                List<Tree<HomomorphismSymbol>> ptbChildren = new ArrayList<Tree<HomomorphismSymbol>>();  // children of hom RHS in PTB interpretation
                List<Tree<Integer>> derivTreeChildren = new ArrayList<Tree<Integer>>();                  // children of node in derivation tree
                List<String> childStates = new ArrayList<String>();                                      // child states in the grammar rule

                if (childrenValues.get(0) == null) {
                    // (only) child was a leaf => nullary terminal symbol
                    assert childrenValues.size() == 1;

                    // string interpretation: one-node hom RHS labeled with the word at the leaf below me
                    String label = node.getChildren().get(0).getLabel();
                    Tree<HomomorphismSymbol> th = Tree.create(HomomorphismSymbol.createConstant(label, hStr.getTargetSignature(), 0));
                    hStr.add(ruleNameId, th);

                    // PTB interpretation: add a single child, labeled with the word at the leaf below me
                    ptbChildren.add(th);

                    // add nothing to derivation tree or to child states of grammar rule
                } else {
                    // binary node => collect child states

                    for (int i = 0; i < childrenValues.size(); i++) {
                        // grammar rule: add one child for each child of this node
                        String label = node.getChildren().get(i).getLabel();
                        childStates.add(label);

                        // derivation tree: add one child for each child of this node
                        derivTreeChildren.add(childrenValues.get(i));

                        // PTB interpretation: add one variable-labeled child for each child of this node
                        String ptbLabel = "?" + String.valueOf(i + 1);
                        ptbChildren.add(Tree.create(HomomorphismSymbol.createVariable(ptbLabel)));
                    }

                    // string interpretation: convert list of variables for children into tree of concatenation operations
                    Tree<HomomorphismSymbol> strInterp = computeInterpretation(1, childrenValues.size(), hStr.getTargetSignature());
                    hStr.add(ruleNameId, strInterp);
                }

                if (!ruleWasKnown) {
                    // PTB interpretation: add entry to homomorphism
                    hPtb.add(ruleNameId, Tree.create(HomomorphismSymbol.createConstant(node.getLabel(), hPtb.getTargetSignature(), 0), ptbChildren));

                    // grammar: add rule
                    c.addRule(c.createRule(node.getLabel(), hStr.getSourceSignature().resolveSymbolId(ruleNameId), childStates));
                }

                // if we're at the root, add parent as final state
                if (node == tree) {
                    c.addFinalState(c.getIdForState(node.getLabel()));
                }

                return Tree.create(ruleNameId, derivTreeChildren);
            }
        });
    }

    /**
     * Creates a string representation for the rule at the PTB-node
     *
     * @param node the PTB-(sub)tree
     * @return String the rule representation
     */
    private String nodeToRuleString(final Tree<String> node) {
        StringBuilder ret = new StringBuilder();

        // the representation starts with the left side of the rule
        ret.append(node.getLabel());

        // and adds all elements of the right side all delimited by '/'
        for (Tree<String> n : node.getChildren()) {
            ret.append("/");
            ret.append(n.getLabel());
        }
        return ret.toString();
    }

    /**
     * Computes the (nested) interpretation for rules containing no terminal
     * symbol
     *
     * @param start the number of the first element of the interpretation, e.g.,
     * 1
     * @param max the number of elements of the interpretation
     * @return String the interpretation
     */
    private Tree<HomomorphismSymbol> computeInterpretation(final int start, final int max, Signature signature) {
        // if there is only one element create the tree and return it
        if (max == 1) {
            return Tree.create(HomomorphismSymbol.createVariable("?1"));
        }

        // if (max < start) becomes true this functions is missused
        if (max < start) {
            return null;
        }

        List<Tree<HomomorphismSymbol>> strChildren = new ArrayList<Tree<HomomorphismSymbol>>();

        // create a variable name
        String startLabel = "?" + String.valueOf(start);
        // and add the variable to the list
        strChildren.add(Tree.create(HomomorphismSymbol.createVariable(startLabel)));

        // proceed with the remaining elements
        if ((start + 1) == max) { // last element to compute
            // create variable name
            String maxLabel = "?" + String.valueOf(max);
            // and add the variable to the list
            strChildren.add(Tree.create(HomomorphismSymbol.createVariable(maxLabel)));
        } else { // more than one remaining element
            // recursive step
            strChildren.add(computeInterpretation(start + 1, max, signature));
        }

        return Tree.create(HomomorphismSymbol.createConstant(StringAlgebra.CONCAT, signature, strChildren.size()), strChildren);
    }

    /**
     * Writes the grammar rules to a writer, e.g., string or file
     *
     * @param writer the writer to store the data into
     * @throws IOException if the writer cannot store the data properly
     */
    private void writeGrammar(final Writer writer) throws IOException {
        writer.write(irtg.toString());
        writer.close();
    }

    /**
     * A tree algebra for trees in the format of the Penn Treebank. The domain
     * of this algebra is the set of all unranked trees, i.e. different nodes
     * with the same label may have different numbers of children.
     * <p>
     *
     * This class is now superseded by {@link PtbTreeInputCodec}. I am keeping
     * it around for now because it is used by PTBConverter in the scripts
     * package. Once the functionality of that class is replicated by more
     * general tools, we can get rid of it.
     *
     * @author koller
     * @author Danilo Baumgarten
     */
    private static class PtbTreeAlgebra extends TreeAlgebra {

        private static final Logger log = Logger.getLogger(PtbTreeAlgebra.class.getName());
        private static final String START_SEQUENCE = "( ";
        private static final Pattern STRIP_PATTERN = Pattern.compile("([^-=]+)([-=])(.+)");
        private static final Pattern LABELFX_PATTERN = Pattern.compile("(\\D+)(\\d+)(.*)");
        private static final Pattern CONCAT_PATTERN = Pattern.compile("(.+)(\\^)(.+)");
        public static final String LABEL_PREFIX = "ART-";

        private int numWords;
        private boolean useParentAnnotation;

        /**
         * Constructor
         *
         * @param useParentAnnotation if true non-terminal symbols get their
         * parent symbol appended
         */
        public PtbTreeAlgebra() {
            this(true);
        }

        /**
         * Constructor
         *
         * @param useParentAnnotation if true non-terminal symbols get their
         * parent symbol appended
         */
        public PtbTreeAlgebra(boolean useParentAnnotation) {
            this.useParentAnnotation = useParentAnnotation;
        }

        /**
         * Getter for LABEL_PREFIX
         *
         * @return String the label prefix
         */
        public static String getArtificialLabelPrefix() {
            return LABEL_PREFIX;
        }

        /**
         * Returns the number of words/token from last parsing
         *
         * @return int number of words/token
         */
        public int getNumWords() {
            return numWords;
        }

        /**
         * Evaluates the given tree i.e. removes the effects of the
         * transformation process (artificial nodes, relabeling, ...)
         *
         * @param tree the tree, e.g., generated from chart
         * @return the evaluated tree
         */
        @Override
        public Tree<String> evaluate(final Tree<String> tree) {
            return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
                @Override
                public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                    List<Tree<String>> newChildValues = new ArrayList<Tree<String>>();
                    String labelAsString = node.getLabel();

                // remove the effects of the binarization
                    // i.e. replace artificial nodes with their children
                    for (int i = 0; i < childrenValues.size(); i++) {
                        Tree<String> child = childrenValues.get(i);
                        if (child.getLabel().startsWith(LABEL_PREFIX)) {
                            newChildValues.addAll(child.getChildren());
                        } else {
                            newChildValues.add(child);
                        }
                    }

                // remove effects of relabeling
                    // i.e. the parent annotation and the appended size of children
                    String label;

                    if (!childrenValues.isEmpty()) {
                        Matcher match = LABELFX_PATTERN.matcher(labelAsString);
                        label = match.matches() ? match.group(1) : labelAsString;
                    } else {
                        label = labelAsString;
                    }

                    return Tree.create(label, newChildValues);
                }
            });
        }

        /**
         * Returns an automaton for the tree In this special case it applies
         * binarization an relabeling first to match the tree with rules in the
         * grammar
         *
         * @param value the tree
         * @return the automaton for the binarized and relabeled tree
         * <tt>value</tt>
         */
        @Override
        public TreeAutomaton decompose(final Tree<String> value) {
        // the binarization is only for internal handling
            // ptb-like trees therefore must be binarized before further processing
            Tree<String> decomposable = binarizeAndRelabel(value);
//        decomposable.draw();

            return super.decompose(decomposable);
        }

        /**
         * Parses PTB-formatted string
         *
         * @param representation the string containing the data
         * @return the parsed PTB-tree
         * @throws ParserException if an error occurs on parsing the input
         */
        @Override
        public Tree<String> parseString(final String representation) throws ParserException {
            Tree<String> ret;

            // first try to the actual parser
            try {
                StringReader reader = new StringReader(representation);
                ret = parseFromReader(reader);
            } catch (IOException e) {
                throw new ParserException(e);
            }

        // if there's neither an exception nor a tree,
            // the input is possibly a tree as a string
            if (ret == null) {
                try {
                    ret = TreeParser.parse(representation);
                } catch (ParseException ex) {
                    throw new ParserException(ex);
                }
            }

            signature.addAllSymbols(ret);

            return ret;
        }

        /**
         * Parses PTB-formatted trees from reader
         *
         * @param reader the reader containing the data
         * @return the parsed PTB-tree
         * @throws IOException if an error occurs on reading chars from
         * <tt>reader</tt>
         */
        public Tree<String> parseFromReader(final Reader reader) throws IOException {
            numWords = 0;
            String input = "";

        // first we're looking for the beginning of a tree
            // in mrg-files the first symbol starts after two left round brackets
            do {
                int c = reader.read();
                if (c == -1) {
                    return null;
                } else if (c == '(') {
                    input = "(";
                } else {
                    input += (char) c;
                }
                // skip every input until reaching the sequence indicating a new tree
            } while (!input.equals(START_SEQUENCE));

            // parse the tree
            Tree<String> tree = parseTree(reader);

        // a PTB-tree consists of a "wrapper"-tree its children representing the actual sentence
            // therefore we take a look at these children
            List<Tree<String>> children = tree.getChildren();

            // without any child nodes we have no tree
            if (children.isEmpty()) {
                return null;
            }

            // there are occurrences in which we have more than on tree representing the sentence
            if (children.size() > 1) {
                // for unification we select one tree and insert the others into that one
                for (int i = 0; i < children.size(); i++) {
                // a tree containing only a terminal symbol has only one child (the one with the terminal)
                    // therefore we're looking for trees with more children or more depth
                    List<Tree<String>> grandchildren = children.get(i).getChildren();
                    if ((grandchildren.size() > 1) || !grandchildren.get(0).getChildren().isEmpty()) {
                        tree = children.get(i);

                        // prepend the trees coming before the new master tree
                        for (int j = 0; j < i; j++) {
                            Tree<String> subTree = children.get(j);
                            tree.getChildren().add(j, subTree);
                        }

                        // append the trees coming after the new master tree
                        for (int j = i + 1; j < children.size(); j++) {
                            Tree<String> subTree = children.get(j);
                            tree.getChildren().add(subTree);
                        }

                        break;
                    }
                }
            } else {
                // if the "wrapper" contains only one child node: that's our new master tree
                tree = children.get(0);
            }

            return tree;
        }

        /**
         * Parses PTB-formatted trees from reader
         *
         * @param reader the reader containing the data
         * @returnsthe parsed PTB-tree
         * @throws IOException if an error occurs on reading chars from
         * <tt>reader</tt>
         */
        private String stripPosTag(StringBuffer buf) {
            String s = buf.toString();

            // remove trace indices
            Matcher matcher = STRIP_PATTERN.matcher(s);
            s = matcher.matches() ? matcher.group(1) : s;

            // clear the buffer
            buf = new StringBuffer();

            return s;
        }

        /**
         * Parses PTB-formatted trees from reader
         *
         * @param reader the reader containing the data
         * @returns the parsed PTB-tree
         * @throws IOException if an error occurs on reading chars from
         * <tt>reader</tt>
         */
        private Tree<String> parseTree(final Reader reader) throws IOException {
            String label = "";
            StringBuffer buffer = new StringBuffer();
            List<Tree<String>> children = new ArrayList<Tree<String>>();

            for (int c; (c = reader.read()) != -1;) {

                if (c == '(') { // start of a new element
                    // recursive call to get the sub tree
                    Tree<String> subTree = parseTree(reader);

                    if (subTree != null) {
                        children.add(subTree);
                    }

                } else if (c == ')') { // end of an element
                    // if the buffer is not empty we take care of that char sequence
                    if (buffer.length() > 0) {
                        // there are 2 possibilities:
                        if (label.isEmpty()) {
                            // 1. the buffer contains a non-terminal symbol
                            label = stripPosTag(buffer);
                        } else {
                            // 2. the buffer contains a terminal symbol
                            children.add(Tree.create(buffer.toString().toLowerCase()));
                            numWords++;
                        }
                    }

                    // no children indicates no valid tree -> returning null
                    if (children.isEmpty()) {
                        return null;
                    }

                // if there's only one child and it has the same symbol return that child
                    // this happens when there were removed null elements
                    if (children.size() == 1) {
                        Tree<String> child = children.get(0);
                        if (!child.getChildren().isEmpty() && child.getLabel().equals(label)) {
                            return child;
                        }
                    }

                    return Tree.create(label, children);

                } else if (c == ' ') { // delimiter for symbols (in almost all cases: "(<NONTERMINAL> <TERMINAL>)")

                    // check the buffer
                    if ((buffer.length() > 0) && label.isEmpty()) {
                        label = stripPosTag(buffer);
                        buffer = new StringBuffer();
                    }

                } else {

                    if ((c == '-') && (buffer.length() == 0) && label.isEmpty()) {
                    // - indcates a null element but only if we are at the start of a symbol (buffer is empty)
                        // and we have no label for the tree (a non terminal symbol) yet
                        // then we skip the current element
                        skipElement(reader);
                        return null;

                    } else if (c == '[') {
                        // we skip elements containing '['
                        skipElement(reader);
                        return null;

                    } else if (c > 32) {
                        // every not yet handled char - except control chars - is stored in the buffer
                        buffer.append((char) c);
                    }

                }
            }

            // we reached the end of the reader without reading the closing char ')' for this tree
            log.log(Level.SEVERE, "Unexpected end of parsed input.");
            return null;
        }

        /**
         * Skips all char up to the end of the current element
         *
         * @param reader the reader containing the data
         */
        private void skipElement(final Reader reader) throws IOException {
            for (int c; (c = reader.read()) != -1;) {
                if (c == ')') {
                    return;
                }
            }
        }

        /**
         * Binarizes a PTB tree (as in Matsukaki et al.,2005) and re-labels the
         * nodes
         *
         * @param tree the PTB tree
         * @return the binarized tree
         */
        public Tree<String> binarizeAndRelabel(final Tree<String> tree) {
            return tree.dfs(new TreeVisitor<String, Void, Tree<String>>() {
                @Override
                public Tree<String> combine(Tree<String> node, List<Tree<String>> childrenValues) {
                    // leaf node: nothing to do
                    if (childrenValues.isEmpty()) {
                        return node;
                    }

                // Re-Labeling:
                    // 1. strip symbols to the main POS and add the #children due to the constraint
                    //    every rule with the same parent state must have the same arity
                    String label = node.getLabel() + String.valueOf(childrenValues.size());

                    // 2. parent annotation (as in Johnson, 1998)
                    if (useParentAnnotation && !childrenValues.get(0).getChildren().isEmpty()) {
                        for (Tree<String> child : childrenValues) {
                            child.setLabel(child.getLabel() + "^" + label);
                        }
                    }

                    // unary nodes need no binarization
                    if (childrenValues.size() == 1) {
                        return Tree.create(label, childrenValues);
                    }

                    // already binary nodes need no binarization
                    if (childrenValues.size() == 2) {
                        return Tree.create(label, childrenValues);
                    }

                    // binarize nodes with children > 2
                    List<Tree<String>> newChildrenValues = new ArrayList<Tree<String>>();
                    newChildrenValues.add(childrenValues.get(0));
                    newChildrenValues.add(binarize(childrenValues, 1));

                    return Tree.create(label, newChildrenValues);
                }
            });
        }

        /**
         * Binarizes a list of trees (as in Mazukaki et al 2005)
         *
         * @param children the list of children trees
         * @param index the current position in the list <tt>children</tt>
         * @return the binarized list
         */
        public Tree<String> binarize(final List<Tree<String>> children, final int index) {
            List<Tree<String>> newChildren = new ArrayList<Tree<String>>();

            // add the first child to the list
            newChildren.add(children.get(index));

            // create an artificial label
            String label = LABEL_PREFIX + concatLabels(children, index);

            if (children.size() > index + 1) {
                // binarize remaining children and add the resulting tree to the list
                newChildren.add(binarize(children, index + 1));
            }

            return Tree.create(label, newChildren);
        }

        /**
         * Concatenates all root-labels of a given list of trees from an index
         * on
         *
         * @param labels the list of trees
         * @param index the from where on the root labels will be concatenated
         * @return the concatenated labels
         */
        public String concatLabels(final List<Tree<String>> trees, final int index) {
            StringBuilder ret = new StringBuilder();

            if (useParentAnnotation) {
                // split the first label at the parent annotation marker
                Matcher match = CONCAT_PATTERN.matcher(trees.get(index).getLabel());
                match.matches();

                // add only the stripped label without parent annotation
                ret.append(match.group(1));

                // and keep the parent label for later use
                String suffix = match.group(3);

                // proceed with remaining elements
                for (int i = index + 1; i < trees.size(); i++) {
                    ret.append("-");
                    match = CONCAT_PATTERN.matcher(trees.get(i).getLabel());
                    match.matches();
                    ret.append(match.group(1));
                }

                // re-add the parent label
                ret.append("^");
                ret.append(suffix);
            } else {
                // add root label
                ret.append(trees.get(index).getLabel());

                // concatenate remaining root labels
                for (int i = index + 1; i < trees.size(); i++) {
                    ret.append("-");
                    ret.append(trees.get(i).getLabel());
                }
            }

            return ret.toString();
        }
    }

}
