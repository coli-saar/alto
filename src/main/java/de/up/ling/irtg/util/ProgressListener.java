/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.util;

/**
 *
 * @author koller
 */
@FunctionalInterface
public interface ProgressListener {
    void accept(int currentValue, int maxValue, String string);
}
