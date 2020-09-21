/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.saar.coli.featstruct.AvmFeatureStructure;
import de.saar.coli.featstruct.FeatureStructure;
import de.saar.coli.featstruct.PlaceholderFeatureStructure;
import de.saar.coli.featstruct.PrimitiveFeatureStructure;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.codec.tag.ElementaryTree;
import de.up.ling.irtg.codec.tag.LexiconEntry;
import de.up.ling.irtg.codec.tag.Node;
import de.up.ling.irtg.codec.tag.NodeAnnotation;
import de.up.ling.irtg.codec.tag.NodeType;
import de.up.ling.irtg.codec.tag.TagGrammar;
import de.up.ling.irtg.codec.tulipac.TulipacLexer;
import de.up.ling.irtg.codec.tulipac.TulipacParser;
import java.io.IOException;
import java.io.InputStream;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.atn.PredictionMode;

import static de.up.ling.irtg.codec.tulipac.TulipacParser.*;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import org.antlr.v4.runtime.dfa.DFA;

import java.io.FileInputStream;
import java.util.*;
import java.util.function.Supplier;

/**
 *
 * @author koller
 */
@CodecMetadata(name = "tulipac", description = "TAG grammar (tulipac format)", extension = "tag", type = InterpretedTreeAutomaton.class)
public class TulipacInputCodec extends InputCodec<InterpretedTreeAutomaton> {
    private TagGrammar tagg;
    private Map<String, List<String>> treeFamilies = new HashMap<>(); // family name -> list(tree name)

    @Override
    public InterpretedTreeAutomaton read(InputStream is) throws CodecParseException, IOException {
        tagg = new TagGrammar();
        parseFile(is);
        return tagg.toIrtg();
    }

