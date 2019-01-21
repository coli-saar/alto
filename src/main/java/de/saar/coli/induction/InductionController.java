/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.induction;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Function;

/**
 *
 * @author christoph_teichmann
 */
public class InductionController {
    
    public static void main(String... args) throws IOException {
        InputStream in = new FileInputStream(args[0]);
        Properties props = new Properties();
        props.load(in);
        
        String automataFolderString = props.getProperty("automataFolderString");
        String automataFolderSemantic = props.getProperty("automataFolderSemantics");
        
        String alignmentFolderString = props.getProperty("alignmentFolderString");
        String alignmentFolderSemantic = props.getProperty("alignmentFolderSemantic");
        
        String nonterminalFolderString = props.getProperty("nonterminalFolderString");
        String nonterminalFolderSemantic = props.getProperty("nonterminalFolderSemantic");
        
        String mainSignatureLocationString = props.getProperty("mainSignatureLocationStrings");
        String mainSignatureLocationSemantics = props.getProperty("mainSignatureLocationSemantics");
        
        // Iterator over triples of tree automaton, assignment from (string) states to
        // alignment markers and assignment from (string) states to 
        Iterable<Pair<TreeAutomaton<String>,Pair<Function<String,IntSet>,Function<String,String>>>> stringData =
                    makeDataIterable(automataFolderString,alignmentFolderString,nonterminalFolderString,mainSignatureLocationString);
        Iterable<Pair<TreeAutomaton<String>,Pair<Function<String,IntSet>,Function<String,String>>>> semanticData =
                    makeDataIterable(automataFolderString,alignmentFolderString,nonterminalFolderString,mainSignatureLocationSemantics);
        
        // First step (reading in data) is done, no we work on computing the 
        String stringRestrictionSpecification = props.getProperty("restrictionSpecifictionString");
        String semanticRestrictionSpecification = props.getProperty("restrictionSpecificationSemantic");
        
        stringData = makeRestrictedIterable(stringData,stringRestrictionSpecification);
        
        
        
        
        // TODO
    }

    /**
     * 
     * @param automataFolderString
     * @param alignmentFolderString
     * @param nonterminalFolderString
     * @param mainSignatureLocationString
     * @return 
     */
    private static Iterable<Pair<TreeAutomaton<String>, Pair<Function<String, IntSet>, Function<String, String>>>> makeDataIterable(String automataFolderString, String alignmentFolderString, String nonterminalFolderString, String mainSignatureLocationString) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * 
     * @param stringData
     * @param stringRestrictionSpecification
     * @return 
     */
    private static Iterable<Pair<TreeAutomaton<String>, Pair<Function<String, IntSet>, Function<String, String>>>> makeRestrictedIterable(Iterable<Pair<TreeAutomaton<String>, Pair<Function<String, IntSet>, Function<String, String>>>> stringData, String stringRestrictionSpecification) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
