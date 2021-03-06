package com.automatatutor.snippet

import scala.Array.canBuildFrom
import scala.xml._
import com.automatatutor.lib._
import com.automatatutor.model._
import com.automatatutor.renderer.{CourseRenderer, ProblemRenderer}
import com.sun.tracing.Probe
import net.liftweb.common.{Box, Empty, EmptyBox, Full}
import net.liftweb.http
import net.liftweb.http.{S, SHtml, _}
import net.liftweb.http.SHtml.ElemAttr.pairToBasic
import net.liftweb.http.SHtml.ElemAttr
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.{JsCmd, JsCmds}
import net.liftweb.mapper.{Ascending, BaseOwnedMappedField, By, Cmp, Descending, MaxRows, OprEnum, OrderBy, QueryParam, StartAt}
import net.liftweb.util.AnyVar.whatVarIs
import net.liftweb.util.Helpers
import net.liftweb.util.Helpers._
import net.liftweb.util.Helpers.strToSuperArrowAssoc
import net.liftweb.util.SecurityHelpers
import net.liftweb.util.Mailer
import net.liftweb.util.Mailer._

import scala.collection.mutable.ListBuffer

//TODO 7/17/2020 don't need to use this sessionvar, there's another one that lives in AutogenSnippet that does something similar
object CurrentEditableProblem extends SessionVar[Problem](null)
object BatchProblems extends SessionVar[ListBuffer[Problem]](null)
object PreviousPage extends SessionVar[String](null)
object SelectedProblemType extends SessionVar[ProblemType](null)

class Problempoolsnippet extends{

  if(BatchProblems.is == null) BatchProblems(new ListBuffer[Problem])

  def renderlocationtree(xhtml: NodeSeq): NodeSeq = {
    val user: User = User.currentUser openOrThrowException "Lift only allows logged-in-users here"
    val supervisedCourses = user.getSupervisedCourses

    val selectedFolders = new ListBuffer[Folder]
    def checkBoxForFolder(folder: Folder): NodeSeq = {
      SHtml.checkbox(false, (chosen: Boolean) => {
        if(chosen) selectedFolders += folder
      })
    }

    <ul id="myUL">
      {
        supervisedCourses.map(course => {
          <li><span class="caret">{course.getName}</span>
            <ul class="nested">
              {
                course.getFolders.map(f => {

                  <li>{f.getLongDescription}{checkBoxForFolder(f)}</li>
                })
              }
            </ul>
          </li>
        })
      }
    </ul> ++
    <div>
    {
      SHtml.button("Send Problems", () => {
        selectedFolders.foreach(folder => {
          BatchProblems.is.foreach(problem => {
            val exercise = new Exercise
            exercise.setCourse(folder.getCourse.get)
              .setProblem(problem)
              .setFolder(folder)
              //TODO 8/10/2020 add a method by which the user can set these settings on first transfer
              .setMaxGrade(10)
              .setAllowedAttempts(10)
              .save
          })
        })
        S.redirectTo("/main/problempool/index")
      })
    }
    </div>
  }

  def renderlocationtreeforsingleproblem(xhtml: NodeSeq, problem: Problem): NodeSeq = {
    val user: User = User.currentUser openOrThrowException "Lift only allows logged-in-users here"
    val supervisedCourses = user.getSupervisedCourses

    val selectedFolders = new ListBuffer[Folder]
    def checkBoxForFolder(folder: Folder): NodeSeq = {
      SHtml.checkbox(false, (chosen: Boolean) => {
        if(chosen) selectedFolders += folder
      })
    }

    <ul id="myUL">
      {
      supervisedCourses.map(course => {
        <li><span class="caret">{course.getName}</span>
          <ul class="nested">
            {
            course.getFolders.map(f => {

              <li>{f.getLongDescription}{checkBoxForFolder(f)}</li>
            })
            }
          </ul>
        </li>
      })
      }
    </ul> ++
      <div>
        {
        SHtml.button("Send Problems", () => {
          selectedFolders.foreach(folder => {
            val exercise = new Exercise
            exercise.setCourse(folder.getCourse.get)
              .setProblem(problem)
              .setFolder(folder)
              //TODO 8/10/2020 add a method by which the user can set these settings on first transfer
              .setMaxGrade(10)
              .setAllowedAttempts(10)
              .save
          })
          S.redirectTo("/main/problempool/index")
        })
        }
      </div>
  }

