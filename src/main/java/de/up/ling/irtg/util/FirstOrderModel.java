/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.saar.basic.StringTools;
import de.up.ling.tree.Tree;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class FirstOrderModel {
    private Map<String, Set<List<String>>> atomicInterpretations;
    private int maxArity;
    private Set<String> allIndividuals;
    private Set<List<String>> allIndividualsAsTuples;
    public static final String TOP = "T";

    public FirstOrderModel() {
        atomicInterpretations = new HashMap<>();
        maxArity = -1;
        allIndividuals = new HashSet<>();
        allIndividualsAsTuples = new HashSet<>();
    }
    
    

    public int getMaxArity() {
        return maxArity;
    }
    
    public Set<String> getUniverse() {
        return allIndividuals;
    }
    
    public Set<List<String>> getUniverseAsTuples() {
        return allIndividualsAsTuples;
    }
    
    public Set<List<String>> getInterpretation(String predicate) {
        if (predicate.equals(TOP)) {
            return allIndividualsAsTuples;
        }
        else if (! atomicInterpretations.containsKey(predicate)) {
            return new HashSet<>();
        }
        else {
            return atomicInterpretations.get(predicate);
        }
    }
    
    public void setInterpretation(String predicate, Set<List<String>> interpretation) {
        atomicInterpretations.put(predicate, interpretation);
    }

    /**
     * Reads the options from a Json string representation. The options for the
     * SetAlgebra consist in a specification of the universe and the
     * interpretations of the atomic concepts. For instance, the following
     * string says that "sleep" is a binary relation with the single element
     * (e,r1), whereas "rabbit" is a unary relation containing the elements r1
     * and r2.<p>
     *
     * {"sleep": [["e", "r1"]], "rabbit": [["r1"], ["r2"]], "white": [["r1"],
     * ["b"]], "in": [["r1","h"], ["f","h2"]], "hat": [["h"], ["h2"]] }
     *
     *
     * @param optionReader
     * @throws Exception
     */
    public static FirstOrderModel read(Reader optionReader) throws Exception {
        FirstOrderModel model = new FirstOrderModel();
        
        String optionString = StringTools.slurp(optionReader);
        Map<String, Set<List<String>>> atomicInterpretations = new HashMap<String, Set<List<String>>>();

        if (!optionString.trim().equals("")) {

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readValue(optionString, JsonNode.class);

            if (!root.isObject()) {
                throw new Exception("Invalid universe description: should be a map");
            } else {
                Iterator<String> preds = root.fieldNames();

                while (preds.hasNext()) {
                    String pred = preds.next();
                    Set<List<String>> tuples = new HashSet<List<String>>();
                    JsonNode child = root.get(pred);

                    if (!child.isArray()) {
                        throw new Exception("Invalid universe description: Entry '" + pred + "' should be a list.");
                    } else {
                        int childIndex = 0;
                        for (JsonNode tuple : child) {
                            List<String> tupleElements = new ArrayList<String>();
                            childIndex++;

                            if (!tuple.isArray()) {
                                throw new Exception("Invalid universe description: tuple " + childIndex + " under '" + pred + "' should be a list.");
                            } else {
                                for (JsonNode tupleEl : tuple) {
                                    tupleElements.add(tupleEl.textValue());
                                }
                            }

                            tuples.add(tupleElements);
                        }
                    }

                    atomicInterpretations.put(pred, tuples);
                }
            }
        }

        model.setAtomicInterpretations(atomicInterpretations);
        return model;
    }

    public final void setAtomicInterpretations(Map<String, Set<List<String>>> atomicInterpretations) {
        this.atomicInterpretations = atomicInterpretations;
//        maxArity = 0;

        allIndividuals.clear();
        allIndividualsAsTuples.clear();

        for (Set<List<String>> sls : atomicInterpretations.values()) {
            for (List<String> ls : sls) {
                allIndividuals.addAll(ls);
                maxArity = Math.max(maxArity, ls.size());

                for (String x : ls) {
                    List<String> tuple = new ArrayList<String>();
                    tuple.add(x);
                    allIndividualsAsTuples.add(tuple);
                }
            }
        }
    }

    /**
     * @return the atomicInterpretations
     */
    public Map<String, Set<List<String>>> getAtomicInterpretations() {
        return atomicInterpretations;
    }
    
    /**
     * Returns all true atoms in this model, as terms.
     * @return 
     */
    public Iterable<Tree<String>> getTrueAtoms() {
        List<Tree<String>> ret = new ArrayList<>();
        
        for( String f : atomicInterpretations.keySet() ) {
            for( List<String> args : atomicInterpretations.get(f) ) {
                Tree t = Tree.create(f, Util.mapToList(args, x -> Tree.create(x)));
                ret.add(t);
            }
        }
        
        return ret;
    }
}
