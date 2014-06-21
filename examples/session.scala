scala -J-server -cp target/irtg-1.1-SNAPSHOT-jar-with-dependencies.jar  -Yrepl-sync -i init.scala 



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

chart.viterbi
chart.language

irtg.decode("english", "german" >> "hans betrachtet die frau mit dem fernrohr")



// algebras

val alg = new StringAlgebra()
alg.decompose(alg.parseString("a b c"))



// ML training 

val irtg = loadIrtg("examples/cfg.irtg")
val corpus = irtg.readCorpus(file("examples/pcfg-annotated-training.txt"))
irtg.trainML(corpus)


// EM training

val irtg = loadIrtg("examples/cfg.irtg")
val corpus = irtg.readCorpus(file("examples/pcfg-training.txt"))

Charts.computeCharts(corpus, irtg, fostream("charts.zip"))
corpus.attachCharts(new Charts(new FileInputStreamSupplier(new File("charts.zip"))))

irtg.trainEM(corpus)

