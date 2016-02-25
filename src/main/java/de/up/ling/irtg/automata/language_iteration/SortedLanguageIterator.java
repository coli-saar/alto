/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.language_iteration;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.WeightedTree;
import de.up.ling.irtg.util.ProgressListener;
import de.up.ling.stream.SortedMergedStream;
import de.up.ling.stream.Stream;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * An iterator for the tree language of an automaton, sorted by descending
 * weight. This works even if the language is infinite. By using the iterator to
 * enumerate the first k elements of the language, for some fixed k, you can
 * efficiently enumerate the k-best algorithms in time comparable to the Huang
 * &amp; Chiang k-best algorithm.
 *
 * @author koller
 */
public class SortedLanguageIterator<State> implements Iterator<WeightedTree> {

    private Int2ObjectMap<StreamForState> streamForState;
    private Set<Integer> visitedStates;
    private static final boolean DEBUG = false;
    private TreeAutomaton<State> auto;
    private Stream<EvaluatedItem> globalStream;
    private int progress;
    private ProgressListener progressListener;
    private RuleRefiner ruleRefiner;
    private ItemEvaluator itemEvaluator;
    private int beamSizePerState = 0;
    private double beamWidthPerState = 1;

    public SortedLanguageIterator(TreeAutomaton<State> auto) {
        this(auto, new IdentityRuleRefiner(), new TreeCombiningItemEvaluator());
    }

    public SortedLanguageIterator(TreeAutomaton<State> auto, RuleRefiner ruleRefiner, ItemEvaluator itemEvaluator) {
        this.auto = auto;
        this.ruleRefiner = ruleRefiner;
        this.itemEvaluator = itemEvaluator;

        streamForState = new Int2ObjectOpenHashMap<StreamForState>();

        this.progress = 0;

        // combine streams for the different start symbols
        visitedStates = new HashSet<Integer>();
        globalStream = new SortedMergedStream<EvaluatedItem>(EvaluatedItemComparator.INSTANCE);
        for (int q : auto.getFinalStates()) {
            StreamForState sq = getStreamForState(q);
            ((SortedMergedStream<EvaluatedItem>) globalStream).addStream(sq);
        }
    }

