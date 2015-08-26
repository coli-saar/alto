/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.TreeAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.commons.math3.util.Pair;

/**
 *
 * @author christoph_teichmann
 */
public class RuleFinder {
 
    /**
     * 
     * @param left
     * @param right
     * @param hm
     * @return 
     */
    public TreeAutomaton getRules(TreeAutomaton left, TreeAutomaton right, HomomorphismManager hm){
        hm.update(left.getAllLabels(), right.getAllLabels());
        
        TreeAutomaton combined = hm.getRestriction().intersect(
                left.inverseHomomorphism(hm.getHomomorphism1())).intersect(right.inverseHomomorphism(hm.getHomomorphism2()));
        
        return combined;
    }
    
    /**
     * 
     * @param ruleTrees
     * @param restriction
     * @return 
     */
    private TreeAutomaton generalize(TreeAutomaton ruleTrees, TreeAutomaton restriction){
        ConcreteTreeAutomaton<String> cta = new ConcreteTreeAutomaton<>();
        VisitorStoringVariable vsv = new VisitorStoringVariable(cta);
        
        vsv.setOriginal(ruleTrees);
        vsv.setToken(1);
        vsv.setNegativeWeight(false);
        ruleTrees.foreachStateInBottomUpOrder(vsv);
        
        vsv.setOriginal(restriction);
        vsv.setToken(2);
        vsv.setNegativeWeight(true);
        restriction.foreachStateInBottomUpOrder(vsv);
        
        for(String i : vsv.getWithX()){
            for(String j : vsv.getWithX()){
                Rule r;
                cta.addRule(r = cta.createRule( i, HomomorphismManager.VARIABLE_PREFIX, new String[] {j}));
                r.setWeight(0.5);
            }
        }
        
        return cta;
    }
    
    /**
     * 
     * @param observations
     * @param hm
     * @param smooth
     * @return 
     */
    public TreeAutomaton getAutomatonForObservations(Collection<Tree<String>> observations,
                                                                        HomomorphismManager hm, double smooth){
        ConcreteTreeAutomaton<String> cta = new ConcreteTreeAutomaton<>(hm.getRestriction().getSignature());
        
        int start = cta.addState("XX");
        cta.addFinalState(start);
        double sum = 0.0;
        for(Tree<String> ob : observations){       
            int state = addRecursively(ob,cta,start);
            
            Iterable<Rule> it = cta.getRulesBottomUp(cta.getSignature().getIdForSymbol("XX"), new int[state]);
            Rule r = pick(it,start);
            if(r == null){
                r = cta.createRule(start, cta.getSignature().getIdForSymbol("XX"), new int[] {state},0.0);
                cta.addRule(r);
            }
            
            r.setWeight(r.getWeight()+1.0);
            sum += 1;
        }
        
        Interner<Object> inter = new Interner<>();
        TreeAutomaton rest = hm.getRestriction();
        rest.normalizeRuleWeights();
        
        Iterable<Rule> it = rest.getRuleSet();
        for(Rule r : it){
            String from = makeSimpleState(r.getParent());
            String label = r.getLabel(rest);
            String[] to = transfer(r.getChildren());
            
            if(HomomorphismManager.VARIABLE_PATTERN.test(label)){
                int[] st = new int[] {start};
                Iterable<Rule> cand = cta.getRulesBottomUp(cta.getSignature().getIdForSymbol(label), st);
                Rule choice = this.pick(cand, cta.addState(from));
                if(choice == null){
                    choice = cta.createRule(cta.addState(from), r.getLabel(), st, r.getWeight());
                    cta.addRule(choice);
                }
                
                cand = cta.getRulesBottomUp(cta.getSignature().getIdForSymbol("XX"), new int[] {cta.addState(to[0])});
                choice = this.pick(cand, start);
                if(choice == null){
                    choice = cta.createRule(start, r.getLabel(), new int[] {cta.addState(to[0])}, smooth);
                    sum += smooth;
                    cta.addRule(choice);
                }
            }else{
                cta.addRule(cta.createRule(from, label, to, r.getWeight()));
            }
        }
        
        Iterable<Rule> rules = cta.getRulesTopDown(start);
        for(Rule r : rules){
            r.setWeight(r.getWeight()/sum);
        }
        
        return cta;
    }

    /**
     * 
     * @param r
     * @return 
     */
    static String makeSimpleState(int state) {
        return ")))"+state;
    }
    
    /**
     * 
     * @param ruleTree
     * @param hm
     * @return 
     */
    public TreeAutomaton generalize(TreeAutomaton ruleTree, HomomorphismManager hm){
        return this.generalize(ruleTree, hm.getRestriction());
    }
    
    /**
     * 
     * @param ruleTrees
     * @param hm
     * @return 
     */
    public List<TreeAutomaton> generalizeBulk(Collection<TreeAutomaton> ruleTrees, HomomorphismManager hm){
        List<TreeAutomaton> ret = new ArrayList<>();
        TreeAutomaton restriction = hm.getRestriction();
        
        for(TreeAutomaton t : ruleTrees){
            ret.add(this.generalize(t, restriction));
        }
        
        return ret;
    }
    
