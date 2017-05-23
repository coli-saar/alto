/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.RdgStringAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.codec.rdg.RdgLexer;
import de.up.ling.irtg.codec.rdg.RdgParser;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 *
 * @author koller
 */
@CodecMetadata(name = "rdg", description = "Regular dependency grammar", extension = "rdg", type = InterpretedTreeAutomaton.class)
public class RdgInputCodec extends InputCodec<InterpretedTreeAutomaton> {
    
    
    @Override
    public InterpretedTreeAutomaton read(InputStream is) throws CodecParseException, IOException {
        RdgLexer l = new RdgLexer(new ANTLRInputStream(is));
        RdgParser p = new RdgParser(new CommonTokenStream(l));
        p.setErrorHandler(new ExceptionErrorStrategy());
        p.getInterpreter().setPredictionMode(PredictionMode.SLL);
        
        try {
            RdgParser.RdgContext result = p.rdg();
            InterpretedTreeAutomaton irtg = build(result);
            return irtg;
        } catch (CodecParseException e) {
            throw e;
        } catch (RecognitionException e) {
            throw new CodecParseException(e.getMessage());
        }
    }

    private InterpretedTreeAutomaton build(RdgParser.RdgContext result) {
        // set up automaton
        ConcreteTreeAutomaton<String> auto = new ConcreteTreeAutomaton<>();
        auto.addFinalState(auto.addState("S"));
        
        // set up interpretation
        RdgStringAlgebra alg = new RdgStringAlgebra();
        Homomorphism hom = new Homomorphism(auto.getSignature(), alg.getSignature());        
        
        // iterate over rules and add IRTG rules for them
        for( RdgParser.Rdg_ruleContext rule : result.rdg_rule()) {
            processRule(rule, auto, hom);
        }
        
        // assemble IRTG object and return it
        InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(auto);
        irtg.addInterpretation("string", new Interpretation(alg, hom));
        return irtg;
    }

    private void processRule(RdgParser.Rdg_ruleContext ruleC, ConcreteTreeAutomaton<String> auto, Homomorphism hom) {
        // extract parent and child states
        String parent = ruleC.NAME().getText();
        
        List<String> children = new ArrayList<>();
        for( TerminalNode child : ruleC.state_list().NAME() ) {
            children.add(child.getText());
        }
        
        // extract order annotation
        String label = ruleC.LABEL().getText();
        label = label.substring(1, label.length()-1); // strip < and >
        Matcher m = RdgStringAlgebra.OA_PATTERN.matcher(label);
        if( ! m.matches() ) {
            throw new CodecParseException("Invalid order annotation: " + label);
        }

        // make automaton rule
        String ruleLabel = Util.gensym("r");
        Rule rule = auto.createRule(parent, ruleLabel, children);
        auto.addRule(rule);
        
        // make homomorphism entry
        List<Tree<String>> childVariables = new ArrayList<>();
        for( int i = 1; i <= children.size(); i++ ) {
            childVariables.add(Tree.create("?" + i));
        }
        hom.add(ruleLabel, Tree.create(label, childVariables));
    }
}
