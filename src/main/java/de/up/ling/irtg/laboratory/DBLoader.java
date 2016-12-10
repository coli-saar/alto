/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

import de.up.ling.irtg.util.BuildProperties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 *
 * @author groschwitz
 */
@Deprecated
class DBLoader {
    
    private Connection conn;
    
    private String currentServerName;
    private String currentUserName;
    private String currentDB;
    private String currentPassword;
    private String currentPort;
    
    
    private void setUp(String serverName, String userName, String password, String port, String dbName) throws SQLException {
        currentDB = dbName;
        currentPassword = password;
        currentPort = port;
        currentServerName = serverName;
        currentUserName = userName;
        getConnection(serverName, userName, password, port, dbName);
    }
    
    private void reconnectIfNecessary() throws SQLException {
        if (!conn.isValid(0)) {
            try {
                conn.close();
            } catch (SQLException ex) {
                System.err.println("Connection already closed? "+ex.toString());
            }
            getConnection(currentServerName, currentUserName, currentPassword, currentPort, currentDB);
        }
    }
    
//    private static final String DERIV_TREE_NAME_IN_DB = "derivation tree";
//    private static final String STD_GRAMMAR_CONTENT_IN_DB = "**grammar**";
    
    
    public int uploadExperimentStartData(int taskId, String comment, String hostname, Map<String, String> varRemapper, List<String> dataIDs) throws SQLException {
        reconnectIfNecessary();
        String query = "INSERT INTO experiment(id, timestamp, task_id, comments, hostname, status, sys_revision) " +
                "VALUES (NULL, NULL, " + taskId + ", '"+comment+"', '"+hostname+"', 'started', ?)";
        PreparedStatement prepStmt = conn.prepareStatement(query);
        prepStmt.setString(1, getSystemRevision());
        prepStmt.executeUpdate();
        String idQuery = "SELECT LAST_INSERT_ID() AS id";
        prepStmt.close();
        Statement stmt = conn.createStatement();
        ResultSet res = stmt.executeQuery(idQuery);
        if (!res.next()) {
            System.err.println("bad error in mysql: cannot get last inserted id");
        }
        int experimentID = res.getInt("id");
        
        //upload variables
        if (varRemapper != null) {
            for (String varName : varRemapper.keySet()) {
                String varValue = varRemapper.get(varName);
                query = "INSERT INTO experiment_variables(id, experiment_id, variable_name, variable_value) "+
                        "VALUES (NULL, ?, ?, ?)";
                prepStmt = conn.prepareStatement(query);
                prepStmt.setInt(1, experimentID);
                prepStmt.setString(2, varName);
                prepStmt.setString(3, varValue);
                prepStmt.executeUpdate();
                prepStmt.close();
            }
        }
        
        //upload chosen additional data IDs
        if (!dataIDs.isEmpty()) {
            String varValue = dataIDs.stream().collect(Collectors.joining(", "));
            query = "INSERT INTO experiment_variables(id, experiment_id, variable_name, variable_value) "+
                    "VALUES (NULL, ?, ?, ?)";
            prepStmt = conn.prepareStatement(query);
            prepStmt.setInt(1, experimentID);
            prepStmt.setString(2, "**additional_data**");
            prepStmt.setString(3, varValue);
            prepStmt.executeUpdate();
            prepStmt.close();
        }
        
        return experimentID;
    }
    
    public void updateExperimentStateFinished(int experimentID) throws SQLException {
        reconnectIfNecessary();
        //update status as finished
        String query = "UPDATE experiment SET status = 'finished' WHERE id = "+experimentID;
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(query);
        
    }
    
