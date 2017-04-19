/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec.tag;

import de.saar.basic.Pair;
import de.saar.coli.featstruct.AvmFeatureStructure;
import de.saar.coli.featstruct.PrimitiveFeatureStructure;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.codec.CodecMetadata;
import de.up.ling.irtg.codec.CodecParseException;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.util.MutableInteger;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author koller
 */
@CodecMetadata(name = "chen-tag", description = "Tree-adjoining grammar (Chen format)", type = InterpretedTreeAutomaton.class)
public class ChenTagInputCodec extends InputCodec<InterpretedTreeAutomaton> {

    @Override
    public InterpretedTreeAutomaton read(InputStream is) throws CodecParseException, IOException {
        TagGrammar tagg = readUnlexicalizedGrammar(new InputStreamReader(is));

        // todo - read lexicon
        return tagg.toIrtg();
    }

    private static String findSecondary(AvmFeatureStructure features) {
        if (features.getAttributes().contains("sgp1")) {
            return (String) features.get("sgp1").getValue();
        } else if (features.get("prt1") == null) {
            return null;
        } else {
            return (String) features.get("prt1").getValue();
        }
    }

    // makes some assumptions that are specific to the Chen input
    // codec => maybe move there
    public List<Tree<String>> lexicalizeFromCorpus(TagGrammar tagg, Reader r) throws IOException {
        BufferedReader br = new BufferedReader(r);
        String line = null;

        // for constructing and storing the derivation trees
        List<Tree<String>> ret = new ArrayList<>();
        NodePosToChildrenPos npcp = new NodePosToChildrenPos(tagg);
        Int2ObjectMap<String> posToLabel = new Int2ObjectOpenHashMap<>();
        Int2ObjectMap<IntList> posToChildPositions = new Int2ObjectOpenHashMap<>();
        int rootPos = 0;

        int counter = 0;

        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\\s+");

            if (parts.length >= 2) {
                String word = parts[1];
                String treename = parts[7];
                LexiconEntry lex = new LexiconEntry(word, treename);
                AvmFeatureStructure fs = new AvmFeatureStructure();

                for (int i = 10; i < parts.length; i++) {
                    assert parts[i].contains("=");
                    String[] split = parts[i].split("=");
                    fs.put(split[0], new PrimitiveFeatureStructure(split[1]));
//                    lex.addFeature(split[0], split[1]);
                }

                lex.setFeatureStructure(fs);
                lex.setSecondaryLex(findSecondary(fs));

                tagg.addLexiconEntry(word, lex);

                // add to derivation tree maps
                if (!"tCO".equals(treename)) {
                    // tCO is for secondary lexical anchors, such as "invest ... _in_".
                    // These trees are not spelled out in the IRTG grammar, and can be
                    // skipped here. (Instead, the secondary lexical anchors are added
                    // as constants to the homomorphisms in the IRTG.)
                    int pos = Integer.parseInt(parts[0]);
                    int parentPos = Integer.parseInt(parts[3]);
                    String parentTreename = parts[8];
                    String label = TagGrammar.makeTerminalSymbol(lex); //  treename + "-" + word;

                    posToLabel.put(pos, label);

                    if (pos == parentPos) {
                        // root
                        rootPos = pos;
                    } else {
                        IntList children = posToChildPositions.get(parentPos);
                        if (children == null) {
                            children = makeIntList(npcp.getNumChildren(parentTreename), -1);
                            posToChildPositions.put(parentPos, children);
                        }

                        int posInParentChildrenList = Integer.parseInt(parts[9]);
//                        System.err.println("(" + treename + ")");
                        children.set(npcp.getChildPosForNodePos(parentTreename, posInParentChildrenList), pos);
//                        System.err.println(String.format("[%d: %s] posInParent=%d, posInParentChildren=%d, parentTree=%s", pos, word, posInParentChildrenList, npcp.getChildPosForNodePos(parentTreename, posInParentChildrenList), parentTreename));
                    }
                }
            } else {
                // finished reading the sentence
                if (!posToLabel.isEmpty()) {
                    Tree<String> dt = makeDerivationTree(rootPos, posToLabel, posToChildPositions, npcp, tagg);
//                    System.err.println(" --> " + dt + "\n");
                    ret.add(dt);
                    posToLabel.clear();
                    posToChildPositions.clear();
                    rootPos = -1;

//                    counter++;
//                    if (counter > 10) {
//                        System.exit(1);
//                    }
                }
            }
        }

