/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.rule_finding.alignments.AlignmentFactory;
import de.up.ling.irtg.rule_finding.pruning.RemoveDead;
import de.up.ling.irtg.automata.RuleFindingIntersectionAutomaton;
import de.up.ling.irtg.automata.TopDownIntersectionAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.rule_finding.pruning.Pruner;
import de.up.ling.irtg.rule_finding.variable_introduction.LeftRightXFromFinite;
import de.up.ling.irtg.rule_finding.variable_introduction.VariableIntroduction;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author christoph_teichmann
 * @param <InputType1>
 * @param <InputType2>
 */
public class CorpusCreator<InputType1,InputType2> {
    
    /**
     * 
     */
    private final Algebra<InputType1> firstAlgebra;
    
    /**
     * 
     */
    private final Algebra<InputType2> secondAlgebra;
    
    /**
     * 
     */
    private final AlignmentFactory firtAL;
    
    /**
     * 
     */
    private final AlignmentFactory secondAL;
    
    /**
     * 
     */
    private final Pruner firstPruner;
    
    /**
     * 
     */
    private final Pruner secondPruner;
    
    /**
     * 
     */
    private final HomomorphismManager hm;
    
    /**
     * 
     */
    private final VariableIntroduction firstVI;
    
    /**
     * 
     */
    private final VariableIntroduction secondVI;

    /**
     * 
     * @param firstAlgebra
     * @param secondAlgebra
     * @param firtAL
     * @param secondAL
     * @param firstPruner
     * @param secondPruner
     * @param firstVI
     * @param secondVI
     */
    protected CorpusCreator(Algebra<InputType1> firstAlgebra, Algebra<InputType2> secondAlgebra, AlignmentFactory firtAL, AlignmentFactory secondAL, Pruner firstPruner, Pruner secondPruner, VariableIntroduction firstVI, VariableIntroduction secondVI) {
        this.firstAlgebra = firstAlgebra;
        this.secondAlgebra = secondAlgebra;
        this.firtAL = firtAL;
        this.secondAL = secondAL;
        this.firstPruner = firstPruner;
        this.secondPruner = secondPruner;
        this.hm = new HomomorphismManager(firstAlgebra.getSignature(), secondAlgebra.getSignature());
        this.firstVI = firstVI;
        this.secondVI = secondVI;
    }
    
    /**
     * 
     * @param firstInputs
     * @param secondInputs
     * @param firstAlignments
     * @param secondAlignments
     * @return 
     * @throws de.up.ling.irtg.algebra.ParserException 
     */
    public List<TreeAutomaton> makeRuleTrees(List<String> firstInputs, List<String> secondInputs,
                                            List<String> firstAlignments, List<String> secondAlignments) throws ParserException{
        int maxSize = Math.min(firstInputs.size(), Math.min(secondInputs.size(),
                    Math.min(firstAlignments.size(), secondAlignments.size())));
                
        Propagator pro = new Propagator();
        List<AlignedTrees> firstRoundOne = makeInitialAlignedTrees(maxSize, firstInputs,
                                                firstAlignments, this.firstAlgebra, this.secondAL);
        List<AlignedTrees> firstRoundTwo = makeFirstPruning(firstRoundOne, firstPruner, firstVI);
        for(int i=0;i<firstRoundTwo.size();++i){
            firstRoundTwo.set(i, pro.convert(firstRoundTwo.get(i)));
        }
        
        List<AlignedTrees> secondRoundOne = makeInitialAlignedTrees(maxSize, secondInputs,
                                                secondAlignments, secondAlgebra, secondAL);
        List<AlignedTrees> secondRoundTwo = makeFirstPruning(secondRoundOne, secondPruner, secondVI);
        for(int i=0;i<secondRoundTwo.size();++i){
            secondRoundTwo.set(i, pro.convert(secondRoundTwo.get(i)));
        }
        
        List<AlignedTrees> firstRoundThree = this.firstPruner.postPrune(firstRoundTwo, secondRoundTwo);
        List<AlignedTrees> secondRoundThree = this.secondPruner.postPrune(secondRoundTwo, firstRoundTwo);
        
        List<TreeAutomaton> result = new ArrayList<>();
        for(int i=0;i<firstRoundThree.size();++i){
            TreeAutomaton first = firstRoundThree.get(i).getTrees();
            TreeAutomaton second = secondRoundThree.get(i).getTrees();
            
            hm.update(first.getAllLabels(), second.getAllLabels());
        }
        
        for(int i=0;i<firstRoundThree.size();++i){
            TreeAutomaton first = firstRoundThree.get(i).getTrees();
            TreeAutomaton second = secondRoundThree.get(i).getTrees();
            
            Homomorphism hm1 = hm.getHomomorphismRestriction1(first.getAllLabels(), second.getAllLabels());
            Homomorphism hm2 = hm.getHomomorphismRestriction2(second.getAllLabels(), first.getAllLabels());
            
            RuleFindingIntersectionAutomaton rfi = new RuleFindingIntersectionAutomaton(first, second, hm1, hm2);
            TreeAutomaton done = new TopDownIntersectionAutomaton(rfi, hm.getRestriction());
            
            done = RemoveDead.reduce(done);
            done = hm.reduceToOriginalVariablePairs(done);
            result.add(done);
        }
        
        return result;
    }

