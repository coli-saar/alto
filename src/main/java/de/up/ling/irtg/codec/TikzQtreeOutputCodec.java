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
@CodecMetadata(name = "tikz-qtree", description = "tikz-qtree", type = Tree.class)
public class TikzQtreeOutputCodec extends OutputCodec<Tree> {
    @Override
    public void write(Tree tree, OutputStream ostream) throws IOException {
        PrintWriter w = new PrintWriter(new OutputStreamWriter(ostream));
        w.print("\\Tree");
        write(tree, "", w);
        w.flush();
    }
    
    private void write(Tree tree, String prefix, PrintWriter w) {
        if( tree.getChildren().isEmpty() ) {
            w.println(prefix + tree.getLabel().toString());
        } else {
            w.println(prefix + "[." + tree.getLabel().toString());
            tree.getChildren().forEach( child -> {
                write((Tree) child, prefix + "  ", w);
            });
            w.println(prefix + "]");
        }
    }
}
