package com.automatatutor.snippet.problems

import com.automatatutor.snippet._
import java.util.Calendar
import java.util.Date

import scala.xml.NodeSeq
import scala.xml.Text
import scala.xml.XML

import com.automatatutor.lib.GraderConnection
import com.automatatutor.model._
import com.automatatutor.model.problems._

import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds
import net.liftweb.http.js.JsCmds.JsHideId
import net.liftweb.http.js.JsCmds.JsShowId
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.http.js.JsCmds.cmdToString
import net.liftweb.http.js.JsCmds.jsExpToJsCmd

import net.liftweb.common.Box
import net.liftweb.common.Full
import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.mapper.By
import net.liftweb.util.Helpers
import net.liftweb.util.Helpers._

object NFAToDFAProblemSnippet extends SpecificProblemSnippet {
  def preprocessAutomatonXml ( input : String ) : String = {
    val withoutNewlines = input.filter(!List('\n', '\r').contains(_)).replace("\u0027", "\'")
    val asXml = XML.loadString(withoutNewlines)
    val symbolsWithoutEpsilon = (asXml \ "alphabet" \ "symbol").filter(node => node.text != "ε")
    val alphabetWithoutEpsilon = <alphabet> { symbolsWithoutEpsilon } </alphabet>
    val automatonWithoutAlphabet = asXml.child.filter(_.label != "alphabet")
    val newAutomaton = <automaton> { alphabetWithoutEpsilon } { automatonWithoutAlphabet } </automaton>
    return newAutomaton.toString.replace("\"","\'")
  }

  def preprocessBlockAutomatonXml(input : String ) : String = {
    val withoutNewlines = input.filter(!List('\n', '\r').contains(_)).replace("\u0027", "\'")
    val asXml = XML.loadString(withoutNewlines)
    val newAutomaton = <automaton> { asXml.child } </automaton>
    return newAutomaton.toString.replace("\"","\'") //Very important line, otherwise app doesn't render
  }

  override def renderCreate(createUnspecificProb: (String, String) => Problem,
                             returnFunc:          (Problem) => Unit) : NodeSeq = {

    var automaton : String = ""
	  var name : String = ""
    var description = "Construct a DFA that recognizes the same language as the given NFA"
    def create() = {
      val unspecificProblem = createUnspecificProb(name, description)

      val specificProblem : NFAToDFAProblem = NFAToDFAProblem.create
      specificProblem.setGeneralProblem(unspecificProblem).setAutomaton(automaton)
      specificProblem.save

      returnFunc(unspecificProblem)
    }

    // Remember to remove all newlines from the generated XML by using filter. Also remove 'ε' from the alphabet, as its implied
    val automatonField = SHtml.hidden(automatonXml => automaton = preprocessBlockAutomatonXml(automatonXml), "", "id" -> "automatonField")
	  val nameField = SHtml.text("", name = _)
    val descriptionField = SHtml.text("", description = _)
    val submitButton = SHtml.submit("Create", create, "onClick" -> "document.getElementById('automatonField').value = Editor.canvas.exportAutomaton()")


    val template : NodeSeq = Templates(List("templates-hidden", "nfa-to-dfa-problem", "create")) openOr Text("Could not find template /templates-hidden/nfa-to-dfa-problem/create")
    Helpers.bind("createform", template,
      "automaton" -> automatonField,
		  "namefield" -> nameField,
      "descriptionfield" -> descriptionField,
      "submit" -> submitButton
      )
  }

  override def renderEdit: Box[(Problem, Problem => Unit) => NodeSeq] = Full(renderEditFunc)

