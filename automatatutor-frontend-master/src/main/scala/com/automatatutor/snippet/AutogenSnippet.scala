package com.automatatutor.snippet

import scala.xml.NodeSeq
import scala.xml.Text
import net.liftweb.http.SHtml
import net.liftweb.http.S
import com.automatatutor.model._
import com.automatatutor.lib.GraderConnection
import net.liftweb.common.Box
import net.liftweb.common.Empty
import net.liftweb.common.Full
import net.liftweb.http._
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds
import net.liftweb.http.js.JsCmds._
import com.automatatutor.lib._
import net.liftweb.util.Helpers
import net.liftweb.util.Helpers._
import net.liftweb.util.Helpers.bind
import net.liftweb.util.Helpers.strToSuperArrowAssoc
import net.liftweb.http.Templates
import com.automatatutor.renderer.ProblemRenderer

object CurrentPracticeProblem extends SessionVar[Problem](null)

class Autogensnippet {
  def autogenerationform(xhtml: NodeSeq): NodeSeq = {
    val user: User = User.currentUser openOrThrowException "Lift only allows logged-in-users here";
    val usersProblems = Problem.findAllByCreator(user).filter(_.getCourse == Empty)

    val maxAutoGenProblems = 5
    val onclick: JsCmd = if (usersProblems.length >= maxAutoGenProblems) {
      JsCmds.Alert("You may only have " + maxAutoGenProblems + " autogenerated problems. \n Please delte some of your older problems first!") & JsRaw("return false")
    } else {
      JsRaw("return true")
    }

    val typeOptions = Array(
      (ProblemType.WordsInGrammarTypeName, ProblemType.WordsInGrammarTypeName),
      (ProblemType.GrammarToCNFTypeName, ProblemType.GrammarToCNFTypeName),
      (ProblemType.CYKTypeName, ProblemType.CYKTypeName),
      (ProblemType.FindDerivationTypeName, ProblemType.FindDerivationTypeName),
      (ProblemType.WhileToTMTypeName, ProblemType.WhileToTMTypeName))

    var difficulty: Int = 50
    var typeString: String = ""

    def generate(): Unit = {
      //only limited nr of generated problem per student user
      if (usersProblems.length >= maxAutoGenProblems) {
        S.warning("You may only have " + maxAutoGenProblems.toString + " problems.")
        return
      }

      val difficultyMin = difficulty - 10
      val difficultyMax = difficulty + 10

      var response: NodeSeq = GraderConnection.generateProblemBestIn(typeString, difficultyMin, difficultyMax)

      //import
      val createdProblem = Problem.fromXML(response.head)
      if (createdProblem) {
        S.notice("created new problem with quality=" + (response \\ "@quality").text + " difficulty=" + (response \\ "@difficulty"))
      }
      else {
        S.notice("ERROR: " + response.text)
      }
    }

    val template: NodeSeq = Templates(List("autogen", "autogenFormTemplate")) openOr Text("Could not find template /autogen/autogenFormTemplate")
    Helpers.bind("autogenform", template,
      "typeoptions" -> SHtml.select(typeOptions, Empty, typeString = _, "id" -> "typeSelect"),
      "generatebutton" -> SHtml.submit("Generate", generate, "onclick" -> onclick.toJsCmd),
      "difficultyslider" -> SHtml.range(difficulty, difficulty = _, 10, 90))
  }


  def autogenproblemlist(xhtml: NodeSeq): NodeSeq = {
    val user: User = User.currentUser openOrThrowException "Lift only allows logged-in-users here";
    val usersProblems = Problem.findAllByCreator(user).filter(_.getCourse == Empty)

    if (usersProblems.isEmpty) return Text("You currently have no autogenerated problems")

    def moveButton(problem: Problem): NodeSeq = {
      if (user.hasSupervisedCourses) {
        return SHtml.link(
          "/autogen/move",
          () => {
            CurrentPracticeProblem(problem)
          },
          <button type='button'>Move To Course</button>)
      } else {
        return NodeSeq.Empty
      }
    }

    val deleteAllLink = SHtml.link(
      "/autogen/index",
      () => {
        usersProblems.map(_.delete_!)
      },
      <button type='button'>Delete All</button>,
      "onclick" -> JsRaw("return confirm('Are you sure you want to delete all your autogenerated problems?')").toJsCmd)

    val returnToHomeLink = SHtml.link(
      "/main/index",
      () => {
      },
      <button type='button'>Return Home</button>)

    return TableHelper.renderTableWithHeader(
      usersProblems,
      ("Description", (problem: Problem) => Text(problem.getShortDescription)),
      ("Problem Type", (problem: Problem) => Text(problem.getTypeName)),
      ("", (problem: Problem) => SHtml.link(
        "/autogen/practice",
        () => {
          CurrentPracticeProblem(problem)
        },
        <button type='button'>Solve</button>) ++ moveButton(problem)),
      ("", (problem: Problem) => {
        (new ProblemRenderer(problem)).renderDeleteLink("/autogen/index")
      })) ++ deleteAllLink ++ returnToHomeLink
  }


  def renderpractice(ignored: NodeSeq): NodeSeq = {
    if (CurrentPracticeProblem == null) {
      S.warning("Please first choose a problem")
      return S.redirectTo("/autogen/index")
    }

    val problem: Problem = CurrentPracticeProblem.is
    val problemSnippet: SpecificProblemSnippet = problem.getProblemType.getProblemSnippet

    def returnFunc(problem: Problem) = {
      S.redirectTo("/autogen/index")
    }

    val returnLink = SHtml.link("/autogen/index", () => {}, Text("Return"))

    return problemSnippet.renderSolve(problem, 10, Empty,
      (grade, date) => SolutionAttempt, returnFunc, () => 1, () => 0) ++ returnLink
  }

  def movetocourseform(xhtml: NodeSeq): NodeSeq = {

    def selectionCallback(folderID: String): Unit = {
      val user: User = User.currentUser openOrThrowException "Lift only allows logged-in-users here";
      val courses = user.getSupervisedCourses
      //The select box remained at "----" meaning "no folder"
      if (folderID.equals("0")) {
        return;
      }

      val folder = Folder.findByID(folderID)
      val problem = CurrentPracticeProblem.is
      val probToFol = new ProblemToFolder
      probToFol.setProblem(problem)
      probToFol.setFolder(folder)
      probToFol.save

      problem.setCourse(folder.getCourse)
      problem.save

    }

    val user: User = User.currentUser openOrThrowException "Lift only allows logged-in-users here";
    val supervisedCourses = user.getSupervisedCourses

    val cancelButton = SHtml.link("/autogen/index", () => {}, <button type='button'>Cancel</button>)

    return <form action="/autogen/index">
      <p>Please select a folder within
        <strong>ONE</strong>
        course, to transfer the problem to</p>
      <p>Leave the rest as "----"</p>
      {(TableHelper.renderTableWithHeader(
        supervisedCourses,
        ("Name", (course: Course) => Text(course.getName)),
        ("", (course: Course) => {

          val folderOptions = ("0" -> "----") :: Folder.findAllByCourse(course)
            .map(f => (f.getFolderID.toString -> f.getLongDescription.toString))

          val default = folderOptions.head
          SHtml.select(folderOptions, Box(default._1), selectionCallback)
        })
      )
    ++ SHtml.button("Send", () => {}, "type" -> "submit")
    ++ cancelButton
    )}
    </form>
  }
}