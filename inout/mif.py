from copy import copy
from models import BaseModel
from myutil import idtool
from onechart import mongo
from onechart.models import Connection, Entity, Network, Node
from xml.dom.minidom import parse, parseString
import os
import time


def importmif():
	dups = {}	
	basedir = "data/IntAct/psi25/datasets"
	cats = os.listdir(basedir)
			
	networks = []
	entities = []
	connections = []
	nodes = [] 	
	
	for c in cats:
		if c != 'Parkinsons': continue
		print "Processing category %s" %c
		files = os.listdir("%s\\%s" %(basedir,c) )
		for filename in files:
			file = "%s\\%s\\%s" %(basedir, c, filename)
			if os.path.isdir(file): continue
			log( "Processing %s" %file)
			res = Network()
			res.group = c 
			res.refs = {}
			res.connections = []
			res.entities = []
			res.refs['intact'] = filename.replace(".xml", "")
			parseFile(file, res)
			
			if res._id in dups:
				error("Duplicated id: %s/%s"%(c, file))
				continue
			networks.append(res)
			dups[res._id ] = 1			
			if res.entities: entities.extend(res.entities)
			
			connections.extend(res.connections)
			tmp_nodes = []
			for con in res.connections:
				if con.nodes: tmp_nodes.extend(con.nodes)
			nodes.extend(tmp_nodes)
			
			log("Connections: %d  Participants %d Interactors: %d" %(len(res.connections), len(tmp_nodes), len(res.entities) ))
				#interactors.extend(a)
			#interactions.extend(b)
			#log("interactors : %d" % len(res.entities))
			#log("interactions: %d" % len(res.entities))
			#break
	
	#log( "Total interactions: %d" % len(interactions))
	
	nc = mongo.getCollection('network')
	ec = mongo.getCollection('entity')
	cc = mongo.getCollection('connection')
	nodec=mongo.getCollection('node')
	
	for con in connections:
		node_ids = []
		con.entities = []
		for node in con.nodes:			
			ent_id = ''
			if node.refs and node.refs['entity']:
				# node.entity is IntAct internal ID
				intact_id = node.refs['entity']				
				for item in entities:
					if item.refs and item.refs['intact'] == intact_id:						
						ent_id = item._id												
						break				
			if not ent_id:
				error("Unresolved interactorRef for %s" %node)						
			else:
				node.entity = ent_id
				node_ids.append(node._id)										
				con.entities.append(ent_id)										
		con.nodes = node_ids		
		
	for con in connections:
		cc.insert(con, safe=True)
		log("Saved connection %s" %con._id)
	
	
	for network in networks:
		del network['entities']
		del network['connections']
		nc.insert(network, safe=True)
		log("Saved network %s" %network._id)
	
	
	for node in nodes:
		if not node.entity: continue
		nodec.insert(node, safe=True)
		log("Saved node %s" %node._id)
	
	dups = []
	for entity in entities:
		if entity._id in dups: continue
		ec.insert(entity, safe=True)
		dups.append(entity._id)
		log("Saved entity %s" %entity._id)
	
	log( "###########################")
	log( "Total networks: %d" % len(networks))
	log( "Total interactors: %d" % len(entities))
	log( "Total nodes: %d" %(len(nodes)))
	
	log("Done")
	return networks

def lookupByRefs(obj):
	pass
			
def getDom(filename = None):
	filename =  filename or "data/IntAct/psi25/datasets/Parkinsons/15983381.xml"
	with open(filename) as f:
		content = f.read()		
	dom = parseString(content)
	return dom

def parseFile(filename, res):
	with open(filename) as f:
		content = f.read()		
	dom = parseString(content)
	
	# one file contains multiple entries, each entry is a network
	parseNetworks(dom, res)
	
	parseInteractors(dom,res)
	
	parseInteractions(dom, res)
	
	"""
	sets = dom1.getElementsByTagName("set")
	for set in sets:
		spec = set.getElementsByTagName("setSpec")
		specName = set.getElementsByTagName("setName")
		try:						
			#setNames.append(specName)
			print "%s: %s" %(spec[0].firstChild.nodeValue, specName[0].firstChild.nodeValue.strip())
			setSpecs.append("%s: %s" %(spec[0].firstChild.nodeValue, specName[0].firstChild.nodeValue.strip()))
		except:
			continue
	
	resumeToken= dom1.getElementsByTagName("resumptionToken")
	if not resumeToken: break
	token = resumeToken[0].firstChild.nodeValue.strip()
	token =  "&resumptionToken="+token		
		
	
	fd = open("ukpmc-all-sets.txt", "w")
	for s in setSpecs:
		try:  
			fd.write("%s\n" %s)
		except: 
			pass
	fd.close()
	return setSpec
	"""	
