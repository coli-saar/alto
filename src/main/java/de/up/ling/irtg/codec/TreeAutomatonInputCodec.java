/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.irtg.IrtgLexer;
import de.up.ling.irtg.codec.irtg.IrtgParser;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;

/**
 * An input codec that reads a top-down tree automaton.
 * Rules take the following form:<p>
 * 
 * <pre>
 * S! -&gt; r(NP, VP) [0.3]
 * </pre>
 * 
 * where S, NP, and VP are states and r is a terminal symbol.
 * S is a final state, as indicated by the exclamation mark.
 * Each rule can optionally be assigned a weight in square brackets;
 * in the example, the weight is 0.3. Rules that do not have an
 * explicit weight get a default weight of 1.
 * 
 * Drop the brackets if r is a zero-place symbol, i.e. a rule for leaves
 * looks like this: <code>D -&gt; a</code>.
 * 
 * @author koller
 */
@CodecMetadata(name = "auto", description = "Tree automata", extension = "auto", type = TreeAutomaton.class)
public class TreeAutomatonInputCodec extends InputCodec<TreeAutomaton> {

    private ConcreteTreeAutomaton<String> automaton = null;

    @Override
    public TreeAutomaton read(InputStream is) throws CodecParseException, IOException {
        IrtgLexer l = new IrtgLexer(CharStreams.fromStream(is));
        l.removeErrorListeners();
        l.addErrorListener(ThrowingErrorListener.INSTANCE);
        IrtgParser p = new IrtgParser(new CommonTokenStream(l));
        p.removeErrorListeners();
        p.addErrorListener(ThrowingErrorListener.INSTANCE);
        p.getInterpreter().setPredictionMode(PredictionMode.SLL);
        
        automaton = new ConcreteTreeAutomaton<>();

        try {
            IrtgParser.FtaContext result = p.fta();

            for (IrtgParser.Auto_ruleContext ruleContext : result.auto_rule()) {
                autoRule(ruleContext);
            }

            return automaton;
        } catch (RecognitionException e) {
            throw new CodecParseException(e.getMessage());
        }
    }

    private String autoRule(IrtgParser.Auto_ruleContext auto_rule) {
        String parent = state(auto_rule.state());
        List<String> children = statelist(auto_rule.state_list());
        String label = name(auto_rule.name());
        double weight = weight(auto_rule.weight());

        automaton.addRule(automaton.createRule(parent, label, children, weight));

        return label;
    }

    private List<String> statelist(IrtgParser.State_listContext rule_args) {
        List<String> ret = new ArrayList<>();

        if (rule_args != null) {
            for (IrtgParser.StateContext sc : rule_args.state()) {
                ret.add(state(sc));
            }
        }

        return ret;
    }

    private String state(IrtgParser.StateContext sc) {
        String ret = name(sc.name());
        int state = automaton.addState(ret);

        if (sc.FIN_MARK() != null) {
            automaton.addFinalState(state);
        }

        return ret;
    }

    private String name(IrtgParser.NameContext nc) {
        boolean isQuoted = (nc instanceof IrtgParser.QUOTEDContext);

        assert !isQuoted || nc.getText().startsWith("'") || nc.getText().startsWith("\"") : "invalid symbol: -" + nc.getText() + "- " + nc.getClass();

        return CodecUtilities.extractName(nc, isQuoted);
    }

    private double weight(IrtgParser.WeightContext weight) {
        if (weight == null) {
            return 1;
        } else {
            return Double.parseDouble(CodecUtilities.stripOuterChars(weight.getText()));
        }
    }
}
