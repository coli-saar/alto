/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtools.datascript;

import com.google.common.collect.Sets;
import de.saar.coli.amrtools.RareWordsAnnotator;
import de.saar.coli.amrtagging.Alignment.Span;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.Counter;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides utility for matching named entities, in particular dates.
 * @author JG
 */
public class TestNER {
    
    public static final String OTHER = "O";
    
    /**
     * Testing script to gain accuracy data for the named entity tagger. Currently
     * uses hardcoded local file paths.
     * @param args
     * @throws IOException
     * @throws ClassCastException
     * @throws ClassNotFoundException
     * @throws CorpusReadingException 
     */
    public static void main(String[] args) throws IOException, ClassCastException, ClassNotFoundException, CorpusReadingException {
        AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier.getClassifier("../../data/stanford-ner-2017-06-09/classifiers/english.conll.4class.distsim.crf.ser.gz");
        
        InterpretedTreeAutomaton dummyIrtg = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton<>());
        Signature dummySignature = new Signature();
        dummyIrtg.addInterpretation(new Interpretation(new StringAlgebra(), new Homomorphism(dummySignature, dummySignature), "string"));
        dummyIrtg.addInterpretation(new Interpretation(new StringAlgebra(), new Homomorphism(dummySignature, dummySignature), "repstring"));
        dummyIrtg.addInterpretation(new Interpretation(new StringAlgebra(), new Homomorphism(dummySignature, dummySignature), "spanmap"));
        Corpus corpus = Corpus.readCorpusWithStrictFormatting(new FileReader("../../experimentData/Corpora/LDC2015E86/corefSplitNDN.corpus"), dummyIrtg);
        
        int match = 0;
        int overlapMatchRecall = 0;
        int overlapMatchPrecision = 0;
        int total = 0;
        int totalPred = 0;
        
        int dateTokens = 0;
        int nrTokens = 0;
        int nrPreds = 0;
        int nrMatches = 0;
        
        for (Instance inst : corpus) {
            String sent = ((List<String>)inst.getInputObjects().get("string")).stream().collect(Collectors.joining(" "));
            List<String> repSent = (List<String>)inst.getInputObjects().get("repstring");
            List<Span> spanMap = ((List<String>)inst.getInputObjects().get("spanmap")).stream().map(s -> new Span(s)).collect(Collectors.toList());
            
            List<List<CoreLabel>> lcls = classifier.classify(sent.replaceAll("[<>]", ""));
            List<CoreLabel> lcl = new ArrayList<>();
            lcls.forEach(l -> lcl.addAll(l));
            
            Set<Span> nerSpans = new HashSet<>();
            int prevIndex = -1;
            String prevCat = "";
            for (int i = 0; i<lcl.size(); i++) {
                CoreLabel cl = lcl.get(i);
                String ner = cl.get(CoreAnnotations.AnswerAnnotation.class);
                if (!ner.equals(OTHER)) {
                    if (prevIndex == -1) {
                        //if we were searching before, now we start.
                        prevIndex = i;
                        prevCat = ner;
                    } else {
                        if (!prevCat.equals(ner)) {
                            //if category switched, save previous span and start new.
                            nerSpans.add(new Span(prevIndex, i));
                            prevIndex = i;
                            prevCat = ner;
                        }
                    }
                } else {
                    if (prevIndex != -1) {
                        //if we were working on a span before, save it and continue searching
                        nerSpans.add(new Span(prevIndex, i));
                        prevIndex = -1;
                    }
                }
            }
            
            Set<Span> gold = new HashSet<>();
            
            for (int i = 0; i<repSent.size(); i++) {
                if (repSent.get(i).equals(RareWordsAnnotator.NAME_TOKEN)) {
                    gold.add(spanMap.get(i));
                }
            }
            
            total += gold.size();
            totalPred += nerSpans.size();
            match += Sets.intersection(gold, nerSpans).size();
            
            for (Span span : gold) {
                for (Span span2 : nerSpans) {
                    if (span.overlaps(span2)) {
                        overlapMatchRecall++;
                        break;
                    }
                }
            }
            
            for (Span span : nerSpans) {
                for (Span span2 : gold) {
                    if (span.overlaps(span2)) {
                        overlapMatchPrecision++;
                        break;
                    }
                }
            }
            
            //dates
            String[] splitSent = sent.split(" ");
            for (int i = 0; i<repSent.size(); i++) {
                if (repSent.get(i).equals(RareWordsAnnotator.DATE_TOKEN)) {
                    dateTokens++;
                } else if (repSent.get(i).equals(RareWordsAnnotator.NUMBER_TOKEN)) {
                    nrTokens++;
                }
                String origToken = splitSent[spanMap.get(i).start];
                int patternID = matchesDatePattern(origToken);
                if (patternID >= 0) {
                    if (repSent.get(i).equals("_DATE_")) {
                        datePatternCorrect.add(patternID);
                    } else {
//                        System.err.println(sent);
                    }
                } else {
                    if (origToken.matches(RareWordsAnnotator.NUMBER_REGEX) && spanMap.get(i).isSingleton()) {
                        nrPreds++;
                        if (repSent.get(i).equals(RareWordsAnnotator.NUMBER_TOKEN)) {
                            nrMatches++;
                        }
                    }
                }
            }
            
            
        }
        
