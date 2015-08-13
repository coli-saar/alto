/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.condensed.ConcreteCondensedTreeAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.BooleanArrayIterator;
import de.up.ling.irtg.util.NChooseK;
import de.up.ling.irtg.util.SingletonIterator;
import de.up.ling.irtg.util.TupleIterator;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 *
 * @author christoph_teichmann
 */
public class HomomorphismManager {
    /**
     * 
     */
    private static final boolean[] RE_USE_FOR_SHARED = new boolean[] {true,true};
    
    /**
     * 
     */
    public final static Predicate<String> VARIABLE_PATTERN = Pattern.compile("^XX.*$").asPredicate();  
    
    /**
     * 
     */
    public final static String VARIABLE_PREFIX = "XX";
    
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
        this.sharedSig.addSymbol(VARIABLE_PREFIX, 1);
        this.hom1.add(VARIABLE_PREFIX, Tree.create(VARIABLE_PREFIX, Tree.create("?1")));
        
        this.hom2 = new Homomorphism(sharedSig, source2);
        this.hom2.add(VARIABLE_PREFIX, Tree.create(VARIABLE_PREFIX, Tree.create("?1")));
        
        this.seenVariables.add(VARIABLE_PREFIX);
        
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
            rhs[0] = RestrictionState.TERMINATION;
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
        makeSingular(sigNum,symName);
        
