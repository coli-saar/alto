/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtools.aligner;

import com.google.common.collect.Sets;
import de.up.ling.irtg.corpus.CorpusReadingException;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.data.ILoadPolicy;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import edu.mit.jwi.morph.WordnetStemmer;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enumerates words that are closely connected in WordNet for a given word, given some conditions.
 * @author Jonas
 */
public class WordnetEnumerator {
    
//    public static final Pointer[] ALLOWED_WORD_RELATIONS = new Pointer[]{Pointer.PERTAINYM, Pointer.PARTICIPLE
//            , Pointer.DERIVATIONALLY_RELATED, Pointer.DERIVED_FROM_ADJ};
//    public static final Pointer[] ALLOWED_SYNSET_RELATIONS = new Pointer[]{Pointer.SIMILAR_TO, Pointer.ATTRIBUTE};//Pointer.HYPERNYM};
    
    public static final Set<Pointer> GOOD_POINTERS = new HashSet<>(Arrays.asList(Pointer.PERTAINYM, Pointer.PARTICIPLE
            , Pointer.DERIVATIONALLY_RELATED, Pointer.DERIVED_FROM_ADJ, Pointer.SIMILAR_TO, Pointer.ATTRIBUTE));
    
    private final WordnetStemmer stemmer;
    private final RAMDictionary dict;
    private final Map<String, Object2DoubleMap<String>> wordPairScores;
    
    WordnetEnumerator(String dictPath) throws IOException {
        this.wordPairScores = new HashMap<>();
        URL url = new URL("file", null, dictPath);
        
        dict = new RAMDictionary(url, ILoadPolicy.BACKGROUND_LOAD);
        dict.open();
        //dict.load(true);//do this when aligning full corpus
        
        stemmer = new WordnetStemmer(dict);
    }
    
    
    private final static int SEARCH_DEPTH = 3;
    private final static double BAD_COST = 1.2;
    private final static double GOOD_COST = 0.2;
    private final static double COST_THRESHOLD = 1.8;//with current SEARCH_DEPTH, worst score with only one BAD_COST is 1.6, so use this to test for more than 1 BAD_COST.
    
    Set<String> getStems(String word) {
        Set<String> ret = new HashSet<>();
        for (POS pos : POS.values()) {
            for (String stem : stemmer.findStems(word, pos)) {
                ret.add(stem);
            }
        }
        return ret;
    }
    
    
    Set<String> getWNCandidates(String word) {
        if (wordPairScores.containsKey(word)) {
            return wordPairScores.get(word).keySet();
        }
        Set<String> ret;
        Object2DoubleMap lemma2score = new Object2DoubleOpenHashMap();
        lemma2score.defaultReturnValue(-1000);
        wordPairScores.put(word, lemma2score);
        if (word.matches("[0-9,.]+")) {
            ret = new HashSet<>();
            String noCommas = word.replaceAll("[.,]", "");
            while (noCommas.endsWith("0")) {
                ret.add(noCommas);
                noCommas = noCommas.substring(0, noCommas.length()-1);
            }
            if (noCommas.length() > 0) {
                ret.add(noCommas);
            }
        } else {
            Set<IWord> iWords = new HashSet<>();
            for (POS pos : POS.values()) {
                try {
                    for (String stem : stemmer.findStems(word, pos)) {
                        IIndexWord idxWord = dict.getIndexWord(stem, pos);
                        if (idxWord != null) {
                            for (IWordID wordID : idxWord.getWordIDs()) {
                                iWords.add(dict.getWord(wordID));
                            }
                        }
                    }
                } catch (java.lang.IllegalArgumentException ex) {
                    System.err.println("*** WARNING *** "
                            + de.up.ling.irtg.util.Util.getStackTrace(ex));
                }
            }
            Object2DoubleMap<IWord> foundCosts = new Object2DoubleOpenHashMap<>();
            foundCosts.defaultReturnValue(Double.MAX_VALUE);
            for (IWord iW : iWords) {
                foundCosts.put(iW, 0);
            }
            Set<IWord> explored = new HashSet<>();
            for (int k = 0; k<SEARCH_DEPTH; k++) {
                for (IWord iW : new HashSet<>(Sets.difference(foundCosts.keySet(), explored ))) {
                    explored.add(iW);
                    double costIW = foundCosts.getDouble(iW);
                    for (Pointer p : Pointer.values()) {
                        iW.getRelatedWords(p).stream().map(id -> dict.getWord(id)).forEach(newIWord -> {
                            double newCost = costIW + (GOOD_POINTERS.contains(p) ? GOOD_COST : BAD_COST);
                            if (newCost < COST_THRESHOLD) {
                                foundCosts.put(newIWord, Math.min(foundCosts.getDouble(newIWord), newCost));
                            }

                        });
                        iW.getSynset().getRelatedSynsets(p).stream().map(id -> dict.getSynset(id)).forEach(syn -> {
                            for (IWord synW : syn.getWords()) {
                                double newCost = costIW + (GOOD_POINTERS.contains(p) ? GOOD_COST : BAD_COST);
                                if (newCost < COST_THRESHOLD) {
                                    foundCosts.put(synW, Math.min(foundCosts.getDouble(synW), newCost));
                                }
                            }
                        });
                    }
                    for (IWord synW : iW.getSynset().getWords()) {
                        if (!synW.equals(iW)) {
                            double newCost = costIW + BAD_COST;
                            if (newCost < COST_THRESHOLD) {
                                foundCosts.put(synW, Math.min(foundCosts.getDouble(synW), newCost));
                            }
                        }
                    }
                }
            }
            for (IWord iW : foundCosts.keySet()) {
                lemma2score.put(iW.getLemma(), -foundCosts.getDouble(iW));
            }
            ret = foundCosts.keySet().stream().map(iWord -> iWord.getLemma()).collect(Collectors.toSet());
        }
        //add the word itself//TODO think about whether we want that -- I think yes, to capture e.g. pronouns
        ret.add(word.toLowerCase());
        lemma2score.put(word.toLowerCase(), 0);
        return ret;
    }
    
