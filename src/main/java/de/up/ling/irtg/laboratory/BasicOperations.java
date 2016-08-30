/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

/**
 * This class contains static methods for Alto Lab that don't necessarily fit
 * anywhere else.
 *
 * @author koller
 */
public class BasicOperations {

    @OperationAnnotation(code = "percent")
    public static double percent(double x, double y) {
        return 100.0 * x / y;
    }
}
