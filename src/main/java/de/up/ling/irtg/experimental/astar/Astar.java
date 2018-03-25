/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.experimental.astar;

import de.up.ling.irtg.util.CpuTimeStopwatch;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author koller
 */
public class Astar {
    private int N, EDGES, TAGS;
    private double[][][] edgep;      // edgep[i][k][o] = log P(o[i -> k])
    private double[][] tagp;         // tagp[i][s]     = log P(s[i])
    private int[][] topSupertags;    // topSupertags[i] = best-k supertags for token #i
    private OutsideEstimator outside;

    // these are shared across all parsing problems
    private String[] supertags = {"john[s]", "john[]", "loves[]", "loves[s,o]", "mary[]"};
    private Type[] supertagTypes = {t("[s]"), t("[]"), t("[]"), t("[s,o]"), t("[]")};
    private String[] edgeLabels = {"APP_s", "APP_o"};
    private static final int APP_S = 0, APP_O = 1;

    public Astar(double[][][] edgep, double[][] tagp, int[][] topSupertags) {
        this.edgep = edgep;
        this.tagp = tagp;
        this.topSupertags = topSupertags;

        this.N = edgep.length;              // sentence length
        this.EDGES = edgeLabels.length;     // # edgelabels
        this.TAGS = supertags.length;       // # supertags

        outside = new OutsideEstimator(edgep, tagp, EDGES, TAGS);
    }

    private Item process() {
        CpuTimeStopwatch w = new CpuTimeStopwatch();
        w.record();

        PriorityQueue<Item> agenda = new ObjectHeapPriorityQueue<>();
        Set<Item>[] rightChart = new Set[N + 1];  // rightChart[i] = all previously dequeued items that start at i
        Set<Item>[] leftChart = new Set[N + 1];   // leftChart[k]  = all previously dequeued items that end at k

        for (int i = 0; i < N + 1; i++) {
            rightChart[i] = new HashSet<>();
            leftChart[i] = new HashSet<>();
        }

        // initialize agenda
        for (int i = 0; i < N; i++) {
            for (int s : topSupertags[i]) {
                Item it = new Item(i, i + 1, i, supertagTypes[s], tagp[i][s]);
                it.setCreatedBySupertag(s);
                it.setOutsideEstimate(outside.evaluate(it));
                agenda.enqueue(it);
            }
        }

        // iterate over agenda
        while (!agenda.isEmpty()) {
            Item it = agenda.dequeue();

            // return first found goal item
            if (isGoal(it)) {
                w.record();
                w.printMilliseconds("process");
                return it;
            }

            // add it to chart
            rightChart[it.getStart()].add(it);
            leftChart[it.getEnd()].add(it);

            // combine it with partners on the right
            for (Item rightPartner : rightChart[it.getEnd()]) {
                for (int op = 0; op < EDGES; op++) {
                    // ... with it as functor
                    Item result = combineRight(op, it, rightPartner);
                    if (result != null) {
                        explain("R1", op, it, rightPartner, result);
                        agenda.enqueue(result);
                    }

                    // ... with it as argument
                    result = combineLeft(op, rightPartner, it);
                    if (result != null) {
                        explain("R2", op, rightPartner, it, result);
                        agenda.enqueue(result);
                    }
                }
            }

            // combine it with partners on the left
            for (Item leftPartner : leftChart[it.getStart()]) {
                for (int op = 0; op < EDGES; op++) {
                    // ... with it as functor
                    Item result = combineLeft(op, it, leftPartner);
                    if (result != null) {
                        explain("L1", op, it, leftPartner, result);
                        agenda.enqueue(result);
                    }

                    // ... with it as argument
                    result = combineRight(op, leftPartner, it);
                    if (result != null) {
                        explain("L2", op, leftPartner, it, result);
                        agenda.enqueue(result);
                    }
                }
            }
        }

        w.record();
        w.printMilliseconds("process/fail");
        return null;
    }

    private void explain(String branch, int op, Item it, Item partner, Item result) {
//        System.err.printf("  enqueue(%s): %s\n      from %s  --%s:%f-->  %s\n", branch, result, it, edgeLabels[op], edgep[it.getRoot()][partner.getRoot()][op], partner);
    }

