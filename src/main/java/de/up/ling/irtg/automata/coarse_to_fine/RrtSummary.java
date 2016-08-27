/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.coarse_to_fine;

import java.util.Arrays;

/**
 *
 * @author koller
 */
class RrtSummary {
    private int coarseParent;
    private int[] coarseChildren;
    private int termId;

    public RrtSummary(int coarseParent, int termId, int[] coarseChildren) {
        this.coarseParent = coarseParent;
        this.termId = termId;
        this.coarseChildren = coarseChildren;
    }

    public int getCoarseParent() {
        return coarseParent;
    }

    public int[] getCoarseChildren() {
        return coarseChildren;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + this.coarseParent;
        hash = 13 * hash + Arrays.hashCode(this.coarseChildren);
        hash = 13 * hash + this.termId;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RrtSummary other = (RrtSummary) obj;
        if (this.coarseParent != other.coarseParent) {
            return false;
        }
        if (this.termId != other.termId) {
            return false;
        }
        if (!Arrays.equals(this.coarseChildren, other.coarseChildren)) {
            return false;
        }
        return true;
    }
    
        
}