    public void uploadExperimentResult(Object result, String name, int instanceID, int experimentID, boolean isGlobal, boolean isNumeric) throws SQLException {
        reconnectIfNecessary();
        
        String query = (isGlobal) ? "INSERT INTO experiment_global_data" : "INSERT INTO experiment_data";
        
        
        if (isNumeric) {
            
            //upload numerical value
            if (isGlobal) {
                query += "_numeric(id, experiment_id, measurement, result) "
                    +"VALUES(NULL, "+experimentID+", '"+name+"', ?)";
            } else {
                query += "_numeric(id, experiment_id, instance, measurement, result) "
                    +"VALUES(NULL, "+experimentID+", "+instanceID+", '"+name+"', ?)";
            }
            
            PreparedStatement stmt = conn.prepareStatement(query);
            if (result instanceof Number) {
                stmt.setDouble(1, ((Number)result).doubleValue());
            } else if (result == null) {
                stmt.setNull(1, java.sql.Types.DOUBLE);
            } else {
                System.err.println("WARNING: result for "+name+" for instance "+instanceID+
                        " was not numeric, although it should have been. Result is being uploaded as string.");
                
                query = (isGlobal) ? "INSERT INTO experiment_global_data" : "INSERT INTO experiment_data";
                if (isGlobal) {
                    query += "_strings(id, experiment_id, measurement, result) "
                    +"VALUES(NULL, "+experimentID+", '"+name+"', ?)";
                } else {
                    query += "_strings(id, experiment_id, instance, measurement, result) "
                    +"VALUES(NULL, "+experimentID+", "+instanceID+", '"+name+"', ?)";
                }
                stmt = conn.prepareStatement(query);
                stmt.setString(1, result.toString());
            }
            stmt.executeUpdate();
        } else {
    
            //upload string value
            if (isGlobal) {
                query += "_strings(id, experiment_id, measurement, result) "
                    +"VALUES(NULL, "+experimentID+", '"+name+"', ?)";
            } else {
                query += "_strings(id, experiment_id, instance, measurement, result) "
                    +"VALUES(NULL, "+experimentID+", "+instanceID+", '"+name+"', ?)";
            }
            
            PreparedStatement stmt = conn.prepareStatement(query);
            if (result != null) {
                stmt.setString(1, result.toString());//also works if result is a String itself
            } else {
                stmt.setNull(1, java.sql.Types.VARCHAR);
            }
            stmt.executeUpdate();
        }
    }
    
    
    /**
     * Connects to the database specified in the database.data file.
     * @throws SQLException 
     */
    public void connect(Properties props) throws SQLException {
        
        String serverName=props.getProperty("db.serverName");
        String userName=props.getProperty("db.userName");
        String password=props.getProperty("db.password");
        String port=props.getProperty("db.port");
        String dbName=props.getProperty("db.dbName");
        
        setUp(serverName, userName, password, port, dbName);
    }
    
    
    private void getConnection(String serverName, String userName, String password, String port, String dbName) throws SQLException {
        //DriverManager.registerDriver(new java.sql.com.mysql.jdbc.Driver ());
        conn = null;
        Properties connectionProps = new Properties();
        connectionProps.put("user", userName);
        connectionProps.put("password", password);

        conn = DriverManager.getConnection("jdbc:mysql://" +
           serverName +
           ":" + port + "/"+dbName,
           connectionProps);
         //System.err.println("Connected to database");
        
    }
    
    
    public static String getSystemRevision() {
        return BuildProperties.getVersion()+" "+BuildProperties.getBuild();
    }
    
