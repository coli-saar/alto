/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.saar.basic.Pair;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.rule_finding.ExtractJointTrees;
import de.up.ling.irtg.rule_finding.alignments.AddressAligner;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.create_automaton.CorpusCreator;
import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.string.RightBranchingNormalForm;
import de.up.ling.irtg.rule_finding.pruning.intersection.tree.NoLeftIntoRight;
import de.up.ling.irtg.rule_finding.variable_introduction.JustXEveryWhere;
import de.up.ling.irtg.rule_finding.variable_introduction.LeftRightXFromFinite;
import de.up.ling.irtg.util.FunctionIterable;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.Tree;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private final static String TEST_INPUT = "What river flows through Kansas ?\n" +
                                             "answer(river(traverse_2(stateid('kansas'))))\n" +
                                             "0:1:1 1:2:2 2:3:3 3:4:4 4:5:5 4:5:6 5:6:7\n" +
                                             "0-0-0:1 0-0-0-0:2 0-0-0-0-0:3 0-0-0-0-0:4 0-0-0-0-0-0:5 0-0-0-0-0-0-0:6 0-0-0-0-0-0:7\n" +
                                             "\n" +
                                             "What river flows through Texas ?\n" +
                                             "answer(river(traverse_2(stateid('texas'))))\n" +
                                             "0:1:1 1:2:2 2:3:3 3:4:4 4:5:5 4:5:6 5:6:7\n" +
                                             "0-0-0:1 0-0-0-0:2 0-0-0-0-0:3 0-0-0-0-0:4 0-0-0-0-0-0:5 0-0-0-0-0-0-0:6 0-0-0-0-0-0:7"
                                            + "\n\n\n";
    
    /**
     * 
     */
    private ArrayList<String> inputs;
    
    /**
     * 
     */
    private ExtractGrammar<List<String>,Tree<String>> extract;
    
    @Before
    public void setUp() throws IOException, ParserException {
        CorpusCreator.Factory fact = new CorpusCreator.Factory();
        fact.setFirstPruner(new IntersectionPruner<>((TreeAutomaton ta) -> {
            return new RightBranchingNormalForm(ta.getSignature(), ta.getAllLabels());
        }));
        fact.setSecondPruner(new IntersectionPruner<>((TreeAutomaton ta) -> {
            return new NoLeftIntoRight(ta.getSignature(), ta.getAllLabels());
        }));
        fact.setFirstVariableSource(new LeftRightXFromFinite());
        fact.setSecondVariableSource(new JustXEveryWhere());
        
        StringAlgebra st1 = new StringAlgebra();
        MinimalTreeAlgebra st2 = new MinimalTreeAlgebra();
        
        SpanAligner.Factory ffact = new SpanAligner.Factory();
        AddressAligner.Factory sfact = new AddressAligner.Factory();
        
        Supplier<Algebra<List<String>>> supp1 = () -> st1;
        Supplier<Algebra<Tree<String>>> supp2 = () -> st2;
        
        CorpusCreator cc = fact.getInstance(supp1, supp2, ffact, sfact);
        ExtractJointTrees gram = new ExtractJointTrees(cc);
        
        InputStream in = new ByteArrayInputStream(TEST_INPUT.getBytes());
        final ArrayList<ByteArrayOutputStream> results = new ArrayList<>();
        Supplier<OutputStream> supp = () -> {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            
            results.add(out);
            
            return out;
        };
        
        gram.getAutomataAndMakeStatistics(in, supp);
        inputs = new ArrayList<>();
        
        results.forEach((ByteArrayOutputStream os) -> {
            try {
                os.close();
                
                inputs.add(os.toString());
            } catch (IOException ex) {
                assertTrue(false);
                Logger.getLogger(ExtractGrammarTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        StringSubtreeIterator.VariableMapping ssi = new StringSubtreeIterator.VariableMapping() {

            @Override
            public String getRoot(Tree<String> whole) {
                return "START";
            }

            @Override
            public String get(Tree<String> child, Tree<String> whole) {
                return child.getLabel();
            }
        };
        
        extract = new ExtractGrammar<>(new StringAlgebra(),new MinimalTreeAlgebra(), ssi,
                        ExtractJointTrees.FIRST_ALGEBRA_ID, ExtractJointTrees.SECOND_ALGEBRA_ID);
    }

    /**
     * Test of extract method, of class ExtractGrammar.
     * @throws java.lang.Exception
     */
    @Test
    public void testExtract() throws Exception {
        ByteArrayOutputStream trees = new ByteArrayOutputStream();
        ByteArrayOutputStream grammar = new ByteArrayOutputStream();
        
        FunctionIterable<InputStream,String> fi = new FunctionIterable<>(inputs, (String s) ->
        {
            return new ByteArrayInputStream(s.getBytes());
        });
        
        extract.extract(fi, trees, grammar);
        
        String one = new String(trees.toByteArray());
        String two = new String(grammar.toByteArray());
        
        String[] arr = one.split("\n");
        assertEquals(arr.length,7);
        
        IrtgInputCodec iic = new IrtgInputCodec();
        InterpretedTreeAutomaton ita = iic.read(new ByteArrayInputStream(two.getBytes()));
        
        TreeAutomaton ta = ita.getAutomaton();
        ta.normalizeRuleWeights();
        
        assertEquals(ta.countTrees(),2);
        
        Map<String,String> input = new HashMap<>();
        input.put("FirstInput", "What river flows through Kansas ?");
        
        TreeAutomaton parse = ita.parse(input);
        
        Iterator<Tree<String>> it = parse.languageIterator();
        assertEquals(parse.countTrees(),1);
        
        Set<Pair<List<String>,Tree<String>>> pairs = new HashSet<>();
        for(int i=0;i<1;++i){
            Tree<String> t = it.next();
            
            Map<String,Object> inter = ita.interpret(t);
            Pair<List<String>,Tree<String>> p = 
                    new Pair<>((List<String>) inter.get("FirstInput"), (Tree<String>) inter.get("SecondInput"));
            pairs.add(p);
        }
        
        StringAlgebra sal = new StringAlgebra();
        assertEquals(pairs.size(),1);
        for(Pair<List<String>,Tree<String>> p : pairs){
            assertEquals(p.getRight(),pt("answer(river(traverse_2(stateid(kansas))))")); 
            assertEquals(p.getLeft(),sal.parseString("What river flows through Kansas ?"));
        }
    }
    
}
