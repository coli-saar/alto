// Start the IRTG tool as follows:
//
// scala -cp irtg-1.0.jar -Yrepl-sync -i init.scala 
//
// If you are using Scala 2.9 or lower, delete the indicated line
// below first.

import de.up.ling.irtg._
import de.up.ling.irtg.algebra._
import de.up.ling.irtg.automata._
import de.up.ling.irtg.hom._
import de.up.ling.irtg.signature._
import de.up.ling.irtg.corpus._
import de.up.ling.irtg.codec._
import de.up.ling.tree._
import de.saar.basic._
import de.up.ling.irtg.util.TestingTools._


import java.io._
import collection.JavaConverters._

// comment this for Scala 2.9 or lower
import scala.language.implicitConversions;

import ScalaShell._


implicit def intmap2integermap(map:scala.collection.immutable.Map[String,Int]) = map.asJava.asInstanceOf[java.util.Map[String,java.lang.Integer]]
implicit def stringmap2java(map:scala.collection.immutable.Map[String,String]) = map.asJava
implicit def string2tree(s:String) = pt(s)
