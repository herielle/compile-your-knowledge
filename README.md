# Compile Your Knowledge

## Overview

`Compile Your Knowledge` is a retro-computer take on the quiz show *Are You Smarter Than a 5th Grader?*, made for CMSC 13 (Survey of Programming Paradigms). The game boots up like an old operating system that has just been loaded to test what you know about programming. The host is the operating system itself, and every screen is an application window floating over a bright sky-and-hills wallpaper.

You play as the user signing in. The grade levels of the original show are replaced by two program folders. Theoretical covers the programming paradigms (procedural, object-oriented, functional, logic, event-driven, imperative, declarative, and modular), while Practical covers syntax and semantics questions in real programming languages. Theoretical modules stay locked behind padlocks until you clear the one before them.

Just like the TV show lets you ask classmates for help, you get lifelines in the form of system utilities: Peek shows a hint, Copy removes two wrong answers, and Save stores your progress to continue later. You start each topic with three hearts, every wrong answer costs one, and losing them all ends the round in Game Over. Questions always go from easy to hard, so every stage gets tougher as you go.

## Installation & Execution Instructions

There are different ways to compile and run the game.

### Standalone Application

The standalone executable included with this project was built for **macOS**.

```text
Compile Your Knowledge.app
```

It is intended to run on Mac computers and may not run on Windows because macOS applications are platform-specific.

To run the standalone version, simply open:

```text
Compile Your Knowledge.app
```

### Windows Compatibility

The included standalone `.app` is **not compatible with Windows**.

Windows users can still run the game from the source code using the Java command line instructions below, provided that Java and the required dependencies are installed.

To create a standalone Windows version, the project must be packaged on a Windows computer. This can produce a Windows executable such as:

```text
Compile Your Knowledge.exe
```

---

### CLI (Command Line Interface)

#### From the root directory (macOS / Linux)

```bash
javac -cp "lib/*" -d bin $(find src/compileyourknowledge -name "*.java")
java -cp "bin:lib/*:src" compileyourknowledge.Main
```

#### From the root directory (Windows)

```bat
dir /s /B src\compileyourknowledge\*.java > sources.txt
javac -cp "lib/*" -d bin @sources.txt
java -cp "bin;lib/*;src" compileyourknowledge.Main
```

---

### Creating and Running the JAR

From the root directory:

```bash
mkdir -p build/classes

javac -cp "lib/*" -d build/classes $(find src/compileyourknowledge -name "*.java")

cp -R src/resources build/classes/

jar cfe build/CompileYourKnowledge.jar compileyourknowledge.Main -C build/classes .
```

To run the JAR on macOS or Linux:

```bash
java -cp "build/CompileYourKnowledge.jar:lib/*" compileyourknowledge.Main
```

To run the JAR on Windows:

```bat
java -cp "build\CompileYourKnowledge.jar;lib/*" compileyourknowledge.Main
```

## NOTE

When running the game from the source code, make sure the project includes:

- `src/resources` folder containing the images, sounds, and questions
- JLayer JAR in the `lib` folder for the music and sound effects