/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class OrderedSGraphTest {

    /**
     * Test of merge method, of class OrderedSGraph.
     */
    @Test
    public void testMerge() {
        //(a<1> b<2> c)
        OrderedSGraph osg1 = new OrderedSGraph();
        osg1.addNode("0", "a");
        osg1.addNode("1", "b");
        osg1.addNode("2", "c");
        
        osg1.addSource("1", "0");
        osg1.addSource("2", "1");
        
        //(""<1> -label-> ""<2>)
        OrderedSGraph osg2 = new OrderedSGraph();
        GraphNode g1 = osg2.addNode("0", "");
        GraphNode g2 = osg2.addNode("1", "");
        
        osg2.addSource("1", "0");
        osg2.addSource("2", "1");
        
        osg2.addEdge(g1, g2, "label");
        
        OrderedSGraph osg4 = osg1.merge(osg2);
        
        //(d e<3>)
        OrderedSGraph osg3 = new OrderedSGraph();
        osg3.addNode("0", "d");
        osg3.addNode("1", "e");
        
        osg3.addSource("3", "1");
        
        OrderedSGraph osg5 = osg4.merge(osg3);
        
        //(""<3> -otherlabel-> ""<2>)
        OrderedSGraph osg6 = new OrderedSGraph();
        GraphNode gn1 = osg6.addNode("1", "");
        GraphNode gn2 = osg6.addNode("2", "");
        
        osg6.addSource("3", "1");
        osg6.addSource("2", "2");
        
        osg6.addEdge(gn1, gn2, "otherlabel");
        
        OrderedSGraph osg7 = osg5.merge(osg6);
        
        System.out.println(osg7);
        
        assertEquals(osg7.getNode("0").getLabel(),"a");
        assertEquals(osg7.getNodeForSource("1"),"0");
        
        assertEquals(osg7.getNode("1").getLabel(),"b");
        assertEquals(osg7.getNodeForSource("2"),"1");
        
        assertEquals(osg7.getNode("4").getLabel(),"e");
        assertEquals(osg7.getNodeForSource("3"),"4");
        
        osg7.getAllNodeNames().forEach((String node) -> {
            osg7.getAllNodeNames().forEach((String otherNode) -> {
                if(OrderedSGraph.ORDER.compare(node,otherNode) < 0) {
                    assertTrue(osg7.getNode(node).getLabel().compareTo(osg7.getNode(otherNode).getLabel()) < 0);
                }
            });
        });
    }
    
}
