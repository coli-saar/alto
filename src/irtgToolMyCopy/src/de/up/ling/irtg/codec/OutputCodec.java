/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 *
 * @author koller
 */
public abstract class OutputCodec<E> {
    public abstract void write(E object, OutputStream ostream) throws IOException;
    
    public String asString(E object) throws IOException {
        ByteArrayOutputStream ostream = new ByteArrayOutputStream();
        write(object, ostream);
        return ostream.toString();
    }
    
    private static Iterable<OutputCodec> getAllOutputCodecs() {
        return ServiceLoader.load(OutputCodec.class);
    }

    public static <T> List<OutputCodec<T>> getInputCodecs(Class<T> forClass) {
        List<OutputCodec<T>> ret = new ArrayList<>();

        for (OutputCodec codec : getAllOutputCodecs()) {
            if (forClass.isAssignableFrom(codec.getMetadata().type())) {
                ret.add(codec);
            }
        }

        return ret;
    }
    
    
    public CodecMetadata getMetadata() {
        return (CodecMetadata) getClass().getAnnotation(CodecMetadata.class);
    }
}
