/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
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
            final Set<Integer> labels = new HashSet<Integer>();

            for (final Rule<Object> rule : baseRuleSet) {
                final Tree<HomomorphismSymbol> homImage = hom.get(rule.getLabel());

                if (homImage.getLabel().isVariable()) {
                    // special case for homomorphisms of the form ?1 or ?2 etc.: store chain rule

                    int childPosition = homImage.getLabel().getValue();
                    chainRules.put(rule.getChildren()[childPosition], rule.getParent());
                } else {
                    // otherwise, iterate over homomorphic image of rule label and
                    // introduce rules as we go along
                    homImage.dfs(new TreeVisitor<HomomorphismSymbol, Void, String>() {
                        @Override
                        public String combine(Tree<HomomorphismSymbol> node, List<String> childrenValues) {
                            HomomorphismSymbol label = node.getLabel();

                            if (label.isVariable()) {
                                return rule.getChildren()[label.getIndex()].toString();
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

                                Rule<String> newRule = createRule(parentState, label.getValue(), childrenValues, weight);
                                storeRule(newRule);
                                labels.add(label.getValue());
                                return parentState;
                            }
                        }
                    });
                }
                
                // now process chain rules
                for( Entry<Object,Object> entry : chainRules.entries() ) {
                    String lowerParent = addState(entry.getKey().toString());
                    String upperParent = addState(entry.getValue().toString());
                    
                    for( Integer label : labels ) {
                        for( Rule<String> ruleForEntry : getRulesTopDownFromExplicit(label, lowerParent) ) {
                            storeRule(createRule(upperParent, label, ruleForEntry.getChildren(), 1)); // TODO: correct weight
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
    public Set<Rule<String>> getRulesBottomUp(int label, List<String> childStates) {
        makeAllRulesExplicit();
        return getRulesBottomUpFromExplicit(label, childStates);
    }

    @Override
    public Set<Rule<String>> getRulesTopDown(int label, String parentState) {
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

    @Override
    public boolean isBottomUpDeterministic() {
        return base.isBottomUpDeterministic();
    }
}
