![build status](https://github.com/project-ncl/smeg/actions/workflows/main.yml/badge.svg)
# SMEg [Sbt Manipulator Extension pluGin]
SMEg is an SBT manipulation tool in the style of PME that aims to keep a consistent API between the two tools. 

## Installation in a project
Add the following to your `./project/plugins.sbt` file:

`addSbtPlugin("org.jboss.pnc.smeg" %% "smeg-plugin" % "0.1.0")`

Change the final string to match the version you wish to use

## Installation as a global plugin
Create the following file:
`~/.sbt/1.0/plugins/build.sbt`

With the following content:
`addSbtPlugin("org.jboss.pnc.smeg" %% "smeg-plugin" % "0.1.0")`

Change the final string to match the version you wish to use

## Extra repositories
Sbt will need access to some additional repositories in order to satisfy all of SMEg's dependencies.

 - Sonatype Snapshots: https://oss.sonatype.org/content/repositories/snapshots
 - Artima Maven Releases: https://repo.artima.com/releases
 - MRRC: https://maven.repository.redhat.com/ga/

You can enable them in the usual way for your build or globally via adding the following lines to `~/.sbt/1.0/global.sbt`:

```
resolvers ++= Seq(
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "Artima Maven" at "https://repo.artima.com/releases",
  "MRRC" at "https://maven.repository.redhat.com/ga/"
)
```

## Usage
Smeg is designed to be run as a pre-build step before you execute your build. It will output the necessary manipulations to a file named `smeg.manipulations.sbt` which defines the manipulations that will be applied at build time.

To run Smeg use the following command:
```
  $ sbt manipulate [options]
```

Options are passed as JVM parameters (i.e. using `-Dparam=value`). See the properties section for available parameters.

### Properties

#### General

##### Disable Manipulation
You can disable manipulation using the property `manipulation.disable`. This essentially turns the whole process into a noop.

Usage:

```
$ sbt manipulate -Dmanipulation.disable=true
```

#### Project Version Manipulation

##### Automatic version increment
The extension can be used to append a version suffix/qualifier to the current project, and then apply an incremented index to the version to provide a unique release version. For example, if the current project version is 1.0.0.GA, the extension can automatically set the version to 1.0.0.GA-rebuild-1, 1.0.0.GA-rebuild-2, etc.

The extension is configured using the property `versionIncrementalSuffix`.

Usage:

```
$ sbt manipulate -DversionIncrementalSuffix=rebuild
```

##### Version increment padding
When using the automatic increment it is also possible to configure padding for the increment. For instance, by setting `versionIncrementalSuffixPadding` to `3` the version will be `rebuild-003`. 

**Default is `5`**.

Usage:

```
$ sbt manipulate -DversionIncrementalSuffix=rebuild
```

##### Version Increment Metadata
The metadata to work out what the correct version of the increment should be can be sourced from a remote REST endpoint. This follows the exact same format as PME for which documentation can be found [here](https://release-engineering.github.io/pom-manipulation-ext/guide/dep-manip.html#rest-endpoint). 

The remote endpoint is activated using the property `resURL`.

Usage:

```
$ sbt manipulate -DrestURL=http://example.com/endpoint
```

##### Manual version suffix
The version suffix to be appended to the current project can be manually selected using the property `versionSuffix`.

Usage:

```
$ sbt manipulate -DversionSuffix=release-1
```

If the current version of the project is “1.2.0.GA”, the new version set during the build will be “1.2.0.GA-release-1”.

**Note**: `versionSuffix` takes precedence over `versionIncrementalSuffix`.

##### Version override
The version can be forcibly overridden by using the property `versionOverride`.

Usage:

```
$ sbt manipulate -DversionOverride=6.1.0.Final
```

If the current version of the project is “6.2.0”, the new version set during the build will be “6.1.0.Final”. A combination of properties may be used e.g.

```
$ sbt manipulate -DversionOverride=6.1.0.Final -DversionSuffix=rebuild-1
```

Using the above example, this would result in the version being “6.1.0.Final-rebuild-1”.

##### Snapshot Detection
The extension can detect snapshot versions and either preserve the snapshot or replace it with a real version. This is controlled by the property `versionSuffixSnapshot`. 

**The default is `false`** (i.e. remove SNAPSHOT and replace by the suffix).


Usage:

```
$ sbt manipulate -DversionSuffixSnapshot=true
```

This means that the SNAPSHOT suffix will be kept.

##### Suffix Stripping
Normally the tool will manipulate the version as given within the POM. However in certain scenarios it is desired that a known suffix is stripped from the version before any further manipulators (e.g. REST, Version etc) are run. To activate this pass:

```
$ sbt manipulate -DversionSuffixStrip=
```

This will utilise the default suffix strip configuration (in regular expression form) of `(.*)(.jbossorg-\d+)$`. To configure this to be something different simply pass e.g.:

```
$ sbt manipulate -DversionSuffixStrip='(.*)(.MYSUFFIX)$'
```

If the special keyword of NONE is used this will also disable the suffix (after it has been enabled) e.g.

```
$ sbt manipulate -DversionSuffixStrip= -DversionSuffixStrip=NONE
```

#### OSGi Compliance
If version manipulation is enabled the extension will also attempt to format the version to be OSGi compliant. For example if the versions are:
```
1
1.3
1.3-GA
1.3.0-GA
```
It will change to:
```
1.0.0
1.3.0
1.3.0.GA
1.3.0.GA
```

This is controlled by the property `versionOsgi`

Usage:

```
$ sbt manipulate -DversionOsgi=false
```

**The default is: `true`**.