    /**
     * returns the score of the path from a word (as occuring in the sentence) to
     * a related lemma. If no relation is found, -1000 is returned. (large enough
     * to make score really bad, but safely away from overflow). Returns a negative value,
     * lower is worse. Current range is from 0 to -1.4
     * @param word
     * @param lemma2check
     * @return 
     */
    double scoreRel(String word, String lemma2check) {
        if (!wordPairScores.containsKey(word)) {
            getWNCandidates(word);//this will fill add an entry for word to the wordPairScores, and set its default return value.
        }
        return wordPairScores.get(word).getDouble(lemma2check);
    }
    
    /**
     * For testing, first argument is the path to wordnet 3.0 (the dict folder), second a word, and optionally
     * third a second word. Prints near neighbors of the first word and, if applicable, shortest paths to the second word.
     * @param args
     * @throws IOException
     * @throws MalformedURLException
     * @throws InterruptedException 
     */
    public static void main(String[] args) throws IOException, MalformedURLException, InterruptedException {
        
        
        
        WordnetEnumerator we = new WordnetEnumerator(args[0]);
        String word = args[1];
        for (POS pos : POS.values()) {
            System.out.println(pos.toString()+": "+we.stemmer.findStems(word, pos));
        }
        System.out.println(we.getWNCandidates(word));
        System.out.println(we.getAllNounHypernyms(word));
        if (args.length > 2) {
            String word2 = args[2];
            Set<IWord> iWords1 = new HashSet<>();
            for (POS pos : POS.values()) {
                for (String stem : we.stemmer.findStems(word, pos)) {
                    IIndexWord idxWord = we.dict.getIndexWord(stem, pos);
                    if (idxWord != null) {
                        for (IWordID wordID : idxWord.getWordIDs()) {
                            iWords1.add(we.dict.getWord(wordID));
                        }
                    }
                }
            }
            Set<IWord> iWords2 = new HashSet<>();
            for (POS pos : POS.values()) {
                for (String stem : we.stemmer.findStems(word2, pos)) {
                    IIndexWord idxWord = we.dict.getIndexWord(stem, pos);
                    if (idxWord != null) {
                        for (IWordID wordID : idxWord.getWordIDs()) {
                            iWords2.add(we.dict.getWord(wordID));
                        }
                    }
                }
            }
            Map<IWord, Set<List<String>>> found = new HashMap<>();
            for (IWord iW : iWords1) {
                addPath(iW, new ArrayList<>(), found);
            }
            for (int k = 0; k<5; k++) {
                Set<IWord> newWords = new HashSet<>();
                for (IWord iW : new HashSet<>(found.keySet())) {
                    for (Pointer p : Pointer.values()) {
                        iW.getRelatedWords(p).stream().map(id -> we.dict.getWord(id)).forEach(newIWord -> {
                            newWords.add(newIWord);
                            for (List<String> path : found.get(iW)) {
                                List<String> newList = new ArrayList<>(path);
                                newList.add(p.toString()+"_"+newIWord.getLemma());
                                addPath(newIWord, newList, found);
                            }
                        });
                        iW.getSynset().getRelatedSynsets(p).stream().map(id -> we.dict.getSynset(id)).forEach(syn -> {
                            for (IWord synW : syn.getWords()) {
                                newWords.add(synW);
                                for (List<String> path : found.get(iW)) {
                                    List<String> newList = new ArrayList<>(path);
                                    newList.add("SYN|"+p.toString()+"_"+synW.getLemma());
                                    addPath(synW, newList, found);
                                }
                            }
                        });
                    }
                    for (IWord synW : iW.getSynset().getWords()) {
                        if (!synW.equals(iW)) {
                            newWords.add(synW);
                            for (List<String> path : found.get(iW)) {
                                List<String> newList = new ArrayList<>(path);
                                newList.add("SYNSET"+"_"+synW.getLemma());
                                addPath(synW, newList, found);
                            }
                        }
                    }
                }
                if (!Sets.intersection(newWords, iWords2).isEmpty()) {
                    break;
                }
            }      
            for (IWord iW : iWords2) {
                if (found.get(iW) != null) {
                    for (List<String> path : found.get(iW)) {
                        System.err.println(path);
                    }
                }
            }
            //System.err.println(found);
        }
        
        
        
    }
    
