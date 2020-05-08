/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.IntervalSet;

/**
 * An error strategy for ANTLR parsers that reports errors
 * as exceptions.
 * 
 * @author koller
 */
public class ExceptionErrorStrategy extends DefaultErrorStrategy {

    @Override
    public void recover(Parser recognizer, RecognitionException e) {
        throw e;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void reportInputMismatch(Parser recognizer, InputMismatchException e) throws RecognitionException {
        String msg = getTokenPosition(e.getOffendingToken()) + ": mismatched input " + getTokenErrorDisplay(e.getOffendingToken());
        msg += " expecting one of " + e.getExpectedTokens().toString(recognizer.getTokenNames());
        RecognitionException ex = new RecognitionException(msg, recognizer, recognizer.getInputStream(), recognizer.getContext());
        ex.initCause(e);
        throw ex;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void reportMissingToken(Parser recognizer) {
        beginErrorCondition(recognizer);
        Token t = recognizer.getCurrentToken();
        IntervalSet expecting = getExpectedTokens(recognizer);
        String msg = getTokenPosition(t) + ": missing " + expecting.toString(recognizer.getTokenNames()) + " at " + getTokenErrorDisplay(t);
        throw new RecognitionException(msg, recognizer, recognizer.getInputStream(), recognizer.getContext());
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void reportNoViableAlternative(Parser parser, NoViableAltException nvae) {
        Token t = parser.getCurrentToken();
        IntervalSet expecting = getExpectedTokens(parser);
        String msg = getTokenPosition(t) + ": no viable alternative: " + expecting.toString(parser.getTokenNames()) + " at " + getTokenErrorDisplay(t);
        throw new RecognitionException(msg, parser, parser.getInputStream(), parser.getContext());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void reportError(Parser parser, RecognitionException e) {
        if (e.getMessage() != null) {
            // if e already contained an error message, just pass it on
            throw e;
        } else if (e instanceof LexerNoViableAltException) {
            LexerNoViableAltException ex = (LexerNoViableAltException) e;
            int start = Math.max(0, ex.getStartIndex()-20);
            int end = Math.min(ex.getInputStream().size(), ex.getStartIndex()+20);
            String offendingSymbol = ex.getInputStream().getText(new Interval(ex.getStartIndex(), ex.getStartIndex())).trim();
            String context = ex.getInputStream().getText(new Interval(start, end)).replaceAll("\\n", "").trim();
            String msg = String.format("Unexpected symbol '%s' at position %d; context: /%s/", offendingSymbol, ex.getStartIndex(), context);
            throw new CodecParseException(msg);
        } else {
            // otherwise, construct a meaningful error message
            Token tok = e.getOffendingToken();
            StringBuilder buf = new StringBuilder();
            buf.append("Line " + tok.getLine() + ":" + tok.getCharPositionInLine() + ": expected token {");
            
            boolean first = true;

            for (int token : e.getExpectedTokens().toArray()) {
                if( first ) {
                    first = false;
                } else {
                    buf.append(", ");
                }
                buf.append(parser.getTokenNames()[token]);
            }
            
            buf.append("}, but got '" + tok.getText() + "'.");

            throw new CodecParseException(buf.toString());
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void reportFailedPredicate(Parser parser, FailedPredicateException fpe) {
        Token t = parser.getCurrentToken();
        IntervalSet expecting = getExpectedTokens(parser);
        String msg = getTokenPosition(t) + ": failed predicate '" + fpe.getMessage() + "': " + expecting.toString(parser.getTokenNames()) + " at " + getTokenErrorDisplay(t);
        throw new CodecParseException(new RecognitionException(msg, parser, parser.getInputStream(), parser.getContext()));
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void reportUnwantedToken(Parser parser) {
        Token t = parser.getCurrentToken();
        IntervalSet expecting = getExpectedTokens(parser);
        String msg = getTokenPosition(t) + ": expected token " + expecting.toString(parser.getTokenNames()) + ", but got " + getTokenErrorDisplay(t) + ".";
        throw new CodecParseException(new RecognitionException(msg, parser, parser.getInputStream(), parser.getContext()));
    }

    private String getTokenPosition(Token t) {
        return "Line " + t.getLine() + ":" + t.getCharPositionInLine();
    }

}
