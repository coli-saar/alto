/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.util.MutableInteger;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author koller
 */
@CodecMetadata(name = "joshua", description = "Joshua grammar", extension = "joshua", type = InterpretedTreeAutomaton.class)
public class JoshuaInputCodec extends InputCodec<InterpretedTreeAutomaton> {

    @Override
    public InterpretedTreeAutomaton read(InputStream is) throws CodecParseException, IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        String line = null;
        boolean firstRule = true;

        ConcreteTreeAutomaton<String> cta = new ConcreteTreeAutomaton<>();
        InterpretedTreeAutomaton ret = new InterpretedTreeAutomaton(cta);

        StringAlgebra left = new StringAlgebra();
        StringAlgebra right = new StringAlgebra();

        Homomorphism hLeft = new Homomorphism(cta.getSignature(), left.getSignature());
        Homomorphism hRight = new Homomorphism(cta.getSignature(), right.getSignature());

        ret.addInterpretation("left", new Interpretation(left, hLeft));
        ret.addInterpretation("right", new Interpretation(right, hRight));

        while ((line = r.readLine()) != null) {
            String[] parts = line.split("\\s*\\|\\|\\|\\s*");
            if (parts.length >= 3) {
                MutableInteger nextVariable = new MutableInteger(1);
                Map<String, String> joshuaVariableToIrtgVariable = new HashMap<>();
                List<String> rhs = new ArrayList<>();

                String lhs = parts[0].substring(1, parts[0].length() - 1); // strip [ ]
                String term = Util.gensym("r");

                Tree<String> leftHom = analyzeRule(parts[1].split("\\s+"), 0, nextVariable, joshuaVariableToIrtgVariable, rhs);
                Tree<String> rightHom = analyzeRuleGivenVariables(parts[2].split("\\s+"), 0, joshuaVariableToIrtgVariable);

                Rule rule = cta.createRule(lhs, term, rhs);
                cta.addRule(rule);

                hLeft.add(term, leftHom);
                hRight.add(term, rightHom);

                if (firstRule) {
                    cta.addFinalState(rule.getParent());
                    firstRule = false;
                }
            }
        }

        return ret;
    }

    private Tree<String> analyzeRule(String[] words, int pos, MutableInteger nextVariable, Map<String, String> joshuaVariableToIrtgVariable, List<String> rhs) {
        Tree<String> firstTree = Tree.create(toConstant(words[pos], nextVariable, joshuaVariableToIrtgVariable, rhs));

        if (pos == words.length - 1) {
            return firstTree;
        } else {
            return Tree.create(StringAlgebra.CONCAT, firstTree, analyzeRule(words, pos + 1, nextVariable, joshuaVariableToIrtgVariable, rhs));
        }
    }

    private String toConstant(String word, MutableInteger nextVariable, Map<String, String> joshuaVariableToIrtgVariable, List<String> rhs) {
        if (isJoshuaVariable(word)) {
            String var = "?" + nextVariable.incValue();
            joshuaVariableToIrtgVariable.put(word, var);
            rhs.add(word.substring(1, word.length() - 1).split(",")[0]); // [X,1] -> X
            return var;
        } else {
            return word;
        }
    }

    private static boolean isJoshuaVariable(String word) {
        int L = word.length();
        return word.charAt(0) == '[' && word.charAt(L - 1) == ']';
    }

    private Tree<String> analyzeRuleGivenVariables(String[] words, int pos, Map<String, String> joshuaVariableToIrtgVariable) {
        String w = isJoshuaVariable(words[pos]) ? joshuaVariableToIrtgVariable.get(words[pos]) : words[pos];
        Tree<String> firstTree = Tree.create(w);

        if (pos == words.length - 1) {
            return firstTree;
        } else {
            return Tree.create(StringAlgebra.CONCAT, firstTree, analyzeRuleGivenVariables(words, pos + 1, joshuaVariableToIrtgVariable));
        }
    }
}
