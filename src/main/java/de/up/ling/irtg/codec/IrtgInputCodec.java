/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec;

import de.up.ling.irtg.AntlrIrtgBuilder;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.IrtgLexer;
import de.up.ling.irtg.IrtgParser;
import java.io.IOException;
import java.io.InputStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 *
 * @author koller
 */
@CodecMetadata(name = "irtg", extension = "irtg", type = InterpretedTreeAutomaton.class)
public class IrtgInputCodec extends InputCodec<InterpretedTreeAutomaton> {
    @Override
    public InterpretedTreeAutomaton read(InputStream is) throws IOException, ParseException {
        
        IrtgLexer l = new IrtgLexer(new ANTLRInputStream(is));
        IrtgParser p = new IrtgParser(new CommonTokenStream(l));
        p.setErrorHandler(new BailErrorStrategy());
        p.getInterpreter().setPredictionMode(PredictionMode.SLL);

        try {
//            long t1 = System.nanoTime();
            IrtgParser.IrtgContext result = p.irtg();
//            long t2 = System.nanoTime();
            InterpretedTreeAutomaton irtg = new AntlrIrtgBuilder().build(result);
//            long t3 = System.nanoTime();

//            System.err.println("parsing: " + (t2 - t1) / 1000000 + "ms / construction: " + (t3 - t2) / 1000000 + "ms");

            return irtg;
        } catch (ParseCancellationException e) {
            throw new ParseException(e.getCause());

        }
    }
}