    /*
    public Corpus loadCorpusFromDB(CorpusReference ref, InterpretedTreeAutomaton irtg) throws SQLException, ParseException, ParserException {
        reconnectIfNecessary();
        Corpus ret = new Corpus();
        Statement stmt = null;
        String query =
            "SELECT instance, interpretation, value " +
            "FROM corpus_data " +
            "WHERE corpusid = "+String.valueOf(ref.id)+" "+
            "ORDER BY instance";

        stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        int prevInstanceID = -1;
        Instance instance = new Instance();//instanciating just to make the compiler shut up
        while (rs.next()) {
            int instanceID = rs.getInt("instance");
            if (instanceID>prevInstanceID) {
                instance = new Instance();
                ret.addInstance(instance);
                prevInstanceID = instanceID;
            }
            String interp = rs.getString("interpretation");
            if (interp.equals(DERIV_TREE_NAME_IN_DB)) {
                instance.setDerivationTree(irtg.getAutomaton().getSignature().addAllSymbols(TreeParser.parse(rs.getString("value"))));
            } else {
                Map<String, Object> inputObjects = instance.getInputObjects();
                if (inputObjects == null) {
                    inputObjects = new HashMap<>();
                }
                inputObjects.put(interp, irtg.getInterpretation(interp).getAlgebra().parseString(rs.getString("value")));
                instance.setInputObjects(inputObjects);
            }
        }
        stmt.close();
        return ret;
    }
 
    
    
    public InterpretedTreeAutomaton loadGrammarFromDB(GrammarReference ref) throws SQLException, IOException {
        reconnectIfNecessary();
        String ext = "irtg";
        InputCodec<InterpretedTreeAutomaton> codec = InputCodec.getInputCodecByExtension(ext);
        
        String query = "SELECT value " +
                "FROM grammar_data " + 
                "WHERE grammar_id = "+String.valueOf(ref.id)+" " +
                "AND grammar_data.key = '"+STD_GRAMMAR_CONTENT_IN_DB+"'";
        //System.err.println(query);
        Statement stmt;
        
        stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        if (rs.next()) {
            InterpretedTreeAutomaton irtg = codec.read(rs.getAsciiStream("value"));
            stmt.close();
            return irtg;
        } else {
            System.err.println("IRTG not found");
            return null;
        }
        
    }
    */
    
    
    /*
    private void throwDBDataFileError(String additionalInfo) {
        String error = "Error loading database connection details. ";
        error += "Put database.data file in current directory, format is:\n";
        error += "serverName=xyz\n";
        error += "userName=xyz\n";
        error += "password=xyz\n";
        error += "port=xyz (use 3306 as default)\n";
        error += "dbName=xyz\n";
        error += "More details: "+additionalInfo;
        throw new RuntimeException(error);
    }
    */
    
    /**
     * Gets all tasks available in the given database. Use connect() beforehand
     * to connect to a database.
     * @return
     * @throws java.lang.Exception
     */
    /*
    public List<TaskReference> getAllTasks() throws Exception {
        reconnectIfNecessary();
        List<TaskReference> ret = new ArrayList<>();
        String query = "SELECT id, name FROM task";
        Statement stmt;

        stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        while (rs.next()) {
            ret.add(new TaskReference(rs.getInt("id"), rs.getString("name")));
        }
        return ret;
        
    }
    */
    
    /**
     * Gets all grammars available in the given database. Use connect() beforehand
     * to connect to a database.
     * @return
     * @throws SQLException
     */
    /*
    public List<GrammarReference> getAllGrammars() throws SQLException {
        reconnectIfNecessary();
        List<GrammarReference> ret = new ArrayList<>();
        String query = "SELECT id, name FROM grammar";
        Statement stmt;

        stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        while (rs.next()) {
            ret.add(new GrammarReference(rs.getInt("id"), rs.getString("name")));
        }
        return ret;
        
    }
    */
    
    /**
     * Gets all grammars available in the given database. Use connect() beforehand
     * to connect to a database.
     * @return
     * @throws SQLException
     */
    /*
    public List<CorpusReference> getAllCorpora() throws SQLException {
        reconnectIfNecessary();
        List<CorpusReference> ret = new ArrayList<>();
        String query = "SELECT id, name FROM corpus";
        Statement stmt;

        stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        while (rs.next()) {
            ret.add(new CorpusReference(rs.getInt("id"), rs.getString("name")));
        }
        return ret;
        
    }
    */
    

