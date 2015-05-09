/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.index;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.util.ArrayInt2IntMap;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import de.up.ling.irtg.util.IntInt2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Predicate;

/**
 *
 * @author koller
 */
class ArrayTopDownIndex implements TopDownRuleIndex, Serializable {

    private final List<Rule> rules;
    private boolean dirty;

    private final Int2IntMap parentStartIndex;
    private final IntInt2IntMap parentLabelStartIndex;

    private final static CpuTimeStopwatch stopwatch = new CpuTimeStopwatch();

    private static final Comparator<Rule> RULE_COMPARATOR1 = Comparator.comparingInt(rule -> rule.getParent());
    private static final Comparator<Rule> RULE_COMPARATOR = RULE_COMPARATOR1.thenComparingInt(rule -> rule.getLabel());

    public ArrayTopDownIndex() {
        rules = new ArrayList<>();
        dirty = true;

        parentStartIndex = new ArrayInt2IntMap();
        parentStartIndex.defaultReturnValue(-1);

        parentLabelStartIndex = new IntInt2IntMap();
        parentLabelStartIndex.setDefaultReturnValue(-1);
    }

    @Override
    public void add(Rule rule) {
        rules.add(rule);
        dirty = true;
    }

    private static String getStackTrace() {
        try {
            throw new Exception();
        } catch (Exception e) {
            StringJoiner ret = new StringJoiner("\n");
            Arrays.asList(e.getStackTrace()).forEach(x -> ret.add(x.toString()));
            return ret.toString();
        }
    }

    private void processNewTopDownRules() {
        if (dirty) {
//            System.err.println("\n\n*** process new, from " + getStackTrace());
            System.err.println("\n*** process new, " + rules.size() + " unorganized rules");

            stopwatch.record(0);

            rules.sort(RULE_COMPARATOR);
            stopwatch.record(1);

//            System.err.println("sorted rules: " + rules);
            parentStartIndex.clear();
            parentLabelStartIndex.clear();

            int previousParent = -1;
            int previousLabel = -1;
            int listWritingPosition = 0;
            Rule previousRule = null;
            

            for (int i = 0; i < rules.size(); i++) {
                Rule ruleHere = rules.get(i);

                if (!ruleHere.equals(previousRule)) {  // skip duplicate rules
                    rules.set(listWritingPosition, ruleHere);

                    int parentHere = ruleHere.getParent();
                    int labelHere = ruleHere.getLabel();

                    if (parentHere != previousParent) {
                        // found first rule with new parent
                        parentStartIndex.put(parentHere, listWritingPosition);
                        parentLabelStartIndex.put(parentHere, labelHere, listWritingPosition);

                        previousParent = parentHere;
                        previousLabel = labelHere;
                    } else if (labelHere != previousLabel) {
                        parentLabelStartIndex.put(parentHere, labelHere, listWritingPosition);
                        previousLabel = labelHere;
                    }

                    previousRule = ruleHere;
                    listWritingPosition++;
                }
            }
            stopwatch.record(2);

            // remove positions in the rules list that were never copied into
            rules.subList(listWritingPosition, rules.size()).clear();

            stopwatch.record(3);
//            stopwatch.printMilliseconds("sort", "index", "free");

            dirty = false;

            System.err.println("\n --> process new done, " + rules.size() + " rules in index");
//            
//            System.err.println("cleaned rules: " + rules);
//            System.err.println("parentmap: " + parentStartIndex);
//            System.err.println("parent-label map: " + parentLabelStartIndex);
        }
    }

    private class RuleUntilIterable implements Iterable<Rule> {

        private final int start;
        private final int originalSize;
        private Predicate<Rule> endTest;

        public RuleUntilIterable(int start, Predicate<Rule> endTest) {
            this.start = start;
            originalSize = rules.size();
            this.endTest = endTest;
        }

        @Override
        public Iterator<Rule> iterator() {
            return new Iterator<Rule>() {
                private int pos = start;

                @Override
                public boolean hasNext() {
                    if (pos >= originalSize) {
//                        System.err.println("it done #");
                        return false;
                    } else if (endTest.test(rules.get(pos))) {
//                        System.err.println("it done !=");
                        return false;
                    } else {
                        return true;
                    }
                }

                @Override
                public Rule next() {
//                    System.err.println("it @" + pos + ", orig size " + originalSize + ": rule= " + rules.get(pos));
                    return rules.get(pos++);
                }
            };
        }

    }

    @Override
    public Iterable<Rule> getRules(final int parentState) {
        processNewTopDownRules();

        int start = parentStartIndex.get(parentState);

        if (start < 0) {
            // parentState has no known transitions
            return Collections.emptyList();
        } else {
//            System.err.println("\n\n** getRules(" + parentState + "), start at " + start);
            assert rules.get(start).getParent() == parentState : "rule(" + parentState + ") has wrong parent, is " + rules.get(start);
            return new RuleUntilIterable(start, rule -> rule.getParent() != parentState);
        }
    }

    @Override
    public IntIterable getLabelsTopDown(int parentState) {
        processNewTopDownRules();

        Int2IntMap labelsMap = parentLabelStartIndex.get(parentState);

        if (labelsMap == null) {
            return IntLists.EMPTY_LIST;
        } else {
            return labelsMap.keySet();
        }
    }

    /**
     *
     * @param labelId
     * @param parentState
     * @return
     */
    public Iterable<Rule> getRules(final int labelId, final int parentState) {
        processNewTopDownRules();

        int start = parentLabelStartIndex.get(parentState, labelId);

        if (start < 0) {
            return Collections.emptyList();
        } else {
//            System.err.println("\n\n** getRules(" + labelId + "," + parentState + "), start at " + start);
            assert rules.get(start).getParent() == parentState : "rule(" + parentState + "," + labelId + ") has wrong parent, is " + rules.get(start);
            assert rules.get(start).getLabel() == labelId : "rule(" + parentState + "," + labelId + ") has wrong label, is " + rules.get(start);;
            return new RuleUntilIterable(start, rule -> rule.getParent() != parentState || rule.getLabel() != labelId);
        }
    }

    /**
     *
     * @param label
     * @param parent
     * @return
     */
    public boolean useCachedRule(int label, int parent) {
        processNewTopDownRules();

        return parentLabelStartIndex.get(parent, label) >= 0;
    }
}
