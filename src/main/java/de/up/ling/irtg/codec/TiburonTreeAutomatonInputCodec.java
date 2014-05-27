/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;

/**
 *
 * @author koller
 */
public class TiburonTreeAutomatonInputCodec extends InputCodec<TreeAutomaton> {
    private ConcreteTreeAutomaton<String> automaton = new ConcreteTreeAutomaton<String>();
    private int gensymNext = 1;

    @Override
    public TreeAutomaton read(InputStream is) throws ParseException, IOException {
        TiburonTreeAutomatonLexer l = new TiburonTreeAutomatonLexer(new ANTLRInputStream(is));
        TiburonTreeAutomatonParser p = new TiburonTreeAutomatonParser(new CommonTokenStream(l));
        p.setErrorHandler(new ExceptionErrorStrategy());
        p.getInterpreter().setPredictionMode(PredictionMode.SLL);

        try {
            TiburonTreeAutomatonParser.FtaContext result = p.fta();

            for (TiburonTreeAutomatonParser.StateContext stateContext : result.state()) {
                automaton.addFinalState(automaton.addState(state(stateContext)));
            }

            Set<String> states = new HashSet<>();
            List<RawRule> rawRules = new ArrayList<>();

            for (TiburonTreeAutomatonParser.Auto_ruleContext ruleContext : result.auto_rule()) {
                autoRule(ruleContext, rawRules, states);
            }
            
            for( RawRule rule : rawRules ) {
                List<String> children = introduceAnonymousStates(automaton, rule.rhs, states);
                automaton.addRule(automaton.createRule(rule.lhs, rule.label, children));
            }

            return automaton;
        } catch (RecognitionException e) {
            throw new ParseException(e.getCause());
        }
    }
    
    private List<String> introduceAnonymousStates(ConcreteTreeAutomaton<String> auto, List<String> children, Set<String> states) {
        List<String> ret = new ArrayList<>();
        
        for( String s : children ) {
            if( states.contains(s) ) {
                ret.add(s);
            } else {
                String newState = gensym("_q_");
                auto.addRule(auto.createRule(newState, s, new ArrayList<>()));
                ret.add(newState);
            }
        }
            
        return ret;
    }
    
    private String gensym(String prefix) {
        return prefix + (gensymNext++);
    }

    private String autoRule(TiburonTreeAutomatonParser.Auto_ruleContext auto_rule, List<RawRule> rawRules, Set<String> states) {
        String parent = state(auto_rule.state());
        List<String> children = statelist(auto_rule.state_list());
        String label = name(auto_rule.name());
        double weight = weight(auto_rule.weight());
        
        states.add(parent);
        rawRules.add(new RawRule(parent, label, children, weight));

//        automaton.addRule(automaton.createRule(parent, label, children, weight));

        return label;
    }

    private List<String> statelist(TiburonTreeAutomatonParser.State_listContext rule_args) {
        List<String> ret = new ArrayList<String>();

        if (rule_args != null) {
            for (TiburonTreeAutomatonParser.StateContext sc : rule_args.state()) {
                ret.add(state(sc));
            }
        }

        return ret;
    }

    private String state(TiburonTreeAutomatonParser.StateContext sc) {
        String ret = name(sc.name());
        int state = automaton.addState(ret);

        return ret;
    }

    private String name(TiburonTreeAutomatonParser.NameContext nc) {
        boolean isQuoted = (nc instanceof TiburonTreeAutomatonParser.QUOTEDContext);

        assert !isQuoted || nc.getText().startsWith("'") || nc.getText().startsWith("\"") : "invalid symbol: -" + nc.getText() + "- " + nc.getClass();

        return CodecUtilities.extractName(nc, isQuoted);
    }

    private double weight(TiburonTreeAutomatonParser.WeightContext weight) {
        if (weight == null) {
            return 1;
        } else {
            return Double.parseDouble(CodecUtilities.stripOuterChars(weight.getText()));
        }
    }

    private static class RawRule {

        String lhs;
        List<String> rhs;
        String label;
        double weight;

        public RawRule(String lhs, String label, List<String> rhs, double weight) {
            this.label = label;
            this.lhs = lhs;
            this.rhs = rhs;
            this.weight = weight;
        }
    }
}