    /*
    public void uploadIRTG(InterpretedTreeAutomaton irtg, String name, String comment) throws SQLException {
        reconnectIfNecessary();
        //insert metadata
        //first check if name is already used
        String query = "SELECT id FROM grammar WHERE name = '"+name+"'";
        Statement stmt = conn.createStatement();
        ResultSet res = stmt.executeQuery(query);
        if (res.next()) {
            name += " "+Util.getCurrentDateAndTimeAsString();
        }
        //now add with hopefully unique name
        query = "INSERT INTO grammar(id, name, comment) " +
                "VALUES (NULL, '" + name+"', '"+comment+"')";
        stmt = conn.createStatement();
        stmt.executeUpdate(query);
        
        //now get the id
        query = "SELECT id FROM grammar WHERE name = '"+name+"'";//this uses that the name entry is unique!
        //stmt = conn.createStatement();
        res = stmt.executeQuery(query);
        int grammarID = -1;
        if (res.next()) {
            grammarID = res.getInt("id");
        }
        
        //insert interpretations
        for (String interpName : irtg.getInterpretations().keySet()) {
            query = "INSERT INTO grammar_data(id, grammar_id, grammar_data.key, grammar_data.value) " +
                "VALUES (NULL, " + grammarID+", '"+ interpName+"', '"+irtg.getInterpretation(interpName).getAlgebra().getClass().getName()+"')";
            //System.err.println(query);
            stmt.executeUpdate(query);
        }
        
        //now add the actual grammar
        query = "INSERT INTO grammar_data(id, grammar_id, grammar_data.key, grammar_data.value) " +
            "VALUES (NULL, " + grammarID+", '"+STD_GRAMMAR_CONTENT_IN_DB+"', '"+irtg.toString().replace("'", "\\'")+"')";
        //System.err.println(query);
        stmt.executeUpdate(query);
    }
    
    
    public int uploadCorpus(Corpus corpus, InterpretedTreeAutomaton irtg, String name, String comment, IntConsumer updateFunction) throws SQLException {
        reconnectIfNecessary();
        //insert metadata
        //first check if name is already used
        String query = "SELECT id FROM corpus WHERE name = '"+name+"'";
        Statement stmt = conn.createStatement();
        ResultSet res = stmt.executeQuery(query);
        if (res.next()) {
            //if name exists, add timestamp to make it unique
            name += " "+Util.getCurrentDateAndTimeAsString();
        }
        //now add with hopefully unique name
        query = "INSERT INTO corpus(id, name, comments) " +
                "VALUES (NULL, '" + name+"', '"+comment+"')";
        stmt = conn.createStatement();
        stmt.executeUpdate(query);
        
        query = "SELECT LAST_INSERT_ID() AS id";
        ResultSet idRes = stmt.executeQuery(query);
        if (!idRes.next()) {
            System.err.println("bad error in mysql: cannot get last inserted id");
        }
        int corpusID = idRes.getInt("id");
        
        //add all the instances
        int batchSize = 100;//arbitrarily chosen
        query = "INSERT INTO corpus_data(id, corpusid, instance, interpretation, value) VALUES";
        int instanceID = 1;
        for (Instance instance : corpus) {
            if (updateFunction != null) {
                updateFunction.accept(instanceID);
            }
            for (Entry<String, Object> entry : instance.getInputObjects().entrySet()) {
                //Algebra#representAsString does not seem to work as expected (gives wrong format for corpus) --EDIT nevermind, just bugged for GraphAlgebra
                String objectRep = (irtg == null) ? entry.getValue().toString() : irtg.getInterpretation(entry.getKey()).getAlgebra().representAsString(entry.getValue());
                //String objectRep = entry.getValue().toString();
                query +="(NULL, "+corpusID+", "+instanceID+", '"+entry.getKey()+"', '"+objectRep+"'), ";
            }
            if (instance.getDerivationTree()!= null && irtg != null) {
                query +="(NULL, "+corpusID+", "+instanceID+", '" + DERIV_TREE_NAME_IN_DB+"', '"+irtg.getAutomaton().getSignature().resolve(instance.getDerivationTree())+"), ";
            }
            if (instanceID % batchSize == 0) {
                stmt.executeUpdate(query.substring(0, query.length()-2));//remove the last ', '
                query = "INSERT INTO corpus_data(id, corpusid, instance, interpretation, value) VALUES";
            }
            instanceID++;
        }
        //catch the instances not done in a batch
        if (!query.equals("INSERT INTO corpus_data(id, corpusid, instance, interpretation, value) VALUES")) {
            stmt.executeUpdate(query.substring(0, query.length()-2));
        }
        return corpusID;
    }
    */
    
