/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysql.jdbc.Util;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 *
 * @author koller
 */
public class JsonResultManager implements ResultManager {
    private static class Result {
        public int instanceID;
        public boolean isNumeric;
        public boolean isGlobal;
        public String name;
        public String value;

        public Result(int instanceID, boolean isNumeric, boolean isGlobal, String name, String value) {
            this.instanceID = instanceID;
            this.isNumeric = isNumeric;
            this.isGlobal = isGlobal;
            this.name = name;
            this.value = value;
        }
    }

    private static class Time {
        public int instanceID;
        public String name;
        public boolean isGlobal;
        public long time;

        public Time(int instanceID, String name, boolean isGlobal, long time) {
            this.instanceID = instanceID;
            this.name = name;
            this.isGlobal = isGlobal;
            this.time = time;
        }
    }

    private static class Error {
        public int instanceID;
        public String name;
        public boolean isGlobal;
        public String error;

        public Error(int instanceID, String name, boolean isGlobal, String error) {
            this.instanceID = instanceID;
            this.name = name;
            this.isGlobal = isGlobal;
            this.error = error;
        }
    }

    private static class Buffer {
        public int experimentID;
        public List<Result> results = new ArrayList<>();
        public List<Time> times = new ArrayList<>();
        public List<Error> errors = new ArrayList<>();

        public void clear() {
            results.clear();
            times.clear();
            errors.clear();
        }
    }

    private Buffer buffer = new Buffer();
    private ObjectMapper mapper = new ObjectMapper();

    private String url;

    public JsonResultManager(int experimentID, String url) {
        this.url = url + "post_results";
        this.buffer.experimentID = experimentID;
    }

    @Override
    public void acceptResult(Object result, int instanceID, String name, boolean doExport, boolean isGlobal, boolean isNumeric) {
        if (doExport) {
            Result r = new Result(instanceID, isNumeric, isGlobal, name, result.toString());
            buffer.results.add(r);
        }
    }

    @Override
    public void acceptTime(long time, int instanceID, String name, boolean isGlobal) {
        Time t = new Time(instanceID, name, isGlobal, time);
        buffer.times.add(t);
    }

    @Override
    public void acceptError(Throwable error, int instanceID, String name, boolean doExport, boolean isGlobal) {
        if (doExport) {
            Error e = new Error(instanceID, name, isGlobal, Util.stackTraceToString(error));
            buffer.errors.add(e);
        }
    }

    @Override
    public synchronized void flush() throws IOException {
        try {
            String s = mapper.writeValueAsString(buffer);
            buffer.clear();

            if (url == null) {
                System.err.println(s);
            } else {
                HttpClient httpClient = HttpClientBuilder.create().build();
                HttpPost request = new HttpPost(url);
                StringEntity params = new StringEntity(s);
                request.addHeader("content-type", "application/json");
                request.setEntity(params);
                HttpResponse response = httpClient.execute(request);
                
                if( response.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
                    throw new IOException("HTTP error: " + response.getStatusLine().toString());
                }
            }
        } catch (JsonProcessingException ex) {
            Logger.getLogger(JsonResultManager.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
