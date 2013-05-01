#!/bin/bash
./run/prepare.sh
mpirun -np 2 ./bin/polyeval_d2 -p 1 -b 5 -r 1 -i 10 -v 0 