    private void parseFile(InputStream is) throws CodecParseException, IOException {
        TulipacLexer l = new TulipacLexer(CharStreams.fromStream(is));
        l.removeErrorListeners();
        l.addErrorListener(ThrowingErrorListener.INSTANCE);
        TulipacParser p = new TulipacParser(new CommonTokenStream(l));
        p.removeErrorListeners();
        p.addErrorListener(ThrowingErrorListener.INSTANCE);
        p.getInterpreter().setPredictionMode(PredictionMode.SLL);

        l.removeErrorListeners();
        l.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                throw e;
            }
        });

        GrmrContext parse = p.grmr();
        buildGrmr(parse);
    }

    private void buildGrmr(GrmrContext parse) throws CodecParseException, IOException {
        for (IncludeContext incl : parse.include()) {
            buildInclude(incl);
        }

        for (TrContext tr : parse.tr()) {
            buildTree(tr);
        }

        for (FamilyContext family : parse.family()) {
            buildFamily(family);
        }

        for (WordByItselfContext word : parse.wordByItself()) {
            buildWordByItself(word);
        }

        for (LemmaContext lemma : parse.lemma()) {
            buildLemma(lemma);
        }
    }

    private void buildInclude(IncludeContext incl) throws CodecParseException, IOException {
        String filename = identifier(incl.identifier());
        parseFile(new FileInputStream(filename));
    }

    private void buildTree(TrContext tr) {
        String name = identifier(tr.identifier());
        Tree<Node> tree = buildNode(tr.node());

        tagg.addElementaryTree(name, new ElementaryTree(tree));
    }

    private Tree<Node> buildNode(NodeContext node) {
        String label = identifier(node.identifier());
        NodeType type = NodeType.DEFAULT;

        if (node.marker() != null) {
            type = nodeType(node.marker());
        }

        Node n = new Node(label, type);
        
        if( node.annotation() != null ) {
            n.setAnnotation(nodeAnnotation(node.annotation()));
        }

        switch (node.fs().size()) {
            case 2: // top, then bottom
                n.setTop(buildFs(node.fs(0)));
                n.setBottom(buildFs(node.fs(1)));
                break;

            case 1: // only top
                n.setTop(buildFs(node.fs(0)));
                break;

            default:
                break;
        }

        List<Tree<Node>> children = Util.mapToList(node.node(), this::buildNode);
        return Tree.create(n, children);
    }

    private FeatureStructure buildFs(FsContext fs) {
        AvmFeatureStructure ret = new AvmFeatureStructure();

        if (fs != null) {
            for (FtContext ft : fs.ft()) {
                addToFs(ft, ret);
            }
        }

        return ret;
    }

    private void addToFs(FtContext ft, AvmFeatureStructure ret) {
        String attr = identifier(ft.identifier(0));

        if (ft.variable() != null) {
            ret.put(attr, new PlaceholderFeatureStructure(variable(ft.variable())));
        } else {
            ret.put(attr, new PrimitiveFeatureStructure(identifier(ft.identifier(1))));
        }
    }

    private void buildFamily(FamilyContext family) {
        String familyName = identifier(family.identifier(0));

        List<String> treeNames = new ArrayList<>();
        for (int i = 1; i < family.identifier().size(); i++) {
            treeNames.add(identifier(family.identifier(i)));
        }

        treeFamilies.put(familyName, treeNames);
    }

    private void buildWordByItself(WordByItselfContext wordC) {
        String word = identifier(wordC.identifier(0));
        FeatureStructure lexFs = buildFs(wordC.fs());

        if (wordC.familyIdentifier() != null) {
            // declared with tree family
            String treeFamilyName = familyIdentifier(wordC.familyIdentifier());

            for (String etreeName : lookupFamily(treeFamilyName, "Word '%s' is declared with unknown tree family '%s'.", word, treeFamilyName)) {
                checkTreeExists(etreeName, "Word '%s' is declared with tree family '%s', which contains the unknown elementary tree '%s'.", word, treeFamilyName, etreeName);
                tagg.addLexiconEntry(word, new LexiconEntry(word, etreeName, lexFs));
            }
        } else {
            // declared with elementary tree name
            String etreeName = identifier(wordC.identifier(1));
            checkTreeExists(etreeName, "Word '%s' is declared with unknown elementary tree '%s'.", word, etreeName);

            LexiconEntry lex = new LexiconEntry(word, etreeName, lexFs);
            tagg.addLexiconEntry(word, lex);
        }
    }

    /**
     * Look up a tree family by name. If the family is not defined, throw an exception with the given
     * error message.
     *
     * @throws CodecParseException
     */
    private List<String> lookupFamily(String familyName, String errorMessage, String... errorArgs) throws CodecParseException {
        List<String> treesInFamily = treeFamilies.get(familyName);

        if( treesInFamily == null ) {
            throw new CodecParseException(String.format(errorMessage, (Object[]) errorArgs));
        }

        return treesInFamily;
    }

    /**
     * Check that an elementary tree with the given name exists. If the tree name is not defined, throw an
     * exception with the given error message.
     *
     * @throws CodecParseException
     */
    private void checkTreeExists(String etreeName, String errorMessage, String... errorArgs) throws CodecParseException {
        ElementaryTree etree = tagg.getElementaryTree(etreeName);

        if( etree == null ) {
            throw new CodecParseException(String.format(errorMessage, (Object[]) errorArgs));
        }
    }

    private void buildLemma(LemmaContext lemmaC) {
        String lemma = identifier(lemmaC.identifier(0));
        List<String> etrees = null;
        FeatureStructure lemmaFs = buildFs(lemmaC.fs());

        if (lemmaC.familyIdentifier() != null) {
            // declared with tree family
            String familyName = familyIdentifier(lemmaC.familyIdentifier());
            etrees = lookupFamily(familyName, "Lemma '%s' is declared with unknown tree family '%s'.", lemma, familyName);
        } else {
            // declared with elementary tree name
            String etreeName = identifier(lemmaC.identifier(1));
            checkTreeExists(etreeName,"Lemma '%s' is declared with unknown elementary tree '%s'.", lemma, etreeName);
            etrees = Collections.singletonList(etreeName);
        }

        for (WordInLemmaContext wc : lemmaC.wordInLemma()) {
            String word = identifier(wc.identifier());
            FeatureStructure wordFs = lemmaFs;

            if (wc.fs() != null) {
                wordFs = wordFs.unify(buildFs(wc.fs()));
            }

            for (String etree : etrees) {
                if (lemmaC.familyIdentifier() != null) {
                    checkTreeExists(etree, "Lemma '%s' is declared with tree family '%s', which contains the unknown elementary tree '%s'.", word, familyIdentifier(lemmaC.familyIdentifier()), etree);
                }

                tagg.addLexiconEntry(word, new LexiconEntry(word, etree, wordFs));
            }
        }
    }

    private String identifier(IdentifierContext nc) {
        boolean isQuoted = (nc instanceof QUOTEDContext) || (nc instanceof DQUOTEDContext);
        return CodecUtilities.extractName(nc, isQuoted);
    }

    private String variable(VariableContext vc) {
        return vc.getText().substring(1); // strip "?"
    }

    private NodeType nodeType(MarkerContext marker) {
        if (marker instanceof SUBSTContext) {
            return NodeType.SUBSTITUTION;
        } else if (marker instanceof FOOTContext) {
            return NodeType.FOOT;
        } else if (marker instanceof ANCHORContext) {
            return NodeType.HEAD;
        } else {
            throw new RuntimeException("Invalid marker: " + marker.getText());
        }
    }

    private static String familyIdentifier(FamilyIdentifierContext identifier) {
        String s = identifier.getText();
        String stripped = s.substring(1, s.length() - 1); // strip off first and last character
        return stripped;
    }

    private NodeAnnotation nodeAnnotation(AnnotationContext annotation) {
        if( "@NA".equals(annotation.getText())) {
            return NodeAnnotation.NO_ADJUNCTION;
        } else {
            return NodeAnnotation.NONE;
        }
    }

}
