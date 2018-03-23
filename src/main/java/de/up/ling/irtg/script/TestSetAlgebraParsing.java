/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.TemplateInterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.SetAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.TemplateIrtgInputCodec;
import de.up.ling.irtg.util.FirstOrderModel;
import java.io.StringReader;
import java.util.HashMap;

/**
 *
 * @author villalba
 */
public class TestSetAlgebraParsing {
    public static String model = "{'behind' : [['br130'], ['fr1'], ['br132'], ['br120'], " +
        "['br131'], ['br19'], ['br127'], ['br129'], ['br128']],\n" +
        "'green' : [['br123'], ['br122']],\n" +
        "'lamp' : [['lr11']],\n" +
        "'right' : [['br123'], ['br125'], ['br124']],\n" +
        "'above-of' : [['br11', 'br16'], ['br11', 'br15'], ['br13', 'br16'], " +
        "['br13', 'br15'], ['br17', 'br18'], ['br15', 'br16'], " +
        "['br13', 'br14'], ['br17', 'br16'], ['br127', 'br120'], " +
        "['br128', 'br132'], ['br128', 'br130'], ['br128', 'br131'], " +
        "['br132', 'br130'], ['br125', 'br123'], ['br11', 'br18'], " +
        "['br128', 'br129'], ['br132', 'br120'], ['br131', 'br130'], " +
        "['br128', 'br127'], ['br18', 'br16'], ['br130', 'br129'], " +
        "['br120', 'br129'], ['br132', 'br129'], ['br12', 'br18'], " +
        "['br14', 'br18'], ['br12', 'br16'], ['br12', 'br15'], " +
        "['br14', 'br16'], ['br12', 'br14'], ['br14', 'br15'], " +
        "['br12', 'br13'], ['br12', 'br11'], ['br127', 'br131'], " +
        "['br128', 'br120'], ['br123', 'br124'], ['br120', 'br130'], " +
        "['br127', 'br129'], ['br132', 'br131'], ['br125', 'br124'], " +
        "['br131', 'br129'], ['br17', 'br14'], ['br17', 'br11']],\n" +
        "'picture' : [['pr1']],\n" +
        "'right-of' : [['br11', 'br17'], ['br15', 'br18'], ['br15', 'br17'], " +
        "['br15', 'br16'], ['br15', 'br14'], ['br132', 'br130'], " +
        "['br129', 'br127'], ['br125', 'br123'], ['br123', 'lr11'], " +
        "['br124', 'br123'], ['br128', 'br127'], ['br132', 'br120'], " +
        "['br132', 'br127'], ['wr12', 'br15'], ['br17', 'wr11'], " +
        "['wr12', 'br13'], ['br120', 'br128'], ['br120', 'br127'], " +
        "['wr12', 'br12'], ['br132', 'br128'], ['br16', 'br11'], " +
        "['br120', 'br129'], ['br132', 'br129'], ['br12', 'br18'], " +
        "['br12', 'br17'], ['br12', 'br16'], ['br16', 'br18'], " +
        "['br12', 'br14'], ['br16', 'br17'], ['br12', 'br11'], " +
        "['br130', 'br131'], ['wr12', 'br126'], ['br131', 'br120'], " +
        "['br18', 'wr11'], ['br19', 'fr1'], ['wr11', 'br15'], " +
        "['wr11', 'br14'], ['br13', 'br11'], ['br131', 'br129'], " +
        "['br131', 'br127'], ['br131', 'br128'], ['wr12', 'br122'], " +
        "['br13', 'br18'], ['br13', 'br17'], ['br13', 'br16'], " +
        "['br13', 'br15'], ['br13', 'br14'], ['br13', 'br12'], " +
        "['br121', 'pr1'], ['br121', 'wr11'], ['br130', 'br120'], " +
        "['br122', 'br126'], ['br130', 'br128'], ['br14', 'br11'], " +
        "['br130', 'br129'], ['br130', 'br127'], ['br11', 'wr11'], " +
        "['br14', 'br18'], ['br14', 'br17'], ['br14', 'br16'], " +
        "['br18', 'br17'], ['br126', 'lr11'], ['br122', 'lr11'], " +
        "['br132', 'br131'], ['br125', 'br124'], ['br16', 'wr11'], " +
        "['br15', 'br12'], ['br15', 'br11']],\n" +
        "'flower' : [['fr1']],\n" +
        "'button' : [['br11'], ['br130'], ['br121'], ['br132'], ['br120'], " +
        "['br131'], ['br18'], ['br123'], ['br19'], ['br122'], ['br16'], " +
        "['br125'], ['br17'], ['br124'], ['br14'], ['br127'], ['br15'], " +
        "['br126'], ['br12'], ['br129'], ['br13'], ['br128']],\n" +
        "'red' : [['br130'], ['br13']],\n" +
        "'left-of' : [['lr11', 'br126'], ['br11', 'br16'], ['br11', 'br15'], " +
        "['br11', 'br14'], ['br11', 'br13'], ['br11', 'br12'], " +
        "['br128', 'br132'], ['br129', 'br120'], ['br128', 'br130'], " +
        "['br128', 'br131'], ['br131', 'br132'], ['br124', 'br125'], " +
        "['br131', 'br130'], ['br16', 'br14'], ['br16', 'br13'], " +
        "['br13', 'wr12'], ['br16', 'br12'], ['wr11', 'br121'], " +
        "['br12', 'br15'], ['br12', 'br13'], ['br16', 'br15'], " +
        "['br127', 'br131'], ['br127', 'br132'], ['fr1', 'br19'], " +
        "['br128', 'br120'], ['br127', 'br130'], ['br123', 'br124'], " +
        "['br127', 'br128'], ['br130', 'br132'], ['br127', 'br129'], " +
        "['br123', 'br125'], ['wr11', 'br18'], ['wr11', 'br17'], " +
        "['wr11', 'br16'], ['br14', 'wr11'], ['br17', 'br15'], " +
        "['wr11', 'br11'], ['br17', 'br14'], ['br17', 'br13'], " +
        "['br17', 'br12'], ['br17', 'br11'], ['br17', 'br18'], " +
        "['br17', 'br16'], ['br127', 'br120'], ['br15', 'wr12'], " +
        "['br18', 'br16'], ['br14', 'br12'], ['br15', 'wr11'], " +
        "['br18', 'br15'], ['br18', 'br14'], ['br18', 'br13'], " +
        "['br18', 'br12'], ['pr1', 'br121'], ['br14', 'br15'], " +
        "['br129', 'br130'], ['br14', 'br13'], ['br126', 'wr12'], " +
        "['br129', 'br131'], ['br129', 'br132'], ['br122', 'wr12'], " +
        "['br126', 'br122'], ['br120', 'br131'], ['br120', 'br130'], " +
        "['br120', 'br132'], ['br15', 'br13'], ['lr11', 'br122'], " +
        "['br12', 'wr12'], ['lr11', 'br123']],\n" +
        "'cs' : [['br123'], ['br122'], ['br16'], ['dh0b1'], ['br14'], " +
        "['br126'], ['br15'], ['br12'], ['br13']],\n" +
        "'blue' : [['br11'], ['br121'], ['br132'], ['br120'], ['br131'], " +
        "['br18'], ['br19'], ['br16'], ['br125'], ['br17'], ['br124'], " +
        "['br14'], ['br127'], ['br15'], ['br126'], ['br12'], ['br129'], ['br128']],\n" +
        "'left' : [['br11'], ['br130'], ['fr1'], ['br121'], ['br132'], " +
        "['br120'], ['br131'], ['wr12'], ['wr11'], ['pr1'], ['lr11'], " +
        "['br18'], ['br19'], ['br122'], ['br16'], ['br17'], ['br14'], " +
        "['br127'], ['br15'], ['br126'], ['br12'], ['br129'], ['br13'], ['br128']],\n" +
        "'below-of' : [['br11', 'br17'], ['br11', 'br12'], ['br15', 'br14'], " +
        "['br13', 'br12'], ['br129', 'br120'], ['br129', 'br127'], " +
        "['br130', 'br120'], ['br131', 'br132'], ['br124', 'br125'], " +
        "['br124', 'br123'], ['br130', 'br128'], ['br16', 'br14'], " +
        "['br14', 'br12'], ['br120', 'br128'], ['br16', 'br13'], " +
        "['br120', 'br127'], ['br132', 'br128'], ['br18', 'br14'], " +
        "['br16', 'br12'], ['br16', 'br11'], ['br18', 'br12'], " +
        "['br18', 'br11'], ['br14', 'br17'], ['br16', 'br18'], " +
        "['br16', 'br17'], ['br129', 'br130'], ['br18', 'br17'], " +
        "['br16', 'br15'], ['br14', 'br13'], ['br129', 'br131'], " +
        "['br129', 'br132'], ['br130', 'br131'], ['br127', 'br128'], " +
        "['br130', 'br132'], ['br123', 'br125'], ['br129', 'br128'], " +
        "['br120', 'br132'], ['br15', 'br13'], ['br15', 'br12'], " +
        "['br15', 'br11'], ['br131', 'br127'], ['br131', 'br128']],\n" +
        "'windowwide' : [['wr12'], ['wr11']],\n" +
        "'front' : [['br11'], ['br121'], ['wr12'], ['wr11'], ['pr1'], " +
        "['lr11'], ['br18'], ['br123'], ['br122'], ['br16'], ['br125'], " +
        "['br17'], ['br124'], ['br14'], ['br15'], ['br126'], ['br12'], " +
        "['br13']]}";

