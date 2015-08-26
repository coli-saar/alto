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
 *
 * @author koller
 */
@CodecMetadata(name = "ptb-out", description = "encodes a tree as a PTB-style Lisp string", type = Tree.class)
public class PtbTreeOutputCodec extends OutputCodec<Tree> {
    @Override
    public void write(Tree tree, OutputStream ostream) throws IOException, UnsupportedOperationException {
        if( hasTrueOption("top")) {
            tree = Tree.create("TOP", tree);
        }
        
        PrintWriter w = new PrintWriter(new OutputStreamWriter(ostream));
        w.write(tree.toLispString());
        w.flush();
    }
}
