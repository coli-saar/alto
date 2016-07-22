/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.data_creation;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author christoph
 */
public class GetAllNodesMap {

    /**
     *
     * @param trees
     * @return
     * @throws ParserException
     */
    public static Map<String, Set<String>>[] getCoreDescriptions(Iterable<String> trees) throws ParserException {
        MinimalTreeAlgebra mta = new MinimalTreeAlgebra();
        Map<String, Set<String>> possibleChildren = new TreeMap<>();
        Map<String, Set<String>> possibleParents = new TreeMap<>();
        Map<String, Set<String>> possibleArities = new TreeMap<>();

        for (String tree : trees) {
            TreeAutomaton ta = mta.decompose(mta.parseString(tree));

            Tree<String> form = (Tree<String>) ta.languageIterator().next();
            form = mta.evaluate(form);

            add(form, possibleChildren, possibleParents, possibleArities);

            String label = form.getLabel();

            Set<String> par = possibleParents.get(label);
            if (par == null) {
                par = new TreeSet<>();
                possibleParents.put(label, par);
            }

            par.add("__ROOT__");
        }

        return new Map[]{possibleChildren, possibleParents, possibleArities};
    }

    /**
     *
     * @param form
     * @param possibleChildren
     * @param possibleParents
     * @param possibleArities
     */
    private static void add(Tree<String> form,
            Map<String, Set<String>> possibleChildren,
            Map<String, Set<String>> possibleParents,
            Map<String, Set<String>> possibleArities) {
        String label = form.getLabel();

        Set<String> ar = possibleArities.get(label);
        if (ar == null) {
            ar = new TreeSet<>();
            possibleArities.put(label, ar);
        }
        ar.add(Integer.toString(form.getChildren().size()));

        Set<String> kids = possibleChildren.get(label);
        if (kids == null) {
            kids = new TreeSet<>();
            possibleChildren.put(label, kids);
        }

        for (Tree<String> child : form.getChildren()) {
            String clab = child.getLabel();
            kids.add(clab);

            Set<String> par = possibleParents.get(clab);
            if (par == null) {
                par = new TreeSet<>();
                possibleParents.put(clab, par);
            }

            par.add(label);

            add(child, possibleChildren, possibleParents, possibleArities);
        }
    }
}
