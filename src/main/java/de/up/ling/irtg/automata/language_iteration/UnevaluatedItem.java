/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.language_iteration;

import de.saar.basic.StringTools;
import java.util.ArrayList;
import java.util.List;

/**
 * The unevaluated item <i1, ..., in> specifies that we should attempt to
 * build a tree for the given rule by combining the i1-best tree for the
 * first child state, the i2-best tree for the second child state, etc.
 */
class UnevaluatedItem {
    public List<Integer> positionsInChildLists;

    public UnevaluatedItem(List<Integer> positionsInChildLists) {
        this.positionsInChildLists = positionsInChildLists;
    }

    public List<UnevaluatedItem> makeVariations() {
        List<UnevaluatedItem> ret = new ArrayList<UnevaluatedItem>();
        for (int pos = 0; pos < positionsInChildLists.size(); pos++) {
            List<Integer> newPositions = new ArrayList<Integer>(positionsInChildLists);
            newPositions.set(pos, newPositions.get(pos) + 1);
            ret.add(new UnevaluatedItem(newPositions));
        }
        return ret;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + (this.positionsInChildLists != null ? this.positionsInChildLists.hashCode() : 0);
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
        final UnevaluatedItem other = (UnevaluatedItem) obj;
        if (this.positionsInChildLists != other.positionsInChildLists && (this.positionsInChildLists == null || !this.positionsInChildLists.equals(other.positionsInChildLists))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "<" + StringTools.join(positionsInChildLists, ",") + ">";
    }
    
}
