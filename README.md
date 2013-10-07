# Simulation of the Dining Philosophers Problem

As always you need git and sbt (at least 0.12.*) installed.

You can then simply clone the repo and run `sbt "run 5 5 3 30"` to simulate 5 philosophers, thinking for 5 secs, eating for 3 secs and running the simulation for 30 secs.

The repo also contains State Transition Diagrams to illustrate/document what is going on. They are build with plantUML. Just go the `uml` directory and run `build.sh`. The `png` files will be in the `target` directory.