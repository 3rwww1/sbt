/*
 * sbt
 * Copyright 2011 - 2018, Lightbend, Inc.
 * Copyright 2008 - 2010, Mark Harrah
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package sbt
package scriptedtest

import java.io.File

import scala.sys.process.{ BasicIO, Process }

import sbt.io.IO
import sbt.util.Logger

import xsbt.IPC

abstract class RemoteSbtCreator private[sbt] {
  def newRemote(server: IPC.Server): Process
}

final class RunFromSourceBasedRemoteSbtCreator(
    directory: File,
    log: Logger,
    launchOpts: Seq[String] = Nil,
) extends RemoteSbtCreator {
  def newRemote(server: IPC.Server) = {
    val globalBase = "-Dsbt.global.base=" + (new File(directory, "global")).getAbsolutePath
    val cp = IO readLinesURL (getClass getResource "/RunFromSource.classpath")
    val cpString = cp mkString File.pathSeparator
    val mainClassName = "sbt.RunFromSourceMain"
    val args = List(mainClassName, directory.toString, "<" + server.port)
    val cmd = "java" :: launchOpts.toList ::: globalBase :: "-cp" :: cpString :: args ::: Nil
    val io = BasicIO(false, log).withInput(_.close())
    val p = Process(cmd, directory) run (io)
    val thread = new Thread() { override def run() = { p.exitValue(); server.close() } }
    thread.start()
    p
  }
}