        System.err.println("recall: "+match+"/"+total+" ("+(match/(double)total)+")");
        System.err.println("precision: "+match+"/"+totalPred+" ("+(match/(double)totalPred)+")");
        System.err.println("recall (overlap): "+overlapMatchRecall+"/"+total+" ("+(overlapMatchRecall/(double)total)+")");
        System.err.println("precision (overlap): "+overlapMatchPrecision+"/"+totalPred+" ("+(overlapMatchPrecision/(double)totalPred)+")");
        
        System.err.println("------ DATE -----");
        System.err.println("recall: "+datePatternCorrect.sum()+"/"+dateTokens);
        System.err.println("precision: "+datePatternCorrect.sum()+"/"+datePatternMatches.sum());
        for (String pattern : datePatterns) {
            System.err.println(pattern+": "+datePatternCorrect.get(datePatterns.indexOf(pattern))
                    +"/"+datePatternMatches.get(datePatterns.indexOf(pattern)));
        }
        
        System.err.println("------ NUMBERS -----");
        System.err.println("recall: "+nrMatches+"/"+nrTokens);
        System.err.println("precision: "+nrMatches+"/"+nrPreds);
        
        
//        
//        
//        String sent = "In actual fact , whether it is Barack Obama or any other foreign owned enterprises in China, does not matter .";
//        
//        System.out.println(classifier.classifyWithInlineXML(sent));
//        
//        for (List<CoreLabel> lcl : classifier.classify(sent)) {
//          for (CoreLabel cl : lcl) {
//              System.out.println(cl.get(CoreAnnotations.AnswerAnnotation.class));
//              System.out.println(cl.toShorterString());
//          }
//        }
//        
    }
    
    private static final List<String> datePatterns = new ArrayList<>();
    
    private static final String M = "[0-9]";
    private static final String d = "[0-9]";
    private static final String MM = "[0-1][0-9]";
    private static final String dd = "[0-3][0-9]";
    private static final String yy = "[09][0-9]";
    private static final String yyyy = "[1-2][0-9][0-9][0-9]";
    
    
    static {
//            datePatterns.add(MM+"-"+dd);
//            datePatterns.add(M+"/"+d);
//            datePatterns.add(MM+"/"+dd);
//            datePatterns.add(dd+"/"+MM);
//            datePatterns.add(d+"/"+M);
//            datePatterns.add(d+"[.]"+M);
//            datePatterns.add(dd+"[.]"+MM);
//            datePatterns.add(yyyy+"-"+MM);
//            datePatterns.add(M+"/"+yy);
//            datePatterns.add(MM+"/"+yyyy);
//            datePatterns.add(M+"/"+yyyy);
//            datePatterns.add(MM+"/"+yy);
//            datePatterns.add(M+"[.]"+yy);
//            datePatterns.add(MM+"[.]"+yyyy);
//            datePatterns.add(M+"[.]"+yyyy);
//            datePatterns.add(MM+"[.]"+yy);
            datePatterns.add(yyyy+"-"+MM+"-"+dd);
//            datePatterns.add(dd+"[.]"+MM+"[.]"+yy);
//            datePatterns.add(d+"[.]"+M+"[.]"+yy);
//            datePatterns.add(dd+"[.]"+MM+"[.]"+yyyy);
//            datePatterns.add(d+"[.]"+M+"[.]"+yyyy);
            datePatterns.add(MM+"/"+dd+"/"+yy);
//            datePatterns.add(M+"/"+d+"/"+yy);
//            datePatterns.add(MM+"/"+dd+"/"+yyyy);
//            datePatterns.add(M+"/"+d+"/"+yyyy);
//            datePatterns.add(dd+"/"+MM+"/"+yyyy);
//            datePatterns.add(d+"/"+M+"/"+yyyy);
//            datePatterns.add(dd+"/"+MM+"/"+yy);
//            datePatterns.add(d+"/"+M+"/"+yy);
            datePatterns.add(yy+MM+dd);
    }
    
    private static Counter<Integer> datePatternMatches = new Counter<>();
    private static Counter<Integer> datePatternCorrect = new Counter<>();
    
    /**
     * Whether a token matches a fixed list of regular expressions for date patterns.
     * @param token
     * @return 
     */
    public static int matchesDatePattern(String token) {
        for (int i = 0; i<datePatterns.size(); i++) {
            if (token.matches(datePatterns.get(i))) {
                datePatternMatches.add(i);
                return i;
            }
        }
        return -1;
    }
    
}
