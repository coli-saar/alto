/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.taa;

import de.saar.basic.Pair;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.corpus.Instance;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Mostly a wrapper class for TAANodes.
 * @author groschwitz
 */
public class TAATree {
    
    
    private TAANode root;


    public TAANode getRoot() {
        return root;
    }

    public void setRoot(TAANode root) {
        this.root = root;
        root.setNoParent();
    }
    
    
    public TAATree makeCopy() {
        TAATree ret = new TAATree();
        final Map<TAANode, TAANode> old2new = new HashMap<>();
        foreachNodeTopDown(taaNode -> {
            TAANode newTAANode = new TAANode(taaNode.getOperation(), taaNode.getArity());
            for (BatchEvaluator eval : taaNode.getSelectedEvaluators()) {
                newTAANode.selectEvaluator(eval);
            }
            newTAANode.setImplementation(taaNode.getImplementation());
            old2new.put(taaNode, newTAANode);
            if (root.equals(taaNode)) {
                ret.setRoot(newTAANode);
            } else {
                //then taaNode has a parent. that parent is in old2new, since this is topdown
                old2new.get(taaNode.getParent()).setChild(taaNode.getPosInParent(), newTAANode);//this also sets the parent in newTAANode
            }
        });
        return ret;
    }
    
    /**
     * Returns all leaves of the tree.
     * This is not a lazy implementation, returns a freshly computed result every time.
     * Returns null if a node has an undefined child,
     * which should never happen though due to the DummyTAANodes.
     * @return 
     */
    public List<TAANode> getAllLeaves() {
        return getLeaves(root);
    }
    
    
    private List<TAANode> getLeaves(TAANode node) {
        if (node.getArity() == 0) {
            return Collections.singletonList(node);
        } else {
            List<TAANode> ret = new ArrayList<>();
            for (int i = 0; i<node.getArity(); i++) {
                TAANode childNode = node.getChild(i);
                if (childNode == null) {
                    return null;
                } else {
                    List<TAANode> childRes = getLeaves(childNode);
                    if (childRes == null) {
                        return null;
                    } else {
                        ret.addAll(childRes);
                    }
                }
            }
            return ret;
        }
    }
    
    /**
     * returns true iff the tree consists of exactly a dummy node, or is empty
     * @return 
     */
    public boolean isTrivial() {
        return root == null || (root.getArity() == 0 && root.getOperation().equals(TAAOperationWrapper.DUMMY));   
    }
    
    /**
     * Returns -1 if node is not in tree.
     * @param node
     * @return 
     */
    public int getLayerForNode(TAANode node) {
       if (node.getParent() == null) {
            if (node.equals(root)) {
                return 0;
            } else {
                return -1;
            }
        } else {
            int parentLayer = getLayerForNode(node.getParent());
            if (parentLayer == -1) {
                return -1;//then also this node is not in the tree
            } else {
                int ret = parentLayer+1;
                return ret;
            }
        }
    }
    
    public Object evaluate(Instance instance, InterpretedTreeAutomaton irtg) {
        /*Map<TAANode, Object> node2res = new HashMap<>();
        foreachNodeBottomUp(node -> {
            List<Object> input = new ArrayList<>();
            node.foreachChild(child -> input.add(node2res.get(child)));
            node2res.put(node, node.apply(input, instance));
        });
        return node2res.get(root);*/
        return root.applyIterative(instance, irtg);
    }
    
    public void foreachNodeBottomUp(Consumer<TAANode> function) {
        Object2IntMap<TAANode> missingArities = new Object2IntOpenHashMap<>();
        foreachNodeTopDown(node -> missingArities.put(node, node.getArity()));
        
        List<TAANode> agenda = new ArrayList<>();
        agenda.addAll(getAllLeaves());
        
        //compute widths and layers
        for (int j = 0; j<agenda.size(); j++) {
            TAANode currentNode = agenda.get(j);
            
            function.accept(currentNode);
            
            TAANode parent = currentNode.getParent();
            if (parent != null) {
                int newParentMissingArities = missingArities.get(parent)-1;
                missingArities.put(parent, newParentMissingArities);
                if (newParentMissingArities == 0) {
                    agenda.add(parent);
                }
            }
        }
    }
    
    public void foreachNodeTopDown(Consumer<TAANode> function) {
        foreachNodeDownward(root, function);
    }
    
    private static void foreachNodeDownward(TAANode node, Consumer<TAANode> function) {
        function.accept(node);
        node.foreachChild(child -> {
            foreachNodeDownward(child, function);
        });
    }
    
    public <T> T applyRecursive(Function<Pair<TAANode, List<T>>, T> function) {
        return applyingRecursion(function, root);
    }
    
    private <T> T applyingRecursion(Function<Pair<TAANode, List<T>>, T> function, TAANode current) {
        List<T> childRes = new ArrayList<>();
        for (int pos = 0; pos < current.getArity(); pos++) {
            childRes.add(applyingRecursion(function, current.getChild(pos)));
        }
        return (T)function.apply(new Pair(current, childRes));
    }
    
    /**
     * returns the path to node (shortest top-down, left to right), 0-based.
     * @param gold
     * @return 
     */
    public IntList getPathToNode(TAANode gold) {
        return getPathToNodeRecursion(gold, root, new IntArrayList());
    }
    
    private static IntList getPathToNodeRecursion(TAANode gold, TAANode current, IntList pathSoFar) {
        if (current.equals(gold)) {
            return pathSoFar;
        }
        for (int pos = 0; pos < current.getArity(); pos++) {
            TAANode child = current.getChild(pos);
            IntList newPath = new IntArrayList(pathSoFar);
            newPath.add(pos);
            IntList resHere = getPathToNodeRecursion(gold, child, newPath);
            if (resHere != null) {
                return resHere;
            }
        }
        //return null if gold was found neither here nor below
        return null;
    }
    
    public TAANode getNodeByPath(IntList path) {
        TAANode current = root;
        for (int pos : path) {
            if (current != null && current.getArity()>pos) {
                current = current.getChild(pos);
            } else {
                return null;
            }
        }
        return current;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.root);
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
        final TAATree other = (TAATree) obj;
        if (!Objects.equals(this.root, other.root)) {
            return false;
        }
        return true;
    }
    
    /**
     * computes the path as an IntList from a string. The string is expected
     * to be of the format given by IntList#toString().
     * @param path
     * @return 
     */
    public static IntList getPathFromString(String path) {
        IntList ret = new IntArrayList();
        String[] stringArray = path.substring(1, path.length()-1).split(", ");//strip the parantheses, and split along commata
        if (stringArray[0].equals("")) {
            //then it is the empty path
            return ret;
        } else {
            //convert strings to numbers
            ret.addAll(Arrays.stream(stringArray).map(string -> Integer.valueOf(string)).collect(Collectors.toList()));
            return ret;
        }
    }
    
}
