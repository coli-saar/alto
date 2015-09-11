/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.align.HomomorphismManager;
import de.up.ling.irtg.align.Propagator;
import de.up.ling.irtg.align.StateAlignmentMarking;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleFindingIntersectionAutomaton;
import de.up.ling.irtg.automata.TopDownIntersectionAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.ArraySampler;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.random.Well44497b;

/**
 *
 * @author christoph_teichmann
 */
public class DividedSampling {
    
    /**
     * 
     */
    private final int threads;
    
    
    public DividedSampling(int threads){
        this.threads = threads;
    }
    
    /**
     * 
     * @param left
     * @param right
     * @param leftAlign
     * @param rightAlign
     * @param leftSig
     * @param rightSig
     * @param prop
     * @param drive
     * @param stW
     * @return 
     */
    public InterpretedTreeAutomaton findRules(List<TreeAutomaton> left, List<TreeAutomaton> right,
                                            List<StateAlignmentMarking> leftAlign,
                                            List<StateAlignmentMarking> rightAlign, Signature leftSig,
                                            Signature rightSig, Propagator prop, SampleDriver drive,
                                            StateWeighter stW){
        Signature shared = new Signature();
        List<HomomorphismManager> connections = new ArrayList<>();
        
        HomomorphismManager complete = makeConnections(left, right, prop, leftAlign, rightAlign,
                leftSig, rightSig, shared, connections);
        
        VariableIndication vi = new VariableIndicationByLookUp(complete.getHomomorphism1());
        TreeAddingAutomaton interMediateModel = new TreeAddingAutomaton(shared,
                            new ConstantSmooth(drive.getSmoothing()), vi);
        DoubleList weights = new DoubleArrayList();
        List<Tree<Rule>> samples = new ObjectArrayList<>();
        ArraySampler arrSam = new ArraySampler(new Well44497b());
        
        makeInitialModel(left, right, prop, leftAlign, rightAlign, drive, stW,
                connections, samples, weights, vi, interMediateModel, complete, arrSam);
        
        for (int k = 0; k < drive.rounds(); ++k) {

            TreeAutomaton model = interMediateModel;
            interMediateModel = new TreeAddingAutomaton(shared, new ConstantSmooth(drive.getSmoothing()), vi);
            
            TreeAutomaton forwardOne = HomAutomaton(model, complete.getHomomorphism1());
            TreeAutomaton fowardTwo  = HomAutomaton(model, complete.getHomomorphism2());
            
            for (int i = 0; i < leftAlign.size() && i < rightAlign.size(); ++i) {
                TreeAutomaton t1 = left.get(i);
                TreeAutomaton t2 = right.get(i);

                t1 = downSample(t1, drive, i, true, false);
                t2 = downSample(t2, drive, i, false, false);

                t1 = prop.convert(t1, leftAlign.get(i));
                t2 = prop.convert(t2, rightAlign.get(i));

            }
        }
        //TODO
        return null;
    }

    /**
     * 
     * @param left
     * @param right
     * @param prop
     * @param leftAlign
     * @param rightAlign
     * @param drive
     * @param stW
     * @param connections
     * @param samples
     * @param weights
     * @param vi
     * @param interMediateModel
     * @param complete
     * @param arrSam 
     */
    private void makeInitialModel(List<TreeAutomaton> left, List<TreeAutomaton> right, Propagator prop,
            List<StateAlignmentMarking> leftAlign, List<StateAlignmentMarking> rightAlign,
            SampleDriver drive, StateWeighter stW,
            List<HomomorphismManager> connections, List<Tree<Rule>> samples,
            DoubleList weights, VariableIndication vi, TreeAddingAutomaton interMediateModel,
            HomomorphismManager complete, ArraySampler arrSam) {
        
        for(int i=0;i<left.size() && i<right.size();++i){
            TreeAutomaton t1 = left.get(i);
            TreeAutomaton t2 = right.get(i);
            
            Int2ObjectMap<IntSet> setL = prop.propagate(t1, leftAlign.get(i));
            Int2ObjectMap<IntSet> setR = prop.propagate(t2, rightAlign.get(i));
            
            reduceToShared(setL, setR);
            
            reweight(t1, t2, setL, setR);
            
            t1 = downSample(t1, drive, i, true, true);
            t2 = downSample(t2, drive, i, false, true);
            
            t1 = prop.convert(t1, leftAlign.get(i));
            t2 = prop.convert(t2, rightAlign.get(i));
            
            HomomorphismManager hom = connections.get(i);
            
            TreeAutomaton ta = new TopDownIntersectionAutomaton(
                    new RuleFindingIntersectionAutomaton(t1, t2, hom.getHomomorphism1(),
                            hom.getHomomorphism2()), hom.getRestriction());
            
            stW.setBasis(ta);
            
            samples.clear();
            weights.clear();
            for(int k=0;k<drive.getInitialSampleCount(i);++k){
                Tree<Rule> sample = stW.nextSample();
                double weight = -stW.logWeightOfLastSample();
                weight += this.makeLogWeight(sample,vi,interMediateModel,complete);
                
                samples.add(sample);
                weights.add(weight);
            }
            
            arrSam.turnIntoCWF(weights);
            for(int k=0;k<samples.size();++k){
                addSample(vi,interMediateModel,complete,samples.get(arrSam.produceSample(weights)));
            }
        }
    }

