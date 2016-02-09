/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing.geoquery;

import de.saar.basic.Pair;
import de.up.ling.irtg.rule_finding.handle_unknown.ReduceAndDrop;
import de.up.ling.irtg.rule_finding.preprocessing.geoquery.CreateLexicon.SimpleCheck;
import de.up.ling.irtg.util.FunctionIterable;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

/**
 *
 * @author christoph_teichmann
 */
public class AntiUnknown {

    /**
     *
     */
    private static final Function<String, String> DEFAULT_UNKNOWN = (String) -> {
        return "__unknown__";
    };

    /**
     *
     */
    private final Function<String, String> unknown;

    /**
     *
     */
    private final Function<String, String> known;

    /**
     *
     */
    private final int minNumber;

    /**
     *
     */
    private final CreateLexicon cl;

    /**
     *
     * @param unknown
     * @param known
     * @param minWordCount
     * @param facts
     * @throws IOException
     */
    public AntiUnknown(Function<String, String> unknown, Function<String, String> known,
            int minWordCount, InputStream facts) throws IOException {
        this.unknown = unknown;
        this.known = known;
        this.minNumber = minWordCount;
        this.cl = new CreateLexicon(facts);
    }

    /**
     *
     * @param lettersToDrop
     * @param minLength
     * @param minWordCount
     * @param facts
     * @throws IOException
     */
    public AntiUnknown(int lettersToDrop, int minLength, int minWordCount,
                                        InputStream facts) throws IOException {
        this(DEFAULT_UNKNOWN, (String keep) -> {
            int length = Math.max(Math.min(minLength, keep.length()), keep.length() - lettersToDrop);

            return keep.substring(0, length);
        }, minWordCount, facts);
    }

    /**
     *
     * @param original
     * @return
     * @throws java.io.IOException
     */
    public Iterable<Pair<String, String>> reduceUnknownWithFacts(Iterable<Pair<String, String>> original)
            throws IOException {
        ReduceAndDrop rad = new ReduceAndDrop(minNumber, unknown, known);
        Iterable<String[]> statStream = new FunctionIterable<>(original, (Pair<String, String> in) -> {
            return in.getLeft().toLowerCase().trim().split("\\s+");
        });

        Function<String[], String> unk = rad.getReduction(statStream.iterator());

        Iterable<Pair<String, String>> main = cl.replace(original);
        return new FunctionIterable<>(main, (Pair<String, String> p) -> {
            String left = unk.apply(p.getLeft().trim().split("\\s+"));

            return new Pair<>(left, p.getRight());
        });
    }

    /**
     * 
     * @param data
     * @return 
     */
    public Iterable<Pair<String, String>> reduceUnknownWithoutFacts(Iterable<Pair<String, String>> data) {
        SimpleCheck check = this.cl.getCheck();
        
        Iterable<String[]> statStream = new FunctionIterable<>(data, (Pair<String, String> in) -> {
            return in.getLeft().toLowerCase().trim().split("\\s+");
        });
        
        ReduceAndDrop rad = new ReduceAndDrop(minNumber, unknown, known);
        
        Function<String[],String> f = rad.getCheckedReduction(statStream.iterator(),check);
        
        return new FunctionIterable<>(data,(Pair<String,String> p) -> {
            String[] fos = p.getLeft().toLowerCase().trim().split("\\s+");
            
            return new Pair<>(f.apply(fos),p.getRight());
        });
    }
}
