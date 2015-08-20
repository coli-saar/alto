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
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
    private final RestrictionManager rm;
    
    /**
     * 
     */
    private final IntSet seenAll1;
    
    /**
     * 
     */
    private final IntSet seenAll2;
    
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
        this.source1 = source1;
        this.source2 = source2;
        this.seen1 = new IntAVLTreeSet();
        this.seen2 = new IntAVLTreeSet();
        this.seenAll1 = new IntAVLTreeSet();
        this.seenAll2 = new IntAVLTreeSet();
        
        this.hom1 = new Homomorphism(sharedSig, source1);
        this.hom2 = new Homomorphism(sharedSig, source2);
        
        this.terminationSequence = null;
        
        this.rm = new RestrictionManager(this.sharedSig);
        
        int symName = this.source1.addSymbol(VARIABLE_PREFIX, 1);
        int sumName = this.source2.addSymbol(VARIABLE_PREFIX, 1);
        this.handleVariable(0, symName);
        this.seenAll1.add(symName);
        this.seenAll2.add(sumName);
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
         
       toDo1.removeAll(this.seenAll1);
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
       
       toDo2.removeAll(this.seenAll2);
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
       
       this.seenAll1.addAll(toDo1);
       this.seenAll2.addAll(toDo2);
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
        
        addMapping(symbols,variables,isJustInsert, 0);
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
        
        this.addMapping(symbols, variables, isJustInsert, 1);
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
    private String addMapping(IntList symbols, IntList variables, BooleanList justVariable,
            int totalVariables) {
        StringBuilder symbol = new StringBuilder();
        
        int varPos = 0;
        for(int i=0;i<symbols.size();++i){
            if(i != 0){
                    symbol.append(" / ");
            }
            
            if(justVariable.getBoolean(i)){
                symbol.append('x').append(variables.getInt(varPos++)+1);
            }else{
                int varNum = (i == 0 ? this.source1 : this.source2).getArity(symbols.getInt(i));
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
        symbol.append(" |").append(totalVariables);
        
        String sym = symbol.toString();
        varPos = 0;
        this.sharedSig.addSymbol(sym, totalVariables);
        
        ObjectArrayList<Tree<String>> storage = new ObjectArrayList<>();
        Tree<String> image1 = null;
        Tree<String> image2 = null;
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
            
            if(image1 == null){
                image1 = t;
            }else{
                image2 = t;
            }
            (i == 0 ? this.hom1 : this.hom2).add(sym, t);
        }
        
        this.rm.addSymbol(sym, image1, image2);
        
        return sym;
    }
    
    /**
     * 
     * @return 
     */
    public TreeAutomaton getRestriction(){
        return this.rm.getRestriction();
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
        
        this.addMapping(symbols, variables, isJustInsert, 1);
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
        
        for(int i=0;i<numVars;++i){
            if(i == 0){
                this.variables.add(varPos, i);
            }else{
                this.variables.set(varPos, i);
            }
            String symbol = this.addMapping(symbols, variables, isJustInsert, numVars);
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
        this.isJustInsert.add(false);
        this.symbols.clear();
        this.symbols.add(lSym);
        this.symbols.add(rSym);
        
        // this set tells us which elements we can pair with elements that do
        // not share a variable
        // note that we need to write no more than less elements into this, since
        // no more will be paired
        IntSortedSet iss = new IntAVLTreeSet();
        
        Iterator<boolean[]> bai;
        if(less == 2){
            bai = new SingletonIterator<>(RE_USE_FOR_SHARED);
        }else{
            bai = new BooleanArrayIterator(less);
        }
        
        int all = (more+less);
        for(int i=more;i<all;++i){
            iss.add(i);
        }
        
        while(bai.hasNext()){
            boolean[] bs = bai.next();
            
            int size = less == 2 ? 2 : findNumberTrue(bs);
            if(size < 2){
                continue;
            }
            
            for (int[] share : new NChooseK(size, more)) {
                int actuallyUsed = more;
                removeAll(iss, share);
                
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
                            this.variables.add(var);
                            ++actuallyUsed;
                        }
                    }
                    
                    this.addMapping(symbols, variables, isJustInsert, actuallyUsed);
                }else{
                    for(int i=0;i<lArity;++i){
                        if(bs[i]){
                            int var = share[posInShared++];
                            this.variables.add(var);
                        }else{
                            int var = ibi.nextInt();
                            ++actuallyUsed;
                            this.variables.add(var);
                        }
                    }
                    
                    for(int i=0;i<rArity;++i){
                        this.variables.add(i);
                    }
                    
                    
                    this.addMapping(symbols, variables, isJustInsert, actuallyUsed);
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
}