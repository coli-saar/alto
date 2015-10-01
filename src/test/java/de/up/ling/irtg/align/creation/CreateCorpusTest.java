/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.creation;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.align.Propagator;
import de.up.ling.irtg.align.alignment_marking.AddressAligner;
import de.up.ling.irtg.align.alignment_marking.SpanAligner;
import de.up.ling.irtg.align.pruning.StringLeftXORRight;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class CreateCorpusTest {
    
    /**
     * 
     */
    private CreateCorpus<List<String>,Tree<String>> cc;
    
    /**
     * 
     */
    private StringAlgebra sal;
    
    /**
     * 
     */
    private MinimalTreeAlgebra mta;
    
    /**
     * 
     */
    private List<CreateCorpus.InputPackage> stringInputs;
    
    /**
     * 
     */
    private List<CreateCorpus.InputPackage> treeInput;
    
    @Before
    public void setUp() {
        this.sal = new StringAlgebra();
        this.mta = new MinimalTreeAlgebra();
        this.stringInputs = new ArrayList<>();
        this.treeInput = new ArrayList<>();
        
        cc = new CreateCorpus<>(sal,mta);
        
        Propagator propDef = new Propagator();
        Propagator propLarge = new Propagator(new StringLeftXORRight());
        
        
        Function<List<String>,Propagator> funct = (List<String> in) -> {
            if(in.size() > 0){
                return propLarge;
            }
            
            return propDef;
        };
        
        SpanAligner.Factory spanFactory = new SpanAligner.Factory();
        
        for(int i=0;i<50;++i){
            String a = "a"+i;
            String align = "";
            this.stringInputs.add(new CreateCorpus.InputPackage(a,align,funct,spanFactory));
        }
        
        for(String[] p : new String[][] {{"a b c","0:1:1 1:2:2 2:3:3"}}){
            this.stringInputs.add(new CreateCorpus.InputPackage(p[0], p[1], funct, spanFactory));     
        }
        
        Function<Tree<String>,Propagator> f = (Tree<String> in) -> {
            return propDef;
        };
        
        AddressAligner.Factory addFactory = new AddressAligner.Factory();
        
        for(int i=0;i<50;++i){
            String a = "a"+i;
            String align = "";
            this.treeInput.add(new CreateCorpus.InputPackage(a,align,f,addFactory));
        }
        
        for(String[] p : new String[][] {{"c(b,a)","0-0-0-1:1 0-0-0-0:2 0-0-0:3"}}){
            this.treeInput.add(new CreateCorpus.InputPackage<>(p[0],p[1],f,addFactory));
        }
    }

    /**
     * Test of makeDataSet method, of class CreateCorpus.
     * @throws java.lang.Exception
     */
    @Test
    public void testMakeDataSet() throws Exception {
        List<TreeAutomaton> data = this.cc.makeDataSet(stringInputs, treeInput);
        
        assertEquals(51,data.size());
        
        for(TreeAutomaton ta : data){
            assertFalse(ta.isEmpty());
        }
        
        TreeAutomaton ta = data.get(data.size()-1);
        
        assertEquals(ta.language().size(),12);
        Iterable<Tree<String>> it = ta.language();
        for (Tree<String> t : it) {
            String s = getLeaf(t);
            assertEquals(s,"a0() / a0() | 0");
        }
    }

    /**
     * Test of getMainManager method, of class CreateCorpus.
     * @throws de.up.ling.irtg.algebra.ParserException
     */
    @Test
    public void testGetMainManager() throws ParserException {
        List<TreeAutomaton> data = this.cc.makeDataSet(stringInputs, treeInput);
        
        Signature sig = this.cc.getMainManager().getSignature();
        
        assertEquals(sig,data.get(0).getSignature());
    }

    /**
     * Test of getAlgebra1 method, of class CreateCorpus.
     * @throws de.up.ling.irtg.algebra.ParserException
     */
    @Test
    public void testGetAlgebra1() throws ParserException {
        this.cc.makeDataSet(stringInputs, treeInput);
        
        Signature sig = this.cc.getMainManager().getHomomorphism1().getTargetSignature();
        
        assertEquals(sig,this.cc.getAlgebra1().getSignature());
    }

    /**
     * Test of getAlgebra2 method, of class CreateCorpus.
     * @throws de.up.ling.irtg.algebra.ParserException
     */
    @Test
    public void testGetAlgebra2() throws ParserException {
        this.cc.makeDataSet(stringInputs, treeInput);
        
        Signature sig = this.cc.getMainManager().getHomomorphism2().getTargetSignature();
        
        assertEquals(sig,this.cc.getAlgebra2().getSignature());
    }   

    /**
     * 
     * @param t
     * @return 
     */
    private String getLeaf(Tree<String> t) {
        if(t.getChildren().isEmpty()){
            return t.getLabel();
        }else{
            return getLeaf(t.getChildren().get(0));
        }
    }
}