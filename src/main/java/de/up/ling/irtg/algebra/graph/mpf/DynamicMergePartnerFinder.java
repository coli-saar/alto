/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph.mpf;

import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonBottomUp;
import de.up.ling.irtg.algebra.graph.mpf.EdgeIntersectionMPF;
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

    public DynamicMergePartnerFinder(int currentSource, int nrSources, int nrNodes, SGraphBRDecompositionAutomatonBottomUp auto)//maybe give expected size of finalSet as parameter?
    {
        this.auto = auto;
        
        this.vertices = new IntOpenHashSet();

        sourceNr = currentSource;
        children = new MergePartnerFinder[nrNodes + 2];
        sourcesRemaining = nrSources;
        
    }

    public DynamicMergePartnerFinder(int currentSource, int nrSources, int nrNodes, SGraphBRDecompositionAutomatonBottomUp auto, IntSet vertices)//maybe give expected size of finalSet as parameter?
    {
        this.auto = auto;
        
        this.vertices = vertices;

        sourceNr = currentSource;
        children = new MergePartnerFinder[nrNodes + 2];
        sourcesRemaining = nrSources;
        
    }

    @Override
    public void insert(int rep) {
        
        int vNr = auto.getStateForId(rep).getSourceNode(sourceNr);
        if (vNr != -1) {
            insertInto(vNr, rep);
        } else {
            for (int v = 0; v < children.length; v++) {
                insertInto(v, rep);
            }
        }
    }

    private void insertInto(int vNr, int rep) {
        
        if (children[vNr] == null) {
            if (sourcesRemaining == 1) {
                children[vNr] = new StorageMPF(auto);
                //children[index] = new EdgeIntersectionMPF((hasAll || (index == ALL)), newVertices, auto);
            } else {
                IntSet newVertices = new IntOpenHashSet();
                newVertices.addAll(vertices);
                newVertices.add(vNr);
                children[vNr] = new DynamicMergePartnerFinder(sourceNr + 1, sourcesRemaining - 1, children.length - 2, auto, newVertices);
            }
        }
        
        children[vNr].insert(rep);
    }

    @Override
    public IntList getAllMergePartners(int rep) {
        int vNr = auto.getStateForId(rep).getSourceNode(sourceNr);
        IntList ret = new IntArrayList();//list is fine, since the two lists we get bottom up are disjoint anyway.
        
        
        if (vNr != -1) {
            if (!(children[vNr] == null)) {
                ret.addAll(children[vNr].getAllMergePartners(rep));
            }
        } else {
            for (MergePartnerFinder children1 : children) {
                if (children1 != null) {
                    ret.addAll(children1.getAllMergePartners(rep));
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
