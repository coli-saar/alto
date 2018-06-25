/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtools.aligner;

import com.google.common.collect.Sets;
import de.saar.basic.Pair;
import de.saar.coli.amrtagging.Alignment;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import edu.stanford.nlp.ling.TaggedWord;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates candidate alignments (based on lexical rules and matches).
 * @author Jonas
 */
public class CandidateMatcher {
    
    private final static Set<String> NAME_FILLERS = new HashSet(Arrays.asList(new String[]{"of", "and", "the", "for", "new", "on"}));
    private final static Set<String> NO_LITERAL_MATCH = new HashSet(Arrays.asList(new String[]{"of", "in", "the", "for", "on", "to", "a", "an", "as", "by", "but", "as"}));
    private final static Set<String> PRONOUNS = new HashSet(Arrays.asList(new String[]{"i", "you", "he", "she", "it", "we", "they"}));
    
    /**
     * Creates candidate alignments (based on lexical rules and matches).
     * @param graph
     * @param sent
     * @param tags
     * @param we
     * @return 
     */
    static Pair<Map<String, Set<Alignment>>, Set<Alignment>> findCandidatesForProb(SGraph graph,
            List<String> sent, List<TaggedWord> tags, WordnetEnumerator we) {
        Set<Alignment> ret = new HashSet<>();
        Map<String, Set<Alignment>> nn2als = new HashMap<>();
        Map<String, List<String>> nn2words = new HashMap<>();
        Map<String, IntSet> nn2candidateIndices = new HashMap<>();
        for (GraphNode node : graph.getGraph().vertexSet()) {
            //look up handwritten rules (includes literal), to generate candidates
            List<String> wordsHere = new ArrayList<>();
            nn2words.put(node.getName(), wordsHere);
            wordsHere.addAll(FixedNodeToWordRules.getIndirectWords(node.getLabel()));
            nn2candidateIndices.put(node.getName(), new IntOpenHashSet());//just setup
        }
        
        
        //check wordnet list to generate matches; this includes exact matches of the fixed node to word rules
        for (int j = 0; j<sent.size(); j++) {
            String word = sent.get(j);
            Set<String> candidates = we.getWNCandidates(word);
            //collect all matches
            for (String nn : nn2words.keySet()) {
                boolean isWiki = false;
                for (GraphEdge e : graph.getGraph().incomingEdgesOf(graph.getNode(nn))) {
                    if (e.getLabel().equals("wiki")) {
                        isWiki = true;
                    }
                }
                if (!isWiki) {//never align wiki nodes directly
                    for (String nnWord : nn2words.get(nn)) {
                        //exact wordnet matches (also includes direct matches, since we.getWNCandidates(word)) always includes the word itself
                        if (!nnWord.equals("name")) {
                            for (String c : candidates) {
                                if (nnWord.equals(c)) {//TODO maybe allow loss at start, c.f. make sure -> ensure-0x
                                    nn2candidateIndices.get(nn).add(j);
                                }
                            }
                        }
                        //non-exact direct matches
                        if (!oneIsTooShort(nnWord, word) &&
                                        containsLiteralMatchModSuffix(word, nnWord) || containsLiteralMatchModSuffix(nnWord, word)) {
                            nn2candidateIndices.get(nn).add(j);
                        }

                    }
                }
            }
        }
        Set<String> untouchedNns = new HashSet<>(graph.getGraph().vertexSet().stream().map(node -> node.getName()).collect(Collectors.toSet()));
        //get name and date spans and match multi-sentence if appropriate.
        for (GraphNode node : graph.getGraph().vertexSet()) {
            //exact and fuzzy matches for named entities
            if (Util.isNameNode(node, graph)) {
                boolean firstExact = true;
                boolean firstAny = true;
                Set<Alignment> alsHere = new HashSet<>();
                Set<String> allNnsHere = new HashSet<>(Util.nameNns(node, graph));//copy for safety, since we modify it.
                String wikiNn = Util.getWikiNn(node, graph);
                if (wikiNn != null) {
                    allNnsHere.add(wikiNn);
                }
                List<String> nameNodesInOrder = Util.getNameNodesInOrder(node, graph);
                for (int i = 0; i<sent.size(); i++) {
                    int skipped = 0;
                    int specialSkippedTokens = 0;
                    double matches = 1.0;
                    for (int j = 0; j<nameNodesInOrder.size(); j++) {
                        String token;
                        if (i+j-skipped+specialSkippedTokens < sent.size()) {
                            token = sent.get(i+j-skipped+specialSkippedTokens);
                        } else {
                            skipped++;
                            continue;
                        }
                        String label = graph.getNode(nameNodesInOrder.get(j)).getLabel();
                        if (token.length() == 1 && !token.matches("[a-zA-Z]") && label.startsWith(token)) {
                            specialSkippedTokens++;
                            if (i+j-skipped+specialSkippedTokens < sent.size()) {
                                token = sent.get(i+j-skipped+specialSkippedTokens);
                            } else {
                                skipped++;
                                continue;
                            }
                        }
                        if (nn2candidateIndices.get(nameNodesInOrder.get(j)).contains(i+j-skipped+specialSkippedTokens)) {
                            matches *= AlignmentScorer.matchFraction(label, token);
                        } else {
                            skipped++;
                        }
                        if (i+j-skipped+specialSkippedTokens+1 < sent.size()) {
                            String lookAhead = sent.get(i+j-skipped+specialSkippedTokens+1);
                            if (lookAhead.length() == 1 && !lookAhead.matches("[a-zA-Z]") && label.endsWith(lookAhead)) {
                                specialSkippedTokens++;
                            }
                        }
                    }
                    int stopIndex = i+nameNodesInOrder.size()-skipped+specialSkippedTokens;
                    if (stopIndex > i && (stopIndex != i+1 || !NAME_FILLERS.contains(sent.get(i)))) {
                        double totalMatchFrac = matches*((double)nameNodesInOrder.size()-(double)skipped)/(double)nameNodesInOrder.size();
                        double score = AlignmentScorer.SCP_SPECIAL;
                        if (totalMatchFrac >= 0.999999) {
                            if (firstExact) {
                                firstExact = false;
                                score += AlignmentScorer.SCP_FIRST_BONUS;
                            }
                        } else if (firstAny) {
                            firstAny = false;
                            score += AlignmentScorer.SCP_FIRST_BONUS;
                        }
                        
                        Alignment al = new Alignment(allNnsHere, new Alignment.Span(i, stopIndex), Collections.singleton(node.getName()), 0, totalMatchFrac*score);
                        alsHere.add(al);
                        ret.add(al);
                    }
                }
                
                //abbreviations
                if (nameNodesInOrder.size()>1) {
                    String abbreviation = nameNodesInOrder.stream().map(n ->
                        graph.getNode(n).getLabel().substring(0, Math.min(graph.getNode(n).getLabel().length(),1))).collect(Collectors.joining());
                    for (int i = 0; i<sent.size(); i++) {
                        if (sent.get(i).toLowerCase().equals(abbreviation.toLowerCase())) {
                            Alignment al = new Alignment(allNnsHere, new Alignment.Span(i, i+1), Collections.singleton(node.getName()), 0,
                                AlignmentScorer.SCP_PERFECT * AlignmentScorer.matchFraction(abbreviation, sent.get(i)));//TODO: first bonus
                            alsHere.add(al);
                            ret.add(al);
                        } else if (sent.get(i).replaceAll(",", "").replaceAll("\\.", "").toLowerCase().equals(abbreviation.toLowerCase())) {
                            Alignment al = new Alignment(allNnsHere, new Alignment.Span(i, i+1), Collections.singleton(node.getName()), 0,
                                AlignmentScorer.SCP_PERFECT * AlignmentScorer.matchFraction(abbreviation, sent.get(i).replaceAll(",", "").replaceAll("\\.", "")));//TODO: first bonus
                            alsHere.add(al);
                            ret.add(al);
                        }
                    }
                }
                // ###### special maps
                //America -> United States
                if (nameNodesInOrder.size() == 2 && graph.getNode(nameNodesInOrder.get(0)).getLabel().equals("United")
                        && graph.getNode(nameNodesInOrder.get(1)).getLabel().equals("States")) {
                    for (int i = 0; i<sent.size(); i++) {
                        if (sent.get(i).toLowerCase().startsWith("america")) {
                            Alignment al = new Alignment(allNnsHere, new Alignment.Span(i, i+1), Collections.singleton(node.getName()), 0,
                                AlignmentScorer.SCP_PERFECT * AlignmentScorer.matchFraction("America", sent.get(i)));//TODO: first bonus
                            alsHere.add(al);
                            ret.add(al );
                        }
                    }
                }
                //French->France
                if (nameNodesInOrder.size() == 1 && graph.getNode(nameNodesInOrder.get(0)).getLabel().equals("France")) {
                    for (int i = 0; i<sent.size(); i++) {
                        if (sent.get(i).toLowerCase().startsWith("french")) {
                            Alignment al = new Alignment(allNnsHere, new Alignment.Span(i, i+1), Collections.singleton(node.getName()), 0,
                                AlignmentScorer.SCP_PERFECT * AlignmentScorer.matchFraction("French", sent.get(i)));//TODO: first bonus
                            alsHere.add(al);
                            ret.add(al );
                        }
                    }
                }
                //Great Britain / British -> Great Britain
                if ((nameNodesInOrder.size() == 2 && graph.getNode(nameNodesInOrder.get(0)).getLabel().equals("Great")
                        && graph.getNode(nameNodesInOrder.get(1)).getLabel().equals("Britain"))) {
                    for (int i = 0; i<sent.size(); i++) {
                        if (sent.get(i).toLowerCase().equals("britain")) {
                            Alignment al = new Alignment(allNnsHere, new Alignment.Span(i, i+1), Collections.singleton(node.getName()), 0,
                                AlignmentScorer.SCP_PERFECT * AlignmentScorer.matchFraction("Britain", sent.get(i)));//TODO: first bonus
                            alsHere.add(al);
                            ret.add(al );
                        } else if (sent.get(i).toLowerCase().equals("british")) {
                            Alignment al = new Alignment(allNnsHere, new Alignment.Span(i, i+1), Collections.singleton(node.getName()), 0,
                                AlignmentScorer.SCP_PERFECT * AlignmentScorer.matchFraction("British", sent.get(i)));//TODO: first bonus
                            alsHere.add(al);
                            ret.add(al );
                        }
                    }
                }
                //Great Britain / British -> Great Britain
                if (nameNodesInOrder.size() == 1 && graph.getNode(nameNodesInOrder.get(0)).getLabel().equals("Islam")) {
                    for (int i = 0; i<sent.size(); i++) {
                        if (sent.get(i).toLowerCase().startsWith("muslim")) {
                            Alignment al = new Alignment(allNnsHere, new Alignment.Span(i, i+1), Collections.singleton(node.getName()), 0,
                                AlignmentScorer.SCP_PERFECT * AlignmentScorer.matchFraction("Muslim", sent.get(i)));//TODO: first bonus
                            alsHere.add(al);
                            ret.add(al );
                        }
                    }
                }
                if (!alsHere.isEmpty()) {
                    untouchedNns.removeAll(allNnsHere);
                    nn2als.put(node.getName(), alsHere);
                }
                //TODO: abbreviations
            //date entity patterns
            } else if (Util.matchesDatePattern(node, graph)) {
                Set<Alignment> alsHere = new HashSet<>();
                Set<String> allNnsHere = Util.dateNns(node, graph);
                //single-token patterns
                for (String pattern : Util.datePatterns(node, graph)) {
                    for (int i = 0; i<sent.size(); i++) {
                        if (sent.get(i).equals(pattern)) {
                            Alignment al = new Alignment(allNnsHere, new Alignment.Span(i, i+1), Collections.singleton(node.getName()), 0, AlignmentScorer.SCP_SPECIAL);
                            ret.add(al);
                            alsHere.add(al);
                            untouchedNns.removeAll(allNnsHere);
                        }
                    }
                }
                //spans
                boolean doSpans = false;
                if (doSpans) {
                    List<String> targetNns = new ArrayList(Sets.difference(allNnsHere, Collections.singleton(node.getName())));
                    for (int i = 0; i<sent.size(); i++) {
                        for (int j = i+1; j<= sent.size(); j++) {
                            switch (targetNns.size()) {
                                case 1:
                                    if (j == i+1 && nn2candidateIndices.get(targetNns.get(0)).contains(i)) {
                                        String label = graph.getNode(targetNns.get(0)).getLabel();
                                        Alignment al = new Alignment(allNnsHere, new Alignment.Span(i, i+1), Collections.singleton(node.getName()), 0,
                                            AlignmentScorer.SCP_SPECIAL*AlignmentScorer.matchFraction(label, sent.get(i)));
                                        ret.add(al);
                                        alsHere.add(al);
                                    }
                                    break;
                                case 2:
                                    double best = Double.MIN_VALUE;
                                    String nn0 = targetNns.get(0);
                                    String nn1 = targetNns.get(1);
                                    if (j > i+1 && nn2candidateIndices.get(nn0).contains(i) && nn2candidateIndices.get(nn1).contains(j-1)) {
                                        best = Math.max(best, matchDateSpan2(sent.get(i), sent.get(j-1),
                                                graph.getNode(nn0).getLabel(), graph.getNode(nn1).getLabel())/Math.pow(2, (j-i)-2));
                                    }//these are not exclusive
                                    if (j > i+1 && nn2candidateIndices.get(targetNns.get(1)).contains(i) && nn2candidateIndices.get(targetNns.get(0)).contains(j-1)) {
                                        best = Math.max(best, matchDateSpan2(sent.get(i), sent.get(j-1),
                                                graph.getNode(nn1).getLabel(), graph.getNode(nn0).getLabel())/Math.pow(2, (j-i)-2));
                                    }
                                    if (best != Double.MIN_VALUE) {
                                        Alignment al2 = new Alignment(allNnsHere, new Alignment.Span(i, j), Collections.singleton(node.getName()), 0, best);
                                        ret.add(al2);
                                        alsHere.add(al2);
                                    }
                                    break;
                                case 3:
                                    double best3 = Double.MIN_VALUE;
                                    List<String> subSent = sent.subList(i, j);
                                    nn0 = targetNns.get(0);
                                    nn1 = targetNns.get(1);
                                    String nn2 = targetNns.get(2);
                                    if (j > i+2 && nn2candidateIndices.get(targetNns.get(0)).contains(i) && nn2candidateIndices.get(targetNns.get(1)).contains(j-1)
                                            && containsInside(nn2candidateIndices.get(targetNns.get(2)), i, j)) {
                                        best3 = Math.max(matchDateSpan3(subSent, graph.getNode(nn0).getLabel(), graph.getNode(nn1).getLabel(),
                                                graph.getNode(nn2).getLabel()) / Math.pow(2, (j-i)-3), best3);
                                    }
                                    if (j > i+2 && nn2candidateIndices.get(targetNns.get(0)).contains(i) && nn2candidateIndices.get(targetNns.get(2)).contains(j-1)
                                            && containsInside(nn2candidateIndices.get(targetNns.get(1)), i, j)) {
                                        best3 = Math.max(matchDateSpan3(subSent, graph.getNode(nn0).getLabel(), graph.getNode(nn2).getLabel(),
                                                graph.getNode(nn1).getLabel()) / Math.pow(2, (j-i)-3), best3);
                                    }
                                    if (j > i+2 && nn2candidateIndices.get(targetNns.get(1)).contains(i) && nn2candidateIndices.get(targetNns.get(0)).contains(j-1)
                                            && containsInside(nn2candidateIndices.get(targetNns.get(2)), i, j)) {
                                        best3 = Math.max(matchDateSpan3(subSent, graph.getNode(nn1).getLabel(), graph.getNode(nn0).getLabel(),
                                                graph.getNode(nn2).getLabel()) / Math.pow(2, (j-i)-3), best3);
                                    }
                                    if (j > i+2 && nn2candidateIndices.get(targetNns.get(1)).contains(i) && nn2candidateIndices.get(targetNns.get(2)).contains(j-1)
                                            && containsInside(nn2candidateIndices.get(targetNns.get(0)), i, j)) {
                                        best3 = Math.max(matchDateSpan3(subSent, graph.getNode(nn1).getLabel(), graph.getNode(nn2).getLabel(),
                                                graph.getNode(nn0).getLabel()) / Math.pow(2, (j-i)-3), best3);
                                    }
                                    if (j > i+2 && nn2candidateIndices.get(targetNns.get(2)).contains(i) && nn2candidateIndices.get(targetNns.get(1)).contains(j-1)
                                            && containsInside(nn2candidateIndices.get(targetNns.get(0)), i, j)) {
                                        best3 = Math.max(matchDateSpan3(subSent, graph.getNode(nn2).getLabel(), graph.getNode(nn1).getLabel(),
                                                graph.getNode(nn0).getLabel()) / Math.pow(2, (j-i)-3), best3);
                                    }
                                    if (j > i+2 && nn2candidateIndices.get(targetNns.get(2)).contains(i) && nn2candidateIndices.get(targetNns.get(0)).contains(j-1)
                                            && containsInside(nn2candidateIndices.get(targetNns.get(1)), i, j)) {
                                        best3 = Math.max(matchDateSpan3(subSent, graph.getNode(nn2).getLabel(), graph.getNode(nn0).getLabel(),
                                                graph.getNode(nn1).getLabel()) / Math.pow(2, (j-i)-3), best3);
                                    }
                                    Alignment al3 = new Alignment(allNnsHere, new Alignment.Span(i, j), Collections.singleton(node.getName()), 0, best3);
                                    if (best3 != Double.MIN_VALUE) {
                                        ret.add(al3);
                                        alsHere.add(al3);
                                    }
                                    break;
                            }
                        }
                    }
                }
                if (!alsHere.isEmpty()) {
                    nn2als.put(node.getName(), alsHere);
                    untouchedNns.removeAll(allNnsHere);
                }
            } else if (node.getLabel().equals("multi-sentence")) {
                boolean first = true;
                Set<Alignment> alsHere = new HashSet<>();
                for (int j = 0; j<sent.size()-1; j++) {//note the minus one, don't want to count last token!
                    if (sent.get(j).equals(".") || sent.get(j).equals(";")) {
                        double score = first ? AlignmentScorer.SCP_SPECIAL+AlignmentScorer.SCP_FIRST_BONUS : AlignmentScorer.SCP_SPECIAL;//TODO maybe deterministically only use first?
                        Alignment al = new Alignment(new HashSet<>(Collections.singleton(node.getName())), new Alignment.Span(j, j+1), Collections.singleton(node.getName()), 0, score);
                        first = false;
                        ret.add(al);
                        alsHere.add(al);
                    }
                }
                if (!alsHere.isEmpty()) {
                    nn2als.put(node.getName(), alsHere);
                    untouchedNns.remove(node.getName());
                }
            } else if (PRONOUNS.contains(node.getLabel())) {//pronouns to first occurrence
                for (int j = 0; j<sent.size(); j++) {
                    if (sent.get(j).toLowerCase().equals(node.getLabel())) {
                        Alignment al = new Alignment(new HashSet<>(Collections.singleton(node.getName())), new Alignment.Span(j, j+1),
                                Collections.singleton(node.getName()), 0, AlignmentScorer.SCP_SPECIAL);
                        ret.add(al);
                        nn2als.put(node.getName(), Collections.singleton(al));
                        untouchedNns.remove(node.getName());
                        break;//always align to first match
                    }
                }
            }
        }
        //verbalization matches
//        Map<String, Set<Pair<GraphNode, GraphNode>>> verbalizationMatches = verbalizer.getMatches(graph);
//        for (int j = 0; j<sent.size(); j++) {
//            String word = sent.get(j);
//            Set<String> stems = we.getStems(word);
//            for (String stem : stems) {
//                if (verbalizationMatches.containsKey(stem)) {
//                    for (Pair<GraphNode, GraphNode> baseAndLex : verbalizationMatches.get(stem)) {
//                        //System.err.println("Verbalization match: "+c+"-"+baseAndLex.left.getLabel()+"-"+baseAndLex.right.getLabel());
//                        Set<String> nodes = new HashSet<>();
//                        nodes.add(baseAndLex.left.getName());
//                        nodes.add(baseAndLex.right.getName());
//                        Alignment al = new Alignment(nodes, new Alignment.Span(j, j+1));
//                        ret.put(al, Math.max(ret.getDouble(al), AlignmentScorer.SCP_SPECIAL));//TODO think about where to use max and where not
//                        untouchedNns.removeAll(nodes);
//                        Set<Alignment> alsHere = nn2als.get(baseAndLex.left.getName());
//                        if (alsHere == null) {
//                            alsHere = new HashSet<>();
//                            nn2als.put(baseAndLex.left.getName(), alsHere);
//                        }
//                        alsHere.add(al);
//                    }
//                }
//            }
//        }
        

        Set<String> allNns = new HashSet<>(graph.getGraph().vertexSet().stream().map(node -> node.getName()).collect(Collectors.toSet()));
        //now basic candidates
        // use untouchedNns instead of allNns to not align stuff we covered above. But for now, use allNns, just to
        for (String nn : allNns) {
            Set<Alignment> alsHere = new HashSet<>();
            if (nn2als.containsKey(nn)) {
                alsHere.addAll(nn2als.get(nn));
            }
            for (int c : nn2candidateIndices.get(nn)) {
                Alignment al = new Alignment(nn, c);
                String label = graph.getNode(nn).getLabel();
                    double score = AlignmentScorer.SCP_PERFECT*
                            Math.max(Math.max(AlignmentScorer.matchFraction(label, sent.get(c)),
                                    Util.matchFractionLiteral(sent.get(c), Util.stripSenseSuffix(label))),
                                    AlignmentScorer.basicScoreProb(we, sent.get(c), label));
                    if (tags.get(c).tag().startsWith("VB") && !label.matches(".+-[0-8][0-9]")) {
                        score *= AlignmentScorer.SCP_VERB2NOUN_FACTOR;
                    }
                    al.setWeight(score);
                    ret.add(al);
                    alsHere.add(al);
            }
            nn2als.put(nn, alsHere);//alsHere may be empty here, and only here
        }
        
        
        return new Pair(nn2als, ret);
    }
    
