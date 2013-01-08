/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra

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
/**
 *
 * @author Danilo Baumgarten
 */
class PtbTreeAlgebraTest {
    @Test
    public void testPtbTreeAlgebra() {
        PtbTreeAlgebra pta = new PtbTreeAlgebra();
        Tree<String> tree = pta.parseString("( (`` ``) (INTJ (UH Yes) (. .) ))");
        assertEquals("INTJ('``'('``'),UH(Yes),'.'('.'))", tree.toString());

        PtbTreeAlgebra.binarizeInit();
        tree = PtbTreeAlgebra.binarize(tree);
        assertEquals("INTJ('``'('``'),ART-BIN2(UH(Yes),ART-UN1('.'('.'))))", tree.toString());
    }
}