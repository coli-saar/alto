/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.taa;

import de.saar.basic.Pair;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.util.ObjectWithStringCode;
import java.util.List;
import java.util.function.Function;

/**
 *
 * @author groschwitz
 */
public class TAAOperationImplementation extends ObjectWithStringCode {
    private final Function<Pair<InterpretedTreeAutomaton, Pair<List<Object>, Instance>>, Object> implementation;
    private final List<TAAOperationProperty> properties;
    private final String name;
    private final String code;
    protected final Class[] childClasses;
    protected final Class resultClass;
    
    
    /**
     * 
     * @param name The name of the Implementation as shown to the user. No internal
     * consequences.
     * @param code This is the code used for this implementation when representing
     * a TAATree as a string. Changing it removes backward compatibility!
     * @param implementation
     * @param properties set to null if there are no properties
     * @param childClasses
     * @param resultClass
     */
    public TAAOperationImplementation(String name, String code, Function<Pair<InterpretedTreeAutomaton, Pair<List<Object>, Instance>>, Object> implementation, List<TAAOperationProperty> properties,
            Class[] childClasses, Class resultClass) {
        this.childClasses = childClasses;
        this.resultClass = resultClass;
        this.implementation = implementation;
        this.properties = properties;
        this.name = name;
        this.code = code;
    }
    
    

    public Class[] getChildClasses() {
        return childClasses;
    }

    public Class getResultClass() {
        return resultClass;
    }
    
    public Object apply(List<Object> input, Instance instance, InterpretedTreeAutomaton irtg) {
        return implementation.apply(new Pair(irtg, new Pair(input, instance)));
    }
    
    public List<TAAOperationProperty> getProperties() {
        return properties;
    }
    
    protected void addProperty(TAAOperationProperty property) {
        properties.add(property);
    }
    
    protected void addAllProperties(List<TAAOperationProperty> properties) {
        this.properties.addAll(properties);
    }
    
    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
}
