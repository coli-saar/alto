package de.up.ling.irtg.script;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.saar.basic.StringTools;
import de.saar.coli.algebra.OrderedFeatureTreeAlgebra;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.tree.Tree;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CogsCorpusGenerator {
    public static void main(String[] args) throws IOException {
        Args cmd = new Args();
        JCommander.newBuilder().addObject(cmd).build().parse(args);

        InterpretedTreeAutomaton irtg = new IrtgInputCodec().read(new FileInputStream(cmd.parameters.get(0)));

        for( int i = 0; i < cmd.count; i++ ) {
            Tree<String> dt = irtg.getAutomaton().getRandomTree();

            List<String> englishValue = (List<String>) irtg.interpret(dt, "english");
            String english = StringTools.join(englishValue, " ");

            OrderedFeatureTreeAlgebra.OrderedFeatureTree ft = (OrderedFeatureTreeAlgebra.OrderedFeatureTree) irtg.interpret(dt, "semantics");

            System.out.printf("%s\t%s\n", english, ft.toString(true));
        }


    }

    public static class Args {
        @Parameter
        private List<String> parameters = new ArrayList<>();

        @Parameter(names = "--count", description="How many instances to generate")
        private int count = 10;
    }
}
