package com.rockthejvm.jobsboard.modules

import cats.effect.*
import cats.data.*
import cats.implicits.*
import com.rockthejvm.jobsboard.http.routes.AuthRoutes
import com.rockthejvm.jobsboard.http.routes.HealthRoutes
import com.rockthejvm.jobsboard.http.routes.JobRoutes
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.typelevel.log4cats.Logger
import tsec.mac.jca.HMACSHA256
import tsec.authentication.IdentityStore
import tsec.common.SecureRandomId
import tsec.authentication.BackingStore
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.config.*
import com.rockthejvm.jobsboard.algebra.Users
import tsec.authentication.JWTAuthenticator
import tsec.authentication.SecuredRequestHandler

class HttpApi[F[_]: Async: Logger] private (
    core: Core[F],
    authenticator: Authenticator[F]
) {
  given requestHandler: SecuredHandler[F] = SecuredRequestHandler(authenticator)
  private val healthRoutes                = HealthRoutes[F].routes
  private val jobRoutes                   = JobRoutes[F](core.jobs).routes
  private val authRoutes                  = AuthRoutes[F](core.auth)(authenticator).routes

  val endpoints: HttpRoutes[F] = Router(
    "/api" -> (healthRoutes <+> jobRoutes <+> authRoutes)
  )
}

object HttpApi {
  def apply[F[_]: Async: Logger](
      core: Core[F]
  )(securityConfig: SecurityConfig): Resource[F, HttpApi[F]] =
    Resource
      .eval(createAuthenticator(core.users)(securityConfig))
      .map(authenticator => new HttpApi[F](core, authenticator))

  def createAuthenticator[F[_]: Sync](
      users: Users[F]
  )(securityConfig: SecurityConfig): F[Authenticator[F]] = {
    // 1. Identity store
    val idStore: IdentityStore[F, String, User] = (email: String) => OptionT(users.find(email))
    // 2. backing store for JWT tokens: BackingStore[F, id, JwtToken]
    val tokenStoreF = Ref.of[F, Map[SecureRandomId, JWTToken]](Map.empty).map { ref =>
      new BackingStore[F, SecureRandomId, JWTToken] {
        override def get(id: SecureRandomId): OptionT[F, JWTToken] = OptionT(ref.get.map(_.get(id)))
        override def put(elem: JWTToken): F[JWTToken] =
          ref.modify(store => (store + (elem.id -> elem), elem))
        override def update(v: JWTToken): F[JWTToken] =
          put(v)
        override def delete(id: SecureRandomId): F[Unit] =
          ref.modify(store => (store - id, ()))
      }
    }

    // 3. hashing key
    val keyF =
      HMACSHA256.buildKey[F](securityConfig.secret.getBytes("UTF-8"))

    for {
      key        <- keyF
      tokenStore <- tokenStoreF
      // 4. authenticator
    } yield JWTAuthenticator.backed.inBearerToken(
      expiryDuration = securityConfig.jwtExpiryDuration, // expiration of tokens
      maxIdle = None,                                    // max idle time (optional)
      identityStore = idStore,                           // identity store
      tokenStore = tokenStore,                           // tokenStore
      signingKey = key                                   // hash key
    )
  }
}
