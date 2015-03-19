/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec.bolinas_hrg;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author christoph_teichmann
 */
class EdgeTree {
   
    /**
     * 
     */
    private final NonterminalWithHyperedge nont;
    
    /**
     * 
     */
    final EdgeTree first;
    
    /**
     * 
     */
    final EdgeTree second;
    
    /**
     * 
     */
    private final GraphEdge de;
    
    /**
     * 
     */
    private final   SortedSet<String> nodes;   
    
    /**
     * 
     * @param nont 
     */
    EdgeTree(NonterminalWithHyperedge nont, Set<String> outer)
    {
        this.nont = nont;
        this.de = null;
        
        this.nodes = new TreeSet<>(this.nont.getEndpoints());
        nodes.retainAll(outer);
        
        first = null;
        second = null;
    }
    
    /**
     * 
     * @param ge 
     */
    EdgeTree(GraphEdge ge, Set<String> outer)
    {
        this.nont = null;
        this.de = ge;
        
        this.nodes = new TreeSet<>();
        this.nodes.add(this.de.getSource().getName());
        this.nodes.retainAll(outer);
        
        first = null;
        second = null;
    }
    
    /**
     * 
     * @param et1
     * @param et2 
     */
    EdgeTree(EdgeTree et1, EdgeTree et2, Set<String> outer)
    {
        this.nont = null;
        this.de = null;
        
        first = et1;
        second = et2;
        
        this.nodes = new TreeSet<>(et1.nodes);
        this.nodes.addAll(et2.nodes);
        this.nodes.retainAll(outer);
    }
    
    /**
     * 
     * @param other
     * @param held
     * @return 
     */
    int joinSize(EdgeTree other, Set<String> held)
    {
        Set<String> set = new TreeSet<>();
        set.addAll(this.nodes);
        set.retainAll(other.nodes);
        
        set.removeAll(held);
        return set.size();
    }
    
    /**
     * 
     * @param counter 
     */
    void addCounts(Object2IntOpenHashMap counter)
    {
        for(String s : this.nodes)
        {
            counter.addTo(s, 1);
        }
    }

    /**
     * 
     * @param ta
     * @param hom
     * @param stso
     * @param nonterminalName
     * @param seenNodes
     * @param ordering 
     */
    void transform(ConcreteTreeAutomaton<String> ta, Homomorphism hom, StringSource stso, String nonterminalName,
            HashSet<String> seenNodes, List<String> ordering, double weight) {
        if(this.de != null)
        {
            handleEdge(stso, ta, nonterminalName, seenNodes, ordering, hom, weight);
        }
        else if(this.nont != null)
        {
            makeNonterminal(stso, ta, nonterminalName, weight, hom);
        }
        else
        {
            
            handleCombination(stso, ta, hom, seenNodes, nonterminalName, ordering, weight);
        }
    }

    void makeNonterminal(StringSource stso, ConcreteTreeAutomaton<String> ta, String nonterminalName, double weight, Homomorphism hom) {
        String label = stso.get();
        String right = BolinasHrgInputCodec.makeLHS(nont);
        
        ta.addRule(ta.createRule(nonterminalName, label, new String[] {right},weight));
        
        Tree<String> main = Tree.create("?1");
        main = rename(this.nont.getEndpoints(),this.nodes, main);
        
        hom.add(label, main);
    }

    /**
     * 
     * @param stso
     * @param ta
     * @param hom
     * @param seenNodes
     * @param nonterminalName 
     */
    private void handleCombination(StringSource stso, ConcreteTreeAutomaton<String> ta,
            Homomorphism hom, HashSet<String> seenNodes, String nonterminalName,
            List<String> outer, double weight) {
        String left = stso.get();
        this.first.transform(ta, hom, stso, left, seenNodes, null,1.0);
        
        
        String right = stso.get();
        this.second.transform(ta, hom, stso, left, seenNodes, null,1.0);
        
        String label = stso.get();
        ta.addRule(ta.createRule(nonterminalName, label, new String[] {left,right}, weight));
        
        SortedSet<String> combined = new TreeSet<>();
        combined.addAll(this.first.nodes);
        combined.addAll(this.second.nodes);
        
        
        Tree<String> lr = rename(first.nodes,combined,Tree.create("?1"));
        Tree<String> rr = rename(second.nodes,combined,Tree.create("?2"));
        
        Tree<String> main = Tree.create("merge", lr, rr);
        
        if(outer == null)
        {
            main = rename(combined,this.nodes,main);
        }
        else
        {
            main = rename(combined,outer,main);
        }
        
        hom.add(label, main);
    }

