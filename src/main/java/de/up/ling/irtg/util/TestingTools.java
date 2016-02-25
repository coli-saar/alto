/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import com.google.common.collect.Iterables;
import de.saar.basic.StringOrVariable;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.parser.TermParser;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.WeightedTree;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomatonParser;
import de.up.ling.irtg.automata.language_iteration.EvaluatedItem;
import de.up.ling.irtg.automata.language_iteration.ItemEvaluator;
import de.up.ling.irtg.automata.language_iteration.UnevaluatedItem;
import de.up.ling.irtg.codec.CodecParseException;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.codec.IsiAmrInputCodec;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.maxent.FeatureFunction;
import de.up.ling.irtg.maxent.MaximumEntropyIrtg;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class TestingTools {
    /**
     * Returns an InputStream for the resource with the given name. Resources
     * are resolved relative to the root of the given classpath; thus files in
     * src/test/resources can be addressed as "foo.txt" etc.
     *
     * @param filename
     * @return
     * @throws IOException
     */
    public static InputStream rs(String filename) throws IOException {
        return TestingTools.class.getClassLoader().getResourceAsStream(filename);
    }

    public static Tree<String> pt(String s) throws Exception {
        return TreeParser.parse(s);
    }

    public static Tree<Integer> pti(String s, Signature sig) throws Exception {
        return sig.addAllSymbols(pt(s));
    }

    public static Tree<StringOrVariable> ptv(String s) {
        Term x = TermParser.parse(s);
        return x.toTreeWithVariables();
    }

    // sig = target signature of homomorphism
    public static Tree<HomomorphismSymbol> pth(String s, Signature sig) throws Exception {
        return HomomorphismSymbol.treeFromNames(pt(s), sig);
    }

    public static TreeAutomaton pa(String s) throws CodecParseException, IOException {
        return (new TreeAutomatonInputCodec()).read(new ByteArrayInputStream(s.getBytes()));
    }

    public static TreeAutomaton pa(InputStream s) throws CodecParseException, IOException {
        return (new TreeAutomatonInputCodec()).read(s);
    }

    public static InterpretedTreeAutomaton pi(String s) throws IOException, de.up.ling.irtg.codec.CodecParseException {
        return new IrtgInputCodec().read(s);
    }

    public static InterpretedTreeAutomaton pi(InputStream r) throws IOException, de.up.ling.irtg.codec.CodecParseException {
        return InterpretedTreeAutomaton.read(r);
    }

    public static SGraph pg(String s) throws de.up.ling.irtg.codec.CodecParseException, IOException {
        return (new IsiAmrInputCodec()).read(new ByteArrayInputStream(s.getBytes()));
    }

    public static Homomorphism hom(Map<String, String> mappings, Signature sourceSignature) throws Exception {
        return hom(mappings, sourceSignature, new Signature());
    }

    public static Homomorphism hom(Map<String, String> mappings, Signature sourceSignature, Signature targetSignature) throws Exception {
        Homomorphism ret = new Homomorphism(sourceSignature, targetSignature);

        for (String sym : mappings.keySet()) {
            ret.add(sourceSignature.getIdForSymbol(sym), pth(mappings.get(sym), ret.getTargetSignature()));
        }

        return ret;
    }

    public static Homomorphism hom(Map<String, String> mappings) throws Exception {
        return hom(mappings, new Signature());
    }

    public static Signature sig(Map<String, Integer> symbols) {
        Signature ret = new Signature();

        for (String sym : symbols.keySet()) {
            ret.addSymbol(sym, symbols.get(sym));
        }

        return ret;
    }

    public static void assertAlmostEquals(double x, double y) {
        assert Math.abs(x - y) < 0.0001 : ("expected " + x + ", got " + y);
    }

    public static Rule rule(String parent, String label, List<String> children, TreeAutomaton automaton) {
        return automaton.createRule(parent, label, children, 1);
    }

    public static Set<Rule> rbu(String label, List children, TreeAutomaton auto) {
        int[] childStates = new int[children.size()];

        for (int i = 0; i < children.size(); i++) {
            childStates[i] = auto.getIdForState(children.get(i));
        }

        Set<Rule> ret = new HashSet<Rule>();
        Iterables.addAll(ret, auto.getRulesBottomUp(auto.getSignature().getIdForSymbol(label), childStates));

        return ret;
    }

    // *** Methodes for Condensed Tree Automatons
    public static CondensedTreeAutomaton pac(String s) throws de.up.ling.irtg.automata.condensed.ParseException {
        return CondensedTreeAutomatonParser.parse(new StringReader(s));
    }

    public static FeatureFunction makeTestFeature(final String x) {
        return new FeatureFunction<String, Double>() {
            public Double evaluate(Rule rule, TreeAutomaton<String> automaton, MaximumEntropyIrtg irtg, Map<String, Object> inputs) {
                return Double.parseDouble(x);
            }
        };
    }

    public static List<String> ss(String s) {
        return Arrays.asList(s.split("\\s+"));
    }

    // multiplies the item weight by a factor given by the rule label
    public static class MultiplyMapItemEvaluator implements ItemEvaluator<Void> {
        private Map<String, Double> factors;
        private Signature sig;

        public MultiplyMapItemEvaluator(Map<String, Double> factors, Signature sig) {
            this.factors = factors;
            this.sig = sig;
        }

        @Override
        public EvaluatedItem<Void> evaluate(Rule refinedRule, List<EvaluatedItem<Void>> children, UnevaluatedItem unevaluatedItem) {
            double weight = 1;
            List<Tree<Integer>> childTrees = new ArrayList<>();
            List<String> childNodeLabels = new ArrayList<>();

            for (EvaluatedItem ch : children) {
                weight *= ch.getWeightedTree().getWeight();
                childTrees.add(ch.getWeightedTree().getTree());
                childNodeLabels.add(sig.resolveSymbolId(ch.getWeightedTree().getTree().getLabel()));
            }

            double itemWeight = weight * refinedRule.getWeight();
            WeightedTree wtree = new WeightedTree(Tree.create(refinedRule.getLabel(), childTrees), itemWeight);

            String ch = String.join(" ", childNodeLabels);
            if (factors.containsKey(ch)) {
                itemWeight *= factors.get(ch);
            }

            return new EvaluatedItem<>(unevaluatedItem, wtree, itemWeight, null);
        }
    }
}
