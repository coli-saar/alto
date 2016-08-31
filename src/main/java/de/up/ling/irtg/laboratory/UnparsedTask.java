/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.laboratory;

/**
 *
 * @author koller
 */
class UnparsedTask {
    // v = {"id":task.id, "name":task.name, "grammar":task.grammar_id, "corpus":task.corpus_id, "tree":task.tree, "warmup":task.warmup, "iterations":task.iterations}
    public int id;
    public String name;
    public int grammar;
    public int corpus;
    public String tree;
    public int warmup;
    public int iterations;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getGrammar() {
        return grammar;
    }

    public int getCorpus() {
        return corpus;
    }

    public String getTree() {
        return tree;
    }

    public int getWarmup() {
        return warmup;
    }

    public int getIterations() {
        return iterations;
    }
    
    

    @Override
    public String toString() {
        return "JacksonTask{" + "id=" + id + ", name=" + name + ", grammar=" + grammar + ", corpus=" + corpus + ", tree=" + tree + ", warmup=" + warmup + ", iterations=" + iterations + '}';
    }
    
}
