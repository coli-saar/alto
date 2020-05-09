package de.up.ling.irtg.algebra.graph

import com.google.common.collect.Sets
import de.saar.basic.Pair
import de.up.ling.irtg.automata.TreeAutomaton
import de.up.ling.irtg.signature.Signature
import de.up.ling.irtg.util.TestingTools;
import de.up.ling.tree.ParseException
import de.up.ling.tree.Tree
import org.junit.Test

import java.util.stream.Collectors;

class ApplyModifyGraphAlgebraTest {

    String GRAPH_TYPE_SEP = ApplyModifyGraphAlgebra.GRAPH_TYPE_SEP;

    String giraffe = "(g<root>/giraffe)";
    String eat = "(e<root>/eat-01 :ARG0 (s<s>))"+GRAPH_TYPE_SEP+"(s)";
    String want = "(w<root>/want-01 :ARG0 (s<s>) :ARG1 (o<o>))"+GRAPH_TYPE_SEP+"(s, o(s))";
    String tall = "(t<root>/tall :mod-of (m<m>))"+GRAPH_TYPE_SEP+"(m)";
    String whistling = "(w<root>/whistle-01 :manner-of (m<m>) :ARG0 (s<s>))"+GRAPH_TYPE_SEP+"(m, s)";
    String persuade = "(p<root>/persuade-01 :ARG0 (s<s>) :ARG1 (o<o>) :ARG2 (o2<o2>))"+GRAPH_TYPE_SEP+"(s, o2(s_UNIFY_o))";
    String andObjControl = "(a<root>/and :op1 (o1<op1>) :op (o2<op2>))"+GRAPH_TYPE_SEP+"(op1(o2(s_UNIFY_o), s), op2(o2(s_UNIFY_o), s))";
    String tree = "(t<root>/tree)";
    String eat2 = "(e<root>/eat-01 :ARG0 (s<s>) :ARG01 (o<o>))"+GRAPH_TYPE_SEP+"(s, o)";
    String appS = "APP_s";
    String appO = "APP_o";
    String appO2 = "APP_o2";
    String appOp1 = "APP_op1";
    String appOp2 = "APP_op2";
    String modM = "MOD_m";
    String modS = "MOD_s";
    String osString = "(o, s)";
    String sString = "(s)";
    String oString = "(o)";
    String o2String = "(o2)";
    String o2soString = "(o2(s_UNIFY_o))";
    String emptyTypeString = "()";
    ApplyModifyGraphAlgebra.Type renameType = new ApplyModifyGraphAlgebra.Type("(o2(s_UNIFY_o), s)");
    String renameTypeString = "(o2(s_UNIFY_o), s)";
    String nestedCoordTypeString = "(op1(o2(s_UNIFY_o), s), op2(o2(s_UNIFY_o), s))";

    @Test
    void typeConstructorAndAllowedAppliesTest() {
        testInvalid("(mod,o(poss_UNIFY_s),o2(s_UNIFY_o),s)"); // actual corpus example from old type system that (correctly) doesn't work anymore
        testInvalid("(mod(s_UNIFY_o),o(s_UNIFY_s),s)"); // actual corpus example from old type system that (correctly) doesn't work anymore
        testInvalid("(o(mod_UNIFY_s),o2(s_UNIFY_o),s))"); // actual corpus example from old type system that (correctly) doesn't work anymore
        testInvalid("(o(poss_UNIFY_s),o2(s_UNIFY_o),s)"); // actual corpus example from old type system that (correctly) doesn't work anymore
        testInvalid("(o(s_UNIFY_o2),o2(poss_UNIFY_s),s)"); // actual corpus example from old type system that (correctly) doesn't work anymore

        testInvalid("(o(s), s(o))"); //cyclic
        testInvalid("(a(b(x_UNIFY_c)), d(b(y_UNIFY_c)))"); // conflicting edge information
        testInvalid("(a(b(x_UNIFY_c)), d(b(c)))"); // implicit conflicting edge information
        testInvalid("(a(b_UNIFY_c, b_UNIFY_d))"); // bad request at a
        testInvalid("(a(b_UNIFY_c, b))"); // implicit bad request at a
    }



    @Test
    void testIncompleteTypeString() {
        //incomplete information is allowed in string notation, but is there in actual graph
        //here the incomplete information is that in o(s), the nested s does not repeat the o2 of s(o2).
        testType("(o(s), s(o2))", Sets.newHashSet("o"), Sets.newHashSet("s", "o2"));
        testRequest("(o(s), s(o2))", "o", "(s(o2))");
    }

