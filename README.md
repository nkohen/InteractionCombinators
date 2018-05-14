# InteractionCombinators
Two implementations of Lambda Calculus using Interaction Combinators

This is my final project for CS:5860 Lambda Calculus

See the Write-Up for a description

Note: this project depends on my Regex project (https://github.com/nkohen/Regex) for parsing in terms although this project does not depend on the implementation of parsing in Term.java

Compilation Instructions:
1) Download this repo
2) Open a terminal in side the src directory
3) "javac Regex\*.java"
4) "cd FourCombinatorImplementation"
5) "javac -cp .. Cell.java InteractionNet.java LambdaNet.java Port.java Term.java Wire.java"
6) "cd ..\FullParallelReduction"
7) "javac -cp .. Cell.java InteractionNet.java LambdaNet.java Port.java Term.java Wire.java" (same as step 5)
Everything except for the tests are now compiled and the main method in LambdaNet of FourCombinatorImplementation (resp. FullParallelReduction) can be run from src by executing "java FourCombinatorImplementation.LambdaNet" (resp "java FullParallelReduction.LambdaNet"), or write your own main method using these classes.

The tests are written for JUnit 5 (https://junit.org/junit5).
