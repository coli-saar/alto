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
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 *
 * @author jonas
 */

public class ComponentManager {
    
    final IntSet components;
    final Int2ObjectMap<Component> componentById;
    final Object2IntMap<Component> componentToId;
    private int maxID;
    
    //for completeGraph
    public ComponentManager(BoundaryRepresentation bRep, GraphInfo graphInfo) {
        maxID = 0;
        components = new IntOpenHashSet();
        componentById = new Int2ObjectOpenHashMap<>();
        componentToId = new Object2IntOpenHashMap<>();
        if (bRep.getInBoundaryEdges().isEmpty()) {
            IdBasedEdgeSet inBoundaryEdges;
            
            if (graphInfo.useBytes()) {
                inBoundaryEdges = new ByteBasedEdgeSet();
            } else {
                inBoundaryEdges = new ShortBasedEdgeSet();
            }
            addComponent(new Component(new int[0], inBoundaryEdges, graphInfo));
        } else {
            IntSet seenEdges = new IntOpenHashSet();
            bRep.getInBoundaryEdges().forEach(edge -> {
                if (!seenEdges.contains(edge)) {
                    //init new component ingredients
                    IntList cvList = new IntArrayList();
                    IdBasedEdgeSet cInBoundaryEdges;
                    if (graphInfo.useBytes()) {
                        cInBoundaryEdges = new ByteBasedEdgeSet();
                    } else {
                        cInBoundaryEdges = new ShortBasedEdgeSet();
                    }
                    
                    //add edge where necessary
                    seenEdges.add(edge);
                    cInBoundaryEdges.add(edge);
                    
                    //init agenda and take care of first vertices
                    IntList agenda = new IntArrayList();
                    boolean useSource = bRep.isSource(graphInfo.getEdgeSource(edge));
                    int firstVertex;
                    int seedVertex;
                    if (useSource) {
                        firstVertex = graphInfo.getEdgeSource(edge);
                        seedVertex = graphInfo.getEdgeTarget(edge);
                    } else {
                        firstVertex = graphInfo.getEdgeTarget(edge);
                        seedVertex = graphInfo.getEdgeSource(edge);
                    }
                    cvList.add(firstVertex);
                    agenda.add(seedVertex);
                    
                    //iteration over agenda
                    for (int curVertex : agenda) {
                        if (bRep.isSource(curVertex)) {
                            cvList.add(curVertex);
                        } else {
                            for (int curEdge : graphInfo.getIncidentEdges(curVertex)) {
                                if (bRep.getInBoundaryEdges().contains(curEdge)) {
                                    seenEdges.add(curEdge);
                                    cInBoundaryEdges.add(curEdge);
                                }
                                int nextVertex = graphInfo.getOtherNode(curEdge, curVertex);
                                if (!agenda.contains(nextVertex)) {
                                    agenda.add(nextVertex);
                                }
                            }
                        }
                    }
                    
                    addComponent(new Component(cvList.toIntArray(), cInBoundaryEdges, graphInfo));
                    
                }
            });
            
            
            
            
            
            
        }
        //checkEquality();
        
    }
    
    private void checkEquality() {
        for (int i1: components) {
            for (int i2: components) {
                if (i1 != i2 && componentById.get(i1).equalsPrecise(componentById.get(i2))) {
                    System.err.println("terrible error in managing component IDs in componentManager!");
                }
            }
        }
    }
    
