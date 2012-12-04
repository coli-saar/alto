/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.tree.Tree;
import java.io.Reader;
import java.io.StringReader;

/**
 *
 * @author koller
 */
public class PtbTreeAlgebra extends TreeAlgebra {
    @Override
    public Tree<String> parseString(String representation) throws ParserException {
        return parseFromReader(new StringReader(representation));
    }
    
    public Tree<String> parseFromReader(Reader reader) throws ParserException {
        return null;
        // TODO - implement me
    }
}