    private static void addPath(IWord iW, List<String> path, Map<IWord, Set<List<String>>> found) {
        Set<List<String>> foundHere = found.get(iW);
        if (foundHere == null) {
            foundHere = new HashSet<>();
            foundHere.add(path);
            found.put(iW, foundHere);
        } else {
            int min = foundHere.stream().map(list -> list.size()).collect(Collectors.minBy(Comparator.naturalOrder())).get();
            if (path.size() < min) {
                foundHere.clear();
                foundHere.add(path);
            } else if (path.size() == min) {
                foundHere.add(path);
            }
        }
    }
    
    private final Map<String, Set<String>> word2Hypers = new HashMap<>();
    
    Set<String> getAllNounHypernyms(String word) {
        if (word2Hypers.containsKey(word)) {
            return word2Hypers.get(word);
        }
        Set<String> ret = new HashSet<>();
        for (String stem : stemmer.findStems(word, POS.NOUN)) {
            IIndexWord idxWord = dict.getIndexWord(stem, POS.NOUN);
            if (idxWord != null) {
                for (IWordID wordID : dict.getIndexWord(stem, POS.NOUN).getWordIDs()) {
                    List<ISynsetID> hypers = dict.getWord(wordID).getSynset().getRelatedSynsets(Pointer.HYPERNYM);
                    while (!hypers.isEmpty()) {
                        List<ISynsetID> newHypers = new ArrayList<>();
                        for (ISynsetID synID : hypers) {
                            ISynset syn = dict.getSynset(synID);
                            newHypers.addAll(syn.getRelatedSynsets(Pointer.HYPERNYM));
                            ret.addAll(syn.getWords().stream().map(iWord -> iWord.getLemma()).collect(Collectors.toList()));
                        }
                        hypers = newHypers;
                    }
                }
            }
        }  
        return ret;
    }
    
    
//    public Set<String> getWNCandidates(String word) {
//        Set<IWord> wordS = new HashSet<>();
//        for (POS pos : POS.values()) {
//            List<String> stems = stemmer.findStems(word, pos);
//            for (String stem : stems) {
//                IIndexWord idxWord = dict.getIndexWord(stem, pos);
//                if (idxWord == null) {
//                    //System.err.println("stem lookup returned NULL! for stem "+stem);
//                } else {
//                    for (IWordID wordID : idxWord.getWordIDs()) {
//                        IWord wnWord = dict.getWord(wordID);
//                        ISynset synset = wnWord.getSynset();
//                        Set<ISynset> synS = new HashSet<>();
//                        synS.add(synset);
//                        for (Pointer p : ALLOWED_SYNSET_RELATIONS) {
//                            for (ISynset rel : synset.getRelatedSynsets(p).stream().map(id -> dict.getSynset(id)).collect(Collectors.toList())) {
//                                synS.add(rel);
//                            }
//                        }
//                        for (ISynset s : synS) {
//                            for (IWord synW : s.getWords()) {
//                                wordS.add(synW);
//                                for (Pointer p : ALLOWED_WORD_RELATIONS) {
//                                    for (IWord rel : synW.getRelatedWords(p).stream().map(id -> dict.getWord(id)).collect(Collectors.toList())) {
//                                        if (!pos.equals(POS.VERB) || rel.getPOS().equals(POS.VERB)) {//for verbs, only allow verbs
//                                            wordS.add(rel);
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        Set<String> ret = wordS.stream().map(w -> w.getLemma()).collect(Collectors.toSet());
//        ret.add(word.toLowerCase());
//        return ret;
//    }
//    
//    public Set<String> getCloseWNCandidates(String word) {
//        Set<IWord> wordS = new HashSet<>();
//        for (POS pos : POS.values()) {
//            List<String> stems = stemmer.findStems(word, pos);
//            for (String stem : stems) {
//                IIndexWord idxWord = dict.getIndexWord(stem, pos);
//                if (idxWord == null) {
//                    //System.err.println("stem lookup returned NULL! for stem "+stem);
//                } else {
//                    for (IWordID wordID : idxWord.getWordIDs()) {
//                        IWord wnWord = dict.getWord(wordID);
//                        ISynset synset = wnWord.getSynset();
//                        for (IWord synW : synset.getWords()) {
//                            wordS.add(synW);
//                        }
//                    }
//                }
//            }
//        }
//        Set<String> ret = wordS.stream().map(w -> w.getLemma()).collect(Collectors.toSet());
//        ret.add(word.toLowerCase());
//        return ret;
//    }
}
