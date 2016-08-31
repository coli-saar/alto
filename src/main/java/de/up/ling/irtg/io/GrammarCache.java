/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.io;

import de.saar.basic.StringTools;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.codec.BinaryIrtgInputCodec;
import de.up.ling.irtg.codec.BinaryIrtgOutputCodec;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.codec.OutputCodec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;

/**
 *
 * @author koller
 */
public class GrammarCache extends HttpCache<InterpretedTreeAutomaton> {
    public GrammarCache(Path baseDir, URI baseURL) {
        super(baseDir, baseURL);
    }

    @Override
    protected String makeCacheFilename(String identifier) {
        String nameWithoutExtension = identifier.substring(0, identifier.lastIndexOf('.'));
        return String.format("grammars/%s.irtb", nameWithoutExtension);
    }

    @Override
    protected InterpretedTreeAutomaton readFromStream(String identifier, InputStream is, boolean remote) throws ValueReadingException, IOException {
        InputCodec<InterpretedTreeAutomaton> ic = null;
        
        if( remote ) { 
            try {
                ic = InputCodec.getInputCodecByNameOrExtension(identifier, null);
            } catch (Exception ex) {
                throw new ValueReadingException(ex);
            }
        } else {
            ic = new BinaryIrtgInputCodec();
        }
        
        
        
        InterpretedTreeAutomaton irtg = ic.read(is);
        return irtg;
    }

    @Override
    protected void writeToStream(String identifier, InterpretedTreeAutomaton value, OutputStream os) throws IOException {
        OutputCodec<InterpretedTreeAutomaton> oc = new BinaryIrtgOutputCodec();
        oc.write(value, os);
        os.flush();
    }
    
}
