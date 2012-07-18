/**
 * This file is part of GeneMANIA.
 * Copyright (C) 2010 University of Toronto.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.genemania.engine.apps;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.SimpleLayout;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.store.FSDirectory;
import org.genemania.domain.InteractionNetwork;
import org.genemania.domain.InteractionNetworkGroup;
import org.genemania.domain.NetworkMetadata;
import org.genemania.domain.Organism;
import org.genemania.engine.apps.support.DataConnector;
import org.genemania.engine.cache.DataCache;
import org.genemania.engine.cache.FileSerializedObjectCache;
import org.genemania.engine.cache.IObjectCache;
import org.genemania.engine.cache.MemObjectCache;
import org.genemania.engine.cache.SynchronizedObjectCache;
import org.genemania.exception.ApplicationException;
import org.genemania.exception.DataStoreException;
import org.genemania.mediator.GeneMediator;
import org.genemania.mediator.NetworkMediator;
import org.genemania.mediator.NodeMediator;
import org.genemania.mediator.OntologyMediator;
import org.genemania.mediator.OrganismMediator;
import org.genemania.mediator.StatsMediator;
import org.genemania.mediator.lucene.LuceneGeneMediator;
import org.genemania.mediator.lucene.LuceneMediator;
import org.genemania.mediator.lucene.LuceneNetworkMediator;
import org.genemania.mediator.lucene.LuceneNodeMediator;
import org.genemania.mediator.lucene.LuceneOntologyMediator;
import org.genemania.mediator.lucene.LuceneOrganismMediator;
import org.genemania.mediator.lucene.LuceneStatsMediator;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Common code used by several engine-related command-line applications,
 * mostly involved with data preparation. covers domain model access (hibernate
 * or lucene), network_cache, and logging. see sample main() implemented here
 * for usage example.
 */
public abstract class AbstractEngineApp {
    private static Logger logger = Logger.getLogger(AbstractEngineApp.class);

    @Option(name = "-cachedir", usage = "location of cache directory")	
    private String cacheDir;
    @Option(name = "-indexDir", usage = "location of lucene indices")
    String indexDir;
    @Option(name = "-log", usage = "name of processing log file to create (will truncate old file)")
    String logFilename;

    DataConnector dataConnector;
    OrganismMediator organismMediator;
    NetworkMediator networkMediator;
    GeneMediator geneMediator;
    NodeMediator nodeMediator;
    OntologyMediator ontologyMediator;
    StatsMediator statsMediator;
    DataCache cache;