    /**
     * 
     * @param firstRoundOne
     * @param p
     * @param vi
     * @return 
     */
    public static List<AlignedTrees> makeFirstPruning(List<AlignedTrees> firstRoundOne,
            Pruner p, VariableIntroduction vi) {
        for(int i=0;i<firstRoundOne.size();++i){
            firstRoundOne.set(i, vi.apply(firstRoundOne.get(i)));
        }
        List<AlignedTrees> firstRoundTwo = p.prePrune(firstRoundOne);
        return firstRoundTwo;
    }

    /**
     * 
     * @param maxSize
     * @param firstInputs
     * @param firstAlignments
     * @param algebra
     * @param aL
     * @return
     * @throws ParserException 
     */
    public static List<AlignedTrees> makeInitialAlignedTrees(int maxSize,
                                                       List<String> firstInputs,
                                                       List<String> firstAlignments,
                                                       Algebra algebra,
                                                       AlignmentFactory aL) throws ParserException {
        List<AlignedTrees> firstRoundOne = new ArrayList<>();
        for(int i=0;i<maxSize;++i){
            TreeAutomaton ft = algebra.decompose(algebra.parseString(firstInputs.get(i)));
            StateAlignmentMarking fal = aL.makeInstance(firstAlignments.get(i), ft);
            firstRoundOne.add(new AlignedTrees<>(ft,fal));
            if(ft.getSignature() != algebra.getSignature()){
                throw new IllegalStateException("Signatures changed during processing");
            }
        }
        
        return firstRoundOne;
    }

    /**
     * 
     * @return 
     */
    public Algebra<InputType1> getFirstAlgebra() {
        return firstAlgebra;
    }

    /**
     * 
     * @return 
     */
    public Algebra<InputType2> getSecondAlgebra() {
        return secondAlgebra;
    }
    
    /**
     * 
     * @return 
     */
    public HomomorphismManager getHomomorphismManager() {
        return hm;
    }

    /**
     * 
     * @return 
     */
    public AlignmentFactory getFirtAL() {
        return firtAL;
    }

    /**
     * 
     * @return 
     */
    public AlignmentFactory getSecondAL() {
        return secondAL;
    }

    /**
     * 
     * @return 
     */
    public Pruner getFirstPruner() {
        return firstPruner;
    }

    /**
     * 
     * @return 
     */
    public Pruner getSecondPruner() {
        return secondPruner;
    }

    /**
     * 
     * @return 
     */
    public VariableIntroduction getFirstVI() {
        return firstVI;
    }

    /**
     * 
     * @return 
     */
    public VariableIntroduction getSecondVI() {
        return secondVI;
    }
    
    /**
     * 
     */
    public static class Factory {
        /**
         * 
         */
        private Pruner firstPruner = Pruner.DEFAULT_PRUNER;
    
        /**
         * 
        */
        private Pruner secondPruner = Pruner.DEFAULT_PRUNER;
    
        /**
        * 
        */
        private VariableIntroduction firstVI = new LeftRightXFromFinite();
    
        /**
        * 
        */
        private VariableIntroduction secondVI = new LeftRightXFromFinite();
        
        /**
         * 
         * @param vi
         * @return 
         */
        public Factory setFirstVariableSource(VariableIntroduction vi){
            this.firstVI = vi;
            return this;
        }
        
        /**
         * 
         * @param vi
         * @return 
         */
        public Factory setSecondVariableSource(VariableIntroduction vi){
            this.secondVI = vi;
            return this;
        }
        
        /**
         * 
         * @param prun
         * @return 
         */
        public Factory setSecondPruner(Pruner prun){
            this.secondPruner = prun;
            return this;
        }
        
        /**
         * 
         * @param prun
         * @return 
         */
        public Factory setFirstPruner(Pruner prun){
            this.firstPruner = prun;
            return this;
        }
        
        
        public <Type1,Type2> CorpusCreator<Type1,Type2> getInstance(
                                                     Algebra<Type1> al1, Algebra<Type2> al2,
                                                     AlignmentFactory af1, AlignmentFactory af2){
            return new CorpusCreator<>(al1, al2, af1, af2, firstPruner, secondPruner, firstVI, secondVI);
        }
    }
}