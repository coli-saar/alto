/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.taa;

import de.saar.basic.Pair;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.util.ObjectWithStringCode;
import de.up.ling.irtg.util.Evaluator;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author groschwitz
 */
public abstract class TAAOperationWrapper extends ObjectWithStringCode {
    
    public static final TAAOperationWrapper DUMMY = new DummyTAAOperationWrapper();
    protected static final String IRTG_SIGNATURE_NAME = "IRTG";
    protected static final String IRTG_STATETYPE_NAME = "IRTG";
    
    
    protected final int arity;
    //private final List<TAAOperationProperty> properties;
    private final List<TAAOperationImplementation> implementations;
    private final List<NodeResultEvaluator> evaluations;
    private final List<BatchEvaluator> batchEvaluations;

    public List<NodeResultEvaluator> getEvaluations() {
        return evaluations;
    }

    public List<BatchEvaluator> getBatchEvaluations() {
        return batchEvaluations;
    }
    
    
    
    protected abstract List<TAAOperationImplementation> makeImplementations(Object parameters);
    
    public String signatureType(List<String> childSignatureTypes) {
        return null;
    }
    
    public String stateType(List<String> childStateTypes) {
        return null;
    }
    
    private TAAOperationWrapper(int arity, Object implementationsParameters, List<NodeResultEvaluator> evaluations) {
        //this.properties = new ArrayList<>();
        this.arity = arity;
        this.implementations = makeImplementations(implementationsParameters);//this is not clean, but I haven't found a better solution yet.
        if (evaluations == null) {
            this.evaluations = new ArrayList<>();
        } else {
            this.evaluations = evaluations;
        }
        this.evaluations.add(NodeResultEvaluator.makeLocalTimeEvaluator());
        this.evaluations.add(NodeResultEvaluator.makeCumulativeTimeEvaluator());
        this.batchEvaluations = BatchEvaluator.getBatchEvaluatorsFromInstanceEvaluators(this.evaluations);
    }
    
    /*public final Object apply(String implementationName, List<Object> input, Instance instanceFromCorpus) {
        if (input.size() == arity) {
            return implementations.get(implementationName).apply(input, instanceFromCorpus);//internalApply(input, instanceFromCorpus);
        } else {
            throw new IllegalArgumentException("This GraphOperationNode has been given the wrong number of input children!");
        }
    }*/
    
    //protected abstract Object internalApply(List<Object> input, Instance instanceFromCorpus);
    
    /**
     * This is the name of this operation shown to the user. Other than the code,
     * you can change this without threatening backward compatibility.
     * @return 
     */
    public abstract String getName();
    
    /**
     * This is the code identifying the class of this Operation. Changing it removes
     * backward compatibility when parsing TAATrees from strings.
     * @return 
     */
    @Override
    public abstract String getCode();
    
    public int getArity() {
        return arity;
    }
    
    /*public TAAOperationImplementation getDefaultImplementation() {
        return implementations.get(0);
    }*/
    
    public TAAOperationImplementation getDefaultImplementation() {
        if (implementations.size() > 0) {
            return implementations.get(0);
        } else {
            System.err.println("no implementation in TAAOperationWrapper!");
            return null;
        }
    }
    
    
    @Override
    public String toString() {
        return getName();
    }
    
    public List<TAAOperationImplementation> getImplementations() {
        return implementations;
    }
    
    
    /**
     * Two TAAOperationWrapper objects are equal iff their name is equal. Be careful!
     * @param other
     * @return 
     */
    /*@Override
    public boolean equals(Object other) {
        if (other != null && other.getClass().equals(TAAOperationWrapper.class)) {
            return getName().equals(((TAAOperationWrapper)other).getName());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }*/
    
    public static class DummyTAAOperationWrapper extends TAAOperationWrapper {
        
        public DummyTAAOperationWrapper() {
            super(0, null, null);
            
        }

        /*@Override
        protected Object internalApply(List<Object> input, Instance instanceFromCorpus) {
            return new ConcreteTreeAutomaton<>();
        }*/

