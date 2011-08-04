/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.algebra.lambda;

import de.saar.basic.Pair;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author koller
 */
public class LambdaTerm {
    public static enum Type {
        CONSTANT, VARIABLE, LAMBDA, APPLY, EXISTS, CONJ, ARGMAX, ARGMIN
    };
    private Type type;
    private List<LambdaTerm> sub;
    private String x = null;

    private LambdaTerm(Type type) {
        this.type = type;
    }

    public static LambdaTerm constant(String x) {
        LambdaTerm ret = new LambdaTerm(Type.CONSTANT);
        ret.x = x;
        return ret;
    }

    public static LambdaTerm variable(String x) {
        LambdaTerm ret = new LambdaTerm(Type.VARIABLE);
        ret.x = x;
        return ret;
    }

    public static LambdaTerm lambda(String x, LambdaTerm sub) {
        LambdaTerm ret = new LambdaTerm(Type.LAMBDA);
        ret.x = x;
        ret.sub = new ArrayList<LambdaTerm>();
        ret.sub.add(sub);
        return ret;
    }

    public static LambdaTerm apply(LambdaTerm functor, LambdaTerm argument) {
        LambdaTerm ret = new LambdaTerm(Type.APPLY);
        ret.sub = new ArrayList<LambdaTerm>();
        ret.sub.add(functor);
        ret.sub.add(argument);
        return ret;
    }

    public static LambdaTerm exists(String x, LambdaTerm sub) {
        LambdaTerm ret = new LambdaTerm(Type.EXISTS);
        ret.x = x;
        ret.sub = new ArrayList<LambdaTerm>();
        ret.sub.add(sub);
        return ret;
    }

    public static LambdaTerm conj(List<LambdaTerm> subs) {
        LambdaTerm ret = new LambdaTerm(Type.CONJ);
        ret.sub = new ArrayList<LambdaTerm>(subs);
        return ret;
    }

    public static LambdaTerm argmax(String x, LambdaTerm sub) {
        LambdaTerm ret = new LambdaTerm(Type.ARGMAX);
        ret.x = x;
        ret.sub = new ArrayList<LambdaTerm>();
        ret.sub.add(sub);
        return ret;
    }

    public static LambdaTerm argmin(String x, LambdaTerm sub) {
        LambdaTerm ret = new LambdaTerm(Type.ARGMIN);
        ret.x = x;
        ret.sub = new ArrayList<LambdaTerm>();
        ret.sub.add(sub);
        return ret;
    }

    public Pair<LambdaTerm, LambdaTerm> split(String top, String bottom) {




        return null;
    }

    @Override
    public String toString() {
        return type + (x == null ? "" : ("." + x)) + (sub == null ? "" : ("." + sub.toString()));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LambdaTerm other = (LambdaTerm) obj;
        if (this.type != other.type) {
            return false;
        }
        if (this.sub != other.sub && (this.sub == null || !this.sub.equals(other.sub))) {
            return false;
        }
        if ((this.x == null) ? (other.x != null) : !this.x.equals(other.x)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 59 * hash + (this.sub != null ? this.sub.hashCode() : 0);
        hash = 59 * hash + (this.x != null ? this.x.hashCode() : 0);
        return hash;
    }
    
    
}
