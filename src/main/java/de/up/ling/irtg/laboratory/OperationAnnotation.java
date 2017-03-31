/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotate a method with this, to make it available in Alto Lab. The String
 * <code>code</code> will be the function name in Alto Lab tasks. Codes for
 * static methods must be globally unique, codes for non-static methods must be
 * unique in a class and all super + sub-classes. If a super-method is annotated,
 * the code carries over.
 * @author groschwitz
 */
@Retention(value = RUNTIME)
public @interface OperationAnnotation {
    String code();
    
}
