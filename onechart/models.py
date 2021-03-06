from django.contrib.auth.models import User
from django.db import models
from myutil import idtool
from myutil.dateutil import getTime
from onechart import mongo
from userena.models import UserenaLanguageBaseProfile
import logging
import re
import time
logger = logging.getLogger(__name__)


CLEANUP_PATTERN = re.compile(r'[()\s]')
class BaseModel(dict):
    
    @classmethod
    def findOne(cls, query):
        col = mongo.getCollection(cls._col)
        r = col.find_one(query)
        if(r): return cls(r)
        return None
    
    def __init__(self, data=None):
        if(data):
            if(isinstance(data, dict)):
                for d in data: self.__setattr__(d, data[d])
            else:
                raise Exception("Invalid initialization data: %s" %data)
            #self.update(data)
        else:
            self.create_tm = getTime()
            self.tm = time.time()
        #self.id = self._id if self._id else None
        if not self._id: self._id= idtool.generate(self._col)
        
            
    def __getattr__(self, attr):
        if(attr == 'id'): attr = '_id'
        return self[attr] if attr in self else None

    def __setattr__(self, attr, value):
        # any keys starts with _ is considered transient, non-persistable
        if(attr[0] == "_" and attr != '_id'):
            #print "Setting attr: %s=%s" %(attr,value)
            self.__dict__[attr] = value
            return
        #print "Setting key: %s=%s" %(attr,value)
        if(attr == 'id'): attr = '_id'
        self[attr] = value

    def save(self):
        if self.validate: self.validate()
        if self.beforeSave: self.beforeSave()
        
        col = mongo.getCollection(self._col)
                
        self._id = self.cleanup_id(self._id)
        
        logger.debug("Persisting %s: %s" %(self._col, self._id))
        self.create_tm = self.create_tm or getTime()
        self.update_tm =  getTime()
        self.tm = self.tm or time.time()
        
        col.save(self, safe=True)
        
        #logger.debug("Done")
        
        if self.afterSave: self.afterSave()
            
    def cleanup_id(self, id):
        return CLEANUP_PATTERN.sub('', id).lower()
    
    
        
    def PK(self):
        """
        Helper method to return a Mongo query based on primary key
        """
        return {'_id': self._id}

class BioModel(BaseModel):    
    def __init__(self, data=None):
        BaseModel.__init__(self, data)
        

class Network(BioModel):
    """
    Primary id: ntwk_DB_DBID  e.g.e, ntwk_intact_1039473
    
    When data is passed in from client, it may contains:
        _connections: list of connections need to be saved
        _nodes: list of nodes (may) need to be saved
        
    
    """
    _col = 'network'
    def __init__(self, data=None):
        BioModel.__init__(self, data)
            
        self.name = self.name or ''
        self.refs = self.refs or {}
        self.visibility = self.visibility or 'private'
        self.owner = self.owner or ''
    
        # Non persistent field
        self._connections = self._connections or []
    
        self.init()
    def init(self):
        
        # convert json to model object
        if(self._connections):
            logger.debug("convert connection objects %d" %len(self._connections))  
            for i in range( len( self._connections)):
                con =self._connections[i]                
                if(isinstance(con, Connection)): continue
                con = Connection(con)
                con.network = con.network or self._id
                self._connections[i] = con                
            
        if(self._nodes):
            logger.debug("convert node objects %d" %len(self._nodes))  
            for i in range( len( self._nodes)):
                node =self._nodes[i]                
                if(isinstance(node, Node)): continue
                node = Node(node)
                node.network = node.network or self._id
                self._nodes[i] = node
        
    def beforeSave(self):
        #self.con_count = len(self._connections)
        pass


    def afterSave(self):
        logger.debug("Performing network specific saving")
        for con in self._connections:
            con.save()
        for node in self._nodes:
            node.save()        

class Node(BioModel):
    _col="node"
    def __init__(self, data=None, entity=None):
        BioModel.__init__(self, data)
        if(entity):
            self.entity = entity._id            
            self.label = self.label or entity.label or entity.name
            self.group = self.group or entity.group
            
            self._entity = entity 
        self.role = self.role or ''
    
    def validate(self):
        
        if not self.entity:
            raise Exception("Entity is required")
        if not self.label:
            raise Exception("Label is required")
        if not self.network:
            raise Exception("Node must belong at least one network")
        
    def afterSave(self):
        logger.debug("Performing Node specific saving")
        if self._entity: self._entity.save()
        
