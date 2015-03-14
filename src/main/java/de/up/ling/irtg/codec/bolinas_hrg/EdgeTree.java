/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec.bolinas_hrg;

import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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
            HashSet<String> seenNodes, List<String> ordering) {
        
        if(this.de != null)
        {
            handleEdge(stso, ta, nonterminalName, seenNodes, ordering, hom);
            return;
        }
        else if(this.nont != null)
        {
            
            //TODO handle nonterminal edge
            return;
        }
        else
        {
            
            String left = stso.get();
            this.first.transform(ta, hom, stso, left, seenNodes, null);
            
            
            String right = stso.get();
            this.second.transform(ta, hom, stso, left, seenNodes, null);
            
            ta.addRule(ta.createRule(nonterminalName, stso.get(), new String[] {left,right}));
            
            //TODO
            
            
            
            //TODO
            return;
        }
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
    private void handleEdge(StringSource stso, ConcreteTreeAutomaton<String> ta, String nonterminalName, HashSet<String> seenNodes, List<String> ordering, Homomorphism hom) {
        String label = stso.get();
        ta.addRule(ta.createRule(nonterminalName, label, new String[] {}));
        
        String s = "'(";
        
        s = addNode(s, seenNodes, ordering);
        s += " :"+this.de.getLabel()+" ";
        s = addNode(s, seenNodes, ordering);
        
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

    private String convert(GraphNode source, HashSet<String> seenNodes) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
