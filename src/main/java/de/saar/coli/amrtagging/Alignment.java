/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtagging;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/**
 *
 * @author Jonas
 */
public class Alignment {
    
    public final Set<String> nodes;
    public Span span;
    public final Set<String> lexNodes;
    public final int color;
    private double weight;

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
    
    public Alignment(Set<String> nodes, Span span, Set<String> lexNodes, int color, double weight) {
        this.nodes = new HashSet(nodes);//such that if nodes is an abstract collection, we can still add something later.
        this.span = span;
        this.lexNodes = new HashSet(lexNodes);//such that if lexNodes is an abstract collection, we can still add something later.
        this.color = color;
        this.weight = weight;
    }
    
    public Alignment(Set<String> nodes, Span span, Set<String> lexNodes, int color) {
        this(nodes, span, lexNodes, color, 1.0);
    }
    
    public Alignment(Set<String> nodes, Span span) {
        this(nodes, span, Collections.EMPTY_SET, 0);
    }
    
    /**
     * Also adds nn to the lexNodes.
     * @param nn
     * @param index 
     */
    public Alignment(String nn, int index) {
        this(Collections.singleton(nn), new Span(index, index+1), Collections.singleton(nn), 0);
    }
    
    @Override
    public String toString() {
        StringJoiner sjN = new StringJoiner("|");
        for (String nn : nodes) {
            if (lexNodes.contains(nn)) {
                sjN.add(nn+"!");
            } else {
                sjN.add(nn);
            }
        }
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(6);
        df.setMinimumFractionDigits(1);
        return sjN.toString() + "||" + span.toString() + "||" + df.format(getWeight());
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + Objects.hashCode(this.nodes);
        hash = 79 * hash + Objects.hashCode(this.span);
        hash = 79 * hash + Objects.hashCode(this.lexNodes);
        hash = 79 * hash + this.color;
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
        final Alignment other = (Alignment) obj;
        if (this.color != other.color) {
            return false;
        }
        if (!Objects.equals(this.nodes, other.nodes)) {
            return false;
        }
        if (!Objects.equals(this.span, other.span)) {
            return false;
        }
        if (!Objects.equals(this.lexNodes, other.lexNodes)) {
            return false;
        }
        return true;
    }
    
    public boolean equalsBasic(Alignment other) {
        return nodes.equals(other.nodes) && span.equals(other.span);
    }
    
    /**
     * adds all nodes, indices and lexNodes of other into this alignment. (keeps color
     * of this alignment).
     * @param other 
     */
    public void mergeIntoThis(Alignment other) {
        nodes.addAll(other.nodes);
        
        span = span.merge(other.span);
        lexNodes.addAll(other.lexNodes);
    }
    
    
    public static Alignment read(String input, int color) {
        if (!input.contains("||")) {
            return null;
        }
        String[] nodesAndIndeces = input.split("\\|\\|");
        Set<String> nodes = new HashSet<>();
        Set<String> lexNodes = new HashSet<>();
        for (String nn : nodesAndIndeces[0].split("\\|")) {
            if (nn.endsWith("!")) {
                nn = nn.substring(0, nn.length()-1);
                lexNodes.add(nn);
            }
            nodes.add(nn);
        }
        Span span;
        if (nodesAndIndeces[1].contains("-")) {
            String[] spanIndices = nodesAndIndeces[1].split("-");
            span = new Span(Integer.valueOf(spanIndices[0]), Integer.valueOf(spanIndices[1]));
        } else {
            int index = Integer.valueOf(nodesAndIndeces[1]);
            span = new Span(index, index+1);
        }
        double weight;
        if (nodesAndIndeces.length >= 3) {
            weight = Double.valueOf(nodesAndIndeces[2]);
        } else {
            weight = 1.0;
        }
        return new Alignment(nodes, span, lexNodes, color, weight);
    }
    
    public static Alignment read(String input) {
        return read(input, 0);
    }
    
    /**
     * A span of indices, including start, excluding end.
     */
    public static class Span {
        
        public final int start;
        public final int end;
        
        /**
         * A span of indices, including start, excluding end.
         * @param start
         * @param end
         */
        public Span(int start, int end) {
            if (start >= end) {
                System.err.println("invalid values "+start+"-"+end+" for span; fixing to "+start+"-"+(start+1)+".");
                end = start+1;
            }
            this.start = start;
            this.end = end;
        }
        
        public Span(String rep) {
            String[] parts = rep.split("-");
            this.start = Integer.valueOf(parts[0]);
            this.end = Integer.valueOf(parts[1]);
        }
        
        @Override
        public String toString() {
            return start+"-"+end;
        }
        
        public boolean overlapsOrTouches(Span other) {
            return end >= other.start && start <= other.end; 
        }
        
        public boolean overlaps(Span other) {
            return end > other.start && start < other.end; 
        }
        
        public Span merge(Span other) {
            if (!overlapsOrTouches(other)) {
                System.err.println("Warning: merging non-touching spans "+this.toString() +" and "+other.toString()+".");
            }
            return new Span(Math.min(start, other.start), Math.max(end, other.end));
        }
        
        public boolean isSingleton() {
            return end == start+1;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 79 * hash + this.start;
            hash = 79 * hash + this.end;
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
            final Span other = (Span) obj;
            if (this.start != other.start) {
                return false;
            }
            if (this.end != other.end) {
                return false;
            }
            return true;
        }
        
        
        
    }
    
}
