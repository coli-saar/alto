/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.data_creation.MakeAlignments;
import de.up.ling.irtg.rule_finding.data_creation.MakeAutomata;
import de.up.ling.irtg.rule_finding.pruning.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionOptions;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class CorpusCreatorTest {

    /**
     *
     */
    private CorpusCreator cc;

    /**
     *
     */
    private final String leftTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "a b c\n"
            + "1 2 3 4 5";

    /**
     *
     */
    private final String rightTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "e f g\n"
            + "1 2 3 4 5";

    /**
     *
     */
    private final String alignments = "0-0 0-1 1-2\n"
            + "0-0 1-1 2-2 3-3 4-4\n";

    /**
     * 
     */
    private Iterable<Pair<TreeAutomaton,StateAlignmentMarking>> pairs1;
    
    /**
     * 
     */
    private Iterable<Pair<TreeAutomaton,StateAlignmentMarking>> pairs2;
    
    @Before
    public void setUp() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, ParserException {
        IntersectionPruner ip1 = new IntersectionPruner(IntersectionOptions.NO_EMPTY, IntersectionOptions.RIGHT_BRANCHING_NORMAL_FORM);
        IntersectionPruner ip2 = new IntersectionPruner(IntersectionOptions.RIGHT_BRANCHING_NORMAL_FORM);

        this.cc = new CorpusCreator(ip1, ip2, 1);

        List<ByteArrayOutputStream> treeOutputs = new ArrayList<>();
        Supplier<OutputStream> supp = () -> {
            ByteArrayOutputStream result = new ByteArrayOutputStream();

            treeOutputs.add(result);

            return result;
        };

        InputStream inTrees = new ByteArrayInputStream(leftTrees.getBytes());

        MakeAutomata.create(inTrees, supp);
        List<InputStream> treeInputs = new ArrayList<>();
        for (ByteArrayOutputStream baos : treeOutputs) {
            treeInputs.add(new ByteArrayInputStream(baos.toByteArray()));
        }

        Iterable<TreeAutomaton> iter1 = MakeAutomata.reconstruct(treeInputs);

        treeOutputs.clear();

        inTrees = new ByteArrayInputStream(rightTrees.getBytes());

        MakeAutomata.create(inTrees, supp);
        treeInputs = new ArrayList<>();
        for (ByteArrayOutputStream baos : treeOutputs) {
            treeInputs.add(new ByteArrayInputStream(baos.toByteArray()));
        }

        Iterable<TreeAutomaton> iter2 = MakeAutomata.reconstruct(treeInputs);

        treeOutputs.clear();
        InputStream align = new ByteArrayInputStream(alignments.getBytes());

        MakeAlignments.makeStringFromStandardAlign(align, supp, false);

        List<InputStream> align1 = new ArrayList<>();
        for (ByteArrayOutputStream baos : treeOutputs) {
            align1.add(new ByteArrayInputStream(baos.toByteArray()));
        }

        treeOutputs.clear();
        align = new ByteArrayInputStream(alignments.getBytes());

        MakeAlignments.makeStringFromStandardAlign(align, supp, true);

        List<InputStream> align2 = new ArrayList<>();
        for (ByteArrayOutputStream baos : treeOutputs) {
            align2.add(new ByteArrayInputStream(baos.toByteArray()));
        }

        pairs1 = () -> {
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

                    return new Pair<>(t, spec);
                }
            };
        };

        pairs2 = () -> {
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

                    return new Pair<>(t, spec);
                }
            };
        };
    }

    @Test
    public void test() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, ParserException, ParseException {
        Iterable<Pair<TreeAutomaton<String>,TreeAutomaton<String>>> pairs = this.cc.pushAlignments(pairs1, pairs2);
        
        Iterable<Pair<TreeAutomaton,HomomorphismManager>> result = this.cc.getSharedAutomata(pairs);
        
        Iterator<Pair<TreeAutomaton,HomomorphismManager>> resIter = result.iterator();
        
        Pair<TreeAutomaton,HomomorphismManager> p1 = resIter.next();
        Pair<TreeAutomaton,HomomorphismManager> p2 = resIter.next();
        assertFalse(resIter.hasNext());
        
        
        assertFalse(p1.getLeft().isEmpty());
        assertFalse(p2.getLeft().isEmpty());
        
        TreeAutomaton ta1 = p1.getLeft();
        HomomorphismManager hm = p1.getRight();
        
        assertEquals(ta1.countTrees(),12);
        
        Set<Pair<String,String>> solutions = new HashSet<>();
        
        for(Tree<String> ts : (Iterable<Tree<String>>) ta1.languageIterable()) {
            Pair<String,String> solution = new Pair<>(hm.getHomomorphism1().apply(ts).toString(),
                                                       hm.getHomomorphism2().apply(ts).toString());
            assertFalse(solutions.contains(solution));
            solutions.add(solution);
        }
        
        assertTrue(solutions.contains(new Pair<>("'__X__{__UAS__}'(*(a,*(b,c)))","'__X__{__UAS__}'(*(e,*(f,g)))")));
        assertTrue(solutions.contains(new Pair<>("'__X__{__UAS__}'(*(a,*('__X__{1-2}'(b),c)))","'__X__{__UAS__}'(*(e,*(f,'__X__{2-3}'(g))))")));
        assertTrue(solutions.contains(new Pair<>("'__X__{__UAS__}'(*(a,'__X__{1-3}'(*(b,c))))","'__X__{__UAS__}'(*(e,*(f,'__X__{2-3}'(g))))")));
        assertTrue(solutions.contains(new Pair<>("'__X__{__UAS__}'(*(a,'__X__{1-3}'(*('__X__{1-2}'(b),c))))","'__X__{__UAS__}'(*(e,*(f,'__X__{2-3}'('__X__{2-3}'(g)))))")));
        assertTrue(solutions.contains(new Pair<>("'__X__{__UAS__}'(*('__X__{0-2}'(*(a,b)),c))","'__X__{__UAS__}'('__X__{0-3}'(*(e,*(f,g))))")));
        assertTrue(solutions.contains(new Pair<>("'__X__{__UAS__}'(*('__X__{0-2}'(*(a,'__X__{1-2}'(b))),c))","'__X__{__UAS__}'('__X__{0-3}'(*(e,*(f,'__X__{2-3}'(g)))))")));
        assertTrue(solutions.contains(new Pair<>("'__X__{__UAS__}'(*('__X__{0-2}'(*('__X__{0-1}'(a),b)),c))","'__X__{__UAS__}'('__X__{0-3}'(*('__X__{0-2}'(*(e,f)),g)))")));
        assertTrue(solutions.contains(new Pair<>("'__X__{__UAS__}'(*('__X__{0-2}'(*('__X__{0-1}'(a),'__X__{1-2}'(b))),c))","'__X__{__UAS__}'('__X__{0-3}'(*('__X__{0-2}'(*(e,f)),'__X__{2-3}'(g))))")));
        assertTrue(solutions.contains(new Pair<>("'__X__{__UAS__}'(*('__X__{0-1}'(a),*(b,c)))","'__X__{__UAS__}'(*('__X__{0-2}'(*(e,f)),g))")));
        assertTrue(solutions.contains(new Pair<>("'__X__{__UAS__}'(*('__X__{0-1}'(a),*('__X__{1-2}'(b),c)))","'__X__{__UAS__}'(*('__X__{0-2}'(*(e,f)),'__X__{2-3}'(g)))")));
        assertTrue(solutions.contains(new Pair<>("'__X__{__UAS__}'(*('__X__{0-1}'(a),'__X__{1-3}'(*(b,c))))","'__X__{__UAS__}'(*('__X__{0-2}'(*(e,f)),'__X__{2-3}'(g)))")));
        assertTrue(solutions.contains(new Pair<>("'__X__{__UAS__}'(*('__X__{0-1}'(a),'__X__{1-3}'(*('__X__{1-2}'(b),c))))","'__X__{__UAS__}'(*('__X__{0-2}'(*(e,f)),'__X__{2-3}'('__X__{2-3}'(g))))")));
        
        
        TreeAutomaton ta2 = p2.getLeft();
        ta2.accepts(TreeParser.parse("'__X__{__UAS__}'('*(x1, x2) / *(x1, x2) | 2'('__X__{0-3 +++ 0-3}'('*(x1, x2) / *(x1, x2) | 2'('__X__{0-2 +++ 0-2}'('*(x1, x2) / *(x1, x2) | 2'('__X__{0-1 +++ 0-1}'('1() / x1 | 1'('x1 / 1() | 1'('___END___() / ___END___() | 0'))),'__X__{1-2 +++ 1-2}'('2() / x1 | 1'('x1 / 2() | 1'('___END___() / ___END___() | 0'))))),'__X__{2-3 +++ 2-3}'('3() / x1 | 1'('x1 / 3() | 1'('___END___() / ___END___() | 0'))))),'*(x1, x2) / *(x1, x2) | 2'('__X__{3-4 +++ 3-4}'('4() / x1 | 1'('x1 / 4() | 1'('___END___() / ___END___() | 0'))),'__X__{4-5 +++ 4-5}'('5() / x1 | 1'('x1 / 5() | 1'('___END___() / ___END___() | 0'))))))"));
    }
}
