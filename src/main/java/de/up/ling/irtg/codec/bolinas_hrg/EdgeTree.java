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
 * This class represents a collection of both normal and non-terminal edges in
 * the right hand side of a HRG rule, organized into a tree in order to convert
 * them more easily into an expression in the s-graph algebra.
 * 
 * It is possible to create trees, ask how many sources could be forgotten if
 * two trees that represent portions of a RHS where merged and to construct the
 * homomorphic image that corresponds to a given tree. For the latter task
 * the class also makes a list of the necessary non-terminals accessible.
 * 
 * @author christoph_teichmann
 */
class EdgeTree {
   
    /**
     * If this tree represents a single non-terminal edge, then it is stored
     * here.
     */
    private final NonterminalWithHyperedge nont;
    
    /**
     * If this tree represents the combination of two edge trees, then this will
     * hold the first of these trees.
     */
    private final EdgeTree first;
    
    /**
     * If this tree represents the combination of two edge trees, then this will
     * hold the second of these trees.
     */
    private final EdgeTree second;
    
    /**
     * If the tree represents a single edge, then it is contained here.
     */
    private final GraphEdge de;
    
    /**
     * This contains all the nodes that are still active.
     * 
     * The active nodes are those that still occur at positions in the RHS that
     * have not been subsumed by this tree.
     */
    private final   SortedSet<String> nodes;   
    
    /**
     * Constructs a new instance representing a single non-terminal edge.
     * 
     * @param nont the edge
     * @param outer all the nodes that might need tracking if they are incident
     * to the edge.
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
     * Constructs a new instance representing a single edge.
     * 
     * @param ge the edge
     * @param outer all the nodes that might need tracking if they are incident
     * to the edge.
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
     * Creates an instance that represents the merge of two subgraphs.
     * 
     * @param et1 the first subgraphs representation.
     * @param et2 the second subgraphs representation.
     * @param outer all the nodes that might need tracking if they are incident
     * to the edge.
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
     * Returns the number of nodes that could be eliminated if the two graphs
     * where merged.
     * 
     * @param other
     * @param held nodes that cannot be eliminated, because they are needed
     * elsewhere
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
     * This will add +1 to the given counter for every node that is still active
     * after the construction of this subgraph.
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
     * Adds a rule corresponding to this subgraph in the given rule to the
     * given automaton and homomorphism.
     * 
     * @param ta automaton to which rules are added.
     * @param hom the homomorphism to which rules are added.
     * @param stso a source for unique strings used to generate labels
     * @param nonterminalName the name of the LHS symbol
     * @param ordering the nodes that are external nodes of the rule, in the
     * order in which they are external.
     * @param weight the weight of the rule
     * @param br the rule to be converted.
     */
    void transform(ConcreteTreeAutomaton<String> ta, Homomorphism hom,
            StringSource stso, String nonterminalName,
            List<String> ordering, double weight,
            BolinasRule br) 
    {
      // create a container for nodes we have already seen (so we do not add
      //  names twice)
      Set<GraphNode> seenNodes = new HashSet<>();
      // used to keep track of the number of non-terminals we have introduced
      // important to compute the variables for the homomorphism
      AtomicInteger variableNumbers = new AtomicInteger();
      List<String> l = new ArrayList<>();

      // first we generate the homomorphic image of the RHS
      Tree<String> image = transform(variableNumbers,seenNodes,l, br);
      
      // then we create a rule that has the appropriate child non-terminals
      String label = stso.get();
      Rule r = ta.createRule(nonterminalName, label,
              l.toArray(new String[l.size()]), weight);
      ta.addRule(r);
        
      // before we can use the image, we have to add operations that rename the
      // sources to fit with our intended external nodes
      hom.add(label, rename(nodes, ordering, image));
    }

    /**
     * Returns a string representation for a single graph node.
     * 
     * @param seenNodes contains a node if its name is generated elsewhere
     * @param ordering legacy variable that is not used in the current version
     * @param n the node we want to represent
     * @return 
     */
    private String addNode(Set<GraphNode> seenNodes, List<String> ordering, 
            GraphNode n) {
        String s = n.getName();
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
     * This method simply converts the arguments into a digestible form for the
     * main rename method.
     * @param from original source assignments
     * @param to goal source assignments
     * @param main tree to extend
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
     * This method simply converts the arguments into a digestible form for the
     * main rename method.
     * @param combined original source assignments
     * @param outer goal source assignments
     * @param main tree to extend
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
     * This method simply converts the arguments into a digestible form for the
     * main rename method.
     * @param combined original source assignments
     * @param outer goal source assignments
     * @param main tree to extend
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
     * This method takes the input tree and assumes that the source assignments
     * are as in from; then builds a more complicated tree that adds operations
     * to get the source assignments as in to.
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
     * Converts this tree into a more readable form for debugging.
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
     * Returns true if the two subgraphs share no source nodes and should
     * therefor not be merged.
     * 
     * @param et2
     * @return 
     */
    boolean disjoint(EdgeTree et2) {
        return Collections.disjoint(nodes, et2.nodes);
    }

    /**
     * Used to recursively transform smaller portions of the subgraph.
     * 
     * @param variableNumbers keeps track of the variables we have already
     * planned for.
     * @param seenNodes keeps track of the nodes for which we have already
     * generated the labels.
     * @param br the rule we are converting, necessary to look up e.g. node
     * labels.
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
     * Returns a tree that represents a single edge.
     * 
     * @param seenNodes
     * @param br
     * @return 
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
     * Returns a tree that represents a single nonterminal edge.
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
        
        // everything that follows is for the case where we have labels for
        // nodes that are only realized by nonterminals edges, which means
        // we have to merge them in step by step, since non-terminal edges do
        // not correspond to anything other than a variable in the homomorphism
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
        
        // here we convert the node labeling from that of the non-terminal to
        // the order used for the rest of the graph.
        return rename(this.nont.getEndpoints(),this.nodes, main);
    }

    /**
     * This handles all cases where two subgraphs are merged in order to
     * generate a larger portion.
     * 
     * Part of this process includes calling the children in order to have them
     * compute their representation first.
     * 
     * @param variableNumbers
     * @param seenNodes
     * @param rhs
     * @return 
     */
    private Tree<String> handleCombination(AtomicInteger variableNumbers,
            Set<GraphNode> seenNodes, List<String> rhs, BolinasRule br) {
        
        // get the children
        Tree<String> tl = this.first.transform(variableNumbers, seenNodes, rhs, br);
        Tree<String> tr = this.second.transform(variableNumbers, seenNodes, rhs, br);
        
        // get them to agree on a source labeling
        SortedSet<String> middle = new TreeSet<>(this.first.nodes);
        middle.addAll(this.second.nodes);
        
        tl = rename(this.first.nodes, middle, tl);
        tr = rename(this.second.nodes, middle, tr);
        
        // create the tree for the merge
        Tree<String> ret = Tree.create("merge", tl,tr);
        
        // have it forget sources that are no longer needed.
        return rename(middle,this.nodes,ret);
    }
}