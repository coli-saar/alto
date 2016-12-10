/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysql.jdbc.Util;
import de.saar.basic.StringTools;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    public void flush() {
        try {
            String s = mapper.writeValueAsString(buffer);
            System.err.println(s);
            buffer.clear();
        } catch (JsonProcessingException ex) {
            Logger.getLogger(JsonResultManager.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