  def renderproblempool(xhtml: NodeSeq): NodeSeq ={
    val user: User = User.currentUser openOrThrowException "Lift only allows logged-in-users here"
    val usersProblems = Problem.findAllByCreator(user)

    if (usersProblems.isEmpty) return Text("You currently have no problems in the problem pool")

    def editProblemButton(problem: Problem): NodeSeq = {
      if (user.hasSupervisedCourses) {
        SHtml.link(
          "/main/problempool/edit",
          () => {
            CurrentEditableProblem(problem)
          },
          <button type='button'>Edit</button>)
      } else {
        NodeSeq.Empty
      }
    }

    def checkBoxForProblem(potentialProblem: Problem): NodeSeq = {
      SHtml.checkbox(false, (chosen: Boolean) => {
        if(chosen) BatchProblems.is += potentialProblem
      }, "class" -> "checkbox")
    }

    //Clear out BatchProblems to avoid over collection of problems if the user goes back and forth between pages
    BatchProblems.is.clear()

    val tableID = "problemPoolTable"

    def renderSendModal(problem: Problem): NodeSeq = {

      <div>
        <button type="button" id={"edit_access-modal-button_" + problem.getProblemID} class="modal-button">Send to a folder</button>

          <div id={"edit_access-modal_" + problem.getProblemID} class="modal">

            <div class="modal-content">
              <div class="modal-header">
                <span class="close" id={"edit_access-span_" + problem.getProblemID}>&times;</span>
                <h3>Edit an Exercise</h3>
              </div>
              <div class="modal-body">
                {
                  this.renderlocationtreeforsingleproblem(xhtml, problem)
                }
              </div>
            </div>
          </div>

      </div>
    }
    
    val table = <form method="POST">
      {
        (TableHelper.renderTableWithHeader(
          tableID,
          usersProblems,
          List(3, 10.5, 21, 5, 13, 13.5, 5, 5.3, 10, 4),
          Map("Problem Stats" -> "Average grade a student gets on this problem/Average attempts per student"),
          ("", (problem: Problem) => checkBoxForProblem(problem)),
          ("Name", (problem: Problem) => Text(problem.getName)),
          ("Description", (problem: Problem) => Text(problem.getDescription)),
          ("Problem Stats", (problem: Problem) => new ProblemRenderer(problem).renderProblemStats),
          ("Problem Type", (problem: Problem) => Text(problem.getTypeName)),
          ("Instances", (problem: Problem) => new ProblemRenderer(problem).renderProblemInstances),
          ("", (problem: Problem) => editProblemButton(problem)),
          ("", (problem: Problem) => SHtml.link(
            "/main/problempool/practice",
            () => {
              CurrentEditableProblem(problem)
            },
            <button type='button'>Solve</button>)),
          ("Send to Folder", (problem: Problem) => renderSendModal(problem)),
          ("", (problem: Problem) => {
            new ProblemRenderer(problem).renderDeleteLink("/main/problempool/index")
          })
        )
        ++ {
          <div>
            <button type="button" id="batch_send-modal-button" class="modal-button">Batch Send</button>
            <br></br>
            <br></br>
            {
              SHtml.button("Delete Selected", ()=>{
                BatchProblems.is.foreach(_.delete_!)
              }, "onclick" -> JsRaw("return confirm('Are you sure you want to delete the selected problems?')").toJsCmd)
            }
            {
              SHtml.button("Export Problems", () => {
                //If batch problem is empty, export all problems
                if(BatchProblems.is.toList.isEmpty){
                  DownloadHelper.offerZipDownloadToUser("AT_DB_Selected_Problems", Problem.findAll().map((problem: Problem) => {
                    (problem.getName, <exported> {problem.toXML}</exported>.toString())
                  }), DownloadHelper.XmlFile, DownloadHelper.ZipFile)
                }
                else{
                  DownloadHelper.offerZipDownloadToUser("AT_DB_Selected_Problems", BatchProblems.is.toList.map((problem: Problem) => {
                    (problem.getName, <exported> {problem.toXML}</exported>.toString())
                  }), DownloadHelper.XmlFile, DownloadHelper.ZipFile)
                }
              })
            }
            <div id="batch_send-modal" class="modal">

              <div class="modal-content">
                <div class="modal-header">
                  <span class="close">&times;</span>
                  <h3>Send Problems to Folders</h3>
                </div>
                <div class="modal-body">
                  {
                    this.renderlocationtree(xhtml)
                  }
                </div>
              </div>
            </div>
          </div>
        })
      }
    </form>

    val nameFilterID = "input_name"
    val nameFilter = SHtml.text("", (x: String)=>{},
      "onkeyup"->s"filterTableRows('$tableID', 'Name', '$nameFilterID');", "id"->nameFilterID)

    val problemTypeFilterID = "input_pt"
    val problemTypeFilter = SHtml.text("", (x: String)=>{},
      "onkeyup"->s"filterTableRows('$tableID', 'Problem Type', '$problemTypeFilterID');", "id"->problemTypeFilterID)

    val descriptionFilterID = "input_desc"
    val descriptionFilter = SHtml.text("", (x: String)=>{},
      "onkeyup"->s"filterTableRows('$tableID', 'Description', '$descriptionFilterID');", "id"->descriptionFilterID)

    Helpers.bind("problempool", xhtml,
      "problemlist" -> table,
      "namefilter" -> nameFilter,
      "problemtypefilter" -> problemTypeFilter,
      "descriptionfilter" -> descriptionFilter
    )
  }

