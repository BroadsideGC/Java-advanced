#!/bin/bash

javadoc -d docs -cp "./IterativeParallelismTest.jar:./lib/*" -sourcepath "./src:/home/big/IdeaProjects/java-advanced-2016/java/" -link https://docs.oracle.com/javase/8/docs/api/ ru.ifmo.ctddev.zemskov.concurrent info.kgeorgiy.java.advanced.concurrent
