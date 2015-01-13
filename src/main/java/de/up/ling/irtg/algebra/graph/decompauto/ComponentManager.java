/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph.decompauto;

import de.up.ling.irtg.algebra.graph.BoundaryRepresentation;
import de.up.ling.irtg.algebra.graph.ByteBasedEdgeSet;
import de.up.ling.irtg.algebra.graph.GraphInfo;
import de.up.ling.irtg.algebra.graph.IdBasedEdgeSet;
import de.up.ling.irtg.algebra.graph.ShortBasedEdgeSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 *
 * @author jonas
 */
public class ComponentManager {
    
    final Set<Component> components;
    
    //for completeGraph
    public ComponentManager(BoundaryRepresentation bRep, GraphInfo graphInfo) {
        components = new HashSet<>();
        
        if (bRep.getInBoundaryEdges().isEmpty()) {
            IdBasedEdgeSet inBoundaryEdges;
            
            if (graphInfo.useBytes) {
                inBoundaryEdges = new ByteBasedEdgeSet();
            } else {
                inBoundaryEdges = new ShortBasedEdgeSet();
            }
            components.add(new Component(new int[0], inBoundaryEdges));
        } else {
            IntSet seenEdges = new IntOpenHashSet();
            bRep.getInBoundaryEdges().forEach(edge -> {
                if (!seenEdges.contains(edge)) {
                    //init new component ingredients
                    IntList cvList = new IntArrayList();
                    IdBasedEdgeSet cInBoundaryEdges;
                    if (graphInfo.useBytes) {
                        cInBoundaryEdges = new ByteBasedEdgeSet();
                    } else {
                        cInBoundaryEdges = new ShortBasedEdgeSet();
                    }
                    
                    //add edge where necessary
                    seenEdges.add(edge);
                    cInBoundaryEdges.add(edge);
                    
                    //init agenda and take care of first vertices
                    IntList agenda = new IntArrayList();
                    IntSet seen = new IntOpenHashSet();
                    boolean useSource = bRep.isSource(graphInfo.edgeSources[edge]);
                    int firstVertex;
                    int seedVertex;
                    if (useSource) {
                        firstVertex = graphInfo.edgeSources[edge];
                        seedVertex = graphInfo.edgeTargets[edge];
                    } else {
                        firstVertex = graphInfo.edgeTargets[edge];
                        seedVertex = graphInfo.edgeSources[edge];
                    }
                    cvList.add(firstVertex);
                    seen.add(firstVertex);
                    agenda.add(seedVertex);
                    
                    //iteration over agenda
                    for (int curVertex : agenda) {
                        if (bRep.isSource(curVertex)) {
                            cvList.add(curVertex);
                        } else {
                            for (int curEdge : graphInfo.getIncidentEdges(curVertex)) {
                                if (bRep.getInBoundaryEdges().contains(curEdge)) {
                                    cInBoundaryEdges.add(curEdge);
                                }
                                int nextVertex = graphInfo.getOtherNode(curEdge, curVertex);
                                if (!agenda.contains(nextVertex)) {
                                    agenda.add(nextVertex);
                                }
                            }
                        }
                    }
                    
                    components.add(new Component(cvList.toIntArray(), cInBoundaryEdges));
                    
                }
            });
            
            
            
            
            
            
        }
        
        
    }
    
    
    
    //for "remember"
    public ComponentManager(ComponentManager cm, int newSourceNode, GraphInfo graphInfo) {
        components = new HashSet<>();
        
        boolean containsNode = false;//just to double check
        for (Component comp : cm.components) {
            if (comp.contains(newSourceNode, graphInfo)) {
                containsNode = true;
                
                int[] incidentEdges = graphInfo.getIncidentEdges(newSourceNode);
                IntSet seenInciEdges = new IntOpenHashSet();
                for (int inciEdge : incidentEdges) {
                    
                    IntSet currentSourceNodes = new IntOpenHashSet();
                    currentSourceNodes.add(newSourceNode);
                    
                    IdBasedEdgeSet currentInBoundaryEdges;
                    if (cm.components.iterator().hasNext()) {
                        if (cm.components.iterator().next().inBoundaryEdges instanceof ShortBasedEdgeSet) {
                            currentInBoundaryEdges = new ShortBasedEdgeSet();
                        } else {
                            currentInBoundaryEdges = new ByteBasedEdgeSet();
                        }
                    } else {
                        System.err.println("Terrible Error in ComponentManager constuctor!");
                        currentInBoundaryEdges = null;
                    }
                    currentInBoundaryEdges.add(inciEdge);
                    
                    int neighborNode = graphInfo.getOtherNode(inciEdge, newSourceNode);
                    if (neighborNode == newSourceNode) {
                        //actually dont need to do any more here
                    } else if (arrayContains(comp.vertices, neighborNode)) {
                        currentSourceNodes.add(neighborNode);
                    } else {
                        IntList agenda = new IntArrayList();
                        agenda.add(neighborNode);
                        IntListIterator it = agenda.iterator();
                        while (it.hasNext()) {
                            int curNode = it.nextInt();
                            for (int edge : graphInfo.getIncidentEdges(curNode)) {
                                int newNode = graphInfo.getOtherNode(edge, curNode);
                                if (newNode == newSourceNode) {
                                    currentInBoundaryEdges.add(edge);
                                    seenInciEdges.add(edge);
                                } else if (arrayContains(comp.vertices, newNode)) {
                                    currentInBoundaryEdges.add(edge);
                                    currentSourceNodes.add(newNode);
                                } else {
                                    if (!agenda.contains(newNode)) {
                                        agenda.add(newNode);
                                    }
                                }

                            }

                        }
                    }
                    
                    components.add(new Component(currentSourceNodes.toIntArray(), currentInBoundaryEdges));
                }
                
            } else {
                components.add(comp);
            }
        }
        if (!containsNode) {
            System.err.println("Terrible Error in ComponentManager constuctor!");
        }
        
        
        
    }
    
    
    
    
    
