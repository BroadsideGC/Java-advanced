#!/bin/bash

rm -rf build

mkdir build

manifest=/tmp/MANIFEST.MF
src_files=/tmp/src_files.txt

find src/ru/ifmo/ctddev/zemskov/implementor -type f -name '*.java' > "$src_files"

javac -classpath /home/big/IdeaProjects/HW2.3/ImplementorTest.jar -d build "@$src_files"

echo "Manifest-Version: 1.0" > "$manifest"
echo "Class-Path: /home/big/IdeaProjects/HW2.3/ImplementorTest.jar" >> "$manifest"
echo "Created-By: Kirill Zemskov" >> "$manifest"
echo "Main-Class: ru.ifmo.ctddev.zemskov.implementor.Runner" >> "$manifest"

jar cvfm Implementor.jar "$manifest" -C build/ .

rm -f "$manifest" "$src_files"

rm -rf build
