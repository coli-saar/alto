/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling.rule_weighters;

import com.google.common.base.Function;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.learning_rates.LearningRate;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author christoph
 */
public class LengthEvaluation extends RegularizedKLRuleWeighting {

    /**
     *
     */
    private final TreeAutomaton<StringAlgebra.Span> weights;

    /**
     *
     */
    private final static Function<Rule, Integer> toINTS = (Rule r) -> r.getLabel();
    
    /**
     * 
     */
    private final List<Integer> leafs;

    /**
     *
     * @param basis
     * @param normalizationExponent
     * @param normalizationDivisor
     * @param rate
     * @param sumLength
     */
    public LengthEvaluation(TreeAutomaton<StringAlgebra.Span> basis, int normalizationExponent,
            double normalizationDivisor, LearningRate rate, int sumLength) {
        super(makeBase(sumLength, basis), normalizationExponent, normalizationDivisor, rate);
        weights = basis;
        this.leafs = weights.viterbiRaw().getTree().getLeafLabels();
    }

    /**
     *
     * @param sumLength
     * @param base
     * @return
     */
    public static TreeAutomaton makeBase(int sumLength, TreeAutomaton<StringAlgebra.Span> base) {
        base.makeAllRulesExplicit();
        Signature sig = base.getSignature();
        ConcreteTreeAutomaton<String> cta = new ConcreteTreeAutomaton<>(sig);

        for (int pos = 0; pos < sumLength; ++pos) {
            String state = makePosState(pos);
            StringAlgebra.Span span = new StringAlgebra.Span(pos, pos + 1);
            Iterable<Rule> it = base.getRulesTopDown(base.getIdForState(span));
            for (Rule r : it) {
                if (r.getArity() == 0) {
                    String symbol = sig.resolveSymbolId(r.getLabel());
                    cta.addRule(cta.createRule(state, symbol, new String[0]));
                }
            }
        }

        for (int width = 1; width <= sumLength; ++width) {
            String parent = makeWidthState(width);
            for (int leftWidth = 1; leftWidth < width; ++leftWidth) {
                String[] lstates = makeStates(0, leftWidth, sumLength, base);
                String[] rstates = makeStates(leftWidth, width - leftWidth, sumLength, base);

                for (int sym = 1; sym <= sig.getMaxSymbolId(); ++sym) {
                    if (sig.getArity(sym) == 2) {
                        String symbol = sig.resolveSymbolId(sym);
                        for (String lState : lstates) {
                            for (String rState : rstates) {
                                cta.addRule(cta.createRule(parent, symbol, new String[]{lState, rState}));
                            }
                        }
                    }
                }
            }
        }

        cta.addFinalState(cta.getIdForState(makeWidthState(sumLength)));
        return cta;
    }

    /**
     *
     * @param width
     * @param sumLength
     * @return
     */
    private static String[] makeStates(int leftWidth, int width, int sumLength,
            TreeAutomaton<StringAlgebra.Span> base) {
        if (width == 1) {
            ArrayList<String> posses = new ArrayList<>();
            for (int pos = leftWidth; pos < sumLength; ++pos) {
                posses.add(makePosState(pos));
            }

            return posses.toArray(new String[posses.size()]);
        } else {
            return new String[]{makeWidthState(width)};
        }
    }

    /**
     *
     * @param pos
     * @return
     */
    private static String makePosState(int pos) {
        return "P" + pos;
    }

    /**
     *
     * @param width
     * @return
     */
    private static String makeWidthState(int width) {
        return "W" + width;
    }

    @Override
    public double getLogTargetProbability(Tree<Rule> sample) {
        Tree<Integer> ti = sample.map(toINTS);
        List<Integer> l = ti.getLeafLabels();
        double total = 0.0;
        for(int i=0;i<l.size();++i) {
            if(l.get(i).equals(this.leafs.get(i))) {
                total += 1;
            }
        }
        
        try {
            Tree<Rule> tr = this.weights.getRuleTree(ti);
            if(tr == null) {
                System.out.println(total);
                return total;
            }
            List<Tree<Rule>> list = tr.getAllNodes();

            for (int i = 0; i < list.size(); ++i) {
                total += Math.log(list.get(i).getLabel().getWeight());
            }

            return total;
        } catch (Exception ex) {
            return Math.log(0.0);
        }
    }
}
