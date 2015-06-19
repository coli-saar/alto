/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 *
 * @author jonas
 */
public abstract class BinaryPartnerFinder {
    
    public abstract IntCollection getPartners(int labelID, int stateID);
        
    public abstract void addState(int stateID);
    
    
    
    //in TreeAutomaton, plus foreach methode
    
    public static class DummyBinaryPartnerFinder extends BinaryPartnerFinder {
        
        IntSet set = new IntOpenHashSet();
        
        @Override
        public IntCollection getPartners(int labelID, int stateID) {
            return set;
        }

        @Override
        public void addState(int stateID) {
            set.add(stateID);
        }
        
    }
    
}
