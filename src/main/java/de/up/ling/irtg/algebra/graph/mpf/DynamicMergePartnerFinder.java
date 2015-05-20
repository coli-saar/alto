/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph.mpf;

import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonBottomUp;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 *
 * @author jonas
 */
public class DynamicMergePartnerFinder extends MergePartnerFinder {

    //private final boolean isFinal;
    private final IntSet vertices;
    private final MergePartnerFinder[] children;
    private final int sourceNr;
    private final int sourcesRemaining;
    private final SGraphBRDecompositionAutomatonBottomUp auto;
    private final int botIndex = 0;//the index for the children if the source is not assigned

    public DynamicMergePartnerFinder(int currentSource, int nrSources, int nrNodes, SGraphBRDecompositionAutomatonBottomUp auto)//maybe give expected size of finalSet as parameter?
    {
        this.auto = auto;
        
        this.vertices = new IntOpenHashSet();

        sourceNr = currentSource;
        children = new MergePartnerFinder[nrNodes+1];
        sourcesRemaining = nrSources;
        
    }

    private DynamicMergePartnerFinder(int currentSource, int nrSources, int nrNodes, SGraphBRDecompositionAutomatonBottomUp auto, IntSet vertices)//maybe give expected size of finalSet as parameter?
    {
        this.auto = auto;
        
        this.vertices = vertices;

        sourceNr = currentSource;
        children = new MergePartnerFinder[nrNodes+1];
        sourcesRemaining = nrSources;
        
    }

    @Override
    public void insert(int rep) {
        
        int vNr = auto.getStateForId(rep).getSourceNode(sourceNr);
        insertInto(vNr, rep);//if source is not assigned, vNr is -1.
    }

    private void insertInto(int vNr, int rep) {
        int index = vNr+1;//if source is not assigned, then index=0=botIndex.
        if (children[index] == null) {
            IntSet newVertices = new IntOpenHashSet();
            newVertices.addAll(vertices);
            if (vNr!= -1) {
                newVertices.add(vNr);
            }
            if (sourcesRemaining == 1) {
                //children[index] = new StorageMPF(auto);
                children[index] = new EdgeMPF(newVertices, auto);
            } else {
                children[index] = new DynamicMergePartnerFinder(sourceNr + 1, sourcesRemaining - 1, children.length-1, auto, newVertices);
            }
        }
        
        children[index].insert(rep);
    }

    @Override
    public IntList getAllMergePartners(int rep) {
        int vNr = auto.getStateForId(rep).getSourceNode(sourceNr);
        int index = vNr+1;
        IntList ret = new IntArrayList();//list is fine, since the two lists we get bottom up are disjoint anyway.
        
        
        if (vNr != -1) {
            if (children[index] != null) {
                ret.addAll(children[index].getAllMergePartners(rep));
            }
            if (children[botIndex] != null){
                ret.addAll(children[botIndex].getAllMergePartners(rep));
            }
        } else {
            for (MergePartnerFinder child : children) {
                if (child != null) {
                    ret.addAll(child.getAllMergePartners(rep));
                }
            }
        }
        
        return ret;
    }
    
    /*private void checkEquality(int index, int rep){
        if (children[index][1] != null){
            if (!children[index][1].getAllMergePartners(rep).containsAll(children[index][0].getAllMergePartners(rep))){
                BoundaryRepresentation bdryRep = auto.getStateForId(rep);
                System.out.println("Not equal!");
                
            }
        }
    }*/
    

    @Override
    public void print(String prefix, int indent) {
        int indentSpaces = 5;
        StringBuilder indenter = new StringBuilder();
        for (int i = 0; i < indent * indentSpaces; i++) {
            indenter.append(" ");
        }
        System.out.println(indenter.toString() + prefix + "S" + String.valueOf(sourceNr) + "(#V="+vertices.size()+")"+":");
        for (int i = 0; i < indentSpaces; i++) {
            indenter.append(" ");
        }
        for (int i = 0; i < children.length; i++) {
            String newPrefix = "V" + String.valueOf(i) + ": ";

            if (children[i] != null) {
                children[i].print(newPrefix, indent + 1);
            } else {
                System.out.println(indenter.toString() + newPrefix + "--");
            }
        }
    }

}
