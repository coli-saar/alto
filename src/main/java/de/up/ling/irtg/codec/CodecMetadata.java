/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation type for adding metadata to a codec class.
 * Each codec class must be annotated with CodecMetadata in order
 * to be registered with the <code>CodecManager</code>. Use this
 * annotation to specify a name and extension for the codec.
 * You may optionally annotate a codec as "experimental=true" to
 * mark it as experimental. (From Utool.)
 * 
 * @author Alexander Koller
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CodecMetadata {
	String name();
        String description();
	String extension() default "";
        Class type();
        boolean displayInPopup() default true;
	boolean experimental() default false;
}
