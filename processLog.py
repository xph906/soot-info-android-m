import sys, os, re
from operator import itemgetter

# Configuration & Global Variables
filePrefix = "out_"
fileSuffix = ".txt"
AllFlows = {} # FlowKey -> [Flow]
AppFlows = {} # appName -> [Flow]

#- $r1 := @parameter0: android.view.View (in <com.hipxel.custom.soundboard.AOPropertiesViewMaker$DFields$2: void onClick(android.view.View)>)

SinkPattern = re.compile("Found a flow to sink .+<(.+): (.+), from the following sources:")
SourcePattern1 = re.compile(\
	'- \$[a-z]\d+ = \w+ .+<(.+): (.+)>.+ \(in <(.+): (.+)>\)')
SourcePattern2 = re.compile(\
	'- \$[a-z]\d+ := (.+): (.+) \(in <(.+): (.+)>\)')
class Flow:
	def __init__(self, sink, source, sinkKey, sourceKey, origin):
		self.sink = sink.strip()
		self.source = source.strip()
		self.origin = origin
		self.sinkKey = sinkKey
		self.sourceKey = sourceKey

	def getKey(self):
		return "%s=>%s" %(self.sourceKey, self.sinkKey)

	def toString(self):
		originStr = self.origin
		if self.origin == None:
			originStr = "NoAppInfo"
		line1 = "%s=>%s [%s]\n" \
			%(self.sourceKey, self.sinkKey, originStr)
		line2 = "  %s => %s" %(self.source, self.sink)

def processLogFile(fname, appName):
	if appName in AppFlows:
		print "%s has been processed with %d unique flows"\
		 %(appName, len(AppFlows[appName]))
		return

	f = open(fname)
	myFlows = {}
	sink = None
	sinkTag = None
	for line in f:
		line = line.strip()
		sinkMatcher = SinkPattern.match(line)
		if sinkMatcher != None:
			curSinkClass = sinkMatcher.group(1).strip()
			curSinkMethod = sinkMatcher.group(2).strip()
			curSinkMethod = re.sub("\$[a-z]\d+", 'arg', curSinkMethod)
			index = curSinkMethod.rfind('>')
			if index > 0:
				curSinkMethod = curSinkMethod[:index]+':'+curSinkMethod[index+1:]
			curSink = curSinkMethod+' @'+curSinkClass
			sink = curSink
			sinkTag = curSinkMethod.split(':')[0]+' @'+curSinkClass
			#print sink
		elif sink != None:
			sourceMatcher = SourcePattern1.match(line)
			if sourceMatcher == None:
				sourceMatcher = SourcePattern2.match(line)
			if sourceMatcher == None:
				continue

			callClass = sourceMatcher.group(3)
			if not callClass.startswith('android'):
				callClass = callClass.split('.')[-1]

			sourceTag = ""
			source = ""
			if sourceMatcher.group(1).startswith("@parameter"):
				sourceTag = sourceMatcher.group(4)+'('+sourceMatcher.group(1)+')'
				source = sourceTag +' @'+callClass
			else:
				sourceMethodClass = sourceMatcher.group(1)
				if not sourceMethodClass.startswith("android"):
					sourceMethodClass = sourceMethodClass.split('.')[-1] 
				sourceTag = sourceMethodClass+'::'+sourceMatcher.group(2)
				source = sourceTag + ' @'+callClass+"::"+sourceMatcher.group(4)

			flow = Flow(sink, source, sinkTag, sourceTag, appName)
			
			#store flow
			if not flow.getKey() in AllFlows:
				AllFlows[flow.getKey()] = [flow]
			else:
				AllFlows[flow.getKey()].append(flow)

			if not appName in AppFlows:
				AppFlows[appName] = [flow]
			else:
				AppFlows[appName].append(flow)
	#print "source: ",str(SourceCount)

def processLogDirectory(dirPath):
	filenames = []
	for n in os.listdir(dirPath):
		if n.startswith(filePrefix) and \
			n.endswith(fileSuffix):
			filenames.append(n)

	fullFileNames = [os.path.join(dirPath, name) for name in filenames]
	print "start processing %d files" %len(fullFileNames)

	for name in fullFileNames:
		try:
			appName = os.path.basename(name)[len(filePrefix):-len(fileSuffix)]
			processLogFile(name, appName)
		except Exception as e:
			print "error processing %s for %s" %(name, str(e))

	count = 0
	for flowKey in AllFlows:
		count += len(AllFlows[flowKey])
	print "done processing %d(%d) flows" %(count, len(AllFlows))

	for key in sorted(AllFlows, key=lambda k: len(AllFlows[k]), reverse=True):
		print key, str(len(AllFlows[key]))

processLogDirectory(sys.argv[1])	
