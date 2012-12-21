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
    public double evaluate(Rule rule){
        String pLabel = this.getLabelFor(rule.getParent());
        if (pLabel == parentLabel) {
            for (Object child : rule.getChildren()) {
                String cLabel = this.getLabelFor(child);
                if (cLabel == childLabel) {
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
