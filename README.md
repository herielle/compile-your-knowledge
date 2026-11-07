# Compile Your Knowledge

## Overview
`Compile Your Knowledge` is a retro-computer take on the quiz show "Are You Smarter Than a 5th Grader?", made for CMSC 13 (Survey of Programming Paradigms). The game boots up like an old operating system that has just been loaded to test what you know about programming. The host is the operating system itself, and every screen is an application window floating over a bright sky-and-hills wallpaper.

You play as the user signing in. The grade levels of the original show are replaced by two program folders. Theoretical covers the programming paradigms (procedural, object-oriented, functional, logic, event-driven, imperative, declarative, and modular), while Practical covers syntax and semantics questions in real programming languages. Theoretical modules stay locked behind padlocks until you clear the one before them.

Just like the TV show lets you ask classmates for help, you get lifelines in the form of system utilities: Peek shows a hint, Copy removes two wrong answers, and Save stores your progress to continue later. You start each topic with three hearts, every wrong answer costs one, and losing them all ends the round in Game Over. Questions always go from easy to hard, so every stage gets tougher as you go.

## Installation & Execution Instructions
Here are the different ways you are able to compile game

### CLI (Command Line Interface)
- From the root directory (macOS / Linux)

    `javac -cp lib/jl1.0.1.jar -d bin -sourcepath src src/compileyourknowledge/Main.java && java -cp bin:lib/jl1.0.1.jar compileyourknowledge.Main`

- From the root directory (Windows)

    `javac -cp lib/jl1.0.1.jar -d bin -sourcepath src src/compileyourknowledge/Main.java && java -cp "bin;lib/jl1.0.1.jar" compileyourknowledge.Main`

- Jar and run (From root directory)

    `jar cfe CompileYourKnowledge.jar compileyourknowledge.Main -C bin . -C src resources`

### NOTE!!!

Must run the game where there are
 - src/resources folder (images, sounds, and questions)
 - JLayer jar (for the music and sound effects), put in `lib`