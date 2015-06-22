/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import com.google.common.collect.Maps;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.tree.Tree;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Reads a corpus from some foreign format and converts it into an Alto corpus.
 * The foreign corpus contains entries of type E. The user provides an
 * Iterator<E> to walk over the entries of the foreign corpus. In addition, the
 * user provides a map that tells us, for each interpretation, how values of
 * type E should be converted into objects of the algebra of that
 * interpretation. Finally, the user provides a mapping of interpretation names
 * to algebra objects.<p>
 *
 * The {@link #convert(java.util.Iterator) } method will then traverse the
 * iterator and write the converted outputs to the writer.<p>
 * 
 * Alternatively, you can use the CorpusConverter as a {@link Consumer} in
 * the context of internal iteration.
 * 
 *
 * @author koller
 */
public class CorpusConverter<E> implements Consumer<E> {
    private final Map<String, Function<E, ? extends Object>> conv;
    private final InterpretedTreeAutomaton irtg;
    private final CorpusWriter cw;
    private Function<E,Tree<String>> derivationTreeMaker;

    public CorpusConverter(String inputCorpusName, Map<String, Algebra> algebras, Map<String, Function<E, ? extends Object>> conv, Writer writer) throws IOException {
        this.conv = conv;

        irtg = InterpretedTreeAutomaton.forAlgebras(algebras);
        cw = new CorpusWriter(irtg, false, "Converted from " + inputCorpusName + "\non " + new Date().toString(), writer);
    }

    public void setDerivationTreeMaker(Function<E, Tree<String>> derivationTreeMaker) {
        this.derivationTreeMaker = derivationTreeMaker;
        cw.setIsAnnotated(true);
    }
    
    public void convert(Iterator<E> inputCorpus) throws IOException {
        while (inputCorpus.hasNext()) {
            E element = inputCorpus.next();
            accept(element);
        }
    }

    private Instance toInstance(E instanceInInputCorpus) {
        Instance inst = new Instance();
        Map<String, Object> objs = Maps.transformValues(conv, f -> f.apply(instanceInInputCorpus));
        inst.setInputObjects(objs);
        return inst;
    }

    @Override
    public void accept(E element) {
        Instance instance = toInstance(element);
        
        if( derivationTreeMaker != null ) {
            Tree<String> dt = derivationTreeMaker.apply(element);
            instance.setDerivationTree(irtg.getAutomaton().getSignature().addAllSymbols(dt));
//            System.exit(0);
        }
        
        cw.accept(instance);
    }
}
