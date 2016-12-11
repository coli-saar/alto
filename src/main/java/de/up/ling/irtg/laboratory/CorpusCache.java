/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.CorpusWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author koller
 */
public class CorpusCache extends AltoLabHttpCache<Corpus> {

    private InterpretedTreeAutomaton irtg;

    public CorpusCache(Path baseDir, URI baseURL, InterpretedTreeAutomaton irtg, AltoLabHttpClient labClient) {
        super(baseDir, baseURL, labClient);
        this.irtg = irtg;
    }

    @Override
    protected String makeCacheFilename(String identifier) {
        return String.format("corpora/%s", identifier);
    }

    @Override
    protected Corpus readFromStream(String identifier, InputStream is, boolean remote) throws ValueReadingException, IOException {
        try {
            return Corpus.readCorpus(new InputStreamReader(is), irtg);
        } catch (CorpusReadingException ex) {
            throw new ValueReadingException(ex);
        }
    }

    @Override
    protected void writeToStream(String identifier, Corpus value, OutputStream os) throws IOException {
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String dateStr = date.format(formatter);
        String comment = String.format("Corpus downloaded from %s on %s", makeURI(identifier), dateStr);
        
        CorpusWriter cw = new CorpusWriter(irtg, comment, "/_/_/_/", new OutputStreamWriter(os));
        cw.writeCorpus(value);
        cw.close();
    }

}
