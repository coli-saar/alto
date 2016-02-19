/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.data_creation;

import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;
import de.up.ling.irtg.util.FunctionIterable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class MakeAutomataTest {
    
    /**
     * 
     */
    private final static String LINES_TO_DECOMPOSE = "de.up.ling.irtg.algebra.StringAlgebra\n"
            +"a b c\n"
            + "\n"
            + "a b\n"
            + "c a b";

    /**
     * Test of create method, of class MakeAutomata.
     */
    @Test
    public void testCreate_InputStream_Supplier() throws Exception {
        List<ByteArrayOutputStream> outs = new ArrayList<>();
        Supplier<OutputStream> outStream = () -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            outs.add(baos);
            return baos;
        };
        
        ByteArrayInputStream bais = new ByteArrayInputStream(LINES_TO_DECOMPOSE.getBytes());
        MakeAutomata.create(bais,outStream);
        
        assertEquals(outs.size(),3);
        StringAlgebra sal = new StringAlgebra();
        TreeAutomaton ta = sal.decompose(sal.parseString("a b")).asConcreteTreeAutomatonWithStringStates();
        
        TreeAutomatonInputCodec taic = new TreeAutomatonInputCodec();
        TreeAutomaton comp = taic.read(new ByteArrayInputStream(outs.get(1).toByteArray()));
        
        assertEquals(comp,ta);
    }

    /**
     * Test of create method, of class MakeAutomata.
     */
    @Test
    public void testCreate_InputStream() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(LINES_TO_DECOMPOSE.getBytes());
        Iterable<String> it = MakeAutomata.create(bais);
        
        int num = 0;
        String aut = null;
        for(String s : it) {
            ++num;
            
            if(num == 2) {
                aut = s;
            }
            
        }
        
        assertEquals(num,3);
        StringAlgebra sal = new StringAlgebra();
        TreeAutomaton ta = sal.decompose(sal.parseString("a b")).asConcreteTreeAutomatonWithStringStates();
        
        TreeAutomatonInputCodec taic = new TreeAutomatonInputCodec();
        TreeAutomaton comp = taic.read(new ByteArrayInputStream(aut.getBytes()));
        
        assertEquals(comp,ta);
    }

    /**
     * Test of reconstruct method, of class MakeAutomata.
     */
    @Test
    public void testReconstruct() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, ParserException {
        ByteArrayInputStream bais = new ByteArrayInputStream(LINES_TO_DECOMPOSE.getBytes());
        Iterable<String> it = MakeAutomata.create(bais);
        
        FunctionIterable<InputStream,String> fi = new FunctionIterable<>(it,(String s) -> {
            return new ByteArrayInputStream(s.getBytes());
        });
        
        Iterable<TreeAutomaton> iterate = MakeAutomata.reconstruct(fi);
        
        int num = 0;
        TreeAutomaton comp = null;
        for(TreeAutomaton ta : iterate) {
            ++num;
            
            if(num == 2) {
                comp = ta;
            }
        }
        
        assertEquals(num,3);
        StringAlgebra sal = new StringAlgebra();
        TreeAutomaton ta = sal.decompose(sal.parseString("a b")).asConcreteTreeAutomatonWithStringStates();
        
        assertEquals(ta,comp);
    }
    
}
