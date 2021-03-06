/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.laboratory;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;

/**
 *
 * @author koller
 */
class TaskCache extends AltoLabHttpCache<UnparsedTask> {
    private ObjectMapper om = new ObjectMapper();

    public TaskCache(Path baseDir, URI baseURL, AltoLabHttpClient labClient) {
        super(baseDir, baseURL, labClient);
    }

    @Override
    protected String makeCacheFilename(String identifier) {
        return String.format("tasks/%s", identifier);
    }

    @Override
    protected UnparsedTask readFromStream(String identifier, InputStream is, boolean remote) throws IOException {
        return om.readValue(is, UnparsedTask.class);
    }

    @Override
    protected void writeToStream(String identifier, UnparsedTask value, OutputStream os) throws IOException {
        om.writeValue(os, value);
    }   
    
}
