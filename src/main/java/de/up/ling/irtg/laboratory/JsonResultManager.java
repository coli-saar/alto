/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

import com.mysql.jdbc.Util;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    private final Buffer buffer = new Buffer();
//    private ObjectMapper mapper = new ObjectMapper();

    private final String postResultsUrl;
    private final String finishExperimentUrl;
    private final AltoLabHttpClient labClient;

    public JsonResultManager(int experimentID, String url, AltoLabHttpClient labClient) {
        this.postResultsUrl = url + "post_results";
        this.finishExperimentUrl = url + "finish_experiment";
        this.buffer.experimentID = experimentID;
        this.labClient = labClient;
    }

    @Override
    public void acceptResult(Object result, int instanceID, String name, boolean doExport, boolean isGlobal, boolean isNumeric) {
        if (doExport) {
            String repr = (result == null) ? "<null>" : result.toString();
            Result r = new Result(instanceID, isNumeric, isGlobal, name, repr);
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
        labClient.postJson(postResultsUrl, buffer);
        buffer.clear();
    }

    @Override
    public int getExperimentID() {
        return buffer.experimentID;
    }

    @Override
    public void finish() throws Exception {
        flush();
        labClient.postJson(finishExperimentUrl, buffer);
    }
}
