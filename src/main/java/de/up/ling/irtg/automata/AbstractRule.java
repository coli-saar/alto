/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

/**
 *
 * @author christoph_teichmann
 */
public interface AbstractRule {

    int getArity();

    int[] getChildren();

    /**
     * Retrieves the auxiliary information from this rule.
     *
     * @see #setExtra(java.lang.Object)
     * @return
     */
    Object getExtra();

    int getParent();

    double getWeight();

    boolean isLoop();

    /**
     * Stores auxiliary information within this rule. Do not use this unless you
     * know what you're doing.
     *
     * @param extra
     */
    void setExtra(Object extra);

    void setWeight(double weight);
}
