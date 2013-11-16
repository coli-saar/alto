/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import java.io.StringReader;
import org.jgrapht.experimental.isomorphism.AdaptiveIsomorphismInspectorFactory;
import org.jgrapht.experimental.isomorphism.GraphIsomorphismInspector;
import org.jgrapht.experimental.isomorphism.IsomorphismRelation;

/**
 *
 * @author koller
 */
class Util {
    public static void assertIsomorphic(LambdaGraph gold, LambdaGraph result, Boolean expectedResult) {
        GraphIsomorphismInspector iso =
                AdaptiveIsomorphismInspectorFactory.createIsomorphismInspector(
                gold.getGraph(),
                result.getGraph(),
                new GraphNode.NodeLabelEquivalenceComparator(),
                null);
        
        boolean isIso = iso.isIsomorphic();
        
        
        // TODO - use this to ensure that the variable lists are equivalent
//        while( iso.hasNext() ) {
//            IsomorphismRelation<GraphNode,GraphEdge> ir = (IsomorphismRelation<GraphNode, GraphEdge>) iso.next();
//            
//            System.err.println("perm:");
//            for( GraphNode u : gold.getGraph().vertexSet() ) {
//                System.err.println("   " + u.repr() + " -> " + ir.getVertexCorrespondence(u, true).repr());
//            }
//        }

        if (expectedResult.booleanValue()) {
            assert iso.isIsomorphic() : "expected " + gold + ", got non-isomorphic " + result;
        } else {
            assert !iso.isIsomorphic() : "expected non-isomorphic to " + gold + ", but got isomorphic " + result;
        }
    }
    
     public static void assertIsomorphic(LambdaGraph gold, LambdaGraph result) {
         assertIsomorphic(gold, result, Boolean.TRUE);
     }
    
    public static LambdaGraph pg(String s) throws ParseException {
        return IsiAmrParser.parse(new StringReader(s));
    }
}
