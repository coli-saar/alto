/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.saar.basic.StringTools;
import de.up.ling.stream.SortedMergedStream;
import de.up.ling.stream.Stream;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * An iterator for the tree language of an automaton, sorted by descending
 * weight. This works even if the language is infinite.
 * 
 * @author koller
 */
public class SortedLanguageIterator<State> implements Iterator<WeightedTree> {
    private Map<Integer, StreamForState> streamForState;
    private Set<Integer> visitedStates;
    private static final boolean DEBUG = false;
    private TreeAutomaton<State> auto;
    private Stream<WeightedTree> globalStream;

    public SortedLanguageIterator(TreeAutomaton<State> auto) {
        this.auto = auto;
        streamForState = new HashMap<Integer, StreamForState>();
        
        // if necessary, this call to getAllStates could be replaced
        // by slightly more careful coding in this constructor and in
        // evaluatedUnevaluatedItems
        
        // combine streams for the different start symbols
        visitedStates = new HashSet<Integer>();
        globalStream = new SortedMergedStream<WeightedTree>(WeightedTreeComparator.INSTANCE);
        for( int q : auto.getFinalStates() ) {
            StreamForState sq = getStreamForState(q);
            ((SortedMergedStream<WeightedTree>) globalStream).addStream(sq);
        }
    }
    
    // retrieve the stream for the state, or create it by need
    private StreamForState getStreamForState(int q) {
        if( streamForState.containsKey(q)) {
            return streamForState.get(q);
        } else {
            StreamForState ret = new StreamForState(q);
            
            int count = 0;
            for( int label : auto.getLabelsTopDown(q)) {
                for( Rule rule : auto.getRulesTopDown(label, q) ) {
                    ret.addEntryForRule(rule);
                    count++;
                }
            }
            
            if(DEBUG) {
                System.err.println("created stream for state " + st(q));
                System.err.println(ret);
            }
            
            streamForState.put(q, ret);
            return ret;
        }
    }

    @Override
    public boolean hasNext() {
        if (DEBUG) {
            System.err.println("\n\nhasNext:");
            printEntireTable();
        }
        
        return globalStream.peek() != null;
    }

    @Override
    public WeightedTree next() {
        if (DEBUG) {
            System.err.println("\n\nnext:");
            printEntireTable();
        }
        
        return globalStream.pop();
    }

    /**
     * The stream of trees for a given state. This stream consists of a list
     * which represents the prefix of the stream of those trees that were
     * previously computed. The rest of the stream is represented by a merge of
     * the streams for the rules with which the state can be expanded top-down.
     *
     */
    private class StreamForState implements Stream<WeightedTree> {
        private List<WeightedTree> known;            // the trees that have been computed so far; guaranteed to be the k-best for this state
        private List<StreamForRule> ruleStreams;     // streams for the individual rules
        private int state;                         // the state which this stream represents
        private int nextTreeInStream;
        private SortedMergedStream<WeightedTree> mergedRuleStream;

        public StreamForState(int state) {
            known = new ArrayList<WeightedTree>();
            ruleStreams = new ArrayList<StreamForRule>();
            this.state = state;
            nextTreeInStream = 0;
            mergedRuleStream = new SortedMergedStream<WeightedTree>(WeightedTreeComparator.INSTANCE);
        }

        public void addEntryForRule(Rule rule) {
            StreamForRule s = new StreamForRule(rule);
            ruleStreams.add(s);
            mergedRuleStream.addStream(s);
        }
        
        /**
         * Returns the k-best tree for this state.
         *
         * @param k
         * @return null if the state has less than k trees or the k-th
         * best tree cannot be generated at this point
         */
        public WeightedTree getTree(int k) {
            if( DEBUG ) {
                System.err.println("getTree(" + k + ") for state " + st(state));
            }
            if (k < known.size()) {
                // If the k-best tree has already been computed,
                // simply return it.
                if(DEBUG) System.err.println("   -> " + k + "-best tree is known: " + formatWeightedTree(known.get(k)));
                return known.get(k);
            } else if (!visitedStates.contains(state)) {
                // Otherwise, attempt to compute the next best tree. This tree must
                // be the 1-best unseen tree for one of the rules with which
                // this state can be expanded. Notice that if the automaton
                // is recursive and we have already visited the state in our
                // top-down search for a next-best tree, we must stick to the
                // known trees and not attempt to expand them further. This avoids
                // running into infinite recursions, while guaranteeing optimality.

                // the algorithm should only ever try to expand the list
                // of known best trees one further
                assert k == known.size();
                
                visitedStates.add(state);
                WeightedTree bestTree = mergedRuleStream.pop();
                visitedStates.remove(state);

                if( bestTree != null ) {
                    known.add(bestTree);
                }
                
                if(DEBUG) System.err.println("   -> " + k + "-best tree for " + st(state) + " computed: " + formatWeightedTree(bestTree));
                return bestTree;
            } else {
                return null;
            }
        }
        
