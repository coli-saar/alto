package de.up.ling.irtg.maxent;

import de.up.ling.irtg.automata.Rule;

/**
 *
 * @author Danilo Baumgarten
 */
public class ChildOfFeature extends FeatureFunction<String> {
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
    public double evaluate(Rule<String> rule){
        if (rule.getParent() == parentLabel) {
            for (String child : rule.getChildren()) {
                if (child == childLabel) {
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
