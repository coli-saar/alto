package de.up.ling.irtg.script;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.BinaryIrtgInputCodec;
import de.up.ling.irtg.codec.BinaryIrtgOutputCodec;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

public class TestAutomataMemorySize {

    public static void main(String[] args) throws IOException, InterruptedException {
        StringAlgebra stringAlgebra = new StringAlgebra();
        BinaryIrtgOutputCodec binaryCodecOut = new BinaryIrtgOutputCodec();
        BinaryIrtgInputCodec binaryCodecIn = new BinaryIrtgInputCodec();
        TreeAutomatonInputCodec codecIn = new TreeAutomatonInputCodec();

        int length = 20;
        List<String> sent = new ArrayList<>();
        for (int i = 0; i<length; i++) {
            sent.add("w"+i);
        }

        ConcreteTreeAutomaton<String> decomp = toStringAutomaton(stringAlgebra.decompose(sent));

        // write to text file
        FileWriter w = new FileWriter("temp.auto");
        w.write(decomp.toString());
        w.close();

        //write to binary file
        binaryCodecOut.write(new InterpretedTreeAutomaton(decomp), new FileOutputStream("temp.irtb"));

        //load from text file
        ConcreteTreeAutomaton<String> stringFileIn = (ConcreteTreeAutomaton<String>)codecIn.read(new FileInputStream("temp.auto"));
//
//        //load from binary file
//        ConcreteTreeAutomaton<String> binaryFileIn = (ConcreteTreeAutomaton<String>)binaryCodecIn.read(new FileInputStream("temp.irtb")).getAutomaton();

        //print automata
//        System.out.println("read from string file:");
//        System.out.println(stringFileIn);
//        System.out.println("read from binary file:");
//        System.out.println(binaryFileIn);
        System.out.println("Done! Going to sleep for five minutes for memory profiling");

        sleep(300000);
    }

    private static ConcreteTreeAutomaton<String> toStringAutomaton(TreeAutomaton<?> auto) {
        ConcreteTreeAutomaton<String> ret = new ConcreteTreeAutomaton<>();
        for (Rule rule : auto.getRuleSet()) {
            List<String> children = Arrays.stream(rule.getChildren()).mapToObj(
                    childID -> auto.getStateForId(childID).toString()).collect(Collectors.toList());
            String parent = auto.getStateForId(rule.getParent()).toString();
            ret.addRule(ret.createRule(parent, rule.getLabel(auto), children));
        }
        for (int finalStateID : auto.getFinalStates()) {
            ret.addFinalState(ret.getIdForState(auto.getStateForId(finalStateID).toString()));
        }
        return ret;
    }

}
