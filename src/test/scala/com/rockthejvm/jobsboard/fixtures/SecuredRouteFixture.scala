package com.rockthejvm.jobsboard.fixtures

import cats.effect.*
import cats.data.*
import com.rockthejvm.jobsboard.domain.security.Authenticator
import tsec.authentication.IdentityStore
import tsec.authentication.JWTAuthenticator
import tsec.mac.jca.HMACSHA256
import com.rockthejvm.jobsboard.domain.user.User
import com.rockthejvm.jobsboard.domain.security.JWTToken
import org.http4s.Request
import tsec.jws.mac.JWTMac
import org.http4s.Credentials
import org.http4s.AuthScheme

import scala.concurrent.duration.*
import org.http4s.headers.Authorization

trait SecuredRouteFixture extends UserFixture {
  val mockedAuthenticator: Authenticator[IO] = {
    val key = HMACSHA256.unsafeGenerateKey
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if (email == adminEmail) OptionT.pure(admin)
      else if (email == recruiterEmail) OptionT.pure(recruiter)
      else OptionT.none[IO, User]

    JWTAuthenticator.unbacked.inBearerToken(
      1.day,   // expiration of tokens
      None,    // max idle time (optional)
      idStore, // identity store
      key      // hash key
    )
  }

  extension (r: Request[IO]) {
    def withBearerToken(jwtToken: JWTToken): Request[IO] =
      r.putHeaders {
        val jwtString = JWTMac.toEncodedString[IO, HMACSHA256](jwtToken.jwt)
        // Authorization: Bearer {jwt}
        Authorization(Credentials.Token(AuthScheme.Bearer, jwtString))
      }
  }
}
