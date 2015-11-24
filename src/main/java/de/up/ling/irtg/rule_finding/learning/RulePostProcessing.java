/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author christoph_teichmann
 */
public class RulePostProcessing {
    /**
     * 
     */
    private final AtomicInteger ai = new AtomicInteger(0);
    
    /**
     * 
     */
    private final String LABEL_BASE_NAME = "rule_";
    
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
     * @param firstTarget
     * @param secondTarget
     */
    public RulePostProcessing(Signature firstTarget, Signature secondTarget) {
        this.underConstruction = new ConcreteTreeAutomaton<>();
        firstBuildImage = new Homomorphism(this.underConstruction.getSignature(), firstTarget);
        secondBuildImage = new Homomorphism(this.underConstruction.getSignature(), secondTarget);
    }
    
    /**
     * 
     * @param input
     * @param firstImage
     * @param secondImage
     * @param isStart 
     */
    public void addRule(Tree<String> input, Homomorphism firstImage, Homomorphism secondImage, boolean isStart){
        String label = LABEL_BASE_NAME+this.ai.getAndIncrement();
        String parent = input.getLabel();
        
        List<String> children = new ArrayList<>();
        addChildren(input.getChildren().get(0),children);
        
        Tree<String> numbered = number(input.getChildren().get(0), new AtomicInteger(1));
        
        Tree<String> firstIm = makeImage(numbered, firstImage);
        Tree<String> secondIm = makeImage(numbered, secondImage);
        
        int par = this.underConstruction.addState(parent);
        if(isStart){
            this.underConstruction.addFinalState(par);
        }
        
        this.underConstruction.addRule(this.underConstruction.createRule(parent, label, children));
        this.firstBuildImage.add(label, firstIm);
        this.secondBuildImage.add(label, secondIm);
    }

    /**
     * 
     * @param get
     * @param children 
     */
    private void addChildren(Tree<String> get, List<String> children) {
        if(Variables.IS_VARIABLE.test(get.getLabel())){
            children.add(get.getLabel());
        }else{
            for(int i=0;i<get.getChildren().size();++i){
                Tree<String> child = get.getChildren().get(i);
                
                addChildren(get, children);
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
        if(Variables.IS_VARIABLE.test(get.getLabel())){
            return Tree.create(Variables.makeVariable(Integer.toString(num.getAndIncrement())));
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
        if(Variables.IS_VARIABLE.test(numbered.getLabel())){
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
        if(mapped.getLabel().startsWith("?")){
            int pos = Integer.parseInt(mapped.getLabel().substring(1));
            
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
}
