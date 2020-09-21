package de.up.ling.irtg.script;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.BinaryIrtgInputCodec;
import de.up.ling.irtg.codec.BinaryIrtgOutputCodec;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

public class TestAutomataMemorySize2 {

    public static void main(String[] args) throws IOException, InterruptedException {

        BinaryIrtgInputCodec binaryCodecIn = new BinaryIrtgInputCodec();
        Scanner scanner = new Scanner(System. in);

        TreeAutomaton<String> auto = binaryCodecIn.read(new FileInputStream("test.irtb")).getAutomaton();

        System.out.println("waiting for input, getRuleSet is next");
        System.out.println(scanner.nextLine());

        auto.getRuleSet();

        System.out.println("waiting for input, inside is next");
        System.out.println(scanner.nextLine());

        Int2ObjectMap<Double> insides = auto.logInside();

        System.out.println("waiting for input, outside is next");
        System.out.println(scanner.nextLine());

        auto.logOutside(insides);

        System.out.println("waiting for input to finish program");
        System.out.println(scanner.nextLine());

    }


}
