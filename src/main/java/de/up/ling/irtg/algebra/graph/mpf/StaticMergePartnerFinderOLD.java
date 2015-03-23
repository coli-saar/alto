/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph.mpf;
import de.up.ling.irtg.algebra.graph.decompauto.SGraphBRDecompositionAutomatonBottomUp;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
/**Deprecated! Use DynamicMergePartherFinder instead
 *
 * 
 * 
 * @author jonas
 */
public class StaticMergePartnerFinderOLD extends MergePartnerFinder {
    
   private final boolean isFinal;
   private final StaticMergePartnerFinderOLD[] children;
   private final IntList finalSet;
   private final int sourceNr;
   private final int ALL;//give this name so its unlikely this is actually a name of a source
   private final int BOT;
   private final SGraphBRDecompositionAutomatonBottomUp auto;
   
   public StaticMergePartnerFinderOLD(int currentSource, int nrSources, int nrNodes, SGraphBRDecompositionAutomatonBottomUp auto)//maybe give expected size of finalSet as parameter?
   {
       this.auto = auto;
       if (nrSources == 0){
           isFinal = true;
           sourceNr = -1;
           children = null;
           finalSet = new IntArrayList();
           ALL = -1;
           BOT = -1;
       }
       else{
           isFinal = false;
           finalSet = null;
           sourceNr = currentSource;
           children = new StaticMergePartnerFinderOLD[nrNodes + 2];
           ALL = nrNodes;
           BOT = nrNodes +1;
           for (int i = 0; i<nrNodes; i++)
           {
               children[i] = new StaticMergePartnerFinderOLD(currentSource + 1, nrSources -1, nrNodes, auto);//TODO: throw error if vName == ALL or vName == BOT
           }
           children[BOT] = new StaticMergePartnerFinderOLD(currentSource + 1, nrSources -1, nrNodes, auto);
           children[ALL] = new StaticMergePartnerFinderOLD(currentSource + 1, nrSources -1, nrNodes, auto);
       }
   }
   
   @Override
   public void insert(int rep)
   {
       if (isFinal)
       {
           finalSet.add(rep);
       }
       else
       {
           int vNr = auto.getStateForId(rep).getSourceNode(sourceNr);
           if (vNr != -1)
           {
               children[vNr].insert(rep);
               children[ALL].insert(rep);
           }
           else
           {
               children[BOT].insert(rep);
               children[ALL].insert(rep);
           }
       }
   }
   
   @Override
   public IntList getAllMergePartners(int rep)
   {
       if (isFinal)
           return finalSet;
       else
       {
           int vNr = auto.getStateForId(rep).getSourceNode(sourceNr);
           if (vNr != -1)
           {
               IntList ret = new IntArrayList();
               ret.addAll(children[vNr].getAllMergePartners(rep));
               ret.addAll(children[BOT].getAllMergePartners(rep));
               return ret;
           }
           else
           {
               return children[ALL].getAllMergePartners(rep);
           }
       }
   }

    @Override
    public void print(String prefix, int indent) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