        return ret;
    }

    private static IntList makeIntList(int size, int value) {
        IntList ret = new IntArrayList();
        for (int i = 0; i < size; i++) {
            ret.add(value);
        }
        return ret;
    }

    private static Tree<String> makeDerivationTree(int nodePos, Int2ObjectMap<String> posToLabel, Int2ObjectMap<IntList> posToChildPositions, NodePosToChildrenPos npcp, TagGrammar tagg) {
        IntList children = posToChildPositions.get(nodePos);
        List<Tree<String>> childTrees = new ArrayList<>();
        String label = posToLabel.get(nodePos);
        String treename = label.split("-")[0];
        List<String> childStates = tagg.getChildStates(treename); //tagg.getElementaryTree(treename));

//        System.err.println("mkdt nodePos=" + nodePos + ", label=" + label);
//        if(children != null) System.err.println("  children=" + children);
//        System.err.println("  childStates=" + childStates);
//        System.err.println("  ")
        if (children == null) {
            // node has only *NOP* children
            children = makeIntList(npcp.getNumChildren(treename), -1);
        }

        for (int i = 0; i < children.size(); i++) {
            int child = children.get(i);

            if (child < 0) {
                // no explicit child specified => put *NOP* of appropriate type here
                childTrees.add(Tree.create(TagGrammar.makeNop(childStates.get(i))));
            } else {
                // otherwise, create subtree and put it here
                childTrees.add(makeDerivationTree(child, posToLabel, posToChildPositions, npcp, tagg));
            }
        }

        return Tree.create(posToLabel.get(nodePos), childTrees);
    }

    public TagGrammar readUnlexicalizedGrammar(Reader r) throws IOException {
        TagGrammar tagg = new TagGrammar();

        BufferedReader br = new BufferedReader(r);
        String line = null;

        try {
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\s+");

                if (parts.length > 1) { // skip blank lines
                    Tree<Node> t = decodeElementaryTree(parts, new MutableInteger(1));
                    boolean isAuxiliary = t.some(p -> p.getType().equals(NodeType.FOOT));
                    tagg.addElementaryTree(parts[0], new ElementaryTree(t, isAuxiliary ? ElementaryTreeType.AUXILIARY : ElementaryTreeType.INITIAL));
                }
            }

            return tagg;
        } catch (CodecParseException e) {
            throw new CodecParseException("Error while parsing line: " + line + ": " + e.getMessage());
        }
    }

    private String[] splitNodeDescriptor(String nodeDescriptor) {
        String[] pparts = nodeDescriptor.split("#", -1); // including trailing empty string
        String[] ret = null;

        if (pparts.length == 5) {
            return pparts;
        } else {
            // has shape like "###2#l#h"

            assert pparts.length == 6;
            ret = new String[5];
            ret[0] = "#";
            ret[1] = pparts[2];
            ret[2] = pparts[3];
            ret[3] = pparts[4];
            ret[4] = pparts[5];
            return ret;
        }
    }

    private Tree<Node> decodeElementaryTree(String[] parts, MutableInteger pos) {
        String[] pparts = splitNodeDescriptor(parts[pos.incValue()]);
        String nt = pparts[0];
        NodeType nodeType = decodeNodetype(pparts[4]);
        String nodename = pparts[2];
        List<Tree<Node>> children = new ArrayList<>();

        assert "l".equals(pparts[3]);

        while (pos.getValue() < parts.length) {
            pparts = splitNodeDescriptor(parts[pos.getValue()]);
            if (pparts[2].equals(nodename)) {
                pos.incValue();
                break;
            }

            children.add(decodeElementaryTree(parts, pos));
        }

        return Tree.create(new Node(nt, nodeType), children);
    }

    private NodeType decodeNodetype(String marker) {
        if ("".equals(marker)) {
            return NodeType.DEFAULT;
        } else if ("f".equals(marker)) {
            return NodeType.FOOT;
        } else if ("h".equals(marker)) {
            return NodeType.HEAD;
        } else if ("c".equals(marker)) {
            return NodeType.SECONDARY_LEX;
        } else if ("s".equals(marker)) {
            return NodeType.SUBSTITUTION;
        } else {
            throw new CodecParseException("Unknown node type: " + marker);
        }
    }

    private static class NodePosToChildrenPos {

        private Map<String, Int2IntMap> map = new HashMap<String, Int2IntMap>();
        private Object2IntMap<String> etreeNumChildren = new Object2IntOpenHashMap<String>();
        private TagGrammar tagg;
        private static final boolean DEBUG = false;

        public NodePosToChildrenPos(TagGrammar tagg) {
            this.tagg = tagg;
        }

        private void ensureEtreeCalculated(String etreeName) {
            if (!etreeNumChildren.containsKey(etreeName)) {
                if (DEBUG) {
                    System.err.println("\n====== etree: " + etreeName);
                    System.err.println("tree: " + tagg.getElementaryTree(etreeName).getTree());
                }

                map.put(etreeName, computeNodeMapping(etreeName));
                // side-effect of computeNodeMapping: etreeNumChildren is set to num children of this etree

                if (DEBUG) {
                    System.err.println("# children: " + etreeNumChildren.get(etreeName));
                }
            }
        }

        public int getChildPosForNodePos(String etreeName, int nodePos) {
            ensureEtreeCalculated(etreeName);

            Int2IntMap cToN = map.get(etreeName);
            return cToN.get(nodePos);
        }

        public int getNumChildren(String etreeName) {
            ensureEtreeCalculated(etreeName);
            return etreeNumChildren.get(etreeName);
        }

        private Int2IntMap computeNodeMapping(String etreeName) {
            // compute tree of post-order visit positions, as when generating
            // the sequence of children in the IRTG rule (see TagGrammar#toIrtg)
            MutableInteger numChildren = new MutableInteger(0);
            Tree<Integer> dfsNodePositions = tagg.makeDfsNodePositions(etreeName, numChildren); // tagg.getElementaryTree(etreeName)
            etreeNumChildren.put(etreeName, numChildren.getValue());

            final Int2IntMap ret = new Int2IntOpenHashMap();
            final MutableInteger nextPosition = new MutableInteger(1);

            if (DEBUG) {
                System.err.println("dfs tree: " + dfsNodePositions);
            }

            // visit nodes in pre-order, as in the tree definitions of the
            // Chen f.str file, and generate pre-order -> post-order mapping
            dfsNodePositions.dfs(new TreeVisitor<Integer, Void, Void>() {
                @Override
                public Void visit(Tree<Integer> node, Void data) {
                    int nodePos = nextPosition.incValue();

                    if (node.getLabel() >= 0) {
                        // skip nodes that do not generate rule children
                        ret.put(nodePos, node.getLabel().intValue());
                    }

                    return null;
                }
            });

            if (DEBUG) {
                System.err.println("mapping: " + ret);
            }
            return ret;
        }
    }

}
