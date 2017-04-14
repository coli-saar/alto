/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.featstruct;

import de.up.ling.irtg.codec.CodecUtilities;
import de.up.ling.irtg.codec.ExceptionErrorStrategy;
import de.up.ling.irtg.codec.irtg.IrtgParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 *
 * @author koller
 */
class FsParser {
    public FeatureStructure parse(InputStream is) throws IOException, FsParsingException {
        FeatStructLexer l = new FeatStructLexer(new ANTLRInputStream(is));
        FeatStructParser p = new FeatStructParser(new CommonTokenStream(l));

        p.setErrorHandler(new ExceptionErrorStrategy());
        p.getInterpreter().setPredictionMode(PredictionMode.SLL);

        FeatStructParser.FeatstructContext result = p.featstruct();
        return build(result);
    }
    
    private FeatureStructure build(FeatStructParser.FeatstructContext context) throws FsParsingException {
        if( context.avm() != null ) {
            return avm(context.avm());
        }
        
        if( context.primitive() != null ) {
            return primitive(context.primitive());
        }
        
        throw new FsParsingException();
    }
    
    private AvmFeatureStructure avm(FeatStructParser.AvmContext context) throws FsParsingException {
        AvmFeatureStructure ret = new AvmFeatureStructure();
        
        for( FeatStructParser.AvpairContext avp : context.avpair() ) {
            String attribute = parseName(avp.name());
            FeatureStructure value = build(avp.featstruct());
            ret.add(attribute, value);
        }
        
        return ret;
    }
    
    private PrimitiveFeatureStructure primitive(FeatStructParser.PrimitiveContext context) throws FsParsingException {
        if( context.name() != null ) {
            return new PrimitiveFeatureStructure<String>(parseName(context.name()));
        }
        
        if( context.INT() != null ) {
            return new PrimitiveFeatureStructure<Integer>(parseInt(context.INT()));
        }
        
        throw new FsParsingException();
    }
    
    private static String parseName(FeatStructParser.NameContext nc) {
        boolean isQuoted = (nc instanceof FeatStructParser.QUOTEDContext || nc instanceof FeatStructParser.DQUOTEDContext);

        assert !isQuoted || nc.getText().startsWith("'") || nc.getText().startsWith("\"") : "invalid symbol: -" + nc.getText() + "- " + nc.getClass();

        return CodecUtilities.extractName(nc, isQuoted);
    }
    
    private static int parseInt(TerminalNode tn) {
        return Integer.parseInt(tn.getText());
    }
    
    
    public static void main(String[] args) throws IOException, FsParsingException {
        String s = "[num: sg, gen: \"masc foo\", bar: [test: 17]]";
        FsParser parser = new FsParser();
        FeatureStructure f = parser.parse(new ByteArrayInputStream(s.getBytes()));
        System.err.println(f);
    }

}
