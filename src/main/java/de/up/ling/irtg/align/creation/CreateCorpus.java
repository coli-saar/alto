/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.creation;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.align.HomomorphismManager;
import de.up.ling.irtg.align.Propagator;
import de.up.ling.irtg.align.StateAlignmentMarking;
import de.up.ling.irtg.align.pruning.RemoveDead;
import de.up.ling.irtg.automata.RuleFindingIntersectionAutomaton;
import de.up.ling.irtg.automata.TopDownIntersectionAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.function.Function;

/**
 *
 * @author christoph_teichmann
 * @param <Type1>
 * @param <Type2>
 */
public class CreateCorpus<Type1,Type2> {
 
    /**
     * 
     */
    private final HomomorphismManager mainMan;
    
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
     * @param alg1
     * @param alg2 
     */
    public CreateCorpus(Algebra<Type1> alg1, Algebra<Type2> alg2) {
        this.alg1 = alg1;
        this.alg2 = alg2;
        this.mainMan = new HomomorphismManager(alg1.getSignature(), alg2.getSignature());
    }
    
    /**
     * 
     * @param left
     * @param right
     * @return
     * @throws ParserException 
     */
    public List<TreeAutomaton> makeDataSet(List<InputPackage> left, List<InputPackage> right) throws ParserException{
        List<TreeAutomaton> ret = new ObjectArrayList<>();
        
        for(int i=0;i<left.size() && i<right.size();++i){
            ret.add(this.makeEntry(left.get(i), right.get(i)));
        }
        
        return ret;
    }
    
    
    /**
     * 
     * @param in1
     * @param in2
     * @return
     * @throws ParserException 
     */
    public TreeAutomaton makeEntry(InputPackage<Type1> in1,InputPackage<Type2> in2) throws ParserException{
        TreeAutomaton ta1 = in1.convert(alg1);
        TreeAutomaton ta2 = in2.convert(alg2);
        
        HomomorphismManager homa = new HomomorphismManager(alg1.getSignature(), alg2.getSignature(),
                mainMan.getSignature());
        mainMan.update(ta1.getAllLabels(), ta2.getAllLabels());
        homa.update(ta1.getAllLabels(), ta2.getAllLabels());
        
        RuleFindingIntersectionAutomaton rfa = 
                new RuleFindingIntersectionAutomaton(ta1, ta2, homa.getHomomorphism1(), homa.getHomomorphism2());
        TreeAutomaton ret = new TopDownIntersectionAutomaton(rfa, homa.getRestriction());
        ret = RemoveDead.reduce(ret);
        
        return ret;
    }

    /**
     * 
     * @return 
     */
    public HomomorphismManager getMainManager() {
        return mainMan;
    }

    /**
     * 
     * @return 
     */
    public Algebra<Type1> getAlgebra1() {
        return alg1;
    }

    /**
     * 
     * @return 
     */
    public Algebra<Type2> getAlgebra2() {
        return alg2;
    }

    /**
     * 
     * @param <Type> 
     */
    public class InputPackage<Type>{
        /**
         * 
         */
        private final String input;
        
        /**
         * 
         */
        private final Function<Type,Propagator> props;
        
        /**
         * 
         */
        private final StateAlignmentMarking sam;
        
        /**
         * 
         * @param input
         * @param props
         * @param sam
         */
        public InputPackage(String input, Function<Type, Propagator> props,
                StateAlignmentMarking sam) {
            this.input = input;
            this.props = props;
            this.sam = sam;
        }
        
        /**
         * 
         * @param alg
         * @return
         * @throws ParserException 
         */
        private TreeAutomaton convert(Algebra<Type> alg) throws ParserException {
            Type x = alg.parseString(input);
            
            Propagator prop = props.apply(x);
            
            TreeAutomaton ta = alg.decompose(x);
            ta = prop.convert(ta, sam);
            return ta;
        }
    }
}