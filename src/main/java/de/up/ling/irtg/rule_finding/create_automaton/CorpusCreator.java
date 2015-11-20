/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.saar.basic.Pair;
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
import de.up.ling.irtg.util.BiFunctionIterable;
import de.up.ling.irtg.util.BiFunctionParallelIterable;
import de.up.ling.irtg.util.FunctionIterable;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private final Supplier<Algebra<InputType1>> firstAlgebra;
    
    /**
     * 
     */
    private final Supplier<Algebra<InputType2>> secondAlgebra;
    
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
    private final VariableIntroduction firstVI;
    
    /**
     * 
     */
    private final VariableIntroduction secondVI;
    
    /**
     * 
     */
    private final int maxThreads;

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
     * @param maxThreads
     */
    protected CorpusCreator(Supplier<Algebra<InputType1>> firstAlgebra, Supplier<Algebra<InputType2>> secondAlgebra, AlignmentFactory firtAL, AlignmentFactory secondAL, Pruner firstPruner, Pruner secondPruner, VariableIntroduction firstVI, VariableIntroduction secondVI, int maxThreads) {
        this.firstAlgebra = firstAlgebra;
        this.secondAlgebra = secondAlgebra;
        this.firtAL = firtAL;
        this.secondAL = secondAL;
        this.firstPruner = firstPruner;
        this.secondPruner = secondPruner;
        this.firstVI = firstVI;
        this.secondVI = secondVI;
        this.maxThreads = maxThreads;
    }
    
    /**
     * 
     * @param firstInputs
     * @param secondInputs
     * @param firstAlignments
     * @param secondAlignments
     * @return
     */
    public Iterable<Pair<TreeAutomaton,HomomorphismManager>> makeRuleTrees(Iterable<String> firstInputs, Iterable<String> secondInputs,
                                            Iterable<String> firstAlignments, Iterable<String> secondAlignments) {
        Propagator pro = new Propagator();
        Iterable<AlignedTrees> firstRoundOne = makeInitialAlignedTrees(firstInputs,
                                                firstAlignments, this.firstAlgebra, this.firtAL);
        Iterable<AlignedTrees> firstRoundTwo = makeFirstPruning(firstRoundOne, firstPruner, firstVI);
        Iterable<AlignedTrees> allFirst = new FunctionIterable<>(firstRoundTwo, (AlignedTrees at) -> {
            return pro.convert(at);
        });
        
        Iterable<AlignedTrees> secondRoundOne = makeInitialAlignedTrees(secondInputs,
                                                secondAlignments, secondAlgebra, secondAL);
        Iterable<AlignedTrees> secondRoundTwo = makeFirstPruning(secondRoundOne, secondPruner, secondVI);
        Iterable<AlignedTrees> allSecond = new FunctionIterable<>(secondRoundTwo, (AlignedTrees at) -> {
            return pro.convert(at);
        });
        
        Iterable<AlignedTrees> firstRoundThree = this.firstPruner.postPrune(allFirst, allSecond);
        Iterable<AlignedTrees> secondRoundThree = this.secondPruner.postPrune(allSecond, allFirst);
        
        return new BiFunctionParallelIterable<>(firstRoundThree,
        secondRoundThree, maxThreads, (AlignedTrees at1, AlignedTrees at2) -> {
            TreeAutomaton first = at1.getTrees();
            TreeAutomaton second = at2.getTrees();
            
            HomomorphismManager hm = new HomomorphismManager(first.getSignature(), second.getSignature());
            hm.update(first.getAllLabels(), second.getAllLabels());
            
            Homomorphism hm1 = hm.getHomomorphismRestriction1(first.getAllLabels(), second.getAllLabels());
            Homomorphism hm2 = hm.getHomomorphismRestriction2(second.getAllLabels(), first.getAllLabels());
            
            RuleFindingIntersectionAutomaton rfi = new RuleFindingIntersectionAutomaton(first, second, hm1, hm2);
            TreeAutomaton done = new TopDownIntersectionAutomaton(rfi, hm.getRestriction());
            
            done = RemoveDead.reduce(done);
            done = hm.reduceToOriginalVariablePairs(done);
            
            return new Pair<>(done,hm);
        });
    }

    /**
     * 
     * @param firstRound
     * @param p
     * @param vi
     * @return 
     */
    public static Iterable<AlignedTrees> makeFirstPruning(final Iterable<AlignedTrees> firstRound,
            final Pruner p, final VariableIntroduction vi) {
        return p.prePrune(new FunctionIterable(firstRound, vi));
    }

    /**
     * 
     * @param <Input>
     * @param inputs
     * @param alignments
     * @param alSupp
     * @param aL
     * @return
     */
    public static <Input> Iterable<AlignedTrees> makeInitialAlignedTrees(final Iterable<String> inputs,
                                                       final Iterable<String> alignments,
                                                       final Supplier<Algebra<Input>> alSupp,
                                                       final AlignmentFactory aL) {
        Algebra algebra = alSupp.get();
        return new BiFunctionIterable<>(inputs, alignments, (String in, String align) -> {          
            try {
                TreeAutomaton ft = algebra.decompose(algebra.parseString(in));
                StateAlignmentMarking fal = aL.makeInstance(align, ft);
                
                return new AlignedTrees(ft, fal);
            } catch (ParserException ex) {
                System.out.println("cannot process: "+in+"\nwith alignments: "+align);               
                Logger.getLogger(CorpusCreator.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return null;
        });
    }

    /**
     * 
     * @return 
     */
    public Supplier<Algebra<InputType1>> getFirstAlgebra() {
        return firstAlgebra;
    }

    /**
     * 
     * @return 
     */
    public Supplier<Algebra<InputType2>> getSecondAlgebra() {
        return secondAlgebra;
    }

    /**
     * 
     * @return 
     */
    public AlignmentFactory getFirtAlignmentFactory() {
        return firtAL;
    }

    /**
     * 
     * @return 
     */
    public AlignmentFactory getSecondAlignmentFactory() {
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
        private int maxThreads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
        
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
         * @param maxThreads
         * @return 
         */
        public Factory setMaxThreads(int maxThreads){
            this.maxThreads = maxThreads;
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
                                                     Supplier<Algebra<Type1>> al1, Supplier<Algebra<Type2>> al2,
                                                     AlignmentFactory af1, AlignmentFactory af2){
            return new CorpusCreator<>(al1, al2, af1, af2, firstPruner, secondPruner, firstVI, secondVI, maxThreads);
        }
    }
}