        @Override
        public String getName() {
            return "              ";//"Add operation here!";//not sure if this is the smartest thing...
        }

        @Override
        protected List<TAAOperationImplementation> makeImplementations(Object implementationsParameters) {
            List<TAAOperationImplementation> ret = new ArrayList<>();
            ret.add(new TAAOperationImplementation("none", "none", pair -> null, null, null, null));
            return ret;
        }

        @Override
        public String getCode() {
            return "dummy";
        }

        
    }
    
    
    public static class GetIrtgAutomaton extends TAAOperationWrapper {

        //private final InterpretedTreeAutomaton irtg;
        
        public GetIrtgAutomaton() {
            super(0, null, null);
            //this.irtg = irtg;
        }

        /*@Override
        public Object internalApply(List<Object> input, Instance instanceFromCorpus) {
            return irtg.getAutomaton();
        }*/

        @Override
        public String stateType(List<String> childStateTypes) {
            return IRTG_STATETYPE_NAME;
        }

        @Override
        public String signatureType(List<String> childSignatureTypes) {
            return IRTG_SIGNATURE_NAME;
        }

        
        
        @Override
        public String getName() {
            return ("IRTG automaton");
        }

        @Override
        protected List<TAAOperationImplementation> makeImplementations(Object parameters) {
            List<TAAOperationImplementation> ret = new ArrayList<>();
            try {
            ret.add(new TAAOperationImplementation("Standard", "std", pair -> pair.left.getAutomaton(),
                    null, new Class[0], InterpretedTreeAutomaton.class.getMethod("getAutomaton", new Class[0]).getReturnType()
                    ));// cannot take the stored irtg since this is called in the constructor of super; as a result do not store irtg
            } catch (java.lang.Exception e) {
                System.err.println("could not initiate IrtgAutomaton TAAOperationWrapper: "+e.toString());
            }
            return ret;
        }

        @Override
        public String getCode() {
            return "irtg";
        }
        
    }
    
    public static class Invhom extends TAAOperationWrapper {

        //private final InterpretedTreeAutomaton irtg;
        private final String interpName;
        
        public Invhom(String interpName) {
            super(1, interpName, null);
            //this.irtg = irtg;
            this.interpName = interpName;
        }

        @Override
        public String stateType(List<String> childStateTypes) {
            if (childStateTypes.size() == 1) {
                return childStateTypes.get(0);
            } else {
                return "ERROR";
            }
        }
        
        @Override
        public String signatureType(List<String> childSignatureTypes) {
            return IRTG_SIGNATURE_NAME;
        }
        
        /*@Override
        public Object internalApply(List<Object> input, Instance instanceFromCorpus) {
            return irtg.getInterpretation(interpName).invhom((TreeAutomaton)input.get(0));
        }*/

        @Override
        public String getName() {
            return (interpName + ": inverse homomorphism");
        }

        @Override
        protected List<TAAOperationImplementation> makeImplementations(Object parameters) {
            List<TAAOperationImplementation> ret = new ArrayList<>();
            try {
                ret.add(new TAAOperationImplementation("Standard", "std", (Pair<InterpretedTreeAutomaton, Pair<List<Object>, Instance>> pair) -> {
                        return pair.left.getInterpretation((String)parameters).invhom((TreeAutomaton)pair.right.left.get(0));
                    }, null, new Class[]{TreeAutomaton.class}, Interpretation.class.getMethod("invhom", new Class[]{TreeAutomaton.class}).getReturnType()));// cannot take the stored irtg since this is called in the constructor of super
                
            } catch (NoSuchMethodException | SecurityException ex) {
                System.err.println("could not initiate Invhom TAAOperationWrapper: "+ex.toString());
            }
            return ret;
        }

        @Override
        public String getCode() {
            return interpName+"Invhom";
        }
        
    }
    
    public static class Intersect extends TAAOperationWrapper {
        
        public Intersect() {
            super(2, null, null);
        }

        /*@Override
        public Object internalApply(List<Object> input, Instance instanceFromCorpus) {
            return ((TreeAutomaton)input.get(0)).intersect((TreeAutomaton)input.get(1));
        }*/

