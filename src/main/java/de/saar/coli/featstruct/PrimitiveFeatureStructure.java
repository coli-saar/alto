/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.featstruct;

/**
 *
 * @author koller
 */
public class PrimitiveFeatureStructure<E> implements FeatureStructure {
    private E value;

    public PrimitiveFeatureStructure(E value) {
        this.value = value;
    }

    public E getValue() {
        return value;
    }

    public void setValue(E value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
