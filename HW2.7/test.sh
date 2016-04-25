#!/bin/bash

sudo echo 0 > /sys/devices/system/node/node0/cpu7/online
sudo echo 0 > /sys/devices/system/node/node0/cpu5/online
sudo echo 0 > /sys/devices/system/node/node0/cpu3/online

java -cp "./ParallelMapperTest.jar:./lib/*:./out/production/HW2.6" info.kgeorgiy.java.advanced.mapper.Tester list ru.ifmo.ctddev.zemskov.mapper.ParallelMapperImpl,ru.ifmo.ctddev.zemskov.mapper.IterativeParallelism $1

sudo echo 1 > /sys/devices/system/node/node0/cpu7/online
sudo echo 1 > /sys/devices/system/node/node0/cpu5/online
sudo echo 1 > /sys/devices/system/node/node0/cpu3/online
sudo echo 1 > /sys/devices/system/node/node0/cpu1/online
sudo echo 1 > /sys/devices/system/node/node0/cpu6/online
sudo echo 1 > /sys/devices/system/node/node0/cpu4/online
sudo echo 1 > /sys/devices/system/node/node0/cpu2/online