  def renderbackbutton(xhtml: NodeSeq): NodeSeq = {
    val redirectTarget = if(PreviousPage.is == null) "/main/problempool/index" else PreviousPage.is
    SHtml.link(redirectTarget, ()=>{PreviousPage(null)}, <button>Cancel</button>)
  }

  def renderproblemedit(ignored: NodeSeq): NodeSeq ={
    if (CurrentEditableProblem.is == null) {
      S.warning("Please first choose a problem to edit")
      return S.redirectTo("/main/problempool/index")
    }

    val problem : Problem = CurrentEditableProblem.is
    val problemSnippet: SpecificProblemSnippet = problem.getProblemType.getProblemSnippet

    def returnFunc(unspecificProblem : Problem) = {
      if(TargetFolder.is != null){
        val exercise = new Exercise
        exercise.setFolder(TargetFolder.is)
          .setProblem(unspecificProblem)
          .setCourse(TargetFolder.is.getCourse.get)
          .save()

        TargetFolder(null)
        S.redirectTo("/main/course/folders/index")
      }

      //TODO 7/27/2020 need a better way of conditional setting the redirect page based on previous page
      val redirectTarget = if(PreviousPage.is == null) "/main/problempool/index" else PreviousPage.is
      PreviousPage(null)
      S.redirectTo(redirectTarget)
    }

    problemSnippet.renderEdit match {
      case Full(renderFunc) => renderFunc(problem, returnFunc)
      case Empty            =>
        S.error("Editing not implemented for this problem type"); S.redirectTo("/main/course/index")
      case _                => S.error("Error when retrieving editing function"); S.redirectTo("/main/course/index")
    }

  }

  def renderproblemstatistics(ignored: NodeSeq): NodeSeq = {
    if (CurrentEditableProblem.is == null) {
      S.warning("Please first choose a problem to edit")
      return S.redirectTo("/main/problempool/index")
    }
    val problem : Problem = CurrentEditableProblem.is

    new ProblemRenderer(problem).renderProblemInstancesTable
  }

  def renderdeletegrades(ignored: NodeSeq): NodeSeq = {
    if (CurrentEditableProblem.is == null) {
      S.warning("Please first choose a problem to edit")
      return S.redirectTo("/main/problempool/index")
    }

    <form>
      {
        val onClick = JsRaw("return confirm('Are you sure you want to delete all the user grades for this problem?')")

        SHtml.link(
          "/main/problempool/index",
          () => {
            SolutionAttempt.deleteAllByProblem(CurrentEditableProblem.is)
          },
          Text("Delete ALL User Grades"),
          "onclick" -> onClick.toJsCmd,
          "style" -> "color: red")
      }
    </form>
  }

  def renderpractice(ignored: NodeSeq): NodeSeq = {
    if (CurrentEditableProblem == null) {
      S.warning("Please first choose a problem")
      return S.redirectTo("/autogen/index")
    }

    val problem: Problem = CurrentEditableProblem.is
    val problemSnippet: SpecificProblemSnippet = problem.getProblemType.getProblemSnippet

    def returnFunc(problem: Problem) = {
      S.redirectTo("/main/problempool/index")
    }

    val returnLink = SHtml.link("/main/problempool/index", () => {}, Text("Return"))

    problemSnippet.renderSolve(problem, 10, Empty,
      (grade, date) => SolutionAttempt, returnFunc, () => 1, () => 0) ++ returnLink
  }

