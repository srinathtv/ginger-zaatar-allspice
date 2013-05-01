#!/bin/bash
./run/prepare.sh
mpirun -np 2 ./bin/bisect -p 1 -b 10 -r 1 -i 10 -v 0
