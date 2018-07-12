import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import helpers.Configuration;
import httpLinkChecker.CollectionThread;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import urlElements.URLElement;
import urlElements.URLElementToBeChecked;

import java.util.concurrent.CountDownLatch;

public class Main {

    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        if (args.length == 0) {
            logger.info("Usage: Please provide the config file path as a parameter.");
            System.exit(1);
        }

        Configuration.loadConfigVariables(args[0]);

        //connect to mongod and get database
        MongoDatabase database = getMongoDatabase();

        //get links from linksToBeChecked
        MongoCollection<Document> linksToBeChecked = database.getCollection("linksToBeChecked");

        //get linksChecked
        MongoCollection<Document> linksChecked = database.getCollection("linksChecked");

        //Ensure that "url" is a unique index
        IndexOptions indexOptions = new IndexOptions().unique(true);
        linksChecked.createIndex(new Document("url", 1), indexOptions);
        linksToBeChecked.createIndex(new Document("url", 1), indexOptions);

        while (true) {


            MongoCursor<Document> cursor = linksToBeChecked.find().iterator();
            try {
                while (cursor.hasNext()) {
                    URLElementToBeChecked urlElementToBeChecked = new URLElementToBeChecked(cursor.next());

                    String collection = urlElementToBeChecked.getCollection();
                    String url = urlElementToBeChecked.getUrl();
                    logger.info("URL to be checked: " + url + ", from collection: " + collection);


                    CollectionThread t = getCollectionThreadByName(collection);
                    if (t == null) {
                        t = new CollectionThread(collection, linksToBeChecked, linksChecked);
                        t.urlQueue.add(url);

                        t.start();
                    } else {
                        if (!t.urlQueue.contains(url)) {
                            t.urlQueue.add(url);
                        }
                    }

                }

                logger.info("Added all links to respective threads.");


                //Create a logger thread that outputs every 10 seconds the current state of Collection threads...
                (new Thread() {
                    public void run() {

                        while (true) {
                            //log current state
                            for (Thread tr : Thread.getAllStackTraces().keySet()) {
                                if (tr.getClass().equals(CollectionThread.class)) {
                                    logger.info("Collection thread: " + tr.getName() + " is running.");
                                    logger.info("It has " + ((CollectionThread) tr).urlQueue.size() + " links in its queue.");
                                }
                            }

                            synchronized (this) {
                                try {
                                    wait(10000);
                                } catch (InterruptedException e) {
                                    //dont do anything, this thread is not that important.
                                }
                            }
                        }
                    }
                }).start();


                logger.info("Waiting for all threads to finish...");//logger thread not included

                //wait for collectionThreads to finish...
                for (Thread tr : Thread.getAllStackTraces().keySet()) {
                    if (tr.getClass().equals(CollectionThread.class)) {
                        try {
                            tr.join();
                        } catch (InterruptedException e) {
                            //this shouldn't happen but if it does, the program keeps running.
                            logger.error(tr.toString() + "was interrupted.");
                        }
                    }
                }

                logger.info("All threads are finished.");
                logger.info("Checked all links.");

                if (linksToBeChecked.count() == 0) {
                    logger.info("Copying all links back to linksToBeChecked from linksChecked.");
                    cursor = linksChecked.find().iterator();

                    while (cursor.hasNext()) {

                        URLElement urlElement = new URLElement(cursor.next());
                        String url = urlElement.getUrl();
                        logger.info("Adding " + url + " to linksToBeChecked.");

                        URLElementToBeChecked urlElementToBeChecked = new URLElementToBeChecked(url, urlElement.getCollection());
                        try {
                            linksToBeChecked.insertOne(urlElementToBeChecked.getMongoDocument());
                        } catch (MongoException e) {
                            //duplicate key error
                            //url is already in the database, do nothing
                        }


                    }
                } else {
                    logger.info("There are new links in linksToBeChecked. Not copying links back from linksChecked. Will do it in the next run if linksToBeChecked is empty.");
                }


            } finally {
                cursor.close();
            }

            logger.info("Done with the run. Running all of it again...");

        }

    }


    private static CollectionThread getCollectionThreadByName(String threadName) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().equals(threadName) && t.getClass().equals(CollectionThread.class)) {
                return (CollectionThread) t;
            }
        }
        return null;
    }


    private static MongoDatabase getMongoDatabase() {
        logger.info("Connecting to database...");

        MongoClient mongoClient;
        if (Configuration.DATABASE_URI.isEmpty()) {//if it is empty, try localhost
            mongoClient = MongoClients.create();
        } else {
            mongoClient = MongoClients.create(Configuration.DATABASE_URI);
        }

        MongoDatabase database = mongoClient.getDatabase(Configuration.DATABASE_NAME);

        logger.info("Connected to database.");

        return database;

    }

}
