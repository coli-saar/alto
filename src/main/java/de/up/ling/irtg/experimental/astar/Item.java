/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.experimental.astar;

import java.util.Objects;

/**
 *
 * @author koller
 */
public class Item implements Comparable<Item> {
    private int start, end, root;
    private Type type;
    private double logProb;
    private double outsideEstimate;
    
    private int operation;
    private Item left, right;

    public Item(int start, int end, int root, Type type, double logProb) {
        this.start = start;
        this.end = end;
        this.root = root;
        this.type = type;
        this.logProb = logProb;
    }
    
    public void setCreatedBySupertag(int operation) {
        this.operation = operation;
        left = null;
        right = null;
    }
    
    public void setCreatedByOperation(int operation, Item functor, Item argument) {
        this.operation = operation;
        this.left = functor;
        this.right = argument;
    }

    public double getOutsideEstimate() {
        return outsideEstimate;
    }

    public void setOutsideEstimate(double outsideEstimate) {
        this.outsideEstimate = outsideEstimate;
    }
    
    public double getScore() {
        return getLogProb() + getOutsideEstimate();
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getRoot() {
        return root;
    }

    public Type getType() {
        return type;
    }

    public double getLogProb() {
        return logProb;
    }

    public int getOperation() {
        return operation;
    }

    public Item getLeft() {
        return left;
    }

    public Item getRight() {
        return right;
    }
    
    

    @Override
    public String toString() {
        return String.format("(%d-%d!%d)%s:%f (est=%f)", start, end, root, type, logProb, getScore());
    }

    @Override
    public int compareTo(Item o) {
        return - Double.compare(getScore(), o.getScore()); // sort in descending order
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + this.start;
        hash = 79 * hash + this.end;
        hash = 79 * hash + this.root;
        hash = 79 * hash + Objects.hashCode(this.type);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Item other = (Item) obj;
        if (this.start != other.start) {
            return false;
        }
        if (this.end != other.end) {
            return false;
        }
        if (this.root != other.root) {
            return false;
        }
        return Objects.equals(this.type, other.type);
    }
    
    
}
