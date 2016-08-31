/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.io;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import de.up.ling.irtg.util.Logging;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

/**
 *
 * @author koller
 */
public abstract class Cache<E> {

    public static void main(String[] args) throws URISyntaxException, ValueReadingException, IOException {
        Logging.get().setLevel(Level.ALL);

        /*
        GrammarCache gc = new GrammarCache(Paths.get(".alto", "cache"), new URI("http://tcl.ling.uni-potsdam.de:5000/rest/"));
        long startTime = System.currentTimeMillis();
        InterpretedTreeAutomaton irtg = gc.get("grammar_7.irtg");
        long endTime = System.currentTimeMillis();
                */

        Path baseDir = Paths.get(".alto", "cache");
        URI baseURI = new URI("http://tcl.ling.uni-potsdam.de:5000/rest/");
        
        GrammarCache gc = new GrammarCache(baseDir, baseURI);
        CpuTimeStopwatch sw = new CpuTimeStopwatch();
        sw.record();
        InterpretedTreeAutomaton irtg = gc.get("grammar_16.irtg");
        sw.record();
        
        CorpusCache cc = new CorpusCache(baseDir, baseURI, irtg);
        Corpus c = cc.get("corpus_23.txt");
        sw.record();
        
        System.err.println(c.getNumberOfInstances());
        sw.printMilliseconds("load grammar", "load corpus");

//        System.err.println(irtg);
//        System.err.println("Loading took " + Util.formatTime(endTime - startTime));
    }

    private final Path baseDir;  // e.g. ~/.alto/cache

    abstract protected E loadFromRemote(String identifier) throws ValueReadingException, IOException;

    abstract protected String makeCacheFilename(String identifier);

    abstract protected E readFromStream(String identifier, InputStream is, boolean remote) throws ValueReadingException, IOException;

    abstract protected void writeToStream(String identifier, E value, OutputStream os) throws IOException;

    protected Cache(Path baseDir) {
        Path b = Paths.get(System.getProperty("user.home"));
        this.baseDir = b.resolve(baseDir);
    }

    public boolean isInCache(String identifier) {
        return getCacheFile(identifier).exists();
    }

    public E get(String identifier) throws ValueReadingException, IOException {
        E ret = loadFromCache(identifier);

        if (ret != null) {
        } else {
            ret = loadFromRemote(identifier);

            if (ret != null) {
                writeToCache(identifier, ret);
            }
        }

        return ret;
    }

    public E getFromRemote(String identifier) throws ValueReadingException, IOException {
        E ret = loadFromRemote(identifier);

        if (ret != null) {
            writeToCache(identifier, ret);
        }

        return ret;
    }

    protected E loadFromCache(String identifier) throws ValueReadingException, IOException {
        File f = getCacheFile(identifier);

        try {
            return readFromStream(identifier, new FileInputStream(f), false);
        } catch (FileNotFoundException ex) {
            return null;
        }
    }

    protected void writeToCache(String identifier, E value) throws IOException {
        File f = getCacheFile(identifier);
        f.getParentFile().mkdirs();
        f.createNewFile();

        writeToStream(identifier, value, new FileOutputStream(f));
    }

    private File getCacheFile(String identifier) {
        return baseDir.resolve(makeCacheFilename(identifier)).toFile();
    }

    public static class ValueReadingException extends Exception {

        public ValueReadingException() {
        }

        public ValueReadingException(String message) {
            super(message);
        }

        public ValueReadingException(String message, Throwable cause) {
            super(message, cause);
        }

        public ValueReadingException(Throwable cause) {
            super(cause);
        }

        public ValueReadingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }

    }
}
