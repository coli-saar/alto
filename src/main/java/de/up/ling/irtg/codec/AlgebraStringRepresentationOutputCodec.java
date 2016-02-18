/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.StringAlgebra;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * A codec that uses an algebra's {@link Algebra#representAsString(java.lang.Object) } method
 * to encode an object as a string. For most algebras, this has the same behavior as the
 * {@link ToStringOutputCodec}; but a few algebras (e.g. {@link StringAlgebra}) overwrite
 * this method. <p>
 * 
 * Because the codec requires an algebra object to be instantiated, we do not add it
 * to the list of registered output codecs, and it will not be returned by
 * {@link OutputCodec#getAllOutputCodecs() } and related methods.
 * 
 * @author koller
 */
@CodecMetadata(name = "text", description = "encodes an object using its algebra's default method", type = Object.class, displayInPopup = false)
public class AlgebraStringRepresentationOutputCodec<E> extends OutputCodec<E> {
    private Algebra<E> algebra;

    public AlgebraStringRepresentationOutputCodec(Algebra<E> algebra) {
        this.algebra = algebra;
    }
    
    @Override
    public void write(E object, OutputStream ostream) throws IOException, UnsupportedOperationException {
        PrintWriter w = new PrintWriter(new OutputStreamWriter(ostream));
        w.write(algebra.representAsString(object));
        w.flush();
    }
}
