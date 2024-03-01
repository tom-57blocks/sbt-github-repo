package xyz.jia.scala.commons.playutils

import java.io.File
import javax.inject.Singleton

import scala.util.Try

import play.api.libs.json.{JsValue, Json}
import xyz.jia.scala.commons.utils

@Singleton
class FileUtils extends utils.FileUtils {

  /** Reads all the contents of a given file and returns them as a `JSON`. Warning: This method
    * should not be used for reading very large files. This is because it loads all the contents of
    * the file to memory and this could lead to system running out of memory.
    *
    * @param filePath
    *   The absolute or relative link to the file to read
    * @return
    *   a `JSON` object containing all the contents of the file.
    */
  def getFileAsJson(filePath: String): Try[JsValue] =
    getFileAsString(filePath).flatMap(content => Try(Json.parse(content)))

  /** Reads all the contents of a given file and returns them as a `JSON`. Warning: This method
    * should not be used for reading very large files. This is because it loads all the contents of
    * the file to memory and this could lead to system running out of memory.
    *
    * @param file
    *   The file to read
    * @return
    *   a `JSON` object containing all the contents of the file.
    */
  def getFileAsJson(file: File): Try[JsValue] =
    getFileAsString(file).flatMap(content => Try(Json.parse(content)))

  /** Reads all the contents of a given resource and returns them as `JSON`. Warning: This method
    * should not be used for reading very large files. This is because it loads all the contents of
    * the file to memory and this could lead to system running out of memory.
    *
    * @param resourceName
    *   The name of the resource to read
    * @return
    *   a `JSON` object containing all the contents of the file.
    */
  def getResourceAsJson(resourceName: String): Try[JsValue] =
    getResourceAsString(resourceName).flatMap(content => Try(Json.parse(content)))

}

object FileUtils extends FileUtils
