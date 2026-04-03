package com.rockthejvm.jobsboard.domain

import cats.*
import cats.implicits.*
import com.rockthejvm.jobsboard.domain.user.*
import org.http4s.Response
import org.http4s.Status
import tsec.authentication.AugmentedJWT
import tsec.authentication.JWTAuthenticator
import tsec.authentication.SecuredRequest
import tsec.authentication.TSecAuthService
import tsec.authorization.AuthorizationInfo
import tsec.authorization.BasicRBAC
import tsec.mac.jca.HMACSHA256

object security {
  type Crypto              = HMACSHA256
  type JWTToken            = AugmentedJWT[HMACSHA256, String]
  type Authenticator[F[_]] = JWTAuthenticator[F, String, User, Crypto]
  type AuthRoute[F[_]]     = PartialFunction[SecuredRequest[F, User, JWTToken], F[Response[F]]]
  type AuthRBAC[F[_]]      = BasicRBAC[F, Role, User, JWTToken]

  // Role based access control
  // BasicRBAC[F, Role, User, JWTToken]
  given authRole[F[_]: MonadThrow]: AuthorizationInfo[F, Role, User] with {
    override def fetchInfo(u: User): F[Role] = u.role.pure[F]
  }

  def allRoles[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC.all[F, Role, User, JWTToken]

  def adminOnly[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC(Role.ADMIN)

  def recruiterOnly[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC(Role.RECRUITER)

  case class Authorizations[F[_]](rbackRoutes: Map[AuthRBAC[F], List[AuthRoute[F]]])
  object Authorizations {
    given combiner[F[_]]: Semigroup[Authorizations[F]] = Semigroup.instance { (authA, authB) =>
      Authorizations(authA.rbackRoutes |+| authB.rbackRoutes)
    }
  }

  extension [F[_]](authRoute: AuthRoute[F]) {
    def restrictedTo(rbac: AuthRBAC[F]): Authorizations[F] =
      Authorizations(Map(rbac -> List(authRoute)))
  }

  given auth2Tsec[F[_]: Monad]: Conversion[Authorizations[F], TSecAuthService[User, JWTToken, F]] =
    auths => {
      val unauthorizedService: TSecAuthService[User, JWTToken, F] =
        TSecAuthService[User, JWTToken, F] { _ =>
          Response[F](Status.Unauthorized).pure[F]
        }

      auths.rbackRoutes.toSeq
        .foldLeft(unauthorizedService) { case (acc, (rbac, routes)) =>
          val bigRoute = routes.reduce(_.orElse(_))
          TSecAuthService.withAuthorizationHandler(rbac)(bigRoute, acc.run)
        }
    }
}
