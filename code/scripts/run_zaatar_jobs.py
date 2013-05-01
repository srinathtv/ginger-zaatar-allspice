#!/usr/bin/python

import os
import sys
import time

def run_one_tacc_job(num_nodes, time, sample_index, log_path, rslt_path,
	    computation, batch_size, input_size, git_tag, clone_remote,
      rebuild_local, protocol, num_tasks_per_node=1, max_provers=60):
  cmd = """./driver_tacc_jobs.py --cluster=longhorn \
             --numnodes=%s \
             --time=%s \
             --email=mailtosrinath@gmail.com \
             --jobtype=data \
             --sample_index=%s \
             --log_path=%s \
             --rslt_path=%s \
             --computation=%s \
             --batch_size=%s \
             --input_size=%s \
             --protocol=%s \
             --git_tag=%s \
             --clone_remote=%s \
             --rebuild_local=%s \
             --taskspernode=%s \
             --max_provers=%s
        """ % (num_nodes, time, sample_index, log_path, rslt_path,
	       computation, batch_size, input_size, protocol, git_tag, clone_remote,
         rebuild_local, num_tasks_per_node, max_provers)
  print "EXECUTING: ", cmd
  os.system(cmd)


## Experiments.
zaatar_git_tag = ""
local_log_prefix = "/home/srinath/logs/pepper/vercomp_logs/zaatar_cr/"
local_rslt_prefix = "/home/srinath/data3/doc/eurosys13/log_reports/"


