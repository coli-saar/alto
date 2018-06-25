/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtools.aligner;

import com.google.common.collect.Sets;
import de.saar.basic.Pair;
import de.saar.coli.amrtagging.Alignment;
import de.saar.coli.amrtagging.Alignment.Span;
import de.up.ling.irtg.algebra.graph.BlobUtils;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import edu.stanford.nlp.ling.TaggedWord;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility function for the scoring part of Aligner.
 * @author Jonas
 */
public class AlignmentScorer {
    
    private final List<String> sent;
    private final SGraph graph;
    private final Map<Integer, Set<Alignment>> index2als;
    private final Map<Integer, Set<Alignment>> fixedIndex2als;
    private final Map<Integer, Set<Alignment>> discardedIndex2als;
    private final WordnetEnumerator we;
    private final Aligner aligner;
    private final Object2DoubleMap<Alignment> scoreBuffer;
    private final Set<Set<String>> coordinationTuples;
    
    public final static double SCP_SPECIAL = 5.0;
    public final static double SCP_FIRST_BONUS = 3.0;//TODO: apply first bonus also to pronouns?
    public final static double SCP_PERFECT = 1.0;
    public final static double SCP_EXTENSION = 1.0;
    public final static double SCP_MAYBE_EXTENSION = 0.1;
    public final static double SCP_NO_MATCH = 0.0;
    public final static double SCP_WE_MULT = 0.6;
    public final static double SCP_INDIRECT_NODE_FACTOR = 0.1;
    public final static double SCP_NEIGHBOR = 0.5;
    public final static double SCP_NEIGHBOR_GRAPH_DEPTH_2_FACTOR = 0.25;
    public final static double SCP_NEIGHBOR_DECAY = Math.sqrt(3.0);
    public final static int SCP_RANGE = 5;
    public final static double SCP_VERB2NOUN_FACTOR = 0.2;
    
    
    
    public final static double SC_SPECIAL = 10.0;
    public final static double SC_PERFECT = 1.0;
    public final static double SC_NO_MATCH = -1.0;
    public final static double SC_INDIRECT_NODE_COST = 1.0;
    public final static double FIRST_BONUS = 3.0;//TODO: apply first bonus also to pronouns?
    private final static double CLOSE_NEIGHBOR = 0.5;
    private final static double FAR_NEIGHBOR = 0.1;
    private final static int CLOSE_RANGE = 1;
    private final static int FAR_RANGE = 3;
    
    
    /**
     * Make a new scorer every time the set of fixed alignments (we decided on)
     * has changed.
     * @param candidates
     * @param fixed
     * @param discarded
     * @param sent
     * @param graph 
     * @param we 
     * @param coordinationTuples 
     * @param aligner 
     */
    AlignmentScorer(Set<Alignment> candidates, Set<Alignment> fixed, Set<Alignment> discarded,
            List<String> sent, SGraph graph, WordnetEnumerator we, Set<Set<String>> coordinationTuples, Aligner aligner) {
        this.scoreBuffer = new Object2DoubleOpenHashMap<>();
        this.sent = sent;
        this.graph = graph;
        this.we = we;
        this.aligner = aligner;
        this.coordinationTuples = coordinationTuples;
        index2als = new HashMap<>();
        for (int i = 0; i<sent.size(); i++) {
            index2als.put(i, new HashSet());
        }
        for (Alignment al : candidates) {
            for (int index = al.span.start; index<al.span.end; index++) {
                index2als.get(index).add(al);
            }
        }
        fixedIndex2als = new HashMap<>();
        for (int i = 0; i<sent.size(); i++) {
            fixedIndex2als.put(i, new HashSet());
        }
        for (Alignment al : fixed) {
            for (int index = al.span.start; index<al.span.end; index++) {
                fixedIndex2als.get(index).add(al);
            }
        }
        discardedIndex2als = new HashMap<>();
        for (int i = 0; i<sent.size(); i++) {
            discardedIndex2als.put(i, new HashSet());
        }
        for (Alignment al : discarded) {
            for (int index = al.span.start; index<al.span.end; index++) {
                discardedIndex2als.get(index).add(al);
            }
        }
    }
            
    double score(Alignment al) {
        double max = 0;
        for (int index = al.span.start; index<al.span.end; index++) {
            Set<Alignment> competitors = Sets.difference(index2als.get(index),
                    Sets.union(discardedIndex2als.get(index), Sets.union(fixedIndex2als.get(index), Collections.singleton(al))));
            for (Alignment c : competitors) {
                boolean compatibleForDual = true;
                for (String nn1 : al.nodes) {
                    for (String nn2 : c.nodes) {
                        if (!Util.areCompatibleForDualAlignment(nn1, nn2, coordinationTuples)) {
                            compatibleForDual = false;
                        }
                    }
                }
                if (!compatibleForDual) {
                    max = Math.max(max, nonrelScore(c));
                }
            }
        }
        return nonrelScore(al)-max;
    }
    
