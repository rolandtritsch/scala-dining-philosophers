#! /bin/bash
export GRAPHVIZ_DOT=/opt/local/bin/dot
java -jar ./bin/plantuml.jar -v -tpng -o "../target" "./src/*"
