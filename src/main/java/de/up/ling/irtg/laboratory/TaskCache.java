/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.laboratory;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.up.ling.irtg.io.HttpCache;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author koller
 */
class TaskCache extends HttpCache<UnparsedTask> {
    private ObjectMapper om = new ObjectMapper();

    public TaskCache(Path baseDir, URI baseURL) {
        super(baseDir, baseURL);
    }

    @Override
    protected String makeCacheFilename(String identifier) {
        return String.format("tasks/%s", identifier);
    }

    @Override
    protected UnparsedTask readFromStream(String identifier, InputStream is, boolean remote) throws ValueReadingException, IOException {
        return om.readValue(is, UnparsedTask.class);
    }

    @Override
    protected void writeToStream(String identifier, UnparsedTask value, OutputStream os) throws IOException {
        om.writeValue(os, value);
    }
    
    public static void main(String[] args) throws URISyntaxException, ValueReadingException, IOException {
        TaskCache tc = new TaskCache(Paths.get(".alto", "cache"), new URI("http://localhost:5000/rest/task/"));
        
        System.err.println(tc.get("13"));
    }
    
    
}
