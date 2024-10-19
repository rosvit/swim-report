# :swimmer: Swim Report

Simple CLI application that reads a FIT file of a lap swim activity and produces summary statistics.

## Features

- Text or JSON output
- General swim activity details:
    - Pool length
    - Number of lengths swum and total distance (all swim strokes combined)
    - Average heart rate
    - Activity duration
    - Activity start time
- Total distance swum for each stroke
- Number of pool lengths swum for each stroke
- Longest continuous swim interval for each stroke (calculated from single stroke laps only)
- Average swim pace for each swim stroke
- Total rest time
- Option to force an attempt to read a corrupt FIT file

## Usage

`swim-report` is a CLI application, Java 8 or newer is required to run it.

To download the latest available build, go to the releases in this repository or use 
[this link](https://github.com/rosvit/swim-report/releases/latest/download/swim-report.jar).

Read FIT file and display report:

```
java -jar swim-report.jar [--output <text|json>] [--ignore-integrity] <FIT file path>
```

For help on supported options:

```
java -jar swim-report.jar --help
```

### How to get activity FIT file

To export a lap swim activity into a FIT file that can be processed by `swim-report`, please use the features
of the native platform of your sports watch:
- [Coros](https://support.coros.com/hc/en-us/articles/360043975752-How-to-export-workout-data-from-COROS-and-manually-upload-to-3rd-party-apps)
- [Garmin](https://support.strava.com/hc/en-us/articles/216917807-Exporting-Files-from-Garmin-Connect)

## Building from source

JDK 8 or later and [sbt](https://www.scala-sbt.org/) are required to build the application from source.

Use `sbt compile` to compile the sources, `sbt test` to run the tests and `sbt assembly` to build the JAR with all dependencies.
