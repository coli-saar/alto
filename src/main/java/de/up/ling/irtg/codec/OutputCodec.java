/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec;

import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.tree.Tree;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
<<<<<<< local
 * An output codec for writing objects of class <code>E</code>
 * to an output stream in a specific format.
=======
 * Generates a representation of some object. Representations will
 * typically be strings, but you could also implement an output codec
 * for binary representations.<p>
>>>>>>> other
 * 
<<<<<<< local
=======
 * The objects that a codec can encode must be of the class specified
 * by the type parameter E. For instance, a {@link TikzQtreeOutputCodec}
 * will encode objects of class {@link Tree} as strings, whereas a
 * {@link SgraphAmrOutputCodec} will encode objects of class {@link SGraph}
 * as strings.<p>
 * 
 * Concrete output codec implementations must implement the abstract method
 * {@link #write(java.lang.Object, java.io.OutputStream) }, which writes
 * a  representation of an object to some output stream. This class then
 * provides a number of convenience methods that make use of the concrete
 * implementation of write.<p>
 * 
 * Output codecs can be <i>registered</i> by adding them to the file
 * src/main/resources/META-INF/services/de.up.ling.codec.OutputCodec.
 * Output codecs that are suitable for a given class of objects can then
 * be found using {@link #getOutputCodecs(java.lang.Class) }. For instance, the Alto GUI
 * allows the user to right-click on an object to obtain a string representation;
 * the possible string representations are automatically discovered from the
 * output codecs that are available for that object.<p>
 * 
 * In order to be discovered correctly, each output codec needs to be annotated
 * with {@link CodecMetadata}. Most importantly, the "type" field of that annotation
 * specifies the class that the codec can encode.
 * 
>>>>>>> other
 * @author koller
 */
public abstract class OutputCodec<E> {
    /**
     * Writes a string representation of a given object to an output stream.
     * Implement this method to implement your own concrete encoding.
     * 
     * @param object
     * @param ostream
     * @throws IOException if something went wrong with I/O
     * @throws UnsupportedOperationException if a problem occurred in computing the representation of the object
     */
    public abstract void write(E object, OutputStream ostream) throws IOException, UnsupportedOperationException;
    
    /**
     * Returns a function that computes a string representation of the given object.
     * The string representation itself is only computed when the function is
     * called, so any errors that occur when encoding will only happen at that time.
     * 
     * @param object
     * @return 
     */
    public Supplier<String> asStringSupplier(E object) {
        return () -> asString(object);
    }
    
    /**
     * Returns a string representation of the given object. Internally, this
     * method calls {@link #write(java.lang.Object, java.io.OutputStream) }
     * and writes a representation into a string.
     * 
     * @param object
     * @return
     * @throws UnsupportedOperationException  if a problem occurred in computing the representation of the object
     */
    public String asString(E object) throws UnsupportedOperationException {
        try {
            ByteArrayOutputStream ostream = new ByteArrayOutputStream();
            write(object, ostream);
            ostream.flush();
            return ostream.toString();
        } catch (IOException ex) {
            // This should not happen -- no IO exceptions when writing to byte array
            Logger.getLogger(OutputCodec.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    private static Iterable<OutputCodec> getAllOutputCodecs() {
        return ServiceLoader.load(OutputCodec.class);
    }

    /**
     * Returns a list of all output codecs that can encode objects
     * of the given class.
     * 
     * @param <T>
     * @param forClass
     * @return 
     */
    public static <T> List<OutputCodec<T>> getOutputCodecs(Class<T> forClass) {
        List<OutputCodec<T>> ret = new ArrayList<>();

        for (OutputCodec codec : getAllOutputCodecs()) {
//            if (forClass.isAssignableFrom(codec.getMetadata().type())) {
            if (codec.getMetadata().type().isAssignableFrom(forClass)) {
                ret.add(codec);
            }
        }

        return ret;
    }
    
    /**
     * Returns the metadata associated with this output object.
     * 
     * @return 
     */
    public CodecMetadata getMetadata() {
        return (CodecMetadata) getClass().getAnnotation(CodecMetadata.class);
    }
}