        private List<String> formatKnownTrees() {
            List<String> ret = new ArrayList<String>();
            for( WeightedTree wt : known ) {
                ret.add(formatWeightedTree(wt));
            }
            return ret;
        }

        @Override
        public String toString() {
            StringBuilder ret = new StringBuilder();

            ret.append("Stream for state " + st(state) + ": ");
            ret.append(" known trees: " + formatKnownTrees() + "\n");
            for (StreamForRule ft : ruleStreams) {
                ret.append(ft.toString() + "\n");
            }

            return ret.toString();
        }
        
        @Override
        public boolean isFinished() {
            return mergedRuleStream.isFinished();
        }

        @Override
        public WeightedTree peek() {
            return getTree(nextTreeInStream);
        }

        @Override
        public WeightedTree pop() {
            return getTree(nextTreeInStream++);
        }
    }

    /**
     * A stream of trees that can be built top-down, starting with a given
     * automaton rule. This stream does not usually contain _all_ trees that can
     * be built with the rule; only those that haven't already been requested by
     * getTree in the StreamForState class. The invariant is that the
     * combination of the "known" list in SFS, plus the StreamForRules for all
     * the state's top-down rules, is always the stream of trees for the state.
     */
    private class StreamForRule implements Stream<WeightedTree> {
        // the rule whose tree stream we represent
        public Rule rule;
        // A priority queue of trees generated by this rule, sorted
        // by weight. Each entry in the queue represents a tree that is
        // actually in the language, together with its weight.
        private PriorityQueue<EvaluatedItem> evaluatedItems;
        // A bag of further items, each of which may or may not be
        // expandable into an actual tree. 
        private List<UnevaluatedItem> unevaluatedItems;
        // Unevaluated items that were already added to the unevaluatedItems
        // list at some earlier time, and need not be re-added.
        private Set<UnevaluatedItem> previouslyDiscoveredItem;

        public StreamForRule(Rule rule) {
            this.rule = rule;

            evaluatedItems = new PriorityQueue<EvaluatedItem>();
            unevaluatedItems = new ArrayList<UnevaluatedItem>();
            previouslyDiscoveredItem = new HashSet<UnevaluatedItem>();

            // initalize the stream by adding an unevaluated item
            // <0, ..., 0>, indicating that the 1-best tree for this rule
            // can be built from the 1-best trees of the child states
            List<Integer> listOfZeroes = new ArrayList<Integer>();
            for (int i = 0; i < rule.getArity(); i++) {
                listOfZeroes.add(0);
            }

            UnevaluatedItem zeroItem = new UnevaluatedItem(listOfZeroes);
            unevaluatedItems.add(zeroItem);
            previouslyDiscoveredItem.add(zeroItem);
        }

        /**
         * Returns the best tree that is left over for this rule. This method
         * does not remove the tree from the stream.
         *
         * @return null if no further trees are available at this time
         */
        @Override
        public WeightedTree peek() {
            evaluateUnevaluatedItems();

            EvaluatedItem item = evaluatedItems.peek();

            if (item == null) {
                return null;
            } else {
                return item.getWeightedTree();
            }
        }

        /**
         * Returns the best tree that is left over for this rule, removes it
         * from the stream.
         *
         * @return null if no further trees are available at this time
         */
        @Override
        public WeightedTree pop() {
            evaluateUnevaluatedItems();

            EvaluatedItem ret = evaluatedItems.poll();

            if (ret == null) {
                return null;
            } else {
                // When the best tree is removed, we know that a next-best
                // tree for this rule might be built by taking the next-best
                // tree for one of the child states. These next-best trees
                // are called "variations", and they are queued up as unevaluated
                // items as long as they have never been discovered before.
                for (UnevaluatedItem it : ret.getItem().makeVariations()) {
                    if (previouslyDiscoveredItem.add(it)) {
                        unevaluatedItems.add(it);
                    }
                }
                
//                evaluateUnevaluatedItems();

                return ret.getWeightedTree();
            }
        }

