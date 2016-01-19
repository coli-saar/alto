/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.paired_lookup.aligned_pair.trees;

import de.up.ling.irtg.paired_lookup.aligned_pair.AlignedStructure;
import de.up.ling.irtg.paired_lookup.aligned_pair.AlignedTree;
import de.up.ling.irtg.paired_lookup.aligned_pair.aligned_trees.BaseAlignedTree;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private final static String[] EMPTY = {};
    
    /**
     * 
     */
    public static String DIVIDER = "::";
    
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

    /**
     * 
     */
    private final boolean includeEmpty;

    /**
     * 
     * @param input
     * @param includeEmpty
     * @throws ParseException 
     */
    public BinaryEveryNode(String input, boolean includeEmpty) throws ParseException {
        Tree<String> withAlignments = TreeParser.parse(input);
        this.numbers = new Interner<>();
        
        this.align = new Int2ObjectOpenHashMap<>();
        this.dominate = new Int2ObjectOpenHashMap<>();
        
        this.base = addTreeInfo(withAlignments);
        
        
        this.fin = new int[] {this.numbers.addObject(base)};
        this.includeEmpty = includeEmpty;
    }
    
    
    @Override
    public Stream<AlignedTree> getAlignedTrees(int state1) {
        Stream.Builder<AlignedTree> build = Stream.builder();
        
        if(includeEmpty) {
            build.add(makeEmpty(state1));
        }
        
        Int2IntMap vars = new Int2IntOpenHashMap();
        vars.defaultReturnValue(-1);
        
        build.accept(makeTree(state1,vars));
        
        IntIterator iterator = this.dominate.get(state1).iterator();
        while(iterator.hasNext()) {
            int child = iterator.nextInt();
            
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
        
        if(at.getNumberVariables() == 1) {
            if(here.equals(at.getAlignmentsForVariable(0)) && this.includeEmpty) {
                build.add(this.makeEmpty(parent));
            }
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
     * @param state1
     * @param vars
     * @return 
     */
    private AlignedTree makeTree(int state1, Int2IntMap vars) {
        Tree<String> t = this.numbers.resolveId(state1);
        IntList states = new IntArrayList();
        List<IntCollection> alignments = new ArrayList<>();
        
        states.add(state1);
            
        alignments.add(this.align.get(state1));
        
        if(vars.isEmpty()) {
            return new BaseAlignedTree(t, alignments, states, 1.0);
        } else {
            
            Tree<String> q = makeTree(state1,vars,alignments,states);
            
            return new BaseAlignedTree(t, alignments, states, state1);
        }
    }

    /**
     * 
     * @param at
     * @param parent
     * @param children
     * @param vars
     * @param il
     * @param build
     * @param i 
     */
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

    /**
     * 
     * @return 
     */
    private AlignedTree makeEmpty(int state1) {
        IntList states = new IntArrayList();
        List<IntCollection> alignments = new ArrayList<>();
        
        states.add(state1);
            
        alignments.add(this.align.get(state1));
        
        Tree<String> t = Tree.create("?1");
        
        return new BaseAlignedTree(t, alignments, states, state1);
    }

    /**
     * 
     * @param state1
     * @param vars
     * @param alignments
     * @param states
     * @return 
     */
    private Tree<String> makeTree(int state1, Int2IntMap vars, List<IntCollection> alignments, IntList states) {
        int var = vars.get(state1);
        
        if(var >= 0) {
            if(states.size() <= var) {
                states.size(var+1);
            }
            
            while(alignments.size() <= var) {
                alignments.add(null);
            }
            
            states.set(var, state1);
            alignments.set(var, this.align.get(state1));
            
            return Tree.create("?"+var);
        }
        
        List<Tree<String>> children = new ArrayList<>();
        Tree<String> t = this.numbers.resolveId(state1);
        
        for(int i=0;i<t.getChildren().size();++i) {
            Tree<String> child = t.getChildren().get(i);
            int state = this.numbers.addObject(child);
            
            children.add(makeTree(state, vars, alignments, states));
        }
        
        return Tree.create(t.getLabel(), children);
    }

    /**
     * 
     * @param withAlignments
     * @return 
     */
    private Tree<String> addTreeInfo(Tree<String> withAlignments) {
        String[] q = withAlignments.getLabel().split(DIVIDER);
        String label = q[0].trim();
        String[] alignments;
        if(q.length > 1) {
            alignments = q[1].trim().split("\\s+");
        } else {
            alignments = EMPTY;
        }
        
        List<Tree<String>> children = new ArrayList<>();
        for(int i=0;i<withAlignments.getChildren().size();++i) {
            Tree<String> child = withAlignments.getChildren().get(i);
            
            children.add(addTreeInfo(child));
        }
        
        Tree<String> normal = Tree.create(label, children);
        int name = this.numbers.addObject(normal);
        
        IntSet localAlignments = new IntOpenHashSet();
        for(String s : alignments) {
            localAlignments.add(Integer.parseInt(s));
        }
        
        IntCollection dominated = new IntOpenHashSet();
        for(int i=0;i<normal.getChildren().size();++i) {
            Tree<String> child = normal.getChildren().get(i);
            int code = this.numbers.addObject(child);
            
            localAlignments.addAll(this.align.get(code));
            dominated.addAll(this.dominate.get(code));
            dominated.add(code);
        }
        
        this.align.put(name, localAlignments);
        this.dominate.put(name, dominated);
        
        return normal;
    }

    @Override
    public String toString() {
        return "BinaryEveryNode{" + "base=" + base + ", numbers=" + numbers + ", align=" + align + ", dominate=" + dominate + ", fin=" + fin + ", includeEmpty=" + includeEmpty + '}';
    }
}
