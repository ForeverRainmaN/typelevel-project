package com.rockthejvm.jobsboard.domain

import tsec.authentication.{AugmentedJWT, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256
import com.rockthejvm.jobsboard.domain.user.*
import tsec.authentication.SecuredRequest
import org.http4s.Response

object security {
  type Crypto              = HMACSHA256
  type JWTToken            = AugmentedJWT[HMACSHA256, String]
  type Authenticator[F[_]] = JWTAuthenticator[F, String, User, Crypto]
  type AuthRoute[F[_]]     = PartialFunction[SecuredRequest[F, User, JWTToken], F[Response[F]]]
}