        @Override
        public String stateType(List<String> childStateTypes) {
            if (childStateTypes.size() == 2) {
                return "("+childStateTypes.get(0)+") x (" + childStateTypes.get(1)+")";
            } else {
                return "ERROR";
            }
        }

        @Override
        public String signatureType(List<String> childSignatureTypes) {
            if (childSignatureTypes.size() == 2) {
                if (childSignatureTypes.get(0).equals(childSignatureTypes.get(1))) {
                    return childSignatureTypes.get(0);
                } else {
                    return "("+childSignatureTypes.get(0)+") intersect (" + childSignatureTypes.get(1)+")";
                }
            } else {
                return "ERROR";
            }
        }

        
        
        @Override
        public String getName() {
            return ("intersect");
        }

        @Override
        protected List<TAAOperationImplementation> makeImplementations(Object parameters) {
            List<TAAOperationImplementation> ret = new ArrayList<>();
            try {
                ret.add(new TAAOperationImplementation("Standard", "std", (Pair<InterpretedTreeAutomaton, Pair<List<Object>, Instance>> pair) -> {
                        return ((TreeAutomaton)pair.right.left.get(0)).intersect((TreeAutomaton)pair.right.left.get(1));
                    }, null,new Class[]{TreeAutomaton.class, TreeAutomaton.class}, TreeAutomaton.class.getMethod("intersect", new Class[]{TreeAutomaton.class}).getReturnType()));// cannot take the stored irtg since this is called in the constructor of super
                
            } catch (NoSuchMethodException | SecurityException ex) {
                System.err.println("could not initiate Invhom TAAOperationWrapper: "+ex.toString());
            }
            return ret;
        }

        @Override
        public String getCode() {
            return "intersect";
        }
        
    }
    
    public static class Viterbi extends TAAOperationWrapper {

        public Viterbi() {
            super(1, null, null);//potentially add evaluation here that compares to derivation tree in annotated instance
        }
        
        /*@Override
        protected Object internalApply(List<Object> input, Instance instanceFromCorpus) {
            return ((TreeAutomaton)input.get(0)).viterbi();
        }*/

        @Override
        public String getName() {
            return "Viterbi";
        }

        @Override
        protected List<TAAOperationImplementation> makeImplementations(Object parameters) {
            List<TAAOperationImplementation> ret = new ArrayList<>();
            try {
                ret.add(new TAAOperationImplementation("Standard", "std", pair -> ((TreeAutomaton)pair.right.left.get(0)).viterbi(), null,
                    new Class[]{TreeAutomaton.class}, TreeAutomaton.class.getMethod("viterbi", new Class[0]).getReturnType()));// cannot take the stored irtg since this is called in the constructor of super
            } catch (java.lang.Exception e) {
                System.err.println("could not initiate Viterbi TAAOperationWrapper: "+e.toString());
            }
            return ret;
        }

        @Override
        public String stateType(List<String> childStateTypes) {
            return null;//no states here!
        }

        @Override
        public String signatureType(List<String> childSignatureTypes) {
            if (childSignatureTypes.size() == 1) {
                return childSignatureTypes.get(0);
            } else {
                return "ERROR";
            }
        }

        @Override
        public String getCode() {
            return "viterbi";
        }
        
        
    }
    
    
    public static class GetAlgebraDecompositionAutomaton extends TAAOperationWrapper {

        //private final Algebra alg;
        private final String interpName;
        
        public GetAlgebraDecompositionAutomaton(InterpretedTreeAutomaton baseIRTG, String interpName) {
            super(0, new Pair(baseIRTG.getInterpretation(interpName).getAlgebra(), interpName), null);
            //this.alg = irtg.getInterpretation(interpName).getAlgebra();
            this.interpName = interpName;
        }
        

        /*@Override
        public Object internalApply(List<Object> input, Instance instanceFromCorpus) {
            return alg.decompose(instanceFromCorpus.getInputObjects().get(interpName));
        }*/

        @Override
        public String getName() {
            return (interpName + ": decompose");
        }

