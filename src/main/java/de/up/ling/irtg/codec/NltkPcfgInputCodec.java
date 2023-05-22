package de.up.ling.irtg.codec;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.TreeWithAritiesAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An input codec for reading PCFG grammars in NLTK format.
 * The codec will also read CFGs; each rule will then get weight 1.
 * The resulting IRTG will look like the output of {@link PcfgIrtgInputCodec}.
 * <p>
 *
 * Known limitations:
 * <ul>
 *     <li>Alto does not support rules with empty right-hand sides, and so this input will not read NLTK grammars with empty right-hand sides.</li>
 *     <li>The "%start" directive in NLTK seems to be undocumented, and we ignore it here. To set a nonterminal as the start symbol, make it the left-hand side of the first production rule.</li>
 * </ul>
 *
 */
@CodecMetadata(name = "nltk_pcfg", description = "Probabilistic context-free grammars in NLTK format", extension = "pcfg", type = InterpretedTreeAutomaton.class)
public class NltkPcfgInputCodec extends InputCodec<InterpretedTreeAutomaton> {
    private static Pattern ARROW_RE = Pattern.compile("\\s*-> \\s*(.*)");
    private static Pattern PROBABILITY_RE = Pattern.compile("\\s*\\[\\s*([0-9\\.]+)\\]\\s*(.*)");
    private static Pattern TERMINAL_RE = Pattern.compile("\\s*('([^']*)'|\"([^\"]*)\")\\s*(.*)"); // group 2 = '..'; group 3 = "..."; group 4 = rest
    private static Pattern DISJUNCTION_RE = Pattern.compile("\\s*\\|\\s*(.*)");
    private static Pattern STANDARD_NONTERM_RE = Pattern.compile("\\s*((\\w|[/])(\\w|[/^<>|-])*)\\s*(.*)"); // group 1 = nonterminal; group 4 = rest of line

    private int gensymNext = 1;

    @Override
    public InterpretedTreeAutomaton read(InputStream is) throws CodecParseException, IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        String line = null;
        String continueLine = "";
        int lineNum = 0;

        ConcreteTreeAutomaton<String> auto = new ConcreteTreeAutomaton<>();
        StringAlgebra stringAlgebra = new StringAlgebra();
        Homomorphism stringHom = new Homomorphism(auto.getSignature(), stringAlgebra.getSignature());
        TreeWithAritiesAlgebra treeAlgebra = new TreeWithAritiesAlgebra();
        Homomorphism treeHom = new Homomorphism(auto.getSignature(), treeAlgebra.getSignature());

        while( (line = r.readLine()) != null ) {
            lineNum++;
            line = continueLine + line.strip();

            if( line.startsWith("#") || line.length() == 0 ) {
                continue;
            }

            if( line.endsWith("\\")) {
                continueLine = line.substring(0, line.length()-1).strip() + " ";
                continue;
            }

            continueLine = "";

            // We ignore the %start directive, it seems to be undocumented, so probably safe to assume we will never encounter it.

            processRule(line, lineNum, auto, stringHom, treeHom);
        }

        InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(auto);
        irtg.addInterpretation(new Interpretation(stringAlgebra, stringHom, "string"));
        irtg.addInterpretation(new Interpretation(treeAlgebra, treeHom, "tree"));
        return irtg;
    }

    private void processRule(String line, int lineNumber, ConcreteTreeAutomaton<String> auto, Homomorphism stringHom, Homomorphism treeHom) {
        // Using the regexes with (.*) for the rest of the line makes the runtime
        // quadratic in the line length. If this ever becomes an issue, let's make it faster.
        //
        // Other than that, the implementation follows the NLTK source code quite closely (see https://www.nltk.org/_modules/nltk/grammar.html).
        String lhs;
        List<String> homLeaves = new ArrayList<>();
        List<String> rhsNonterminals = new ArrayList();
        double probability = 1;
        int i = 1;

        // parse the left-hand side
        Matcher m = STANDARD_NONTERM_RE.matcher(line);
        if( ! m.matches() ) {
            throw new CodecParseException("Line " + lineNumber + " does not start with a nonterminal.");
        } else {
            lhs = m.group(1);
            line = m.group(4);
        }

        // first rule => add my LHS as final state
        if( auto.getFinalStates().isEmpty() ) {
            auto.addFinalState(auto.addState(lhs));
        }

        // skip over the arrow
        m = ARROW_RE.matcher(line);
        if( ! m.matches() ) {
            throw new CodecParseException("Line " + lineNumber + ": Expected an arrow, got '" + line + "'");
        } else {
            line = m.group(1);
        }

        while( line.length() > 0 ) {
//            System.err.println("Remainder: '" + line + "'");

            // probability
            m = PROBABILITY_RE.matcher(line);
            if( m.matches() ) {
//                System.err.printf("PROB '%s'\n", line);
                probability = Double.parseDouble(m.group(1));
                line = m.group(2);

                if( probability > 1 ) {
                    throw new CodecParseException("Line " + lineNumber + ": Production probability " + probability + " should not be greater than 1.");
                }

                continue;
            }

            // string => add terminal
            m = TERMINAL_RE.matcher(line);
            if( m.matches() ) {
                String terminal = (m.group(2) != null) ? m.group(2) : m.group(3); // depending on '...' vs "..."

                // silently replace "*" by "__*__" for the string algebra; this is undone for the tree algebra
                // in makeRule
                if( StringAlgebra.CONCAT.equals(terminal)) {
                    terminal = StringAlgebra.SPECIAL_STAR;
                }

                homLeaves.add(terminal);
                line = m.group(4);
                continue;
            }

            // vertical bar => start new RHS
            m = DISJUNCTION_RE.matcher(line);
            if( m.matches() ) {
//                System.err.printf("DISJ '%s'\n", line);
                makeRule(auto, stringHom, treeHom, lhs, homLeaves, rhsNonterminals, probability);
                homLeaves.clear();
                rhsNonterminals.clear();
                probability = 1;
                i = 1;
//                System.err.printf("DISJUNCTION: '%s' -> '%s'\n", line, m.group(1));
                line = m.group(1);

                continue;
            }

            // otherwise nonterminal
            m = STANDARD_NONTERM_RE.matcher(line);
            if( ! m.matches() ) {
                throw new CodecParseException("Line " + lineNumber + ": Expected nonterminal, found '" + line + "'");
            } else {
//                System.err.format("NT: '%s' - '%s'\n", m.group(1), m.group(4));
                homLeaves.add("?" + (i++));
                rhsNonterminals.add(m.group(1));
                line = m.group(4);
                continue;
            }
        }

        makeRule(auto, stringHom, treeHom, lhs, homLeaves, rhsNonterminals, probability);
    }

    private void makeRule(ConcreteTreeAutomaton<String> auto, Homomorphism stringHom, Homomorphism treeHom, String lhs, List<String> homLeaves, List<String> rhsNonterminals, double probability) {
        if( rhsNonterminals.size() + homLeaves.size() == 0 ) {
            throw new CodecParseException("Unlike NLTK, Alto does not support rules with empty right-hand sides.");
        }

        String terminal = gensym("r");
        auto.addRule(auto.createRule(lhs, terminal, rhsNonterminals, probability));
        stringHom.add(terminal, Util.makeBinaryTree("*", homLeaves));

        Tree<String> treeTerm = Util.makeTreeWithArities(Tree.create(lhs, Util.mapToList(homLeaves, Tree::create)));

        // replace __*__ from the grammar by * on the tree algebra;
        // note that by this point __*__ has been suffixed with an arity
        boolean hasSpecialStar = false;
        for( String x : homLeaves ) {
            if( x.startsWith(StringAlgebra.SPECIAL_STAR)) {
                hasSpecialStar = true;
            }
        }

        if( hasSpecialStar ) { // this is skipped for efficiency if the homLeaves don't contain the __*__
            if (homLeaves.contains(StringAlgebra.SPECIAL_STAR)) {
                treeTerm = Util.mapTree(treeTerm, nodeLabel -> {
                    if( nodeLabel.startsWith(StringAlgebra.SPECIAL_STAR)) {
                        return StringAlgebra.CONCAT + nodeLabel.substring(StringAlgebra.SPECIAL_STAR.length());
                    } else {
                        return nodeLabel;
                    }
                });
            }
        }

        treeHom.add(terminal, treeTerm);
    }

    private String gensym(String prefix) {
        return prefix + (gensymNext++);
    }
}


/*

    start = None
    productions = []
    continue_line = ""
    for linenum, line in enumerate(lines):
        line = continue_line + line.strip()
        if line.startswith("#") or line == "":
            continue
        if line.endswith("\\"):
            continue_line = line[:-1].rstrip() + " "
            continue
        continue_line = ""
        try:
            if line[0] == "%":
                directive, args = line[1:].split(None, 1)
                if directive == "start":
                    start, pos = nonterm_parser(args, 0)
                    if pos != len(args):
                        raise ValueError("Bad argument to start directive")
                else:
                    raise ValueError("Bad directive")
            else:
                # expand out the disjunctions on the RHS
                productions += _read_production(line, nonterm_parser, probabilistic)
        except ValueError as e:
            raise ValueError(f"Unable to parse line {linenum + 1}: {line}\n{e}") from e

    if not productions:
        raise ValueError("No productions found!")
    if not start:
        start = productions[0].lhs()
    return (start, productions)



_STANDARD_NONTERM_RE = re.compile(r"( [\w/][\w/^<>-]* ) \s*", re.VERBOSE)
 */