        /**
         * Attempts to expand each unevaluated item into an evaluated item. This
         * needs to be done each time we want to access the best remaining tree.
         */
        private void evaluateUnevaluatedItems() {
            List<UnevaluatedItem> itemsToRemove = new ArrayList<UnevaluatedItem>();
            
            if( DEBUG ) {
                System.err.println("computing next tree for " + rule.toString(auto));
            }

            for (UnevaluatedItem item : unevaluatedItems) {
                double weight = 1;
                boolean available = true;
                boolean keepItemAround = true;
                List<Tree<Integer>> children = new ArrayList<Tree<Integer>>();

                // for each child, attempt to obtain the k-best tree, where k
                // is the index specified for this child in the unevaluated item
                for (int i = 0; i < rule.getArity(); i++) {
                    StreamForState stateStream = getStreamForState(rule.getChildren()[i]);
                    WeightedTree weightedTree = stateStream.getTree(item.positionsInChildLists.get(i));

                    if (weightedTree == null) {
                        // If no k-best tree is available for the child at this time, we can't
                        // evaluate the item. If furthermore the stream for the child state is
                        // finished, we can drop the unevaluated item altogether. If it is not
                        // finished, we need to keep the item around, because the necessary
                        // index might become available at some point in the future.
                        available = false;

                        if (stateStream.isFinished()) {
                            keepItemAround = false;
                        }
                    } else {
                        // Otherwise, update the weight and record the k-best tree
                        weight *= weightedTree.getWeight();
                        children.add(weightedTree.getTree());
                    }
                }

                if (available) {
                    // If the specified tree was available for each child,
                    // create the new tree and add it to the evaluated items.
                    WeightedTree wtree = new WeightedTree(Tree.create(rule.getLabel(), children), weight * rule.getWeight());
                    evaluatedItems.add(new EvaluatedItem(item, wtree));
                    itemsToRemove.add(item);
                } else {
                    // Otherwise, deal with non-expandable items as explained above.
                    if (keepItemAround) {
                        if (DEBUG) {
                            System.err.println(" * evaluate " + rule.toString(auto) + ": " + item + " cannot be expanded, keeping it for later");
                        }
                    } else {
                        itemsToRemove.add(item);
                        if (DEBUG) {
                            System.err.println(" * evaluate " + rule.toString(auto) + ": " + item + " cannot be expanded and stream is finished, deleting it");
                        }
                    }
                }
            }

            unevaluatedItems.removeAll(itemsToRemove);

            if (DEBUG) {
                System.err.println("- after evaluation -");
                System.err.println(this.toString());
            }
        }

        /**
         * Checks whether this stream is finished. A stream is finished
         * if it contains neither evaluated nor unevaluated items.
         * 
         * @return 
         */
        @Override
        public boolean isFinished() {
            return evaluatedItems.isEmpty() && unevaluatedItems.isEmpty();
        }

        @Override
        public String toString() {
            return " " + rule.toString(auto) + ":\n"
                    + "   evaluated items:   " + evaluatedItems + "\n"
                    + "   unevaluated items: " + unevaluatedItems;
        }
    }

    /**
     * An evaluated item, consisting of a weighted tree and the
     * original unevaluated item from which it was created.
     */
    class EvaluatedItem implements Comparable<EvaluatedItem> {
        private UnevaluatedItem item;
        private WeightedTree weightedTree;

        public EvaluatedItem(UnevaluatedItem item, WeightedTree wtree) {
            this.item = item;
            weightedTree = wtree;
        }

        public UnevaluatedItem getItem() {
            return item;
        }

        public WeightedTree getWeightedTree() {
            return weightedTree;
        }

        @Override
        public int compareTo(EvaluatedItem o) {
            // evalItem1 < evalItem2 if the tree in evalItem1 has a HIGHER weight than the tree in evalItem2
            return Double.compare(o.weightedTree.getWeight(), weightedTree.getWeight());
        }

