/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author christoph
 */
public class TreeArityEnsure implements Function<TreeAutomaton, TreeAutomaton> {

    /**
     *
     */
    private final Object2ObjectMap<String, IntList> arities;

    /**
     *
     * @param arities
     */
    public TreeArityEnsure(Object2ObjectMap<String, IntList> arities) {
        this.arities = arities;
    }

    /**
     *
     */
    private final static Pattern ARITY_INFORMATION
            = Pattern.compile("(([^\\s:']+):(\\p{Digit}+))|('([^']+)':(\\p{Digit}+))");

    /**
     *
     * @param input
     * @return
     */
    public static Function<TreeAutomaton, TreeAutomaton> getRestrictionFactory(String input) {
        Object2ObjectMap<String, IntList> arities = new Object2ObjectOpenHashMap<>();
        Matcher match = ARITY_INFORMATION.matcher(input);

        while (match.find()) {
            String name = match.group(2);
            String value = match.group(3);

            if (name == null && value == null) {
                name = match.group(5);
                value = match.group(6);
            }

            int val = Integer.parseInt(value);

            IntList il = arities.get(name);
            if (il == null) {
                il = new IntArrayList();
                arities.put(name, il);
            }

            il.add(val);
        }

        return new TreeArityEnsure(arities);
    }

    @Override
    public TreeAutomaton apply(TreeAutomaton t) {
        ConcreteTreeAutomaton<Integer> cta = new ConcreteTreeAutomaton<>(t.getSignature());
        IntIterator labels = t.getAllLabels().iterator();

        IntList def = new IntArrayList();
        def.add(0);

        int max = 0;
        
        Integer[] empty = new Integer[0];
        while (labels.hasNext()) {
            int lab = labels.nextInt();

            String label = t.getSignature().resolveSymbolId(lab);
            if (t.getSignature().getArity(lab) > 0) {
                if (!label.equals(MinimalTreeAlgebra.RIGHT_INTO_LEFT)
                        && !label.equals(MinimalTreeAlgebra.LEFT_INTO_RIGHT)) {
                    Integer[] children = new Integer[t.getSignature().getArity(lab)];
                    Arrays.fill(children, 0);

                    cta.addRule(cta.createRule(0, label, children));
                }
            } else {

                IntList ars = this.arities.get(label);
                if (ars == null) {
                    ars = def;
                }

                for (int i = 0; i < ars.size(); ++i) {
                    int arity = ars.getInt(i);

                    max = Math.max(max, arity);

                    cta.addRule(cta.createRule(arity, label, empty));
                }
            }
        }

        cta.addFinalState(cta.addState(0));

        for(int arity=1;arity<=max;++arity) {
            Integer parent = arity - 1;

            cta.addRule(cta.createRule(parent, MinimalTreeAlgebra.LEFT_INTO_RIGHT, new Integer[]{0, arity}));
            cta.addRule(cta.createRule(parent, MinimalTreeAlgebra.RIGHT_INTO_LEFT, new Integer[]{arity, 0}));
        }
        
        return cta;
    }
}