class Entity(BioModel):
    """
    Primary id: 
        enti_up_xxxxx   (for genes, uisng unioric number)
        
    """
    _col='entity'
    def __init__(self, data=None):
        BioModel.__init__(self, data)
        
        self.group = self.group or ''  # protein, gene etc
        self.cats = self.cats or {}  #  dictionary of categories organism: human 
        self.label=self.label or '' # short label
        self.name= self.name or '' # full name
        self.alias = self.alias or []  # list of aliases
        self.dbref = self.dbref or {}  # DB reference, such as UniProcKB or CHEBI etc
        self.refs = self.refs or {}
        
        # Non persisten fields        
        
        
class Connection(BioModel):
    """
    Primary id: 
        conn_   (for genes, uisng unioric number)
        
    """
    _col="connection"
    def __init__(self, data=None):
        BioModel.__init__(self, data)
            
        
        """
        Arbitrary References
            pubmed: pubmed id, i.e., 15102471            
            intact: intact id, i.e., EBI-2433438
        """
        self.refs = self.refs or  {}
        """
        Wrapper over physical entities
        """
        self.nodes = self.nodes or []
        self.type  = self.type or ''
        
        self.visibility = self.visibility or 'private'
                
        """
        entity objects
        """
        self._entities = self._entities or []        
        self._nodes = self._nodes or []        

    def validate(self):
        if((not self.nodes) or (not self.entities)):
            raise Exception("Missing nodes/entities")
        if(len( self.nodes) != len(self.entities)):
            raise Exception("Number of nodes and entities in a connection should agree")
        if(len(self.nodes) < 2 or (not (self.nodes[0] and self.nodes[1] )  )):
            raise Exception("At least 2 nodes are required")
        if not self.network:
            raise Exception("Missing required field: network")
    def afterSave(self):
        logger.debug("Performing Connection specific saving")
        for node in self._nodes:
            node.save()
      
class Publication(BioModel):
    """
    Primary id: 
        publ_           
    """    
    _col="publication"    
    def __init__(self, data=None):
        BioModel.__init__(self, data)
        
        """
        Arbitrary References
            pubmed: pubmed id, i.e., 15102471            
            intact: intact id, i.e., EBI-2433438
        """
        self.refs = self.refs or  {}
        self.pubmed_id = self.pubmed_id or ''
        self.authors = self.authors or []
        self.abstract = self.abstract or  ''
        self.entities = self.entities or []  # { id:'', name:'', group:''}

class Association(BaseModel):
    _col = 'association'
    _index=['name']
    def __init__(self, data=None):
        BaseModel.__init__(self, data)
        
        self.name= self.name or ''
        self.group = self.group or ''
        if self.name: self._id = 'asso_%s' %self.name

class Tagging(BaseModel):
    _col = 'tagging'
    _index =['name', 'owner']
    def __init__(self, data=None):
        BaseModel.__init__(self, data)
        
        self.name = self.name or ''
        self.owner =self.owner or ''
        self.type = self.type   # type: network, links, nodes, pubs anything
        self.builtin = self.builtin or 0
        self.ids = self.ids or [] 
        
    def validate(self):
        if not self.name:            
            raise Exception("Missing required field: name")
        
        
class Experiment(BioModel):
    pass

class People(BioModel):    
    """
    Primary id: 
        peopXXXX           
    """    
    _col="people"    
    def __init__(self, data=None):
        BioModel.__init__(self, data)
        self.first = self.first or ''
        self.last =self.last or ''
        self.middle = self.middle or ''
        
        
        
class PreconProfile(UserenaLanguageBaseProfile ):
    user = models.OneToOneField(User,
                                unique=True,
                                verbose_name= 'user',
                                related_name='my_profile')
    favourite_snack = models.CharField( 'favourite snack' ,
                                       max_length=5)
prefix_mapping =  {'netw':'network' , 'ntwk':'network', 'enti':'entity', 'node':'node', 'conn': 'connection', 'publ':'publication'}
model_label_mapping =  {'network':'Study' ,  'entity':'Entity', 'node':'Node', 'connection': 'Link', 'publication':'Publication'}


