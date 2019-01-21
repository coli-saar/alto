/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.experimental.astar;

/**
 *
 * @author koller
 */
public class OutsideEstimator {
    private double[] bestEdgep;      // bestEdgep[k]   = max_{i,o} edgep[i][k][o]
    private double[] bestTagp;       // bestTagp[k]    = max_s tagp[k][s]
    private double[] outsideLeft;    // outsideLeft[k] = sum_{0 <= i < k} bestEdgep[i] + bestTagp[i]
    private double[] outsideRight;   // outsideRight[k] = sum_{k <= i < n} bestEdgep[i] + bestTagp[i]
    private double[] worstIncomingLeft; // min score of the best incoming edges in 0...k
    private double[] worstIncomingRight; // min score of the best incoming edges in k...n
    
    private final int N;
    
    private double left(int i) {
        return outsideLeft[i];
    }
    
    private double right(int i) {
        if( i >= N ) {
            return 0;
        } else {
            return outsideRight[i];
        }
    }

    /**
     * Returns an outside estimate of the given item.
     * 
     * @param it
     * @return 
     */
    public double evaluate(Item it) {
        double v = left(it.getStart()) + right(it.getEnd()); // supertags and best incoming edges for the left and right context
        double worstIncomingInOutside = Math.min(worstIncomingLeft[it.getStart()], worstIncomingRight[it.getEnd()]); // worst of the best incoming edges in the left and right context
        double ret = 0;
        
        // We get to ignore one of the incoming edges (for the node that will end up as the root).
        // This edge is either into it.root, or into one of the nodes in the left or right context.
        if( bestEdgep[it.getRoot()] < worstIncomingInOutside ) {
            ret = v;
        } else {
            ret = v + bestEdgep[it.getRoot()] - worstIncomingInOutside;
        }
        
//        System.err.printf("  eval %s -> outside est=%f\n", it, ret);
        return ret;
    }
    
    public OutsideEstimator(double[][][] edgep, double[][] tagp, int NUM_EDGELABELS, int NUM_TAGS) {
        N  = edgep.length;              // sentence length
        
        // calculate best incoming edge for each token
        bestEdgep = new double[N];
        for( int k = 0; k < N; k++ ) {
            double max = Double.NEGATIVE_INFINITY;
            
            for( int i = 0; i < N; i++ ) {
                for( int o = 0; o < NUM_EDGELABELS; o++ ) {
                    max = Math.max(max, edgep[i][k][o]);
                }
            }
            
            bestEdgep[k] = max;
        }
        
        // calculate best supertag for each token
        bestTagp = new double[N];
        for( int k = 0; k < N; k++ ) {
            double max = Double.NEGATIVE_INFINITY;
            for( int s = 0; s < NUM_TAGS; s++ ) {
                max = Math.max(max, tagp[k][s]);
            }
            bestTagp[k] = max;
        }
        
        // calculate left-side outside estimates
        outsideLeft = new double[N];
        worstIncomingLeft = new double[N];
        
        for( int k = 0; k < N; k++ ) {
            double sum = 0;
            double worst = 0;
            
            for( int i = 0; i < k; i++ ) {
                sum += bestTagp[i] + bestEdgep[i];
                worst = Math.min(worst, bestEdgep[i]);
            }
            
            outsideLeft[k] = sum;
            worstIncomingLeft[k] = worst;
        }
        
        // calculate right-side outside estimates
        outsideRight = new double[N+1];
        worstIncomingRight = new double[N+1];
        
        for( int k = 0; k <= N; k++ ) {
            double sum = 0;
            double worst = 0;
            
            for( int i = k; i < N; i++ ) {
                sum += bestTagp[i] + bestEdgep[i];
                worst = Math.min(worst, bestEdgep[i]);
            }
            
            outsideRight[k] = sum;
            worstIncomingRight[k] = worst;
        }
    }
}