    // retrieve the stream for the state, or create it by need
    private StreamForState getStreamForState(int q) {
        if (streamForState.containsKey(q)) {
            return streamForState.get(q);
        } else {
            StreamForState ret = new StreamForState(q);

            int count = 0;
            for (int label : auto.getLabelsTopDown(q)) {
                for (Rule rule : auto.getRulesTopDown(label, q)) {
                    ret.addEntryForRule(rule);
                    count++;
                }
            }

            ret.ensureBeam();

            if (DEBUG) {
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

    /**
     * Returns the next tree from the iterator. Because the first tree may take
     * a while to compute (this operation initializes internal data structures),
     * you can pass a {@link ProgressListener} to track the progress.
     *
     * @param listener
     * @return
     */
    public WeightedTree next(ProgressListener listener) {
        progressListener = listener;
        progress = 0;
        WeightedTree ret = next();
        progressListener = null;
        return ret;
    }

    @Override
    public WeightedTree next() {
        if (DEBUG) {
            System.err.println("\n\nnext:");
            printEntireTable();
        }

        EvaluatedItem ei = globalStream.pop();

        if (ei == null) {
            return null;
        } else {
            return ei.getWeightedTree();
        }
    }

    /**
     * The stream of trees for a given state. This stream consists of a list
     * which represents the prefix of the stream of those trees that were
     * previously computed. The rest of the stream is represented by a merge of
     * the streams for the rules with which the state can be expanded top-down.
     *
     */
    private class StreamForState implements Stream<EvaluatedItem> {

        private SortedList<EvaluatedItem> known;   // the e-items that have been computed so far; hoped to be the k-best for this state
        private List<StreamForRule> ruleStreams;   // streams for the individual rules
        private int state;                         // the state which this stream represents
        private int nextItemInStream;
        private SortedMergedStream<EvaluatedItem> mergedRuleStream;
        private boolean sortingRequired = false;

        public StreamForState(int state) {
            known = new SortedList<>();
            ruleStreams = new ArrayList<StreamForRule>();
            this.state = state;
            nextItemInStream = 0;
            mergedRuleStream = new SortedMergedStream<EvaluatedItem>(EvaluatedItemComparator.INSTANCE);
        }

        public void addEntryForRule(Rule rule) {
            StreamForRule s = new StreamForRule(rule);
            ruleStreams.add(s);
            mergedRuleStream.addStream(s);
        }
        
        /**
         * Returns the k-best item for this state.
         *
         * @param k
         * @return null if the state has less than k items or the k-th best item
         * cannot be generated at this point
         */
        EvaluatedItem getEvaluatedItem(int k) {
            if (DEBUG) {
                System.err.println("getEvaluatedItem(" + k + ") for state " + st(state));
            }
            if (k < known.size()) {
                // If the k-best tree has already been computed,
                // simply return it.
                if (DEBUG) {
                    System.err.println("   -> " + k + "-best item is known: " + WeightedTree.formatWeightedTree(known.get(k).getWeightedTree(), auto.getSignature()));
                }
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
                EvaluatedItem bestItem = mergedRuleStream.pop();
                visitedStates.remove(state);

                if (bestItem != null) {
                    known.add(bestItem);
                }

                if (DEBUG) {
                    System.err.println("   -> " + k + "-best item for " + st(state) + " computed: " + WeightedTree.formatWeightedTree(bestItem.getWeightedTree(), auto.getSignature()));
                }
                return bestItem;
            } else {
                return null;
            }
        }

        private List<String> formatKnownTrees() {
            List<String> ret = new ArrayList<String>();
            for (EvaluatedItem wt : known) {
                ret.add(WeightedTree.formatWeightedTree(wt.getWeightedTree(), auto.getSignature()));
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
        public EvaluatedItem peek() {
            return getEvaluatedItem(nextItemInStream);
        }

        @Override
        public EvaluatedItem pop() {
            return getEvaluatedItem(nextItemInStream++);
        }

        /**
         * Initializes the stream to the desired beam size and width.
         *
         * @see SortedLanguageIterator#setBeamSizePerState(int)
         */
        private void ensureBeam() {
            if (beamSizePerState > 0) {
                EvaluatedItem firstItem = pop();

                if (firstItem != null) {
                    for (int i = 1; i < beamSizePerState; i++) {
                        EvaluatedItem item = pop();

                        if( item == null ) {
                            // ran out of items
                            break;
                        }
                        
                        if (item.getItemWeight() / firstItem.getItemWeight() < beamWidthPerState) {
                            // items became too unlikely
                            break;
                        }
                    }
                }
            }
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
    private class StreamForRule implements Stream<EvaluatedItem> {
        // The rule whose tree stream we represent
        public Rule rule;

        // The rules into which "rule" is refined. This is ordinarily a 
        // singleton consisting only of "rule", but may contain more elements
        // if we're using this class for cube pruning.
        private List<Rule> refinedRules;

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
            this.refinedRules = ruleRefiner.refine(rule);

            evaluatedItems = new PriorityQueue<EvaluatedItem>();
            unevaluatedItems = new ArrayList<UnevaluatedItem>();
            previouslyDiscoveredItem = new HashSet<UnevaluatedItem>();

            // initalize the stream by adding an unevaluated item
            // <0, ..., 0>, indicating that the 1-best tree for this rule
            // can be built from the 1-best trees of the child states
            List<Integer> listOfZeroes = new ArrayList<Integer>();
            for (int i = 0; i < rule.getArity() + 1; i++) {
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
        public EvaluatedItem peek() {
            evaluateUnevaluatedItems();

            EvaluatedItem item = evaluatedItems.peek();

            if (item == null) {
                return null;
            } else {
                return item;
            }
        }

        /**
         * Returns the best tree that is left over for this rule, removes it
         * from the stream.
         *
         * @return null if no further trees are available at this time
         */
        @Override
        public EvaluatedItem pop() {
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

                return ret;
            }
        }

        private static final int PROGRESS_GRANULARITY = 100000;
        private static final int PROGRESS_BAR_LENGTH = 10000000;

        /**
         * Attempts to expand each unevaluated item into an evaluated item. This
         * needs to be done each time we want to access the best remaining tree.
         */
        private void evaluateUnevaluatedItems() {
            List<UnevaluatedItem> leftoverUnevalItems = new ArrayList<>();

            if (DEBUG) {
                System.err.println("computing next tree for " + rule.toString(auto));
            }

            for (UnevaluatedItem item : unevaluatedItems) {
                boolean available = true;
                boolean keepItemAround = true;
                List<EvaluatedItem> children = new ArrayList<>();
                Rule refinedRule = null;

                if (progressListener != null) {
                    if (progress % PROGRESS_GRANULARITY == 0) {
                        progressListener.accept((progress + 1) % PROGRESS_BAR_LENGTH, PROGRESS_BAR_LENGTH, "Initializing language iterator: processed " + progress + " items");
                    }

                    progress++;
                }

                // The first entry in the tuple represents the label ID of the rule we are
                // generating. If it is unavailable, skip this item.
                if (item.getRefinedRulePosition() >= refinedRules.size()) {
                    keepItemAround = false;
                    available = false;
                } else {
                    // Otherwise, get the label ID
                    refinedRule = refinedRules.get(item.getRefinedRulePosition());

                    // for each child, attempt to obtain the k-best tree, where k
                    // is the index specified for this child in the unevaluated item
                    for (int i = 0; i < rule.getArity(); i++) {
                        StreamForState stateStream = getStreamForState(rule.getChildren()[i]);
                        EvaluatedItem evaluatedItemForChild = stateStream.getEvaluatedItem(item.getPositionInChildList(i));

                        if (evaluatedItemForChild == null) {
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
                            // Otherwise, record the k-best tree
                            children.add(evaluatedItemForChild);
                        }
                    }
                }

                if (available) {
                    // If the specified item was available for each child,
                    // create the new item and add it to the evaluated items.
                    evaluatedItems.add(itemEvaluator.evaluate(refinedRule, children, item));
//                    itemsToRemove.add(item);
                } else {
                    // Otherwise, deal with non-expandable items as explained above.
                    if (keepItemAround) {
                        if (DEBUG) {
                            System.err.println(" * evaluate " + rule.toString(auto) + ": " + item + " cannot be expanded, keeping it for later");
                        }
                        leftoverUnevalItems.add(item);
                    } else //                    itemsToRemove.add(item);
                    {
                        if (DEBUG) {
                            System.err.println(" * evaluate " + rule.toString(auto) + ": " + item + " cannot be expanded and stream is finished, deleting it");
                        }
                    }
                }
            }

            unevaluatedItems = leftoverUnevalItems;

            if (DEBUG) {
                System.err.println("- after evaluation -");
                System.err.println(this.toString());
            }
        }

        /**
         * Checks whether this stream is finished. A stream is finished if it
         * contains neither evaluated nor unevaluated items.
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

    String eviToString(EvaluatedItem evi) {
        return "[" + WeightedTree.formatWeightedTree(evi.getWeightedTree(), auto.getSignature()) + " (from " + evi.getItem().toString() + ")]";
    }

    /**
     * Gets the beam size per state.
     *
     * @see #setBeamSizePerState(int)
     * @return
     */
    public int getBeamSizePerState() {
        return beamSizePerState;
    }

    /**
     * Sets the beam size per state. When the stream for a state is initialized,
     * we start enumerating items until either the beamSize is reached, or the
     * weight ratio to the best item exceeds the beamWidth. Default is beamSize
     * = 0 (only items that are explicitly requested from the outside are
     * computed).
     *
     * @param beamSizePerState
     */
    public void setBeamSizePerState(int beamSizePerState) {
        this.beamSizePerState = beamSizePerState;
    }

    /**
     * Gets the beam width per state.
     *
     * @see #setBeamSizePerState(int)
     * @return
     */
    public double getBeamWidthPerState() {
        return beamWidthPerState;
    }

    /**
     * Sets the beam width per state.
     *
     * @see #setBeamSizePerState(int)
     * @param beamWidthPerState
     */
    public void setBeamWidthPerState(double beamWidthPerState) {
        this.beamWidthPerState = beamWidthPerState;
    }

}
