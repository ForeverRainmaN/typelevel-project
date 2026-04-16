package com.rockthejvm.jobsboard

import tyrian.*

object Router {
  enum Msg:
    case InternalLinkClicked(path: String)
    case NavigateTo(path: String)
    case InitializeFromUrl(path: String)
    case NoOp

  def fromLocation: Location => Msg = {
    case loc: Location.Internal =>
      loc.pathName match
        case "/jobs"   => Msg.InternalLinkClicked("/jobs")
        case "/login"  => Msg.InternalLinkClicked("/login")
        case "/signup" => Msg.InternalLinkClicked("/signup")
        case _         => Msg.NoOp

    case loc: Location.External =>
      Msg.NavigateTo(loc.href)
  }

  object Routes:
    val Jobs   = "/jobs"
    val Login  = "/login"
    val SignUp = "/signup"
}
