/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;

import java.io.InputStream;

/**
 * Input codec corresponding to the default SGraph#toString method.
 * @author groschwitz
 */
public class SGraphInputCodec extends InputCodec<SGraph>{

    @Override
    public SGraph read(InputStream is) throws CodecParseException {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        try {
            return graphFromString(s.hasNext() ? s.next() : "");
        } catch (ParserException ex) {
            throw new CodecParseException(ex);
        }

    }
    
    private SGraph graphFromString(String graphRep) throws ParserException {
        SGraph ret = new SGraph();
        String[] edgeList = graphRep.substring(1, graphRep.length()-1).split("; ");
        for (String edgeWithSourceAndTarget : edgeList) {
            String[] splitOffTargetNode = edgeWithSourceAndTarget.split("-> ");
            switch (splitOffTargetNode.length) {
                case 2: 
                    String targetNodeRep = splitOffTargetNode[1];
                    int edgeStart = splitOffTargetNode[0].lastIndexOf(" -");
                    String sourceNodeRep = splitOffTargetNode[0].substring(0, edgeStart);
                    String edgeLabel = splitOffTargetNode[0].substring(edgeStart+2);
                    //System.err.println("Source: "+sourceNodeRep + " Edge: "+edgeLabel + " Target: "+targetNodeRep);
                    GraphNode sourceNode = parseNodeAndAddToGraph(sourceNodeRep, ret);
                    GraphNode targetNode = parseNodeAndAddToGraph(targetNodeRep, ret);
                    ret.addEdge(sourceNode, targetNode, edgeLabel);
                    break;
                case 1: 
                    parseNodeAndAddToGraph(splitOffTargetNode[0], ret);//when pattern '-> ' is not found, we have no edge and can just add the node.
                    break;
                default: throw new ParserException("Could not uniquely identify edge, after splitting along '; '. Edge "+edgeWithSourceAndTarget+" in "+graphRep);
            }
        }
        //System.err.println(ret.toString());
        return ret;
    }
    
    private GraphNode parseNodeAndAddToGraph(String nodeRep, SGraph graphToConstruct)  throws ParserException {
        int splitPoint = nodeRep.indexOf("/");//take first index, assume it is more likely to have a / in the label
        String inFromOfSplit;
        String nodeLabel = null;
        if (splitPoint >-1) {
            inFromOfSplit = nodeRep.substring(0, splitPoint);
            nodeLabel = nodeRep.substring(splitPoint+1);
        } else {
            //then no label
            inFromOfSplit = nodeRep;
        }
        String[] sources;
        String nodeName;
        if (inFromOfSplit.endsWith(">")) {
            //that means we have sources
            int sourceStart = inFromOfSplit.lastIndexOf("<");
            if (sourceStart < 0) {
                throw new ParserException("Error when reading the sources in '"+inFromOfSplit+"'");
            }
            String sourceString = inFromOfSplit.substring(sourceStart+1, inFromOfSplit.length()-1);//strip the < and >
            sources = sourceString.split(",");
            nodeName = inFromOfSplit.substring(0, sourceStart);
        } else {
            sources = new String[0];
            nodeName = inFromOfSplit;
        }
        
        GraphNode existingNode = graphToConstruct.getNode(nodeName);
        if (existingNode != null) {
            return existingNode;//then we are done, since the node exists already
        } else {
            //add the node
            GraphNode ret = graphToConstruct.addNode(nodeName, nodeLabel);
            for (String sourceName : sources) {
                graphToConstruct.addSource(sourceName, nodeName);
            }
            return ret;
        }
    }
    
}
