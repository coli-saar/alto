/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import org.jgrapht.DirectedGraph;
/**
 *
 * @author jonas
 */
public class PairwiseShortestPaths {//this class computes (and stores) pairwise shortest paths and their last edge in the constructor.
    private int[][] pwShortestPaths;
    private int[][] edges;
    private final int graphSize;
    
    public PairwiseShortestPaths(SGraph wholeGraph, GraphInfo graphInfo)
    {
        graphSize = wholeGraph.getGraph().vertexSet().size();
        //find the pairwise shortest paths (-> Floyd-Warshall):
        FloydWarshall(wholeGraph, graphInfo);
    }
    
    
    private void FloydWarshall(SGraph wholeGraph, GraphInfo graphInfo)
    {
        int n = graphSize;// just for less typing and better reading
        DirectedGraph<GraphNode, GraphEdge> g = wholeGraph.getGraph();
        pwShortestPaths = new int[n][n];
        edges = new int[n][n];
        int[][] oldP = new int[n][n];//path lengths from previous or base step, from node j to l if oldP[j][l]  --- maybe use just p[][][] instead, and not clone later? seems like storage vs runtime.
        int[][] newP = new int[n][n];// path lengths in current step, from node j to l if newP[j][l]
        int[][] oldE = new int[n][n];//last edge from previous or base step, in path from j to l if oldE[j][l]
        int[][] newE = new int[n][n];//last edge from current step, in path from j to l if newE[j][l]
        for (int j = 0; j<n; j++)// start with paths without internal nodes
        {
            for (int l = j; l<n; l++)//maybe k=j+1, since diagonal unimportant?
            {
                GraphNode nodej = wholeGraph.getNode(graphInfo.getNodeForInt(j));
                GraphNode nodek = wholeGraph.getNode(graphInfo.getNodeForInt(l));
                if (g.containsEdge(nodej, nodek))
                {
                    oldP[j][l] = 1;
                    oldP[l][j] = 1;
                    oldE[j][l] = graphInfo.getEdge(j,l); // always making new edges may be very storage inefficient. Better have one big set and get them from there!.
                    oldE[l][j] = graphInfo.getEdge(j,l);
                }
                else if (g.containsEdge(nodek, nodej))
                {
                    oldP[j][l] = 1;
                    oldP[l][j] = 1;
                    oldE[j][l] = graphInfo.getEdge(l,j); // always making new edges may be very storage inefficient. Better have one big set and get them from there!.
                    oldE[l][j] = graphInfo.getEdge(l,j);
                }
                else
                {
                    oldP[j][l] = n+1;//this is longer than any shortest path!
                    oldP[l][j] = n+1;
                    //no edges assigned here.
                }
            }
        }
        for (int k = 0; k<n; k++)// add internal nodes k one after the other
        {
            for (int j = 0; j<n; j++)
            {
                for (int l = j; l<n; l++)//maybe k=j+1, since diagonal unimportant?
                {
                    int a = oldP[j][l];
                    int b = oldP[j][k]+oldP[k][l];
                    if (a<b)//then path using only internal nodes "smaller than" k is shorter
                    {
                        newP[j][l] = a;
                        newP[l][j] = a;
                        newE[j][l] = oldE[j][l];// gotta be careful about the indices here! s.t. last edge in one is last edge in other.
                        newE[l][j] = oldE[l][j];// gotta be careful about the indices here! s.t. last edge in one is last edge in other.
                    }
                    else// then path using also node k is shorter
                    {
                        newP[j][l] = b;
                        newP[l][j] = b;
                        newE[j][l] = oldE[k][l];// gotta be careful about the indices here! s.t. last edge in one is last edge in other.
                        newE[l][j] = oldE[k][j];// gotta be careful about the indices here! s.t. last edge in one is last edge in other.
                    }
                }
            }
            oldP = newP.clone();
            oldE = newE.clone();
        }
        
        pwShortestPaths = newP;
        edges = newE;
    }
    
    public Integer getDistance(int node1, int node2)
    {
        return pwShortestPaths[node1][node2];
    }
    
    /**
     * returns the edge of shortest path between the nodes which is incident to endNode.
     * @param startNode
     * @param endNode
     * @return
     */
    public int getEdge(int startNode, int endNode)
    {
        return edges[startNode][endNode];
    }
    
    public int getGraphSize()
    {
        return graphSize;
    }
}