        @Override
        protected List<TAAOperationImplementation> makeImplementations(Object parameters) {
            Pair<Algebra, String> algAndInterpName = (Pair<Algebra, String>)parameters;
            
            //see documentation of Algebra#getDecompositionImplementations for more infomation
            List<Pair<Pair<String,String>,  Pair<Function<Object, Object>, Class>>> implementations = algAndInterpName.left.getDecompositionImplementations(algAndInterpName.right);
            
            List<TAAOperationImplementation> ret = new ArrayList<>();
            
            for (Pair<Pair<String,String>,  Pair<Function<Object, Object>, Class>> entry : implementations) {
                ret.add(new TAAOperationImplementation(entry.left.left, entry.left.right, (Pair<InterpretedTreeAutomaton, Pair<List<Object>, Instance>> pair) -> {
                        List<Pair<Pair<String,String>,  Pair<Function<Object, Object>, Class>>> implementationsHere =
                                pair.left.getInterpretation(algAndInterpName.right).getAlgebra().getDecompositionImplementations(algAndInterpName.right);
                        Function<Object, Object> actualImplementation = null;
                        for (Pair<Pair<String,String>,  Pair<Function<Object, Object>, Class>> stringsAndFunction : implementationsHere) {
                            if (stringsAndFunction.left.right.equals(entry.left.right)) {
                                actualImplementation = stringsAndFunction.right.left;
                            }
                        }
                        return actualImplementation.apply(pair.right.right.getInputObjects().get(interpName));
                    }, null, new Class[0], entry.right.right));
            }
            
            
            return ret;
        }

        @Override
        public String stateType(List<String> childStateTypes) {
            return interpName;
        }

        @Override
        public String signatureType(List<String> childSignatureTypes) {
            return interpName;
        }

        @Override
        public String getCode() {
            return interpName+"Decomp";
        }
        
    }
    
    
    
    public static class EvaluateWithAlgebra extends TAAOperationWrapper {

        //private final Algebra alg;
        private final String interpName;
        
        public EvaluateWithAlgebra(InterpretedTreeAutomaton irtg, String interpName) {
            super(1, interpName, (List<NodeResultEvaluator>)irtg.getInterpretation(interpName).getAlgebra().getEvaluationMethods().stream().map(obj -> {
                Evaluator eval = (Evaluator)obj;
                return new NodeResultEvaluator(eval.getName(), eval.getCode()) {
                    
                    @Override
                    public Pair<Double, Double> evaluate(TAANode node, Instance gold) {
                        return eval.evaluate(node.getLastResult(), gold.getInputObjects().get(interpName));
                    }
                };
            }).collect(Collectors.toList()));
            //this.alg = irtg.getInterpretation(interpName).getAlgebra();
            this.interpName = interpName;
        }
        

        /*@Override
        public Object internalApply(List<Object> input, Instance instanceFromCorpus) {
            return alg.decompose(instanceFromCorpus.getInputObjects().get(interpName));
        }*/

        @Override
        public String getName() {
            return (interpName+": evaluate");
        }

        @Override
        protected List<TAAOperationImplementation> makeImplementations(Object parameters) {
            String interpNameHere = (String)parameters;
            
            
            List<TAAOperationImplementation> ret = new ArrayList<>();
            
            ret.add(new TAAOperationImplementation("Standard", "std", irtgAndInputAndInstance -> 
                    irtgAndInputAndInstance.left.getInterpretation(interpNameHere).getAlgebra().evaluate((Tree)irtgAndInputAndInstance.right.left.get(0))
                            , null, new Class[]{Tree.class}, Object.class));
            
            
            return ret;
        }

        @Override
        public String stateType(List<String> childStateTypes) {
            return interpName;
        }

        @Override
        public String signatureType(List<String> childSignatureTypes) {
            return null;//does not have signature
        }

        @Override
        public String getCode() {
            return interpName+"Evaluate";
        }
    }
    
    public static class ApplyHomomorphismToTree extends TAAOperationWrapper {

        //private final Algebra alg;
        private final String interpName;
        