    /**
     * 
     * @param ta
     * @return 
     */
    public TreeAutomaton normalize(TreeAutomaton ta){
        ConcreteTreeAutomaton ret = new ConcreteTreeAutomaton(ta.getSignature());
        
        Visitor vis = new Visitor(ret, ta);
        ta.foreachStateInBottomUpOrder(vis);
        
        return ret;
    }
    
    /**
     * 
     * @param tas
     * @return 
     */
    public List<TreeAutomaton> normalizeBulk(Collection<TreeAutomaton> tas){
        List<TreeAutomaton> ret = new ArrayList<>();
        
        for(TreeAutomaton ta : tas){
            ret.add(this.normalize(ta));
        }
        
        return ret;
    }
    
    /**
     * 
     * @param ruleTree
     * @param hm
     * @param lAlg
     * @param rAlg
     * @return 
     */
    public InterpretedTreeAutomaton getInterpretation(TreeAutomaton ruleTree, HomomorphismManager hm,
            Algebra lAlg, Algebra rAlg){
        InterpretedTreeAutomaton ita = new InterpretedTreeAutomaton(ruleTree);
        
        Homomorphism hom = makeXLessHomomorphism(hm.getHomomorphism1(),lAlg);
        Interpretation inter = new Interpretation(lAlg, hom);
        
        ita.addInterpretation("left", inter);
        
        hom = makeXLessHomomorphism(hm.getHomomorphism2(),rAlg);
        inter = new Interpretation(rAlg, hom);
        
        ita.addInterpretation("right", inter);
        
        TreeAlgebra ta = new TreeAlgebra();
        inter = new Interpretation(ta, hm.getHomomorphism1());
        ita.addInterpretation("ruleVizualization1", inter);
        
        ta = new TreeAlgebra();
        inter = new Interpretation(ta, hm.getHomomorphism2());
        ita.addInterpretation("ruleVizualization2", inter);
        
        return ita;
    }
    
    /**
     * 
     * @param left
     * @param right
     * @param hm
     * @param lAlg
     * @param rAlg
     * @return 
     */
    public InterpretedTreeAutomaton getInterpretation(TreeAutomaton left, TreeAutomaton right, HomomorphismManager hm,
                                                        Algebra lAlg, Algebra rAlg){
        return this.getInterpretation(this.getRules(left, right, hm), hm, lAlg, rAlg);
    }

    /**
     * 
     * @param hom
     * @param algebra
     * @return 
     */
    private Homomorphism makeXLessHomomorphism(Homomorphism hom, Algebra algebra) {
        Homomorphism ret = new Homomorphism(hom.getSourceSignature(), algebra.getSignature());
        
        for(String s : hom.getSourceSignature().getSymbols()){
            if(HomomorphismManager.VARIABLE_PATTERN.test(s)){
                ret.add(s, Tree.create("?1"));
            }else{
                ret.add(s, hom.get(s));
            }
        }
        
        return ret;
    }

    /**
     * 
     * @param it
     * @param start
     * @return 
     */
    private Rule pick(Iterable<Rule> it, int state) {
        for(Rule r : it){
            if(r.getParent() == state){
                return r;
            }
        }
        
        return null;
    }

    /**
     * 
     * @param ob
     * @param cta
     * @param start
     * @param i
     * @return 
     */
    private int addRecursively(Tree<String> ob, ConcreteTreeAutomaton<String> cta, int start) {
        int state = this.makeState(ob,cta);
        String label = ob.getLabel();
        int l = cta.getSignature().addSymbol(label, ob.getChildren().size());
        
        if(HomomorphismManager.VARIABLE_PATTERN.test(label)){
            cta.addRule(cta.createRule(state, l, new int[start], 1.0));
            cta.addRule(cta.createRule(start, l, new int[] {makeState(ob.getChildren().get(0),cta)}, 1.0));
        }else{
            int[] a = new int[ob.getChildren().size()];
            int pos = 0;
            for(Tree<String> child : ob.getChildren()){
                a[pos++] = this.makeState(child,cta);
            }
            cta.addRule(cta.createRule(start, l, a, 1.0));
        }
        
                
        for(Tree<String> t : ob.getChildren()){
            addRecursively(t, cta, start);
        }
        
        return state;
    }

    /**
     * 
     * @param children
     * @return 
     */
    private String[] transfer(int[] children) {
        String[] ret = new String[children.length];
        
        for(int i=0;i<children.length;++i){
            ret[i] = makeSimpleState(children[i]);
        }
        
        return ret;
    }

    /**
     * 
     * @param ob
     * @return 
     */
    private int makeState(Tree<String> ob, TreeAutomaton<String> ta) {
        String s = makeString(ob);
        
        return ta.getIdForState(s);
    }

