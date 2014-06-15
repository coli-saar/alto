/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.TemplateInterpretedTreeAutomaton;
import de.up.ling.irtg.TemplateInterpretedTreeAutomaton.Guard;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;

import static de.up.ling.irtg.codec.TemplateIrtgParser.*;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import java.util.ArrayList;

/**
 *
 * @author koller
 */
@CodecMetadata(name = "template-irtg", description = "Template IRTG", extension = "tirtg", type = TemplateInterpretedTreeAutomaton.class)
public class TemplateIrtgInputCodec extends InputCodec<TemplateInterpretedTreeAutomaton> {

    private TemplateInterpretedTreeAutomaton tirtg;

    @Override
    public TemplateInterpretedTreeAutomaton read(InputStream is) throws ParseException, IOException {
        TemplateIrtgLexer l = new TemplateIrtgLexer(new ANTLRInputStream(is));
        TemplateIrtgParser p = new TemplateIrtgParser(new CommonTokenStream(l));
        p.setErrorHandler(new ExceptionErrorStrategy());
        p.getInterpreter().setPredictionMode(PredictionMode.SLL);

        tirtg = new TemplateInterpretedTreeAutomaton();

        try {
            TemplateIrtgParser.Template_irtgContext result = p.template_irtg();
            build(result);
            return tirtg;
        } catch (ParseException e) {
            throw e;
        } catch (RecognitionException e) {
            throw new ParseException(e.getMessage());
        }
    }

    private void build(TemplateIrtgParser.Template_irtgContext context) throws ParseException {
        for (TemplateIrtgParser.Interpretation_declContext ic : context.interpretation_decl()) {
            String id = name(ic.name(0));
            String classname = name(ic.name(1));
            tirtg.addAlgebraClass(id, classname);
        }

        for (Feature_declContext c : context.feature_decl()) {
            String id = c.name(0).getText();
            String classname = c.name(1).getText();
            List<String> arguments = CodecUtilities.processList(c, x -> x.state_list().state(), x -> name(x.name()));
            tirtg.addFeatureDeclaration(id, classname, arguments);
        }

        int numRules = context.template_irtg_rule().size();
        int i = 1;
        for (TemplateIrtgParser.Template_irtg_ruleContext c : context.template_irtg_rule()) {
            TemplateInterpretedTreeAutomaton.TemplateRule trule = new TemplateInterpretedTreeAutomaton.TemplateRule();

            if (c.guarded_irtg_rule() != null) {
                processRule(c.guarded_irtg_rule().irtg_rule(), trule);
                processGuard(c.guarded_irtg_rule().guard(), trule);
            } else if (c.irtg_rule() != null) {
                processRule(c.irtg_rule(), trule);
            }

            tirtg.addRuleTemplate(trule);

            notifyProgressListener(i, numRules, "Read " + i + "/" + numRules + " rules");
            i++;
        }
    }

    private static String name(TemplateIrtgParser.NameContext nc) {
        boolean isQuoted = (nc instanceof TemplateIrtgParser.QUOTEDContext);

        assert !isQuoted || nc.getText().startsWith("'") || nc.getText().startsWith("\"") : "invalid symbol: -" + nc.getText() + "- " + nc.getClass();

        return CodecUtilities.extractName(nc, isQuoted);
    }

    private void processRule(TemplateIrtgParser.Irtg_ruleContext irtgRule, TemplateInterpretedTreeAutomaton.TemplateRule trule) {
        // process automaton rule
        Auto_ruleContext autoRule = irtgRule.auto_rule();

        trule.lhs = name(autoRule.state().name());
        trule.rhs = Util.mapList(autoRule.state_list().state(), x -> name(x.name()));
        trule.label = name(autoRule.name());

        if (autoRule.state().FIN_MARK() != null) {
            trule.lhsIsFinal = true;
        }

        if (autoRule.weight() != null) {
            trule.weight = CodecUtilities.weight(autoRule.weight(), x -> x.getText());
        }

        // process hom rules
        for (Hom_ruleContext hc : irtgRule.hom_rule()) {
            String interp = name(hc.name());
            Tree<String> rhs = term(hc.term());
            trule.hom.put(interp, rhs);
        }
    }

    private static void processGuard(TemplateIrtgParser.GuardContext guard, TemplateInterpretedTreeAutomaton.TemplateRule trule) throws ParseException {
        List<String> variables = CodecUtilities.processList(guard.name_list(), nl -> nl.name(), TemplateIrtgInputCodec::name);
        trule.variables = variables;
        trule.guard = processGuardCondition(guard.guard_condition());
    }

    private static TemplateInterpretedTreeAutomaton.Guard processGuardCondition(TemplateIrtgParser.Guard_conditionContext condition) throws ParseException {
        if (condition.atomic_guard_condition() != null) {
            return processGuardCondition(condition.atomic_guard_condition());
        } else if (condition.conjunctive_guard_condition() != null) {
            return processGuardCondition(condition.conjunctive_guard_condition());
        } else {
            throw new ParseException("Invalid guard condition 1");
        }
    }

    private static TemplateInterpretedTreeAutomaton.Guard processGuardCondition(TemplateIrtgParser.Atomic_guard_conditionContext condition) throws ParseException {
        Tree<String> guardTerm = term(condition.term());

        List<String> guardArgs = Util.mapList(guardTerm.getChildren(), x -> x.getLabel());
        return new TemplateInterpretedTreeAutomaton.AtomicGuard(guardTerm.getLabel(), guardArgs);
    }

    private static TemplateInterpretedTreeAutomaton.Guard processGuardCondition(TemplateIrtgParser.Conjunctive_guard_conditionContext condition) throws ParseException {
        Guard g1 = processGuardCondition(condition.atomic_or_bracketed_guard_condition());
        Guard g2 = processGuardCondition(condition.guard_condition());
        return new TemplateInterpretedTreeAutomaton.ConjGuard(g1, g2);
    }

    private static TemplateInterpretedTreeAutomaton.Guard processGuardCondition(TemplateIrtgParser.Atomic_or_bracketed_guard_conditionContext condition) throws ParseException {
        if (condition.atomic_guard_condition() != null) {
            return processGuardCondition(condition.atomic_guard_condition());
        } else {
            return processGuardCondition(condition.guard_condition());
        }
    }

    private static Tree<String> term(TemplateIrtgParser.TermContext t) {
        if (t instanceof TemplateIrtgParser.VARIABLE_TERMContext) {
            TemplateIrtgParser.VARIABLE_TERMContext vt = (TemplateIrtgParser.VARIABLE_TERMContext) t;
            return Tree.create(vt.variable().VARIABLE().getText());
        } else {
            TemplateIrtgParser.CONSTANT_TERMContext ct = (TemplateIrtgParser.CONSTANT_TERMContext) t;
            List<Tree<String>> subtrees = new ArrayList<Tree<String>>();

            for (TemplateIrtgParser.TermContext sub : ct.term()) {
                subtrees.add(term(sub));
            }

            return Tree.create(name(ct.name()), subtrees);
        }
    }
}
