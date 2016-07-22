/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.taa;

import de.saar.basic.Pair;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * TAA = tree automaton algebra
 * 
 * @author groschwitz
 */
public class TAANode {
    
    
    private TAAOperationImplementation currentImplementation;
    private long lastApplicationTime;
    private long lastApplicationTimeCumulative;
    
    public final void setImplementation(TAAOperationImplementation implementation) {
        currentImplementation = implementation;
    }
    
    public TAAOperationImplementation getImplementation() {
        return currentImplementation;
    }
    
    private Object lastResult;
    
    public Object getLastResult() {
        return lastResult;
    }
    
    public String getResString(Instance instance, InterpretedTreeAutomaton irtg) {
        //reuse result if possible.
        Object res = getLastResult();
        if (res == null) {
            res = applyIterative(instance, irtg);
        }
        
        String ret = "";
        //write evaluation results first
        try {
            ret += "time local: "+lastApplicationTime/1000000+" ms\n";
            ret += "time cumulative: "+lastApplicationTimeCumulative/1000000+" ms\n";
        } catch (java.lang.Exception ex) {
            ret += "could not compute times: "+ex.toString();
        }
        for (NodeResultEvaluator eval: getSelectedInstanceEvaluators()) {
            Pair<Double, Double> evalRes = eval.evaluate(this, instance);
            ret += eval.toString() + ": " + evalRes.left + " (weight " + evalRes.right + ")\n";
        }
        ret +="\n-----------------------------\n\n";
        
        
        //if still null, then e.g. viterbi found no result or similar
        if (res == null) {
            ret+="";//this is symbolic
        } else {
            if (res instanceof TreeAutomaton) {
                TreeAutomaton auto = (TreeAutomaton)res;
                auto.makeAllRulesExplicit();
            }
            ret += res.toString();
        }
        return ret;
    }
    
    private TAAOperationWrapper operation;
    
    private TAANode[] children;
    
    private TAANode parent;
    
    private int posInParent;
    
    public String getStateType() {
        return getOperation().stateType(Arrays.stream(children).map(child -> child.getStateType()).collect(Collectors.toList()));
    }
    
    public String getSignatureType() {
        return getOperation().signatureType(Arrays.stream(children).map(child -> child.getSignatureType()).collect(Collectors.toList()));
    }
    
    private TAANode() {
        this.operation = new TAAOperationWrapper.DummyTAAOperationWrapper();
        setImplementation(operation.getDefaultImplementation());
        children = new TAANode[0];
    }
     
    public TAANode (TAAOperationWrapper operation, int arity) {
        this.operation = operation;
        setImplementation(operation.getDefaultImplementation());
        children = new TAANode[arity];
        for (int i = 0; i<arity; i++) {
            setChild(i, new TAANode());//fill with dummy nodes
        }
    }
    
    /**
     * 
     * @return 
     */
    public int updateArity() {
        TAANode[] oldChildren = children;
        if (operation.arity > children.length) {
            children = new TAANode[operation.arity];
            for (int i = 0; i<oldChildren.length; i++) {
                setChild(i, oldChildren[i]);
            }
            for (int i = oldChildren.length; i<children.length; i++) {
                setChild(i, new TAANode());
            }
        } else if (operation.arity < children.length) {
            int k = children.length;
            while (k> 0 && children[k-1].operation.getClass().equals(TAAOperationWrapper.DummyTAAOperationWrapper.class)
                    && children[k-1].getArity() == 0) {
                k--;
            }
            children = new TAANode[k];
            for (int i = 0; i<children.length; i++) {
                setChild(i, oldChildren[i]);
            }
        }
        return children.length;
    }
    
    private final List<BatchEvaluator> selectedEvaluators = new ArrayList<>();
    
    public void selectEvaluator(BatchEvaluator evaluator) {
        selectedEvaluators.add(evaluator);
    }
    
    public void deselectEvaluator(BatchEvaluator evaluator) {
        selectedEvaluators.remove(evaluator);
    }
    
    public List<BatchEvaluator> getSelectedEvaluators() {
        return selectedEvaluators;
    }
    