    private double nonrelScore(Alignment al) {
        if (scoreBuffer.containsKey(al)) {
            return scoreBuffer.get(al);
        } else {
            double max = 0;
            for (int index = al.span.start; index<al.span.end; index++) {
                for (String nn : al.nodes) {
                    max = Math.max(max, neighborScore(nn, index));
                }
            }
            double ret = aligner.al2Matchscore.getDouble(al) + max;
            scoreBuffer.put(al, ret);
            return ret;
        }
    }
    
    private double neighborScore(String nn, int index) {
        if (isWikiNode(nn)) {
            return -1000;
        }
        Set<Alignment> fixedAlsHere = fixedIndex2als.get(index);
        if (!fixedAlsHere.isEmpty()) {
            for (Alignment otherAl : fixedAlsHere) {
                for (String otherNn : otherAl.nodes) {
                    if (!Util.areCompatibleForDualAlignment(nn, otherNn, coordinationTuples)) {
                        return -1000;
                    }
                }
            }
            //else continue as normal
        }
        double score = 0;
        int nonemptyCount = 0;
        for (int i = index+1; i<sent.size(); i++) {
            if (!index2als.get(i).isEmpty()) {
                int neighbCount = 0;
                for (Alignment neighbAl : fixedIndex2als.get(i)) {
                    for (String neighbNn :neighbAl.nodes) {
                        if (Util.areNeighbors(nn, neighbNn, graph)) {
                            neighbCount++;
                        }
                    }
                }
                nonemptyCount ++;
                if (nonemptyCount <= CLOSE_RANGE) {
                    score += neighbCount*CLOSE_NEIGHBOR;
                } else if (nonemptyCount <= FAR_RANGE) {
                    score += neighbCount*FAR_NEIGHBOR;
                }
                if (nonemptyCount >= FAR_RANGE) {
                    break;
                }
            }
        }
        nonemptyCount = 0;
        for (int i = index-1; i>=0; i--) {
            if (!index2als.get(i).isEmpty()) {
                int neighbCount = 0;
                for (Alignment neighbAl : fixedIndex2als.get(i)) {
                    for (String neighbNn :neighbAl.nodes) {
                        if (Util.areNeighbors(nn, neighbNn, graph)) {
                            neighbCount++;
                        }
                    }
                }
                nonemptyCount ++;
                if (nonemptyCount <= CLOSE_RANGE) {
                    score += neighbCount*CLOSE_NEIGHBOR;
                } else if (nonemptyCount <= FAR_RANGE) {
                    score += neighbCount*FAR_NEIGHBOR;
                }
                if (nonemptyCount >= FAR_RANGE) {
                    break;
                }
            }
        }
        return score;
    }
    
    //TODO skip words that have no candidate assignment
    static double neighborScoreForProb(GraphNode node, Span span, SGraph graph, Map<String, Set<Pair<Span, Double>>> nn2scoredCandidates) {
        double ret = 0.0;
        Set<String> covered = new HashSet<>();
        for (GraphEdge e : graph.getGraph().edgesOf(node)) {
            GraphNode otherN = BlobUtils.otherNode(node, e);
            String otherNn = otherN.getName();
            if (!covered.contains(otherNn)) {
                covered.add(otherNn);
                ret += neighborScoreSingleNode(otherNn, span, nn2scoredCandidates);
            }
            for (GraphEdge deepE : graph.getGraph().edgesOf(otherN)) {
                String deepNn = BlobUtils.otherNode(otherN, deepE).getName();
                if (!deepNn.equals(node.getName()) && !covered.contains(deepNn)) {
                    covered.add(deepNn);
                    ret += SCP_NEIGHBOR_GRAPH_DEPTH_2_FACTOR*neighborScoreSingleNode(deepNn, span, nn2scoredCandidates);
                }
            }
        }
        return ret;
    }
    
