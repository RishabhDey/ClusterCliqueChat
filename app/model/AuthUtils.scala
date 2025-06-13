package model
import java.time.Clock
import java.security.{KeyPairGenerator, PrivateKey, PublicKey}
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}


/*
Initializes a secure JWT token for access and refresh token for
the user to store in cookies. Refresh Token should only be used in POST
requests and should only be used to request JWT.

All functions have descriptive names.
 */
object AuthUtil {
  implicit val clock: Clock = Clock.systemUTC()

  private val refreshTokenValidity: Long = 10
  private val refreshTokenUnit = ChronoUnit.DAYS
  private val algorithm = JwtAlgorithm.EdDSA

  private val keyPair = {
    val kpg = KeyPairGenerator.getInstance("Ed25519")
    kpg.generateKeyPair()
  }
  private val privateKey: PrivateKey = keyPair.getPrivate

  /*If needed in the future (other API calls and such), this can be made public for JWT access for other API calls,
  just lmk before you do it though.
  */
  private val publicKey: PublicKey = keyPair.getPublic

  private[model] def generateNewRefreshToken(user: User): RefreshToken = {
    RefreshToken(user = user, expiry = Instant.now.plus(refreshTokenValidity, refreshTokenUnit))
  }
  private[model] def generateNewAccessToken(user: User): String = {
    val claim = JwtClaim(subject = Some(user.userId),
                          issuer = Some("ClusterApp"),
                          issuedAt = Some(Instant.now.getEpochSecond),
                          expiration = Some(Instant.now.plusSeconds(3600).getEpochSecond))
    Jwt.encode(claim, privateKey, algorithm)
  }
  def validateUserToken(token: String): Option[String] = {
    Jwt.decode(token, publicKey, Seq(algorithm))
      .toOption.filter{ decoded => decoded.isValid}
      .flatMap{_.subject}
  }

}
case class RefreshToken(refreshToken: String = UUID.randomUUID().toString, user: User, expiry: Instant)
