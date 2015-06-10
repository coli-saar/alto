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
<<<<<<< local
 * An output codec that encodes Tree objects as Latex code,
 * using the the <a href="https://www.ctan.org/pkg/tikz-qtree">tikz-qtree</a>
 * package. You can copy and paste this code into your Latex
 * document and have it typeset.
=======
 * Encodes a tree as a series of instructions for the
 * tikz-qtree tree drawing package. You can copy and paste
 * the resulting string into Latex and compile it.
 * (Don't forget to \\usepackage{tikz-qtree})
>>>>>>> other
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