        public ApplyHomomorphismToTree(InterpretedTreeAutomaton irtg, String interpName) {
            super(1, interpName, null);
            //this.alg = irtg.getInterpretation(interpName).getAlgebra();
            this.interpName = interpName;
        }
        

        /*@Override
        public Object internalApply(List<Object> input, Instance instanceFromCorpus) {
            return alg.decompose(instanceFromCorpus.getInputObjects().get(interpName));
        }*/

        @Override
        public String getName() {
            return interpName + ": homomorphism";
        }

        @Override
        protected List<TAAOperationImplementation> makeImplementations(Object parameters) {
            String interpNameHere = (String)parameters;
            
            
            List<TAAOperationImplementation> ret = new ArrayList<>();
            
            ret.add(new TAAOperationImplementation("Standard", "std", irtgAndInputAndInstance ->
                    irtgAndInputAndInstance.left.getInterpretation(interpNameHere).getHomomorphism().apply((Tree)irtgAndInputAndInstance.right.left.get(0)),
                    null, new Class[]{Tree.class}, Tree.class));
            
            
            return ret;
        }

        @Override
        public String stateType(List<String> childStateTypes) {
            return interpName;
        }

        @Override
        public String signatureType(List<String> childSignatureTypes) {
            return interpName;
        }

        @Override
        public String getCode() {
            return interpName+"HomTree";
        }
    }
    
    public static class FullIRTG extends TAAOperationWrapper {

        public FullIRTG() {
            super(0, null, null);
        }

        @Override
        protected List<TAAOperationImplementation> makeImplementations(Object parameters) {
            List<TAAOperationImplementation> ret = new ArrayList<>();
            ret.add(new TAAOperationImplementation("Standard", "std", pair -> pair.left, null, new Class[0], InterpretedTreeAutomaton.class));
            return ret;
        }

        @Override
        public String getName() {
            return "Full IRTG";
        }

        @Override
        public String getCode() {
            return "fullIRTG";
        }
        
    }
    
    public static class FilteredIRTG extends TAAOperationWrapper {

        private final String interpName;
        
        public FilteredIRTG(String interpretation) {
            super(0, interpretation, null);
            interpName = interpretation;
        }

        @Override
        protected List<TAAOperationImplementation> makeImplementations(Object parameters) {
            String interpNameHere = (String)parameters;
            List<TAAOperationImplementation> ret = new ArrayList<>();
            ret.add(new TAAOperationImplementation("Checking constants", "checkConst",
                    pair -> pair.left.filterForAppearingConstants(interpNameHere, pair.right.right.getInputObjects().get(interpNameHere)),
                    null, new Class[0], InterpretedTreeAutomaton.class));
            return ret;
        }

        @Override
        public String getName() {
            return interpName+": IRTG filtered per instance";
        }

        @Override
        public String getCode() {
            return interpName+"InstanceFilteredIRTG";
        }
        
    }
    
    
    
    public static List<TAAOperationWrapper> getAllTAAOperations(InterpretedTreeAutomaton irtg) {
        List<TAAOperationWrapper> ret = new ArrayList<>();
        
        //dummy first
        ret.add(DUMMY);
        
        //interpretation specific
        for (String interpName : irtg.getInterpretations().keySet()) {
            ret.add(new EvaluateWithAlgebra(irtg, interpName));
            ret.add(new ApplyHomomorphismToTree(irtg, interpName));
            ret.add(new GetAlgebraDecompositionAutomaton(irtg, interpName));
            ret.add(new Invhom(interpName));
        }
        ret.add(new GetIrtgAutomaton());
        
        //general operations
        ret.add(new Intersect());
        
        ret.add(new Viterbi());
        
        return ret;
    }
    
    public static List<TAAOperationWrapper> getAllIRTGOperations(InterpretedTreeAutomaton irtg) {
        List<TAAOperationWrapper> ret = new ArrayList<>();
        
        //full IRTG
        ret.add(new FullIRTG());
        
        //interpretation specific
        for (String interpName : irtg.getInterpretations().keySet()) {
            ret.add(new FilteredIRTG(interpName));
        }
        
        return ret;
    }
    
    
}
