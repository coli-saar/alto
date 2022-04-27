/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec


import org.junit.*
import java.util.*
import java.io.*
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import static de.up.ling.irtg.util.TestingTools.*;
import de.up.ling.irtg.*
import de.up.ling.irtg.codec.isiamr.IsiAmrParser;

import org.junit.*
import java.util.*
import java.io.*
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.algebra.graph.GraphEdge
import de.up.ling.irtg.algebra.graph.GraphNode
import de.up.ling.irtg.algebra.graph.SGraph
import de.up.ling.irtg.hom.*;
import static de.up.ling.irtg.util.TestingTools.*;
import de.up.ling.irtg.*


import org.jgrapht.*;
import org.jgrapht.alg.*;
import org.jgrapht.graph.*;


/**
 *
 * @author koller
 */
class IsiAmrParserTest {
    @Test
    public void testSimple() {
        SGraph graph = IsiAmrParser.parse(new StringReader(AMR1));
    }
    
    @Test
    public void testComplex() {
        SGraph graph = IsiAmrParser.parse(new StringReader(AMR2));
    }
    
    @Test
    public void testNoChildren() {
        SGraph graph = IsiAmrParser.parse(new StringReader(AMR4));
    }
    
    @Test
    public void testCyclic() {
        SGraph graph = IsiAmrParser.parse(new StringReader(AMR3));
        assert ! new CycleDetector(graph.getGraph()).detectCycles();
        
        graph = IsiAmrParser.parse(new StringReader(AMR3_WITH_CYCLE));
        assert new CycleDetector(graph.getGraph()).detectCycles();
    }
    
    @Test
    public void testVars() {
        SGraph graph = IsiAmrParser.parse(new StringReader(AMR5));
        
        assertEquals("w", graph.getNodeForSource("root"));
        assertEquals("b", graph.getNodeForSource("subj"));
    }
    
    @Test
    public void testMultiSource() {
        SGraph graph = IsiAmrParser.parse(new StringReader(AMR6));
        
        assertEquals("w", graph.getNodeForSource("root"));
        assertEquals("w", graph.getNodeForSource("coref1"));
        assertEquals("b", graph.getNodeForSource("subj"));
    }
    
    @Test
    public void testAnonymousSources() {
        SGraph graph = IsiAmrParser.parse(new StringReader(AMR_ANONYMOUS_SRC));
        
        assert graph.getNodeForSource("root") != null;
        assertEquals(graph.getNodeForSource("root"), graph.getNodeForSource("coref1"));
        
        assertEquals("b", graph.getNodeForSource("subj"));
        
        assert graph.getNodeForSource("foo") != null;
    }
    
    @Test
    public void testNames() {
        SGraph graph = IsiAmrParser.parse(new StringReader(AMR_NAMES_TEST));
        
        assertEquals("boy", graph.getNode("b").getLabel());

        assert graph.getNode("c") != null;
        assertEquals(null, graph.getNode("c").getLabel());
        
        GraphNode g = graph.getNode("g");
        boolean foundEdge = false;
        for( GraphEdge e : graph.getGraph().outgoingEdgesOf(g) ) {
            if( e.getLabel().equals("ARGx")) {
                foundEdge = true;
                assertEquals("c", e.getTarget().getName());
            }
        }
        
        assert foundEdge;
    }

    @Test
    public void testReentrancyFirst() {
        SGraph graph = IsiAmrParser.parse(new StringReader(AMR1_REENTRANCY_FIRST));

        assert graph.getGraph().vertexSet().size() == 3;

        GraphNode goNode = graph.getNode("g");
        for (GraphEdge e : graph.getGraph().outgoingEdgesOf(goNode)) {
            if (e.getLabel().equals("ARG0")) {
                assertEquals("boy", e.getTarget().getLabel());
            }
        }
    }


    @Test
    public void testNodesWithNoNodenames() {
        SGraph graph = IsiAmrParser.parse(new StringReader(AMR_NO_NODENAMES));
        assert hasNodename(graph, "John");
        assert hasNodename(graph, "1234");
        assert hasNodename(graph, "-");
        assert hasNodename(graph, "+");
        assert hasNodename(graph, "interrogative");
        assert hasNodename(graph, "a");
        assert hasNodename(graph, "b2");
    }

    private static hasNodename(SGraph graph, String nodeName) {
        return graph.getGraph().vertexSet().stream().anyMatch({ n -> n.getLabel().equals(nodeName) });
    }


    private static final String AMR_ANONYMOUS_SRC = """
   (<root, coref1> / want-01
       :ARG0 (b<subj> / boy)
       :ARGx <foo>
       :ARG1 (g / go-01
                 :ARG0 b))
""";
    
    private static final String AMR6 = """
   (w<root, coref1> / want-01
  :ARG0 (b<subj> / boy)
  :ARG1 (g / go-01
          :ARG0 b))
""";
    
    private static final String AMR5 = """
   (w<root> / want-01
  :ARG0 (b<subj> / boy)
  :ARG1 (g / go-01
          :ARG0 b))
""";
    
    public static final String AMR1 = """(w / want-01
  :ARG0 (b / boy)
  :ARG1 (g / go-01
          :ARG0 b))
""";
    
    // intended: b/boy, g -ARG0-> b, g -ARGx-> c/null
    public static final String AMR_NAMES_TEST = """(w / want-01
        :ARG0 (b / boy)
        :ARG1 (g / go-01
                   :ARG0 b
                   :ARGx c))
""";

    public static final String AMR1_REENTRANCY_FIRST = """(w / want-01
  :ARG1 (g / go-01
          :ARG0 b)
  :ARG0 (b / boy))
""";

    // makes no linguistic sense, just for testing
    // all of John, 1234, - and interrogative should be node labels.
    public static final String AMR_NO_NODENAMES = """(l / list
  :op1 "John"
  :op2 1234
  :op3 "a"
  :op4 "b2"
  :polarity -
  :polite +
  :mode interrogative)
""";
    
    public static final String AMR2 = """
(s / say-01
  :ARG0 (g / organization
          :name (n / name
                  :op1 "UN"))
  :ARG1 (f / flee-01
          :ARG0 (p / person
                  :quant (a / about
                           :op1 14000))
          :ARG1 (h / home
                  :poss p)
          :time (w / weekend)
          :time (a2 / after
                  :op1 (w2 / warn-01
                         :ARG1 (t / tsunami)
                         :location (l / local))))
  :medium (s2 / site
            :poss g
            :mod (w3 / web)))

    """;
    
    // this is the original graph that the ISI people
    // find "cyclic" (which it is if one assumes an
    // ARG0-of edge from w to n)
    private static final String AMR3 = """    
    (w / woman
   :ARG0-of (n / nominate-01
               :ARG1 (b / boss
                        :poss w)))
                        """;
    
    // this graph is a linguistically meaningless but actually
    // cyclic version of AMR3
    private static final String AMR3_WITH_CYCLE = """    
    (w / woman
       :ARG0 (n / nominate-01
               :ARG1 (b / boss
                        :poss w)))
                        """;
    
    public static final String AMR4 = """
    (s / sing-01
   :ARG0 (s2 / soldier)
   :beneficiary (g / girl)
   :time (w / walk-01
            :ARG0 g
            :accompanier s2
            :destination (t / town)))
            """;
}

