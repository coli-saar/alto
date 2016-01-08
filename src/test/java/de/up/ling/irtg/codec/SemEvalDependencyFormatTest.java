/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.OrderedSGraph;
import java.io.ByteArrayInputStream;
import java.util.Set;
import org.jgrapht.DirectedGraph;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class SemEvalDependencyFormatTest {

    /**
     * 
     */
    private final String TEST_STRING = "#20003025\n" +
                                       "1	Areas	area	NNS	-	+	n_of:x-i	_	_	_	ARG1	_	_	_\n" +
                                       "2	of	of	IN	-	-	_	_	_	_	_	_	_	_\n" +
                                       "3	the	the	DT	-	+	q:i-h-h	_	_	_	_	_	_	_\n\n" +
                                       "4	factory	factory	NN	-	-	n:x	ARG1	BV	_	_	_	_	_\n" +
                                       "   #comment\n"+
                                       "5	were	were	VBD	-	-	_	_	_	_	_	_	_	_\n" +
                                       "6	particularly	particularly	RB	-	+	a:e-e	_	_	_	_	_	_	_\n" +
                                       "7	dusty	dusty	JJ	+	+	a:e-p	_	_	ARG1	_	loc	_	_\n" +
                                       "8	where	where	WRB	-	+	q:i-h-h	_	_	_	_	_	_	_\n" +
                                       "9	the	the	DT	-	+	q:i-h-h	_	_	_	_	_	_	_\n" +
                                       "10	crocidolite	_generic_nn_	NN	-	-	n:x	_	_	_	_	_	BV	ARG2\n" +
                                       "11	was	was	VBD	-	-	_	_	_	_	_	_	_	_\n" +
                                       "12	used	use	VBN	-	+	v:e-i-p	_	_	_	_	loc	_	_\n" +
                                       "13	.	_	.	-	-	_	_	_	_	_	_	_	_\n" +
                                       "\n";
    
    
    /**
     * Test of read method, of class SemEvalDependencyFormat.
     * @throws java.lang.Exception
     */
    @Test
    public void testRead() throws Exception {
        //TODO
        SemEvalDependencyFormat input = new SemEvalDependencyFormat();
        ByteArrayInputStream bais = new ByteArrayInputStream(TEST_STRING.getBytes());
        
        OrderedSGraph osg = input.read(bais);
        assertEquals(osg.getAllNodeNames().size(),14);
        
        GraphNode root = osg.getNode("0");
        DirectedGraph<GraphNode,GraphEdge> dgraph = osg.getGraph();
        
        Set<GraphEdge> edges = dgraph.edgesOf(root);
        assertEquals(edges.size(),13);
        boolean hit = false;
        
        for(GraphEdge ged : edges) {
            GraphNode gn = ged.getTarget();
            
            if(gn.getName().equals("7")) {
                assertEquals(ged.getLabel(),"isAttached");
                hit = true;
            }else {
                assertEquals(ged.getLabel(),"unattached");
            }
        }
        assertTrue(hit);
        
        GraphNode gn = osg.getNode("1");
        edges = osg.getGraph().edgesOf(gn);
        assertEquals(gn.getLabel(),"NNS");
        
        assertEquals(edges.size(),3);
        for(GraphEdge e : edges) {
            switch (e.getSource().getName()) {
                case "0":
                    assertEquals(e.getLabel(),"unattached");
                    assertEquals(e.getTarget(),gn);
                    break;
                case "7":
                    assertEquals(e.getLabel(),"ARG1");
                    assertEquals(e.getTarget(),gn);
                    break;
                default:
                    assertEquals(e.getSource(),gn);
                    assertEquals(e.getLabel(),"ARG1");
                    assertEquals(e.getTarget().getName(),"4");
                    break;
            }
        }
        
        gn = osg.getNode("2");
        edges = osg.getGraph().edgesOf(gn);
        assertEquals(gn.getLabel(),"IN");
        assertEquals(edges.size(),1);
        
        gn = osg.getNode("3");
        edges = osg.getGraph().edgesOf(gn);
        assertEquals(gn.getLabel(),"DT");
        assertEquals(edges.size(),2);
        
        for(GraphEdge e : edges) {
            switch (e.getSource().getName()) {
                case "0":
                    assertEquals(e.getLabel(),"unattached");
                    assertEquals(e.getTarget(),gn);
                    break;
                default:
                    assertEquals(e.getSource(),gn);
                    assertEquals(e.getLabel(),"BV");
                    assertEquals(e.getTarget().getName(),"4");
                    break;
            }
        }
        
        gn = osg.getNode("4");
        edges = osg.getGraph().edgesOf(gn);
        assertEquals(gn.getLabel(),"NN");
        assertEquals(edges.size(),3);
        
        gn = osg.getNode("5");
        edges = osg.getGraph().edgesOf(gn);
        assertEquals(gn.getLabel(),"VBD");
        assertEquals(edges.size(),1);
        
        gn = osg.getNode("6");
        edges = osg.getGraph().edgesOf(gn);
        assertEquals(gn.getLabel(),"RB");
        assertEquals(edges.size(),2);
        
        for(GraphEdge e : edges) {
            switch (e.getSource().getName()) {
                case "0":
                    assertEquals(e.getLabel(),"unattached");
                    assertEquals(e.getTarget(),gn);
                    break;
                default:
                    assertEquals(e.getSource(),gn);
                    assertEquals(e.getLabel(),"ARG1");
                    assertEquals(e.getTarget().getName(),"7");
                    break;
            }
        }
        
        gn = osg.getNode("7");
        edges = osg.getGraph().edgesOf(gn);
        assertEquals(gn.getLabel(),"JJ");
        assertEquals(edges.size(),4);
        
        gn = osg.getNode("8");
        edges = osg.getGraph().edgesOf(gn);
        assertEquals(gn.getLabel(),"WRB");
        assertEquals(edges.size(),3);
        
        for(GraphEdge e : edges) {
            switch (e.getTarget().getName()) {
                case "8":
                    assertEquals(e.getLabel(),"unattached");
                    assertEquals(e.getSource().getName(),"0");
                    break;
                case "7":
                    assertEquals(e.getLabel(),"loc");
                    assertEquals(e.getSource(),gn);
                    break;
                default:
                    assertEquals(e.getSource(),gn);
                    assertEquals(e.getLabel(),"loc");
                    assertEquals(e.getTarget().getName(),"12");
                    break;
            }
        }
        
        gn = osg.getNode("9");
        edges = osg.getGraph().edgesOf(gn);
        assertEquals(gn.getLabel(),"DT");
        assertEquals(edges.size(),2);
        
        for(GraphEdge e : edges) {
            switch (e.getSource().getName()) {
                case "0":
                    assertEquals(e.getLabel(),"unattached");
                    assertEquals(e.getTarget(),gn);
                    break;
                default:
                    assertEquals(e.getSource(),gn);
                    assertEquals(e.getLabel(),"BV");
                    assertEquals(e.getTarget().getName(),"10");
                    break;
            }
        }
        
        gn = osg.getNode("10");
        edges = osg.getGraph().edgesOf(gn);
        assertEquals(gn.getLabel(),"NN");
        assertEquals(edges.size(),3);
        
        gn = osg.getNode("11");
        edges = osg.getGraph().edgesOf(gn);
        assertEquals(gn.getLabel(),"VBD");
        assertEquals(edges.size(),1);
        
        gn = osg.getNode("12");
        edges = osg.getGraph().edgesOf(gn);
        assertEquals(gn.getLabel(),"VBN");
        assertEquals(edges.size(),3);
        
        for(GraphEdge e : edges) {
            switch (e.getSource().getName()) {
                case "0":
                    assertEquals(e.getLabel(),"unattached");
                    assertEquals(e.getTarget(),gn);
                    break;
                case "8":
                    assertEquals(e.getLabel(),"loc");
                    assertEquals(e.getTarget(),gn);
                    break;    
                default:
                    assertEquals(e.getSource(),gn);
                    assertEquals(e.getLabel(),"ARG2");
                    assertEquals(e.getTarget().getName(),"10");
                    break;
            }
        }
        
        gn = osg.getNode("13");
        edges = osg.getGraph().edgesOf(gn);
        assertEquals(gn.getLabel(),".");
        assertEquals(edges.size(),1);
    }
}
