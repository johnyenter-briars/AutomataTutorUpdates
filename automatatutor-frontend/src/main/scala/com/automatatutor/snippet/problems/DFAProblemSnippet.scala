package com.automatatutor.snippet.problems

import com.automatatutor.snippet._
import java.util.Calendar
import java.util.Date

import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.Text
import scala.xml.XML

import com.automatatutor.lib.GraderConnection
import com.automatatutor.model._
import com.automatatutor.model.problems._

import net.liftweb.common.Box
import net.liftweb.common.Full
import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.SHtml.ElemAttr.pairToBasic
import net.liftweb.http.Templates
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds
import net.liftweb.http.js.JsCmds.JsHideId
import net.liftweb.http.js.JsCmds.JsShowId
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.http.js.JsCmds.cmdToString
import net.liftweb.http.js.JsCmds.jsExpToJsCmd
import net.liftweb.util.AnyVar.whatVarIs
import net.liftweb.util.Helpers
import net.liftweb.util.Helpers.strToSuperArrowAssoc

object DFAConstructionSnippet extends SpecificProblemSnippet {

  def preprocessBlockAutomatonXml(input : String ) : String = {
    val withoutNewlines = input.filter(!List('\n', '\r').contains(_)).replace("\u0027", "\'")
    val asXml = XML.loadString(withoutNewlines)
    val newAutomaton = <automaton> { asXml.child } </automaton>
    return newAutomaton.toString.replace("\"","\'") //Very important line, otherwise app doesn't render
  }

  override def renderCreate(createUnspecificProb: (String, String) => Problem,
                             returnFunc:          (Problem) => Unit) : NodeSeq = {

    var name : String = ""
    var description : String = ""
    var automaton : String = ""

    def create() = {
      val unspecificProblem = createUnspecificProb(name, description)
      
      val specificProblem : DFAConstructionProblem = DFAConstructionProblem.create
      specificProblem.setGeneralProblem(unspecificProblem).setAutomaton(automaton)
      specificProblem.save
      
      returnFunc(unspecificProblem)
    }
    
    
    // Remember to remove all newlines from the generated XML by using filter
    val automatonField = SHtml.hidden(automatonXml => automaton = preprocessBlockAutomatonXml(automatonXml), "", "id" -> "automatonField")
    val namefield = SHtml.text(name, name = _)
    val descriptionfield = SHtml.textarea(description, description = _, "cols" -> "80", "rows" -> "5")
    //das automatonField ist nur dafür da, dass der Automat dort als XML eingefügt wird (--> deswegen auch hidden) (bei onClick des Submit-Button) und
    val submitButton = SHtml.submit("Create", create, "onClick" -> "document.getElementById('automatonField').value = Editor.canvas.exportAutomaton()")
    
    val template : NodeSeq = Templates(List("templates-hidden", "dfa-construction", "create")) openOr Text("Could not find template /templates-hidden/dfa-construction/create")
    Helpers.bind("createform", template,
        "automaton" -> automatonField,
        "namefield" -> namefield,
        "descriptionfield" -> descriptionfield,
        "submit" -> submitButton)
  }
  
  override def renderEdit: Box[(Problem, Problem => Unit) => NodeSeq] = Full(renderEditFunc)

  private def renderEditFunc(problem: Problem, returnFunc: (Problem => Unit)): NodeSeq = {
    val dfaConstructionProblem = DFAConstructionProblem.findByGeneralProblem(problem)

    var problemName: String = problem.getName
    var problemDescription: String = problem.getDescription
    var automaton : String = ""

    def create() = {
      problem.setName(problemName).setDescription(problemDescription).save()
      dfaConstructionProblem.setAutomaton(automaton).save()
      returnFunc(problem)
    }
    
    // Remember to remove all newlines from the generated XML by using filter
    val automatonField = SHtml.hidden(automatonXml => automaton = preprocessBlockAutomatonXml(automatonXml), "", "id" -> "automatonField")
    val nameField = SHtml.text(problemName, problemName = _)
    val descriptionField = SHtml.textarea(problemDescription, problemDescription = _, "cols" -> "80", "rows" -> "5")
    val submitButton = SHtml.submit("Save", create, "onClick" -> "document.getElementById('automatonField').value = Editor.canvas.exportAutomaton()")
    val setupScript = <script type="text/javascript"> initCanvas(); Editor.canvas.setAutomaton("{ dfaConstructionProblem.getAutomaton }") </script>
    
    val template : NodeSeq = Templates(List("templates-hidden", "dfa-construction", "edit")) openOr Text("Could not find template /templates-hidden/dfa-construction/edit")
    Helpers.bind("editform", template,
        "automaton" -> automatonField,
        "setupscript" -> setupScript,
        "namefield" -> nameField,
        "descriptionfield" -> descriptionField,
        "submit" -> submitButton)
  }
  
