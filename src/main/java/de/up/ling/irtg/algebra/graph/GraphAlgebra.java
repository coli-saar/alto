/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import com.google.common.collect.Sets;
import de.up.ling.irtg.algebra.EvaluatingAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;

/**
 *
 * @author koller
 */
public class GraphAlgebra extends EvaluatingAlgebra<SGraph> {
    @Override
    protected SGraph evaluate(String label, List<SGraph> childrenValues) {
        try {
            if (label == null) {
                return null;
            } else if (label.equals("merge")) {
                return childrenValues.get(0).merge(childrenValues.get(1));
            } else if (label.startsWith("r_")) {
                String[] parts = label.split("_");
                
                if( parts.length == 2 ) {
                    parts = new String[] { "r", "root", parts[1] };
                }
                
                return childrenValues.get(0).renameSource(parts[1], parts[2]);
            } else if( label.equals("f")) {
                // forget all sources
                return childrenValues.get(0).forgetSourcesExcept(Collections.EMPTY_SET);
            } else if( label.equals("fr")) {
                // forget all sources, except "root"
                return childrenValues.get(0).forgetSourcesExcept(Collections.singleton("root"));
            } else if( label.startsWith("fe_") || label.startsWith("f_")) {
                // forget all sources, except ...
                String[] parts = label.split("_");
                Set<String> retainedSources = new HashSet<>();
                for( int i = 1; i < parts.length; i++ ) {
                    retainedSources.add(parts[i]);
                }
                
                if( label.startsWith("f_")) {
                    retainedSources = Sets.difference(childrenValues.get(0).getAllSources(), retainedSources);
                }
                
                return childrenValues.get(0).forgetSourcesExcept(retainedSources);
            } else {
                SGraph sgraph = IsiAmrParser.parse(new StringReader(label));
                return sgraph.withFreshNodenames();
            }
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Could not parse operation \"" + label + "\": " + ex.getMessage());
        }
    }

    @Override
    protected boolean isValidValue(SGraph value) {
        return true;
    }

    @Override
    public SGraph parseString(String representation) throws ParserException {
        try {
            return IsiAmrParser.parse(new StringReader(representation));
        } catch (ParseException ex) {
            throw new ParserException(ex);
        }
    }

    @Override
    public JComponent visualize(SGraph graph) {
        return SGraphDrawer.makeComponent(graph);
    }

    @Override
    public Map<String, String> getRepresentations(SGraph object) {
        Map<String, String> ret = super.getRepresentations(object);

        ret.put("ISI-style AMR", object.toIsiAmrString());

        return ret;
    }

}
