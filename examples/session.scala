scala -J-server -cp ../scala-irtg-shell/target/scala-2.9.1.final/scala-irtg-shell_2.9.1-1.0.jar:target/irtg-1.0-SNAPSHOT-jar-with-dependencies.jar 

:load ../scala-irtg-shell/init.scala

val irtg = IrtgParser.parse(file("examples/scfg.irtg"))
val chart = irtg.parseFromReaders("german" -> "hans betrachtet die frau mit dem fernrohr")
chart.viterbi()
chart.language()

irtg.decodeFromReaders("english", "german" -> "hans betrachtet die frau mit dem fernrohr")

val irtg = IrtgParser.parse(file("examples/cfg.irtg"))
val pco = irtg.parseCorpus(file("examples/pcfg-training.txt"))

time(irtg.trainEM(pco))

pco.write("/tmp/foo")
val pco2 = ParsedCorpus.read(fistream("/tmp/foo"))
time(irtg.trainEM(pco2))


