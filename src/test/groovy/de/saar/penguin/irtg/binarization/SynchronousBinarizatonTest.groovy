
package de.saar.penguin.irtg


import org.junit.*
import java.util.*
import java.io.*
import static org.junit.Assert.*
import de.saar.penguin.irtg.binarization.StringAlgebraBinarizer
import de.saar.penguin.irtg.binarization.RegularBinarizer
import static de.saar.penguin.irtg.util.TestingTools.pt;


class SynchronousBinarizationTest {
    
    @Test
    public void testSynchronousBinarization() {
        
        String grammarstring = '''
interpretation i1: de.saar.penguin.irtg.algebra.StringAlgebra
interpretation i2: de.saar.penguin.irtg.algebra.StringAlgebra

S! -> r1(A,B,C) 
  [i1] *(?1,?2,?3)
  [i2] *(?3,?1,?2)

A -> r2 
  [i1] a
  [i2] a

B -> r3
  [i1] b
  [i2] b

C -> r4 
  [i1] c
  [i2] c
        ''';

        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(grammarstring));       
        InterpretedTreeAutomaton irtgBinarized = testBinarize(irtg); 

        Set decoding = irtgBinarized.decode(new StringReader("i2"), [i1: new StringReader("a b c")]);
        assertEquals(new HashSet([["c","a","b"]]), decoding);
        
        Set termDecoding = irtgBinarized.decodeToTermsFromReaders(new StringReader("i2"), [i1: new StringReader("a b c")]);
        assertEquals(new HashSet([pt("*(c,*(a,b))")]), termDecoding);
    } 
    
    
    
    @Test
    public void testSynchronousBinarizationTwoBinarizationTrees() {
        
        String grammarstring = '''
interpretation i1: de.saar.penguin.irtg.algebra.StringAlgebra
interpretation i2: de.saar.penguin.irtg.algebra.StringAlgebra

S! -> r1(A,B,C)
  [i1] *(?1,?2,?3)
  [i2] *(?3,?2,?1)

A -> r2 
  [i1] a
  [i2] a

B -> r3 
  [i1] b
  [i2] b

C -> r4 
  [i1] c
  [i2] c
        ''';

        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(grammarstring));
        InterpretedTreeAutomaton irtgBinarized = testBinarize(irtg); 

        Set decoding = irtgBinarized.decode(new StringReader("i2"), [i1: new StringReader("a b c")]);
        assertEquals(new HashSet([["c","b","a"]]), decoding);
        
        Set termDecoding = irtgBinarized.decodeToTermsFromReaders(new StringReader("i2"), [i1:  new StringReader("a b c")]);
        assertEquals(new HashSet([pt("*(c,*(b,a))"),pt("*(*(c,b),a)")]), termDecoding);
    }    
    
   
    
    @Test
    public void testSynchronousBinarizationDeletingHomomorphism() {
        
        String grammarstring = '''
interpretation i1: de.saar.penguin.irtg.algebra.StringAlgebra
interpretation i2: de.saar.penguin.irtg.algebra.StringAlgebra

S! -> r1(A,B,C)
  [i1] *(?1,?2,?3)
  [i2] *(?3,?2,?1)

A -> r2(D)
  [i1] *(?1,a)
  [i2] *(e,f,?1)

B -> r3 
  [i1] b
  [i2] b

C -> r4 
  [i1] c
  [i2] c

D -> r5 
  [i1] d
  [i2] d
        ''';

        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(grammarstring));       
        InterpretedTreeAutomaton irtgBinarized = testBinarize(irtg); 

        Set decoding = irtgBinarized.decode(new StringReader("i2"), [i1: new StringReader("d a b c")]);
        assertEquals(new HashSet([["c","b","e","f","d"]]), decoding);
        
        Set termDecoding = irtgBinarized.decodeToTermsFromReaders(new StringReader("i2"), [i1: new StringReader("d a b c")]);
        assertEquals(new HashSet([  pt("*(c,*(b,*(e,*(f,d))))"),
                                    pt("*(*(c,b),*(e,*(f,d)))"),
                                    pt("*(*(c,b),*(*(e,f),d))"),
                                    pt("*(c,*(b,*(*(e,f),d)))") ]), termDecoding);
    }     
    
     
    
    private InterpretedTreeAutomaton testBinarize(InterpretedTreeAutomaton irtg){ 
        RegularBinarizer bin1 = new StringAlgebraBinarizer();
        RegularBinarizer bin2 = new StringAlgebraBinarizer();
        Map<String,RegularBinarizer> binarizers = new HashMap<String, RegularBinarizer>();
        binarizers.put("i1",bin1);
        binarizers.put("i2",bin2);
        InterpretedTreeAutomaton newAuto = irtg.binarize(binarizers);
        return newAuto;
    }
    
}