        IntIterator iit = (sigNum == 0 ? this.seen2 : this.seen1).iterator();
        
       
        while(iit.hasNext()){
            int other = iit.nextInt();
            int lSym = (sigNum == 0 ? symName : other);
            int rSym = (sigNum == 1 ? symName : other);
            
            makePairing(lSym,rSym);
        }
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
                symbol.append('x').append(variables.getInt(varPos++)+1);
                max = Math.max(max, 1);
            }else{
                int varNum = (i == 0 ? this.source1 : this.source2).getArity(symbols.getInt(i));
                max = Math.max(varNum, max);
                String sym  = (i == 0 ? this.source1 : this.source2).resolveSymbolId(symbols.getInt(i));
                
                if(sym == null){
                    throw new IllegalArgumentException("symbol must be known to given signatures");
                }
                
                symbol.append(sym).append('(');
                for(int k=0;k < varNum; ++k){
                    if(k != 0){
                        symbol.append(", ");
                    }
                    
                    symbol.append("x").append(variables.getInt(varPos++)+1);
                }
                symbol.append(')');
            }
        }
        
        String sym = symbol.toString();
        varPos = 0;
        this.sharedSig.addSymbol(sym, max);
        ObjectArrayList<Tree<String>> storage = new ObjectArrayList<>();
        for(int i=0;i<symbols.size();++i){
            storage.clear();
            Tree<String> t;
            
            if(justVariable.getBoolean(i)){
                t = Tree.create("?"+(variables.getInt(varPos++)+1));
            }
            else{
                int varNum = (i == 0 ? this.source1 : this.source2).getArity(this.symbols.getInt(i));
                String label = (i == 0 ? this.source1 : this.source2).resolveSymbolId(this.symbols.getInt(i));
                
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
        
        return VARIABLE_PATTERN.test(s);
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
     * @return 
     */
    public CondensedTreeAutomaton<RestrictionState> getCondensedRestriction(){
        return ConcreteCondensedTreeAutomaton.fromTreeAutomaton(restriction);
    }

    /**
     * 
     * @param sigNum
     * @param symName 
     */
    private void makeSingular(int sigNum, int symName) {
        int numVars = (sigNum == 0 ? this.source1.getArity(symName) : this.source2.getArity(symName));
        
        this.isJustInsert.clear();
        this.symbols.clear();
        this.variables.clear();
        int varPos = -1;
        
        for(int i=0;i<2;++i){
            if(i == sigNum){
                this.isJustInsert.add(false);
                this.symbols.add(symName);
                for(int k=0;k<numVars;++k){
                    this.variables.add(k);
                }
            }else{
                this.isJustInsert.add(true);
                this.symbols.add(-1);
                varPos = this.variables.size();
            }
        }
        
        RestrictionState lhs;
        RestrictionState[] rhs = new RestrictionState[numVars];
        RestrictionState filler = (sigNum == 0 ? RestrictionState.getByDescription(false, 0) : RestrictionState.getByDescription(false, 0));
        Arrays.fill(rhs, filler);
        
        for(int i=0;i<numVars;++i){
            this.variables.add(varPos, i);
            String symbol = this.addMapping(symbols, variables, isJustInsert);
            
            if(sigNum == 0){
                lhs = RestrictionState.START;
                rhs[i] = RestrictionState.getByDescription(true, 2);                
                this.restriction.addRule(this.restriction.createRule(lhs, symbol, rhs));
                
               rhs[i] = RestrictionState.getByDescription(true, 1);
               this.restriction.addRule(this.restriction.createRule(lhs, symbol, rhs));
               
               lhs = RestrictionState.getByDescription(true, 2);
               rhs[i] = RestrictionState.getByDescription(true, 2);
               this.restriction.addRule(this.restriction.createRule(lhs, symbol, rhs));
               
               rhs[i] = RestrictionState.getByDescription(true, 1);
               this.restriction.addRule(this.restriction.createRule(lhs, symbol, rhs));
               
               lhs = RestrictionState.getByDescription(true, 0);
               rhs[i] = RestrictionState.getByDescription(true, 0);
               this.restriction.addRule(this.restriction.createRule(lhs, symbol, rhs));
               if(i == 0){
                   lhs = RestrictionState.getByDescription(false, 2);
                   rhs[0] = RestrictionState.getByDescription(false, 2);
                   this.restriction.addRule(this.restriction.createRule(lhs, symbol, rhs));
                   
                   lhs = RestrictionState.START;
                   this.restriction.addRule(this.restriction.createRule(lhs, symbol, rhs));
                   
                   lhs = RestrictionState.getByDescription(false, 2);
                   rhs[0] = RestrictionState.getByDescription(false, 1);
                   this.restriction.addRule(this.restriction.createRule(lhs, symbol, rhs));
                   
                   lhs = RestrictionState.START;
                   this.restriction.addRule(this.restriction.createRule(lhs, symbol, rhs));
                   
                   lhs = RestrictionState.getByDescription(false, 0);
                   rhs[0] = RestrictionState.getByDescription(false, 0);
                   this.restriction.addRule(this.restriction.createRule(lhs, symbol, rhs));
               }
            }else{
                lhs = RestrictionState.getByDescription(true, 1);
                rhs[i] = RestrictionState.getByDescription(true, 1);
                this.restriction.addRule(this.restriction.createRule(lhs, symbol, rhs));
                
                if(i == 0){
                    lhs = RestrictionState.getByDescription(false, 1);
                    rhs[i] = RestrictionState.getByDescription(false, 1);
                    this.restriction.addRule(this.restriction.createRule(lhs, symbol, rhs));
                }
            }
            
            rhs[i] = filler;
        }
    }

    /**
     * 
     * @param lSym
     * @param rSym 
     */
    private void makePairing(int lSym, int rSym) {
        int lArity = this.source1.getArity(lSym);
        int rArity = this.source2.getArity(rSym);
        
        int less = Math.min(lArity, rArity);
        if(less == 1){
            return;
        }
        int more = Math.max(lArity, rArity);
        
        this.isJustInsert.clear();
        this.isJustInsert.add(false);
        this.isJustInsert.add(true);
        this.symbols.clear();
        this.symbols.add(lSym);
        this.symbols.add(rSym);
        
        // this set tells us which elements we can pair with elements that do
        // not share a variable
        // note that we need to write no more than less elements into this, since
        // no more will be paired
        IntSortedSet iss = new IntAVLTreeSet();
        for(int i=0;i<less;++i){
            iss.add(i);
        }
        
        Set<RestrictionState>[] settings = new Set[more];
        RestrictionState[] container = new RestrictionState[more];
        for(int i=0;i<more;++i){
            settings[i] =new ObjectAVLTreeSet<>();
        }
        
        Iterator<boolean[]> bai;
        if(less == 2){
            bai = new SingletonIterator<>(RE_USE_FOR_SHARED);
        }else{
            bai = new BooleanArrayIterator(less);
        }
        
        IntSet used = new IntRBTreeSet();
        
        while(bai.hasNext()){            
            boolean[] bs = bai.next();
            
            int size = less == 2 ? 2 : findNumberTrue(bs);
            if(size < 2){
                continue;
            }
            
            for (int[] share : new NChooseK(size, more)) {
                removeAll(iss, share);
                used.clear();
                
                IntIterator ibi = iss.iterator();
                this.variables.clear();
                int posInShared = 0;
                
                if(less == rArity){
                    for(int i=0;i<lArity;++i){
                        this.variables.add(i);
                    }
                    
                    for(int i=0;i<rArity;++i){
                        if(bs[i]){
                            int var = share[posInShared++];
                            this.variables.add(var);
                        }else{
                            int var = ibi.nextInt();
                            used.add(var);
                            this.variables.add(var);
                        }
                    }
                    
                    String label = this.addMapping(symbols, variables, isJustInsert);
                    Arrays.sort(share);
                    
                    for(int i=0;i<more;++i){
                        Set<RestrictionState> setting = settings[i];
                        setting.clear();
                        if(Arrays.binarySearch(share, i) >= 0){
                            for(int side=0;side<=2;++side){
                                setting.add(RestrictionState.getByDescription(true, side));
                            }
                        }else{
                            int side = (used.contains(i) ? 2 : 0);
                            setting.add(RestrictionState.getByDescription(false, side));
                        }
                    }
                    
                    TupleIterator<RestrictionState> states = new TupleIterator<>(settings,container);
                    while(states.hasNext()){
                        RestrictionState[] rhs = states.next();
                        RestrictionState lhs = RestrictionState.START;
                        this.restriction.addRule(this.restriction.createRule(lhs, label, rhs));
                        
                        lhs = RestrictionState.getByDescription(true, 0);
                        this.restriction.addRule(this.restriction.createRule(lhs, label, rhs));
                        
                        lhs = RestrictionState.getByDescription(true, 1);
                        this.restriction.addRule(this.restriction.createRule(lhs, label, rhs));
                    }
                }else{
                    for(int i=0;i<lArity;++i){
                        if(bs[i]){
                            int var = share[posInShared++];
                            this.variables.add(var);
                        }else{
                            int var = ibi.nextInt();
                            used.add(var);
                            this.variables.add(var);
                        }
                    }
                    
                    for(int i=0;i<rArity;++i){
                        this.variables.add(i);
                    }
                    
                    
                    String label = this.addMapping(symbols, variables, isJustInsert);
                    Arrays.sort(share);
                    
                    for(int i=0;i<more;++i){
                        Set<RestrictionState> setting = settings[i];
                        setting.clear();
                        if(Arrays.binarySearch(share, i) >= 0){
                            for(int side=0;side<=2;++side){
                                setting.add(RestrictionState.getByDescription(true, side));
                            }
                        }else{
                            if(used.contains(i)){
                                setting.add(RestrictionState.getByDescription(false, 2));
                            }else{
                                setting.add(RestrictionState.getByDescription(false, 1));
                            }
                        }
                    }
                    
                    TupleIterator<RestrictionState> states = new TupleIterator<>(settings,container);
                    while(states.hasNext()){
                        RestrictionState[] rhs = states.next();
                        RestrictionState lhs = RestrictionState.START;
                        this.restriction.addRule(this.restriction.createRule(lhs, label, rhs));
                        
                        lhs = RestrictionState.getByDescription(true, 0);
                        this.restriction.addRule(this.restriction.createRule(lhs, label, rhs));
                        
                        lhs = RestrictionState.getByDescription(true, 1);
                        this.restriction.addRule(this.restriction.createRule(lhs, label, rhs));
                    }
                }
                    
                    
                addAll(iss, share);
            }
        }
    }

    /**
     * 
     * @param s
     * @param share 
     */
    private void removeAll(IntSortedSet s, int[] share) {
        for(int i : share){
            s.rem(i);
        }
    }

    /**
     * 
     * @param s
     * @param share 
     */
    private void addAll(IntSortedSet s, int[] share) {
        for(int i : share){
            s.add(i);
        }
    }

    /**
     * 
     * @param bs
     * @return 
     */
    private int findNumberTrue(boolean[] bs) {
        int ret = 0;
        for(boolean b : bs){
            ret += b ? 1 : 0;
        }
        return ret;
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