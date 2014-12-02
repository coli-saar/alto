/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.spark;

import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import java.io.FileReader;
import java.io.Reader;

import de.up.ling.irtg.induction.IrtgInducer;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;

/**
 *
 * @author jonas
 */
public class BRCorpusParser {

    
    private static ParsingResult workerFunction(ParsingArgument arg) throws Exception{
        CpuTimeStopwatch sw = new CpuTimeStopwatch();
      boolean res = IrtgInducer.parseInstance(arg.instance, arg.nrSources, false, true, false, "/data/csuserb/parsingResults/", sw);
      ParsingResult ret = new ParsingResult(res, sw.getTimeBefore(1), arg.instance.id);
      return ret;
    }

  public static void main(String[] args) throws Exception{
    
    int nrSources = 3;
      
    if (args.length >= 2) {
        nrSources = Integer.valueOf(args[1]);
    }  
      
    SparkConf conf = new SparkConf().setAppName("Boundary Representation Parser");
    JavaSparkContext sc = new JavaSparkContext(conf);
    int tasks = 1;
    if (args.length > 0) {
        tasks = Integer.valueOf(args[0]);
    }
    
    
    // load corpus
    Reader corpusReader = new FileReader("corpora/amr-bank-v1.3.txt");
    IrtgInducer inducer = new IrtgInducer(corpusReader);
    
    //inducer.corpusSerializable.
    
    /*inducer.corpusSerializable.sort(Comparator.comparingInt(inst -> {
        try {
            return (new GraphAlgebra()).parseString(inst.graph).getAllNodeNames().size();
        } catch (java.lang.Exception e) {
            System.err.println(e + ": Corpus not properly sorted as a result.");
            return 0;
        }
    }));*/
    

    int min = 0;
    int max = inducer.corpusSerializable.size();
    
    if (args.length >= 4) {
        min = Integer.valueOf(args[2]);
        max = Integer.valueOf(args[3]);
    }
    
    List<IrtgInducer.TrainingInstanceSerializable> relevantCorpus = ((List<IrtgInducer.TrainingInstanceSerializable>) inducer.corpusSerializable).subList(min, max);
    
    List<ParsingArgument> argumentList = new ArrayList<>();
    
    for (IrtgInducer.TrainingInstanceSerializable relevantCorpu : relevantCorpus) {
        argumentList.add(new ParsingArgument(nrSources, relevantCorpu));
    }

    // do the actual work
    JavaRDD<ParsingArgument> inputs = sc.parallelize(argumentList, tasks);

    List<ParsingResult> results = inputs.map( BRCorpusParser::workerFunction).collect();

    // IMPORTANT: "collect" is a Spark _action_, which means that it
    // triggers the distributed computations, collects the results,
    // and puts them in an Array that is available on the driver.
    // The following "foreach" iteration thus takes place on the driver.
    // Without the "collect", the foreach would run on the RDD and
    // thus be executed in the workers.
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss");
    Date date = new Date();
    String sDate = dateFormat.format(date); //2014/08/06_15:59:48
    
    String imacPath = "/Users/jonas/Documents/Logs";
    String ketosPath = "/data/csuserb/Logs";
    boolean onImac = false;
    try {
        File targetDir = new File(imacPath);
        onImac = targetDir.isDirectory();
    } catch (java.lang.Exception e) {
        onImac = false;
    }
    
    
    String logFilename;
    if (onImac) {
        logFilename = imacPath;
    } else {
        logFilename = ketosPath;
    }
    logFilename += "/BRCorpusParserLog"+sDate+".txt";
    
    Writer logWriter = new FileWriter(logFilename);
    
    for (ParsingResult pRes : results) {
        logWriter.write(pRes.toString());
    }
    logWriter.close();
}
  
  
  
    private static class ParsingResult implements Serializable{
        final boolean hasParse;
        final long decompTime;
        final int id;
        public ParsingResult(boolean hasParse, long decompTime, int id){
            this.hasParse = hasParse;
            this.decompTime = decompTime;
            this.id = id;
        }
        @Override
        public String toString(){
            return "ID " + id +"(" + hasParse + "); Decomposition Time: " + decompTime;
        }
    }
     
    private static class ParsingArgument implements Serializable{
        final int nrSources;
        final IrtgInducer.TrainingInstanceSerializable instance;
        
        public ParsingArgument(int nrSources, IrtgInducer.TrainingInstanceSerializable instance) {
            this.nrSources = nrSources;
            this.instance = instance;
        }
    }
}