    /*
    public void uploadInstancePropertyBatch(int corpusID, List<List<Pair<String, Double>>> measurementsAndResults, int instanceIDOffset) throws SQLException {
        reconnectIfNecessary();
        String query = "INSERT INTO corpus_data_numeric(id, corpus_id, instance, measurement, result) VALUES ";
        int i = 1;
        for (List<Pair<String, Double>> mAndRforInstance : measurementsAndResults) {
            for (Pair<String, Double> mAndR : mAndRforInstance) {
                query += "(NULL, "+corpusID+", "+(i+instanceIDOffset)+", '"+mAndR.left+"', "+mAndR.right.toString()+"), ";
            }
            i++;
        }
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(query.substring(0, query.length()-2));//-2 to get rid of the extra comma and space in the end
    }
    
    public void clearInstanceProperties(int corpusID) throws SQLException {
        reconnectIfNecessary();
        String query = "DELETE FROM corpus_data_numeric WHERE corpus_id = "+corpusID;
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(query);
    }
    */
    
    /**
     * 
     * @param task
     * @return the task ID of the uploaded task
     * @throws SQLException 
     */
//    public TaskReference uploadTask(Task task) throws SQLException, UncheckedExecutionException {
//        reconnectIfNecessary();
//        String query = "INSERT INTO task(id, name, grammar_id, corpus_id, tree, irtg_tree, iterations, warmup) "
//                +"VALUES (NULL, '"+task.getName()+"', "+task.getGrammar().id+", "+task.getCorpus().id+", '"+OperationTreeParser.encode(task.getTree())
//                +"','"+OperationTreeParser.encode(task.getIRTGTree())+"', "+task.getIterations()+", "+task.getWarmup()+")";
//        Statement stmt = conn.createStatement();
//        stmt.executeUpdate(query);
//        query = "SELECT LAST_INSERT_ID() AS id";
//        ResultSet res = stmt.executeQuery(query);
//        if (!res.next()) {
//            System.err.println("bad error in mysql: cannot get last inserted id");
//        }
//        int taskID = res.getInt("id");
//        
//        //evaluators
//        task.getTree().dfs((Tree<ParsingOperation> node, List<Void> childrenValues) -> {
//            //local evaluators
//            List<NodeResultEvaluator> evals = node.getLabel().getSelectedInstanceEvaluators();
//            for (NodeResultEvaluator eval : evals) {
//                String queryHere = "INSERT INTO task_evaluators(id, task_id, pos_in_tree, name, is_global) "
//                        + "VALUES(NULL, "+taskID+", '"+task.getTree().getPathToNode(node)+"', '"+eval.getCode()+"', 0)";//is not global
//                try {
//                    stmt.executeUpdate(queryHere);
//                } catch (SQLException ex) {
//                    throw new UncheckedExecutionException(ex);
//                }
//            }
//            
//            List<BatchEvaluator> batchEvals = node.getLabel().getSelectedEvaluators();
//            for (BatchEvaluator eval : batchEvals) {
//                String queryHere = "INSERT INTO task_evaluators(id, task_id, pos_in_tree, name, is_global) "
//                        + "VALUES(NULL, "+taskID+", '"+task.getTree().getPathToNode(node)+"', '"+eval.getCode()+"', 1)";//is global
//                try {
//                    stmt.executeUpdate(queryHere);
//                } catch (SQLException ex) {
//                    throw new UncheckedExecutionException(ex);
//                }
//            }
//            return null;
//        });
//        return new TaskReference(taskID, task.getName());
//    }
    
