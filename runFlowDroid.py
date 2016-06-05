from multiprocessing import Process
import threading,argparse,subprocess,logging,Queue,os,time,sys
import psutil,json

# Configuration
configFileName="runFlowDroid.config"

# Set Logger
logger = logging.getLogger('RunFlowDroid')
hdlr = logging.FileHandler('./runflowdroid.log')
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
hdlr.setFormatter(formatter)
consoleHandler = logging.StreamHandler()
consoleHandler.setFormatter(formatter)
logger.addHandler(hdlr) 
logger.addHandler(consoleHandler)
logger.setLevel(logging.DEBUG)

def killProcess(pid):
  try:
    cmd = ['kill','-9','']
    cmd[2] = str(pid)
    subprocess.Popen(cmd)
  except Exception as e:
    logger.error("killProcess exception %s" % str(e))
    return

def killProcessAndChildProcesses(parent_pid):
  try:
    p = psutil.Process(parent_pid)
    child_pid = p.get_children(recursive=True)
    for pid in child_pid:
      killProcess(pid.pid)
    killProcess(parent_pid)
  except Exception as e:
    logger.error("killProcessAndChildProcesses exception %s" % str(e))
    return

def runFlowDroid(appList):
  # read configure file
  with open(configFileName, 'r') as f:
    config = json.load(f)
  javaMem = "-Xmx%dg" %config['javamem']
  flowdroidargs = None
  if len(config['flowdroidargs'].strip()) != 0:
    flowdroidargs = config['flowdroidargs'].split()

  appListFile = open(appList)
  for app in appListFile:
    app = app.strip().lower()
    bname = os.path.basename(app)
    if app.startswith('#') or \
      len(app) == 0:
      continue
    try:
      args = ['java',javaMem, '-cp', '.:soot-trunk.jar:soot-infoflow.jar:soot-infoflow-android.jar:slf5j-api-1.7.5.jar:slf4j-simple-1.7.5.jar:axml-2.0.jar', 'soot.jimple.infoflow.android.TestApps.Test']
      args += [app, str(config['platformpath'])]
      #args = ['java', '-cp', './javaExample/', 'SleepMessages']
      if flowdroidargs != None:
        args += [str(arg) for arg in flowdroidargs]
      logger.debug("command: "+str(args))
      worker = subprocess.Popen( \
        args, 
        stdout=open(os.path.join(str(config['logdir']), \
          'out_%s.txt'%bname), 'w'),
        stderr=open(os.path.join(str(config['logdir']), \
          'err_%s.txt'%bname), 'w'))
      pid = worker.pid
      #detect
      logger.debug("[start] processing %s[pid:%d]" %(bname, pid) )
      startingTime = time.time()
      code = None
      while int(time.time()-startingTime) < int(config['timeout']):
        code = worker.poll()
        if code != None:
          logger.debug("[finish] processing %s[pid:%d] code:%s" \
            %(bname, pid, str(code)))
          break
        else:
          logger.debug("[wait] has been working process %d for %d seconds..." \
            %(pid, int(time.time()-startingTime)) )
          time.sleep(5)
      # timeout
      if code == None:
        logger.debug("[timeout] kill process %d" %pid)
        killProcessAndChildProcesses(pid)
    except Exception as e:
      logger.error("runFlowDroid: "+str(e))

def main():
  if len(sys.argv) != 2:
    logger.error("usage: python runFlowDroid.py appList")
    sys.exit(1)
  runFlowDroid(sys.argv[1])

if __name__ == "__main__":
  main()