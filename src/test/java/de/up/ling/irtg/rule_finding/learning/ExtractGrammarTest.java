/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.saar.basic.Pair;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.rule_finding.ExtractJointTrees;
import de.up.ling.irtg.rule_finding.create_automaton.ExtractionHelper;
import de.up.ling.irtg.rule_finding.pruning.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.Pruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionOptions;
import de.up.ling.irtg.util.FunctionIterable;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class ExtractGrammarTest {
    /**
    *
    */
    private final static String leftTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "what river flows through kansas\n"
            + "what river flows through texas";

    /**
     *
     */
    private final static String rightTrees = "de.up.ling.irtg.algebra.MinimalTreeAlgebra\n"
            + "answer(river(traverse_2(stateid('kansas'))))\n"
            + "answer(river(traverse_2(stateid('texas'))))";

    /**
     *
     */
    private final static String alignments = "0-0 1-1 2-2 3-2 3-3 4-3 4-4\n"
            + "0-0 1-1 2-2 3-2 3-3 4-3 4-4";
    
    /**
     * 
     */
    private ExtractGrammar<List<String>,Tree<String>> extract;
    
    /**
     * 
     */
    private Iterable<InputStream> inputs;
    
    @Before
    public void setUp() throws IOException, ParserException, ClassNotFoundException,
            InstantiationException, IllegalAccessException, ParseException {
        extract = new ExtractGrammar<>(new StringAlgebra(),new MinimalTreeAlgebra(),
                        ExtractJointTrees.FIRST_ALGEBRA_ID, ExtractJointTrees.SECOND_ALGEBRA_ID,
                        new MostFrequentVariables(),"string","tree");
        
        Pruner one = new IntersectionPruner(IntersectionOptions.NO_EMPTY,IntersectionOptions.RIGHT_BRANCHING_NORMAL_FORM);
        Pruner two = new IntersectionPruner(IntersectionOptions.NO_EMPTY,IntersectionOptions.NO_LEFT_INTO_RIGHT);
        
        Iterable<String> sols = ExtractionHelper.getTreeIRTGs(leftTrees, rightTrees, alignments, one, two);
        
        inputs = new FunctionIterable<>(sols,(String input) -> {
            return new ByteArrayInputStream(input.getBytes());
        });
    }

    /**
     * Test of extract method, of class ExtractGrammar.
     * @throws java.lang.Exception
     */
    @Test
    public void testExtract() throws Exception {
        ByteArrayOutputStream trees = new ByteArrayOutputStream();
        ByteArrayOutputStream grammar = new ByteArrayOutputStream();
        
        extract.extract(inputs, trees, grammar);
        
        String one = new String(trees.toByteArray());
        String two = new String(grammar.toByteArray());
        
        String[] arr = one.split("\n");
        assertEquals(arr.length,7);
        
        IrtgInputCodec iic = new IrtgInputCodec();
        InterpretedTreeAutomaton ita = iic.read(new ByteArrayInputStream(two.getBytes()));
        
        TreeAutomaton ta = ita.getAutomaton();
        assertTrue(ta.countTrees() >= 2);
        
        Map<String,String> input = new HashMap<>();
        input.put("string", "What river flows through Kansas".toLowerCase());
        
        TreeAutomaton parse = ita.parse(input);
        
        Iterator<Tree<String>> it = parse.languageIterator();
        assertEquals(parse.countTrees(),1);
        
        Set<Pair<List<String>,Tree<String>>> pairs = new HashSet<>();
        for(int i=0;i<1;++i){
            Tree<String> t = it.next();
            
            Map<String,Object> inter = ita.interpret(t);
            Pair<List<String>,Tree<String>> p = 
                    new Pair<>((List<String>) inter.get("string"), (Tree<String>) inter.get("tree"));
            pairs.add(p);
        }
        
        StringAlgebra sal = new StringAlgebra();
        assertEquals(pairs.size(),1);
        for(Pair<List<String>,Tree<String>> p : pairs){
            assertEquals(p.getRight(),pt("answer(river(traverse_2(stateid(kansas))))")); 
            assertEquals(p.getLeft(),sal.parseString("What river flows through Kansas".toLowerCase()));
        }
    }
    
    @Test
    public void testExtractFromSubtrees() throws Exception {
        ByteArrayOutputStream grammar = new ByteArrayOutputStream();
        SubtreeExtractor suex = new GetAllRules();
        
        extract.extract(inputs, grammar, suex, "__UAS__");
        
        String two = new String(grammar.toByteArray());
        
        IrtgInputCodec iic = new IrtgInputCodec();
        InterpretedTreeAutomaton ita = iic.read(new ByteArrayInputStream(two.getBytes()));
        
        Map<String,String> input = new HashMap<>();
        input.put("string", "What river flows through Kansas".toLowerCase());
        
        TreeAutomaton parse = ita.parse(input);
        
        Iterator<Tree<String>> it = parse.languageIterator();
        
        Set<Pair<List<String>,Tree<String>>> pairs = new HashSet<>();
        for(int i=0;i<1;++i){
            Tree<String> t = it.next();
            
            Map<String,Object> inter = ita.interpret(t);
            Pair<List<String>,Tree<String>> p = 
                    new Pair<>((List<String>) inter.get("string"), (Tree<String>) inter.get("tree"));
            pairs.add(p);
        }
        
        StringAlgebra sal = new StringAlgebra();
        assertEquals(pairs.size(),1);
        for(Pair<List<String>,Tree<String>> p : pairs){
            assertEquals(p.getRight(),pt("answer(river(traverse_2(stateid(kansas))))")); 
            assertEquals(p.getLeft(),sal.parseString("What river flows through Kansas".toLowerCase()));
        }
    }
}
