/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.condensed.ConcreteCondensedTreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.IncreasingSequencesIterator;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 *
 * @author christoph_teichmann
 */
public class HomomorphismManager {
    
    /**
     * 
     */
    public final static Pattern VARIABLE_PATTERN = Pattern.compile("XX.*");
    
    /**
     * 
     */
    public final static String VARIABLE_PREFIX = "XX.*";
    
    /**
     * 
     */
    private final Signature[] sigs;
    
    /**
     * 
     */
    private final IntSet[] seen;
    
    /**
     * 
     */
    private final Homomorphism[] homs;
    
    /**
     * 
     */
    private final Signature sig = new Signature();
    
    /**
     * 
     */
    private final Integer[] terminate;
    
    /**
     * 
     */
    private final IntList symbols = new IntArrayList();
    
    /**
     * 
     */
    private final IntList variables = new IntArrayList();
    
    /**
     * 
     */
    private final BooleanList isJustInsert = new BooleanArrayList();
    
    /**
     * 
     */
    private final ConcreteCondensedTreeAutomaton<String> restriction;  
    
    /**
     * 
     */
    public static final String START = "S";
    
    /**
     * 
     */
    public static final String GENERAL_STATE_PREFIX = "I";
    
    /**
     * 
     */
    public static final String VARIABLE_STATE = "X";
    
    /**
     * 
     */
    private final ObjectSet<String> seenVariables = new ObjectOpenHashSet<>();
    
    /**
     * 
     * @param sigs 
     */
    public HomomorphismManager(Signature... sigs){
        this.sigs = Arrays.copyOf(sigs, sigs.length);
        this.seen = new IntSet[sigs.length];
        this.homs = new Homomorphism[sigs.length];
        this.terminate = new Integer[sigs.length];
        this.restriction = new ConcreteCondensedTreeAutomaton<>(this.sig);
        
        Arrays.fill(this.terminate, null);
        
        for(int i=0;i<sigs.length;++i){
            this.seen[i] = new IntAVLTreeSet();
            this.homs[i] = new Homomorphism(sig, this.sigs[i]);
        }
        
        this.restriction.addFinalState(this.restriction.addState(START));
    }
    
    /**
     * 
     * @param number
     * @return 
     */
    public Homomorphism getHomomorphism(int number){
        return this.homs[number];
    }
    
    /**
     * 
     * @param toDo
     */
    public void update(IntSet... toDo){
       for(int i=0;i<this.terminate.length;++i){
           Integer k = this.terminate[i];
           if(k == null){
               Integer def = findDefault(this.sigs[i],toDo[i]);
               if(def == null){
                   throw new IllegalStateException("We have no 0-ary symbol for signature "+i);
               }
               
               this.terminate[i] = def;
           }
       }
        
       for(int sigNum=0;sigNum<this.sigs.length;++sigNum){
           toDo[sigNum].removeAll(this.seen[sigNum]);
           
           IntIterator iit = toDo[sigNum].iterator();
           while(iit.hasNext()){
               int symName = iit.nextInt();
               int arity = this.sigs[sigNum].getArity(symName);
               
               if(arity == 0){
                   handle0Ary(sigNum,symName);
               }else if(isVariable(sigNum,symName)){
                   handleVariable(sigNum,symName);
               }
               else{
                   handleSym(sigNum,symName);                  
                   this.seen[sigNum].add(symName);
               }
           }
       }
       
       ensureTermination();
    }

    /**
     * Finds a 0-ary symbol in the given set according to the given signature.
     * 
     * 
     * @param sig
     * @param toDo
     * @return 
     */
    private Integer findDefault(Signature sig, IntSet toDo) {
        IntIterator iit = toDo.iterator();
        
        while(iit.hasNext()){
            int sym = iit.nextInt();
            
            if(sig.getArity(sym) == 0){
                return sym;
            }
        }
        
        return null;
    }

    /**
     * 
     */
    private void ensureTermination() {
        this.symbols.clear();
        this.variables.clear();
        this.isJustInsert.clear();
        
        for(int sym : this.terminate){
            symbols.add(sym);
            isJustInsert.add(false);
        }
        
        String symbol = addMapping(symbols,variables,isJustInsert);
        String[] syms = new String[] {symbol};
        String[] empty = new String[] {};
        
        this.restriction.addRule(this.restriction.createRule(START, syms, empty));
        this.restriction.addRule(this.restriction.createRule(GENERAL_STATE_PREFIX, syms, empty));
        this.restriction.addRule(this.restriction.createRule(VARIABLE_STATE, syms, empty));
        
        IncreasingSequencesIterator isi = new IncreasingSequencesIterator(this.sigs.length);
        while(isi.hasNext()){
            IntList il = isi.next();
            
            String state = makeState(il);
            this.restriction.addRule(this.restriction.createRule(state, syms, empty));
        }
    }