    private void addComponent(Component comp) {
        maxID++;
        componentById.put(maxID, comp);
        componentToId.put(comp, maxID);
        components.add(maxID);
    }
    
    
    //for "remember"
    public ComponentManager(ComponentManager cm, int newSourceNode, GraphInfo graphInfo) {
        components = new IntOpenHashSet();
        componentById = new Int2ObjectOpenHashMap<>();
        componentToId = new Object2IntOpenHashMap<>();
        componentById.putAll(cm.componentById);
        componentToId.putAll(cm.componentToId);
        this.maxID = cm.maxID;
        boolean containsNode = false;//just to double check
        for (int compID : cm.components) {
            Component comp = componentById.get(compID);
            if (comp.contains(newSourceNode, graphInfo)) {
                containsNode = true;
                
                int[] incidentEdges = graphInfo.getIncidentEdges(newSourceNode);
                for (int inciEdge : incidentEdges) {
                    
                    IntSet currentSourceNodes = new IntOpenHashSet();
                    currentSourceNodes.add(newSourceNode);
                    
                    IdBasedEdgeSet currentInBoundaryEdges;
                    if (cm.componentToId.keySet().iterator().hasNext()) {
                        if (cm.componentToId.keySet().iterator().next().inBoundaryEdges instanceof ShortBasedEdgeSet) {
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
                    
                    addComponent(new Component(currentSourceNodes.toIntArray(), currentInBoundaryEdges, graphInfo));
                }
                
            } else {
                components.add(compID);
            }
        }
        if (!containsNode) {
            System.err.println("Terrible Error in ComponentManager constuctor!");
        }
        
        //checkEquality();
        
    }
    
    
    
    
    
    //for "split"
    ComponentManager(ComponentManager parent, IntSet components) {
        this.components = components;
        componentById = parent.componentById;
        componentToId = parent.componentToId;
        this.maxID = parent.maxID;
        //checkEquality();
    }
    
    public boolean isSourceNode(int vNr) {
        for (int compID : components) {
            Component comp = componentById.get(compID);
            if (comp.arrayContains(comp.vertices, vNr)) {
                return true;
            }
        }
        return false;
    }
    
    public Component getAComponent(){
        return componentById.get(components.iterator().nextInt());
    }
            
    public boolean isEmpty() {
        return components.isEmpty();
    }
    
    public List<ComponentManager[]> getAllConnectedNonemptyComplementsPairs() {
        List<ComponentManager[]> ret = new ArrayList<>();
        int first = components.iterator().nextInt();
        IntList rest = new IntArrayList(components);
        rest.rem(first);
        iterateOverAllSubsetsByComplements(new IntArrayList(first), rest, cm -> {
            IntSet complement = getIntComplement(cm);
            if (isConnectedInt(cm) && isConnectedInt(complement) && !cm.isEmpty() && ! complement.isEmpty()) {
                ret.add(new ComponentManager[]{new ComponentManager(this, cm), new ComponentManager(this, complement)});
            }
        });
        
        /*IntSet copySet = new IntOpenHashSet();
        copySet.addAll(components);
        List<IntSet> allSubsets = getAllSubsets(copySet);
        Set<IntSet> allManagers = new HashSet<>(allSubsets);
        //System.err.println("set: " + components);
        //System.err.println("setSize: " + components.size());
        //System.err.println("subsets: " + allSubsets.size());
        
        while (!allManagers.isEmpty()) {
            IntSet cm = allManagers.iterator().next();
            IntSet complement = getIntComplement(cm);
            if (isConnectedInt(cm) && isConnectedInt(complement) && !cm.isEmpty() && ! complement.isEmpty()) {
                ret.add(new ComponentManager[]{new ComponentManager(this, cm), new ComponentManager(this, complement)});
            }
            allManagers.remove(cm);
            allManagers.remove(complement);
            //System.err.println(allManagers.size());
        }*/
        return ret;
    }
    
    private boolean isConnectedInt(IntSet cm) {
        IntIterator it = cm.iterator();
        if (it.hasNext()) {
            Component seed = componentById.get(it.next());
            Set<Component> agenda = new HashSet<>();
            Set<Component> lastAdded = new HashSet<>();
            while (it.hasNext()) {
                agenda.add(componentById.get(it.nextInt()));
            }
            
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
    
    private ComponentManager getComplement(ComponentManager cm) {
        IntSet newSet = new IntOpenHashSet();
        for (int compID : components) {
            if (!cm.components.contains(compID)) {
                newSet.add(compID);
            }
        }
        return new ComponentManager(this, newSet);
    }
    
    private IntSet getIntComplement(IntSet other) {
        IntSet newSet = new IntOpenHashSet();
        for (int compID : components) {
            if (!other.contains(compID)) {
                newSet.add(compID);
            }
        }
        return newSet;
    }
    
    private List<IntSet> getAllSubsets(IntSet set) {
        List<IntSet> ret = new ArrayList<>();
        if (!set.isEmpty()) {
            int first = set.iterator().nextInt();
            if (set.size()>1) {
                IntSet newSet = new IntOpenHashSet(set);
                newSet.remove(first);
                for (IntSet recSet : getAllSubsets(newSet)) {
                    ret.add(recSet);
                    IntSet with = new IntOpenHashSet();
                    with.add(first);
                    with.addAll(recSet);
                    ret.add(with);
                }
            } else {
                ret.add(new IntOpenHashSet());
                IntSet with = new IntOpenHashSet();
                with.add(first);
                ret.add(with);
            }
        }
        return ret;
    }
    
    
    //start with previous containing one of the total items
    private void iterateOverAllSubsetsByComplements(IntList previous, IntList next, Consumer<IntSet> function) {
        int nextSize = next.size();
        if (nextSize>0) {
            int first = next.iterator().nextInt();
            if (nextSize>1) {
                IntList newNext = next.subList(1, nextSize);
                IntList newPreviousWith = new IntArrayList(previous);
                newPreviousWith.add(first);
                iterateOverAllSubsetsByComplements(previous, newNext, function);
                iterateOverAllSubsetsByComplements(newPreviousWith, newNext, function);
                
            } else {
                IntSet with = new IntOpenHashSet(previous);
                with.add(next.get(0));
                function.accept(with);
                function.accept(new IntOpenHashSet(previous));
            }
        }
    }
    
    private boolean isConnected() {
        return isConnectedInt(components);
    }
    
    
    //note that these equality checks only work if the int maps are sufficiently identical (i.e. if there is a common parent).
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
        GraphInfo completeGraphInfo;
        
        public Component(int[] vertices, IdBasedEdgeSet inBoundaryEdges, GraphInfo completeGraphInfo) {
            this.inBoundaryEdges = inBoundaryEdges;
            this.vertices = vertices;
            this.completeGraphInfo = completeGraphInfo;
        }
        
        public boolean contains (int vNr, GraphInfo graphInfo) {
            if (vertices.length == 0) {
                return true;
            } else if (arrayContains(vertices, vNr)) {
                System.err.println("Feature in Component.contains not supported yet!");
                return true;
            } else {
                int relevantEdge = graphInfo.getDecidingEdgePWSP(vertices, vNr);
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
        
        public boolean equalsPrecise(Object other) {
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
        public String toString() {
            StringJoiner nodes = new StringJoiner(", ", "[", "]");

            for (int vNr = 0; vNr < completeGraphInfo.getNrNodes(); vNr++) {
                if (arrayContains(vertices, vNr)) {
                    StringBuilder vRes = new StringBuilder();
                    vRes.append(completeGraphInfo.getNodeForInt(vNr));
                    int[] edges = completeGraphInfo.getIncidentEdges(vNr);
                    StringJoiner edgeSJ = new StringJoiner(", ", "{", "}");
                    for (int edge : edges) {
                        if (inBoundaryEdges.contains(edge)) {
                            edgeSJ.add(completeGraphInfo.getNodeForInt(completeGraphInfo.getEdgeSource(edge)) + "_" + completeGraphInfo.getNodeForInt(completeGraphInfo.getEdgeTarget(edge)));
                        }
                    }
                    vRes.append(" " + edgeSJ);
                    nodes.add(vRes);
                }

            }
            return nodes.toString();
        }
        
        //treat them all distinctly for now
        /*@Override
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
        }*/
    }
    
}
