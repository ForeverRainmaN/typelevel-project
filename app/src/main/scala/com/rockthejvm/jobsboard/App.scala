package com.rockthejvm.jobsboard

import cats.effect.*
import cats.effect.unsafe.implicits.global
import org.scalajs.dom.window
import tyrian.*
import tyrian.Html.*

import scala.scalajs.js.annotation.*

object App {
  type Msg = Router.Msg
  case class Model(currentPage: String)
}

@JSExportTopLevel("RockTheJvmApp")
class App extends TyrianApp[IO, App.Msg, App.Model] {

  override val run: IO[Nothing] => Unit =
    io => io.unsafeRunAndForget()

  override def router: Location => App.Msg =
    Router.fromLocation

  override def subscriptions(model: App.Model): Sub[IO, App.Msg] =
    Sub.None

  override def init(flags: Map[String, String]): (App.Model, Cmd[IO, App.Msg]) =
    val initialPath = window.location.pathname
    val pageToShow  = if (initialPath == "/") Router.Routes.Jobs else initialPath
    (App.Model(pageToShow), Cmd.None)

  override def view(model: App.Model): Html[App.Msg] =
    div(
      renderNavlink("Jobs", Router.Routes.Jobs),
      renderNavlink("Login", Router.Routes.Login),
      renderNavlink("Sign Up", Router.Routes.SignUp),
      div(s"Current page: ${model.currentPage}")
    )

  private def renderNavlink(text: String, location: String) =
    a(
      href    := location,
      `class` := "nav-link"
    )(text)

  override def update(model: App.Model): App.Msg => (App.Model, Cmd[IO, App.Msg]) =
    case Router.Msg.InternalLinkClicked(path) =>
      (model.copy(currentPage = path), Cmd.None)

    case Router.Msg.NavigateTo(path) =>
      (model.copy(currentPage = path), Nav.pushUrl(path))

    case Router.Msg.InitializeFromUrl(path) =>
      (model.copy(currentPage = path), Cmd.None)

    case Router.Msg.NoOp =>
      (model, Cmd.None)
}
