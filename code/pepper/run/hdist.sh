#!/bin/bash
./run/prepare.sh
mpirun -np 2 ./bin/hdist -p 1 -b 1 -r 1 -i 20 -v 0
