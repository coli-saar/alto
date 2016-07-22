/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.taa;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 *
 * @author groschwitz
 */
public class TAAOperationProperty {
    protected final String name;
    
    protected Object value;
    protected String valueName;
    protected final Map<String, Object> possibleValues;//set to null if not supposed to be used
    
    protected Consumer<String> valueSetter;
    
    /**
     * 
     * @param name the name of this property
     * @param possibleValues must not be null!
     */
    //note: internally, we sometimes use this with possibleValues = null, but then overwrite valueSetter accordingly.
    public TAAOperationProperty(String name, Map<String, Object> possibleValues) {
        this.possibleValues = possibleValues;
        this.name = name;
        
        valueSetter = (inputName) -> {
          Object inputObject = possibleValues.get(inputName);
          if (inputObject != null) {
              valueName = inputName;
              value = inputObject;
          }
        };
    }
    
    /*protected GraphOperationProperty(String name, Consumer<String> valueSetter) {
        this.possibleValues = null;
        this.name = name;
        
        this.valueSetter = valueSetter;
    }*/
    
    public Object getValue() {
        return value;
    }
    
    public String getValueName() {
        return valueName;
    }
    
    public Set<String> getPossibleValueNames() {
        if (possibleValues != null) {
            return possibleValues.keySet();
        } else {
            return null;
        }
    }
    
    public void setValueByName(String valueName) {
        valueSetter.accept(valueName);
    }
    
    public String getName() {
        return name;
    }
    
    
    
    public static class StringProperty extends TAAOperationProperty{

        public StringProperty(String name, String defaultValue) {
            super(name, null);
            valueSetter = (inputName) -> {
                valueName = inputName;
                value = inputName;
            };
            setValue(defaultValue);
        }
        
        //just for quality of life
        public final void setValue(String value) {
            setValueByName(value);
        }
        
        //just for quality of life
        @Override
        public String getValue() {
            return (String) value;
        }
    }
    
    
    public static class BooleanProperty extends TAAOperationProperty{

        public BooleanProperty(String name, boolean defaultValue) {
            super(name, null);
            valueSetter = (inputName) -> {
                valueName = inputName;
                try {
                    value = Boolean.valueOf(inputName);
                } catch (java.lang.Exception e) {
                    //i think it is ok to do nothing here
                }
            };
            setValue(defaultValue);
        }
        
        public final void setValue(boolean value) {
            setValueByName(String.valueOf(value));
        }
        
        @Override
        public Boolean getValue() {
            return (Boolean)value;
        }
    }
    
    
    
    public static class IntegerProperty extends TAAOperationProperty{

        public IntegerProperty(String name, int defaultValue) {
            super(name, null);
            valueSetter = (inputName) -> {
                valueName = inputName;
                try {
                    value = Integer.valueOf(inputName);
                } catch (java.lang.Exception e) {
                    //i think it is ok to do nothing here
                }
            };
            setValue(defaultValue);
        }
        
        
        public final void setValue(int value) {
            setValueByName(String.valueOf(value));
        }
        
        @Override
        public Integer getValue() {
            return (Integer)value;
        }
        
    }
    
    public static class DoubleProperty extends TAAOperationProperty{

        public DoubleProperty(String name, double defaultValue) {
            super(name, null);
            valueSetter = (inputName) -> {
                valueName = inputName;
                try {
                    value = Double.valueOf(inputName);
                } catch (java.lang.Exception e) {
                    //i think it is ok to do nothing here
                }
            };
            setValue(defaultValue);
        }
        
        
        public final void setValue(double value) {
            setValueByName(String.valueOf(value));
        }
        
        @Override
        public Double getValue() {
            return (Double)value;
        }
        
    }
}
