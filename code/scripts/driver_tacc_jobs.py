#!/usr/bin/python

import os
import sys
import time
import random
from optparse import OptionParser
import math
import re

USERNAME = "srinath"
SCRATCH_DIR = "/scratch/01934/srinath"
LONGHORN = "longhorn.tacc.utexas.edu"
LONESTAR = "lonestar.tacc.utexas.edu"

HOME_DIR = ""
REPO_FOLDER = "expt_clone_repo/"
REPO_DIR = ""
CODE_DIR = ""
BIN_DIR = ""
MODULE_DIR = ""
PKG_DIR = ""

SCRIPT_FILE = "job_pepper.sh"
JOB_ID_FILE = "job_id.txt"
CODE_REV_LOG = "code_revision_id.log"
CONFIG_LOG = "config.log"
OUTPUT_FILE = "client_cpu.log"

BATCH_SIZE = 150
NUM_REPS = 1
INPUT_SIZE = 100

SAMPLE_INDEX = 1
PROJECT_NAME = "pepper_mpi"

BUILD_COMMAND = "make"

class Experiment(object):

  def __init__(self, user, host, index, computation, batch_size, input_size, \
	       log_path, rslt_path, protocol_name):
    self.user = user
    self.host = host
    self.sample_index = index
    self.expt_name = protocol_name + "_" + computation + "_" + input_size + "_" + str(self.sample_index)
    self.computation = computation
    self.batch_size = batch_size
    self.input_size = input_size
    self.rslt_path = rslt_path
    self.set_log_path(log_path)
    print "LOG PATH: ", self.log_path
    print "RSLT PATH: ", self.rslt_path
    self.create_dir(self.log_path)
    self.create_dir(self.rslt_path)
    self.protocol = protocol_name
  
  def create_dir(self, dir_path):
    if os.system("mkdir -p %s" % dir_path) != 0:
      print "Failed to create path: ", dir_path
      sys.exit(1)

  def create_remote_dir(self, dir_path):
    command = "mkdir %s" % dir_path
    self.run_remote_command(command)
  
  def set_log_path(self, log_path):
    tm = time.localtime()
    self.log_path = \
      os.path.join(log_path, self.computation, "%s_%s_%s" % (tm.tm_mday, tm.tm_mon, tm.tm_year),\
                   "venezia_%s_%s_%s_%s_%s_%s_%s_%s_%s" % \
                   (tm.tm_mday, tm.tm_mon, tm.tm_year, tm.tm_hour, tm.tm_min,\
                    tm.tm_sec, tm.tm_wday, tm.tm_yday, tm.tm_isdst))

  def get_lonestar_job_script(self, jobname, tasks_per_node, num_nodes, \
    time_hrs, email, protocol, job_type="gpu"):
    
    cmt_protocol_type = "0"
    if (protocol == "cmt"):
      cmt_protocol_type = "0"
    elif (protocol == "cmtpp"):
      cmt_protocol_type = "1" 
    
    job_script = """#!/bin/bash
#$ -V    # Inherit the submission environment
#$ -cwd  # Start job in submission directory
#$ -N %s        # Job Name
#$ -j y  # Combine stderr and stdout
#$ -o %s         # Name of the output file (eg. myMPI.oJobID)
#$ -pe %sway %s   # Requests 8 tasks/node, 32 cores total
#$ -l h_rt=%s      # Run time (hh:mm:ss)
#$ -q %s        # Queue name "normal"
#$ -m be	 # Email at Begin and End of job
#$ -M %s      # Use email notification address
#$ -A Pepper

set -x	 # Echo commands, use "set echo" with csh
module use ~/modulefiles 
module load python 
module load gmp 
module load ntl
module load chacha 
module load encrypt 
module load fcgi 
module load libconfig
module load papi
module load cuda
module load cuda_SDK
module load pbc
"""  %  (jobname, OUTPUT_FILE, tasks_per_node, str(int(num_nodes)*12), time_hrs, \
           job_type, email)

    if (protocol == "zaatar" or protocol == "ginger" or protocol == "ginger-tailored"):
      job_script += """
ibrun tacc_affinity ./bin/%s -p 1 -b %s -r %s -i %s -v %s
"""  %  (self.computation, self.batch_size, NUM_REPS, self.input_size, 0)
    elif(protocol == "cmtpp" or protocol == "cmt"):
      job_script += """
ibrun tacc_affinity ./bin/cmtgkr/apps/%s %s %s
"""  %  (self.computation, str(int(math.log(float(self.input_size))/math.log(2.0))), cmt_protocol_type)
    elif(protocol == "batch-cmtpp"):
      if (self.computation == "bisect_sfdl"):
        denom = "5"
      elif (self.computation == "pd2_sfdl"):
        denom = "32"
      else:
        denom = "1"
      job_script += """
ibrun tacc_affinity ./bin/cmtgkr/apps/pws_cmtgkr bin/%s.pws %s 32 %s 
"""  %  (self.computation, self.batch_size, denom)
     
    return job_script
 
  
  
  def get_longhorn_job_script(self, jobname, tasks_per_node, num_nodes, \
    time_hrs, email, protocol, job_type="data"):

    cmt_protocol_type = "0"
    if (protocol == "cmt"):
      cmt_protocol_type = "0"
    elif (protocol == "cmtpp"):
      cmt_protocol_type = "1"

    job_script = """#!/bin/bash
#$ -V    # Inherit the submission environment
#$ -cwd	 # Start job in submission directory
#$ -N %s	 # Job Name
#$ -j y	 # Combine stderr and stdout
#$ -o %s 	 # Name of the output file (eg. myMPI.oJobID)
#$ -pe %sway %s	 # Requests 8 tasks/node, 32 cores total
#$ -q normal	 # Queue name "normal"
#$ -l h_rt=%s	 # Run time (hh:mm:ss)
#$ -M %s # Use email notification address
#$ -m be	 # Email at Begin and End of job
#$ -P %s
#$ -A Pepper
set -x	 # Echo commands, use "set echo" with csh
""" % (jobname, OUTPUT_FILE, tasks_per_node, str(int(num_nodes)*8), time_hrs, email, job_type)

    if (protocol == "zaatar" or protocol == "ginger" or protocol == "ginger-tailored"):
      job_script += """
ibrun tacc_affinity ./bin/%s -p 1 -b %s -r %s -i %s -v %s
"""  %  (self.computation, self.batch_size, NUM_REPS, self.input_size, 0)
    elif(protocol == "cmtpp" or protocol == "cmt"):
      job_script += """
ibrun tacc_affinity ./bin/cmtgkr/apps/%s %s %s
"""  %  (self.computation, str(int(math.log(float(self.input_size))/math.log(2.0))), cmt_protocol_type)
    elif(protocol == "batch-cmtpp"):
      if (self.computation == "bisect_sfdl"):
        denom = "5"
      elif (self.computation == "pd2_sfdl"):
        denom = "32"
      else:
        denom = "1"
      job_script += """
ibrun tacc_affinity ./bin/cmtgkr/apps/pws_cmtgkr bin/%s.pws %s 32 %s 
"""  %  (self.computation, self.batch_size, denom)
     
    return job_script

  def run_remote_command(self, cmd, rdir=""):
    if (rdir == ""):
      rdir = HOME_DIR
    remote_cmd = 'ssh %s@%s "cd %s && %s"' % (self.user, self.host, rdir, cmd)
    print "Executing :", remote_cmd
    os.system(remote_cmd)
    time.sleep(1)
  
  def load_tacc_module(self, module):
    self.run_remote_command("module load %s" % (module))


  def clone_repo(self, checkout_tag):
    self.run_remote_command("mkdir %s" % (REPO_DIR))
    self.run_remote_command("module load git && git clone ssh://neo.csres.utexas.edu/var/git/vercomp.git", rdir=REPO_DIR)
    self.run_remote_command("module load git && git checkout -b distprover remotes/origin/distprover", rdir=CODE_DIR)
    if (checkout_tag != ""):
      self.run_remote_command("module load git && git checkout %s" % checkout_tag, rdir=CODE_DIR)

  def rename_build_files(self):
    self.run_remote_command("cp GNUmakefile GNUmakefile_local", rdir=CODE_DIR)
    self.run_remote_command("cp GNUmakefile_tacc GNUmakefile", rdir=CODE_DIR)
  
  def build_without_clean(self, cmd):
    self.run_remote_command("""module use %s && 
                            module load python &&
                            module load gmp && 
                            module load ntl &&
                            module load chacha && 
                            module load encrypt &&
                            module load fcgi && 
                            module load libconfig && 
                            module load papi &&
                            module load cuda &&
                            module load cuda_SDK &&
                            module load pbc &&
                            %s""" % (MODULE_DIR, cmd), rdir=CODE_DIR)



  def build(self, cmd):
    self.run_remote_command("""module use %s && 
                            module load python &&
                            module load gmp && 
                            module load ntl &&
                            module load chacha && 
                            module load encrypt &&
                            module load fcgi && 
                            module load libconfig && 
                            module load papi &&
                            module load cuda &&
                            module load cuda_SDK &&
                            module load pbc &&
                            make clean && %s""" % (MODULE_DIR, cmd),
                            rdir=CODE_DIR)


  def generate_job_script(self, opt):
    job_script = self.get_longhorn_job_script(self.expt_name, opt.tasks, opt.nodes,\
                                              opt.time, opt.email,
                                              opt.protocol,
                                              opt.jobtype) \
                  if opt.cluster == "longhorn" else \
                  self.get_lonestar_job_script(self.expt_name, opt.tasks, opt.nodes,\
                                               opt.time, opt.email,
                                               opt.protocol,
                                               opt.jobtype)
    f = open("%s" % SCRIPT_FILE, "w")
    f.write(job_script)
    f.close()
    os.system("scp %s %s@%s:%s%s" % (SCRIPT_FILE, self.user, self.host, \
              CODE_DIR, SCRIPT_FILE))
    os.system("rm %s" % (SCRIPT_FILE))


  def run_job(self):
    self.run_remote_command("""module use %s && 
                            module load gmp && 
                            module load ntl &&
                            module load chacha && 
                            module load encrypt &&
                            module load fcgi && 
                            module load libconfig && 
                            module load papi &&
                            module load cuda &&
                            module load cuda_SDK &&
                            module load pbc &&
                            qsub %s""" % (MODULE_DIR, SCRIPT_FILE),
                            rdir=CODE_DIR)
                            
    while True:
      print "Waiting for job to finish..." + self.computation
      print "input_size is " + self.input_size
      print "sample_index is " + self.sample_index
      
      time.sleep(60)
      self.run_remote_command("qstat | grep %s | awk '{print \$1}' > %s" % \
        (self.user, JOB_ID_FILE), rdir=CODE_DIR)
      os.system("scp %s@%s:%s%s %s" % (self.user, self.host, CODE_DIR, \
                JOB_ID_FILE, os.path.join(self.log_path, JOB_ID_FILE)))
      f = open(os.path.join(self.log_path, JOB_ID_FILE))
      id = f.readlines()
      f.close()
      #os.system("rm %s" % (JOB_ID_FILE))
      if len(id) == 0:
        break
  
      jobid = int(id[0].split("\n")[0])
      self.get_output()
      try:
        f = open(os.path.join(self.log_path, OUTPUT_FILE))
      except IOError:
        print "output file does not exist yet..."
        continue
      if f is not None:
        content = f.read()
        f.close()
        # open again to find the job id
        jobid = 0
        fp = open(os.path.join(self.log_path, OUTPUT_FILE))
        lines = fp.readlines()
        for line in lines:
          line = line.strip()
          if line.find("TACC: Starting up job") != -1:
            words = line.split(); 
            jobid = words[len(words)-1]
        fp.close()
        print "Job id is " + str(jobid)  
        if content.find("p_d_latency") != -1:
          self.run_remote_command("qdel %s" % jobid, rdir=CODE_DIR)
          break
  
    print "Job Finished."


  def get_output(self):
    os.system("scp %s@%s:%s%s %s" % (self.user, self.host, CODE_DIR, \
              OUTPUT_FILE, os.path.join(self.log_path, OUTPUT_FILE)))
  
  def get_code_revision_log(self):
    self.run_remote_command("module load git && (git log | head -n 1) > %s" % CODE_REV_LOG,\
                            rdir=CODE_DIR)
    os.system("scp %s@%s:%s%s %s" % (self.user, self.host, CODE_DIR, \
              CODE_REV_LOG, os.path.join(self.log_path, CODE_REV_LOG)))
  
  
  def write_experiment_config(self, num_nodes, num_threads):
    f = open("%s" % os.path.join(self.log_path, CONFIG_LOG), "w")
    f.write("input_size %s\n" % self.input_size)
    f.write("computation_name %s\n" % self.computation)
    f.write("sample_index %s\n" % self.sample_index)
    f.write("num_comps_in_batch %s\n" % self.batch_size)
    f.write("num_reps %s\n" % NUM_REPS)
    f.write("emulab %s\n" % "emulab-dummy")
    f.write("project %s\n" % PROJECT_NAME)
    f.write("experiment %s\n" % self.expt_name)
    f.write("variant %s\n" % self.protocol)
    f.write("num_threads %s\n" % num_threads)
    f.write("num_prover_machines %s\n" % (int(num_nodes) - 1))
    f.close()
  
  
  def analyze_output(self):
    os.system("./analyze.pl --log_path=%s --rslt_path=%s" % (self.log_path, self.rslt_path))
  
  def clean(self):
    self.run_remote_command("rm -rf %s" % REPO_DIR)
    self.run_remote_command("rm -rf %s/computation_state/*" % SCRATCH_DIR)
  
  def delete_job_file(self):
    self.run_remote_command("rm %s/%s" % (CODE_DIR,OUTPUT_FILE)) 

