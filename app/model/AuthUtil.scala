package model
import java.time.Clock

import java.security.{KeyPairGenerator, PrivateKey, PublicKey}
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

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
  val publicKey: PublicKey = keyPair.getPublic

  def generateNewRefreshToken(user: User): RefreshToken = {
    RefreshToken(user = user, expiry = Instant.now.plus(refreshTokenValidity, refreshTokenUnit))
  }
  def generateNewAccessToken(user: User): String = {
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
