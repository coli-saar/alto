/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import java.io.StringReader;

/**
 *
 * @author koller
 */
class Util {
    
    public static void assertIsomorphic(LambdaGraph gold, LambdaGraph result, Boolean expectedResult) {
        boolean iso = gold.equals(result);

        if (expectedResult.booleanValue()) {
            assert iso : "expected " + gold + ", got non-isomorphic " + result;
        } else {
            assert !iso : "expected non-isomorphic to " + gold + ", but got isomorphic " + result;
        }
    }
    
     public static void assertIsomorphic(LambdaGraph gold, LambdaGraph result) {
         assertIsomorphic(gold, result, Boolean.TRUE);
     }
    
    public static LambdaGraph pg(String s) throws ParseException {
        return IsiAmrParser.parse(new StringReader(s));
    }
}
