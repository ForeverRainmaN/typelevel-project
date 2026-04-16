package com.rockthejvm.jobsboard

import org.scalajs.dom.document

import scala.scalajs.js.annotation.*

@JSExportTopLevel("RockTheJvmApp")
class App {
  @JSExport
  def doSomething(containerId: String) =
    document.getElementById(containerId).innerHTML = "THIS ROCKS"
}
