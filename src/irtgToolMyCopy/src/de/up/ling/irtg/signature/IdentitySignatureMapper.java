/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.signature;

/**
 *
 * @author koller
 */
public class IdentitySignatureMapper extends SignatureMapper {
    public IdentitySignatureMapper(Signature signature) {
        forward = null;
        backward = null;
        input = signature;
        output = signature;
    }

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
