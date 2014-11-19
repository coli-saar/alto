/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.induction;

import de.up.ling.irtg.util.Either;
import de.up.ling.tree.Tree;
import java.util.Objects;

/**
 *
 * @author koller
 */
class ElementaryTree  {
    private Tree<Either<LabeledRule,String>> tree;

    public ElementaryTree(Tree<Either<LabeledRule,String>> tree) {
        this.tree = tree;
    }

    public Tree<Either<LabeledRule,String>> getTree() {
        return tree;
    }

    public void setTree(Tree<Either<LabeledRule,String>> tree) {
        this.tree = tree;
    }
    
    public Tree<String> getLabelTree() {
        return tree.map(lr -> lr.isFirst() ? lr.asFirst().label : lr.asSecond());
    }

    @Override
    public String toString() {
        return getLabelTree().toString();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.tree);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ElementaryTree other = (ElementaryTree) obj;
        if (!Objects.equals(this.tree, other.tree)) {
            return false;
        }
        return true;
    }
    
    
}