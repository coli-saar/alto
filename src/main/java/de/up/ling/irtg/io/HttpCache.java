/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.io;

import de.up.ling.irtg.util.Logging;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

/**
 *
 * @author koller
 */
public abstract class HttpCache<E> extends Cache<E> {
    private final URI baseURL;
    
    public HttpCache(Path baseDir, URI baseURL) {
        super(baseDir);
        this.baseURL = baseURL;
    }

    @Override
    protected E loadFromRemote(String identifier) throws IOException, ValueReadingException {
        URI uri = makeURI(identifier);
        
        try {
            URL url = uri.toURL();
            return readFromStream(identifier, url.openStream(), true);
        } catch (MalformedURLException ex) {
            Logging.get().warning("Malformed URL in HttpCache#loadFromRemote: " + uri);
            return null;
        } 
    }
    
    protected URI makeURI(String identifier) {
        return baseURL.resolve(identifier);
    }
}
