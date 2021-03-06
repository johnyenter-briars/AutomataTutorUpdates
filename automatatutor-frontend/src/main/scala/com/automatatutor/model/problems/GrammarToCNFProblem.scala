package com.automatatutor.model.problems

import com.automatatutor.model._

import scala.xml.NodeSeq
import scala.xml.XML
import net.liftweb.mapper.By
import net.liftweb.mapper.IdPK
import net.liftweb.mapper.LongKeyedMapper
import net.liftweb.mapper.LongKeyedMetaMapper
import net.liftweb.mapper.MappedString
import net.liftweb.mapper.MappedText
import net.liftweb.mapper.MappedInt
import net.liftweb.mapper.MappedLongForeignKey
import bootstrap.liftweb.StartupHook
import net.liftweb.common.{Box, Full}

import scala.xml.Node

class GrammarToCNFProblem extends LongKeyedMapper[GrammarToCNFProblem] with IdPK with SpecificProblem[GrammarToCNFProblem] {
  def getSingleton = GrammarToCNFProblem

  object problemId extends MappedLongForeignKey(this, Problem)
  object grammar extends MappedText(this)

  def getGrammar = this.grammar.is
  def setGrammar(g: String) = this.grammar(g)

  override def copy(): GrammarToCNFProblem = {
    val retVal = new GrammarToCNFProblem
    retVal.problemId(this.problemId.get)
    retVal.grammar(this.grammar.get)
    return retVal
  }

  override def toXML(): Node = {
    return <GrammarToCNFProblem>
             <Grammar>{ this.getGrammar }</Grammar>
           </GrammarToCNFProblem>
  }

  override def setGeneralProblem(newProblem: Problem) = this.problemId(newProblem)

}

object GrammarToCNFProblem extends GrammarToCNFProblem with SpecificProblemSingleton with LongKeyedMetaMapper[GrammarToCNFProblem] {
  def findByGeneralProblem(generalProblem: Problem): GrammarToCNFProblem =
    find(By(GrammarToCNFProblem.problemId, generalProblem)) openOrThrowException ("Must only be called if we are sure that generalProblem is a GrammarToCNFProblem")

  def deleteByGeneralProblem(generalProblem: Problem): Boolean =
    this.bulkDelete_!!(By(GrammarToCNFProblem.problemId, generalProblem))

  override def fromXML(generalProblem: Problem, xml: Node): Box[SpecificProblem[_]] = {
    val retVal = new GrammarToCNFProblem
    retVal.problemId(generalProblem)
    retVal.grammar((xml \ "Grammar").text)
    retVal.save()
    return Full(retVal)
  }
}