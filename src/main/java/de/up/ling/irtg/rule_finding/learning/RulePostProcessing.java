/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.math3.util.Pair;

/**
 *
 * @author christoph_teichmann
 * @param <Type1>
 * @param <Type2>
 */
public class RulePostProcessing<Type1,Type2> {
    /**
     * 
     */
    private final static Tree<String> EMPTY = Tree.create("");
    
    /**
     * 
     */
    private final AtomicInteger ai = new AtomicInteger(0);
    
    /**
     * 
     */
    private final ConcreteTreeAutomaton<String> underConstruction;
    
    /**
     * 
     */
    private final Homomorphism firstBuildImage;
    
    /**
     * 
     */
    private final Homomorphism secondBuildImage;

    /**
     * 
     */
    private final Interner<Pair<Tree<String>,Tree<String>>> intern;
    
    /**
     * 
     */
    private final Algebra<Type1> alg1;
    
    /**
     * 
     */
    private final Algebra<Type2> alg2;
    
    /**
     * 
     * @param firstAlg
     * @param secondAlg 
     */
    public RulePostProcessing(Algebra<Type1> firstAlg, Algebra<Type2> secondAlg) {
        this.underConstruction = new ConcreteTreeAutomaton<>();
        firstBuildImage = new Homomorphism(this.underConstruction.getSignature(), firstAlg.getSignature());
        secondBuildImage = new Homomorphism(this.underConstruction.getSignature(), secondAlg.getSignature());
        
        intern = new Interner<>();
        
        this.alg1 = firstAlg;
        this.alg2 = secondAlg;
    }
    
    /**
     * 
     * @param firstAlg 
     */
    public RulePostProcessing(Algebra<Type1> firstAlg) {
        this.underConstruction = new ConcreteTreeAutomaton<>();
        firstBuildImage = new Homomorphism(this.underConstruction.getSignature(), firstAlg.getSignature());
        secondBuildImage = null;
        
        intern = new Interner<>();
        
        this.alg1 = firstAlg;
        this.alg2 = null;
    }
    
    /**
     * Takes a subtree and adds the corresponding rule to the automaton under construction.
     * 
     * 
     * @param input
     * @param firstImage
     * @param secondImage
     * @param isStart 
     */
    public void addRule(Tree<String> input, Homomorphism firstImage,
                                    Homomorphism secondImage, boolean isStart){
        String parent = Variables.getInformation(input.getLabel());
        
        List<String> children = new ArrayList<>();
        addChildren(input.getChildren().get(0),children);
        
        Tree<String> numbered = number(input.getChildren().get(0), new AtomicInteger(1));
        
        Tree<String> firstIm = makeImage(numbered, firstImage);
        Tree<String> secondIm = makeImage(numbered, secondImage);
        int num = this.intern.addObject(new Pair<>(firstIm,secondIm));
        
        String label = parent+"_"+num+"_"+children;
        
        int par = this.underConstruction.addState(parent);
        if(isStart){
            this.underConstruction.addFinalState(par);
        }
        
        this.makeRule(par,label,children);
        
        this.firstBuildImage.add(label, firstIm);
        this.secondBuildImage.add(label, secondIm);
    }

    /**
     * 
     * @param get
     * @param children 
     */
    private void addChildren(Tree<String> get, List<String> children) {
        if(Variables.isVariable(get.getLabel())){
            children.add(Variables.getInformation(get.getLabel()));
        }else{
            for(int i=0;i<get.getChildren().size();++i){
                Tree<String> child = get.getChildren().get(i);
                
                addChildren(child, children);
            }
        }
    }

    /**
     * 
     * @param get
     * @param num
     * @return 
     */
    private Tree<String> number(Tree<String> get, AtomicInteger num) {
        if(Variables.isVariable(get.getLabel())){
            return Tree.create(Variables.createVariable(Integer.toString(num.getAndIncrement())));
        }else{
            String label = get.getLabel();
            List<Tree<String>> children = new ArrayList<>();
            
            for(int i=0;i<get.getChildren().size();++i){
                children.add(number(get.getChildren().get(i), num));
            }
            
            return Tree.create(label, children);
        }
    }

    /**
     * 
     */
    private Tree<String> makeImage(Tree<String> numbered, Homomorphism imager) {
        if(Variables.isVariable(numbered.getLabel())){
            String s = Variables.getInformation(numbered.getLabel());
            
            return Tree.create("?"+s);
        }else{
            Tree<String> mapped = imager.get(numbered.getLabel());
            
            return makeFromOtherSide(mapped, numbered, imager);
        }
    }

