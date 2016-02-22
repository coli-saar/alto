/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection;

import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.create_automaton.CorpusCreator;
import de.up.ling.irtg.rule_finding.create_automaton.SpecifiedAligner;
import de.up.ling.irtg.rule_finding.create_automaton.StateAlignmentMarking;
import de.up.ling.irtg.rule_finding.data_creation.MakeAlignments;
import de.up.ling.irtg.rule_finding.data_creation.MakeAutomata;
import de.up.ling.irtg.rule_finding.pruning.IntersectionPruner;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class LexicalizedTest {
    /**
     * 
     */
    private IntersectionPruner rbp;
    
    /**
     * 
     */
    private final String leftTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "a b c\n"
            + "d c c c";
    
    /**
     * 
     */
    private final String rightTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "e f g\n"
            + "g z z z";
    
    /**
     * 
     */
    private final String alignments = "0-0 0-1\n"
            + "0-0 1-3 2-1\n";
    
    /**
     * 
     */
    private Iterable<Pair<TreeAutomaton<String>, TreeAutomaton<String>>> automata;
    
    /**
     * 
     */
    private IntersectionPruner ip1;
    
    /**
     * 
     */
    private IntersectionPruner ip2;
    
    @Before
    public void setUp() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, ParserException, ParseException {
        List<ByteArrayOutputStream> treeOutputs = new ArrayList<>();
        Supplier<OutputStream> supp = () -> {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            
            treeOutputs.add(result);
            
            return result;
        };
        
        InputStream inTrees = new ByteArrayInputStream(leftTrees.getBytes());
        
        MakeAutomata.create(inTrees, supp);
        List<InputStream> treeInputs = new ArrayList<>();
        for(ByteArrayOutputStream baos : treeOutputs) {
            treeInputs.add(new ByteArrayInputStream(baos.toByteArray()));
        }
        
        Iterable<TreeAutomaton> iter1 = MakeAutomata.reconstruct(treeInputs);
        
        treeOutputs.clear();
        
        inTrees = new ByteArrayInputStream(rightTrees.getBytes());
        
        MakeAutomata.create(inTrees, supp);
        treeInputs = new ArrayList<>();
        for(ByteArrayOutputStream baos : treeOutputs) {
            treeInputs.add(new ByteArrayInputStream(baos.toByteArray()));
        }
        
        Iterable<TreeAutomaton> iter2 = MakeAutomata.reconstruct(treeInputs);
        
        treeOutputs.clear();
        InputStream align = new ByteArrayInputStream(alignments.getBytes());
        
        MakeAlignments.makeStringFromStandardAlign(align, supp, false);
        
        List<InputStream> align1 = new ArrayList<>();
        for(ByteArrayOutputStream baos :treeOutputs) {
            align1.add(new ByteArrayInputStream(baos.toByteArray()));
        }
        
        treeOutputs.clear();
        align = new ByteArrayInputStream(alignments.getBytes());
        
        MakeAlignments.makeStringFromStandardAlign(align, supp, true);
        
        List<InputStream> align2 = new ArrayList<>();
        for(ByteArrayOutputStream baos : treeOutputs) {
            align2.add(new ByteArrayInputStream(baos.toByteArray()));
        }
        
        Iterable<Pair<TreeAutomaton,StateAlignmentMarking>> pairs1 = () -> {
            return new Iterator<Pair<TreeAutomaton, StateAlignmentMarking>>() {
                /**
                 * 
                 */
                private final Iterator<TreeAutomaton> it = iter1.iterator();
                
                /**
                 * 
                 */
                private final Iterator<InputStream> aligns = align1.iterator();
                
                @Override
                public boolean hasNext() {
                    return it.hasNext() && aligns.hasNext();
                }

                @Override
                public Pair<TreeAutomaton, StateAlignmentMarking> next() {
                    TreeAutomaton t = it.next();
                    
                    SpecifiedAligner spec = null;
                    try {
                        spec = new SpecifiedAligner(t, aligns.next());
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    
                    return new Pair<>(t,spec);
                }
            };
        };
        
        Iterable<Pair<TreeAutomaton,StateAlignmentMarking>> pairs2 = () -> {
            return new Iterator<Pair<TreeAutomaton, StateAlignmentMarking>>() {
                /**
                 * 
                 */
                private final Iterator<TreeAutomaton> it = iter2.iterator();
                
                /**
                 * 
                 */
                private final Iterator<InputStream> aligns = align2.iterator();
                
                @Override
                public boolean hasNext() {
                    return it.hasNext() && aligns.hasNext();
                }

                @Override
                public Pair<TreeAutomaton, StateAlignmentMarking> next() {
                    TreeAutomaton t = it.next();
                    
                    SpecifiedAligner spec = null;
                    try {
                        spec = new SpecifiedAligner(t, aligns.next());
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    
                    return new Pair<>(t,spec);
                }
            };
        };
        
        CorpusCreator corpCreat = new CorpusCreator(null,null,2);
        
        automata = corpCreat.pushAlignments(pairs1, pairs2);
        
        ip1 = new IntersectionPruner(IntersectionOptions.NO_EMPTY,IntersectionOptions.RIGHT_BRANCHING_NORMAL_FORM);
        ip2 = new IntersectionPruner(IntersectionOptions.NO_EMPTY,IntersectionOptions.LEXICALIZED);
    }

    @Test
    public void testSomeMethod() throws ParseException {
        Iterator<Pair<TreeAutomaton<String>, TreeAutomaton<String>>> it = this.automata.iterator();
        
        TreeAutomaton t = it.next().getRight();
        TreeAutomaton ta1 = ip1.apply(t);
        
        ta1.normalizeRuleWeights();
        assertEquals(ta1.countTrees(),4);
        
        assertTrue(ta1.accepts(TreeParser.parse("*('__X__{1,2 _@_ 0-2}'(*(e,f)),g)")));
        assertTrue(ta1.accepts(TreeParser.parse("*('__X__{1,2 _@_ 0-2}'(*(e,f)),'__X__{ _@_ 2-3}'(g))")));
        assertTrue(ta1.accepts(TreeParser.parse("*(e,*(f,g))")));
        assertTrue(ta1.accepts(TreeParser.parse("*(e,*(f,'__X__{ _@_ 2-3}'(g)))")));
        
        
        ta1 = ip2.apply(t);
        
        ta1.normalizeRuleWeights();
        assertEquals(ta1.countTrees(),5);
        
        Iterator<Tree<String>> li = ta1.languageIterator();
        
        assertTrue(ta1.accepts(TreeParser.parse("*(*(e,f),g)")));
        assertTrue(ta1.accepts(TreeParser.parse("*('__X__{1,2 _@_ 0-2}'(*(e,f)),g)")));
        assertTrue(ta1.accepts(TreeParser.parse("*(*(e,f),'__X__{ _@_ 2-3}'(g))")));
        assertTrue(ta1.accepts(TreeParser.parse("*(e,*(f,'__X__{ _@_ 2-3}'(g)))")));
        assertTrue(ta1.accepts(TreeParser.parse("*(e,*(f,g))")));
    }
}
