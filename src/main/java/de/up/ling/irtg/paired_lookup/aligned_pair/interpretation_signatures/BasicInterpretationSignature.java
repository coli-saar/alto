/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.paired_lookup.aligned_pair.interpretation_signatures;

import de.up.ling.irtg.paired_lookup.aligned_pair.AlignedTree;
import de.up.ling.irtg.paired_lookup.aligned_pair.InterpretationSignature;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author christoph_teichmann
 */
public class BasicInterpretationSignature implements InterpretationSignature {
    /**
     * 
     */
    private final Signature signature = new Signature();

    /**
     * 
     */
    private final AtomicLong num = new AtomicLong(0);
    
    /**
     * 
     */
    private final Object2ObjectMap<Tree<String>,Object2ObjectMap<Tree<String>,String>> labels = 
                                                                            new Object2ObjectOpenHashMap<>();
    
    /**
     * 
     */
    private final Int2ObjectMap<Tree<String>> firstImage = new Int2ObjectOpenHashMap<>();
    
    /**
     * 
     */
    private final Int2ObjectMap<Tree<String>> secondImage = new Int2ObjectOpenHashMap<>();
    
    @Override
    public Signature getUnderlyingSignature() {
        return signature;
    }

    @Override
    public String getLabel(AlignedTree in1, AlignedTree in2) {
        int code = this.getCode(in1, in2);
        
        return this.signature.resolveSymbolId(code);
    }

    @Override
    public int getCode(AlignedTree in1, AlignedTree in2) {
        Tree<String> hom1 = in1.getTree();
        
        Object2ObjectMap<Tree<String>,String> map = this.labels.get(hom1);
        
        if(map == null) {
            map = new Object2ObjectOpenHashMap<>();
            
            this.labels.put(hom1, map);
        }
        
        Tree<String> hom2 = in2.getTree();
        String label = map.get(hom2);
        
        if(label == null) {
            label = this.getSym();
            
            int code = this.signature.addSymbol(label, in1.getNumberVariables());
            this.firstImage.put(code, hom1);
            this.secondImage.put(code, hom2);
            
            return code;
        }
        
        return this.signature.getIdForSymbol(label);
    }

    @Override
    public Tree<String> getFirstHomomorphicImage(int code) {
        return this.firstImage.get(code);
    }

    @Override
    public Tree<String> getSecondHomomorphicImage(int code) {
        return this.secondImage.get(code);
    }

    /**
     * 
     * @return 
     */
    private String getSym() {
        return "l"+this.num.getAndIncrement();
    }
}
