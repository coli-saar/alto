/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

/**
 *
 * @author Jonas
 */
public abstract class ObjectWithStringCode {
    
    abstract public String getCode();

    //not sure if this is a good idea
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj.getClass().equals(getClass()))) {
            return false;
        }
        ObjectWithStringCode objWSC = (ObjectWithStringCode) obj;
        return getCode().equals(objWSC.getCode());
    }

    @Override
    public int hashCode() {
        return getCode().hashCode();
    }
    
    
    
    //how do i formulate this if ObjectWithStringCode is an interface?
    public static <T extends ObjectWithStringCode> T  getObjectWithCode(Iterable<T> iterable, String code) {
        T ret = null;
        for (T candidate : iterable) {
            if (candidate.getCode().equals(code)) {
                ret = candidate;
            }
        }
        return ret;
    }
    
}
