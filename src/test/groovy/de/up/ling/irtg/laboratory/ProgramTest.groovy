/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.laboratory

import org.junit.Test
import static org.junit.Assert.*
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier


/**
 *
 * @author groschwitz
 */
class ProgramTest {
    
    @Test
     public void algMethodTest() {
        Method algMethod = Program.getGetAlgMethod();
        assert algMethod != null;
     }
     
    @Test
     public void staticDuplicatesTest() {
        Map<String, Method> map = new HashMap<>();
        for (Class clazz : Program.getAllClassesIn_irtg_Classpath()) {
            for (Method m : clazz.getMethods()) {
                if (Modifier.isStatic(m.getModifiers()) && m.getAnnotation(OperationAnnotation.class) != null) {
                    OperationAnnotation annot = m.getAnnotation(OperationAnnotation.class);
                    Method other = map.get(annot.code());
                    assert (other == null || other.equals(m));
                    map.put(annot.code(), m);
                }
            }
        }
     }
     
    @Test
     public void constructorDuplicatesTest() {
        Map<String, Constructor> map = new HashMap<>();
        for (Class clazz : Program.getAllClassesIn_irtg_Classpath()) {
            for (Constructor m : clazz.getConstructors()) {
                if (m.getAnnotation(OperationAnnotation.class) != null) { //Modifier.isStatic(m.getModifiers()) &&
                    OperationAnnotation annot = (OperationAnnotation) m.getAnnotation(OperationAnnotation.class);
                    Constructor other = map.get(annot.code());
                    assert (other == null || other.equals(m));
                    map.put(annot.code(), m);
                }
            }
        }
     }
     
    @Test
    public void classMethodDuplicatesTest() {
        for (Class clazz : Program.getAllClassesIn_irtg_Classpath()) {
            Map<String, Method> map = new HashMap<>();
            for (Method m : clazz.getMethods()) {
                OperationAnnotation annot = Program.findAnnotation(m, OperationAnnotation.class);
                if (annot != null) {
                    Method other = map.get(annot.code());
                    assert (other == null || (other.getName().equals(m.getName()) && Arrays.equals(other.getParameterTypes(), m.getParameterTypes())));
                    map.put(annot.code(), m);
                }
            }
        }
    }
    
}
