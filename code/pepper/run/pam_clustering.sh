#!/bin/bash
./run/prepare.sh
mpirun -np 2 ./bin/pam_clustering -p 1 -b 2 -r 1 -i 10 -v 0
