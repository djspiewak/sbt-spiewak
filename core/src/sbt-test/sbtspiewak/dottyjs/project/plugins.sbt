sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("com.codecommit" % "sbt-spiewak" % x)
  case _       => sys.error("""|The system property 'plugin.version' is not defined.
                               |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}

addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "1.1.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")
