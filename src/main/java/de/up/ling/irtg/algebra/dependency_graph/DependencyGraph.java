/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.dependency_graph;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author christoph_teichmann
 */
public class DependencyGraph {
    /**
     * 
     */
    private final List<String> nodes = new ArrayList<>();
    
    /**
     * 
     */
    private final Object2ObjectSortedMap<String,IntArrayList> open = new Object2ObjectAVLTreeMap<>(); 
    
    /**
     * 
     */
    private final IntList openPosition = new IntArrayList();
    
    /**
     * 
     */
    private final List<String> openLabel = new ArrayList();
    
    /**
     * 
     */
    private final List<Int2ObjectMap<String>> edges = new ArrayList<>();
    
    /**
     * 
     */
    private final List<String> rootEdge = new ArrayList<>();
    
    /**
     * 
     * @param label 
     */
    public void addNode(String label) {
        this.nodes.add(label);
        
        this.edges.add(new Int2ObjectOpenHashMap<>());
        
        this.rootEdge.add(null);
        this.openPosition.add(-1);
        this.openLabel.add(null);
    }
    
    /**
     * 
     * @param label
     * @param open 
     */
    public void addNode(String label, String open) {
        int pos = this.nodes.size();
        this.nodes.add(label);
        
        this.edges.add(new Int2ObjectOpenHashMap<>());
        
        IntArrayList il = this.open.get(open);
        if(il == null) {
            this.open.put(open, il = new IntArrayList());
        }
        int oPos = il.size();
        il.add(pos);
        
        this.rootEdge.add(null);
        
        this.openPosition.add(oPos);
        this.openLabel.add(open);
    }
    
    
    /**
     * 
     * @param from
     * @param to
     * @param label 
     */
    public void addEdge(int from, int to, String label) {
        if(from < 0 || from >= this.nodes.size() || to < 0 || to >= this.nodes.size()) {
            throw new IllegalArgumentException("Nodes to connect do not exist");
        }
        
        this.edges.get(from).put(to, label);
    }
    
    /**
     * 
     * @param name
     * @param position
     * @return 
     */
    public DependencyGraph forget(String name, int position) {
       DependencyGraph dg = deepCopy();
       
       dg.forgetInternal(name, position);
       return dg;
    }
    
    /**
     * 
     * @param name
     * @return 
     */
    public DependencyGraph forget(String name) {
        return this.forget(name, 0);
    }
    
    
    /**
     * 
     * @param name
     * @param position 
     */
    protected void forgetInternal(String name, int position) {
        IntList il = this.open.get(name);
        
        if(il==null || il.size() <= position) {
            throw new IllegalArgumentException("No such open node: "+name+" "+position);
        } 
        
        int old = il.removeInt(position);
        if(il.isEmpty()) {
            this.open.remove(name);
        }
        
        this.openPosition.set(old, -1);
        this.openLabel.set(old, null);
    }
    
    /**
     * 
     * @param name
     * @param position
     * @return 
     */
    public int getOpenNode(String name, int position) {
        IntList il = this.open.get(name);
        
        if(il==null || il.size() <= position) {
            throw new IllegalArgumentException("No such open node: "+name+" "+position);
        }
        
        return il.getInt(position);
    }
    
    /**
     * 
     * @param nameFrom
     * @param posFrom
     * @param nameTo
     * @param posTo
     * @param name
     * @return 
     */
    public DependencyGraph addEdge(String nameFrom, int posFrom, String nameTo, int posTo,
                                        String name) {
        DependencyGraph dg = this.deepCopy();
        int n1 = this.getOpenNode(nameFrom, posFrom);
        int n2 = this.getOpenNode(nameTo, posTo);
        
        dg.addEdge(n1, n2, name);
        
        return dg;
    }

    /**
     * 
     * @return 
     */
    private DependencyGraph deepCopy() {
        DependencyGraph ret = new DependencyGraph();
        ret.nodes.addAll(this.nodes);
        
        this.open.entrySet().stream().forEach((ent) -> {
            ret.open.put(ent.getKey(), new IntArrayList(ent.getValue()));
        });
        for(int i=0;i<this.edges.size();++i) {
           ret.edges.add(new Int2ObjectOpenHashMap<>(this.edges.get(i)));
        }
        
        ret.rootEdge.addAll(this.rootEdge);
        ret.openLabel.addAll(this.openLabel);
        ret.openPosition.addAll(this.openPosition);
        
        return ret;
    }
    /**
     * 
     * @param other
     * @return 
     */
    public DependencyGraph concatenate(DependencyGraph other) {
        DependencyGraph dg = this.deepCopy();
        
        int offset = dg.length();
        
        dg.nodes.addAll(other.nodes);
        
        other.open.entrySet().stream().forEach((ent) -> {
            String label = ent.getKey();
            IntArrayList nPos = shift(ent.getValue(),offset);
            
            for(int i=0;i<nPos.size();++i) {
                dg.addOpen(label,nPos.getInt(i));
            }
        });
        
        for(int i=0;i<other.edges.size();++i) {
            Int2ObjectMap<String> map;
            dg.edges.add(map = new Int2ObjectOpenHashMap<>());
            
            for(Int2ObjectMap.Entry<String> ent : edges.get(i).int2ObjectEntrySet()) {
                map.put(ent.getIntKey()+offset, ent.getValue());
            }
        }
        
        return dg;
    }
    
