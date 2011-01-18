/* sbt -- Simple Build Tool
 * Copyright 2010  Mark Harrah
 */
package sbt
package complete

	import java.io.File

object HistoryCommands
{
	val Start = "!"
	// second characters
	val Contains = "?"
	val Last = "!"
	val ListCommands = ":"

	def ContainsFull = h(Contains)
	def LastFull = h(Last)
	def ListFull = h(ListCommands)

	def ListN = ListFull + "n"
	def ContainsString = ContainsFull + "string"
	def StartsWithString = Start + "string"
	def Previous = Start + "-n"
	def Nth = Start + "n"
	
	private def h(s: String) = Start + s
	def plainCommands = Seq(ListFull, Start, LastFull, ContainsFull)

	def descriptions = Seq(
		LastFull -> "Execute the last command again",
		ListFull -> "Show all previous commands",
		ListN -> "Show the last n commands",
		Nth -> ("Execute the command with index n, as shown by the " + ListFull + " command"),
		Previous -> "Execute the nth command before this one",
		StartsWithString -> "Execute the most recent command starting with 'string'",
		ContainsString -> "Execute the most recent command containing 'string'"
	)
	def helpString = "History commands:\n   " + (descriptions.map{ case (c,d) => c + "    " + d}).mkString("\n   ")
	def printHelp(): Unit =
		println(helpString)

	def apply(s: String, historyPath: Option[File], maxLines: Int, error: String => Unit): Option[List[String]] =
		if(s.isEmpty)
		{
			printHelp()
			Some(Nil)
		}
		else
		{
			val lines = historyPath.toList.flatMap( p => IO.readLines(p) ).toArray
			if(lines.isEmpty)
			{
				error("No history")
				None
			}
			else
			{
				val history = complete.History(lines, error)
				if(s.startsWith(ListCommands))
				{
					val rest = s.substring(ListCommands.length)
					val show = complete.History.number(rest).getOrElse(lines.length)
					printHistory(history, maxLines, show)
					Some(Nil)
				}
				else
				{
					val command = historyCommand(history, s)
					command.foreach(lines(lines.length - 1) = _)
					historyPath foreach { h => IO.writeLines(h, lines) }
					Some(command.toList)
				}
			}
		}
	def printHistory(history: complete.History, historySize: Int, show: Int): Unit = history.list(historySize, show).foreach(println)
	def historyCommand(history: complete.History, s: String): Option[String] =
	{
		if(s == Last)
			history !!
		else if(s.startsWith(Contains))
			history !? s.substring(Contains.length)
		else
			history ! s
	}
}