  override def renderSolve(generalProblem: Problem, maxGrade: Long, lastAttempt: Box[SolutionAttempt],
                           recordSolutionAttempt: (Int, Date) => SolutionAttempt, returnFunc: (Problem => Unit), 
						   remainingAttempts: () => Int, bestGrade: () => Int): NodeSeq = {
	  
    val dfaConstructionProblem = DFAConstructionProblem.findByGeneralProblem(generalProblem)
    
    def grade( attemptDfaDescription : String ) : JsCmd = {
      if(remainingAttempts() <= 0) {
        return JsShowId("feedbackdisplay") & 
		SetHtml("feedbackdisplay", 
		Text("You do not have any attempts left for this problem. Your final grade is " + 
				bestGrade().toString + "/" + 
				maxGrade.toString + "."))
      }

      val attemptDfaXml = XML.loadString(attemptDfaDescription)
      val correctDfaDescription = dfaConstructionProblem.getXmlDescription.toString
      val attemptTime = Calendar.getInstance.getTime()
      val graderResponse = GraderConnection.getDfaFeedback(correctDfaDescription, attemptDfaDescription, maxGrade.toInt)
      
      val numericalGrade = graderResponse._1
      val generalAttempt = recordSolutionAttempt(numericalGrade, attemptTime)
      
      // Only save the specific attempt if we saved the general attempt
      if(generalAttempt != null) {
    	  DFAConstructionSolutionAttempt.create.solutionAttemptId(generalAttempt).attemptAutomaton(preprocessBlockAutomatonXml(attemptDfaDescription)).save
      }
      
      val setNumericalGrade : JsCmd = SetHtml("grade", Text(graderResponse._1.toString + "/" + maxGrade.toString))
      val setFeedback : JsCmd = SetHtml("feedback", graderResponse._2)
      val showFeedback : JsCmd = JsShowId("feedbackdisplay")
      
      return setNumericalGrade & setFeedback & showFeedback & JsCmds.JsShowId("submitbutton")
    }
	
	//reconstruct last attempt
	val lastAttemptDFAScript = lastAttempt.map({ generalAttempt => 
		<script type="text/javascript">
      initCanvas();
			Editor.canvas.setAutomaton("{ 
				DFAConstructionSolutionAttempt.getByGeneralAttempt(generalAttempt).attemptAutomaton.is 
			}") 
		</script> 
	}) openOr <div></div>
    
	//build html
    val problemAlphabet = dfaConstructionProblem.getAlphabet
    
    val alphabetJavaScriptArray = "[\"" + problemAlphabet.mkString("\",\"") + "\"]"
    val alphabetScript : NodeSeq = <script type="text/javascript"> initCanvas(); Editor.canvas.setAlphabetArray( { alphabetJavaScriptArray } ) </script>
	
    val problemAlphabetNodeSeq = Text("{" + problemAlphabet.mkString(",") + "}")
    val problemDescriptionNodeSeq = Text(generalProblem.getDescription)
    
    val hideSubmitButton : JsCmd = JsHideId("submitbutton")
    val ajaxCall : JsCmd = SHtml.ajaxCall(JsRaw("Editor.canvas.exportAutomaton()"), grade(_))
    val submitButton : NodeSeq = <button type='button' id='submitbutton' onclick={hideSubmitButton & ajaxCall}>Submit</button>
    
    val template : NodeSeq = Templates(List("templates-hidden", "dfa-construction", "solve")) openOr Text("Template /templates-hidden/dfa-construction/solve not found")
    return SHtml.ajaxForm(Helpers.bind("dfaeditform", template,
        "alphabetscript" -> alphabetScript,
        "dfascript" -> lastAttemptDFAScript,
        "alphabettext" -> problemAlphabetNodeSeq,
        "problemdescription" -> problemDescriptionNodeSeq,
        "submitbutton" -> submitButton))
  }
  
  override def onDelete( generalProblem : Problem ) : Unit = {
    DFAConstructionProblem.deleteByGeneralProblem(generalProblem)
  }
}

// We have this as an extra class in order to get around Lift's problems with proper capitalization when looking for snippets
class Dfacreationsnippet {
  def preprocessAutomatonXml ( input : String ) : String = input.filter(!List('\n', '\r').contains(_)).replace("\u0027", "\'")
  
  /*def editform( xhtml : NodeSeq ) : NodeSeq = {
    val unspecificProblem : Problem = chosenProblem
    val dfaConstructionProblem : DFAConstructionProblem = DFAConstructionProblem.findByGeneralProblem(chosenProblem)

    var shortDescription : String = chosenProblem.getShortDescription
    var longDescription : String = chosenProblem.getLongDescription
    var automaton : String = "" // Will get replaced by an XML-description of the canvas anyways

    def edit() = {
      unspecificProblem.setShortDescription(shortDescription).setLongDescription(longDescription).save
      dfaConstructionProblem.setAutomaton(automaton).save
      
      S.redirectTo("/problems/index")
    }
    
    val automatonField = SHtml.hidden(automatonXml => automaton = preprocessAutomatonXml(automatonXml), "", "id" -> "automatonField")
    val shortDescriptionField = SHtml.text(shortDescription, shortDescription = _)
    val longDescriptionField = SHtml.textarea(longDescription, longDescription = _, "cols" -> "80", "rows" -> "5")
    val submitButton = SHtml.submit("Submit", edit, "onClick" -> "document.getElementById('automatonField').value = Editor.canvas.exportAutomaton()")
    
    Helpers.bind("createform", xhtml,
        "automaton" -> automatonField,
        "shortdescription" -> shortDescriptionField,
        "longdescription" -> longDescriptionField,
        "submit" -> submitButton)
  }*/
}