        @Override
        public String toString() {
            return "[" + formatWeightedTree(weightedTree) + " (from " + item.toString() + ")]";
        }
    }

    /**
     * The unevaluated item <i1, ..., in> specifies that we should attempt
     * to build a tree for the given rule by combining the i1-best tree for the
     * first child state, the i2-best tree for the second child state, etc.
     */
    static class UnevaluatedItem {
        public List<Integer> positionsInChildLists;

        public UnevaluatedItem(List<Integer> positionsInChildLists) {
            this.positionsInChildLists = positionsInChildLists;
        }

        public List<UnevaluatedItem> makeVariations() {
            List<UnevaluatedItem> ret = new ArrayList<UnevaluatedItem>();

            for (int pos = 0; pos < positionsInChildLists.size(); pos++) {
                List<Integer> newPositions = new ArrayList<Integer>(positionsInChildLists);
                newPositions.set(pos, newPositions.get(pos) + 1);
                ret.add(new UnevaluatedItem(newPositions));
            }

            return ret;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 43 * hash + (this.positionsInChildLists != null ? this.positionsInChildLists.hashCode() : 0);
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
            final UnevaluatedItem other = (UnevaluatedItem) obj;
            if (this.positionsInChildLists != other.positionsInChildLists && (this.positionsInChildLists == null || !this.positionsInChildLists.equals(other.positionsInChildLists))) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "<" + StringTools.join(positionsInChildLists, ",") + ">";
        }
    }

    private void printEntireTable() {
        for (int q : streamForState.keySet()) {
            System.err.println("\nEntries for state " + st(q) + ":");
            System.err.println(streamForState.get(q).toString());
        }
    }
    
    private State st(int q) {
        return auto.getStateForId(q);
    }
    
    private String lb(int labelId) {
        return auto.getSignature().resolveSymbolId(labelId);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove items from this iterator.");
    }
    
    private static class WeightedTreeComparator implements Comparator<WeightedTree> {
        public static WeightedTreeComparator INSTANCE = new WeightedTreeComparator();
        
        @Override
        public int compare(WeightedTree w1, WeightedTree w2) {
            // streams that can't deliver values right now are dispreferred (= get minimum weight)
            double weight1 = (w1 == null) ? Double.NEGATIVE_INFINITY : w1.getWeight();
            double weight2 = (w2 == null) ? Double.NEGATIVE_INFINITY : w2.getWeight();
            
            // sort descending, i.e. streams with high weights go at the beginning of the list
            return Double.compare(weight2, weight1);
        }        
    }
    
//    public static void main(String[] args) throws ParseException, FileNotFoundException, IOException {
//        Reader reader = null;
//        
//        if( args[0].endsWith(".gz")) {
//            reader = new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(args[0]))));
//        } else {
//            reader = new FileReader(new File(args[0]));
//        }        
//        
//        TreeAutomaton auto = TreeAutomatonParser.parse(reader);
//        int k = Integer.parseInt(args[1]);
//        
//        long s = System.currentTimeMillis();
//        SortedLanguageIterator it = new SortedLanguageIterator(auto);
//        long e = System.currentTimeMillis();
//        
//        System.out.println("Init time: " + (e-s) + " millisec\n");
//        
////        System.out.println(auto);
//        
//        long totalStart = System.currentTimeMillis();
//        long numReadings = 0;
//        for( int i = 0; i < k && it.hasNext(); i++ ) {
//            long start = System.nanoTime();
//            WeightedTree w = it.next();
//            long end = System.nanoTime();
//            
//            System.out.println("" + (i+1) + ": " + w.getTree());
//            System.out.println("  [" + w.getWeight() + ", " + (end-start)/1000 + " microsec]\n");
//            numReadings++;
//        }
//        long totalEnd = System.currentTimeMillis();
//        
//        System.err.println("Enumerated " + numReadings + " trees in " + (totalEnd-totalStart) + " ms (" + (totalEnd-totalStart+0.0)/numReadings + " ms/tree)");
//    }
    
    private String formatWeightedTree(WeightedTree wt) {
        if( wt == null ) {
            return "!!!null wt!!!";
        } else {
            return auto.getSignature().resolve(wt.getTree()) + ":" + wt.getWeight();
        }
    }
}
