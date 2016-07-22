/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.nonterminals;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.rule_finding.create_automaton.HomomorphismManager;
import de.up.ling.tree.Tree;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author christoph_teichmann
 */
public class ReplaceNonterminal {

    /**
     *
     */
    public static final String DIVIDER_REGEX = " \\|\\|\\| ";
    public static final String DIVIDER = " ||| ";

    /**
     *
     */
    private final Map<String, String> replacementsLeft;

    /**
     *
     */
    private final Map<String, String> replacementsRight;

    /**
     *
     */
    private final String rootNonterminal;

    /**
     *
     * @param repLeft
     * @param repRight
     * @param root
     * @throws IOException
     */
    public ReplaceNonterminal(InputStream repLeft, InputStream repRight, String root) throws IOException {
        replacementsLeft = new HashMap<>();
        replacementsRight = new HashMap<>();

        addEntries(repLeft, replacementsLeft);
        addEntries(repRight, replacementsRight);

        this.rootNonterminal = root;
    }

    /**
     *
     * @param ita
     * @param defAult
     * @return
     */
    public InterpretedTreeAutomaton introduceNonterminals(InterpretedTreeAutomaton ita,
            String defAult) {
        TreeAutomaton basis = ita.getAutomaton();

        ConcreteTreeAutomaton ta = new ConcreteTreeAutomaton(basis.getSignature());
        Map<String, Interpretation> ints = ita.getInterpretations();

        Iterable<Rule> rules = basis.getAllRulesTopDown();

        for (Rule rule : rules) {
            Object parent = basis.getStateForId(rule.getParent());
            if (basis.getFinalStates().contains(rule.getParent())) {
                ta.addFinalState(ta.addState(parent));
            }

            String label = basis.getSignature().resolveSymbolId(rule.getLabel());

            Object[] children = new Object[rule.getChildren().length];
            for (int i = 0; i < children.length; ++i) {
                children[i] = basis.getStateForId(rule.getChildren()[i]);
            }

            if (Variables.isVariable(label)) {
                String content = Variables.getInformation(label);
                if (content.equals(HomomorphismManager.UNIVERSAL_START)) {
                    label = Variables.createVariable(this.rootNonterminal);
                } else {
                    int pos = content.indexOf(HomomorphismManager.FINAL_VARIABLE_STATE_DELIMITER);

                    String l = content.substring(0, pos);
                    String r = content.substring(pos + HomomorphismManager.FINAL_VARIABLE_STATE_DELIMITER.length());

                    String nl = this.replacementsLeft.get(l);
                    String nr = this.replacementsRight.get(r);

                    if (nl == null) {
                        nl = defAult;
                    }

                    if (nr == null) {
                        nr = defAult;
                    }

                    label = Variables.createVariable(nl + "*" + nr);
                }

                for (Interpretation inter : ints.values()) {
                    Homomorphism hom = inter.getHomomorphism();

                    Tree<String> t = Tree.create("?1");
                    t = Tree.create(label, t);

                    hom.getSourceSignature().addSymbol(label, 1);

                    hom.add(label, t);
                }
            }

            ta.addRule(ta.createRule(parent, label, children, rule.getWeight()));
        }

        InterpretedTreeAutomaton result = new InterpretedTreeAutomaton(ta);
        result.addAllInterpretations(ints);
        return result;
    }

    /**
     *
     * @param repLeft
     * @param store
     * @throws IOException
     */
    private void addEntries(InputStream repLeft, Map<String, String> store) throws IOException {
        try (BufferedReader left = new BufferedReader(new InputStreamReader(repLeft))) {
            String line;

            while ((line = left.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split(DIVIDER_REGEX);
                String name = parts[0];

                if (name.startsWith("'")) {
                    name = name.substring(1);
                }
                if (name.endsWith("'")) {
                    name = name.substring(0, name.length() - 1);
                }

                store.put(name, parts[1].trim());
            }
        }
    }
}