    /**
     * 
     * @param ob
     * @return 
     */
    private String makeString(Tree<String> ob) {
        String label = ob.getLabel();
        
        if(HomomorphismManager.VARIABLE_PATTERN.test(label)){
            return label;
        }else{
            StringBuilder sb = new StringBuilder();
            sb.append(label);
            sb.append("(");
            boolean first = true;
            for(Tree<String> child : ob.getChildren()){
                if(first){
                    first = false;
                }else{
                    sb.append(" ,");
                }
                
                sb.append(makeString(child));
            }
            sb.append(")");
            return sb.toString();
        }
    }
    
    /**
     * 
     */
    private class VisitorStoringVariable implements TreeAutomaton.BottomUpStateVisitor
    {        
        /**
         * the automaton we are constructing.
         */
        private final ConcreteTreeAutomaton<String> goal;
        
        /**
         * the original with which we started.
         */
        private TreeAutomaton original = null;
        
        /**
         * 
         */
        private Object token = null;
        
        /**
         * 
         */
        private final Interner<Pair<Object,Object>> inter = new Interner();
        
        /**
         * 
         */
        private final Set<String> withX = new ObjectOpenHashSet<>();
        
        /**
         * 
         */
        private final Set<String> fromX = new ObjectOpenHashSet<>();
        
        /**
         * 
         */
        private boolean negativeWeight = false;
        
        /**
         * Construct a new instance.
         * 
         */
        public VisitorStoringVariable(ConcreteTreeAutomaton<String> goal) {
            this.goal = goal;
        }

        /**
         * 
         * @param original 
         */
        public void setOriginal(TreeAutomaton original) {
            this.original = original;
        }

        /**
         * 
         * @param token 
         */
        public void setToken(Object token) {
            this.token = token;
        }

        /**
         * 
         * @param negativeWeight 
         */
        public void setNegativeWeight(boolean negativeWeight) {
            this.negativeWeight = negativeWeight;
        }
        
        /**
         * 
         * @return 
         */
        public Set<String> getWithX() {
            return withX;
        }

        /**
         * 
         * @return 
         */
        public Set<String> getFromX() {
            return fromX;
        }
        
        @Override
        public void visit(int state, Iterable<Rule> rulesTopDown) {
            String st = this.makeState(state);
            
            if(original.getFinalStates().contains(state))
            {
                this.fromX.add(st);
                this.goal.addFinalState(this.goal.getIdForState(st));
            }
            
            for(Rule r : rulesTopDown)
            {
                String[] arr = makeCopy(r.getChildren());
                String label = r.getLabel(original);
                if(HomomorphismManager.VARIABLE_PATTERN.test(st)){
                    this.withX.add(st);
                    this.fromX.add(arr[0]);
                    this.goal.addFinalState(this.goal.getIdForState(arr[0]));
                    continue;
                }
                
                double weight = this.negativeWeight ? -1 : 1;
                
                this.goal.addRule(goal.createRule(st, label, arr, weight));
            }
        }

        /**
         * Creates a copy of a rules child states
         * 
         * @param children
         * @return 
         */
        private String[] makeCopy(int[] children) {
            String[] obs = new String[children.length];
            
            for (int i = 0; i < children.length; i++) {
                int state = children[i];
                String val = makeState(state);
                
                obs[i] = val;
            }
            
            return obs;
        }

        /**
         * 
         * @param state
         * @return 
         */
        String makeState(int state) {
            int ret = this.inter.addObject(new Pair(this.token, this.original.getStateForId(state)));
            String r = Integer.toString(ret);
            this.goal.addState(r);
            return r;
        }
        
    }
    
    /**
     * 
     */
    private class Visitor implements TreeAutomaton.BottomUpStateVisitor
    {        
        /**
         * the automaton we are constructing.
         */
        private final ConcreteTreeAutomaton goal;
        
        /**
         * the original with which we started.
         */
        private final TreeAutomaton original;
        

        /**
         * Construct a new instance.
         * 
         */
        public Visitor(ConcreteTreeAutomaton goal, TreeAutomaton original) {
            this.goal = goal;
            this.original = original;
        }
        
        
        @Override
        public void visit(int state, Iterable<Rule> rulesTopDown) {
            Object st = this.original.getStateForId(state);
            
            if(original.getFinalStates().contains(state))
            {
                this.goal.addFinalState(this.goal.getIdForState(st));
            }
            
            for(Rule r : rulesTopDown)
            {
                Object[] arr = makeCopy(r.getChildren());
                String label = r.getLabel(original);
                if(HomomorphismManager.VARIABLE_PATTERN.test(label)){
                    label = HomomorphismManager.VARIABLE_PREFIX;
                }
                
                double weight = r.getWeight();
                
                this.goal.addRule(goal.createRule(st, label, arr, weight));
            }
        }

        /**
         * Creates a copy of a rules child states
         * 
         * @param children
         * @return 
         */
        private Object[] makeCopy(int[] children) {
            Object[] obs = new Object[children.length];
            
            for (int i = 0; i < children.length; i++) {
                obs[i] = this.original.getStateForId(children[i]);
            }
            
            return obs;
        }
        
    }
}