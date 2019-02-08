package eu.clarin.curation.linkchecker.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Configuration {

    private static Properties properties = new Properties();
    private final static Logger _logger = LoggerFactory.getLogger(Configuration.class);


    public static String DATABASE_NAME;
    public static String DATABASE_URI;
    public static int TIMEOUT;
    public static int REDIRECT_FOLLOW_LIMIT;
    public static String USERAGENT;
    public static long CRAWLDELAY;
    public static boolean ONLY_BROKEN;
    public static Map<String, Long> CRAWLDELAYMAP = new HashMap<>();


    public static void loadConfigVariables(String configPath) {
        try {
            properties.load(new FileInputStream(configPath));
        } catch (IOException e) {

            _logger.error("Can't load properties file: " + e.getMessage());
            System.exit(1);

        }

        DATABASE_NAME = properties.getProperty("DATABASE_NAME");
        TIMEOUT = Integer.parseInt(properties.getProperty("TIMEOUT"));
        REDIRECT_FOLLOW_LIMIT = Integer.parseInt(properties.getProperty("REDIRECT_FOLLOW_LIMIT"));
        DATABASE_URI = properties.getProperty("DATABASE_URI");
        USERAGENT = properties.getProperty("USERAGENT");
        CRAWLDELAY = Long.parseLong(properties.getProperty("CRAWLDELAY"));
        ONLY_BROKEN = Boolean.parseBoolean(properties.getProperty("ONLY_BROKEN"));

        String crawlDelayList = properties.getProperty("CRAWLDELAYLIST");
        for (String crawlDelayEntry : crawlDelayList.split(",")) {
            String collection = crawlDelayEntry.split("=")[0];
            long delay = Long.parseLong(crawlDelayEntry.split("=")[1]);
            CRAWLDELAYMAP.put(collection, delay);
        }


    }

}