  def renderproblemoptions(xhtml: NodeSeq): NodeSeq = {
    if (CurrentFolderInCourse.is == null) {
      S.warning("Please first choose a folder to send problems to")
      return S.redirectTo("/main/course/folders/index")
    }

    def problemsAreIdentical(problem1: Problem, problem2: Problem): Boolean = {
      problem1.getProblemID == problem2.getProblemID
    }

    val user: User = User.currentUser openOrThrowException "Lift only allows logged-in-users here";
    val folder = CurrentFolderInCourse.is
    //Only show the problems which are not in the current folder
    val problems = Problem.findAllByCreator(user).filterNot(folder.getProblemsUnderFolder.contains(_))

    def checkBoxForProblem(potentialProblem: Problem): NodeSeq = {

      SHtml.checkbox(false, (chosen: Boolean) => {
        if(chosen){
          //TODO 7/21/2020 refactor this to make less ugly and more responsive. Something like returning a JSCmd to alert the user
          //If an exercise with the same problem already exists with the folder don't add it
          var isDuplicate: Boolean = false
          Exercise.findAllByFolder(folder).map(_.getProblem).foreach(problem =>{
            if(problemsAreIdentical(problem, potentialProblem)) isDuplicate = true
          })

          if(!isDuplicate){
            val exerise = new Exercise
            exerise.setProblem(potentialProblem)
              .setFolder(folder)
              //TODO 7/17/2020 add a method by which the user can set these settings on first transfer
              .setAllowedAttempts(10)
              .setMaxGrade(10)
              .setCourse(folder.getCourse)
              .save
          }
        }
      }, "class"->"checkbox")
    }
    val tableID = "problemOptionsTable"

    val table = <form>
      {
        TableHelper.renderTableWithHeaderPlusID(
          tableID,
          problems,
          ("Click to add problem", (problem: Problem) => checkBoxForProblem(problem)),
          ("Description", (problem: Problem) => Text(problem.getName)),
          ("Problem Type", (problem: Problem) => Text(problem.getTypeName))

        )
      }
      {
        SHtml.button("Add Problems", () => {
          S.redirectTo("/main/course/folders/index")
        })
      }
    </form>

    val descriptionFilterID = "input_desc"
    val descriptionFilter = SHtml.text("", (x: String)=>{},
      "onkeyup"->s"filterTableRows('$tableID', 'Description', '$descriptionFilterID');", "id"->descriptionFilterID)

    val problemTypeFilterID = "input_pt"
    val problemTypeFilter = SHtml.text("", (x: String)=>{},
      "onkeyup"->s"filterTableRows('$tableID', 'Problem Type', '$problemTypeFilterID');", "id"->problemTypeFilterID)

    val longdescriptionFilterID = "input_long_desc"
    val longdescriptionFilter = SHtml.text("", (x: String)=>{},
      "onkeyup"->s"filterTableRows('$tableID', 'Long Description', '$longdescriptionFilterID');", "id"->longdescriptionFilterID)

    Helpers.bind("problemoptions", xhtml,
      "problemlist" -> table,
      "descriptionfilter" -> descriptionFilter,
      "problemtypefilter" -> problemTypeFilter,
      "longdescriptionfilter" -> longdescriptionFilter
    )
  }

  def rendercreate(ignored: NodeSeq): NodeSeq = {
    if (SelectedProblemType.is == null) {
      S.warning("You have not selected a problem type")
      return S.redirectTo("/main/course/index")
    }

    val problemType = SelectedProblemType.is

    def createUnspecificProb(name: String, desc: String): Problem = {
      val createdBy: User = User.currentUser openOrThrowException "Lift protects this page against non-logged-in users"

      val unspecificProblem: Problem = Problem.create.setCreator(createdBy)
      unspecificProblem.setName(name).setDescription(desc).setProblemType(problemType).save

      if(TargetFolder.is != null){
        val exercise = new Exercise
        exercise.setFolder(TargetFolder.is)
                .setProblem(unspecificProblem)
                .setCourse(TargetFolder.is.getCourse.get)
                .save()
      }

      return unspecificProblem
    }

    def returnFunc(problem: Problem) = {
      CurrentProblemInCourse(problem)
      if(TargetFolder.is != null){
        TargetFolder(null)
        S.redirectTo("/main/course/folders/index")
      }

      S.redirectTo("/main/problempool/index")
    }

    return problemType.getProblemSnippet().renderCreate(createUnspecificProb, returnFunc)
  }

  def renderimportbutton(xhtml: NodeSeq): NodeSeq = {
    new UploadHelper(new UploadTarget(UploadTargetEnum.ProblemPool, null)).fileUploadForm(xhtml)
  }

  def renderaddproblemheader(xhtml: NodeSeq): NodeSeq = {

    <div>
      <div style="display: flex">
        <h2 style="margin-bottom: 0.5em; margin-right: 0.5em">All Exercises</h2>
        <br></br>
        <button type="button" id="add-problem_button_1" class="modal-button far fa-plus-square"/>
      </div>

      <div id="add-problem_modal_1" class="modal">

        <div class="modal-content">
          <div class="modal-header">
            <span class="close" id="add-problem_span_1">
              &times;
            </span>
          </div>
          <div class="modal-body">
            <h4>Select a Problem Type To Create</h4>
            <ul>
              {
                ProblemType.findAll().map(pt => {
                  <li>
                    {
                      SHtml.link("/main/problempool/create",() => {
                        SelectedProblemType(pt)
                      }, <button>{pt.getProblemTypeName}</button>)
                    }
                  </li>
                })
              }
            </ul>
          </div>
        </div>
      </div>
    </div>
  }
}

object Problempoolsnippet{
}