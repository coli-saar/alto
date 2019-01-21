/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtools.aligner;

import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.graph.BlobUtils;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import edu.stanford.nlp.ling.TaggedWord;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains functions that score how good extending an alignment to another node is.
 * @author Jonas
 */
public class AlignmentExtender {
    
    private static final Set<String> DATE_EDGES = new HashSet<>(Arrays.asList("day", "month", "year"));
        
    private static final List<String> ALL_DATE_EDGES_ORDERED= Arrays.asList("timezone", "quarter", "season", "decade", "century", "calendar", "era", "dayperiod", "time", "weekday","day", "month", "year2", "year");
    
    
    
    /**
     * Scores how likely it is to perform the spread defined by the given objects.
     * returns a number between 0 and 1 (excluding 0).
     * to be used in prob model
     * @param from spread from this node
     * @param edge spread via this edge
     * @param to spread to this node
     * @param wordsAndProbs
     * @param graph
     * @param we
     * @return how badly we want to extend the alignment from "from" to "to"
     */
    static double scoreExtension(GraphNode from, GraphEdge edge, GraphNode to, Set<Pair<TaggedWord, Double>> wordsAndProbs, SGraph graph,
            WordnetEnumerator we) {
        if (edge.getLabel().equals("name") && edge.getTarget().equals(from)) {
            return AlignmentScorer.SCP_EXTENSION;//name to named entity
        } else if (!Util.isNamed(to, graph)) {
            if (edge.getLabel().equals("unit") && edge.getTarget().equals(from)) {
                return AlignmentScorer.SCP_EXTENSION;//unit to quantified thing
            } else if (edge.getLabel().equals("value") && edge.getTarget().equals(from)) {
                return AlignmentScorer.SCP_EXTENSION;//spreads to all sorts of X-entity nodes
            } else if (ALL_DATE_EDGES_ORDERED.contains(edge.getLabel()) && edge.getTarget().equals(from) && edge.getSource().getLabel().equals("date-entity")) {
                return AlignmentScorer.SCP_EXTENSION*(1.0+0.01*ALL_DATE_EDGES_ORDERED.indexOf(edge.getLabel()));//quantity to quantified thing
            } else if (edge.getLabel().equals("quant") && edge.getTarget().equals(from)) {
                return AlignmentScorer.SCP_MAYBE_EXTENSION;//quantity to quantified thing
            } else if (edge.getSource().getLabel().equals("have-org-role-91") && edge.getLabel().equals("ARG2") && edge.getTarget().equals(from)) {
                return AlignmentScorer.SCP_EXTENSION;
            } else if (edge.getSource().getLabel().equals("have-rel-role-91") && edge.getLabel().equals("ARG2") && edge.getTarget().equals(from)) {
                return AlignmentScorer.SCP_EXTENSION;
            } else if (edge.getSource().getLabel().equals("have-org-role-91") && edge.getLabel().equals("ARG0") && edge.getSource().equals(from)) {
                return AlignmentScorer.SCP_EXTENSION;
            } else if (edge.getSource().getLabel().equals("have-rel-role-91") && edge.getLabel().equals("ARG0") && edge.getSource().equals(from)) {
                return AlignmentScorer.SCP_EXTENSION;
            } else if (edge.getTarget().getLabel().equals("government-organization") && edge.getSource().getLabel().equals("govern-01")) {
                return AlignmentScorer.SCP_EXTENSION;//between government-organization and govern
            } else if (edge.getSource().getLabel().equals("multiple") && edge.getLabel().equals("op1") && edge.getSource().equals(to)) {
                return AlignmentScorer.SCP_EXTENSION;
            } else if (edge.getLabel().equals("mode") && to.getLabel().equals("imperative") && edge.getTarget().equals(to)) {
                return AlignmentScorer.SCP_EXTENSION;  // extends verb to imperative
            } else if ((edge.getLabel().startsWith("ARG") || edge.getLabel().equals("domain")) && to.getLabel().equals("you") && edge.getTarget().equals(to)) {
                return AlignmentScorer.SCP_MAYBE_EXTENSION;  // imperatives have an implicit "you". 
                                                             //This should extend the alignment from the verb to "you" just if they are currently unaligned.
            } else if (edge.getLabel().equals("degree") && from.getLabel().equals("thing")) {
                return AlignmentScorer.SCP_MAYBE_EXTENSION;
            } else if (edge.getLabel().equals("op1") && (from.getLabel().equals("before") || from.getLabel().equals("after")) && to.getLabel().equals("now") && edge.getTarget().equals(to)) {
                return AlignmentScorer.SCP_EXTENSION;
            } else if (edge.getLabel().equals("time") && to.getLabel().equals("now") && edge.getTarget().equals(to)) {
                return AlignmentScorer.SCP_MAYBE_EXTENSION;
            } else if (to.getLabel().equals("byline-91") && edge.getLabel().equals("ARG0") && edge.getTarget().equals(from)) {
                return AlignmentScorer.SCP_EXTENSION;  // prefer extending from publication to extending from author
            } else if (to.getLabel().equals("byline-91") && (edge.getLabel().equals("ARG1") || edge.getLabel().equals("ARG2") || edge.getLabel().equals("ARG3") || edge.getLabel().equals("ARG4") ) && edge.getTarget().equals(from)) {
                return AlignmentScorer.SCP_MAYBE_EXTENSION;
            } else if ((edge.getLabel().equals("ARG2") || edge.getLabel().equals("ARG3")) && to.getLabel().equals("rate-entity-91") && edge.getTarget().equals(from) ) {
                return AlignmentScorer.SCP_EXTENSION;  // extend to rate entity from date or time span if per/every/each not available
            } else if ((edge.getLabel().equals("ARG1") || edge.getLabel().equals("ARG4")) && to.getLabel().equals("rate-entity-91") && edge.getTarget().equals(from) ) {
                return AlignmentScorer.SCP_MAYBE_EXTENSION;  // extend to rate entity from other arguments if per/every/each and date or time span not available
            } else { // if (wordsAndProbs != null)// do not check this, we never want the map to be null and want an error if it is.
                if (edge.getLabel().equals("polarity") && to.getLabel().equals("-") && edge.getTarget().equals(to)) {
                    double factor = 0.0;
                    for (Pair<TaggedWord, Double> wAndP : wordsAndProbs) {
                        String word = wAndP.left.word().toLowerCase();
                        if (word.startsWith("un") || word.startsWith("in") || word.startsWith("il") || word.startsWith("im") 
                                || word.startsWith("non") || word.startsWith("irr")|| word.startsWith("ill")|| word.startsWith("ab")
                                || word.endsWith("less") || word.startsWith("dis")
                                || word.equals("nor")
                                ) {
                            factor += wAndP.right;
                        }
                    }
                    return Math.max(0, AlignmentScorer.SCP_EXTENSION*factor);   
                } else if (edge.getLabel().equals("polarity") && to.getLabel().equals("-") && edge.getTarget().equals(to)
                        && from.getLabel().equals("ever")) {
                    return AlignmentScorer.SCP_EXTENSION;  // if "never" was accidentally made with negation on ever, woohoo! Extend that baby.
                } else if (edge.getLabel().equals("ARG1") && to.getLabel().equals("possible-01") && edge.getSource().equals(to)) {
                    double factor = 0.0;
                    for (Pair<TaggedWord, Double> wAndP : wordsAndProbs) {
                        String word = wAndP.left.word().toLowerCase();
                        if (word.contains("able") || word.contains("ible") || word.contains("ably") ||word.contains("ibly") 
                                ||word.contains("abilit")){
                            factor += wAndP.right;
                        }
                    }
                    return Math.max(0, AlignmentScorer.SCP_EXTENSION*factor);
                } else if (edge.getLabel().equals("degree") && to.getLabel().equals("more") && edge.getTarget().equals(to)) {//comparative
                    double factor = 0.0;
                    for (Pair<TaggedWord, Double> wAndP : wordsAndProbs) {
                        String word = wAndP.left.word().toLowerCase();
                        if (word.endsWith("er") || word.equals("worse")){
                            factor += wAndP.right;
                        }
                    }
                    return Math.max(0, AlignmentScorer.SCP_EXTENSION*factor);
                } else if (edge.getLabel().equals("degree") && to.getLabel().equals("most") && edge.getTarget().equals(to)) {//superlative
                    double factor = 0.0;
                    for (Pair<TaggedWord, Double> wAndP : wordsAndProbs) {
                        String word = wAndP.left.word().toLowerCase();
                        if (word.endsWith("est") || word.equals("worst")){
                            factor += wAndP.right;
                        }
                    }
                    return Math.max(0, AlignmentScorer.SCP_EXTENSION*factor);
                } else if (edge.getLabel().equals("ARG0") && to.getLabel().equals("cause-01") && edge.getSource().equals(to)) {//Why
                    double factor = 0.0;
                    for (Pair<TaggedWord, Double> wAndP : wordsAndProbs) {
                        String word = wAndP.left.word().toLowerCase();
                        if (word.equals("why")){
                            factor += wAndP.right;
                        }
                    }
                    return Math.max(0, AlignmentScorer.SCP_EXTENSION*factor);
                } else if (edge.getLabel().startsWith("ARG") && edge.getSource().equals(from)) {//verbalizations
                    double ret = 0.0;
                    for (Pair<TaggedWord, Double> wAndP : wordsAndProbs) {
                        String word = wAndP.left.word().toLowerCase();
                        String tag = wAndP.left.tag();
                        boolean isNoun = tag.startsWith("NN");
                        boolean isPerson = false;
                        boolean isPersonLight = false;
                        try {  // WordNet might throw an error
                            isPersonLight = to.getLabel().equals("person");
                            isPerson = we.getAllNounHypernyms(word).contains("person") && to.getLabel().equals("person");
                        } catch (java.lang.IllegalArgumentException ex) {
                            System.err.println("***WARNING*** \n WordNet probably doesn't recognise some character in the word\n "
                                    +de.up.ling.irtg.util.Util.getStackTrace(ex));                         
                        }
                        boolean isThing = to.getLabel().equals("thing");
                        if (isPerson || isThing) {
                            if (isNoun) {
                                ret += AlignmentScorer.SCP_EXTENSION * wAndP.right;
                            } else {
                                ret += AlignmentScorer.SCP_MAYBE_EXTENSION * wAndP.right;
                            }
                        }
                        if (isPersonLight) {
                            ret += AlignmentScorer.SCP_MAYBE_EXTENSION*0.1 * wAndP.right;
                        }
                    }
                    return Math.max(0, ret);//was 0.0001
                }
            }
        }
        return 0;//TODO get rid of this constant?//was 0.0001
    }
    
