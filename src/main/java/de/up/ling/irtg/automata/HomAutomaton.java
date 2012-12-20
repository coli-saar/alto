/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import de.saar.basic.StringOrVariable;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
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
        super(hom.getTargetSignature());
        
        this.base = base;
        this.hom = hom;

        for (Object st : base.getFinalStates()) {
            addFinalState(st.toString());
        }
    }

    @Override
    public void makeAllRulesExplicit() {
        if (!isExplicit) {
            Set<Rule> baseRuleSet = base.getRuleSet();
            SetMultimap<Object, Object> chainRules = HashMultimap.create();
            final Set<String> labels = new HashSet<String>();

            for (final Rule<Object> rule : baseRuleSet) {
                final Tree<StringOrVariable> homImage = hom.get(rule.getLabel());

                if (homImage.getLabel().isVariable()) {
                    // special case for homomorphisms of the form ?1 or ?2 etc.: store chain rule

                    int childPosition = Homomorphism.getIndexForVariable(homImage.getLabel());
                    chainRules.put(rule.getChildren()[childPosition], rule.getParent());
                } else {
                    // otherwise, iterate over homomorphic image of rule label and
                    // introduce rules as we go along
                    homImage.dfs(new TreeVisitor<StringOrVariable, Void, String>() {
                        @Override
                        public String combine(Tree<StringOrVariable> node, List<String> childrenValues) {
                            StringOrVariable label = node.getLabel();

                            if (label.isVariable()) {
                                return rule.getChildren()[Homomorphism.getIndexForVariable(label)].toString();
                            } else {
                                String parentState = null;
                                double weight = 0;

                                if (node == homImage) {
                                    parentState = rule.getParent().toString();
                                    weight = rule.getWeight();
                                } else {
                                    parentState = gensymState();
                                    weight = 1;
                                }

                                Rule<String> newRule = new Rule<String>(parentState, label.toString(), childrenValues, weight);
                                storeRule(newRule);
                                labels.add(label.toString());
                                return parentState;
                            }
                        }
                    });
                }
                
                // now process chain rules
                for( Entry<Object,Object> entry : chainRules.entries() ) {
                    String lowerParent = addState(entry.getKey().toString());
                    String upperParent = addState(entry.getValue().toString());
                    
                    for( String label : labels ) {
                        for( Rule<String> ruleForEntry : getRulesTopDownFromExplicit(label, lowerParent) ) {
                            storeRule(new Rule<String>(upperParent, label, ruleForEntry.getChildren()));
                        }
                    }
                }
            }

            isExplicit = true;
        }
    }

    private String gensymState() {
        return addState("qh" + (gensymNext++));
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

    @Override
    public Set<String> getFinalStates() {
        return finalStates;
    }

    @Override
    public Set<String> getAllStates() {
        makeAllRulesExplicit();
        return super.getAllStates();
    }
}
