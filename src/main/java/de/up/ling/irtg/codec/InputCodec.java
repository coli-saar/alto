/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.ProgressListener;
import de.up.ling.irtg.util.Util;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reconstructs an object from a representation. Representations will
 * typically be strings, but you could also implement an input codec
 * for binary representations.<p>
 * 
 * The codec will attempt to decode the representation into an object
 * of the class specified
 * by the type parameter E. For instance, a {@link IrtgInputCodec}
 * will decode a string representation into an {@link InterpretedTreeAutomaton},
 * whereas a {@link TreeAutomatonInputCodec} will decode a string into
 * a {@link TreeAutomaton}.<p>
 * 
 * Concrete input codec implementations must implement the abstract method
 * {@link #read(java.io.InputStream) }, which reads data from an input stream
 * and decodes it into an object. This class then
 * provides a number of convenience methods that make use of the concrete
 * implementation of read.<p>
 * 
 * Input codecs can be <i>registered</i> by adding them to the file
 * src/main/resources/META-INF/services/de.up.ling.codec.InputCodec.
 * Input codecs that are suitable for a given class of objects can then
 * be found using {@link #getInputCodecs(java.lang.Class) }. <p>
 * 
 * In order to be discovered correctly, each input codec needs to be annotated
 * with {@link CodecMetadata}, such as the type of E and a filename extension.
 * 
 * @author koller
 */
public abstract class InputCodec<E> {
    private static Map<String, InputCodec> codecByName = null;
    private static Map<String, InputCodec> codecByExtension = null;
    private Map<String,String> options = new HashMap<>();
    
    protected ProgressListener progressListener;

    /**
     * Reads an object from an input stream.
     * 
     * @throws CodecParseException if an error occurred while decoding
     * the input stream into an object
     * @throws IOException if an error occurred while reading
     * data from the input stream
     */
    public abstract E read(InputStream is) throws CodecParseException, IOException;

    /**
     * Reads an object from a string representation.
     * This method uses {@link String#getBytes() } to decode
     * the string into a sequence of bytes using the
     * platform's default encoding, and then reads
     * an object from this byte stream using {@link #read(java.io.InputStream) }.
     * It is provided as a convenience method for the
     * frequent special case where the object is represented
     * as a string of some kind.
     * 
     * @throws CodecParseException
     */
    public E read(String s) throws CodecParseException {
        try {
            return read(new ByteArrayInputStream(s.getBytes()));
        } catch (IOException ex) {
            // this should never happen -- no IO exceptions when reading from strings
            Logger.getLogger(InputCodec.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /**
     * Returns the metadata for this input codec.
     * 
     */
    public CodecMetadata getMetadata() {
        return getClass().getAnnotation(CodecMetadata.class);
    }

    /**
     * Returns all registered input codecs. You can register
     * a new input codec by adding its fully qualified class name
     * to the file <code>META-INF/services/de.up.ling.irtg.codec.InputCodec</code>.
     * You can find this file under <code>src/main/resources</code>
     * in the IRTG source code repository. Note: It is usually a better idea
     * to call {@link #getInputCodecs(java.lang.Class) } to ensure type-safety.
     * 
     */
    public static Iterable<InputCodec> getAllInputCodecs() {
        return ServiceLoader.load(InputCodec.class);
    }

    /**
     * Returns all registered input codecs that can produce objects
     * of class <code>T</code> (or a subtype). For instance, call
     * <code>InputCodec.getInputCodecs(Tree.class)</code> to obtain
     * all input codecs for trees.
     * The method takes the
     * information about what class an input codec reads from the
     * <code>type</code> field in the codec's metadata annotation.
     * 
     * @param <T>
     */
    public static <T> List<InputCodec<T>> getInputCodecs(Class<T> forClass) {
        List<InputCodec<T>> ret = new ArrayList<>();

        for (InputCodec codec : getAllInputCodecs()) {
            if (forClass.isAssignableFrom(codec.getMetadata().type())) {
                ret.add(codec);
            }
        }

        return ret;
    }

    /**
     * Returns the registered input codec with the given name (as per
     * the codec metadata's <code>name</code> field). If
     * no codec with this name can be found, returns null.
     * 
     */
    public static InputCodec getInputCodecByName(String name) {
        if (codecByName == null) {
            codecByName = new HashMap<>();
            for (InputCodec ic : getAllInputCodecs()) {
                codecByName.put(ic.getMetadata().name(), ic);
            }
        }

        return codecByName.get(name);
    }

    /**
     * Returns the registered input codec for the given
     * filename extension (as per the codec metadata's
     * <code>extension</code> field). If no codec for
     * this extension can be found, returns null.
     * 
     */
    public static InputCodec getInputCodecByExtension(String extension) {
        if (codecByExtension == null) {
            codecByExtension = new HashMap<>();
            for (InputCodec ic : getAllInputCodecs()) {
                codecByExtension.put(ic.getMetadata().extension(), ic);
            }
        }

        return codecByExtension.get(extension);
    }
    
    /**
     * Returns the input codec specified by the given codec name and/or
     * filename. If the codecName is non-null, tries to resolve it to a
     * registered input codec; if none is found, the method throws an
     * exception. If the codecName is null, tries to determine the input
     * codec from the filename extension of the (non-null) filename argument.
     * If this is not possible, or both arguments are null, throws an exception.
     * 
     * @throws Exception 
     */
    public static InputCodec getInputCodecByNameOrExtension(String filename, String codecName) throws CodecLookupException {
        InputCodec ic = null;
        
        if( codecName != null ) {
            ic = InputCodec.getInputCodecByName(codecName);
            
            if( ic == null ) {
                throw new CodecLookupException("Unknown input codec: " + codecName);
            }
        } else if( filename != null ) {            
            String ext = Util.getFilenameExtension(filename);
            ic = InputCodec.getInputCodecByExtension(ext);
            
            if( ic == null ) {
                throw new CodecLookupException("Could not determine input codec from filename extension '" + ext + "'");
            }
        } else {
            throw new CodecLookupException("Specify an input codec, either explicitly or through the filename extension of the input file.");
        }
        
        return ic;
    }

    public static class CodecLookupException extends Exception {
        public CodecLookupException(String message) {
            super(message);
        }
    }
    
    /**
     * Sets the progress listener for this codec. If the
     * input codec takes a long time to decode an input
     * stream, it may inform the user about its progress
     * by sending updates to the progress listener.
     * You can call this method with a null argument
     * to disable progress updates.
     * 
     */
    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }
    
    protected void notifyProgressListener(int currentValue, int maxValue, String string) {
        if( progressListener != null ) {
            progressListener.accept(currentValue, maxValue, string);
        }
    }
    
    public void addOptions(String options) {
        String[] parts = options.split("\\s*[,=:]\\s*");
        for( int i = 0; i < parts.length; i += 2 ) {
            this.options.put(parts[i], parts[i+1]);
        }
    }
    
    public void setOption(String option, String value) {
        options.put(option, value);
    }
    
    public String getOption(String key) {
        return options.get(key);
    }
    
    public boolean hasTrueOption(String key) {
        String val = getOption(key);

        return val != null && val.toLowerCase().equals("true");
    }

    public static void main(String[] args) throws FileNotFoundException, CodecParseException, IOException, Exception {
        List<InputCodec<InterpretedTreeAutomaton>> irtgCodecs = InputCodec.getInputCodecs(InterpretedTreeAutomaton.class);
        for (InputCodec i : irtgCodecs) {
            System.err.println(i.getMetadata());
        }
    }
}
