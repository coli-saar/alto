/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.signature;

/**
 * This is a special mapper for a single interner, which always maps a symbol
 * to itself.
 * 
 * @author koller
 */
public class IdentitySignatureMapper extends SignatureMapper {
    
    /**
     * Creates a new instance for the given interner.
     * @param interner 
     */
    public IdentitySignatureMapper(Interner interner) {
        forward = null;
        backward = null;
        input = interner;
        output = interner;
    }

//    public IdentitySignatureMapper(Signature signature) {
//        this(signature.getInterner());
//    }
    
    @Override
    public void recompute() {
        // NOP
    }

    @Override
    public int remapBackward(int symbolID) {
        return symbolID;
    }

    @Override
    public int remapForward(int symbolID) {
        return symbolID;
    }

    @Override
    public String toString() {
        return "identity mapping for " + input.toString();
    }
}
