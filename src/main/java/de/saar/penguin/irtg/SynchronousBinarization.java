/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg;

import de.saar.basic.Pair;
import de.saar.penguin.irtg.automata.ConcreteTreeAutomaton;
import de.saar.penguin.irtg.automata.Rule;
import de.saar.penguin.irtg.automata.TreeAutomaton;
import de.saar.penguin.irtg.hom.Homomorphism;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 *
 * @author koller
 */
public class SynchronousBinarization<E, F> {
    private Set<CorrespondenceItem> correspondenceChart;
    private Set<LeftItem> leftChart;
    private Set<RightItem> rightChart;
    private Queue<Item> agenda;
    private TreeAutomaton<E> leftAuto;
    private TreeAutomaton<F> rightAuto;

    public void binarize(TreeAutomaton<E> leftAuto, TreeAutomaton<F> rightAuto, ConcreteTreeAutomaton<Pair<E,F>> outputAutomaton, Homomorphism leftHom, Homomorphism rightHom) {
        correspondenceChart = new HashSet<CorrespondenceItem>();
        leftChart = new HashSet<LeftItem>();
        rightChart = new HashSet<RightItem>();
        agenda = new LinkedList<Item>();

        this.leftAuto = leftAuto;
        this.rightAuto = rightAuto;

        // Schritt 1: Agenda und Charts mit (Var), (Const)-Regeln fuellen

        while (!agenda.isEmpty()) {
            Item item = agenda.remove();

            if (item instanceof SynchronousBinarization.CorrespondenceItem) {
                CorrespondenceItem itemAsC = (CorrespondenceItem) item;

                for (CorrespondenceItem other : correspondenceChart) {
                    union(itemAsC, other);
                    unionReverse(itemAsC, other);
                }

                for (LeftItem other : leftChart) {
                    binary1L(itemAsC, other);
                    binary1R(other, itemAsC);
                }
            } else if (item instanceof SynchronousBinarization.LeftItem) {
                LeftItem itemAsL = (LeftItem) item;

                for (CorrespondenceItem other : correspondenceChart) {
                    binary1L(other, itemAsL);
                    binary1R(itemAsL, other);
                }

                for (LeftItem other : leftChart) {
                    binaryConstL(itemAsL, other);
                }
            }
        }
    }

    private void union(CorrespondenceItem item, CorrespondenceItem other) {
        for (String leftLabel : leftAuto.getAllLabels()) {
            List leftChildren = new ArrayList();
            leftChildren.add(item.leftState);
            leftChildren.add(other.leftState);
            for (Rule<E> leftRule : leftAuto.getRulesBottomUp(leftLabel, leftChildren)) {
                // nochmal fuer rechte regeln
            }
        }
    }

    private void unionReverse(CorrespondenceItem item, CorrespondenceItem other) {
        // union
    }

    private void binary1L(CorrespondenceItem itemAsC, LeftItem other) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void binary1R(LeftItem other, CorrespondenceItem itemAsC) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void binaryConstL(LeftItem itemAsL, LeftItem other) {

        // ...
        if (leftChart.add(null)) {
            agenda.add(null);
        }

        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    
    
    
    
      private interface Item {
    }

    private class CorrespondenceItem implements Item {
        E leftState;
        F rightState;
        Set<String> variables;

        public CorrespondenceItem(E leftState, F rightState, Set<String> variables) {
            this.leftState = leftState;
            this.rightState = rightState;
            this.variables = variables;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CorrespondenceItem other = (CorrespondenceItem) obj;
            if (this.leftState != other.leftState && (this.leftState == null || !this.leftState.equals(other.leftState))) {
                return false;
            }
            if (this.rightState != other.rightState && (this.rightState == null || !this.rightState.equals(other.rightState))) {
                return false;
            }
            if (this.variables != other.variables && (this.variables == null || !this.variables.equals(other.variables))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 29 * hash + (this.leftState != null ? this.leftState.hashCode() : 0);
            hash = 29 * hash + (this.rightState != null ? this.rightState.hashCode() : 0);
            hash = 29 * hash + (this.variables != null ? this.variables.hashCode() : 0);
            return hash;
        }
        
        
    }

    private class LeftItem implements Item {
        E state;

        public LeftItem(E state) {
            this.state = state;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LeftItem other = (LeftItem) obj;
            if (this.state != other.state && (this.state == null || !this.state.equals(other.state))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + (this.state != null ? this.state.hashCode() : 0);
            return hash;
        }
        
        
    }

    private class RightItem implements Item {
        F state;

        public RightItem(F state) {
            this.state = state;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final RightItem other = (RightItem) obj;
            if (this.state != other.state && (this.state == null || !this.state.equals(other.state))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 97 * hash + (this.state != null ? this.state.hashCode() : 0);
            return hash;
        }
    }
}