    public List<NodeResultEvaluator> getSelectedInstanceEvaluators() {
        List<NodeResultEvaluator> ret = new ArrayList<>();
        for (NodeResultEvaluator candidate : operation.getEvaluations()) {
            boolean found = false;
            for (BatchEvaluator batchEval : getSelectedEvaluators()) {
                if (Arrays.asList(batchEval.getRequiredInstanceEvaluators()).contains(candidate.getCode())) {
                    found = true;
                }
            }
            if (found) {
                ret.add(candidate);
            }
        }
        return ret;
    }
    
    
    public Object apply(List<Object> input, Instance instance, InterpretedTreeAutomaton irtg) {
        try {
            lastResult = getImplementation().apply(input, instance, irtg);
        } catch (NullPointerException ex) {
            boolean anInputIsNull = false;
            for (Object obj : input) {
                if (obj == null) {
                    anInputIsNull = true;
                }
            }
            if (anInputIsNull) {
                //simply propagate the null
                lastResult = null;
            } else {
                throw ex;
            }
        }
        return lastResult;
    }
    
    public Object applyIterative(Instance instance, InterpretedTreeAutomaton irtg) {
        CpuTimeStopwatch watch = new CpuTimeStopwatch();
        watch.record(0);
        List<Object> fromBottomUp = new ArrayList<>();
        for (TAANode children1 : children) {
            fromBottomUp.add(children1.applyIterative(instance, irtg));
        }
        watch.record(1);
        lastResult = apply(fromBottomUp, instance, irtg);
        watch.record(2);
        lastApplicationTime = watch.getTimeBefore(2);
        lastApplicationTimeCumulative = watch.getTimeBefore(1)+watch.getTimeBefore(2);
        return lastResult;
    }
    
    //should not be used, since parent-child relation should only be set via the parent!
    /*public TAANode (TAAOperationWrapper operation, int arity, TAANode parent) {
        this.operation = operation;
        this.parent = parent;
        children = new TAANode[arity];
        for (int i = 0; i<arity; i++) {
            children[i] = new TAANode();
            children[i].setParent(this);
        }
    }*/
    
    public TAAOperationWrapper getOperation() {
        return operation;
    }

    public void setOperation(TAAOperationWrapper operation) {
        this.operation = operation;
        setImplementation(operation.getDefaultImplementation());
        selectedEvaluators.clear();
    }
    
    public boolean isAncestor(TAANode potentialAncestor) {
        TAANode parentIterative = this.getParent();
        int endlessLoopInhibitor = 0;
        while (parentIterative != null) {
            if (parentIterative.equals(potentialAncestor)) {
                return true;
            }
            parentIterative = parentIterative.getParent();
            
            if (endlessLoopInhibitor >100000) {
                break;
            } else {
                endlessLoopInhibitor++;
            }
        }
        return false;
    }
    

    public TAANode getParent() {
        return parent;
    }

    //this is breaking with the convention that a parent is set via parent.setChild(i), is there a cleaner solution?
    public void setNoParent() {
        parent = null;
    }
    
    /*public void setParent(TAANode parent) {
        this.parent = parent;
    }*/
    
    public int getPosInParent() {
        return posInParent;
    }
    
    public final void setChild(int pos, TAANode child) {
        children[pos] = child;
        child.parent = this;
        child.posInParent = pos;
    }
    
    public TAANode getChild(int pos) {
        return children[pos];
    }
    
    public int getArity() {
        return children.length;
    }
    
    public String getName() {
        return operation.getName();
    }

    public long getLastApplicationTime() {
        return lastApplicationTime;
    }

    public long getLastApplicationTimeCumulative() {
        return lastApplicationTimeCumulative;
    }
    
    
    
    public void foreachChild(Consumer<TAANode> function) {
        for (TAANode children1 : children) {
            function.accept(children1);
        }
    }
    
    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.currentImplementation);
        hash = 97 * hash + Objects.hashCode(this.operation);
        hash = 97 * hash + Arrays.deepHashCode(this.children);
        hash = 97 * hash + Objects.hashCode(this.selectedEvaluators);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TAANode other = (TAANode) obj;
        if (!Objects.equals(this.currentImplementation, other.currentImplementation)) {
            return false;
        }
        if (!Objects.equals(this.operation, other.operation)) {
            return false;
        }
        if (!Arrays.deepEquals(this.children, other.children)) {
            return false;
        }
        if (!Objects.equals(this.selectedEvaluators, other.selectedEvaluators)) {
            return false;
        }
        return true;
    }
    
    
    
    
}
