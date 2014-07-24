/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import static de.up.ling.irtg.codec.CodecUtilities.findFeatureConstructor;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.maxent.FeatureFunction;
import de.up.ling.irtg.maxent.MaximumEntropyIrtg;
import de.up.ling.tree.Tree;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;

/**
 *
 * @author koller
 */
@CodecMetadata(name = "irtg", description = "IRTG grammars", extension = "irtg", type = InterpretedTreeAutomaton.class)
public class IrtgInputCodec extends InputCodec<InterpretedTreeAutomaton> {

    private ConcreteTreeAutomaton<String> automaton = new ConcreteTreeAutomaton<String>();
    private Map<String, Homomorphism> homomorphisms = new HashMap<String, Homomorphism>();
    private Map<String, Interpretation> interpretations = new HashMap<String, Interpretation>();
    private Map<String, FeatureFunction> features = new HashMap<String, FeatureFunction>();

    @Override
    public InterpretedTreeAutomaton read(InputStream is) throws IOException, ParseException {
        IrtgLexer l = new IrtgLexer(new ANTLRInputStream(is));
        IrtgParser p = new IrtgParser(new CommonTokenStream(l));
        p.setErrorHandler(new ExceptionErrorStrategy());
        p.getInterpreter().setPredictionMode(PredictionMode.SLL);

        automaton = new ConcreteTreeAutomaton<String>();
        homomorphisms = new HashMap<String, Homomorphism>();
        interpretations = new HashMap<String, Interpretation>();
        features = new HashMap<String, FeatureFunction>();

        try {
            IrtgParser.IrtgContext result = p.irtg();
            InterpretedTreeAutomaton irtg = build(result);
            return irtg;
        } catch (ParseException e) {
            throw e;
        } catch (RecognitionException e) {
            throw new ParseException(e.getMessage());
        }
    }

    private InterpretedTreeAutomaton build(IrtgParser.IrtgContext context) throws ParseException {
        try {
            for (IrtgParser.Interpretation_declContext c : context.interpretation_decl()) {
                interpretationDecl(c);
            }

            for (IrtgParser.Feature_declContext c : context.feature_decl()) {
                featureDecl(c);
            }

            int numRules = context.irtg_rule().size();
            int i = 1;
            for (IrtgParser.Irtg_ruleContext c : context.irtg_rule()) {
                String label = autoRule(c.auto_rule());

                for (IrtgParser.Hom_ruleContext hc : c.hom_rule()) {
                    homRule(label, hc);
                }

                notifyProgressListener(i, numRules, "Read " + i + "/" + numRules + " rules");
                i++;
            }

            InterpretedTreeAutomaton ret = features.isEmpty() ? new InterpretedTreeAutomaton(automaton) : new MaximumEntropyIrtg(automaton, features);
            ret.addAllInterpretations(interpretations);

            return ret;
        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ParseException("Unexpected parsing error: " + e.toString());
        }
    }

    private void interpretationDecl(IrtgParser.Interpretation_declContext ic) throws ParseException {
        String id = name(ic.name(0));
        String classname = name(ic.name(1));

        try {
            Class algebraClass = Class.forName(classname);
            Algebra algebra = (Algebra) algebraClass.newInstance();
            Homomorphism hom = new Homomorphism(automaton.getSignature(), algebra.getSignature());

            interpretations.put(id, new Interpretation(algebra, hom));
            homomorphisms.put(id, hom);
        } catch (Exception e) {
            throw new ParseException("Could not instantiate algebra class " + classname + " for interpretation " + id + ": " + e.toString());
        }
    }

