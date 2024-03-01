package xyz.jia.scala.commons.playutils.http.rest

import play.api.libs.ws.WSResponse

class UnsupportedApiResponseException(val message: String, val response: WSResponse)
    extends Exception(message)
