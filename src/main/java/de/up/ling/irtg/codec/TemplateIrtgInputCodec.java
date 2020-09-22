/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.TemplateInterpretedTreeAutomaton;
import de.up.ling.irtg.TemplateInterpretedTreeAutomaton.Guard;
import de.up.ling.irtg.codec.template_irtg.TemplateIrtgLexer;
import de.up.ling.irtg.codec.template_irtg.TemplateIrtgParser;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;

import static de.up.ling.irtg.codec.template_irtg.TemplateIrtgParser.*;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import java.util.ArrayList;

/**
 * An input codec for template IRTGs. A template IRTG is an
 * IRTG in which rules may additionally be prefixed by quantification
 * over variables using the <code>foreach</code> keyword, like so:<p>
 * 
 * <pre>{@code
 *  foreach { a,b | left-of(a,b) and target(a) and distractor(b)}:
 *  NP_$a -> leftof_$a(N_$a, NP_$b)
 *  [sem] project_1(intersect_2(intersect_1(left-of, ?1), ?2))
 *  [string] *(?1, *("left of", ?2))
 * }</pre>
 * 
 * See {@link TemplateInterpretedTreeAutomaton} for details about
 * what such rules mean.
 * 
 * @author koller
 */
@CodecMetadata(name = "template-irtg", description = "Template IRTG", extension = "tirtg", type = TemplateInterpretedTreeAutomaton.class)
public class TemplateIrtgInputCodec extends InputCodec<TemplateInterpretedTreeAutomaton> {

    private TemplateInterpretedTreeAutomaton tirtg;

    @Override
    public TemplateInterpretedTreeAutomaton read(InputStream is) throws CodecParseException, IOException {
        TemplateIrtgLexer l = new TemplateIrtgLexer(CharStreams.fromStream(is));
        l.removeErrorListeners();
        l.addErrorListener(ThrowingErrorListener.INSTANCE);
        TemplateIrtgParser p = new TemplateIrtgParser(new CommonTokenStream(l));
        p.removeErrorListeners();
        p.addErrorListener(ThrowingErrorListener.INSTANCE);
        p.getInterpreter().setPredictionMode(PredictionMode.SLL);

        tirtg = new TemplateInterpretedTreeAutomaton();

        try {
            TemplateIrtgParser.Template_irtgContext result = p.template_irtg();
            build(result);
            return tirtg;
        } catch (CodecParseException e) {
            throw e;
        } catch (RecognitionException e) {
            throw new CodecParseException(e.getMessage());
        }
    }

    private void build(TemplateIrtgParser.Template_irtgContext context) throws CodecParseException {
        for (TemplateIrtgParser.Interpretation_declContext ic : context.interpretation_decl()) {
            String id = name(ic.name(0));
            String classname = name(ic.name(1));
            tirtg.addAlgebraClass(id, classname);
        }

        for (Feature_declContext c : context.feature_decl()) {
            processFeature(c);
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
    
    private void processFeature(TemplateIrtgParser.Feature_declContext co) {
        if( co instanceof TemplateIrtgParser.CONSTRUCTOR_FEATUREContext ) {
            TemplateIrtgParser.CONSTRUCTOR_FEATUREContext c = (TemplateIrtgParser.CONSTRUCTOR_FEATUREContext) co;
            String id = c.name(0).getText();
            String classname = c.name(1).getText();
            List<String> arguments = CodecUtilities.processList(c, x -> x.state_list().state(), x -> name(x.name()));
            tirtg.addConstructorFeatureDeclaration(id, classname, arguments);
        } else if( co instanceof TemplateIrtgParser.STATIC_FEATUREContext ) {
            TemplateIrtgParser.STATIC_FEATUREContext c = (STATIC_FEATUREContext) co;
            String id = c.name(0).getText();
            String classname = c.name(1).getText();
            String methodname = c.name(2).getText();
            List<String> arguments = CodecUtilities.processList(c, x -> x.state_list().state(), x -> name(x.name()));
            tirtg.addStaticFeatureDeclaration(id, classname, methodname, arguments);
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
        trule.rhs = Util.mapToList(autoRule.state_list().state(), x -> name(x.name()));
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

    private static void processGuard(TemplateIrtgParser.GuardContext guard, TemplateInterpretedTreeAutomaton.TemplateRule trule) throws CodecParseException {
        List<String> variables = CodecUtilities.processList(guard.name_list(), nl -> nl.name(), TemplateIrtgInputCodec::name);
        trule.variables = variables;
        trule.guard = processGuardCondition(guard.guard_condition());
    }

    private static TemplateInterpretedTreeAutomaton.Guard processGuardCondition(TemplateIrtgParser.Guard_conditionContext condition) throws CodecParseException {
        if (condition.atomic_guard_condition() != null) {
            return processGuardCondition(condition.atomic_guard_condition());
        } else if (condition.conjunctive_guard_condition() != null) {
            return processGuardCondition(condition.conjunctive_guard_condition());
        } else {
            throw new CodecParseException("Invalid guard condition 1");
        }
    }

    private static TemplateInterpretedTreeAutomaton.Guard processGuardCondition(TemplateIrtgParser.Atomic_guard_conditionContext condition) throws CodecParseException {
        Tree<String> guardTerm = term(condition.term());

        List<String> guardArgs = Util.mapToList(guardTerm.getChildren(), x -> x.getLabel());
        return new TemplateInterpretedTreeAutomaton.AtomicGuard(guardTerm.getLabel(), guardArgs);
    }

    private static TemplateInterpretedTreeAutomaton.Guard processGuardCondition(TemplateIrtgParser.Conjunctive_guard_conditionContext condition) throws CodecParseException {
        Guard g1 = processGuardCondition(condition.atomic_or_bracketed_guard_condition());
        Guard g2 = processGuardCondition(condition.guard_condition());
        return new TemplateInterpretedTreeAutomaton.ConjGuard(g1, g2);
    }

    private static TemplateInterpretedTreeAutomaton.Guard processGuardCondition(TemplateIrtgParser.Atomic_or_bracketed_guard_conditionContext condition) throws CodecParseException {
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
            List<Tree<String>> subtrees = new ArrayList<>();

            for (TemplateIrtgParser.TermContext sub : ct.term()) {
                subtrees.add(term(sub));
            }

            return Tree.create(name(ct.name()), subtrees);
        }
    }
}
