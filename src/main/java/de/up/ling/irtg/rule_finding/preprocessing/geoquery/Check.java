/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing.geoquery;

/**
 *
 * @author christoph_teichmann
 */
public interface Check {

    /**
     *
     * @param pos
     * @param arr
     * @return
     */
    int knownPattern(int pos, String[] arr);
    
}
