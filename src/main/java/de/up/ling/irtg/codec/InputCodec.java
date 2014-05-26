/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 *
 * @author koller
 */
public abstract class InputCodec<E> {
    public abstract E read(InputStream is) throws ParseException, IOException;
    
    public E read(String s) throws ParseException, IOException {
        return read(new ByteArrayInputStream(s.getBytes()));
    }
    
    public CodecMetadata getMetadata() {
        return (CodecMetadata) getClass().getAnnotation(CodecMetadata.class);
    }
    
    public static <T> List<InputCodec<T>> getInputCodecs(Class<T> forClass) {
        List<InputCodec<T>> ret = new ArrayList<>();
        
        ServiceLoader<InputCodec> serviceLoader = ServiceLoader.load(InputCodec.class);
        for( InputCodec codec : serviceLoader ) {            
            if( forClass.isAssignableFrom(codec.getMetadata().type()) ) {
                ret.add(codec);
            }
        }
        
        return ret;
    }
    
    public static void main(String[] args) {
        List<InputCodec<InterpretedTreeAutomaton>> irtgCodecs = InputCodec.getInputCodecs(InterpretedTreeAutomaton.class);
        for( InputCodec i : irtgCodecs ) {
            System.err.println(i.getMetadata());
        }
    }
}
