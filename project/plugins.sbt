resolvers += Resolver.url("djspiewak-sbt-plugins", url("https://dl.bintray.com/djspiewak/sbt-plugins"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.codecommit" % "sbt-spiewak-bintray" % "0.3.1")