    private Tree<String> decode(Item item) {
        if (item.getLeft() == null) {
            // leaf; decode op as supertag
            return Tree.create(supertags[item.getOperation()]);
        } else {
            // non-leaf; decode op as edge
            Tree<String> left = decode(item.getLeft());
            Tree<String> right = decode(item.getRight());
            return Tree.create(edgeLabels[item.getOperation()], left, right);
        }
    }

    /**
     * Parses the given string and returns an AM term.
     * 
     * @return 
     */
    public Tree<String> parse() {
        Item goalItem = process();

        if (goalItem == null) {
            return null;
        } else {
            return decode(goalItem);
        }
    }

    // check whether the item is a goal item
    private boolean isGoal(Item item) {
        return item.getStart() == 0 && item.getEnd() == N && item.getType().getType().equals("[]");
    }

    // combine functor with an argument on the right
    private Item combineRight(int op, Item functor, Item argument) {
        Type t = combine(op, functor.getType(), argument.getType());

        if (t == null) {
            return null;
        } else {
            double logEdgeProbability = edgep[functor.getRoot()][argument.getRoot()][op];
            Item ret = new Item(functor.getStart(), argument.getEnd(), functor.getRoot(), t, functor.getLogProb() + argument.getLogProb() + logEdgeProbability);
            ret.setCreatedByOperation(op, functor, argument);
            ret.setOutsideEstimate(outside.evaluate(ret));
            return ret;
        }
    }

    // combine functor with an argument on the left
    private Item combineLeft(int op, Item functor, Item argument) {
        Type t = combine(op, functor.getType(), argument.getType());

        if (t == null) {
            return null;
        } else {
            double logEdgeProbability = edgep[functor.getRoot()][argument.getRoot()][op];
            Item ret = new Item(argument.getStart(), functor.getEnd(), functor.getRoot(), t, functor.getLogProb() + argument.getLogProb() + logEdgeProbability);
            ret.setCreatedByOperation(op, functor, argument);
            ret.setOutsideEstimate(outside.evaluate(ret));
            return ret;
        }
    }

    // combine a functor and argument type using the given operation
    private Type combine(int op, Type functor, Type argument) {
        if (op == APP_S) { // APP_s
            if (argument.getType().equals("[]")) {
                if (functor.getType().equals("[s]")) {
                    return new Type("[]");
                } else if (functor.getType().equals("[s,o]")) {
                    return new Type("[o]");
                }
            }
        } else if (op == APP_O) { // APP_o
            if (argument.getType().equals("[]")) {
                if (functor.getType().equals("[o]")) {
                    return new Type("[]");
                } else if (functor.getType().equals("[s,o]")) {
                    return new Type("[s]");
                }
            }
        }

        return null;
    }

    private static Type t(String type) {
        return new Type(type);
    }

    private static void fill(double[][] arr, double value) {
        for (int i = 0; i < arr.length; i++) {
            Arrays.fill(arr[i], value);
        }
    }

    private static void fill(double[][][] arr, double value) {
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[0].length; j++) {
                Arrays.fill(arr[i][j], value);
            }
        }
    }

    public static void main(String[] args) {
        double[][][] edgep = new double[3][3][2];
        fill(edgep, -10000);
        edgep[1][0][APP_S] = -1;
        edgep[1][2][APP_O] = -1;

        // private String[] supertags =   { "john[s]", "john[]", "loves[]", "loves[s,o]", "mary[]" };
        double[][] tagp = new double[3][5];
        fill(tagp, -10000);
        tagp[0][0] = -1;
        tagp[0][1] = -2;
        tagp[1][2] = -1;
        tagp[1][3] = -2;
        tagp[2][4] = -1;

        int[][] topSupertags = {{0, 1}, {2, 3}, {4, 4}};

        Astar astar = new Astar(edgep, tagp, topSupertags);

        for (int i = 0; i < 1; i++) {
            System.err.println(astar.parse());
        }
    }
}
