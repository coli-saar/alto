/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

import de.up.ling.irtg.util.BuildProperties;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author koller
 */
public class JsonResultManagerFactory {
    private final String url;
    private final String createExperimentUrl;
    private final AltoLabHttpClient client;

    public JsonResultManagerFactory(String url, AltoLabHttpClient client) {
        this.url = url;
        this.client = client;
        createExperimentUrl = url + "create_experiment";
    }

    public JsonResultManager startExperiment(int taskId, String comment, String hostname, Map<String, String> varRemapper, List<String> dataIDs) throws IOException {
        int experimentID = getExperimentID(taskId, comment, hostname, varRemapper, dataIDs);
        return new JsonResultManager(experimentID, url, client);
    }

    private int getExperimentID(int taskId, String comment, String hostname, Map<String, String> varRemapper, List<String> dataIDs) throws IOException {
        ExperimentData data = new ExperimentData(taskId, comment, hostname, getSystemRevision(), varRemapper, dataIDs);
        String resp = client.postJson(createExperimentUrl, data);
        return Integer.parseInt(resp);
    }


    private static class ExperimentData {
        public int taskId;
        public String comment;
        public String hostname;
        public String systemRevision;
        public Map<String, String> varRemapper;
        public List<String> dataIDs;

        public ExperimentData(int taskId, String comment, String hostname, String systemRevision, Map<String, String> varRemapper, List<String> dataIDs) {
            this.taskId = taskId;
            this.comment = comment;
            this.hostname = hostname;
            this.systemRevision = systemRevision;
            this.varRemapper = varRemapper;
            this.dataIDs = dataIDs;
        }

        
    }
    
    private static String getSystemRevision() {
        return BuildProperties.getVersion()+" "+BuildProperties.getBuild();
    }

}