    /**
     * Makes sure that the given spread does not violate blob constraints
     * required to find an AM derivation.
     * @param nodesOfAlignment
     * @param nnToSpreadTo
     * @param graph
     * @return 
     */
    static boolean isSpreadFineWithBlobs(Set<String> nodesOfAlignment, String nnToSpreadTo, SGraph graph) {
        boolean hasOutgoing = false;
        boolean hasIncoming = false;
        boolean isConnected = isConnected(nodesOfAlignment, graph);
        for (String nn : nodesOfAlignment) {
            boolean hasOutgoingHere = false;
            boolean hasIncomingHere = false;
            GraphNode node = graph.getNode(nn);
            for (GraphEdge e : graph.getGraph().edgesOf(node)) {
                if (e.getLabel().equals("wiki")) {
                    continue;//ignore wiki edges here
                }
                String other = BlobUtils.otherNode(node, e).getName();
                if (!nodesOfAlignment.contains(other) && !nnToSpreadTo.equals(other)) {
                    if (BlobUtils.isBlobEdge(node, e)) {
                        if (hasOutgoing && isConnected) {
                            //only throw warning if connected
                            System.err.println("***WARNING*** found too many nodes with outgoing edges in alignment! "+nodesOfAlignment.toString());
                        }
                        hasOutgoingHere = true;
                    } else {
                        if (hasIncoming && isConnected) {
                            //only throw warning if connected
                            System.err.println("***WARNING*** found too many nodes with incoming edges in alignment! "+nodesOfAlignment.toString());
                        }
                        hasIncomingHere = true;
                    }
                }
            }
            if (hasOutgoingHere) {
                hasOutgoing = true;
            }
            if (hasIncomingHere) {
                hasIncoming = true;
            }
        }
        GraphNode node = graph.getNode(nnToSpreadTo);
        for (GraphEdge e : graph.getGraph().edgesOf(node)) {
            if (!nodesOfAlignment.contains(BlobUtils.otherNode(node, e).getName())) {
                if (BlobUtils.isBlobEdge(node, e)) {
                    if (hasOutgoing) {
                        return false;
                    }
                } else {
                    if (hasIncoming) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    private static boolean isConnected(Set<String> nodeNames, SGraph graph) {
        if (nodeNames.isEmpty()) {
            return true;
        }
        Set<String> found = new HashSet<>();
        found.add(nodeNames.iterator().next());
        int oldSize = 0;
        //this loop is horribly inefficient, but total cost is so small that I don't care
        while (found.size() > oldSize) {
            oldSize = found.size();
            Set<String> newFound = new HashSet<>();
            for (String nn : found) {
                GraphNode node = graph.getNode(nn);
                for (GraphEdge e : graph.getGraph().edgesOf(node)) {
                    GraphNode other = BlobUtils.otherNode(node, e);
                    if (nodeNames.contains(other.getName())) {
                        newFound.add(other.getName());
                    }
                }
            }
            found.addAll(newFound);
        }
        return found.equals(nodeNames);
    }
    
}
