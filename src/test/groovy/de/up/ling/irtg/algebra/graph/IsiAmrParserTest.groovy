/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra.graph


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

        assert new CycleDetector(graph.getGraph()).detectCycles();
    }
    
    @Test
    public void testVars() {
        SGraph graph = IsiAmrParser.parse(new StringReader(AMR5));
        
        assertEquals("w", graph.getSource("root"));
        assertEquals("b", graph.getSource("subj"));
    }
    
    
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
    
    private static final String AMR3 = """
    
    (w / woman
   :ARG0-of (n / nominate-01
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

