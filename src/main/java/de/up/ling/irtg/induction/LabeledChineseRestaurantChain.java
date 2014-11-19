/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.induction;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 *
 * @author koller
 */
public class LabeledChineseRestaurantChain<E,K> {
    private Function<E,K> restaurantExtractor;
    private Map<K,LabeledChineseRestaurant<E>> restaurants;
    private double initAlpha, initBeta;

    public LabeledChineseRestaurantChain(Function<E, K> restaurantExtractor, double initAlpha, double initBeta) {
        this.restaurantExtractor = restaurantExtractor;
        restaurants = new HashMap<>();
        this.initAlpha = initAlpha;
        this.initBeta = initBeta;
    }
    
    public LabeledChineseRestaurantChain<E,K> makeEmptyCopy() {
        return new LabeledChineseRestaurantChain<>(restaurantExtractor, initAlpha, initBeta);
    }
    
    public void observeLabel(E label) {
        getOrCreateRestaurantFor(restaurantExtractor.apply(label)).observeLabel(label);
    }
    
    public LabeledChineseRestaurant<E> getRestaurantFor(E label) {
        return getOrCreateRestaurantFor(restaurantExtractor.apply(label));
    }
    
    public void clear() {
        restaurants.values().forEach(x -> x.clear());
    }
    
    private LabeledChineseRestaurant<E> getOrCreateRestaurantFor(K key) {
        LabeledChineseRestaurant<E> ret = restaurants.get(key);
        
        if( ret == null ) {
            ret = createRestaurant(key);
            restaurants.put(key, ret);
        }
        
        return ret;
    }
    
    protected LabeledChineseRestaurant<E> createRestaurant(K key) {
        return new LabeledChineseRestaurant<>(initAlpha, initBeta);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        
        for( K key : restaurants.keySet() ) {
            buf.append("\n\n\nRESTAURANT FOR " + key.toString() + ":\n\n");
            buf.append(restaurants.get(key).toString());
        }
        
        return buf.toString();
    }
    
    
    
    public LabeledChineseRestaurantChain<E,K> plus(LabeledChineseRestaurantChain<E,K> other) {
        return new LabeledChineseRestaurantChain<E,K>(restaurantExtractor, initAlpha, initBeta) {
            @Override
            protected LabeledChineseRestaurant<E> createRestaurant(K key) {
                return LabeledChineseRestaurantChain.this.getOrCreateRestaurantFor(key).plus(other.getOrCreateRestaurantFor(key));
            }
        };      
    }
}