    /**
     * 
     * @param stso
     * @param ta
     * @param nonterminalName
     * @param seenNodes
     * @param ordering
     * @param hom 
     */
    private void handleEdge(StringSource stso, ConcreteTreeAutomaton<String> ta, String nonterminalName,
            HashSet<String> seenNodes, List<String> ordering, Homomorphism hom, double weight) {
        String label = stso.get();
        ta.addRule(ta.createRule(nonterminalName, label, new String[] {},weight));
        
        String s = "'(";
        
        s = addNode(s, seenNodes, ordering);
        s += " :"+this.de.getLabel()+" ";
        s = "("+addNode(s, seenNodes, ordering)+")";
        
        s += ")'";
        
        Tree<String> h = Tree.create(s);
        hom.add(label, h);
    }

    /**
     * 
     * @param s
     * @param seenNodes
     * @param ordering
     * @return 
     */
    private String addNode(String s, HashSet<String> seenNodes, List<String> ordering) {
        s += convert(this.de.getSource(),seenNodes);
        if(this.nodes.contains(this.de.getSource().getName()))
        {
            if(ordering != null && ordering.contains(this.de.getSource().getName()))
            {
                int num = ordering.indexOf(this.de.getSource().getName());
                s += "<"+num+">";
            }
            else
            {
                int num = this.nodes.headSet(this.de.getSource().getName()).size();
                s += "<"+num+">";
            }
        }
        return s;
    }

    /**
     * 
     * @param source
     * @param seenNodes
     * @return 
     */
    private String convert(GraphNode source, HashSet<String> seenNodes) {
        return source.getName();
    }

    /**
     * 
     * @param from
     * @param to
     * @param string
     * @return 
     */
    private Tree<String> rename(SortedSet<String> f, SortedSet<String> t, Tree<String> main) {
        BiMap<String,Integer> from = HashBiMap.create();
        int i = 0;
        for(String s : f)
        {
            from.put(s,i++);
        }
        
        BiMap<String,Integer> to = HashBiMap.create();
        i = 0;
        for(String s : t)
        {
            to.put(s, i++);
        }
        
        return rename(from,to,main);
    }

    private Tree<String> rename(SortedSet<String> combined, List<String> outer, Tree<String> main) {
        BiMap<String,Integer> from = HashBiMap.create();
        int i = 0;
        for(String s : combined)
        {
            from.put(s,i++);
        }
        
        BiMap<String,Integer> to = HashBiMap.create();
        i = 0;
        for(String s : outer)
        {
            to.put(s, i++);
        }
        
        return rename(from,to,main);
    }
    
    private Tree<String> rename(List<String> combined, SortedSet<String> outer, Tree<String> main) {
        BiMap<String,Integer> from = HashBiMap.create();
        int i = 0;
        for(String s : combined)
        {
            from.put(s,i++);
        }
        
        BiMap<String,Integer> to = HashBiMap.create();
        i = 0;
        for(String s : outer)
        {
            to.put(s, i++);
        }
        
        return rename(from,to,main);
    }
    
    /**
     * 
     * @param from
     * @param to
     * @param main
     * @return 
     */
    private Tree<String> rename(BiMap<String,Integer> from, BiMap<String,Integer> to,
                        Tree<String> main)
    {
        Collection<String> c = new ArrayList<String>(from.keySet());
        
        for(String s : c)
        {
            if(!to.containsKey(s))
            {
                main = Tree.create("f_"+from.get(s), main);
                continue;
            }
            Integer current = from.get(s);
            Integer goal    = to.get(s);
            
            if(current.equals(goal))
            {
                continue;
            }
            
            if(from.inverse().containsKey(goal))
            {
                String block = from.inverse().get(goal);
                from.remove(s);
                from.remove(block);
                from.put(block, current);
                from.put(s, goal);
                
                main = Tree.create("s_"+from+"_"+goal, main);
            }
            else
            {
                from.remove(s);
                from.put(s, goal);
                main = Tree.create("r_"+current+"_"+goal, main);
            }
        }
        
        return main;
    }
}