def run_local_command(cmd):
  print "Executing :", cmd
  os.system(cmd)

def main():
  parser = OptionParser()
  parser.add_option("-c", "--cluster", dest="cluster")
  #parser.add_option("-o", "--outputfile", dest="out")
  parser.add_option("-t", "--taskspernode", dest="tasks")
  parser.add_option("-m", "--max_provers", dest="maxprovers")
  parser.add_option("-n", "--numnodes", dest="nodes")
  parser.add_option("-d", "--time", dest="time", help="in the form of hh::mm:ss")
  parser.add_option("-e", "--email", dest="email")
  parser.add_option("-z", "--jobtype", dest="jobtype")
  parser.add_option("-s", "--sample_index", dest="sampleindex")
  parser.add_option("-l", "--log_path", dest="logpath")
  parser.add_option("-r", "--rslt_path", dest="rsltpath")
  parser.add_option("-p", "--computation", dest="computation")
  parser.add_option("-b", "--batch_size", dest="batchsize")
  parser.add_option("-i", "--input_size", dest="inputsize")
  parser.add_option("--protocol", dest="protocol", default="zaatar")
  parser.add_option("-g", "--git_tag", dest="gittag", default="")
  parser.add_option("--clone_remote", dest="clone_remote", default="1")
  parser.add_option("--rebuild_local", dest="rebuild_local", default="1")
  
  (opt, args) = parser.parse_args()

  if opt.cluster != "longhorn" and opt.cluster != "lonestar":
    print "Cluster should be either longhorn or lonestar."
    return 1
  host = opt.cluster + ".tacc.utexas.edu"

  build_command = BUILD_COMMAND 
  if (opt.cluster == "lonestar"):
    build_command = BUILD_COMMAND + " USE_GPU=1"

  build_command = build_command + " NUM_PROVERS_MAX=" + opt.maxprovers 

  global HOME_DIR
  global REPO_DIR
  global CODE_DIR
  global BIN_DIR
  global MODULE_DIR
  global PKG_DIR

  if (opt.cluster == "lonestar"):
    HOME_DIR = "/home1/01934/srinath/"
  else:
    HOME_DIR = "/home/01934/srinath/"
  
  REPO_DIR = HOME_DIR + REPO_FOLDER
  CODE_DIR = REPO_DIR + "vercomp/code/pepper/"
  BIN_DIR = CODE_DIR + "bin/"
  MODULE_DIR = HOME_DIR + "modulefiles"
  PKG_DIR = HOME_DIR + "pkg"

  num_nodes = opt.nodes
  num_threads = 4 #if opt.cluster == "longhorn" else 6
  checkout_tag = opt.gittag

  framework = "GINGER"
  if (opt.protocol == "zaatar" or opt.protocol == "batch-cmtpp"):
    framework = "ZAATAR"
 
  #pass empty string if protocol is cmt or cmtpp
  # overwrite SFDL file with input size provided by the script
  # assumes that input is defined in line "const m = <>;"

  sfdl_computation = "mm_sfdl";
  if (opt.protocol == "zaatar" or opt.protocol == "ginger" or opt.protocol == "batch-cmtpp"):
    sfdl_computation = opt.computation
    
    file_path_sfdl = "../pepper/apps_sfdl/" + opt.computation + ".sfdl"
    fp = open(file_path_sfdl, "r")
    lines = fp.read()
    fp.close()
    
    search_line = ""
    replace_line = ""

    if (opt.computation == "fannkuch"):
      search_line = 'const L = \d+;'
      replace_line = "const L = " + opt.inputsize + ";" 
    else:
      search_line = 'const m = \d+;'
      replace_line = "const m = " + opt.inputsize + ";"
    
    lines = re.sub(search_line, replace_line, lines)
    fp = open(file_path_sfdl, "w")
    fp.write(lines)
    fp.close();
 
    if (opt.computation == "dna_align"):
      file_path_sfdl = "../pepper/apps_sfdl/" + opt.computation + ".sfdl"
      fp = open(file_path_sfdl, "r")
      lines = fp.read()
      fp.close()
      search_line = 'const n = \d+;'
      replace_line = "const n = " + opt.inputsize + ";"
      lines = re.sub(search_line, replace_line, lines)
      fp = open(file_path_sfdl, "w")
      fp.write(lines)
      fp.close();

  build_command = build_command + " FRAMEWORK=" + framework  + " SFDL_FILES=" + sfdl_computation 

  exp = Experiment(USERNAME, host, opt.sampleindex, opt.computation,\
		   opt.batchsize, opt.inputsize, opt.logpath, opt.rsltpath, opt.protocol)

  if (opt.clone_remote == "1"):
    exp.clean()
    exp.clone_repo(checkout_tag)
    exp.rename_build_files()
 
  exp.delete_job_file()
  
  if (opt.rebuild_local == "1"):
    command = "cd ../pepper/; make clean;" + build_command + "; cd"
    run_local_command(command)
    
  # copy files to remote
  folders_to_create = ["bin", "apps_sfdl", "apps_sfdl_gen", "apps_sfdl_hw"]
  for folder in folders_to_create:
    remote_dir = CODE_DIR + folder + "/";
    exp.create_remote_dir(remote_dir)
    
  remote_path = host + ":"
  files_to_copy = ["GNUmakefile", "GNUmakefile_tacc", "bin/*qap", "bin/*pws", "bin/*gamma*", "bin/*f1index", "apps_sfdl/*", "apps_sfdl_gen/*", "apps_sfdl_hw/*", "crypto/*", "libv/*", "common/*", "apps_tailored/*"]
  for file_to_copy in files_to_copy:
    pieces = file_to_copy.split("/");
    command = "scp ../pepper/" + file_to_copy + " " + remote_path + CODE_DIR + pieces[0]
    run_local_command(command)
  
  exp.rename_build_files()
  exp.build_without_clean(build_command)
  exp.generate_job_script(opt)
  exp.run_job()
  exp.get_output()
  exp.get_code_revision_log()
  exp.write_experiment_config(num_nodes, num_threads)
  exp.analyze_output()

if __name__ == "__main__":
  main()
