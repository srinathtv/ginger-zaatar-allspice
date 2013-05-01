#!/bin/bash
batch_size=2
reps=1
mat_size=1

if [ $# == 1 ]
then
    mat_size=$1
fi

./run/prepare.sh
mpirun -np 2 ./bin/matrix_cubicp_q -p 1 -b ${batch_size} -r ${reps} -i ${mat_size} -v 0