    public static String tirtg = "interpretation sem: de.up.ling.irtg.algebra.SetAlgebra\n" +
        "interpretation string: de.up.ling.irtg.algebra.StringAlgebra\n" +
        "\n" +
        "foreach { a | trophy(a) }:\n" +
        "N_$a -> trophy_$a [0.5]\n" +
        "[sem] trophy\n" +
        "[string] trophy\n" +
        "\n" +
        "foreach { a | button(a) }:\n" +
        "N_$a -> button_$a [0.5]\n" +
        "[sem] button\n" +
        "[string] button\n" +
        "\n" +
        "foreach { a | stateless-button(a) }:\n" +
        "N_$a -> button_$a [0.5]\n" +
        "[sem] button\n" +
        "[string] button\n" +
        "\n" +
        "foreach { a | chair(a) }:\n" +
        "N_$a -> chair_$a [0.5]\n" +
        "[sem] chair\n" +
        "[string] chair\n" +
        "\n" +
        "foreach { a | lamp(a) }:\n" +
        "N_$a -> lamp_$a [0.5]\n" +
        "[sem] lamp\n" +
        "[string] lamp\n" +
        "\n" +
        "foreach { a | bed(a) }:\n" +
        "N_$a -> bed_$a [0.5]\n" +
        "[sem] bed\n" +
        "[string] bed\n" +
        "\n" +
        "foreach { a | flower(a) }:\n" +
        "N_$a -> flower_$a [0.5]\n" +
        "[sem] flower \n" +
        "[string] plant\n" +
        "\n" +
        "foreach { a | windowwide(a) }:\n" +
        "N_$a -> window_$a [0.5]\n" +
        "[sem] windowwide\n" +
        "[string] window\n" +
        "\n" +
        "foreach { a | picture(a) }:\n" +
        "N_$a -> picture_$a [0.5]\n" +
        "[sem] picture \n" +
        "[string] picture\n" +
        "\n" +
        "foreach { a | door(a) }:\n" +
        "N_$a -> door_$a [0.5]\n" +
        "[sem] door \n" +
        "[string] doorway \n" +
        "\n" +
        "foreach {a | blue(a) }:\n" +
        "N_$a -> blue_$a(N_$a) [0.5]\n" +
        "[sem] intersect_1(blue, ?1)\n" +
        "[string] *(blue, ?1)\n" +
        "\n" +
        "foreach { a | red(a) }:\n" +
        "N_$a -> red_$a(N_$a) [0.5]\n" +
        "[sem] intersect_1(red, ?1)\n" +
        "[string] *(red, ?1)\n" +
        "\n" +
        "foreach { a | yellow(a) }:\n" +
        "N_$a -> yellow_$a(N_$a) [0.5]\n" +
        "[sem] intersect_1(yellow, ?1)\n" +
        "[string] *(yellow, ?1)\n" +
        "\n" +
        "foreach { a | green(a) }:\n" +
        "N_$a -> green_$a(N_$a) [0.5]\n" +
        "[sem] intersect_1(green, ?1)\n" +
        "[string] *(green, ?1)\n" +
        "\n" +
        "foreach { a | round(a) }:\n" +
        "N_$a -> round_$a(N_$a)  [0.5]\n" +
        "[sem] intersect_1(round, ?1)\n" +
        "[string] *(round, ?1)\n" +
        "\n" +
        "foreach { a | square(a) }:\n" +
        "N_$a -> square_$a(N_$a)  [0.5]\n" +
        "[sem] intersect_1(square, ?1)\n" +
        "[string] *(square, ?1)\n" +
        "\n" +
        "foreach { a,b | right-of(a,b) }:\n" +
        "N_$a -> rightof_$a_$b(N_$a, NP_$b) [0.5]\n" +
        "[sem] project_1(intersect_2(intersect_1(right-of, ?1), ?2))\n" +
        "[string] *(?1, *(\"to the right of\", ?2))\n" +
        "\n" +
        "foreach { a,b | left-of(a,b) }:\n" +
        "N_$a -> leftof_$a_$b(N_$a, NP_$b) [0.5]\n" +
        "[sem] project_1(intersect_2(intersect_1(left-of, ?1), ?2))\n" +
        "[string] *(?1, *(\"to the left of\", ?2))\n" +
        "\n" +
        "foreach { a,b | above-of(a,b) }:\n" +
        "N_$a -> above_$a_$b(N_$a, NP_$b) [0.5]\n" +
        "[sem] project_1(intersect_2(intersect_1(above-of, ?1), ?2))\n" +
        "[string] *(?1, *(\"above\", ?2))\n" +
        "\n" +
        "foreach { a,b | below-of(a,b) }:\n" +
        "N_$a -> below_$a_$b(N_$a, NP_$b) [0.5]\n" +
        "[sem] project_1(intersect_2(intersect_1(below-of, ?1), ?2))\n" +
        "[string] *(?1, *(\"underneath\", ?2))\n" +
        "\n" +
        "foreach { a | T(a) }:\n" +
        "NP_$a! -> def_$a(N_$a) [0.5]\n" +
        "[sem] project_1(?1)\n" +
        "[string] *(a, ?1)\n" +
        "\n" +
        "foreach {a | T(a)}:\n" +
        "NPH_$a! -> hatdef_$a(N_$a) [0.5]\n" +
        "[sem] uniq_$a(intersect_1(?1, cs))\n" +
        "[string] *(the, ?1)";

    public static void main(String [] args) throws Exception
    {
        TemplateIrtgInputCodec input_codec = new TemplateIrtgInputCodec();
        TemplateInterpretedTreeAutomaton automaton = input_codec.read(tirtg);

        FirstOrderModel fomodel = FirstOrderModel.read(
                                  new StringReader(model.replaceAll("'", "\"")));
        InterpretedTreeAutomaton ita = automaton.instantiate(fomodel);
        
        ((SetAlgebra)ita.getInterpretation("sem").getAlgebra()).setModel(fomodel);
        
        HashMap<String, String> mymap = new HashMap<>();
        TreeAutomaton tree;
        // If I try to parse a string, I get at least one tree
        System.out.println("Parse 1");
        mymap.put("string", "a lamp");
        tree = ita.parse(mymap);
        for (Object t : tree.languageIterable())
        {
            System.out.println(t);
        }

        System.out.println("Parse 2");
        // However, if I try to parse a set, I get nothing
        mymap.clear();
        mymap.put("sem", "{lr11}");
        tree = ita.parse(mymap);
        for (Object t : tree.languageIterable())
        {
            System.out.println(t);
        }
    }
}
