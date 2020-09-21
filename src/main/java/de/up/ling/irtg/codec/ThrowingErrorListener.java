package de.up.ling.irtg.codec;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * CC-by-sa 4.0 Mouagip (if this snippet is copyrightable at all)
 * https://stackoverflow.com/questions/18132078/handling-errors-in-antlr4
 */

public class ThrowingErrorListener extends BaseErrorListener {

    public static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg, RecognitionException e)
            throws CodecParseException {
        throw new CodecParseException("line " + line + ":" + charPositionInLine + " " + msg);
    }
}