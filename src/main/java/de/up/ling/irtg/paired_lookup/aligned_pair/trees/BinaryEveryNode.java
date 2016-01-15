/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.paired_lookup.aligned_pair.trees;

import de.up.ling.irtg.paired_lookup.aligned_pair.AlignedStructure;
import de.up.ling.irtg.paired_lookup.aligned_pair.AlignedTree;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 *
 * @author christoph
 */
public class BinaryEveryNode implements AlignedStructure<Tree<String>> {
    /**
     * 
     */
    private final Tree<String> base;
    
    /**
     * 
     */
    private final Interner<Tree<String>> numbers;
    
    /**
     * 
     */
    private final Int2ObjectMap<IntCollection> align;
    
    /**
     * 
     */
    private final Int2ObjectMap<IntCollection> dominate;
    
    /**
     * 
     */
    private final int[] fin;

    public BinaryEveryNode(Tree<String> base, Map<Tree<String>,IntCollection> baseAlignments) {
        this.base = base;
        
        this.numbers = new Interner<>();
        
        this.align = new Int2ObjectOpenHashMap<>();
        this.dominate = new Int2ObjectOpenHashMap<>();
        
        int top = addNodeInfo(base, baseAlignments);
        
        this.fin = new int[] {top};
    }

    @Override
    public Stream<AlignedTree> getAlignedTrees(int state1) {
        Stream.Builder<AlignedTree> build = Stream.builder();
        
        Int2IntMap vars = new Int2IntOpenHashMap();
        vars.defaultReturnValue(-1);
        
        build.accept(makeTree(state1,vars));
        
        IntIterator iterator = this.dominate.get(state1).iterator();
        while(iterator.hasNext()) {
            int child = iterator.nextInt();
            
            if(child == state1) {
                continue;
            }
            
            vars.clear();
            vars.put(child, 1);
            
            build.add(makeTree(state1, vars));
            
            IntIterator inner = this.dominate.get(state1).iterator();
            while(inner.hasNext()) {
                int otherChild = inner.nextInt();
                
                if(otherChild > child && !this.dominate.get(child).contains(otherChild)) {
                    vars.clear();
                    vars.put(child, 1);
                    vars.put(otherChild, 2);
                    
                    build.add(makeTree(state1,vars));
                }
            }
        }
        
        return build.build();
    }

    @Override
    public Stream<AlignedTree> getAlignedTrees(int parent, AlignedTree at) {
        Stream.Builder<AlignedTree> build = Stream.builder();
        
        IntCollection ic = at.getRootAlignments();
        IntCollection here = this.align.get(parent);
        
        if(!equals(ic,here)) {
            return build.build();
        }
        
        Int2IntMap vars = new Int2IntOpenHashMap();
        if(at.getNumberVariables() == 0) {
            build.add(this.makeTree(parent, vars));
            return build.build();
        }
        
        IntArrayList il = new IntArrayList();
        il.add(parent);
        IntCollection children = this.dominate.get(parent);
        
        this.addToBuild(at, parent, children, vars, il, build, 0);
        
        return build.build();
    }

    @Override
    public Tree<String> getState(int state) {
        return this.numbers.resolveId(state);
    }

    @Override
    public IntStream getFinalStates() {
        return Arrays.stream(fin);
    }

    @Override
    public IntCollection getAlignments(int state) {
        return this.align.get(state);
    }

    /**
     * 
     * @param base
     * @param baseAlignments
     * @return 
     */
    private int addNodeInfo(Tree<String> base, Map<Tree<String>,IntCollection> baseAlignments) {
        int num = this.numbers.addObject(base);
        
        IntSet propoagatedAlignments = new IntOpenHashSet();
        IntSet dom = new IntOpenHashSet();
        
        propoagatedAlignments.addAll(baseAlignments.get(base));
        dom.add(num);
        
        for(int i=0;i<base.getChildren().size();++i) {
            Tree<String> child = base.getChildren().get(i);
            int code = addNodeInfo(child, baseAlignments);
            
            propoagatedAlignments.addAll(this.align.get(code));
            dom.addAll(this.dominate.get(code));
        }
        
        this.align.put(num, propoagatedAlignments);
        this.dominate.put(num, dom);
        
        return num;
    }

    private AlignedTree makeTree(int state1, Int2IntMap vars) {
        Tree<String> t = this.numbers.resolveId(state1);
        
        if(vars.isEmpty()) {
            //TODO write an implementation for aligned trees.
        }
        
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void addToBuild(AlignedTree at, int parent, IntCollection children, Int2IntMap vars,
                            IntArrayList il, Stream.Builder<AlignedTree> build, int i) {
        IntIterator iit = children.iterator();
        IntCollection there = at.getAlignmentsForVariable(i);
        
        outer : while(iit.hasNext()) {
            int next = iit.nextInt();
            il.size(i+1);
            
            IntCollection here = this.align.get(next);
            if(!equals(there,here)) {
                continue;
            }
            for(int k=1;k<il.size();++k) {
                int other = il.getInt(k);
                
                if(this.dominate.get(other).contains(next)) {
                    continue outer;
                }
            }
            
            if(i+1 == at.getNumberVariables()) {
                vars.clear();
                
                for(int k=1;k<il.size();++k) {
                    vars.put(il.getInt(k), k);
                }
                
                build.add(this.makeTree(parent, vars));
            }else {
                this.addToBuild(at, parent, children, vars, il, build, i+1);
            }   
        }
    }

    /**
     * 
     * @param ic
     * @param here
     * @return 
     */
    private boolean equals(IntCollection ic, IntCollection here) {
        if(!ic.containsAll(here)) {
            return false;
        }
        
        return here.containsAll(ic);
    }
}
