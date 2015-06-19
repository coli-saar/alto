/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import com.google.common.collect.Maps;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.codec.ParseException;
import de.up.ling.irtg.corpus.CorpusWriter;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

/**
 * Reads a corpus from some foreign format into an Alto corpus.
 * The foreign corpus contains entries of type E. The user provides
 * an Iterator<E> to walk over the entries of the foreign corpus.
 * In addition, the user provides a map that tells us, for each
 * interpretation, how values of type E should be converted into
 * objects of the algebra of that interpretation. Finally, the user
 * provides a mapping of interpretation names to algebra objects.<p>
 * 
 * The {@link #convert(java.util.Iterator) } method will then
 * traverse the iterator and write the converted outputs to
 * the writer.
 * 
 * @author koller
 */
public class CorpusConverter<E> {
    private Map<String, Function<E, ? extends Object>> conv;
    private InterpretedTreeAutomaton irtg;
    private CorpusWriter cw;

    public CorpusConverter(String inputCorpusName, Map<String, Algebra> algebras, Map<String, Function<E, ? extends Object>> conv, Writer writer) throws IOException {
        this.conv = conv;

        irtg = makeIrtg(algebras);
        cw = new CorpusWriter(irtg, false, "Converted from " + inputCorpusName + "\non " + new Date().toString(), writer);
    }

    public void convert(Iterator<E> inputCorpus) throws IOException {
        convert(inputCorpus, cw, instanceInInputCorpus -> {
            Instance inst = new Instance();
            Map<String, Object> objs = Maps.transformValues(conv, f -> f.apply(instanceInInputCorpus));
            inst.setInputObjects(objs);
            return inst;
        });
    }

    private InterpretedTreeAutomaton makeIrtg(Map<String, Algebra> algebras) {
        InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton<>());

        for (String i : algebras.keySet()) {
            irtg.addInterpretation(i, new Interpretation(algebras.get(i), new Homomorphism(irtg.getAutomaton().getSignature(), algebras.get(i).getSignature())));
        }

        return irtg;
    }

    private void convert(Iterator<E> corpus, CorpusWriter writer, Function<E, Instance> conv) throws ParseException, IOException {
        while (corpus.hasNext()) {
            E element = corpus.next();
            Instance instance = conv.apply(element);
            writer.accept(instance);
        }
    }
}
