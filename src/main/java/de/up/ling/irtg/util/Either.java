/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.util;

/**
 *
 * @author koller
 */
public class Either<A,B> {
    private boolean isFirst;
    private A first;
    private B second;

    public static <A,B> Either<A,B> makeFirst(A first) {
        Either<A,B> ret = new Either<>();
        ret.first = first;
        ret.second = null;
        ret.isFirst = true;
        return ret;
    }

    public static <A,B> Either<A,B> makeSecond(B second) {
        Either<A,B> ret = new Either<>();
        ret.first = null;
        ret.second = second;
        ret.isFirst = false;
        return ret;
    }

    public boolean isFirst() {
        return isFirst;
    }

    public A asFirst() {
        return first;
    }

    public B asSecond() {
        return second;
    }
    
    private Object get() {
        return isFirst ? first : second;
    }

    @Override
    public String toString() {
        if(isFirst) {
            return "<" + first.toString();
        } else {
            return ">" + second.toString();
        }
    }

    @Override
    public int hashCode() {
        return (isFirst ? 7 : 13) + get().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        
        final Either<A,B> other = (Either<A,B>) obj;
        
        if( other.isFirst != isFirst ) {
            return false;
        } else if( other.get().getClass() != this.get().getClass() ) {
            return false;
        } else {
            return other.get().equals(this.get());
        }
    }
    
    
}