def parseNetworks(dom, network):
	"""
	entrySet/entry
	
	<experimentList>
	        <experimentDescription id="412916">
	            <names>
	                <shortLabel>junn-2005-1</shortLabel>
	                <fullName>Interaction of DJ-1 with Daxx inhibits apoptosis signal-regulating kinase 1 activity and cell death.</fullName>
	"""
	expTag = dom.getElementsByTagName('experimentDescription')[0]	               
	network.name = getText(expTag, "names", "fullName")		 
	setPrimaryRef(expTag, network)
	# TBD: we probably should list experiments
	#network.refs['intact'] = network.refs['pubmed']
	network._id = "ntwk_intact_%s" %network.refs['intact']
	network.source = "intact"
	# lookup by reference
	
			
	#for p in  participants:
def getFirstTag(node, name):
	ems = node.getElementsByTagName(name)
	return ems[0] if ems and len(ems)>0 else None		
def parseInteractions(dom,network):
	"""
	 <interaction id="678614" imexId="IM-12113-2">
                <names>
                    <shortLabel>pf1130-pf1128-2</shortLabel>
                    <fullName>Affinity purification by hydroxy apatite chromatography</fullName>
                </names>
                <xref>
                    <primaryRef refTypeAc="MI:0356" refType="identity" id="EBI-2507294" dbAc="MI:0469" db="intact"/>
                    <secondaryRef refType="imex source" id="MI:0469" dbAc="MI:0488" db="psi-mi"/>
                    <secondaryRef refTypeAc="MI:0662" refType="imex-primary" id="IM-12113-2" dbAc="MI:0670" db="imex"/>
                </xref>
                <experimentList>
                    <experimentRef>678502</experimentRef>
                </experimentList>
                <participantList>
                    <participant id="678615">
                        <names>
                            <shortLabel>n/a</shortLabel>
                        </names>
                        <xref>
                            <primaryRef refTypeAc="MI:0356" refType="identity" id="EBI-2507299" dbAc="MI:0469" db="intact"/>
                        </xref>
                        <interactorRef>678518</interactorRef>
                        <biologicalRole>
                            <names>
                                <shortLabel>unspecified role</shortLabel>
                                <fullName>unspecified role</fullName>
                            </names>
                            <xref>
                                <primaryRef refTypeAc="MI:0356" refType="identity" id="MI:0499" dbAc="MI:0488" db="psi-mi"/>
                                <secondaryRef refTypeAc="MI:0356" refType="identity" id="EBI-77781" dbAc="MI:0469" db="intact"/>
                                <secondaryRef refTypeAc="MI:0358" refType="primary-reference" id="14755292" dbAc="MI:0446" db="pubmed"/>
                            </xref>
                        </biologicalRole>
                        <experimentalRoleList>
                            <experimentalRole>
                                <names>
                                    <shortLabel>neutral component</shortLabel>
                                    <fullName>neutral component</fullName>
                                </names>
                                <xref>
                                    <primaryRef refTypeAc="MI:0356" refType="identity" id="MI:0497" dbAc="MI:0488" db="psi-mi"/>
                                    <secondaryRef refTypeAc="MI:0356" refType="identity" id="EBI-55" dbAc="MI:0469" db="intact"/>
                                    <secondaryRef refTypeAc="MI:0358" refType="primary-reference" id="14755292" dbAc="MI:0446" db="pubmed"/>
                                </xref>
                            </experimentalRole>
                        </experimentalRoleList>
                    </participant>
	"""
	ems = dom.getElementsByTagName("interaction")	
	connections  = []
	nodeCount = 0	
	for em in ems:
		#interactions.append(interaction)		
		participants =  em.getElementsByTagName("participant")
		con_nodes = []
		for p in  participants:
			node = Node()
			node.network = network._id
			nodeCount+=1
			node._id = "node%s_%d" %(node.network[4:], nodeCount)
			node.role = getText(p, "biologicalRole", "names", "shortLabel")
			#node.refs['intact'] = p.getAttribute("id")
			setPrimaryRef(p, node)			
			ref = getText(p, 'interactorRef')
			if ref: node.refs['entity'] = ref  # temporary
			con_nodes.append(node)
		
		connection = Connection()
		connection._id = "conn_intact_%s" % em.getAttribute("id")
		connection.refs={}
		setPrimaryRef(em, connection)
		connection.type=getText( em.getElementsByTagName("interactionType")[0], 'shortLabel')		
		connection.label = getText(em,'shortLabel')
		connection.network = network._id
		
		connection.nodes = con_nodes
		connections.append(connection)
				
	network.connections = connections
		
