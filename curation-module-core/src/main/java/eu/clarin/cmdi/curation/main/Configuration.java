package eu.clarin.cmdi.curation.main;

import eu.clarin.cmdi.rasa.helpers.RasaFactory;
import eu.clarin.cmdi.rasa.helpers.impl.ACDHRasaFactory;
import eu.clarin.cmdi.rasa.linkResources.CategoryStatisticsResource;
import eu.clarin.cmdi.rasa.linkResources.CheckedLinkResource;
import eu.clarin.cmdi.rasa.linkResources.LinkToBeCheckedResource;
import eu.clarin.cmdi.vlo.config.DefaultVloConfigFactory;
import eu.clarin.cmdi.vlo.config.VloConfig;
import eu.clarin.cmdi.vlo.config.XmlVloConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Configuration {

    private static Logger logger = LoggerFactory.getLogger(Configuration.class);


    public static Long reportGenerationDate = System.currentTimeMillis();

    public static String SCORE_NUMERIC_DISPLAY_FORMAT;
    public static String TIMESTAMP_DISPLAY_FORMAT;
    public static long MAX_FILE_SIZE;
    public static boolean SAVE_REPORT;
    public static Path OUTPUT_DIRECTORY = null;
    public static Path CACHE_DIRECTORY = null;
    public static int THREAD_POOL_SIZE = 100;
    public static Collection<String> FACETS = null;
    public static int REDIRECT_FOLLOW_LIMIT;
    public static int TIMEOUT;

    public static VloConfig VLO_CONFIG;
    private static String DATABASE_USERNAME;
    private static String DATABASE_URI;
    private static String DATABASE_PASSWORD;
    public static String USERAGENT;
    public static String BASE_URL;

    public static CheckedLinkResource checkedLinkResource;
    public static LinkToBeCheckedResource linkToBeCheckedResource;
    public static CategoryStatisticsResource statisticsResource;

    //this is a boolean that is set by core-module(false) and web-module(true)
    public static boolean enableProfileLoadTimer = false;

    public static boolean COLLECTION_MODE = false; //when true values wont be extracted for facet "text", saves a lot of time in collection assessment

    private static RasaFactory factory;

    public static void init(String file) throws IOException {
        logger.info("Initializing configuration from {}", file);
        Properties config = new Properties();
        config.load(new FileInputStream(file));
        readProperties(config);
        //readProperties(new PropertiesConfiguration(file));
    }

    public static void initDefault() throws IOException {
        logger.info("Initializing configuration with default config file");
        Properties config = new Properties();
        config.load(Configuration.class.getResourceAsStream("/config.properties"));
        readProperties(config);
        //readProperties(new PropertiesConfiguration("config.properties"));

    }

    public static void tearDown() {
        logger.info("Stopping Curation Module...");
        factory.tearDown();
        logger.info("Curation Module stopped.");
    }

    private static void readProperties(Properties config) throws IOException {

        SCORE_NUMERIC_DISPLAY_FORMAT = config.getProperty("SCORE_NUMERIC_DISPLAY_FORMAT");
        TIMESTAMP_DISPLAY_FORMAT = config.getProperty("TIMESTAMP_DISPLAY_FORMAT");
        MAX_FILE_SIZE = Long.parseLong(config.getProperty("MAX_FILE_SIZE"));
        SAVE_REPORT = Boolean.parseBoolean(config.getProperty("SAVE_REPORT"));

        String timeout = config.getProperty("TIMEOUT");
        if (timeout == null || timeout.isEmpty()) {
            //in ms(if config file doesnt have it)
            int TIMEOUTDEFAULT = 5000;
            logger.info("Timeout is not specified in config.properties file. Default timeout is assumed: " + TIMEOUTDEFAULT + "ms.");
            TIMEOUT = TIMEOUTDEFAULT;
        } else {
            TIMEOUT = Integer.parseInt(timeout);
        }
        THREAD_POOL_SIZE = Integer.parseInt(config.getProperty("THREAD_POOL_SIZE", "100"));

        String[] facets = config.getProperty("FACETS").split(",");
        FACETS = Arrays.stream(facets).map(String::trim).collect(Collectors.toList());

        String outDir = config.getProperty("OUTPUT_DIRECTORY");
        String cacheDir = config.getProperty("CACHE_DIRECTORY");

        if (outDir != null && !outDir.isEmpty()) {
            OUTPUT_DIRECTORY = Files.createDirectories(Paths.get(outDir));
        }
        if (cacheDir != null && !cacheDir.isEmpty()) {
            CACHE_DIRECTORY = Files.createDirectories(Paths.get(cacheDir));
        }

        String redirectFollowLimit = config.getProperty("REDIRECT_FOLLOW_LIMIT");
        if (redirectFollowLimit != null && !redirectFollowLimit.isEmpty()) {
            REDIRECT_FOLLOW_LIMIT = Integer.parseInt(redirectFollowLimit);
        }

        DATABASE_USERNAME = config.getProperty("DATABASE_USERNAME");
        DATABASE_URI = config.getProperty("DATABASE_URI");
        DATABASE_PASSWORD = config.getProperty("DATABASE_PASSWORD");
        DATABASE_PASSWORD = DATABASE_PASSWORD == null ? "" : DATABASE_PASSWORD;

        factory = new ACDHRasaFactory(DATABASE_URI, DATABASE_USERNAME, DATABASE_PASSWORD);
        checkedLinkResource = factory.getCheckedLinkResource();
        linkToBeCheckedResource = factory.getLinkToBeCheckedResource();
        statisticsResource = factory.getStatisticsResource();


        String vloConfigLocation = config.getProperty("VLO_CONFIG_LOCATION");

        if (vloConfigLocation == null || vloConfigLocation.isEmpty()) {
            logger.warn("loading default VloConfig.xml from vlo-commons.jar - PROGRAM WILL WORK BUT WILL PROBABABLY DELIVER UNATTENDED RESULTS!!!");
            logger.warn("make sure to define a valid VLO_CONFIG_LOCATION in the file config.properties");
            VLO_CONFIG = new DefaultVloConfigFactory().newConfig();
        } else {
            logger.info("loading VloConfig.xml from location {}", vloConfigLocation);
            VLO_CONFIG = new XmlVloConfigFactory(new File(config.getProperty("VLO_CONFIG_LOCATION")).toURI().toURL()).newConfig();
        }

        USERAGENT = config.getProperty("USERAGENT");
        BASE_URL = config.getProperty("BASE_URL");
    }

    public static class StormycheckerConstants{

        public static final List<Integer> okStatusCodes = new ArrayList<>(Arrays.asList(200, 304));

        public static final List<Integer> redirectStatusCodes = new ArrayList<>(Arrays.asList(301, 302, 303, 307, 308));

        //this determines what status codes will not be considered broken links. urls with these codes will also not factor into the url-scores
        public static final List<Integer> undeterminedStatusCodes = new ArrayList<>(Arrays.asList(401, 405, 429));
    }
}
