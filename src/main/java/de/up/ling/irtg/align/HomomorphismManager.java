/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
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
    private final Signature source1;
    
    /**
     * 
     */
    private final Signature source2;
    
    /**
     * 
     */
    private final IntSet seen1;
    
    /**
     * 
     */
    private final IntSet seen2;
    
    /**
     * 
     */
    private final Homomorphism hom1;
    
    /**
     * 
     */
    private final Homomorphism hom2;

    /**
     * 
     */
    private final Signature sharedSig;
    
    /**
     * 
     */
    private int[] terminationSequence;

    /**
     * 
     */
    private final IntList symbols;

    /**
     * 
     */
    private final IntList variables;

    /**
     * 
     */
    private final BooleanList isJustInsert;
    
    /**
     * 
     */
    private final ConcreteTreeAutomaton<RestrictionState> restriction;  

    /**
     * 
     */
    private final ObjectSet<String> seenVariables;
    
    /**
     * 
     * @param source1
     * @param source2 
     */
    public HomomorphismManager(Signature source1, Signature source2){
        this.symbols = new IntArrayList();
        this.isJustInsert = new BooleanArrayList();
        this.variables = new IntArrayList();
        this.sharedSig = new Signature();
        this.seenVariables = new ObjectOpenHashSet<>();
        this.source1 = source1;
        this.source2 = source2;
        this.seen1 = new IntOpenHashSet();
        this.seen2 = new IntOpenHashSet();
        this.hom1 = new Homomorphism(sharedSig, source1);
        this.hom2 = new Homomorphism(sharedSig, source2);
        this.terminationSequence = null;
        this.restriction = new ConcreteTreeAutomaton<>(this.sharedSig);
        
        this.restriction.addFinalState(this.restriction.addState(RestrictionState.START));
    }
    
    /**
     * 
     * @return 
     */
    public Homomorphism getHomomorphism1(){
        return this.hom1;
    }
    
    /**
     * 
     * @return 
     */
    public Homomorphism getHomomorphism2(){
        return this.hom2;
    }
    
    /**
     * 
     * @param toDo1
     * @param toDo2 
     */
    public void update(IntSet toDo1, IntSet toDo2){
       if(this.terminationSequence == null){
           this.terminationSequence = new int[2];
           
           Integer def = findDefault(this.source1,toDo1);
           if(def == null){
                throw new IllegalStateException("We have no 0-ary symbol for signature "+1);
           }
           this.terminationSequence[0] = def;
           
           def = findDefault(this.source2,toDo2);
           if(def == null){
                throw new IllegalStateException("We have no 0-ary symbol for signature "+2);
           }
           this.terminationSequence[1] = def;
       }
         
       toDo1.removeAll(this.seen1);
       IntIterator iit = toDo1.iterator();
       while(iit.hasNext()){
           int symName = iit.nextInt();
           int arity = this.source1.getArity(symName);
           
            if(arity == 0){
                handle0Ary(0,symName);
            }else if(isVariable(0,symName)){
                handleVariable(0,symName);
            }
            else{
                handleSym(0,symName);                  
                this.seen1.add(symName);
            }
       }
       
       toDo2.removeAll(this.seen2);
       iit = toDo2.iterator();
       while(iit.hasNext()){
           int symName = iit.nextInt();
           int arity = this.source2.getArity(symName);
           
            if(arity == 0){
                handle0Ary(1,symName);
            }else if(isVariable(1,symName)){
                handleVariable(1,symName);
            }
            else{
                handleSym(1,symName);                  
                this.seen2.add(symName);
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
        
        for(int sym : this.terminationSequence){
            symbols.add(sym);
            isJustInsert.add(false);
        }
        
        String symbol = addMapping(symbols,variables,isJustInsert);
        RestrictionState[] empty = new RestrictionState[] {};
        
        this.restriction.addRule(this.restriction.createRule(RestrictionState.TERMINATION, symbol, empty));
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
        
        for(int i=0;i<2;++i){
            if(i == sigNum){
                this.isJustInsert.add(false);
                this.symbols.add(symName);
            }else{
                this.isJustInsert.add(true);
                this.symbols.add(-1);
                this.variables.add(0);
            }
        }
        
        String label = this.addMapping(symbols, variables, isJustInsert);
        
        RestrictionState[] rhs = new RestrictionState[1];
        
        if(sigNum == 0){
            rhs[0] = RestrictionState.TERMINATION;
            RestrictionState lhs = RestrictionState.getByDescription(false, 0);
            this.restriction.addRule(this.restriction.createRule(lhs, label, rhs));
            
            rhs[0] = RestrictionState.getByDescription(false, 1);
            lhs = RestrictionState.getByDescription(false, 2);
            this.restriction.addRule(this.restriction.createRule(lhs, label, rhs));
            
            rhs[0] = RestrictionState.getByDescription(false, 0);
            lhs = RestrictionState.START;
            this.restriction.addRule(this.restriction.createRule(lhs, label, rhs));
        }else{
            rhs[1] = RestrictionState.TERMINATION;
            RestrictionState lhs = RestrictionState.getByDescription(false, 1);
            this.restriction.addRule(this.restriction.createRule(lhs, label, rhs));
        }
    }

    /**
     * 
     * @param sigNum
     * @param symName 
     */
    private void handleSym(int sigNum, int symName) {
        //TODO
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
                int varNum = (i == 0 ? this.source1 : this.source2).getArity(symbols.getInt(i));
                max = Math.max(varNum, max);
                String sym  = (i == 0 ? this.source1 : this.source2).resolveSymbolId(variables.getInt(i));
                
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
                int varNum = (i == 0 ? this.source1 : this.source2).getArity(this.variables.getInt(i));
                String label = (i == 0 ? this.source1 : this.source2).resolveSymbolId(this.variables.getInt(i));
                
                for(int k=0;k < varNum; ++k){
                    storage.add(Tree.create("?"+(variables.getInt(varPos++)+1)));
                }
                
                t = Tree.create(label, storage);
            }
            
            (i == 0 ? this.hom1 : this.hom2).add(sym, t);
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
     * @param sigNum
     * @param symName
     * @return 
     */
    private boolean isVariable(int sigNum, int symName) {
        String s = (sigNum == 0 ? this.source1 : this.source2).resolveSymbolId(symName);
        
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
        
        String sym = (sigNum == 0 ? this.source1 : this.source2).resolveSymbolId(symName);
        
        if(!source1.contains(sym) || !source2.contains(sym)){
            return;
        }
        
        this.isJustInsert.add(false);
        this.isJustInsert.add(false);
        this.variables.add(0);
        this.symbols.add(source1.getIdForSymbol(sym));
        this.symbols.add(source2.getIdForSymbol(sym));
        
        String label = this.addMapping(symbols, variables, isJustInsert);
        RestrictionState[] rhs = new RestrictionState[1];
        
        RestrictionState lhs = RestrictionState.getByDescription(true, 0);
        rhs[0] = RestrictionState.START;
        this.restriction.addRule(this.restriction.createRule(lhs, label, rhs));
        
        lhs = RestrictionState.getByDescription(true, 1);
        this.restriction.addRule(this.restriction.createRule(lhs, label, rhs));
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
    
    public CondensedTreeAutomaton<RestrictionState> getCondensedRestriction(){
        //TODO
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    
    /**
      * 
      */
     public static enum RestrictionState{
        START,
        HAS_VARIABLE_BOTH,
        HAS_VARIABLE_LEFT,
        HAS_VARIABLE_RIGHT,
        NO_VARIABLE_BOTH,
        NO_VARIABLE_LEFT,
        NO_VARIABLE_RIGHT,
        TERMINATION;
        
        /**
         * 
         * @param hasVariable
         * @param leftRightBoth
         * @return 
         */
        public static RestrictionState getByDescription(boolean hasVariable, int leftRightBoth){
            switch(leftRightBoth){
                case 0:
                    return hasVariable ? HAS_VARIABLE_LEFT : NO_VARIABLE_LEFT;
                case 1:
                    return hasVariable ? HAS_VARIABLE_RIGHT : NO_VARIABLE_RIGHT;
                default:
                    return hasVariable ? HAS_VARIABLE_BOTH : NO_VARIABLE_BOTH;
            }
        }
    }
}