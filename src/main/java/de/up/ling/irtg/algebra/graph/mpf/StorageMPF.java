/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph.mpf;

import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonBottomUp;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * simply puts everything inserted in a set, and returns the whole set when asked for partners. 
 * @author jonas
 */
class StorageMPF extends MergePartnerFinder{
    private final IntList finalSet;//list is fine, since every subgraph gets sorted in at most once.
    private final SGraphBRDecompositionAutomatonBottomUp auto;
    
    public StorageMPF(SGraphBRDecompositionAutomatonBottomUp auto){
        finalSet = new IntArrayList();
        this.auto = auto;
    }
    
    @Override
    public void insert(int rep) {
        finalSet.add(rep);
    }

    @Override
    public IntList getAllMergePartners(int rep) {
        return finalSet;
    }

    @Override
    public void print(String prefix, int indent) {
        int indentSpaces= 5;
        StringBuilder indenter = new StringBuilder();
        for (int i= 0; i<indent*indentSpaces; i++){
            indenter.append(" ");
        }
        StringBuilder content = new StringBuilder();
        for (int i : finalSet)
        {
            //content.append(String.valueOf(i)+",");
            content.append(auto.getStateForId(i).toString()+",");
        }
        System.out.println(indenter.toString()+prefix+content);
    }
    
}
