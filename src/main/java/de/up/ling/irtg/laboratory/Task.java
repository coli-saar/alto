/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

import java.util.Objects;

/**
 *
 * @author groschwitz
 */
public class Task {
    
    private final String name;
    private final DBLoader.GrammarReference grammar;
    private final DBLoader.CorpusReference corpus;
    private final Program program;
    private final int iterations;
    private final int warmup;
    
    public Task(String name, DBLoader.GrammarReference grammar, DBLoader.CorpusReference corpus, Program program, int warmup, int iterations) {
        this.name = name;
        this.grammar = grammar;
        this.corpus = corpus;
        this.program = program; // TODO: find out why cloning was necessary before.
        this.iterations = iterations;
        this.warmup = warmup;
    }

    public String getName() {
        return name;
    }

    public DBLoader.GrammarReference getGrammar() {
        return grammar;
    }

    public DBLoader.CorpusReference getCorpus() {
        return corpus;
    }

    public Program getProgram() {
        return program;
    }


    public int getIterations() {
        return iterations;
    }

    public int getWarmup() {
        return warmup;
    }
    
    public void setNumThreads(int threads) {
        program.setNumThreads(threads);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.name);
        hash = 17 * hash + Objects.hashCode(this.grammar);
        hash = 17 * hash + Objects.hashCode(this.corpus);
        hash = 17 * hash + Objects.hashCode(this.program);
        hash = 17 * hash + this.iterations;
        hash = 17 * hash + this.warmup;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Task other = (Task) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.grammar, other.grammar)) {
            return false;
        }
        if (!Objects.equals(this.corpus, other.corpus)) {
            return false;
        }
        if (!Objects.equals(this.program, other.program)) {
            return false;
        }
        if (this.iterations != other.iterations) {
            return false;
        }
        if (this.warmup != other.warmup) {
            return false;
        }
        return true;
    }
    
    
    
    
}