    /**
     * 
     * @return 
     */
    public ConcreteTreeAutomaton<String> getAutomaton() {
        return underConstruction;
    }

    /**
     * 
     * @return 
     */
    public Homomorphism getFirstImage() {
        return firstBuildImage;
    }

    /**
     * 
     * @return 
     */
    public Homomorphism getSecondImage() {
        return secondBuildImage;
    }

    /**
     * 
     * @param mapped
     * @param numbered
     * @param imager
     * @return 
     */
    private Tree<String> makeFromOtherSide(Tree<String> mapped, Tree<String> numbered, Homomorphism imager) {
        if(mapped.getLabel().matches("\\?\\d+")){
            int pos = Integer.parseInt(mapped.getLabel().substring(1))-1;
            
            return makeImage(numbered.getChildren().get(pos), imager);
        }else{
            String label = mapped.getLabel();
            List<Tree<String>> children = new ArrayList<>();
            
            for(int i=0;i<mapped.getChildren().size();++i){
                children.add(this.makeFromOtherSide(mapped.getChildren().get(i), numbered, imager));
            }
            
            return Tree.create(label, children);
        }
    }

    /**
     * 
     * @param parent
     * @param label
     * @param children
     * @return 
     */
    private void makeRule(int parent, String label, List<String> children) {
        int lab = this.underConstruction.getSignature().addSymbol(label, children.size());
        Rule r = null;
        
        Iterator<Rule> options = this.underConstruction.getRulesTopDown(lab, parent).iterator();
        int[] kids = toIntArray(children);
        
        while(options.hasNext()){
            Rule option = options.next();
            if(Arrays.equals(option.getChildren(), kids)){
                r = option;
            }
        }
        
        if(r == null){
            this.underConstruction.addRule(this.underConstruction.createRule(parent, lab, kids, 1.0));
        }else{
            r.setWeight(r.getWeight()+1.0);
        }
    }

    /**
     * 
     * @param children
     * @return 
     */
    private int[] toIntArray(List<String> children) {
        int[] arr = new int[children.size()];
        
        for(int i=0;i<children.size();++i){
            arr[i] = this.underConstruction.addState(children.get(i));
        }
        
        return arr;
    }
    
    /**
     * 
     * @param name1
     * @param name2
     * @return 
     */
    public InterpretedTreeAutomaton getIRTG(String name1, String name2){
        InterpretedTreeAutomaton ita = new InterpretedTreeAutomaton(underConstruction);
        
        Interpretation<Type1> inter1 = new Interpretation<>(alg1, this.firstBuildImage);
        ita.addInterpretation(name1, inter1);
        
        Interpretation<Type2> inter2 = new Interpretation<>(alg2, this.secondBuildImage);
        ita.addInterpretation(name2, inter2);
        
        return ita;
    }

    /**
     * 
     * @param name 
     */
    public void addFinalState(String name) {
        this.underConstruction.addFinalState(this.underConstruction.addState(name));
    }

    
    /**
     * 
     * @param input
     * @param firstImage
     * @param isStart 
     */
    public void addRule(Tree<String> input, Homomorphism firstImage, boolean isStart) {
        String parent = Variables.getInformation(input.getLabel());
        
        List<String> children = new ArrayList<>();
        addChildren(input.getChildren().get(0),children);
        
        Tree<String> numbered = number(input.getChildren().get(0), new AtomicInteger(1));
        
        Tree<String> firstIm = makeImage(numbered, firstImage);
        
        int num = this.intern.addObject(new Pair<>(firstIm, EMPTY));
        
        String label = parent+"_"+num+"_"+children;
        
        int par = this.underConstruction.addState(parent);
        if(isStart){
            this.underConstruction.addFinalState(par);
        }
        
        this.makeRule(par,label,children);
        
        this.firstBuildImage.add(label, firstIm);
    }

    /**
     * 
     * @param name1
     * @return 
     */
    public InterpretedTreeAutomaton getIRTG(String name1) {
        InterpretedTreeAutomaton ita = new InterpretedTreeAutomaton(underConstruction);
        
        Interpretation<Type1> inter1 = new Interpretation<>(alg1, this.firstBuildImage);
        ita.addInterpretation(name1, inter1);
        
        return ita;
    }
}
