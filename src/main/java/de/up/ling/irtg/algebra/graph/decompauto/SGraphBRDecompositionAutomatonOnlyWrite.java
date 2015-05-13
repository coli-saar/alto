/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph.decompauto;

import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.graph.BRUtil;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.induction.IrtgInducer;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SGraphBRDecompositionAutomatonOnlyWrite extends SGraphBRDecompositionAutomatonMPFTrusting {

    
    private boolean actuallyWrite = true;
    
    
    private final int flushThreshold = 10000;
    private final Writer writer;
    private int ruleCount = 0;
    int count;
    public final boolean foundFinalState;
    //BitSet onlyFoundByRename = new BitSet();
    BitSet foundByR = new BitSet();
    BitSet foundByO = new BitSet();
    Int2ObjectMap<Set<String>> rulesOnlyFoundByRename = new Int2ObjectOpenHashMap<>();
    //R=rename, O=other than rename
    //for merge (need 2):
    Int2ObjectMap<IntSet> ORight2OLeft = new Int2ObjectOpenHashMap<>();
    Int2ObjectMap<IntSet> RRight2OLeft = new Int2ObjectOpenHashMap<>();
    //Int2ObjectMap<IntSet> OLeft2RRight = new Int2ObjectOpenHashMap<>();
    Int2ObjectMap<Int2ObjectMap<String>> needOR = new Int2ObjectOpenHashMap<>();
    Int2ObjectMap<Int2ObjectMap<String>> needOO = new Int2ObjectOpenHashMap<>();
    //need only one (from r, f, or merge):
    Int2ObjectMap<Set<String>> needR = new Int2ObjectOpenHashMap<>();
    Int2ObjectMap<Set<String>> needO = new Int2ObjectOpenHashMap<>();
    Object2IntMap<String> rule2Parent = new Object2IntOpenHashMap<>();//if the int is negative, the state comes from a rename rule.
    
    
    private void foundO(int state) throws Exception {
        
        foundByO.set(state);
        
        Int2ObjectMap<String> needFurtherO = needOO.get(state);
        if (needFurtherO != null) {
            for (int otherO : needFurtherO.keySet()) {
                String rule = needFurtherO.get(otherO);
                putStringInSetByInt(otherO, rule, needO);
            }
            needOO.remove(state);
            for (IntSet set : ORight2OLeft.values()) {
                set.remove(state);
            }
        }
        
        IntSet leftOs = ORight2OLeft.get(state);
        if (leftOs != null) {
            for (int leftO : leftOs) {
                Int2ObjectMap<String> needFurtherOLeft = needOO.get(leftO);
                if (needFurtherOLeft != null) {
                    String rule = needFurtherOLeft.get(state);
                    putStringInSetByInt(leftO, rule, needO);
                    needFurtherOLeft.remove(state);
                    if (needFurtherOLeft.isEmpty()) {
                        needOO.remove(leftO);
                    }
                }
            }
            ORight2OLeft.remove(state);
        }
        
        Int2ObjectMap<String> needFurtherR = needOR.get(state);
        if (needFurtherR != null) {
            for (int otherR : needFurtherR.keySet()) {
                String rule = needFurtherR.get(otherR);
                putStringInSetByInt(otherR, rule, needR);
            }
            needOR.remove(state);
            for (IntSet set : RRight2OLeft.values()) {
                set.remove(state);
            }
        }
        
        
        Set<String> rules = needO.get(state);
        if (rules != null) {
            for (String rule : rules) {
                writer.write(rule);
                int recState = rule2Parent.getInt(rule);
                
                if (recState >= 0) {
                    if (!foundByO.get(recState)) {
                        foundO(recState);
                    }
                } else {
                    if (!foundByR.get(-recState)) {
                        foundR(-recState);
                    }
                }
                rule2Parent.remove(rule);
            }
            needO.remove(state);
        }
    }
    
    private void foundR(int state) throws Exception {
        
        foundByR.set(state);
        
        IntSet leftOs = RRight2OLeft.get(state);
        if (leftOs != null) {
            for (int leftO : leftOs) {
                Int2ObjectMap<String> needFurtherOLeft = needOR.get(leftO);
                if (needFurtherOLeft != null) {
                    String rule = needFurtherOLeft.get(state);
                    putStringInSetByInt(leftO, rule, needO);
                    needFurtherOLeft.remove(state);
                    if (needFurtherOLeft.isEmpty()) {
                        needOR.remove(leftO);
                    }
                }
            }
            RRight2OLeft.remove(state);
        }
        
        
        Set<String> rules = needR.get(state);
        if (rules != null) {
            for (String rule : rules) {
                writer.write(rule);
                int recState = rule2Parent.getInt(rule);
                
                if (recState >= 0) {
                    if (!foundByO.get(recState)) {
                        foundO(recState);
                    }
                } else {
                    if (!foundByR.get(-recState)) {
                        foundR(-recState);
                    }
                }
                rule2Parent.remove(rule);
            }
            needR.remove(state);
        }
    }
    
    private static void putStringInSetByInt(int i, String s, Int2ObjectMap<Set<String>> setMap) {
        Set<String> set = setMap.get(i);
        if (set == null) {
            set = new HashSet<>();
            setMap.put(i, set);
        }
        set.add(s);
    }
    
    private static void putIntInSetByInt(int i, int value, Int2ObjectMap<IntSet> setMap) {
        IntSet set = setMap.get(i);
        if (set == null) {
            set = new IntOpenHashSet();
            setMap.put(i, set);
        }
        set.add(value);
    }
    
    private static void putStringInDepht2IntTrie(int l, int r, String s, Int2ObjectMap<Int2ObjectMap<String>> depth2Trie, Int2ObjectMap<IntSet> second2FirstIndex) {
        Int2ObjectMap<String> r2Value = depth2Trie.get(l);
        if (r2Value == null) {
            r2Value = new Int2ObjectOpenHashMap<>();
            depth2Trie.put(l, r2Value);
        }
        r2Value.put(r, s);
        putIntInSetByInt(r, l, second2FirstIndex);
    }

    public SGraphBRDecompositionAutomatonOnlyWrite(SGraph completeGraph, GraphAlgebra algebra, Signature signature, Writer writer) throws Exception {
        super(completeGraph, algebra, signature);
        stateInterner.setTrustingMode(true);
        this.writer = writer;
        count = 0;

        SGraphBRDecompAutoInstruments instr = new SGraphBRDecompAutoInstruments(this, completeGraphInfo.getNrSources(), completeGraphInfo.getNrNodes(), doBolinas());//maybe check algebra if it contains bolinasmerge?
        foundFinalState = instr.iterateThroughRulesBottomUp1Clean(algebra);
        
        //instr.iterateThroughRulesBottomUp1(algebra, true, false);
        //foundFinalState = false;
        //System.out.println(toString());
        
        

    }
    
    public void writeRule(Rule rule) throws Exception {

        
        boolean found = false;
        Object2IntMap<String> rule2ParentLocal = new Object2IntOpenHashMap<>();
        
        
        int parent = rule.getParent();
        int arity = rule.getArity();
        
        String ruleString = getRuleString(rule, false);
        
        switch(arity) {
            case 0:
                writer.write(ruleString);
                found = true;
                break;
            case 1:
                int child = rule.getChildren()[0];
                if (foundByO.get(child)) {
                    writer.write(ruleString);
                    found = true;
                } else {
                    if (rule.getLabel(this).startsWith(GraphAlgebra.OP_RENAME)) {
                        rule2ParentLocal.put(ruleString, -parent);//the minus to store that this was rename
                    } else {
                        rule2ParentLocal.put(ruleString, parent);
                    }
                    putStringInSetByInt(child, ruleString, needO);
                }
                break;
            case 2:
                int child1 = rule.getChildren()[0];
                int child2 = rule.getChildren()[1];
                String ruleStringR = getRuleString(rule, true);
                //Rule ruleSwapped = createRule(rule.getParent(), rule.getLabel(), new int[]{child2, child1}, 1);
                //String ruleStringRswapped = getRuleString(ruleSwapped, true);
                if (foundByO.get(child1)) {
                    if (foundByO.get(child2)) {
                        writer.write(ruleString);
                        found = true;
                    } else {
                        putStringInSetByInt(child2, ruleString, needO);
                        rule2ParentLocal.put(ruleString, parent);
                    }
                    if (foundByR.get(child2)) {
                        writer.write(ruleStringR);
                        found = true;
                    } else {
                        putStringInSetByInt(child2, ruleStringR, needR);
                        rule2ParentLocal.put(ruleStringR, parent);
                    }
                } else {
                    if (foundByO.get(child2)) {
                        putStringInSetByInt(child1, ruleString, needO);
                        rule2ParentLocal.put(ruleString, parent);
                    } else {
                        putStringInDepht2IntTrie(child1, child2, ruleString, needOO, ORight2OLeft);
                        rule2ParentLocal.put(ruleString, parent);
                    }
                    if (foundByR.get(child2)) {
                        putStringInSetByInt(child1, ruleStringR, needO);
                        rule2ParentLocal.put(ruleStringR, parent);
                    } else {
                        putStringInDepht2IntTrie(child1, child2, ruleStringR, needOR, RRight2OLeft);
                        rule2ParentLocal.put(ruleStringR, parent);
                    }
                }
                /*if (foundByO.get(child2)) {
                    if (foundByR.get(child1)) {
                        writer.write(ruleStringRswapped);
                    } else {
                        putStringInSetByInt(child1, ruleStringRswapped, needR);
                    }
                } else {
                    if (foundByR.get(child1)) {
                        putStringInSetByInt(child2, ruleStringRswapped, needO);
                    } else {
                        putStringInDepht2IntTrie(child2, child1, ruleStringRswapped, needOR, RRight2OLeft);
                    }                /*if (foundByO.get(child2)) {
                    if (foundByR.get(child1)) {
                        writer.write(ruleStringRswapped);
                    } else {
                        putStringInSetByInt(child1, ruleStringRswapped, needR);
                    }
                } else {
                    if (foundByR.get(child1)) {
                        putStringInSetByInt(child2, ruleStringRswapped, needO);
                    } else {
                        putStringInDepht2IntTrie(child2, child1, ruleStringRswapped, needOR, RRight2OLeft);
                    }
                }*/
        }
        
            
            if (found) {
                if (rule.getLabel(this).startsWith(GraphAlgebra.OP_RENAME)) {
                    foundR(parent);
                } else {
                    foundO(parent);
                }
            } else {
                rule2Parent.putAll(rule2Parent);
            }

            count++;
            if (count % flushThreshold == 0) {
                writer.flush();
            }
            
    }
    
    private String getRuleString(Rule rule, boolean useRenameInMerge) {
        String renameStatePrefix = "0R";
        String parentPrefix = rule.getLabel(this).startsWith(GraphAlgebra.OP_RENAME) ? renameStatePrefix : "";

        StringBuilder sb = new StringBuilder(Tree.encodeLabel(parentPrefix + encodeShort(rule.getParent())) + (finalStates.contains(rule.getParent()) ? "!" : "") + " -> " + Tree.encodeLabel(rule.getLabel(this)));

        boolean first = true;
        if (rule.getChildren().length > 0) {
            sb.append("(");

            for (int child : rule.getChildren()) {
                String childStateString;
                if (first) {
                    first = false;
                    childStateString = (child == 0) ? "null" : Tree.encodeLabel(encodeShort(child));
                } else {
                    childStateString = (child == 0) ? "null" : Tree.encodeLabel((useRenameInMerge ? renameStatePrefix : "") + encodeShort(child));
                    sb.append(", ");
                }

                sb.append(childStateString);
            }

            sb.append(")");
        }
        sb.append("\n");
        
        return sb.toString();
    }
    
    private String encodeShort(int stateId){
        return String.valueOf(stateId)+"_"+getStateForId(stateId).allSourcesToString();
    }



    @Override
    public boolean supportsTopDownQueries() {
        return true;
    }

    @Override
    public boolean supportsBottomUpQueries() {
        return true;
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        Iterable<Rule> res = calculateRulesBottomUpMPF(labelId, childStates);

        
        

        Iterator<Rule> it = res.iterator();
        while (it.hasNext()) {
            try{
                ruleCount++;
                double variable = Math.log10(ruleCount);
                if ((variable == Math.floor(variable)) && !Double.isInfinite(variable)) {
                    System.out.println(String.valueOf(ruleCount));
                }
                if (actuallyWrite) {
                    writeRule(it.next());
                } else {
                    it.next();
                }
            } catch (java.lang.Exception e) {
                System.err.println(e.toString());
            }

        }

        return res;
    }

    //finds out which parses have a final state, set actuallyWrite to false for that.
    public static void main(String[] args) throws Exception {
        if (args.length<5) {
            System.out.println("Need four arguments: corpusPath sourceCount startIndex stopIndex targetFolderPath");
            return;
        }
        String corpusPath = args[0];
        int sourceCount = Integer.valueOf(args[1]);
        int start = Integer.valueOf(args[2]);
        int stop = Integer.valueOf(args[3]);
        String targetPath = args[4];
        Reader corpusReader = new FileReader(corpusPath);
        IrtgInducer inducer = new IrtgInducer(corpusReader);
        inducer.getCorpus().sort(Comparator.comparingInt(inst -> inst.graph.getAllNodeNames().size()
        ));
        
        
        
        for (int i = start; i<stop; i++) {
            System.out.println(i);
            IrtgInducer.TrainingInstance instance = inducer.getCorpus().get(i);
            GraphAlgebra alg = new GraphAlgebra();
            BRUtil.makeIncompleteDecompositionAlgebra(alg, instance.graph, sourceCount);
            Writer rtgWriter = new FileWriter(targetPath+String.valueOf(instance.id)+".rtg");
            SGraphBRDecompositionAutomatonOnlyWrite auto = (SGraphBRDecompositionAutomatonOnlyWrite) alg.writeCompleteDecompositionAutomaton(instance.graph, rtgWriter);
            rtgWriter.close();
            
            System.out.println(String.valueOf(auto.ruleCount));
        }
        
        //resWriter.close();
        
    }
    
}
