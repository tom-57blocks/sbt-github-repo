package xyz.jia.scala.commons.utils

import org.apache.commons.codec.digest.DigestUtils

trait ChecksumUtil {

  def checksum(data: String): String = DigestUtils.sha256Hex(data)

  def checksum(data: Array[Byte]): String = DigestUtils.sha256Hex(data)

}

object ChecksumUtil extends ChecksumUtil
