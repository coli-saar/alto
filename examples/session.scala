scala -J-server -cp ../scala-irtg-shell/target/scala-2.9.1/scala-irtg-shell_2.9.1-1.0.jar:target/irtg-1.0-SNAPSHOT.jar  -Yrepl-sync -i ../scala-irtg-shell/init.scala 



// parsing and drawing trees

val t = pt("f(a,g(c))")
t.draw


// loading and intersecting automata; language of automata

val auto = loadAutomaton("examples/test.auto")
val auto2 = loadAutomaton("examples/test2.auto")

val intersection = auto.intersect(auto2)

intersection.language

intersection.language.iterator.next.draw


// homomorphisms

val s = sig(Map("F" -> 2, "A" -> 0))
val h = hom(Map("A" -> "h(a)", "F" -> "f(f(?1,b),?2)"), s)
h.apply(pt("F(A,A)"))


// inverse homomorphism of automata

val original = loadAutomaton("examples/test3.auto")
original.language

val preimage = original.inverseHomomorphism(h)
preimage.language


// IRTGs

val irtg = loadIrtg("examples/scfg.irtg")
val chart = irtg.parse("german" >> "hans betrachtet die frau mit dem fernrohr")

chart.language


irtg.decode("english", "german" >> "hans betrachtet die frau mit dem fernrohr")



// algebras

val alg = new StringAlgebra()
alg.decompose(alg.parseString("a b c"))





val irtg = IrtgParser.parse(file("examples/scfg.irtg"))
val chart = irtg.parse("german" -> "hans betrachtet die frau mit dem fernrohr")
chart.viterbi()
chart.language()

irtg.decode("english", "german" -> "hans betrachtet die frau mit dem fernrohr")

val irtg = IrtgParser.parse(file("examples/cfg.irtg"))
val pco = irtg.parseCorpus(file("examples/pcfg-training.txt"))

time(irtg.trainEM(pco))

pco.write("/tmp/foo")
val pco2 = ParsedCorpus.read(fistream("/tmp/foo"))
time(irtg.trainEM(pco2))


val ger = irtg.getInterpretation("german")
val alg = ger.getAlgebra().asInstanceOf[Algebra[List[String]]]
irtg.getAutomaton().intersect(alg.decompose(alg.parseString("hans betrachtet die frau mit dem fernrohr")).inverseHomomorphism(ger.getHomomorphism()))


irtg.parse("german" -> "hans betrachtet die frau mit dem fernrohr", "english" -> "john watches the woman with the telescope")