    /**
     * 
     * @param sigNum
     * @param symName 
     */
    private void handle0Ary(int sigNum, int symName) {
        this.isJustInsert.clear();
        this.symbols.clear();
        this.variables.clear();
        
        for(int i=0;i<this.sigs.length;++i){
            if(i == sigNum){
                this.isJustInsert.add(false);
                this.symbols.add(symName);
            }else{
                this.isJustInsert.add(true);
                this.symbols.add(-1);
                this.variables.add(1);
            }
        }
        
        String label = this.addMapping(symbols, variables, isJustInsert);
        String[] tLabel = new String[] {label};
        
        this.restriction.addRule(this.restriction.createRule(GENERAL_STATE_PREFIX, tLabel, new String[] {GENERAL_STATE_PREFIX+"_"+sigNum}));
        this.restriction.addRule(this.restriction.createRule(START, tLabel, new String[] {GENERAL_STATE_PREFIX+"_"+sigNum}));
        this.restriction.addRule(this.restriction.createRule(VARIABLE_STATE, tLabel, new String[] {GENERAL_STATE_PREFIX+"_"+sigNum}));
              
        Iterator<IntArrayList> it = new IncreasingSequencesIterator(this.sigs.length);
        while(it.hasNext()){
            IntArrayList il = it.next();
            // no step like this for the completely terminated state, since we do not want infinite
            // languages if we can avoid them
            if(il.size() == this.sigs.length){
                continue;
            }
            
            String state = makeState(il);
            
            if(extend(il,sigNum)){
                String goal = makeState(il);
                this.restriction.addRule(this.restriction.createRule(state, tLabel,
                        new String[] {goal}));
            
            }
        }
    }

    /**
     * 
     * @param sigNum
     * @param symName 
     */
    private void handleSym(int sigNum, int symName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * 
     * @param symbols
     * @param variables
     * @param justVariable
     * @return the newly created symbol
     */
    private String addMapping(IntList symbols, IntList variables, BooleanList justVariable) {
        StringBuilder symbol = new StringBuilder();
        
        int varPos = 0;
        int max = 0;
        for(int i=0;i<symbols.size();++i){
            if(i != 0){
                    symbol.append(" / ");
            }
            
            if(justVariable.getBoolean(i)){
                symbol.append('?').append(variables.getInt(varPos++)+1);
                max = Math.max(max, 1);
            }else{
                int varNum = this.sigs[i].getArity(symbols.getInt(i));
                max = Math.max(varNum, max);
                String sym  = this.sigs[i].resolveSymbolId(variables.getInt(i));
                
                symbol.append(sym).append('(');
                for(int k=0;k < varNum; ++k){
                    if(k != 0){
                        symbol.append(", ");
                    }
                    
                    symbol.append(variables.getInt(varPos++)+1);
                }
                symbol.append(sym).append(')');
            }
        }
        
        String sym = symbol.toString();
        varPos = 0;
        ObjectArrayList<Tree<String>> storage = new ObjectArrayList<>();
        for(int i=0;i<symbols.size();++i){
            Tree<String> t;
            
            if(justVariable.getBoolean(i)){
                t = Tree.create("?"+(variables.getInt(varPos++)+1));
            }
            else{
                int varNum = this.sigs[i].getArity(this.variables.getInt(i));
                String label = this.sigs[i].resolveSymbolId(this.variables.getInt(i));
                
                for(int k=0;k < varNum; ++k){                    
                    storage.add(Tree.create("?"+(variables.getInt(varPos++)+1)));
                }
                
                t = Tree.create(label, storage);
            }
            
            this.homs[i].add(sym, t);
        }
        
        return sym;
    }
    
    /**
     * 
     * @return 
     */
    public TreeAutomaton getRestriction(){
        return this.restriction;
    }

    /**
     * 
     * @param il
     * @return 
     */
    private String makeState(IntList il) {
        StringBuilder sb = new StringBuilder(GENERAL_STATE_PREFIX);
        
        for(int i=0;i<il.size();++i){
            sb.append("_").append(il.get(i));
        }
        
        return sb.toString();
    }

    /**
     * 
     * @param sigNum
     * @param symName
     * @return 
     */
    private boolean isVariable(int sigNum, int symName) {
        String s = this.sigs[sigNum].resolveSymbolId(symName);
        
        return VARIABLE_PATTERN.matcher(s).matches();
    }

    
    /**
     * 
     * @param sigNum
     * @param symName 
     */
    private void handleVariable(int sigNum, int symName) {
        this.isJustInsert.clear();
        this.symbols.clear();
        this.variables.clear();
        
        String sym = this.sigs[sigNum].resolveSymbolId(symName);
        
        for(Signature sign : this.sigs){
            if(!sign.contains(sym)){
                return;
            }
            
            this.isJustInsert.add(false);
            this.variables.add(1);
            this.symbols.add(sig.getIdForSymbol(sym));
        }
        
        String label = this.addMapping(symbols, variables, isJustInsert);
        this.restriction.addRule(this.restriction.createRule(GENERAL_STATE_PREFIX, new String[] {label},
                                                                        new String[] {VARIABLE_STATE}));
    }

    /**
     * 
     * @param il
     * @param sigNum 
     */
    private boolean extend(IntArrayList il, int sigNum) {
        int pos = IntArrays.binarySearch(il.elements(), 0, il.size(), sigNum);
        
        if(pos < 0){
            pos = -(pos+1);
            il.add(pos, sigNum);
            return true;
        }else{
            return false;
        }
    }
}