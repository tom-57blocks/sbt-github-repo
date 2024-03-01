package xyz.jia.scala.commons.utils

import java.io.File
import java.nio.file.{Files, Path, Paths}

import scala.io.Source
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.{Try, Using}

trait FileUtils {

  /** Locates a file at a specific path on the filesystem.
    *
    * @param filePath
    *   The absolute or relative link to the file
    * @throws java.lang.IllegalArgumentException
    *   if the file is not a regular file.
    * @return
    *   the `Path` representation of the file
    */
  @throws[IllegalArgumentException]
  def getFile(filePath: String): Path = {
    val regularFilePath = Paths.get(filePath)
    require(
      Files.isRegularFile(regularFilePath),
      s"'$filePath' is not a regular file or doesn't exist"
    )
    regularFilePath
  }

  /** Reads all the contents of a given file and returns them as a string. Warning: This method
    * should not be used for reading very large files. This is because it loads all the contents of
    * the file to memory and this could lead to system running out of memory.
    *
    * @param filePath
    *   The absolute or relative link to the file to read
    * @return
    *   a string containing all the contents of the file.
    */
  def getFileAsString(filePath: String): Try[String] =
    Try(getFile(filePath)).flatMap(file => getFileAsString(file.toFile))

  /** Reads all the contents of a given file and returns them as a string. Warning: This method
    * should not be used for reading very large files. This is because it loads all the contents of
    * the file to memory and this could lead to system running out of memory.
    *
    * @param file
    *   The file to read
    * @return
    *   a string containing all the contents of the file.
    */
  def getFileAsString(file: File): Try[String] =
    Using(Source.fromFile(file))(_.getLines().mkString("\n"))

  /** Reads all the contents of a given resource and returns them as a string. Warning: This method
    * should not be used for reading very large files. This is because it loads all the contents of
    * the file to memory and this could lead to system running out of memory.
    *
    * @param resourceName
    *   The name of the resource to read
    * @return
    *   a string containing all the contents of the file.
    */
  def getResourceAsString(resourceName: String): Try[String] =
    Using(Source.fromResource(resourceName))(_.getLines().mkString("\n"))

  /** Gets all files in a given directory. By default files in sub-directories will not be returned.
    *
    * @param directory
    *   The directory from which to read files
    * @param maxDepth
    *   controls how sub directories are handled. 1 means don't read sub directories, 2 means read
    *   up to the contents of the first sub directory
    * @throws java.lang.IllegalArgumentException
    *   if the specified location is not a directory
    * @return
    *   list of file `Path`s
    */
  @throws[IllegalArgumentException]
  def getFiles(directory: String, maxDepth: Int = 1): List[Path] = {
    val directoryPath = Paths.get(directory)
    require(Files.isDirectory(directoryPath), s"'$directory' is not a directory")
    Files
      .walk(directoryPath, maxDepth)
      .filter(path => Files.isRegularFile(path))
      .iterator()
      .asScala
      .toList
  }

}

object FileUtils extends FileUtils
