/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.featstruct;

import de.up.ling.irtg.codec.CodecUtilities;
import de.up.ling.irtg.codec.ExceptionErrorStrategy;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;

/**
 *
 * @author koller
 */
class FsParser {
    private Map<String, FeatureStructure> indexToFs = new HashMap<>();
    private Map<String, FeatureStructure> canonicalPlaceholders = new HashMap<>();

    public FeatureStructure parse(InputStream is) throws IOException, FsParsingException {
        FeatStructLexer l = new FeatStructLexer(new ANTLRInputStream(is));
        FeatStructParser p = new FeatStructParser(new CommonTokenStream(l));

        p.setErrorHandler(new ExceptionErrorStrategy());
        p.getInterpreter().setPredictionMode(PredictionMode.SLL);

        FeatStructParser.FeatstructContext result = p.featstruct();
        FeatureStructure fs = build(result);

        // resolve coindexation
        for (FeatureStructure ifs : indexToFs.values()) {
            resolveCoindexation(ifs, indexToFs);
        }
        resolveCoindexation(fs, indexToFs);

        return fs;
    }

    private FeatureStructure build(FeatStructParser.FeatstructContext context) throws FsParsingException {
        FeatureStructure ret = null;

        if (context.avm() != null) {
            ret = avm(context.avm());
        }

        if (context.primitive() != null) {
            ret = primitive(context.primitive());
        }

        if (context.index() != null) {
            String index = parseIndex(context.index());
            FeatureStructure previouslySeen = indexToFs.get(index);

            // redefining index FS not yet supported => throw exception
            if (ret != null && previouslySeen != null) {
                throw new UnsupportedOperationException("Only one non-null #index per feature structure supported.");
            }

            // FS was first non-null encounter of #index => store it
            if (ret != null) {
                indexToFs.put(index, ret);
                ret.setIndex(index);
            }

            // In this pass over the FS, replace all #index'ed nodes with
            // placeholders. These will be replaced by indexToFs.get(index)
            // in a second pass.
            ret = new PlaceholderFeatureStructure(index);

            // remember first placeholder for index as canonical
            if (!canonicalPlaceholders.containsKey(index)) {
                canonicalPlaceholders.put(index, ret);
            }
        }

        return ret;
    }

    private AvmFeatureStructure avm(FeatStructParser.AvmContext context) throws FsParsingException {
        AvmFeatureStructure ret = new AvmFeatureStructure();

        for (FeatStructParser.AvpairContext avp : context.avpair()) {
            String attribute = parseName(avp.name());
            FeatureStructure value = build(avp.featstruct());
            ret.put(attribute, value);
        }

        return ret;
    }

    private PrimitiveFeatureStructure primitive(FeatStructParser.PrimitiveContext context) throws FsParsingException {
        if (context.name() != null) {
            return new PrimitiveFeatureStructure(parseName(context.name()));
        }

        if (context.number() != null) {
            return new PrimitiveFeatureStructure(parseNumber(context.number()));
        }

        throw new FsParsingException();
    }

    private static String parseName(FeatStructParser.NameContext nc) {
        boolean isQuoted = (nc instanceof FeatStructParser.QUOTEDContext || nc instanceof FeatStructParser.DQUOTEDContext);

        assert !isQuoted || nc.getText().startsWith("'") || nc.getText().startsWith("\"") : "invalid symbol: -" + nc.getText() + "- " + nc.getClass();

        return CodecUtilities.extractName(nc, isQuoted);
    }

    private static int parseNumber(FeatStructParser.NumberContext nc) throws FsParsingException {
        if (nc.INT() != null) {
            return Integer.parseInt(nc.INT().getText());
        }

        throw new FsParsingException();
    }

    private static String parseIndex(FeatStructParser.IndexContext ic) {
        return ic.INDEX().getText().substring(1);
    }

//    public static void main(String[] args) throws IOException, FsParsingException {
//        String s = StringTools.slurp(new FileReader("/tmp/fs.txt")); // [num: sg, gen: \"masc foo\", bar: [test: 17]]";
//        FsParser parser = new FsParser();
//        FeatureStructure f = parser.parse(new ByteArrayInputStream(s.getBytes()));
//        System.err.println(f);
//    }
    private void resolveCoindexation(FeatureStructure fs, Map<String, FeatureStructure> indexToFs) {
        if (fs instanceof AvmFeatureStructure) {
            AvmFeatureStructure afs = (AvmFeatureStructure) fs;

            for (String attr : afs.getAttributes()) {
                FeatureStructure f = afs.get(attr);

                if (f instanceof PlaceholderFeatureStructure) {
                    String index = ((PlaceholderFeatureStructure) f).getIndex();
                    FeatureStructure replacement = indexToFs.get(index);

                    if (replacement == null) {
                        // null means: have seen this #index multiple times, but never
                        // with a concrete value => put canonical placeholder there
                        // to enforce reentrancy
                        replacement = canonicalPlaceholders.get(index);
                    }

                    assert replacement != null;

                    afs.put(attr, replacement);
                } else {
                    resolveCoindexation(f, indexToFs);
                }
            }
        }
    }
}
