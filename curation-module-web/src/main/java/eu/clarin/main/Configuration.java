package eu.clarin.main;

import eu.clarin.cmdi.rasa.linkResources.CategoryStatisticsResource;
import eu.clarin.cmdi.rasa.linkResources.CheckedLinkResource;
import eu.clarin.cmdi.rasa.linkResources.LinkToBeCheckedResource;
import eu.clarin.cmdi.rasa.linkResources.StatisticsResource;
import eu.clarin.helpers.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * This class is run at the start(deployment) of the web application- It reads global constants from config files, establishes connections with the databases and generally initialises the web app.
 */
public class Configuration {

    private static Logger logger = LoggerFactory.getLogger(Configuration.class);

    public static String VIEW_RESOURCES_PATH;
    public static String RECORDS_PATH;
    public static String OUTPUT_DIRECTORY;
    public static String BASE_URL;
    public static ArrayList<String> clarinCollections;
    public static ArrayList<String> europeanaCollections;

    public static CheckedLinkResource checkedLinkResource;
    public static LinkToBeCheckedResource linkToBeCheckedResource;
    public static CategoryStatisticsResource statisticsResource;

    public static void init(ServletContext servletContext) throws IOException {

        VIEW_RESOURCES_PATH = servletContext.getRealPath("/WEB-INF/classes/view/");

        String path = servletContext.getInitParameter("config.location");

        //this is necessary for core module methods.
        eu.clarin.cmdi.curation.main.Configuration.init(path);

        checkedLinkResource = eu.clarin.cmdi.curation.main.Configuration.checkedLinkResource;
        linkToBeCheckedResource = eu.clarin.cmdi.curation.main.Configuration.linkToBeCheckedResource;
        statisticsResource = eu.clarin.cmdi.curation.main.Configuration.statisticsResource;

        logger.info("Initializing configuration from: " + path);
        Properties properties = new Properties();
        properties.load(new FileInputStream(path));
        loadVariables(properties);
        scheduleCleaningTemp();

    }

    public static void tearDown(){
        eu.clarin.cmdi.curation.main.Configuration.tearDown();
    }

    private static void scheduleCleaningTemp() {
        //scheduler to delete temporary instance and profile reports every day at 1 am.
        Runnable instanceCleaner = () -> {
            String instancesXmlFolder = OUTPUT_DIRECTORY + "/xml/instances";
            String instancesHtmlFolder = OUTPUT_DIRECTORY + "/html/instances";

            try {
                FileManager.cleanFolders(Arrays.asList(instancesXmlFolder, instancesHtmlFolder), null);
                logger.info("Cleaned temp instances.");
            } catch (IOException e) {
                logger.error("Error when cleaning instances: " + e.getMessage());
            }
        };

        Runnable profileCleaner = () -> {
            String profilesXmlFolder = OUTPUT_DIRECTORY + "/xml/profiles";
            String profilesHtmlFolder = OUTPUT_DIRECTORY + "/html/profiles";
            try {
                FileManager.cleanFolders(Arrays.asList(profilesHtmlFolder, profilesXmlFolder), "^\\d{13}_.+$");
                logger.info("Cleaned temp profiles.");
            } catch (IOException e) {
                logger.error("Error when cleaning profiles: " + e.getMessage());
            }
        };


        long oneAM = LocalDateTime.now().until(LocalDate.now().plusDays(1).atTime(1, 0), ChronoUnit.MINUTES);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        scheduler.scheduleAtFixedRate(instanceCleaner, oneAM, TimeUnit.DAYS.toMinutes(1), MINUTES);
        scheduler.scheduleAtFixedRate(profileCleaner, oneAM, TimeUnit.DAYS.toMinutes(1), MINUTES);

    }

    private static void loadVariables(Properties properties) {

        OUTPUT_DIRECTORY = properties.getProperty("OUTPUT_DIRECTORY");
        RECORDS_PATH = properties.getProperty("RECORDS_PATH");

        BASE_URL = properties.getProperty("BASE_URL");

        //collection categorizing into europeana and clarin based on the data folder
        String dataDirectory = properties.getProperty("DATA_DIRECTORY");
        try (Stream<Path> walk = Files.walk(Paths.get(dataDirectory + "/clarin/results/cmdi/"))) {
            clarinCollections = (ArrayList<String>) walk.filter(Files::isDirectory).filter(path -> !path.getFileName().toString().equals("cmdi")).map(file -> file.getFileName().toString()).collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error when reading folders from :" + dataDirectory + "/clarin/results/cmdi, Message:" + e.getMessage());
        }

        try (Stream<Path> walk = Files.walk(Paths.get(dataDirectory + "/europeana/results/cmdi/"))) {
            europeanaCollections = (ArrayList<String>) walk.filter(Files::isDirectory).filter(path -> !path.getFileName().toString().equals("cmdi")).map(file -> file.getFileName().toString()).collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error when reading folders from :" + dataDirectory + "/europeana/results/cmdi, Message:" + e.getMessage());
        }
        logger.info("Clarin collections: " + clarinCollections);
        logger.info("Europeana collections: " + europeanaCollections);

        //create Instances directory if it doesn't exist
        try {
            Path path = Paths.get(OUTPUT_DIRECTORY + "/xml/instances");
            Files.createDirectories(path);
            path = Paths.get(OUTPUT_DIRECTORY + "/html/instances");
            Files.createDirectories(path);
            path = Paths.get(OUTPUT_DIRECTORY + "/xml/profiles");
            Files.createDirectories(path);
            path = Paths.get(OUTPUT_DIRECTORY + "/html/profiles");
            Files.createDirectories(path);
        } catch (IOException e) {
            logger.error("There was an error creating the profiles/instances xml/html folder: " + e.getMessage());
        }


    }
}