	protected static Searcher createSearcher(String indexPath) throws IOException {
		ArrayList<Searcher> searchers = new ArrayList<Searcher>();
		
		File indices = new File(indexPath);
		for (File file : indices.listFiles()) {
			if (!LuceneMediator.indexExists(file)) {
				continue;
			}
			try {
				FSDirectory directory = FSDirectory.open(file);
				searchers.add(new IndexSearcher(directory));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (searchers.size() == 0) {
			throw new IOException("No indices found");
		}
		return new MultiSearcher(searchers.toArray(new Searchable[searchers.size()]));
	}

	public void initLucene(String indexDir) throws IOException {
		Analyzer analyzer = LuceneMediator.createDefaultAnalyzer();
		Searcher searcher = createSearcher(indexDir);

		setGeneMediator(new LuceneGeneMediator(searcher, analyzer));
		setNetworkMediator(new LuceneNetworkMediator(searcher, analyzer));
		setOrganismMediator(new LuceneOrganismMediator(searcher, analyzer));
		setNodeMediator(new LuceneNodeMediator(searcher, analyzer));
		setOntologyMediator(new LuceneOntologyMediator(searcher, analyzer));
		setStatsMediator(new LuceneStatsMediator(searcher, analyzer));
	}
	
	/*
	 * container for index and cache objects,
	 * still some redundancy here with the object
	 * fields, TODO: cleanup
	 */
    void initDataConnector() {
    	dataConnector = new DataConnector();
    	dataConnector.setCache(cache);
    	dataConnector.setGeneMediator(geneMediator);
    	dataConnector.setNetworkMediator(networkMediator);
    	dataConnector.setNodeMediator(nodeMediator);
    	dataConnector.setOntologyMediator(ontologyMediator);
    	dataConnector.setOrganismMediator(organismMediator);
    	dataConnector.setStatsMediator(statsMediator);
    }
    /*
     * select networks marked as default for the organism in the db. walk the
     * entire network tree for the org to get the grouping right ...
     *
     */
    public static Collection<Collection<Long>> getDefaultNetworks(Organism organism) throws ApplicationException, DataStoreException {

        int numFound = 0;

        Collection<InteractionNetworkGroup> groups = organism.getInteractionNetworkGroups();

        Collection<Collection<Long>> ids = new ArrayList<Collection<Long>>();
        for (InteractionNetworkGroup group: groups) {


            Collection<InteractionNetwork> networks = group.getInteractionNetworks();
            List<Long> list = new ArrayList<Long>();

            for (InteractionNetwork n: networks) {
                if (n.isDefaultSelected()) {
                NetworkMetadata metadata = n.getMetadata();

                    logger.info(String.format("using default network %d containing %d interactions from group %s: %s",
                            n.getId(), metadata.getInteractionCount(), group.getName(), n.getName()));

                    list.add(n.getId());
                    numFound += 1;
                }
            }

            if (list.size() > 0) {
                ids.add(list);
            }
        }

        if (ids.size() == 0) {
            throw new ApplicationException("no default networks found!");
        }

        return ids;
    }

    /*
     * return list of all networks associated with the organism in the db,
     * organized by group
     */
    public static Collection<Collection<Long>> getAllNetworks(Organism organism) {
        Collection<InteractionNetworkGroup> groups = organism.getInteractionNetworkGroups();
        int numFound = 0;

        Collection<Collection<Long>> ids = new ArrayList<Collection<Long>>();
        for (InteractionNetworkGroup group: groups) {

            Collection<InteractionNetwork> networks = group.getInteractionNetworks();
            List<Long> list = new ArrayList<Long>();

            for (InteractionNetwork n: networks) {
                list.add(n.getId());
                numFound += 1;
            }

            if (list.size() > 0) {
                ids.add(list);
            }
        }

        return ids;
    }

    /*
     * given string of comma delimited ids (no space), validate against networks in the db,
     * and return grouped by the corresponding network groups.
     *
     * you would think we could just do networkMediator.getNetwork(id) to do the lookups, but
     * i don't see how you can get to the network groups this way. so instead we actually iterate
     * through all the network groups and search each (argh!)
     */
    static Collection<Collection<Long>> getNetworksById(Organism organism, String idsArg) throws ApplicationException, DataStoreException {
        String [] parts = idsArg.split(",");

        HashSet<String> wantedIds = new HashSet<String>();
        wantedIds.addAll(Arrays.asList(parts));
        int numFound = 0;

        Collection<InteractionNetworkGroup> groups = organism.getInteractionNetworkGroups();

        Collection<Collection<Long>> ids = new ArrayList<Collection<Long>>();
        for (InteractionNetworkGroup group: groups) {

            Collection<InteractionNetwork> networks = group.getInteractionNetworks();
            List<Long> list = new ArrayList<Long>();

            for (InteractionNetwork n: networks) {
                NetworkMetadata metadata = n.getMetadata();
                String key = "" + n.getId();

                if (wantedIds.contains(key)) {

                    logger.info(String.format("using network %d containing %d interactions from group %s: %s",
                            n.getId(), metadata.getInteractionCount(), group.getName(), n.getName()));

                    list.add(n.getId());
                    numFound += 1;
                }
            }

            if (list.size() > 0) {
                ids.add(list);
            }
        }

        if (numFound != parts.length) {
            throw new ApplicationException("some of the specified networks could not be found");
        }

        return ids;
    }

    /*
     * network ids are grouped, count em up individually.
     */
    public static int count(Collection<Collection<Long>> networkIds) {
        int sum = 0;

        for (Collection<Long> group: networkIds) {
            sum += group.size();
        }

        return sum;
    }

    /*
     * create a data cache object
     */
    public void initDataCache(String cacheDir, boolean memCacheEnabled) {
    	IObjectCache objectCache = new FileSerializedObjectCache(cacheDir);
    	if (memCacheEnabled) {
    		objectCache = new MemObjectCache(objectCache);
    	}
    	
    	objectCache = new SynchronizedObjectCache(objectCache);
        cache = new DataCache(objectCache);
    }
 
    /*
     * use mem caching by default
     */
    public void initDataCache(String cacheDir) {
    	initDataCache(cacheDir, true);
    }

    public DataCache getCache() {
        return cache;
    }

    public void setCache(DataCache cache) {
        this.cache = cache;
    }

    public GeneMediator getGeneMediator() {
        return geneMediator;
    }

    public void setGeneMediator(GeneMediator geneMediator) {
        this.geneMediator = geneMediator;
    }

    public NetworkMediator getNetworkMediator() {
        return networkMediator;
    }

    public void setNetworkMediator(NetworkMediator networkMediator) {
        this.networkMediator = networkMediator;
    }

    public NodeMediator getNodeMediator() {
        return nodeMediator;
    }

    public void setNodeMediator(NodeMediator nodeMediator) {
        this.nodeMediator = nodeMediator;
    }

    public OrganismMediator getOrganismMediator() {
        return organismMediator;
    }

    public void setOrganismMediator(OrganismMediator organismMediator) {
        this.organismMediator = organismMediator;
    }

    public OntologyMediator getOntologyMediator() {
        return ontologyMediator;
    }

    public void setOntologyMediator(OntologyMediator ontologyMediator) {
        this.ontologyMediator = ontologyMediator;
    }
        
    public StatsMediator getStatsMediator() {
		return statsMediator;
	}

	public void setStatsMediator(StatsMediator statsMediator) {
		this.statsMediator = statsMediator;
	}

	public static Logger getLogger() {
		return logger;
	}

	public static void setLogger(Logger logger) {
		AbstractEngineApp.logger = logger;
	}

	public String getCacheDir() {
		return cacheDir;
	}

	public void setCacheDir(String cacheDir) {
		this.cacheDir = cacheDir;
	}

	public String getIndexDir() {
		return indexDir;
	}

	public void setIndexDir(String indexDir) {
		this.indexDir = indexDir;
	}

	public String getLogFilename() {
		return logFilename;
	}

	public void setLogFilename(String logFilename) {
		this.logFilename = logFilename;
	}
	
	public DataConnector getDataConnector() {
		return dataConnector;
	}

	public boolean getCommandLineArgs(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        }
        catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("java -jar myprogram.jar [options...] arguments...");
            parser.printUsage(System.err);
            return false;
        }

        return true;
    }
    
