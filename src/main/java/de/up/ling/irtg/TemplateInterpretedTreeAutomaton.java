/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg;

import de.saar.basic.CartesianIterator;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import static de.up.ling.irtg.codec.CodecUtilities.findFeatureConstructor;
import de.up.ling.irtg.codec.ParseException;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.maxent.FeatureFunction;
import de.up.ling.irtg.maxent.MaximumEntropyIrtg;
import de.up.ling.irtg.util.FirstOrderModel;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class TemplateInterpretedTreeAutomaton {

    private List<TemplateRule> ruleTemplates;
    private Map<String, String> algebraClasses;
    private List<FeatureDeclaration> features;

    public TemplateInterpretedTreeAutomaton() {
        ruleTemplates = new ArrayList<>();
        algebraClasses = new HashMap<>();
        features = new ArrayList<FeatureDeclaration>();
    }

    public void addRuleTemplate(TemplateRule trule) {
        ruleTemplates.add(trule);
    }

    public void addFeatureDeclaration(String id, String featureClass, List<String> arguments) {
        FeatureDeclaration decl = new FeatureDeclaration();
        decl.id = id;
        decl.featureClass = featureClass;
        decl.arguments = arguments;
        features.add(decl);
    }

    public void addAlgebraClass(String interpretation, String className) {
        algebraClasses.put(interpretation, className);
    }

    public InterpretedTreeAutomaton instantiate(FirstOrderModel model) throws ClassNotFoundException, InstantiationException, IllegalAccessException, ParseException {
        ConcreteTreeAutomaton<String> auto = new ConcreteTreeAutomaton<>();
        Map<String, Interpretation> interps = new HashMap<>();

        // initialize interpretations
        for (String interp : algebraClasses.keySet()) {
            Class algebraClass = Class.forName(algebraClasses.get(interp));
            Algebra algebra = (Algebra) algebraClass.newInstance();
            Homomorphism hom = new Homomorphism(auto.getSignature(), algebra.getSignature());
            Interpretation intp = new Interpretation(algebra, hom);
            interps.put(interp, intp);
        }

        // instantiate all rules
        for (TemplateRule trule : ruleTemplates) {
            forAllInstances(trule, model, (parent, label, children, weight, variableAssignment) -> {
                Rule rule = auto.createRule(parent, label, children, weight);
                auto.addRule(rule);

                if (trule.lhsIsFinal) {
                    auto.addFinalState(rule.getParent());
                }

                for (String interp : trule.hom.keySet()) {
                    interps.get(interp).getHomomorphism().add(label, instantiateTerm(trule.hom.get(interp), variableAssignment));
                }
            });
        }

        // combine into IRTG
        InterpretedTreeAutomaton irtg = features.isEmpty() ? new InterpretedTreeAutomaton(auto) : new MaximumEntropyIrtg(auto, makeFeatureMap(features));
        irtg.addAllInterpretations(interps);

        return irtg;
    }

    private static void forAllInstances(TemplateRule trule, FirstOrderModel model, RuleConsumer consumer) {
        List<Set<String>> nUniverses = new ArrayList<Set<String>>();
        for (int i = 0; i < trule.variables.size(); i++) {
            nUniverses.add(model.getUniverse());
        }

        CartesianIterator<String> it = new CartesianIterator<>(nUniverses);
        while (it.hasNext()) {
            Map<String, String> variableAssignment = makeVariableAssignment(trule.variables, it.next());
            if (trule.guard.isSatisfiedBy(variableAssignment, model)) {
                String parent = instantiate(trule.lhs, variableAssignment);
                String label = instantiate(trule.label, variableAssignment);
                List<String> children = Util.mapList(trule.rhs, x -> instantiate(x, variableAssignment));
                consumer.accept(parent, label, children, trule.weight, variableAssignment);
            }
        }
    }

    private static Map<String, String> makeVariableAssignment(List<String> variables, List<String> values) {
        Map<String, String> ret = new HashMap<>();

        for (int i = 0; i < variables.size(); i++) {
            ret.put(variables.get(i), values.get(i));
        }

        return ret;
    }

    private static String instantiate(String template, Map<String, String> variableAssignment) {
        String ret = template;

        for (String var : variableAssignment.keySet()) {
            ret = ret.replace("$" + var, variableAssignment.get(var));
        }

        return ret;
    }

    private Tree<String> instantiateTerm(Tree<String> term, Map<String, String> variableAssignment) {
        return Util.dfs(term, (node, children) -> Tree.create(instantiate(node.getLabel(), variableAssignment), children));
    }

    private Map<String, FeatureFunction> makeFeatureMap(List<FeatureDeclaration> features) throws ParseException {
        Map<String, FeatureFunction> ret = new HashMap<>();

        for (FeatureDeclaration ft : features) {
            try {
                Constructor<FeatureFunction> con = findFeatureConstructor(ft.featureClass, ft.arguments.size());

                String[] args = new String[ft.arguments.size()];
                ft.arguments.toArray(args);

                FeatureFunction feature = con.newInstance(args);
                ret.put(ft.id, feature);
            } catch (Exception e) {
                throw new ParseException("Could not instantiate FeatureFunction class " + ft.featureClass + " for feature " + ft.id + ": " + e.toString());
            }
        }

        return ret;
    }

    private static interface RuleConsumer {

        void accept(String parent, String label, List<String> children, double weight, Map<String, String> variableAssignment);
    }

    public static class TemplateRule {

        public List<String> variables;
        public Guard guard;
        public String lhs;
        public String label;
        public boolean lhsIsFinal;
        public double weight;
        public List<String> rhs;
        public Map<String, Tree<String>> hom;

        public TemplateRule() {
            variables = new ArrayList<>();
            rhs = new ArrayList<>();
            weight = 1;
            guard = new TopGuard();
            hom = new HashMap<>();
        }

        @Override
        public String toString() {
            return lhs + " -> " + label + rhs;
        }
    }

    public abstract static class Guard {

        abstract boolean isSatisfiedBy(Map<String, String> variableAssignment, FirstOrderModel model);
    }

    public static class AtomicGuard extends Guard {

        private String predicate;
        private List<String> arguments;

        @Override
        boolean isSatisfiedBy(Map<String, String> variableAssignment, FirstOrderModel model) {
            Set<List<String>> extension = model.getInterpretation(predicate);
            List<String> values = new ArrayList<>();

            for (String arg : arguments) {
                if (variableAssignment.containsKey(arg)) {
                    values.add(variableAssignment.get(arg));
                } else {
                    values.add(arg);
                }
            }

            return extension.contains(values);
        }

        public AtomicGuard(String predicate, List<String> arguments) {
            this.predicate = predicate;
            this.arguments = arguments;
        }

        @Override
        public String toString() {
            return predicate + arguments;
        }

    }

    public static class ConjGuard extends Guard {

        private final List<Guard> subguards;

        @Override
        boolean isSatisfiedBy(Map<String, String> variableAssignment, FirstOrderModel model) {
            return subguards.stream().allMatch(sub -> sub.isSatisfiedBy(variableAssignment, model));
        }

        public ConjGuard(List<Guard> subguards) {
            this.subguards = subguards;
        }

        public ConjGuard(Guard s1, Guard s2) {
            this.subguards = new ArrayList<>();
            subguards.add(s1);
            subguards.add(s2);
        }

        @Override
        public String toString() {
            return String.join(" and ", Util.mapList(subguards, x -> x.toString()));
        }
    }

    public static class TopGuard extends Guard {

        @Override
        boolean isSatisfiedBy(Map<String, String> variableAssignment, FirstOrderModel model) {
            return true;
        }

        @Override
        public String toString() {
            return "T";
        }
    }

    private static class FeatureDeclaration {

        public String id;
        public String featureClass;
        public List<String> arguments;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        for (String interp : algebraClasses.keySet()) {
            buf.append("interpretation " + interp + ": " + algebraClasses.get(interp) + "\n");
        }

        for (TemplateRule trule : ruleTemplates) {
            buf.append(trule.variables + " " + trule.guard + "\n");
            buf.append(trule.lhs + (trule.lhsIsFinal ? " !" : "") + " -> " + trule.label + trule.rhs + "\n");
            buf.append("\n");
        }

        return buf.toString();
    }

}