    static double extendingNeighborScoreForProb(GraphNode node, Span span, SGraph graph,
            List<TaggedWord> tags, Map<String, Alignment> nn2fixedAlign,
            Map<String, Set<Pair<Span, Double>>> nn2scoredCandidates, WordnetEnumerator we) {
        Object2DoubleMap<GraphNode> extensionScores = new Object2DoubleOpenHashMap<>();
        for (GraphEdge edge : graph.getGraph().edgesOf(node)) {
            GraphNode other = BlobUtils.otherNode(node, edge);
            Set<Pair<TaggedWord, Double>> alignedWordAndProb = new HashSet();
            if (nn2fixedAlign.containsKey(other.getName())) {
                Alignment.Span spanHere = nn2fixedAlign.get(other.getName()).span;
                if (spanHere.isSingleton()) {
                    alignedWordAndProb.add(new Pair(tags.get(spanHere.start), 1.0));
                }
            }
            double score = AlignmentExtender.scoreExtension(node, edge, other, alignedWordAndProb, graph, we);
            if (score > 0.01) {
                extensionScores.put(other, Math.max(score, extensionScores.getDouble(other)));
                for (GraphEdge deepE : graph.getGraph().edgesOf(other)) {
                    GraphNode deepO = BlobUtils.otherNode(other, deepE);
                    Set<Pair<TaggedWord, Double>> deepAlignedWordAndProb = new HashSet();
                    if (nn2fixedAlign.containsKey(deepO.getName())) {
                        Alignment.Span spanHere = nn2fixedAlign.get(deepO.getName()).span;
                        if (spanHere.isSingleton()) {
                            deepAlignedWordAndProb.add(new Pair(tags.get(spanHere.start), 1.0));
                        }
                    }
                    if (!deepO.equals(node)) {
                        double deepS = score*AlignmentExtender.scoreExtension(other, deepE, deepO, deepAlignedWordAndProb, graph, we);
                        if (deepS > 0.01) {
                            extensionScores.put(deepO, Math.max(deepS, extensionScores.getDouble(deepO)));
                        }
                    }
                }
            }
        }
        return neighborScoreForProb(node, span, graph, nn2scoredCandidates)
                + extensionScores.object2DoubleEntrySet().stream().map(entry ->
                    neighborScoreForProb(entry.getKey(), span, graph, nn2scoredCandidates)*entry.getDoubleValue()).collect(Collectors.summingDouble(d->d));
    }
    
    private static double neighborScoreSingleNode(String nn, Span span, Map<String, Set<Pair<Span, Double>>> nn2scoredCandidates) {
        double ret = 0.0;
        if (nn2scoredCandidates.containsKey(nn)) {
            for (Pair<Span, Double> scoredC : nn2scoredCandidates.get(nn)) {
                if (scoredC.left.end <= span.start && scoredC.left.end > span.start-SCP_RANGE) {
                    ret += scoredC.right*SCP_NEIGHBOR/Math.pow(2, span.start - scoredC.left.end);
                } else if (scoredC.left.start >= span.end && scoredC.left.start < span.end+SCP_RANGE) {
                    ret += scoredC.right*SCP_NEIGHBOR/Math.pow(SCP_NEIGHBOR_DECAY, scoredC.left.start - span.end);
                }
            }
        }
        return ret;
    }
        
    static double basicScoreProb(WordnetEnumerator we, String word, String label) {
        Set<String> wnLemmas = we.getWNCandidates(word);
        Set<String> closeLabels = FixedNodeToWordRules.getDirectWords(label);
        if (closeLabels.contains(word)) {
            return SCP_PERFECT;
        }
        Set<String> relLabels = FixedNodeToWordRules.getIndirectWords(label);
        Set<String> closeIntersection = Sets.intersection(wnLemmas, closeLabels);
        if (!closeIntersection.isEmpty()) {
            return SCP_PERFECT + SCP_WE_MULT * closeIntersection.stream().map(lemma -> we.scoreRel(word, lemma))
                    .collect(Collectors.maxBy(Comparator.naturalOrder())).get();
        } else {
            Set<String> indirectIntersection = Sets.intersection(wnLemmas, relLabels);
            if (!indirectIntersection.isEmpty()) {
                return (SCP_PERFECT + SCP_WE_MULT * indirectIntersection.stream().map(lemma -> we.scoreRel(word, lemma))
                        .collect(Collectors.maxBy(Comparator.naturalOrder())).get()) * SCP_INDIRECT_NODE_FACTOR;
            } else {
                return SCP_NO_MATCH;
            }
        }
    }
    
    private boolean isWikiNode(String nn) {
        Set<GraphEdge> inEdges = graph.getGraph().incomingEdgesOf(graph.getNode(nn));
        return !inEdges.isEmpty() && inEdges.iterator().next().getLabel().equals("wiki");
    }
    
    static double matchFraction(String label, String word) {
        label = Util.stripSenseSuffix(label);
        String wordWithoutSpecial = word.replaceAll("[,\\.'()]", "");
        if (!word.equals(wordWithoutSpecial)) {
            return Math.max(Util.matchFractionLiteral(label, word), Util.matchFractionLiteral(label, wordWithoutSpecial));
        } else {
            return Util.matchFractionLiteral(label, word);
        }
    }
    
}