    /**
     * initialize domain model & data cache, logging
     */
    public void init() throws Exception {
    	initLucene(indexDir);
        initDataCache(cacheDir);
        initDataConnector();
        setupLogging();
    }    
    
    public void cleanup() throws Exception {
    	logger.info("done");
    }
    
    /**
     * For command-line usage, user can specify a log file with an optional argument.
     * log4j is used as the actual logging mechanism, and is configured here.
     *
     */
    public void setupLogging() throws Exception {
        // setup logging
        if (logFilename == null) {
            return;
        }
       
        Appender appender;
        if (logFilename.equalsIgnoreCase("console")) {
            SimpleLayout layout = new SimpleLayout();
            appender = new ConsoleAppender(layout);
        }
        else {
            PatternLayout layout = new PatternLayout("%d{HH:mm:ss} %-5p: %m%n");
            appender = new FileAppender(layout, logFilename, false);
        }

        Logger.getLogger("org.genemania").setLevel((Level) Level.DEBUG);
        Logger.getRootLogger().addAppender(appender);        
    }
    
    public abstract void process() throws Exception;

	/*
	 *  for testing, just loop through organisms and print names
	 */
    public static void main(String[] args) throws Exception {


        AbstractEngineApp app = new AbstractEngineApp() {
        	public void process() throws Exception {
                for (Organism organism: organismMediator.getAllOrganisms()) {
                    logger.info(String.format("Organism %d: %s", organism.getId(), organism.getName()));
                }
        	}
        };
        
        if (!app.getCommandLineArgs(args)) {
            System.exit(1);
        }        
        
        try {
        	app.init();
        	app.process();
        	app.cleanup();
        }
        catch (Exception e) {
            logger.error("Fatal error", e);
            System.exit(1);
        }
    }
}
