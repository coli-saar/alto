import de.saar.penguin.irtg._
import java.io._
import collection.JavaConversions._

val irtg = IrtgParser.parse(new FileReader("examples/cfg.irtg"))
val pco = ParsedCorpus.read(new FileInputStream("/tmp/pco"))

irtg.trainEM(pco)

val parsedInput = irtg.parseStrings(Map("i" -> "john watches the woman with the telescope"))
val chart = irtg.parse(parsedInput)

chart.viterbi()
chart.language()



val irtg = IrtgParser.parse(new FileReader("examples/scfg.irtg"))
val parsedInput = irtg.parseStrings(Map("german" -> "hans betrachtet die frau mit dem fernrohr"))
irtg.decode("english", parsedInput)

