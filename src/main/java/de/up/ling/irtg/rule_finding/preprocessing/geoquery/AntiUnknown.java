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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

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
     * @param firstLine
     * @param secondLine
     * @return
     * @throws IOException
     */
    public Iterable<Pair<String, String>> reduceUnknownWithFacts(Supplier<InputStream> original,
                            int firstLine, int secondLine) throws IOException {
        Iterable<Pair<String, String>> source = new Iterable<Pair<String, String>>() {
            @Override
            public Iterator<Pair<String, String>> iterator() {
                Iterator<Pair<String, String>> it = new Iterator() {
                    /**
                     *
                     */
                    private final BufferedReader br;
                    {
                        br = new BufferedReader(new InputStreamReader(original.get()));
                        updateMain();
                    }

                    /**
                     *
                     */
                    private Pair<String, String> main;

                    /**
                     *
                     */
                    private boolean closed = false;

                    @Override
                    public boolean hasNext() {
                        return main != null;
                    }

                    @Override
                    public Object next() {
                        Pair<String, String> m = this.main;
                        this.updateMain();
                        return m;
                    }

                    /**
                     *
                     */
                    private void updateMain() {
                        if (this.closed) {
                            this.main = null;
                            return;
                        }

                        try {
                            String line = this.br.readLine();
                            if (line == null) {
                                this.main = null;

                                closeUp();
                                return;
                            }

                            List<String> list = new ArrayList<>();
                            list.add(line);

                            while ((line = br.readLine()) != null) {
                                line = line.trim();

                                if (line.isEmpty()) {
                                    break;
                                }

                                list.add(line);
                            }

                            if (line == null) {
                                this.closeUp();
                            }

                            if (firstLine > 0 && secondLine > 0 && firstLine < list.size()
                                    && secondLine < list.size()) {
                                main = new Pair<>(list.get(firstLine), list.get(secondLine));
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(AntiUnknown.class.getName()).log(Level.SEVERE, null, ex);
                            throw new RuntimeException("IO problem in iteration .");
                        }
                    }

                    /**
                     *
                     * @throws IOException
                     */
                    private void closeUp() throws IOException {
                        this.br.close();
                        this.closed = true;
                    }
                };

                return it;
            }
        };

        return this.reduceUnknownWithFacts(source);
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
            return in.getLeft().trim().split("\\s+");
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
            return in.getLeft().trim().split("\\s+");
        });
        
        ReduceAndDrop rad = new ReduceAndDrop(minNumber, unknown, known);
        
        Function<String[],String> f = rad.getCheckedReduction(statStream.iterator(),check);
        
        return new FunctionIterable<>(data,(Pair<String,String> p) -> {
            String[] fos = p.getLeft().trim().split("\\s+");
            
            return new Pair<>(f.apply(fos),p.getRight());
        });
    }
}
