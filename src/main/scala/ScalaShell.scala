import java.io._
import de.up.ling.irtg.automata._
import de.up.ling.irtg._
import de.up.ling.irtg.codec._
import de.up.ling.irtg.signature._
import collection.JavaConverters._

object ScalaShell {
  def file(filename: String) = new FileReader(new File(filename))
  def fistream(filename: String) = new FileInputStream(new File(filename))
  def fostream(filename: String) = new FileOutputStream(new File(filename))

  def loadAutomaton(filename: String) = (new TreeAutomatonInputCodec()).read(new FileInputStream(filename))

  def loadIrtg(filename: String) = InterpretedTreeAutomaton.read(fistream(filename))
  def saveIrtg(irtg: InterpretedTreeAutomaton, filename:String) = {
    val w = new PrintWriter(new FileWriter(new File(filename)))
    w.println(irtg.toString)
    w.close
  }

 def time[T](code: => T) = {
    val t0 = System.nanoTime: Double
    val res = code
    val t1 = System.nanoTime: Double
    println("[execution time: %.1f ms]".format((t1 - t0) / 1000000.0))
    res
  }

  def atime(code: => Any) = {
    val t0 = System.nanoTime: Double
    val res = code
    val t1 = System.nanoTime: Double
    println("[execution time: %.1f ms]".format((t1 - t0) / 1000000.0))
    res
  }

  case class PairBuilder(left: String) {
    def >>(right: String) = (left, right)
  }

  implicit def string2PairBuilder(left: String) = PairBuilder(left)

  implicit def pairs2Map(entries: Product) = {
    val ret: java.util.Map[String, String] = new java.util.HashMap[String, String]()
    entries.productIterator.foreach {
      case (key: String, value: String) => ret.put(key, value)
    }
    ret
  }

  implicit def pair2Map(entry: (String, String)) = {
    val ret: java.util.Map[String, String] = new java.util.HashMap[String, String]()
    ret.put(entry._1, entry._2)
    ret
  }

//  implicit def string2Reader(x: String) = new StringReader(x)

  // convert Scala function to Google Collections function
  def gfn[T,U](code: T => U) = new com.google.common.base.Function[T,U] { def apply(x:T) = code(x) }

  // convert Scala Map[String,Int] to Java Map[String,Integer]
  // apparently this no longer works in Scala 2.10; see  scala.language.implicitConversions
//  implicit def intmap2integermap(map:scala.collection.immutable.Map[String,Int]) = map.asJava.asInstanceOf[java.util.Map[String,java.lang.Integer]]

}