    /**
     * 
     * @param left
     * @param right
     * @param prop
     * @param leftAlign
     * @param rightAlign
     * @param leftSig
     * @param rightSig
     * @param shared
     * @param connections 
     */
    private HomomorphismManager makeConnections(List<TreeAutomaton> left, List<TreeAutomaton> right,
            Propagator prop,
            List<StateAlignmentMarking> leftAlign,
            List<StateAlignmentMarking> rightAlign,
            Signature leftSig, Signature rightSig,
            Signature shared, List<HomomorphismManager> connections) {
        
        HomomorphismManager complete = new HomomorphismManager(leftSig, rightSig, shared);
        IntSet varsL = new IntOpenHashSet();
        IntSet varsR = new IntOpenHashSet();
        
        for(int i=0;i<left.size() && i<right.size();++i){
            TreeAutomaton t1 = left.get(i);
            TreeAutomaton t2 = right.get(i);
            
            Int2ObjectMap<IntSet> setL = prop.propagate(t1, leftAlign.get(i));
            Int2ObjectMap<IntSet> setR = prop.propagate(t2, rightAlign.get(i));
            
            dumpVars(varsL,setL, prop);
            dumpVars(varsR,setR, prop);
            
            HomomorphismManager hom = new HomomorphismManager(leftSig, rightSig, shared);
            hom.update(t1.getAllLabels(), t2.getAllLabels());
            hom.update(varsL, varsR);
            
            complete.update(t1.getAllLabels(), t2.getAllLabels());
            complete.update(varsL, varsR);
            
            connections.add(hom);
        }
        
        return complete;
    }

    /**
     * 
     * @param varsL
     * @param setL
     * @param prop 
     */
    private void dumpVars(IntSet varsL, Int2ObjectMap<IntSet> setL, Propagator prop) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * 
     * @param setL
     * @param setR 
     */
    private void reduceToShared(Int2ObjectMap<IntSet> setL, Int2ObjectMap<IntSet> setR) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * 
     * @param t1
     * @param t2
     * @param setL
     * @param setR 
     */
    private void reweight(TreeAutomaton t1, TreeAutomaton t2, Int2ObjectMap<IntSet> setL, Int2ObjectMap<IntSet> setR) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * 
     * @param automaton
     * @param drive
     * @param position
     * @param isLeft
     * @return 
     */
    private TreeAutomaton downSample(TreeAutomaton automaton, SampleDriver drive, int position, boolean isLeft,
                                    boolean isInitial) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * 
     * @param sample
     * @param vi
     * @param interMediateModel
     * @return 
     */
    private double makeLogWeight(Tree<Rule> sample, VariableIndication vi, TreeAddingAutomaton interMediateModel,
                                HomomorphismManager complete) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void addSample(VariableIndication vi, TreeAddingAutomaton interMediateModel, HomomorphismManager complete, Tree<Rule> get) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private TreeAutomaton HomAutomaton(TreeAutomaton model, Homomorphism homomorphism1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}