/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.laboratory;

import de.saar.basic.StringTools;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Path;

/**
 *
 * @author koller
 */
public class AdditionalDataCache extends AltoLabHttpCache<String> {
    public AdditionalDataCache(Path baseDir, URI baseURL, AltoLabHttpClient labClient) {
        super(baseDir, baseURL, labClient);
    }
    
    @Override
    protected String makeCacheFilename(String identifier) {
        return String.format("additional_data/%s", identifier);
    }

    @Override
    protected String readFromStream(String identifier, InputStream is, boolean remote) throws IOException {
        String ret = StringTools.slurp(new InputStreamReader(is));
        return ret;
    }

    @Override
    protected void writeToStream(String identifier, String value, OutputStream os) throws IOException {
        Writer w = new OutputStreamWriter(os);
        w.write(value);
        w.flush();
    }
    
}
