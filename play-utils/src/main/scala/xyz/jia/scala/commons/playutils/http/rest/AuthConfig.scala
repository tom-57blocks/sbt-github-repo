package xyz.jia.scala.commons.playutils.http.rest

import play.api.libs.ws.WSAuthScheme

case class AuthConfig(
    username: String,
    password: String,
    scheme: WSAuthScheme
)
