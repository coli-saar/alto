/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;

/**
 * A codec for reading tree automata written in Thomas Hanneforth's format
 * for weighted bottom-up tree automata. Rules are written in the following format:<p>
 * 
 * <pre>TOP(q1,q2) -> q0	&lt;35133&gt;</pre>
 * 
 * where TOP is the terminal symbol, and q0, q1, q2 are states. Each rule
 * may optionally be followed by a weight in angle brackets. If no weight is
 * specified, the codec assumes a rule weight of 1.<p>
 * 
 * The final states of the automaton are listed before the first 
 * rule, i.e.:<p>
 * 
 * <pre>q0
 *q1</pre><p>
 * 
 * The comment character is #; everything following a # symbol on the same line
 * is ignored by the codec.<p>
 * 
 * Note that the codec is currently limited in that one can write symbols like
 * " and &lt; in the original file format. But the codec will only accept
 * symbols that start and end with " and have no " in between (and analogously
 * for ').
 * 
 * @author koller
 */
@CodecMetadata(name="bu-fta", description="Bottom-up tree automata (Hanneforth style)", extension = "fta", type = TreeAutomaton.class)
public class BottomUpTreeAutomatonInputCodec extends InputCodec<TreeAutomaton>{
    private ConcreteTreeAutomaton<String> automaton = new ConcreteTreeAutomaton<String>();

    @Override
    public TreeAutomaton read(InputStream is) throws CodecParseException, IOException {
        BottomUpTreeAutomatonLexer l = new BottomUpTreeAutomatonLexer(new ANTLRInputStream(is));
        BottomUpTreeAutomatonParser p = new BottomUpTreeAutomatonParser(new CommonTokenStream(l));
        p.setErrorHandler(new ExceptionErrorStrategy());
        p.getInterpreter().setPredictionMode(PredictionMode.SLL);
        
        automaton = new ConcreteTreeAutomaton<String>();

        try {
            BottomUpTreeAutomatonParser.FtaContext result = p.fta();
            
            for( BottomUpTreeAutomatonParser.StateContext stateContext : result.state() ) {
                automaton.addFinalState(automaton.addState(state(stateContext)));
            }

            for (BottomUpTreeAutomatonParser.Auto_ruleContext ruleContext : result.auto_rule()) {
                autoRule(ruleContext);
            }

            return automaton;
        } catch (RecognitionException e) {
            throw new CodecParseException(e.getMessage());
        }
    }
    
    private String autoRule(BottomUpTreeAutomatonParser.Auto_ruleContext auto_rule) {
        String parent = state(auto_rule.state());
        List<String> children = statelist(auto_rule.state_list());
        String label = name(auto_rule.name());
        double weight = weight(auto_rule.weight());

        Rule rule = automaton.createRule(parent, label, children, weight);
        automaton.addRule(rule);

        return label;
    }

    private List<String> statelist(BottomUpTreeAutomatonParser.State_listContext rule_args) {
        List<String> ret = new ArrayList<String>();

        if (rule_args != null) {
            for (BottomUpTreeAutomatonParser.StateContext sc : rule_args.state()) {
                ret.add(state(sc));
            }
        }

        return ret;
    }

    private String state(BottomUpTreeAutomatonParser.StateContext sc) {
        String ret = name(sc.name());
        int state = automaton.addState(ret);
        return ret;
    }

    private String name(BottomUpTreeAutomatonParser.NameContext nc) {
        boolean isQuoted = (nc instanceof BottomUpTreeAutomatonParser.QUOTEDContext);
        
        // if name is of the form <....>, do NOT strip the angle brackets,
        // they are part of the name

        assert !isQuoted || nc.getText().startsWith("\"") || nc.getText().startsWith("\'") : "invalid symbol: -" + nc.getText() + "- " + nc.getClass();

        return CodecUtilities.extractName(nc, isQuoted);
    }

    private double weight(BottomUpTreeAutomatonParser.WeightContext weight) {
        if (weight == null) {
            return 1;
        } else {
            return Double.parseDouble(CodecUtilities.stripOuterChars(weight.getText()));
        }
    }
}