    //for "split"
    ComponentManager(Set<Component> components) {
        this.components = components;
    }
    
    public boolean isSourceNode(int vNr) {
        for (Component comp : components) {
            if (comp.arrayContains(comp.vertices, vNr)) {
                return true;
            }
        }
        return false;
    }
    
    public List<ComponentManager[]> getAllConnectedNonemptyComplementsPairs() {
        List<ComponentManager[]> ret = new ArrayList<>();
        Set<Component> copySet = new HashSet<>();
        copySet.addAll(components);
        Set<Set<Component>> allSubsets = getAllSubsets(copySet);
        Set<ComponentManager> allManagers = new HashSet<>();
        for (Set<Component> set : allSubsets) {
            allManagers.add(new ComponentManager(set));
        }
        while (!allManagers.isEmpty()) {
            ComponentManager cm = allManagers.iterator().next();
            ComponentManager complement = getComplement(cm);
            if (cm.isConnected() && complement.isConnected() && !cm.components.isEmpty() && ! complement.components.isEmpty()) {
                ret.add(new ComponentManager[]{cm, complement});
            }
            allManagers.remove(cm);
            allManagers.remove(complement);
        }
        return ret;
    }
    
    private ComponentManager getComplement(ComponentManager cm) {
        Set<Component> newSet = new HashSet<>();
        for (Component comp : components) {
            if (!cm.components.contains(comp)) {
                newSet.add(comp);
            }
        }
        return new ComponentManager(newSet);
    }
    
    private Set<Set<Component>> getAllSubsets(Set<Component> set) {
        Set<Set<Component>> ret = new HashSet<>();
        Iterator<Component> it = set.iterator();
        if (it.hasNext()) {
            Component first = it.next();
            if (it.hasNext()) {
                set.remove(first);
                for (Set<Component> recList : getAllSubsets(set)) {
                    ret.add(recList);
                    Set<Component> with = new HashSet<>();
                    with.add(first);
                    with.addAll(recList);
                    ret.add(with);
                }
            } else {
                ret.add(new HashSet<>());
                Set<Component> with = new HashSet<>();
                with.add(first);
                ret.add(with);
            }
        }
        return ret;
    }
    
    private boolean isConnected() {
        Iterator<Component> it = components.iterator();
        if (it.hasNext()) {
            Set<Component> agenda = new HashSet<>();
            Set<Component> lastAdded = new HashSet<>();
            agenda.addAll(components);
            
            Component seed = it.next();
            agenda.remove(seed);
            lastAdded.add(seed);
            
            while (!agenda.isEmpty()) {
                Set<Component> added = new HashSet<>();
                for (Component comp : agenda) {
                    for (Component last : lastAdded) {
                        if (comp.isNeighbour(last)) {
                            added.add(comp);
                            break;
                        }
                    }
                }
                if (added.isEmpty()) {
                    return false;
                } else {
                    agenda.removeAll(added);
                    lastAdded = added;
                }
            }
            return true;
        } else {
            return true;
        }
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null) {
                return false;
            }
            if (other == this) {
                return true;
            }
            if (!(other instanceof ComponentManager)) {
                return false;
            }
            ComponentManager f = (ComponentManager) other;
            return components.equals(f.components);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(19, 43).append(components).toHashCode();
    }
    
    private boolean arrayContains(int[] array, int value) {
        boolean res = false;
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                res = true;
            }
        }
        return res;
    }
    
    public class Component {
        IdBasedEdgeSet inBoundaryEdges;
        int[] vertices;
        
        public Component(int[] vertices, IdBasedEdgeSet inBoundaryEdges) {
            this.inBoundaryEdges = inBoundaryEdges;
            this.vertices = vertices;
        }
        
        public boolean contains (int vNr, GraphInfo graphInfo) {
            if (vertices.length == 0) {
                return true;
            } else if (arrayContains(vertices, vNr)) {
                System.err.println("Feature in Component.contains not supported yet!");
                return true;
            } else {
                int relevantEdge = -1;
                int k = graphInfo.getNrNodes()+1;
                for (int vertex : vertices) {
                    int dist = graphInfo.pwsp.getDistance(vNr, vertex);
                    if (dist<k) {
                        k = dist;
                        relevantEdge = graphInfo.pwsp.getEdge(vNr, vertex);
                    }
                }
                return inBoundaryEdges.contains(relevantEdge);
            }
        }
        
        public boolean isNeighbour(Component other) {
            boolean ret = false;
            for (int vertex : vertices) {
                if (arrayContains(other.vertices, vertex)) {
                    ret = true;
                }
            }
            return ret;
        }
        
        private boolean arrayContains(int[] array, int value) {
            boolean res = false;
            for (int i = 0; i < array.length; i++) {
                if (array[i] == value) {
                    res = true;
                }
            }
            return res;
        }
        
        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (other == this) {
                return true;
            }
            if (!(other instanceof Component)) {
                return false;
            }
            Component f = (Component) other;
            return (inBoundaryEdges.equals(f.inBoundaryEdges) && Arrays.equals(vertices, f.vertices));
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(19, 43).append(inBoundaryEdges).append(vertices).toHashCode();
        }
    }
    
}
