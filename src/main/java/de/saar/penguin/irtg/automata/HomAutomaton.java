/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.automata;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import de.saar.basic.StringOrVariable;
import de.saar.basic.tree.Tree;
import de.saar.basic.tree.TreeVisitor;
import de.saar.penguin.irtg.hom.Homomorphism;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author koller
 */
class HomAutomaton extends TreeAutomaton<String> {

    private TreeAutomaton base;
    private Homomorphism hom;
    private int gensymNext = 1;

    public HomAutomaton(TreeAutomaton base, Homomorphism hom) {
        this.base = base;
        this.hom = hom;

        allStates = new HashSet<String>();

        finalStates = new HashSet<String>();
        for (Object st : base.getFinalStates()) {
            finalStates.add(st.toString());
        }
    }

    @Override
    public void makeAllRulesExplicit() {
        if (!isExplicit) {
            Set<Rule> baseRuleSet = base.getRuleSet();
            SetMultimap<Object, Object> chainRules = HashMultimap.create();
            final Set<String> labels = new HashSet<String>();

            for (final Rule<Object> rule : baseRuleSet) {
                final Tree<StringOrVariable> tree = hom.get(rule.getLabel());

                if (tree.getLabel(tree.getRoot()).isVariable()) {
                    // special case for homomorphisms of the form ?1: store chain rule
                    assert rule.getChildren().length == 1;

                    chainRules.put(rule.getChildren()[0], rule.getParent());
                } else {
                    // otherwise, iterate over homomorphic image of rule label and
                    // introduce rules as we go along
                    tree.dfs(new TreeVisitor<Void, String>() {

                        @Override
                        public String combine(String node, List<String> childrenValues) {
                            StringOrVariable label = tree.getLabel(node);

                            if (label.isVariable()) {
                                return rule.getChildren()[Homomorphism.getIndexForVariable(label)].toString();
                            } else {
                                String parentState = null;
                                double weight = 0;

                                if (node.equals(tree.getRoot())) {
                                    parentState = rule.getParent().toString();
                                    weight = rule.getWeight();
                                } else {
                                    parentState = gensymState();
                                    weight = 1;
                                }

                                storeRule(new Rule<String>(parentState, label.toString(), childrenValues, weight));
                                labels.add(label.toString());
                                return parentState;
                            }
                        }
                    });
                }
                
                // now process chain rules
                for( Entry<Object,Object> entry : chainRules.entries() ) {
                    for( String label : labels ) {
                        for( Rule<String> ruleForEntry : getRulesTopDownFromExplicit(label, entry.getKey().toString()) ) {
                            storeRule(new Rule<String>(entry.getValue().toString(), label, ruleForEntry.getChildren()));
                        }
                    }
                }

            }

            isExplicit = true;
        }
    }

    private String gensymState() {
        return "qh" + (gensymNext++);
    }

    @Override
    public Set<Rule<String>> getRulesBottomUp(String label, List<String> childStates) {
        makeAllRulesExplicit();
        return getRulesBottomUpFromExplicit(label, childStates);
    }

    @Override
    public Set<Rule<String>> getRulesTopDown(String label, String parentState) {
        makeAllRulesExplicit();
        return getRulesTopDownFromExplicit(label, parentState);
    }

//    @Override
//    public int getArity(String label) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
    @Override
    public Set<String> getAllLabels() {
        makeAllRulesExplicit();
        return explicitRules.keySet();
    }

    @Override
    public Set<String> getFinalStates() {
        return finalStates;
    }

    @Override
    public Set<String> getAllStates() {
        makeAllRulesExplicit();
        return allStates;
    }
}
