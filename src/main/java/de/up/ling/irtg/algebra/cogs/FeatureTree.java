package de.up.ling.irtg.algebra.cogs;

import de.saar.basic.Pair;
import de.saar.basic.StringTools;
import de.up.ling.irtg.util.Util;

import java.util.List;

public class FeatureTree {
    private String label;
    private List<Pair<String,FeatureTree>> children;

    public FeatureTree(String label, List<Pair<String, FeatureTree>> children) {
        this.label = label;
        this.children = children;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<Pair<String, FeatureTree>> getChildren() {
        return children;
    }

    public void setChildren(List<Pair<String, FeatureTree>> children) {
        this.children = children;
    }

    public FeatureTree deepCopy() {
        List<Pair<String,FeatureTree>> childrenCopies = Util.mapToList(children, child -> new Pair(child.left, child.right.deepCopy()));
        return new FeatureTree(label, childrenCopies);
    }

    private static String printChild(Pair<String,FeatureTree> child) {
        return child.left + " = " + child.right.toString();
    }

    @Override
    public String toString() {
        if( !children.isEmpty() ) {
            String childrenStr = StringTools.join(Util.mapToList(children, child -> printChild(child)), ", ");
            return label + "(" + childrenStr + ")";
        } else {
            return label;
        }
    }

    // TODO implement me
    public static FeatureTree parse(String str) {
        return null;
    }
}
