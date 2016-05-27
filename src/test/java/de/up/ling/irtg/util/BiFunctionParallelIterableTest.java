/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class BiFunctionParallelIterableTest {
    
    /**
     * 
     */
    private DoubleArrayList second;
    
    /**
     * 
     */
    private DoubleArrayList first;
    
    /**
     * 
     */
    private BiFunctionParallelIterable<Double,Double,Double> iter;
    
    @Before
    public void setUp() {
        first = new DoubleArrayList();
        second = new DoubleArrayList();
        
        for(int i=0;i<3000;++i){
            first.add(i);
        }
        
        for(int i=0;i<2000;i += 3){
            second.add(i);
        }
        
        iter = new BiFunctionParallelIterable<>(first,second,4, (Double a, Double b) -> {
            return b-a;
        });
    }

    /**
     * Test of iterator method, of class BiFunctionParallelIterable.
     */
    @Test
    public void testIterator() {
        DoubleArrayList dal = new DoubleArrayList();
        
        iter.forEach((Double d) -> dal.add(d));
        assertEquals(dal.size(),second.size());
        for(int i=0;i<second.size();++i){           
            assertEquals(dal.getDouble(i),second.getDouble(i)-i,0.000000001);
        }
    }
}