    private static boolean oneIsTooShort(String s1, String s2) {
        return  s1.length() <= 3 || s2.length() <= 3;
    }
        
    
    private static double matchDateSpan2(String word1, String word2, String label1, String label2) {
        double score = AlignmentScorer.SCP_SPECIAL;
        double max = 0;
        for (String word : FixedNodeToWordRules.getDirectWords(label1)) {
            max = Math.max(max, AlignmentScorer.matchFraction(word, word1));
        }
        score *= max;
        //reset for word2
        max = 0;
        for (String word : FixedNodeToWordRules.getDirectWords(label2)) {
            max = Math.max(max, AlignmentScorer.matchFraction(word, word2));
        }
        score *= max;
        return score;
    }
    
    /**
     * label3 is middle
     * @param fullSpan
     * @param label1
     * @param label2
     * @param label3
     * @return 
     */
    private static double matchDateSpan3(List<String> fullSpan, String label1, String label2, String label3) {
        double match2 = matchDateSpan2(fullSpan.get(0), fullSpan.get(fullSpan.size()-1), label1, label2);
        double max = 0;
        for (int i = 1; i<fullSpan.size()-1; i++) {
            for (String word : FixedNodeToWordRules.getDirectWords(label3)) {
                max = Math.max(max, AlignmentScorer.matchFraction(word, fullSpan.get(i)));
            }
        }
        return match2*max;
    }
    
    
    
    private static boolean containsInside(IntSet set, int i, int j) {
        for (int k : set) {
            if (i<k && k<j-1) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean containsLiteralMatchModSuffix(String c, String word) {
        int sublength;
        if (c.length() <= 4) {
            sublength = Math.min(c.length(), 3);
        } else if (c.length() == 5 || c.length() == 6) {
            sublength = 4;
        } else {
            sublength = 5;
        }
        return word.toLowerCase().contains(c.substring(0, sublength).toLowerCase())
                || word.replaceAll(",", "").replaceAll("\\.", "").contains(c.substring(0, sublength).toLowerCase());
    }
    
}
