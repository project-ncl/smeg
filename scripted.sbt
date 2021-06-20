scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq(
    "-Xmx1024M",
    "-Dplugin.version=" + version.value,
    "-Dsbt.boot.properties=./sbt.boot.properties"
  )
}

scriptedBufferLog := false