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
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
        this.nodes.add(this.de.getTarget().getName());
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
    void transform(ConcreteTreeAutomaton<String> ta, Homomorphism hom,
            StringSource stso, String nonterminalName,
            Set<GraphNode> seenNodes, List<String> ordering, double weight,
            BolinasRule br) 
    {
        if(this.de != null)
        {
            handleEdge(stso, ta, nonterminalName, seenNodes, ordering, hom, weight, br);
        }
        else if(this.nont != null)
        {
            makeNonterminal(stso, ta, nonterminalName, weight, hom, seenNodes, br);
        }
        else
        {
            handleCombination(stso, ta, hom, seenNodes, nonterminalName, ordering, weight, br);
        }
    }

    /**
     * 
     * @param stso
     * @param ta
     * @param nonterminalName
     * @param weight
     * @param hom 
     */
    private void makeNonterminal(StringSource stso, ConcreteTreeAutomaton<String> ta,
            String nonterminalName, double weight, Homomorphism hom,
            Set<GraphNode> seenNodes, BolinasRule br) 
    {
        String label = stso.get();
        String right = BolinasHrgInputCodec.makeLHS(nont);
        
        Rule r = ta.createRule(nonterminalName, label, new String[] {right},weight);
        ta.addRule(r);
        
        Tree<String> main = Tree.create("?1");
        
        int i = 0;
        for(String s : nont.getEndpoints())
        {
            GraphNode g = null;
            for(GraphNode gn : br.getRhsGraph().vertexSet())
            {
                if(gn.getName().equals(s))
                {
                    g = gn;
                    break;
                }
            }
            
            if(g == null || g.getLabel() == null)
            {
                ++i;
                continue;
            }
            
            String k = "("+s;
            k += " <"+i+">";
            if(!seenNodes.contains(g))
            {
               k += " / "+g.getLabel();
               seenNodes.add(g);
            }
            k += ")";
            Tree<String> t = Tree.create(k);
            main = Tree.create("merge", t, main);
            ++i;
        }
        
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
    private void handleCombination(final StringSource stso, final ConcreteTreeAutomaton<String> ta,
            final Homomorphism hom, final Set<GraphNode> seenNodes, final String nonterminalName,
            final List<String> outer, final double weight, BolinasRule br) {
        
        final String left = stso.get();
        final String right = stso.get();
        
        String label = stso.get();
        
        Rule r = ta.createRule(nonterminalName, label, new String[] {left,right}, weight);
        ta.addRule(r);
        
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
        
        this.first.transform(ta, hom, stso, left, seenNodes, null, 1.0, br);
        this.second.transform(ta, hom, stso, right, seenNodes, null, 1.0, br);
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
            Set<GraphNode> seenNodes, List<String> ordering, Homomorphism hom,
            double weight, BolinasRule br) {
        String label = stso.get();
        
        Rule r = ta.createRule(nonterminalName, label, new String[] {},weight);
        ta.addRule(r);
        
        String s = "(";
        
        s += addNode(seenNodes, ordering, this.de.getSource());
        s += " :"+this.de.getLabel()+" ";
        s += "("+addNode(seenNodes, ordering,this.de.getTarget())+")";
        
        s += ")";
        
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
    private String addNode(Set<GraphNode> seenNodes, List<String> ordering,
            GraphNode n) {
        String s = convert(n);
        if(this.nodes.contains(n.getName()))
        {
            if(ordering != null && ordering.contains(n.getName()))
            {
                int num = ordering.indexOf(n.getName());
                s += "<"+num+">";
            }
            else
            {
                int num = this.nodes.headSet(n.getName()).size();
                s += "<"+num+">";
            }
        }
        
        if(!seenNodes.contains(n) && n.getLabel() != null)
        {
            seenNodes.add(n);
            s += " / "+n.getLabel();
        }
        
        return s;
    }

    /**
     * 
     * @param source
     * @param seenNodes
     * @return 
     */
    private String convert(GraphNode node) {
        return node.getName();
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

    /**
     * 
     * @param combined
     * @param outer
     * @param main
     * @return 
     */
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
    
    /**
     * 
     * @param combined
     * @param outer
     * @param main
     * @return 
     */
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
        Collection<String> c = new ArrayList<>(from.keySet());
        
        for(String s : c)
        {
            if(!to.containsKey(s))
            {
                Integer source = from.get(s);
                main = Tree.create("f_"+source, main);
                from.remove(s);
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
                
                main = Tree.create("s_"+current+"_"+goal, main);
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
    
    /**
     * 
     * @return 
     */
    @Override
    public String toString()
    {
       String s = this.nodes.toString();
       s += " ";
       if(this.de != null)
       {
           return s + this.de.getSource()+"-"+this.de.getLabel()+"->"+this.de.getTarget();
       }
       else if(this.nont != null)
       {
           return s + this.nont.toString();
       }
       else
       {
           s += "( "+this.first.toString()+" , "+this.second.toString()+" )";
           return s;
       }
    }

    /**
     * 
     * @param et2
     * @return 
     */
    boolean disjoint(EdgeTree et2) {
        return Collections.disjoint(nodes, et2.nodes);
    }
}