def findInteractorByPsiId(interactors, psi_id):
	for i in interactors:
		if i.psi_id == psi_id: return i
	return None

def parseInteractors(dom, network):
	ems = dom.getElementsByTagName("interactor")
	
	interactors = []
	#print "interactor tags %d " %len(ems)
	for em in ems:
		interactor = Entity()
		tmp = em.getElementsByTagName("shortLabel")
		slabel = tmp[0].firstChild.nodeValue.strip()
		slabel = slabel.split('_')[0]
		
		interactor.symbol = slabel		
		interactor.refs = {}		
		
		interactor.name =  getText(em, 'fullName')
		setPrimaryRef(em, interactor)
		if 'uniprotkb' in interactor.refs:
			interactor._id = "enti_up_%s" %(interactor.refs['uniprotkb'])
		else:
			interactor._id = idtool.generate("entity")	
		interactor.refs['intact'] = em.getAttribute('id')
		interactor.group = getText( em.getElementsByTagName("interactorType")[0], 'shortLabel')
		interactors.append(interactor)

	network.entities =  interactors


def setPrimaryRef(em, obj):
	pr = getFirstTag(em, "primaryRef")
	if(pr):
		obj.refs = obj.refs or {}
		obj.refs[ pr.getAttribute("db") ] =  pr.getAttribute("id")
	
def getText(node, *args):
	
	for arg in args:
		tmp = node.getElementsByTagName(arg)
		if tmp: node = tmp[0]
	return node.firstChild.nodeValue.strip()
		
	
			
def error(msg):
	log( "ERROR: %s" %msg)			
	
	
	"""
	 self._id = self._id or '' 
        self.cat = self.cat or ''  # protein, gene etc
        self.org = self.org or ''  # organism: human 
        self.label=self.label or '' # short label
        self.name= self.name or '' # full name
        self.alias = self.alias or []  # list of aliases
        self.uniproc_id = self.uniproc_id or ''  # uniproc id
        self.psi_id = self.psi_id or ''
	 <interactor id="678547">
	    <names>
	        <shortLabel>if5a_pyrfu</shortLabel>
	        <fullName>Translation initiation factor 5A</fullName>
	        <alias type="gene name" typeAc="MI:0301">eif5a</alias>
	        <alias type="gene name synonym" typeAc="MI:0302">eIF-5A</alias>
	        <alias type="gene name synonym" typeAc="MI:0302">Hypusine-containing protein</alias>
	        <alias type="locus name" typeAc="MI:0305">PF1264</alias>
	    </names>
	    <xref>
	        <primaryRef refTypeAc="MI:0356" refType="identity" version="SP_45" id="Q8U1E4" dbAc="MI:0486" db="uniprotkb"/>
	      
	    </xref>
	    <interactorType>
	        <names>
	            <shortLabel>protein</shortLabel>
	            <fullName>protein</fullName>
	        </names>
	        <xref>
	            <primaryRef refTypeAc="MI:0356" refType="identity" id="MI:0326" dbAc="MI:0488" db="psi-mi"/>
	            <secondaryRef refTypeAc="MI:0356" refType="identity" id="EBI-619654" dbAc="MI:0469" db="intact"/>
	            <secondaryRef refTypeAc="MI:0358" refType="primary-reference" id="14755292" dbAc="MI:0446" db="pubmed"/>
	            <secondaryRef refTypeAc="MI:0361" refType="see-also" id="SO:0000358" dbAc="MI:0486" db="uniprotkb"/>
	        </xref>
	    </interactorType>
	    <organism ncbiTaxId="186497">
	        <names>
	            <shortLabel>pyrfu</shortLabel>
	            <fullName>Pyrococcus furiosus</fullName>
	        </names>
	    </organism>
	</interactor>
	<interactor id="678544">
	"""
#writeFile("ukpmc-all-sets.txt", "\n".join(setSpecs))

"""	
<set>
<setSpec>aabc</setSpec>
<setName>
Advances and Applications in Bioinformatics and Chemistry : AABC
</setName>
"""


def filesInDir(path, fullpath=False, includeHiddenFiles=False):
	try: files = os.listdir(path)
	except: return []
	if not includeHiddenFiles: files = [name for name in files if (name and name[0] != '.')]
	if not fullpath: return files
	else: return ['%s/%s' % (path, name) for name in files]

def log(msg):	
	msg = time.strftime("%H:%M:%S ") + msg +"\n"
	print msg
	with open("import.log", "a") as f:
		f.write(msg) 




"""

psi25 / datasets 
			disease_types/
							AFCS/pubmed_id.xml
							Alzheimers
		pmid
			year/2003
				/2004
			
		species/
					
		
"""		