    /*
    public GrammarReference getGrammarRefForTask(TaskReference taskRef) throws SQLException {
        reconnectIfNecessary();
        String query = "SELECT grammar_id FROM task WHERE id = "+taskRef.id;
        Statement stmt = conn.createStatement();
        ResultSet res = stmt.executeQuery(query);
        if (res.next()) {
            //first read out all the data, and close the statement
            int grammarID = res.getInt("grammar_id");
            res.close();
            GrammarReference grammar;
            query = "SELECT name FROM grammar WHERE id = "+grammarID;
            try (ResultSet grammarRes = stmt.executeQuery(query)) {
                if (grammarRes.next()) {
                    grammar = new GrammarReference(grammarID, grammarRes.getString("name"));
                } else {
                    throw new SQLException("Grammar id "+grammarID+" not found in database");
                }
            }
            return grammar;
        } else {
            throw new SQLException("task not found!");
        }
        
    }
    */
    
    
    /*
    double getNumericMeasurement(int experimentID, int instance, String measurement) throws SQLException {
        reconnectIfNecessary();
        String query = "SELECT result FROM experiment_data_numeric WHERE experiment_id = "+experimentID+" AND instance = "+instance +" AND measurement = '"+measurement+"'";
        Statement stmt = conn.createStatement();
        ResultSet res = stmt.executeQuery(query);
        if (res.next()) {
            //first read out all the data, and close the statement
            return res.getInt("result");
        } else {
            return -1;
        }
    }
    */
    
//    public int uploadExperimentStartDataUnknownTask(Task task, TaskReference possibleCorrespondingTaskRef, String comment, String hostname) throws SQLException, ParseException, IOException {
//        reconnectIfNecessary();
//        InterpretedTreeAutomaton irtg = loadGrammarFromDB(task.getGrammar());
//        //do not want to remap variables here
//        Task possibleCorrespondingTask = loadTaskFromDBWithIrtg(possibleCorrespondingTaskRef, irtg, ParsingOperation.getAllParsingOperations(irtg), ParsingOperation.getAllIRTGOperations(irtg), null);
//        
//        TaskReference taskRef;
//        if (task.equals(possibleCorrespondingTask)) {
//            taskRef = possibleCorrespondingTaskRef;
//        } else {
//            taskRef = uploadTask(task);
//        }
//        
//        return uploadExperimentStartData(taskRef, comment, hostname);
//    }
    
    
    
    /*
    public TaskReference getTaskReferenceByID(int taskID) throws SQLException {
        reconnectIfNecessary();
        String query = "SELECT name FROM task WHERE id = "+taskID;
        Statement stmt = conn.createStatement();
        ResultSet res = stmt.executeQuery(query);
        if (res.next()) {
            return new TaskReference(taskID, res.getString("name"));
        } else {
            throw new SQLException("Task name not found!");
        }
    }
    
    */
    
    /*
    public static class TaskReference {
        private final int id;
        private final String name;
        private TaskReference(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 59 * hash + this.id;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TaskReference other = (TaskReference) obj;
            return this.id == other.id;
        }
        
        public String getName() {
            return name;
        }
        
        public int getID() {
            return id;
        }
        
        @Override
        public String toString() {
            return id+": " +name;
        }
    }
*/
    /*
    private static class GrammarReference {
        private final int id;
        private final String name;
        private GrammarReference(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 59 * hash + this.id;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final GrammarReference other = (GrammarReference) obj;
            return this.id == other.id;
        }
        
        
        public String getName() {
            return name;
        }
        
        public int getID() {
            return id;
        }
        
        @Override
        public String toString() {
            return id+": " +name;
        }
    }
*/
    
    /*
    private static class CorpusReference {

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 71 * hash + this.id;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CorpusReference other = (CorpusReference) obj;
            return this.id == other.id;
        }
        
        
        public String getName() {
            return name;
        }
        
        public int getID() {
            return id;
        }
        
        private final int id;
        private final String name;
        private CorpusReference(int id, String name) {
            this.id = id;
            this.name = name;
        }
        
        @Override
        public String toString() {
            return id+": " +name;
        }
    }
    */
    
    
}
