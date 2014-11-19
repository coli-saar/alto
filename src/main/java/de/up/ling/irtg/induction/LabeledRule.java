/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.induction;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 *
 * @author koller
 */
class LabeledRule {
        public String lhs;
        public String label;
        public List<String> rhs;
        
        public static LabeledRule fromRule(Rule rule, TreeAutomaton automaton) {
            List<String> children = Arrays.stream(rule.getChildren()).mapToObj(stateId -> getState(automaton.getStateForId(stateId))).collect(Collectors.toList());
            return new LabeledRule(getState(automaton.getStateForId(rule.getParent())), automaton.getSignature().resolveSymbolId(rule.getLabel()), children);
        }
        
        public static LabeledRule create(Object parent, String label) {
            return new LabeledRule(getState(parent), label, Collections.EMPTY_LIST);
        }

        public LabeledRule(String lhs, String label, List<String> rhs) {
            this.lhs = lhs;
            this.label = label;
            this.rhs = rhs;
        }
        
        private static String getState(Object fullState) {
            return IrtgInducer.getPrimaryState(fullState.toString());
        }

        @Override
        public String toString() {
            return lhs.toString() + " -> " + label + rhs;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 17 * hash + Objects.hashCode(this.lhs);
            hash = 17 * hash + Objects.hashCode(this.label);
            hash = 17 * hash + Objects.hashCode(this.rhs);
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
            final LabeledRule other = (LabeledRule) obj;
            if (!Objects.equals(this.lhs, other.lhs)) {
                return false;
            }
            if (!Objects.equals(this.label, other.label)) {
                return false;
            }
            if (!Objects.equals(this.rhs, other.rhs)) {
                return false;
            }
            return true;
        }
    }