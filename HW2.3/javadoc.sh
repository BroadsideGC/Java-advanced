#!/bin/bash

javadoc -d docs -private -cp "./ImplementorTest.jar:./lib/*" -sourcepath "./src:/home/big/IdeaProjects/java-advanced-2016/java/" -link https://docs.oracle.com/javase/8/docs/api/ ru.ifmo.ctddev.zemskov.implementor info.kgeorgiy.java.advanced.implementor