  private def renderEditFunc(problem: Problem, returnFunc: (Problem => Unit)): NodeSeq = {
    val nfaToDfaProblem = NFAToDFAProblem.findByGeneralProblem(problem)

    var problemName: String = problem.getName
    var problemDescription: String = problem.getDescription
    var automaton : String = ""

    def create() = {
      problem.setName(problemName).setDescription(problemDescription).save()
      nfaToDfaProblem.setAutomaton(automaton).save()
      returnFunc(problem)
    }

    // Remember to remove all newlines from the generated XML by using filter
    val automatonField = SHtml.hidden(automatonXml => automaton = preprocessBlockAutomatonXml(automatonXml), "", "id" -> "automatonField")
    val nameField = SHtml.text(problemName, problemName = _)
    val descriptionField = SHtml.text(problemDescription, problemDescription = _)
    val submitButton = SHtml.submit("Save", create, "onClick" -> "document.getElementById('automatonField').value = Editor.canvas.exportAutomaton()")
    val setupScript =
      <script type="text/javascript">
        initCanvas();
		Editor.canvas.setAutomaton( "{ preprocessBlockAutomatonXml(nfaToDfaProblem.getAutomaton) }" );
      </script>


    val template : NodeSeq = Templates(List("templates-hidden", "nfa-to-dfa-problem", "edit")) openOr Text("Could not find template /templates-hidden/nfa-to-dfa-problem/edit")
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

    val nfaToDfaProblem = NFAToDFAProblem.findByGeneralProblem(generalProblem)

	def grade( attemptDfaDescription : String ) : JsCmd = {
      if(remainingAttempts() <= 0) {
        return JsShowId("feedbackdisplay") & SetHtml("feedbackdisplay",
		Text("You do not have any attempts left for this problem. Your final grade is " +
		bestGrade().toString + "/" + maxGrade.toString + "."))
      }

      val attemptDfaXml = XML.loadString(attemptDfaDescription)
      val correctNfaDescription = nfaToDfaProblem.getXmlDescription.toString
      val attemptTime = Calendar.getInstance.getTime()
      val graderResponse = GraderConnection.getNfaToDfaFeedback(correctNfaDescription, attemptDfaDescription, maxGrade.toInt)

      val numericalGrade = graderResponse._1
      val generalAttempt = recordSolutionAttempt(numericalGrade, attemptTime)

      // Only save the specific attempt if we saved the general attempt
      if (generalAttempt != null) {
    	  NFAToDFASolutionAttempt.create.solutionAttemptId(generalAttempt).attemptAutomaton( preprocessAutomatonXml(attemptDfaDescription) ).save
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
			Editor.canvasDfa.setAutomaton("{
				preprocessAutomatonXml(NFAToDFASolutionAttempt.getByGeneralAttempt(generalAttempt).attemptAutomaton.is)
			}")
		</script>
	}) openOr <div></div>

	//build html
	val problemAlphabet = nfaToDfaProblem.getAlphabet
    val problemAlphabetNodeSeq = Text("{" + problemAlphabet.mkString(",") + "}")
    val problemDescriptionNodeSeq = Text(generalProblem.getDescription)

    val hideSubmitButton : JsCmd = JsHideId("submitbutton")
    val ajaxCall : JsCmd = SHtml.ajaxCall(JsRaw("Editor.canvasDfa.exportAutomaton()"), grade(_))
    val submitButton : NodeSeq = <button type='button' id='submitbutton' onclick={hideSubmitButton & ajaxCall}>Submit</button>

	val alphabetJavaScriptArray = "[\"" + problemAlphabet.mkString("\",\"") + "\"]"

    val setupScript : NodeSeq =
      <script type="text/javascript">
        initCanvas();
        Editor.canvasNfa.setAutomaton( "{ preprocessBlockAutomatonXml(nfaToDfaProblem.getXmlDescription.toString) }" );
        Editor.canvasNfa.lockCanvas();
        Editor.canvasDfa.setAlphabet( { alphabetJavaScriptArray } );
        Editor.canvasDfa.setNumberOfStates( "{ preprocessBlockAutomatonXml(nfaToDfaProblem.getXmlDescription.toString) }", "{ preprocessBlockAutomatonXml(nfaToDfaProblem.getXmlDescription.toString) }" );
      </script>
	
    	
    	
    val template : NodeSeq = Templates(List("templates-hidden", "nfa-to-dfa-problem", "solve")) openOr Text("Template /templates-hidden/nfa-to-dfa-problem/solve not found")
    return SHtml.ajaxForm(Helpers.bind("nfatodfaform", template,
	    "setupscript" -> setupScript,
		//"lastattemptdfascript" -> lastAttemptDFAScript,     TODO: javascript not working
        "submitbutton" -> submitButton))
	
  }
    
  override def onDelete( problem : Problem ) : Unit = {
    NFAToDFAProblem.deleteByGeneralProblem(problem)
  }
}