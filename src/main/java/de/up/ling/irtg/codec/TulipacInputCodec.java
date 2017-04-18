/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import com.google.common.io.Files;
import de.saar.coli.featstruct.AvmFeatureStructure;
import de.saar.coli.featstruct.FeatureStructure;
import de.saar.coli.featstruct.PlaceholderFeatureStructure;
import de.saar.coli.featstruct.PrimitiveFeatureStructure;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.codec.tag.ElementaryTree;
import de.up.ling.irtg.codec.tag.LexiconEntry;
import de.up.ling.irtg.codec.tag.Node;
import de.up.ling.irtg.codec.tag.NodeType;
import de.up.ling.irtg.codec.tag.TagGrammar;
import de.up.ling.irtg.codec.tulipac.TulipacLexer;
import de.up.ling.irtg.codec.tulipac.TulipacParser;
import java.io.IOException;
import java.io.InputStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;

import static de.up.ling.irtg.codec.tulipac.TulipacParser.*;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        TulipacLexer l = new TulipacLexer(new ANTLRInputStream(is));
        TulipacParser p = new TulipacParser(new CommonTokenStream(l));
        p.setErrorHandler(new ExceptionErrorStrategy());
        p.getInterpreter().setPredictionMode(PredictionMode.SLL);

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

        // TODO - annotations
        
        
        if (!node.marker().isEmpty()) {
            type = buildNodeType(node.marker(0));
        }

        Node n = new Node(label, type);

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

        for (FtContext ft : fs.ft()) {
            addToFs(ft, ret);
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

        // TODO - feature structure that came with word declaration
        
        if (wordC.familyIdentifier() != null) {
            // declared with tree family
            for (String etreeName : treeFamilies.get(familyIdentifier(wordC.familyIdentifier()))) {
                tagg.addLexiconEntry(word, new LexiconEntry(word, etreeName));
            }
        } else {
            // declared with elementary tree name
            String etreeName = identifier(wordC.identifier(1));
            tagg.addLexiconEntry(word, new LexiconEntry(word, etreeName));
        }
    }

    private void buildLemma(LemmaContext lemmaC) {
        String lemma = identifier(lemmaC.identifier(0));
        List<String> etrees = null;
        
        // TODO - feature structure that came with word declaration
        
        if (lemmaC.familyIdentifier() != null) {
            // declared with tree family
            etrees = treeFamilies.get(familyIdentifier(lemmaC.familyIdentifier()));
        } else {
            // declared with elementary tree name
            etrees = Collections.singletonList(identifier(lemmaC.identifier(1)));
        }
        
        for( WordInLemmaContext wc : lemmaC.wordInLemma() ) {
            String word = identifier(wc.identifier());
            // TODO - feature structure
            
            for( String etree : etrees ) {
                tagg.addLexiconEntry(word, new LexiconEntry(word, etree));
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

    private NodeType buildNodeType(MarkerContext marker) {
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

    public static void main(String[] args) throws FileNotFoundException, CodecParseException, IOException {
        TulipacInputCodec tic = new TulipacInputCodec();
        InterpretedTreeAutomaton irtg = tic.read(new FileInputStream("/Users/koller/Dropbox/Documents/Lehre/alt/gramf-11/tag/new-shieber.tag"));
        Files.write(irtg.toString().getBytes(), new File("shieber.irtg"));
    }

    private static String familyIdentifier(FamilyIdentifierContext identifier) {
        String s = identifier.getText();
        String stripped = s.substring(1, s.length()-1); // strip off first and last character
        return stripped;
    }

}
