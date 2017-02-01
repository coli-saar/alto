/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

import de.up.ling.tree.Tree;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 *
 * @author groschwitz
 */
public interface Operation {
    
    
    public Object apply(List<Object> input) throws IllegalAccessException, InvocationTargetException, InstantiationException;
    
    public Class getReturnType();
    
    
    
    public static class MethodOperation implements Operation {

        private final Method m;
        private final Class returnType;
        private final boolean isStatic;
        
        public MethodOperation(Method m, boolean isStatic) {
            this.m = m;
            returnType = m.getReturnType();
            this.isStatic = isStatic;
        }
        
        public MethodOperation(Method m, Class returnTypeOverride, boolean isStatic) {
            this.m = m;
            returnType = returnTypeOverride;
            this.isStatic = isStatic;
        }
        
        @Override
        public Object apply(List<Object> input) throws IllegalAccessException, InvocationTargetException {
            Object invokingObject = null;
            if (!isStatic) {
                invokingObject = input.remove(0);
            }
            
            if( invokingObject == null && ! isStatic ) {
                throw new NullPointerException("Attempting to invoke method " + m.toString() + " on null object.");
            }
            
            if (input != null && !input.isEmpty()) {
                return m.invoke(invokingObject, input.toArray());
            } else {
                return m.invoke(invokingObject);
            }
        }

        @Override
        public Class getReturnType() {
            return returnType;
        }
        
        @Override
        public String toString() {
            return m.getName();
        }
    }
    
    
    public static class ConstructorOperation implements Operation {

        private final Constructor m;
        private final Class returnType;
        
        public ConstructorOperation(Constructor m) {
            this.m = m;
            returnType = m.getDeclaringClass();
        }
        
        
        @Override
        public Object apply(List<Object> input) throws IllegalAccessException, InvocationTargetException, InstantiationException {
            return m.newInstance(input.toArray());
        }

        @Override
        public Class getReturnType() {
            return returnType;
        }
        
        @Override
        public String toString() {
            return m.getName();
        }
    }
    
    
    
    public static class PrimitiveTypeOperation implements Operation {

        private Object ret;
        private Class returnType;
        
        public PrimitiveTypeOperation(String s) throws NumberFormatException {
            if (s == null) {
                throw new NumberFormatException("String is null, cannot be parsed");
            } else if (s.equals("true") || s.equals("false")) {
                ret = Boolean.valueOf(s);
                returnType = Boolean.class;
            } else {
                try {
                    ret = Byte.valueOf(s);
                    returnType = Byte.class;
                } catch (NumberFormatException ex) {
                    try {
                        ret = Short.valueOf(s);
                        returnType = Short.class;
                    } catch (NumberFormatException ex1) {
                        try {
                            ret = Integer.valueOf(s);
                            returnType = Integer.class;
                        } catch (NumberFormatException ex2) {
                            try {
                                ret = Long.valueOf(s);
                                returnType = Long.class;
                            } catch (NumberFormatException ex3) {
                                try {
                                    ret = Float.valueOf(s);
                                    returnType = Float.class;
                                } catch (NumberFormatException ex4) {
                                    try {
                                        ret = Double.valueOf(s);
                                        returnType = Double.class;
                                    } catch (NumberFormatException ex5) {
                                        throw new NumberFormatException("String '"+s+"' could not be parsed to any primitive number type");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        
        @Override
        public Object apply(List<Object> input) {
            //TODO: show an error message if input is not null or empty?
            return ret;
        }

        @Override
        public Class getReturnType() {
            return returnType;
        }
        
        @Override
        public String toString() {
            return ret.toString();
        }
    }
    
    public static class StringOperation implements Operation {

        private final String s;
        
        public StringOperation(String s) {
            this.s = s;
        }
        
        
        @Override
        public Object apply(List<Object> input) {
            return s;
        }

        @Override
        public Class getReturnType() {
            return String.class;
        }
        
        @Override
        public String toString() {
            return "'"+s+"'";
        }
    }
    
    public static class LookupVariableOperation implements Operation {
        
        private final Object[] computedObjectsTracker;
        private final Class[] resultsTypeTracker;
        private final int index;
        private final String extra;
        /**
         * 
         * @param computedObjectsTracker The array where during execution of the
         * task the variables are stored. Contains the base irtg at index 0,
         * followed by the inputs.
         * @param resultsTypeTracker Here, during type checking the types of the
         * variables are stored. Contains the type of the base irtg at index 0,
         * followed by the inputs.
         * @param index 
         * @param extra //store extra information, currently used to distinguish global from local lookups
         */
        public LookupVariableOperation(Object[] computedObjectsTracker, Class[] resultsTypeTracker, int index, String extra) {
            this.computedObjectsTracker = computedObjectsTracker;
            this.resultsTypeTracker = resultsTypeTracker;
            this.index = index;
            this.extra = extra;
        }
        
        @Override
        public Object apply(List<Object> input) throws IllegalAccessException, InvocationTargetException {
            return computedObjectsTracker[index];
        }

        String getExtra() {
            return extra;
        }

        Class[] getResultsTypeTracker() {
            return resultsTypeTracker;
        }

        int getIndex() {
            return index;
        }
        
        @Override
        public Class getReturnType() {
            return resultsTypeTracker[index];
        }
        
        @Override
        public String toString() {
            return (extra == null)? "lookup_"+index : extra+"_lookup_"+index;
        }
    }
    
    
    public static class NullOperation implements Operation {

        @Override
        public Object apply(List<Object> input) throws IllegalAccessException, InvocationTargetException {
            return null;
        }

        @Override
        public Class getReturnType() {
            return Void.class;
        }
        @Override
        public String toString() {
            return "NULL";
        }
    }
    
    public static Object executeTree(Tree<Operation> tree) throws Throwable {
        try {
            return tree.dfs((Tree<Operation> node, List<Object> childrenValues) -> {
                try {
                    return node.getLabel().apply(childrenValues);//TODO: add null handling there (if a null pointer exception occurs and that is because an input was null, do not throw an error but return null (do that whenever an error occurs?)
                } catch (IllegalAccessException ex) {
                    System.err.println("Error in executing parsing program: "+ex.toString());
                    throw new WrapperException(ex);//TODO: should make shure that this can never occur, i.e. in tests and class checking in the tree before calling this.
                } catch ( InvocationTargetException ex) {
                    throw new WrapperException(ex.getCause());//this covers all errors occuring in executing the code tree
                } catch (InstantiationException ex) {
                    throw new WrapperException(ex);//TODO: should make shure that this can never occur, i.e. in tests and class checking in the tree before calling this --TODO: think about whether this can be caused by a poorly written program
                }
            });
        } catch(WrapperException e) {
            throw e.getCause();
        }
    }
    
    static class WrapperException extends RuntimeException {

        public WrapperException(String message, Throwable cause) {
            super(message, cause);
        }

        public WrapperException(Throwable cause) {
            super(cause);
        }

        public WrapperException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
        
    }
    
    
}