# Set of experiments used in Zaatar 
do_clone_remote = "1"
do_rebuild_local = "1"
for sample in range(1,4):
  run_one_tacc_job(num_nodes=2, time="03:00:00", sample_index=sample,\
      log_path=local_log_prefix + "zaatar_one_cpu2",\
      rslt_path=local_rslt_prefix + "zaatar_one_cpu2",\
      computation="f_w_apsp", batch_size=1, input_size=25,\
      git_tag=zaatar_git_tag, clone_remote=do_clone_remote,
      rebuild_local=do_rebuild_local, protocol="zaatar",
      num_tasks_per_node="1", max_provers="1")

  run_one_tacc_job(num_nodes=2, time="03:00:00", sample_index=sample,\
      log_path=local_log_prefix + "zaatar_one_cpu2",\
      rslt_path=local_rslt_prefix + "zaatar_one_cpu2",\
      computation="f_w_apsp", batch_size=1, input_size=20,\
      git_tag=zaatar_git_tag, clone_remote=do_clone_remote,
      rebuild_local=do_rebuild_local, protocol="zaatar",
      num_tasks_per_node="1", max_provers="1")

  run_one_tacc_job(num_nodes=2, time="03:00:00", sample_index=sample,\
      log_path=local_log_prefix + "zaatar_one_cpu2",\
      rslt_path=local_rslt_prefix + "zaatar_one_cpu2",\
      computation="f_w_apsp", batch_size=1, input_size=10,\
      git_tag=zaatar_git_tag, clone_remote=do_clone_remote,
      rebuild_local=do_rebuild_local, protocol="zaatar",
      num_tasks_per_node="1", max_provers="1")

  run_one_tacc_job(num_nodes=2, time="03:00:00", sample_index=sample,\
      log_path=local_log_prefix + "zaatar_one_cpu2",\
      rslt_path=local_rslt_prefix + "zaatar_one_cpu2",\
      computation="f_w_apsp", batch_size=1, input_size=5,\
      git_tag=zaatar_git_tag, clone_remote=do_clone_remote,
      rebuild_local=do_rebuild_local, protocol="zaatar",
      num_tasks_per_node="1", max_provers="1")

  run_one_tacc_job(num_nodes=2, time="03:00:00", sample_index=sample,\
      log_path=local_log_prefix + "zaatar_one_cpu2",\
      rslt_path=local_rslt_prefix + "zaatar_one_cpu2",\
      computation="pam_clustering", batch_size=1, input_size=20,\
      git_tag=zaatar_git_tag, clone_remote=do_clone_remote,
      rebuild_local=do_rebuild_local, protocol="zaatar",
      num_tasks_per_node="1", max_provers="1")
  
  run_one_tacc_job(num_nodes=2, time="03:00:00", sample_index=sample,\
      log_path=local_log_prefix + "zaatar_one_cpu2",\
      rslt_path=local_rslt_prefix + "zaatar_one_cpu2",\
      computation="pam_clustering", batch_size=1, input_size=10,\
      git_tag=zaatar_git_tag, clone_remote=do_clone_remote,
      rebuild_local=do_rebuild_local, protocol="zaatar",
      num_tasks_per_node="1", max_provers="1")

  run_one_tacc_job(num_nodes=2, time="03:00:00", sample_index=sample,\
      log_path=local_log_prefix + "zaatar_one_cpu2",\
      rslt_path=local_rslt_prefix + "zaatar_one_cpu2",\
      computation="pam_clustering", batch_size=1, input_size=5,\
      git_tag=zaatar_git_tag, clone_remote=do_clone_remote,
      rebuild_local=do_rebuild_local, protocol="zaatar",
      num_tasks_per_node="1", max_provers="1")

  run_one_tacc_job(num_nodes=2, time="03:00:00", sample_index=sample,\
      log_path=local_log_prefix + "zaatar_one_cpu2",\
      rslt_path=local_rslt_prefix + "zaatar_one_cpu2",\
      computation="bisect_sfdl", batch_size=1, input_size=256,\
      git_tag=zaatar_git_tag, clone_remote=do_clone_remote,
      rebuild_local=do_rebuild_local, protocol="zaatar",
      num_tasks_per_node="1", max_provers="1")

  run_one_tacc_job(num_nodes=2, time="03:00:00", sample_index=sample,\
      log_path=local_log_prefix + "zaatar_one_cpu2",\
      rslt_path=local_rslt_prefix + "zaatar_one_cpu2",\
      computation="bisect_sfdl", batch_size=1, input_size=128,\
      git_tag=zaatar_git_tag, clone_remote=do_clone_remote,
      rebuild_local=do_rebuild_local, protocol="zaatar",
      num_tasks_per_node="1", max_provers="1")

  run_one_tacc_job(num_nodes=2, time="03:00:00", sample_index=sample,\
      log_path=local_log_prefix + "zaatar_one_cpu2",\
      rslt_path=local_rslt_prefix + "zaatar_one_cpu2",\
      computation="bisect_sfdl", batch_size=1, input_size=64,\
      git_tag=zaatar_git_tag, clone_remote=do_clone_remote,
      rebuild_local=do_rebuild_local, protocol="zaatar",
      num_tasks_per_node="1", max_provers="1")

  run_one_tacc_job(num_nodes=2, time="03:00:00", sample_index=sample,\
      log_path=local_log_prefix + "zaatar_one_cpu2",\
      rslt_path=local_rslt_prefix + "zaatar_one_cpu2",\
      computation="dna_align", batch_size=1, input_size=300,\
      git_tag=zaatar_git_tag, clone_remote=do_clone_remote,
      rebuild_local=do_rebuild_local, protocol="zaatar",
      num_tasks_per_node="1", max_provers="1")

  run_one_tacc_job(num_nodes=2, time="03:00:00", sample_index=sample,\
      log_path=local_log_prefix + "zaatar_one_cpu2",\
      rslt_path=local_rslt_prefix + "zaatar_one_cpu2",\
      computation="dna_align", batch_size=1, input_size=150,\
      git_tag=zaatar_git_tag, clone_remote=do_clone_remote,
      rebuild_local=do_rebuild_local, protocol="zaatar",
      num_tasks_per_node="1", max_provers="1")

  run_one_tacc_job(num_nodes=2, time="03:00:00", sample_index=sample,\
      log_path=local_log_prefix + "zaatar_one_cpu2",\
      rslt_path=local_rslt_prefix + "zaatar_one_cpu2",\
      computation="dna_align", batch_size=1, input_size=75,\
      git_tag=zaatar_git_tag, clone_remote=do_clone_remote,
      rebuild_local=do_rebuild_local, protocol="zaatar",
      num_tasks_per_node="1", max_provers="1")

  run_one_tacc_job(num_nodes=2, time="03:00:00", sample_index=sample,\
      log_path=local_log_prefix + "zaatar_one_cpu2",\
      rslt_path=local_rslt_prefix + "zaatar_one_cpu2",\
      computation="fannkuch", batch_size=1, input_size=100,\
      git_tag=zaatar_git_tag, clone_remote=do_clone_remote,
      rebuild_local=do_rebuild_local, protocol="zaatar",
      num_tasks_per_node="1", max_provers="1")

  run_one_tacc_job(num_nodes=2, time="03:00:00", sample_index=sample,\
      log_path=local_log_prefix + "zaatar_one_cpu2",\
      rslt_path=local_rslt_prefix + "zaatar_one_cpu2",\
      computation="fannkuch", batch_size=1, input_size=50,\
      git_tag=zaatar_git_tag, clone_remote=do_clone_remote,
      rebuild_local=do_rebuild_local, protocol="zaatar",
      num_tasks_per_node="1", max_provers="1")

  run_one_tacc_job(num_nodes=2, time="03:00:00", sample_index=sample,\
      log_path=local_log_prefix + "zaatar_one_cpu2",\
      rslt_path=local_rslt_prefix + "zaatar_one_cpu2",\
      computation="fannkuch", batch_size=1, input_size=25,\
      git_tag=zaatar_git_tag, clone_remote=do_clone_remote,
      rebuild_local=do_rebuild_local, protocol="zaatar",
      num_tasks_per_node="1", max_provers="1")