    /**
     * 
     * @return 
     */
    public int length() {
        return this.nodes.size();
    }

    /**
     * 
     * @param value
     * @param offset
     * @return 
     */
    private IntArrayList shift(IntList value, int offset) {
        IntArrayList ret = new IntArrayList();
        
        for(int i=0;i<value.size();++i) {
            ret.add(value.getInt(i)+offset);
        }
        
        return ret;
    }
    
    /**
     * 
     * @param position
     * @return 
     */
    public String getNode(int position) {
        return this.nodes.get(position);
    }
    
    /**
     * 
     * @param fromNode
     * @return 
     */
    public Iterable<Int2ObjectMap.Entry<String>> getEdges(int fromNode) {
        return this.edges.get(fromNode).int2ObjectEntrySet();
    }
    
    /**
     * 
     * @param pos
     * @param label
     */
    public void addRootEdge(int pos, String label) {
        this.rootEdge.set(pos, label);
    }
    
    /**
     * 
     * @param label 
     * @return  
     */
    public DependencyGraph addRootEdge(String label) {
        if(this.nodes.size() != 1) {
            throw new IllegalStateException("Only applicable when there is exactly 1 node.");
        }
        
        DependencyGraph dg = this.deepCopy();
        
        dg.rootEdge.set(0, label);
        return dg;
    }
    
    /**
     * 
     * @param node
     * @return 
     */
    public String getRootEdge(int node) {
        return this.rootEdge.get(node);
    }
    
    /**
     * 
     * @return 
     */
    public Iterable<String> getOpenNames() {
        return this.open.keySet();
    }
    
    /**
     * 
     * @param name
     * @return 
     */
    public int getNumberOpen(String name) {
        IntList il = this.open.get(name);
        
        return il == null ? 0 : il.size();
    }
    
    /**
     * 
     * @param name
     * @param newName
     * @return 
     */
    public DependencyGraph renameOpen(String name, String newName) {
        return this.renameOpen(name, 0, newName);
    }
    
    /**
     * 
     * @param name
     * @param pos
     * @param newName
     * @return 
     */
    public DependencyGraph renameOpen(String name, int pos, String newName) {
        int old = this.getOpenNode(name, pos);
        
        DependencyGraph dg = this.forget(name, pos);
        IntArrayList il = (IntArrayList) dg.open.get(newName);
        
        if(il == null) {
            il = new IntArrayList();
            dg.open.put(newName, il);
        }
        
        int insert = (-Arrays.binarySearch(il.elements(), 0, il.size(), old))+1;
        
        if(insert < il.size()) {
            il.add(insert, old);
        } else {
            il.add(old);
        }
        
        dg.openLabel.set(old, name);
        dg.openPosition.set(old, insert);
        
        return dg;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.nodes);
        hash = 37 * hash + Objects.hashCode(this.open);
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
        final DependencyGraph other = (DependencyGraph) obj;
        if (!Objects.equals(this.nodes, other.nodes)) {
            return false;
        }
        if (!Objects.equals(this.open, other.open)) {
            return false;
        }
        if (!Objects.equals(this.edges, other.edges)) {
            return false;
        }
        return Objects.equals(this.rootEdge, other.rootEdge);
    }
    
    /**
     * 
     * @param node
     * @return 
     */
    public boolean isOpen(int node) {
        return this.openPosition.get(node) >= 0;
    }

    /**
     * 
     * @param node
     * @return 
     */
    public int getOpenPosition(int node) {
        return this.openPosition.getInt(node);
    }
    
    /**
     * 
     * @param node
     * @return 
     */
    public String getOpenLabel(int node) {
        return this.openLabel.get(node);
    }
    
    /**
     * 
     * @param openLabel
     * @param node 
     */
    private void addOpen(String openLabel, int node) {
        IntArrayList ili  = this.open.get(openLabel);
        
        if(ili == null) {
            this.open.put(openLabel, ili = new IntArrayList());
        }
        
        int insertion = Arrays.binarySearch(ili.elements(), 0, ili.size(), node);
        insertion = (-insertion)+1;
        
        ili.add(insertion, node);
        
        this.openPosition.set(node, insertion);
        this.openLabel.set(node, openLabel);
    }
}
