/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.FeatureStructureAlgebra;
import de.up.ling.irtg.algebra.TagStringAlgebra;
import de.up.ling.irtg.algebra.TagTreeAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.codec.tulipac.TulipacLexer;
import de.up.ling.irtg.codec.tulipac.TulipacParser;
import de.up.ling.irtg.hom.Homomorphism;
import java.io.IOException;
import java.io.InputStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;

import static de.up.ling.irtg.codec.tulipac.TulipacParser.*;

/**
 *
 * @author koller
 */
@CodecMetadata(name = "tulipac", description = "TAG grammar (tulipac format)", extension = "tag", type = InterpretedTreeAutomaton.class)
public class TulipacInputCodec extends InputCodec<InterpretedTreeAutomaton> {
    private ConcreteTreeAutomaton<String> automaton;
    private Homomorphism ht, hs, hf;
    
    @Override
    public InterpretedTreeAutomaton read(InputStream is) throws CodecParseException, IOException {
        TulipacLexer l = new TulipacLexer(new ANTLRInputStream(is));
        TulipacParser p = new TulipacParser(new CommonTokenStream(l));
        p.setErrorHandler(new ExceptionErrorStrategy());
        p.getInterpreter().setPredictionMode(PredictionMode.SLL);
        
        automaton = new ConcreteTreeAutomaton<>();
        
        Algebra at = new TagTreeAlgebra();
        Algebra as = new TagStringAlgebra();
        Algebra af = new FeatureStructureAlgebra();
        
        ht = new Homomorphism(automaton.getSignature(), at.getSignature());
        hs = new Homomorphism(automaton.getSignature(), as.getSignature());
        hf = new Homomorphism(automaton.getSignature(), af.getSignature());
        
        GrmrContext parse = p.grmr();
        buildGrmr(parse);
        
        InterpretedTreeAutomaton ret = new InterpretedTreeAutomaton(automaton);
        ret.addInterpretation("string", new Interpretation(as, hs));
        ret.addInterpretation("tree", new Interpretation(at, ht));
        ret.addInterpretation("ft", new Interpretation(af, hf));
        
        return ret;
    }

    private void buildGrmr(GrmrContext parse) {
        for( IncludeContext incl : parse.include()) {
            buildInclude(incl);
        }
        
        for( TrContext tr : parse.tr()) {
            buildTree(tr);
        }
        
        for( FamilyContext family : parse.family() ) {
            buildFamily(family);
        }
        
        for( WordByItselfContext word : parse.wordByItself() ) {
            buildWordByItself(word);
        }
        
        for( LemmaContext lemma : parse.lemma() ) {
            buildLemma(lemma);
        }
    }    

    private void buildInclude(IncludeContext incl) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    private void buildTree(TrContext tr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void buildFamily(FamilyContext family) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void buildWordByItself(WordByItselfContext word) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void buildLemma(LemmaContext lemma) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
