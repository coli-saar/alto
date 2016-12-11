/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

import de.up.ling.irtg.io.Cache;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 *
 * @author koller
 */
abstract class AltoLabHttpCache<E> extends Cache<E> {
    private final AltoLabHttpClient labClient;
    private final URI baseURL;
    
    public AltoLabHttpCache(Path baseDir, URI baseURL, AltoLabHttpClient labClient) {
        super(baseDir);
        this.labClient = labClient;
        this.baseURL = baseURL;
    }
    
    @Override
    protected E loadFromRemote(String identifier) throws ValueReadingException, IOException {
        if( labClient == null ) {
            return null;
        } else {
            URI uri = makeURI(identifier);
            String response = labClient.get(uri.toString());
            return readFromStream(identifier, new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), true);
        }
    }
    
    protected URI makeURI(String identifier) {
        return baseURL.resolve(identifier);
    }
}
