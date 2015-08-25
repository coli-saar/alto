package de.up.ling.irtg.maxent;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.Map;

/**
 *
 * @author Danilo Baumgarten
 */
public class ChildOfFeature extends FeatureFunction<String, Double> {
    private String parentLabel;
    private String childLabel;

    public ChildOfFeature(String parentLabel, String childLabel) {
        this.parentLabel = parentLabel;
        this.childLabel = childLabel;
    }

    public String getParentLabel() {
        return parentLabel;
    }

    public String getChildLabel() {
        return childLabel;
    }

    @Override
    public Double evaluate(Rule rule, TreeAutomaton<String> automaton, MaximumEntropyIrtg irtg, Map<String,Object> inputs){
        String pLabel = getLabelFor(automaton.getStateForId(rule.getParent()));
        
        if (pLabel.equals(parentLabel)) {
            for (int child : rule.getChildren()) {
                String cLabel = getLabelFor(automaton.getStateForId(child));
                if (cLabel.equals(childLabel)) {
                    return 1.0;
                }
            }
        }
        return 0.0;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(super.toString());
        ret.append("(");
        ret.append(masking(parentLabel));
        ret.append(",");
        ret.append(masking(childLabel));
        ret.append(")");
        return ret.toString();
    }

}
