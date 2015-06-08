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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

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
        
        this.nodes = new TreeSet<>();
        
        for(String node : et1.nodes)
        {
            if(outer.contains(node) || !et2.nodes.contains(node))
            {
                nodes.add(node);
            }
        }
        
        for(String node : et2.nodes)
        {
            if(outer.contains(node) || !et1.nodes.contains(node))
            {
                nodes.add(node);
            }
        }
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
            List<String> ordering, double weight,
            BolinasRule br) 
    {
      Set<GraphNode> seenNodes = new HashSet<>();
      AtomicInteger variableNumbers = new AtomicInteger();
      List<String> l = new ArrayList<>();

      Tree<String> image = transform(variableNumbers,seenNodes,l, br);
      
      String label = stso.get();
      Rule r = ta.createRule(nonterminalName, label,
              l.toArray(new String[l.size()]), weight);
      
      ta.addRule(r);
        
      hom.add(label, rename(nodes, ordering, image));
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
    private static Tree<String> rename(SortedSet<String> f, SortedSet<String> t, Tree<String> main) {
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
    private static Tree<String> rename(SortedSet<String> combined, List<String> outer, Tree<String> main) {
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
    private static Tree<String> rename(List<String> combined, SortedSet<String> outer, Tree<String> main) {
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
    private static Tree<String> rename(BiMap<String,Integer> from, BiMap<String,Integer> to,
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

    /**
     * 
     * @param variableNumbers
     * @param seenNodes
     * @param br
     * @return 
     */
    private Tree<String> transform(AtomicInteger variableNumbers,
            Set<GraphNode> seenNodes, List<String> rhs, BolinasRule br) {
        if(this.de != null)
        {
            return this.handleEdge(seenNodes, br);
        }
        else if(this.nont != null)
        {
            return this.handleNonterminal(seenNodes,rhs, variableNumbers, br);
        }
        else if(this.first != null && this.second != null)
        {
            return this.handleCombination(variableNumbers,seenNodes,rhs, br);
        }
        
        throw new IllegalStateException("This Edgetree somehow ended up without structure");
    }

    /**
     * 
     */
    private Tree<String> handleEdge(Set<GraphNode> seenNodes, BolinasRule br) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        
        sb.append(this.addNode(seenNodes, null, this.de.getSource()));
        
        sb.append(" :");
        sb.append(this.de.getLabel());
        
        sb.append(" (");
        sb.append(this.addNode(seenNodes, null, this.de.getTarget()));
        sb.append(")");
        
        sb.append(" )");
        
        Tree<String> t = Tree.create(sb.toString());
        return t;
    }

    /**
     * 
     * @param seenNodes
     * @param rhs
     * @param variableNumbers
     * @return 
     */
    private Tree<String> handleNonterminal(Set<GraphNode> seenNodes, List<String> rhs,
            AtomicInteger variableNumbers, BolinasRule br) {
        
        String nt = BolinasHrgInputCodec.makeLHS(this.nont);
        int num = variableNumbers.incrementAndGet();
        
        rhs.add(nt);
        
        Tree<String> main = Tree.create("?"+num);
        
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
            
            if(g == null || g.getLabel() == null || seenNodes.contains(g))
            {
                ++i;
                continue;
            }
            
            String k = "("+s;
            k += " <"+i+">";
            k += " / "+g.getLabel();
            seenNodes.add(g);
            k += ")";
            
            Tree<String> t = Tree.create(k);
            main = Tree.create("merge", t, main);
            ++i;
        }
        
        return rename(this.nont.getEndpoints(),this.nodes, main);
    }

    /**
     * 
     * @param variableNumbers
     * @param seenNodes
     * @param rhs
     * @return 
     */
    private Tree<String> handleCombination(AtomicInteger variableNumbers,
            Set<GraphNode> seenNodes, List<String> rhs, BolinasRule br) {
        Tree<String> tl = this.first.transform(variableNumbers, seenNodes, rhs, br);
        Tree<String> tr = this.second.transform(variableNumbers, seenNodes, rhs, br);
        
        SortedSet<String> middle = new TreeSet<>(this.first.nodes);
        middle.addAll(this.second.nodes);
        
        tl = rename(this.first.nodes, middle, tl);
        tr = rename(this.second.nodes, middle, tr);
        
        Tree<String> ret = Tree.create("merge", tl,tr);
        
        return rename(middle,this.nodes,ret);
    }
}