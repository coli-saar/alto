/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.tree.Tree;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * An output codec that converts a tree to its yield string.
 * The yield string consists of the leaf labels of the tree, from
 * left to right, separated by single spaces. Thus, if the tree
 * is f(a, g(b,c)), then its encoding with this codec is "a b c".
 * 
 * @author koller
 */
@CodecMetadata(name = "treeAsSentence", description = "Converts a tree to its yield string", type = Tree.class)
public class TreeYieldOutputCodec extends OutputCodec<Tree> {
    @Override
    public void write(Tree tree, OutputStream ostream) throws IOException, UnsupportedOperationException {
        PrintWriter w = new PrintWriter(new OutputStreamWriter(ostream));
        w.write(String.join(" ", tree.getLeafLabels()));
        w.flush();
    }
    
}
