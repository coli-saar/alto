/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 *
 * @author jonas
 */
public class BolinasMPF extends MergePartnerFinder{

    private final boolean hasAll;
    private final IntSet vertices;
    private final MergePartnerFinder[] children;
    private final int sourceNr;
    private final int sourcesRemaining;
    private final int ALL;
    private final int BOT;
    private final SGraphBRDecompositionAutomaton auto;

    public BolinasMPF(int currentSource, int nrSources, int nrNodes, SGraphBRDecompositionAutomaton auto)//maybe give expected size of finalSet as parameter?
    {
        this.auto = auto;
        /*if (nrSources == 0){
         //isFinal = true;
         sourceNr = -1;
         children = null;
         ALL = -1;
         BOT = -1;
         sourcesRemaining = nrSources;
         }
         else{*/
        //isFinal = false;
        this.vertices = new IntOpenHashSet();
        this.hasAll = false;

        sourceNr = currentSource;
        children = new MergePartnerFinder[nrNodes + 2];
        ALL = nrNodes;
        BOT = nrNodes + 1;
        sourcesRemaining = nrSources;
        /*for (int i = 0; i<nrNodes; i++)
         {
         children[i] = new MergePartnerFinder(currentSource + 1, nrSources -1, nrNodes, auto);//TODO: throw error if vName == ALL or vName == BOT
         }
         children[BOT] = new MergePartnerFinder(currentSource + 1, nrSources -1, nrNodes, auto);
         children[ALL] = new MergePartnerFinder(currentSource + 1, nrSources -1, nrNodes, auto);*/
        //}
    }

    public BolinasMPF(int currentSource, int nrSources, int nrNodes, SGraphBRDecompositionAutomaton auto, boolean hasAll, IntSet vertices)//maybe give expected size of finalSet as parameter?
    {
        this.auto = auto;
        /*if (nrSources == 0){
         //isFinal = true;
         sourceNr = -1;
         children = null;
         ALL = -1;
         BOT = -1;
         sourcesRemaining = nrSources;
         }
         else{*/
        //isFinal = false;
        this.vertices = vertices;
        this.hasAll = hasAll;
        
        int nrSkips = 0;
        if (currentSource == auto.BOLINASROOTSOURCENR || currentSource == auto.BOLINASSUBROOTSOURCENR){
            nrSkips++;
            if (currentSource+nrSkips == auto.BOLINASROOTSOURCENR || currentSource+nrSkips == auto.BOLINASSUBROOTSOURCENR){
                nrSkips++;
            }
        }
        

        sourceNr = currentSource+nrSkips;
        children = new MergePartnerFinder[nrNodes + 2];
        ALL = nrNodes;
        BOT = nrNodes + 1;
        
        int nrPastSkips = 0;
        if (sourceNr+1 == auto.BOLINASROOTSOURCENR || sourceNr+1 == auto.BOLINASSUBROOTSOURCENR){
            nrPastSkips++;
            if (sourceNr+1+nrPastSkips == auto.BOLINASROOTSOURCENR || currentSource+1+nrPastSkips == auto.BOLINASSUBROOTSOURCENR){
                nrPastSkips++;
            }
        }
        
        sourcesRemaining = nrSources-nrSkips-nrPastSkips;
        /*for (int i = 0; i<nrNodes; i++)
         {
         children[i] = new MergePartnerFinder(currentSource + 1, nrSources -1, nrNodes, auto);//TODO: throw error if vName == ALL or vName == BOT
         }
         children[BOT] = new MergePartnerFinder(currentSource + 1, nrSources -1, nrNodes, auto);
         children[ALL] = new MergePartnerFinder(currentSource + 1, nrSources -1, nrNodes, auto);*/
        //}
    }

    @Override
    public void insert(int rep) {
        /*if (isFinal)
         {
         //if (finalSet.contains(rep))
         //    System.out.println(rep + " already here!");
         finalSet.add(rep);
         }
         else
         {*/
        int vNr = auto.getStateForId(rep).getSourceNode(sourceNr);
        if (vNr != -1) {
            IntSet newVertices = new IntOpenHashSet();
            newVertices.addAll(vertices);
            newVertices.add(vNr);
            insertInto(vNr, rep, newVertices);
            insertInto(ALL, rep, vertices);
        } else {
            insertInto(BOT, rep, vertices);
        }
    }

    private void insertInto(int index, int rep, IntSet newVertices) {
        if (children[index] != null) {
            children[index].insert(rep);
        } else {
            
            if (sourcesRemaining == 1) {
                //children[index] = new StorageMPF(auto);
                children[index] = new BolinasEdgeMPF((hasAll || (index == ALL)), newVertices, auto);
            } else {
                children[index] = new BolinasMPF(sourceNr + 1, sourcesRemaining - 1, children.length - 2, auto, (hasAll || (index == ALL)), newVertices);
            }
            children[index].insert(rep);
        }
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
            if (children[ALL] != null) {
                ret.addAll(children[ALL].getAllMergePartners(rep));
            }
        }
            
            
        if (!(children[BOT] == null)) {
            ret.addAll(children[BOT].getAllMergePartners(rep));
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
            String newPrefix;
            if (i == ALL) {
                newPrefix = "ALL: ";
            } else if (i == BOT) {
                newPrefix = "BOT: ";
            } else {
                newPrefix = "V" + String.valueOf(i) + ": ";
            }

            if (children[i] != null) {
                children[i].print(newPrefix, indent + 1);
            } else {
                System.out.println(indenter.toString() + newPrefix + "--");
            }
        }
    }

}