    @Test
    void testTerms() {
        Tree<String> term1 = Tree.create(appS, Tree.create(eat), Tree.create(giraffe));
        test(term1, false, 1, false);
        Tree<String> term2 = Tree.create(appS,
                Tree.create(appO, Tree.create(want), Tree.create(eat)),
                Tree.create(modM, Tree.create(giraffe), Tree.create(tall)));
        test(term2, false, 1, false);
        Tree<String> termWhistling = Tree.create(appS,
                Tree.create(modM, Tree.create(eat), Tree.create(whistling)),
                Tree.create(giraffe));
        test(termWhistling, false, 1, false);
        Tree<String> termTransitive = Tree.create(appS,
                Tree.create(appO, Tree.create(eat2), Tree.create(tree)),
                Tree.create(giraffe));
        test(termTransitive, false, 2, false);
        Tree<String> termPersuade = Tree.create(appS,
                    Tree.create(appO,
                        Tree.create(appO2, Tree.create(persuade), Tree.create(eat)),
                        Tree.create(giraffe)),
                    Tree.create(tree));
        Tree<String> termPersuade2 =
                Tree.create(appO,
                        Tree.create(appO2,
                                Tree.create(appS, Tree.create(persuade), Tree.create(tree)),
                                Tree.create(eat)),
                        Tree.create(giraffe));
        test(termPersuade, false, 3, false);
        test(termPersuade2, false, 3, false);

        Tree<String> termAndPersuade = Tree.create(appS,
                Tree.create(appO,
                    Tree.create(appO2,
                        Tree.create(appOp2,
                            Tree.create(appOp1, Tree.create(andObjControl), Tree.create(persuade)),
                            Tree.create(persuade)),
                        Tree.create(eat)),
                    Tree.create(giraffe)),
                Tree.create(giraffe));
        test(termAndPersuade, false, 6, false);
    }


    @Test
    void testIllTypedTerms() {
        Tree<String> termWrongApp = Tree.create(appO, Tree.create(eat), Tree.create(giraffe));
        test(termWrongApp, true, -1, false);

        Tree<String> termWrongMod = Tree.create(modS, Tree.create(giraffe), Tree.create(tall));
        test(termWrongMod, true, -1, false);

        Tree<String> termWrongType1 = Tree.create(appS, Tree.create(appO, Tree.create(eat2), Tree.create(eat)),
                                                                                Tree.create(giraffe));
        test(termWrongType1, true, -1, false);

        Tree<String> termWrongType2 = Tree.create(appS, Tree.create(appO, Tree.create(want), Tree.create(tall)),
                Tree.create(giraffe));
        test(termWrongType2, true, -1, false);

        Tree<String> termWrongType3 = Tree.create(appS, Tree.create(appO, Tree.create(want), Tree.create(tree)),
                Tree.create(giraffe));
        test(termWrongType3, true, -1, false);
    }


    @Test
    void testRenameType() {
        testType(renameTypeString, Sets.newHashSet("s", "o2"), Sets.newHashSet("o"));
        testRequest(renameTypeString, "o2", sString);
        testRequest(renameTypeString, "o", emptyTypeString);
        testRequest(renameTypeString, "s", emptyTypeString);
        assert renameType.toRequestNamespace("o2", "o").equals("s");
        testApply(renameTypeString, "o2", osString);
        testApply(renameTypeString, "s", o2soString);
    }

    @Test
    void testNestedCoordType() {
        testType(nestedCoordTypeString, Sets.newHashSet("op1", "op2"), Sets.newHashSet("o2", "o", "s"));
        testRequest(nestedCoordTypeString, "op1", renameTypeString);
        testRequest(nestedCoordTypeString, "op2", renameTypeString);
        testRequest(nestedCoordTypeString, "o2", sString);
        testRequest(nestedCoordTypeString, "o", emptyTypeString);
        testRequest(nestedCoordTypeString, "s", emptyTypeString);
        testApply(nestedCoordTypeString, "op1", "(op2(o2(s_UNIFY_o()), s(), o()))");
        testApply("(op2(o2(s_UNIFY_o()), s(), o()))", "op2", renameTypeString);
    }