    private void featureDecl(IrtgParser.Feature_declContext co) throws ParseException {
        if (co instanceof IrtgParser.CONSTRUCTOR_FEATUREContext) {
            IrtgParser.CONSTRUCTOR_FEATUREContext c = (IrtgParser.CONSTRUCTOR_FEATUREContext) co;
            String id = c.name(0).getText();
            String classname = c.name(1).getText();
            List<String> arguments = statelist(c.state_list());

            addConstructorFeature(id, classname, arguments, features);
        } else if (co instanceof IrtgParser.STATIC_FEATUREContext) {
            IrtgParser.STATIC_FEATUREContext c = (IrtgParser.STATIC_FEATUREContext) co;
            String id = c.name(0).getText();
            String classname = c.name(1).getText();
            String methodname = c.name(2).getText();
            List<String> arguments = statelist(c.state_list());

            addStaticFeature(id, classname, methodname, arguments, features);
        }
    }

    public static void addConstructorFeature(String id, String classname, List<String> arguments, Map<String, FeatureFunction> features) throws ParseException {
        try {
            Constructor<FeatureFunction> con = findFeatureConstructor(classname, arguments.size());

            String[] args = new String[arguments.size()];
            arguments.toArray(args);

            FeatureFunction feature = con.newInstance(args);
            features.put(id, feature);
        } catch (Exception e) {
            throw new ParseException("Could not instantiate FeatureFunction class " + classname + " for feature " + id + ": " + e.toString());
        }
    }

    public static void addStaticFeature(String id, String classname, String methodname, List<String> arguments, Map<String, FeatureFunction> features) throws ParseException {
        try {
            Method meth = CodecUtilities.findStaticFeatureFactory(classname, methodname, arguments.size());

            Object[] args = new String[arguments.size()];
            arguments.toArray(args);

            FeatureFunction feature = (FeatureFunction) meth.invoke(null, args);
            features.put(id, feature);
        } catch (ClassNotFoundException e) {
            throw new ParseException("Could not find feature factory class " + classname + " for feature " + id + ": " + e.toString());
        } catch (NoSuchMethodException e) {
            throw new ParseException("Could not find feature factory method " + methodname + " in class " + classname + " for feature " + id + ": " + e.toString());
        } catch (IllegalAccessException e) {
            throw new ParseException("Not allowed to invoke factory method " + classname + "::" + methodname + "for feature " + id + ": " + e.toString());
        } catch (IllegalArgumentException e) {
            throw new ParseException("Not allowed to invoke factory method " + classname + "::" + methodname + "for feature " + id + ": " + e.toString());
        } catch (InvocationTargetException e) {
            throw new ParseException("Not allowed to invoke factory method " + classname + "::" + methodname + "for feature " + id + ": " + e.toString());
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

    private void homRule(String label, IrtgParser.Hom_ruleContext hc) throws ParseException {
        String interp = name(hc.name());
        Homomorphism hom = homomorphisms.get(interp);
        Tree<String> rhs = term(hc.term());

        if (!interpretations.containsKey(interp)) {
            throw new ParseException("Undeclared interpretation '" + interp + "'");
        } else if (hom == null) {
            throw new ParseException("Homomorphism declaration for unknown interpretation '" + interp + "'");
        } else if (hom.get(hom.getSourceSignature().getIdForSymbol(label)) != null) {
            if (!hom.get(label).equals(rhs)) {
                throw new ParseException("Redefined value of interpretation '" + interp + "' for " + label + " as " + rhs + " (was: " + hom.get(label) + ")");
            }
        }

        hom.add(label, rhs);
    }

    private List<String> statelist(IrtgParser.State_listContext rule_args) {
        return CodecUtilities.processList(rule_args, lc -> lc.state(), this::state);
    }

    private Tree<String> term(IrtgParser.TermContext t) {
        if (t instanceof IrtgParser.VARIABLE_TERMContext) {
            IrtgParser.VARIABLE_TERMContext vt = (IrtgParser.VARIABLE_TERMContext) t;
            return Tree.create(vt.variable().VARIABLE().getText());
        } else {
            IrtgParser.CONSTANT_TERMContext ct = (IrtgParser.CONSTANT_TERMContext) t;
            List<Tree<String>> subtrees = new ArrayList<Tree<String>>();

            for (IrtgParser.TermContext sub : ct.term()) {
                subtrees.add(term(sub));
            }

            return Tree.create(name(ct.name()), subtrees);
        }
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
