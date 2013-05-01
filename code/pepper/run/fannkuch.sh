#!/bin/bash
./run/prepare.sh
mpirun -np 2 ./bin/fannkuch -p 1 -b 100 -r 1 -i 10 -v 0