    @Test
    void testDeepType() {
        //from thesis, Figure 5.14, with an added source F that has A and its descendants as children.
        String deepTypeString = "(f(a(a_UNIFY_b(c, e_UNIFY_d), c, d)), e)";
        testType(deepTypeString, Sets.newHashSet("f", "e"), Sets.newHashSet("a", "b", "c", "d"));
        String outer = "(r<root>/r :e1 (f<f>) :e2 (e<e>))"+GRAPH_TYPE_SEP+deepTypeString;
        String f = "(f<root> / f :e3 (a<a>))"+GRAPH_TYPE_SEP+"(a(a_UNIFY_b(c, e_UNIFY_d), c, d))";
        String a = "(a<root> / a :e4 (b<a>) :e5 (c<c>) :e6 (d<d>))"+GRAPH_TYPE_SEP+"(a(c, e_UNIFY_d), c, d)";
        String b = "(b<root> / b :e7 (c<c>) :e8 (d<e>))"+GRAPH_TYPE_SEP+"(c,e)";
        String c = "(c<root> / c)"+GRAPH_TYPE_SEP+"()";
        String d = "(d<root> / d)"+GRAPH_TYPE_SEP+"()";
        String e = "(e<root> / e)"+GRAPH_TYPE_SEP+"()";
        Tree<String> deepTerm = Tree.create("APP_e",
                Tree.create("APP_c",
                        Tree.create("APP_d",
                                Tree.create("APP_b",
                                        Tree.create("APP_a",
                                                Tree.create("APP_f", Tree.create(outer), Tree.create(f)),
                                                Tree.create(a)),
                                        Tree.create(b)),
                                Tree.create(d)),
                        Tree.create(c)),
                Tree.create(e));
        test(deepTerm, false, 12, false);
    }

    @Test
    void testEquals() {
        assert !new ApplyModifyGraphAlgebra.Type(sString).equals(new ApplyModifyGraphAlgebra.Type(oString))
        assert new ApplyModifyGraphAlgebra.Type(sString).equals(new ApplyModifyGraphAlgebra.Type(sString))
        assert new ApplyModifyGraphAlgebra.Type("(o(s), s(o2))").equals(new ApplyModifyGraphAlgebra.Type("(o(s(o2)), s(o2))"))
        assert new ApplyModifyGraphAlgebra.Type("(o(s), s)").equals(new ApplyModifyGraphAlgebra.Type("(o(s))"))
    }

    @Test
    void testApplySet() {
        runTestApplySet(renameTypeString, sString, Sets.newHashSet("o2", "o"));
        runTestApplySet(renameTypeString, oString, Sets.newHashSet("o2", "s"));
        runTestApplySet(renameTypeString, o2String, null);
    }

    @Test
    void testSubtypes() {
        runTestSubtypes(renameTypeString, Sets.newHashSet("(o2(s_UNIFY_o()))", "(o2(s_UNIFY_o()), s())", "(s())", "(s(), o())", "()", "(o())"));
        runTestSubtypes(nestedCoordTypeString, Sets.newHashSet("(op2(o2(s_UNIFY_o()), s(), o()), op1(o2(s_UNIFY_o()), s(), o()))",
                "(o2(s_UNIFY_o()))", "(op1(o2(s_UNIFY_o()), s(), o()))", "(o2(s_UNIFY_o()), s())", "(s())", "(op2(o2(s_UNIFY_o()), s(), o()))", "(s(), o())", "()", "(o())"));
    }

    @Test
    void testChangingTypes() {
        ApplyModifyGraphAlgebra.Type t = new ApplyModifyGraphAlgebra.Type("(s)");
        t = t.addSource("o");
        assert t.equals(new ApplyModifyGraphAlgebra.Type("(s(), o())"));
        t = t.addSource("o2");
        assert t.equals(new ApplyModifyGraphAlgebra.Type("(s(), o2(), o())"));
        t = t.setDependency("o2", "o", "o");
        assert t.equals(new ApplyModifyGraphAlgebra.Type("(s(), o2(o()))"));
        t = t.setDependency("o2", "o", "s");
        assert t.equals(new ApplyModifyGraphAlgebra.Type("(s(), o2(s_UNIFY_o()))"));
        ApplyModifyGraphAlgebra.Type t2 = t.setDependency("o2", "s", "x");
        // check that we didn't change the original type
        assert t.equals(new ApplyModifyGraphAlgebra.Type("(s(), o2(s_UNIFY_o()))"));
        assert t2.equals(new ApplyModifyGraphAlgebra.Type("(o2(s_UNIFY_o(), x_UNIFY_s()))"));
        t2 = t2.setDependency("o", "s", "o");
        assert t2.equals(new ApplyModifyGraphAlgebra.Type("(o2(s_UNIFY_o(o_UNIFY_s()), x_UNIFY_s()))"));
        t2 = t2.setDependency("o", "s", "s");
        assert t2.equals(new ApplyModifyGraphAlgebra.Type("(o2(s_UNIFY_o(s()), x_UNIFY_s()))"));
        boolean threwError = false;
        try {
            t2 = t2.setDependency("s", "o2", "o2");
        } catch (IllegalArgumentException ex) {
            threwError = true;
        }
        assert threwError;
    }

