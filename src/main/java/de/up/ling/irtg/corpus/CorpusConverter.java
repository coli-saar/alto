/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import com.google.common.collect.Maps;
import de.up.ling.tree.Tree;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
 * Alternatively, you can use the CorpusConverter as a {@link Consumer} in the
 * context of internal iteration.
 *
 *
 * @author koller
 */
public class CorpusConverter<E> implements Consumer<E> {
    private final Map<String, Function<E, ? extends Object>> conv;
    private final AbstractCorpusWriter cw;
    private Function<E, Tree<Integer>> derivationTreeMaker;
    private List<Function<E, E>> transformations;
    private List<Consumer<E>> otherConsumers;

    public CorpusConverter(AbstractCorpusWriter cw, Map<String, Function<E, ? extends Object>> conv) throws IOException {
        this.conv = conv;
        this.cw = cw;
        transformations = new ArrayList<>();
        otherConsumers = new ArrayList<>();
    }

    public void setDerivationTreeMaker(Function<E, Tree<Integer>> derivationTreeMaker) {
        this.derivationTreeMaker = derivationTreeMaker;
        cw.setAnnotated(true);
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

    private E applyTransformations(E obj) {
        for (Function<E, E> trf : transformations) {
            obj = trf.apply(obj);
        }

        return obj;
    }

    public void addTransformation(Function<E, E> transformation) {
        transformations.add(transformation);
    }
    
    public void addConsumer(Consumer<E> consumer) {
        otherConsumers.add(consumer);
    }

    @Override
    public void accept(E element) {
        E transformed = applyTransformations(element);
        Instance instance = toInstance(transformed);
        
//        System.err.println(transformed);

        if (derivationTreeMaker != null) {
            Tree<Integer> dt = derivationTreeMaker.apply(transformed);
            instance.setDerivationTree(dt);
        }

        cw.accept(instance);
        
        for( Consumer<E> other : otherConsumers ) {
            other.accept(transformed);
        }
    }
}
