/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.lambda;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 *
 * @author koller
 */
public class LexiconGenerator {
    public <LeftState, RightState> SetMultimap<String, String> getLexicalPairs(TreeAutomaton<LeftState> left, TreeAutomaton<RightState> right) {
        SetMultimap<String, String> ret = HashMultimap.create();
        Set<Pair<LeftState, RightState>> seenStatePairs = new HashSet<Pair<LeftState, RightState>>();
        Queue<Pair<LeftState, RightState>> agenda = new LinkedList<Pair<LeftState, RightState>>();

        for (LeftState l : left.getFinalStates()) {
            for (RightState r : right.getFinalStates()) {
                Pair<LeftState, RightState> p = new Pair(l, r);
                agenda.offer(p);
                seenStatePairs.add(p);
            }
        }

        while (!agenda.isEmpty()) {
            Pair<LeftState, RightState> pq = agenda.remove();
            Collection<String> labelsLeft = left.getLabelsTopDown(pq.left);
            Collection<String> labelsRight = right.getLabelsTopDown(pq.right);

            for (String labelLeft : labelsLeft) {
                for (Rule<LeftState> leftRule : left.getRulesTopDown(labelLeft, pq.left)) {
                    for (String labelRight : labelsRight) {
                        for (Rule<RightState> rightRule : right.getRulesTopDown(labelRight, pq.right)) {
                            if (leftRule.getArity() == rightRule.getArity()) {
                                if (leftRule.getArity() == 0) {
                                    Pair<String, String> terminals = new Pair(labelLeft, labelRight);
                                    ret.put(labelLeft, labelRight);
                                } else {
                                    for (int i = 0; i < leftRule.getArity(); i++) {
                                        Pair<LeftState, RightState> childPair = new Pair(leftRule.getChildren()[i], rightRule.getChildren()[i]);
                                        if (!seenStatePairs.contains(childPair)) {
                                            agenda.offer(childPair);
                                            seenStatePairs.add(childPair);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return ret;
    }

    public static void main(String[] args) throws ParserException {
        TreeAutomaton english = makeDecompAutomaton("what is the highest point in florida", new StringAlgebra());
        TreeAutomaton lambda = makeDecompAutomaton("(argmax $0 (and (place:t $0) (loc:t $0 florida:s)) (elevation:i $0))", new LambdaTermAlgebra());
//        BottomUpAutomaton english = makeDecompAutomaton("what are the high points of states surrounding mississippi", new StringAlgebra());
//        BottomUpAutomaton lambda = makeDecompAutomaton("(lambda $0 (exists $1 (and (state:t $1) (next_to:t $1 mississippi:s) (high_point:t $1 $0))))", new LambdaTermAlgebra());
        
//        lambda.makeAllRulesExplicit();
//        System.err.println(lambda.getAllLabels().size());
//        System.exit(0);

        LexiconGenerator gen = new LexiconGenerator();
        SetMultimap<String, String> lexicalPairs = null;

//        for (int i = 0; i < 3; i++) {
            long startTime = System.currentTimeMillis();
            lexicalPairs = gen.getLexicalPairs(english, lambda);
            long endTime = System.currentTimeMillis();
            System.out.println("lexicon generated in " + (endTime - startTime) + "ms");
//        }

        for (String word : lexicalPairs.keySet()) {
            System.out.println("\n" + lexicalPairs.get(word).size() + " lambda terms for " + word + ":");

            List<String> lambdas = new ArrayList<String>(lexicalPairs.get(word));
            Collections.sort(lambdas);

            for (String l : lambdas) {
                System.out.println("  -> " + l);
            }
        }
    }

    private static <E> TreeAutomaton makeDecompAutomaton(String repr, Algebra<E> algebra) throws ParserException {
        E algebraObject = algebra.parseString(repr);
        return algebra.decompose(algebraObject);
    }
}