    @Test
    public void testTypeSerialization() {
        // Types needs to be serializable because the A* parser in am-tools caches the type lexicon

        ApplyModifyGraphAlgebra.Type t = new ApplyModifyGraphAlgebra.Type(nestedCoordTypeString); // new ApplyModifyGraphAlgebra.Type("(s)"); // from testChangingTypes

        byte[] serialized = TestingTools.pickle(t);
        ApplyModifyGraphAlgebra.Type unpickled = TestingTools.unpickle(serialized, ApplyModifyGraphAlgebra.Type.class);

        assert t.equals(unpickled);
    }


    private static void testType(String typeString, Set<String> canApplyNow, Set<String> cannotApplyNow) throws ParseException {
        ApplyModifyGraphAlgebra.Type t = new ApplyModifyGraphAlgebra.Type(typeString);
        assert new ApplyModifyGraphAlgebra.Type(t.toString()).equals(t);//check toString and constructor for consistency
        for (String s : canApplyNow) {
            assert t.canApplyNow(s);
        }
        for (String s : cannotApplyNow) {
            assert !t.canApplyNow(s);
        }
    }

    private static void testInvalid(String typeString) {
        boolean failed = false;
        try {
            new ApplyModifyGraphAlgebra.Type(typeString);
        } catch (ParseException | IllegalArgumentException ex) {
            failed = true;
        }
        assert failed;
    }

    private static void testRequest(String typeString, String source, String requestTypeString) throws ParseException {
        ApplyModifyGraphAlgebra.Type t = new ApplyModifyGraphAlgebra.Type(typeString);
        ApplyModifyGraphAlgebra.Type req = new ApplyModifyGraphAlgebra.Type(requestTypeString);
        assert t.getRequest(source).equals(req);
    }

    private static void testApply(String typeString, String source, String resultTypeString) throws ParseException {
        ApplyModifyGraphAlgebra.Type t = new ApplyModifyGraphAlgebra.Type(typeString);
        ApplyModifyGraphAlgebra.Type result = new ApplyModifyGraphAlgebra.Type(resultTypeString);
        assert t.performApply(source).equals(result);
    }

    private static void runTestApplySet(String typeString, String subtypeString, Set<String> applySet) throws ParseException {
        ApplyModifyGraphAlgebra.Type t = new ApplyModifyGraphAlgebra.Type(typeString);
        ApplyModifyGraphAlgebra.Type subtype = new ApplyModifyGraphAlgebra.Type(subtypeString);
        Set<String> ret = t.getApplySet(subtype);
        if (ret == null) {
            assert  applySet == null;
        } else {
            assert ret.equals(applySet);
        }
    }

    private static void runTestSubtypes(String typeString, Set<String> subtypeStrings) throws ParseException {
        ApplyModifyGraphAlgebra.Type t = new ApplyModifyGraphAlgebra.Type(typeString);
        Set<ApplyModifyGraphAlgebra.Type> subtypes = subtypeStrings.stream().map({ s ->
            try {
                return new ApplyModifyGraphAlgebra.Type(s);
            } catch (ParseException e) {
                return null;
            }
        }).collect(Collectors.toSet());
        assert t.getAllSubtypes().equals(subtypes);
    }


    private static void test(Tree<String> term, boolean evalShouldFail, int expectedLanguageSize, boolean debug) {
        if (debug) {
            System.err.println("Testing " + term.toString());
        }
        Signature sig = new Signature();
        term.dfs({ Tree<String> tree, List<Void> list ->
            sig.addSymbol(tree.getLabel(), list.size());
            return null;
        });

        ApplyModifyGraphAlgebra alg = new ApplyModifyGraphAlgebra(sig);
        Pair<SGraph, ApplyModifyGraphAlgebra.Type> asGraph = alg.evaluate(term);
        if (debug) {
            System.err.println("evaluation result: "+String.valueOf(asGraph));
        }

        if (evalShouldFail) {
            assert asGraph == null;
        } else {
            assert asGraph != null;
            TreeAutomaton auto = alg.decompose(asGraph).asConcreteTreeAutomatonBottomUp();
            if (debug) {
                System.err.println(auto);
                System.err.println("decomp viterbi: "+auto.viterbi());
                for (Tree<String> tree : (Iterable<Tree<String>>)auto.languageIterable()) {
                    System.err.println(tree);
                }
            }
            assert auto.accepts(term);
            assert alg.evaluate(auto.viterbi()).equals(asGraph);
            assert auto.countTrees() == expectedLanguageSize;
        }
    }



}
