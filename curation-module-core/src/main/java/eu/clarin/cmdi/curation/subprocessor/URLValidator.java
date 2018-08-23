package eu.clarin.cmdi.curation.subprocessor;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import eu.clarin.cmdi.curation.entities.CMDInstance;
import eu.clarin.cmdi.curation.instance_parser.ParsedInstance;
import eu.clarin.cmdi.curation.instance_parser.ParsedInstance.InstanceNode;
import eu.clarin.cmdi.curation.main.Configuration;
import eu.clarin.cmdi.curation.report.CMDInstanceReport;
import eu.clarin.cmdi.curation.report.CMDInstanceReport.URLReport;
import eu.clarin.cmdi.curation.report.Score;
import eu.clarin.cmdi.curation.report.Severity;
import eu.clarin.cmdi.curation.utils.TimeUtils;
import eu.clarin.curation.linkchecker.httpLinkChecker.HTTPLinkChecker;
import eu.clarin.curation.linkchecker.urlElements.URLElement;
import eu.clarin.curation.linkchecker.urlElements.URLElementToBeChecked;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dostojic
 */

public class URLValidator extends CMDSubprocessor {

    private static final Logger _logger = LoggerFactory.getLogger(URLValidator.class);

    private static final MongoClient mongoClient;

    static { //since MongoClient is already a connection pool only one instance should exist in the application
        if (Configuration.DATABASE) {
            _logger.info("Connecting to database...");
            if (Configuration.DATABASE_URI == null || Configuration.DATABASE_URI.isEmpty()) {//if it is empty, try localhost
                mongoClient = MongoClients.create();
            } else {
                mongoClient = MongoClients.create(Configuration.DATABASE_URI);
            }
        }else{
            mongoClient =null;
        }
    }

    @Override
    public void process(CMDInstance entity, CMDInstanceReport report) {
        //do nothing this is not used
    }

    public void process(CMDInstance entity, CMDInstanceReport report, String parentName) {
        ParsedInstance parsedInstance = entity.getParsedInstance();
        Collection<String> links = parsedInstance.getNodes()
                .stream()
                .filter(node -> !node.getXpath().equals("/CMD/@xsi:schemaLocation"))
                .filter(node -> !node.getXpath().equals("/CMD/@xmlns:xsi"))
                .filter(node -> !node.getXpath().equals("/CMD/@xml:xsi"))
                .map(InstanceNode::getValue)
                .filter(url -> url.startsWith("http"))
                .collect(Collectors.toList());
        int numOfLinks = links.size();
        links = links.stream().distinct().collect(Collectors.toList());
        int numOfUniqueLinks = links.size();

        // links are unique
        if (Configuration.HTTP_VALIDATION) {
            AtomicInteger numOfCheckedLinks = new AtomicInteger(0);
            AtomicInteger numOfBrokenLinks = new AtomicInteger(0);
            if (Configuration.DATABASE && Configuration.COLLECTION_MODE) {

                MongoDatabase database = mongoClient.getDatabase(Configuration.DATABASE_NAME);
                _logger.info("Connected to database.");


                //get links from linksToBeChecked
                MongoCollection<Document> linksToBeChecked = database.getCollection("linksToBeChecked");

                //get linksChecked
                MongoCollection<Document> linksChecked = database.getCollection("linksChecked");

                links.stream().forEach(url -> {

                    _logger.info("Checking database for url: " + url);

                    Bson filter = Filters.eq("url", url);
                    MongoCursor<Document> cursor = linksChecked.find(filter).iterator();

                    //because urls are unique in the database if cursor has next, it found the only one. If not, the url wasn't found.
                    if (cursor.hasNext()) {
                        URLElement urlElement = new URLElement(cursor.next());

                        addMessageForStatusCode(urlElement.getStatus(), numOfBrokenLinks, url);

                        CMDInstanceReport.URLElement urlElementReport = new CMDInstanceReport.URLElement().convertFromLinkCheckerURLElement(urlElement);
                        report.addURLElement(urlElementReport);
                        numOfCheckedLinks.incrementAndGet();

                    } else {

                        String collection = parentName;

                        if (collection == null) {
                            collection = report.getName();
                        }
                        URLElementToBeChecked urlElementToBeChecked = new URLElementToBeChecked(url, collection);

                        try {

                            linksToBeChecked.insertOne(urlElementToBeChecked.getMongoDocument());
                        } catch (MongoException e) {
                            //duplicate key error
                            //the url is already in the database, do nothing
                        }


                    }
                });

            } else {

                links.stream().forEach(url -> {

                    try {// check if URL is broken
                        _logger.info("Checking url: " + url);

                        URLElement urlElement = new HTTPLinkChecker(Configuration.TIMEOUT, Configuration.REDIRECT_FOLLOW_LIMIT).checkLink(url, 0, 0, url);//redirect follow level is current level, because this is the first request it is set to 0

                        addMessageForStatusCode(urlElement.getStatus(), numOfBrokenLinks, url);

                        CMDInstanceReport.URLElement urlElementReport = new CMDInstanceReport.URLElement().convertFromLinkCheckerURLElement(urlElement);
                        report.addURLElement(urlElementReport);


                    } catch (IOException e) {
                        CMDInstanceReport.URLElement urlElement = new CMDInstanceReport.URLElement();
                        urlElement.message = e.getLocalizedMessage();
                        urlElement.url = url;
                        urlElement.status = 0;
                        urlElement.contentType = null;
                        urlElement.byteSize = "0";
                        urlElement.timestamp = TimeUtils.humanizeToDate(System.currentTimeMillis());
                        urlElement.duration = "0 ms";
                        urlElement.redirectCount = 0;
                        report.addURLElement(urlElement);

                        numOfBrokenLinks.incrementAndGet();
                        addMessage(Severity.ERROR, "URL: " + url + "    STATUS:" + e.toString());
                    }
                    numOfCheckedLinks.incrementAndGet();
                });

            }
            report.urlReport = createURLReport(numOfLinks, numOfBrokenLinks.get(), numOfUniqueLinks, numOfCheckedLinks.get());
        } else {
            report.urlReport = createURLReport(numOfLinks, 0, numOfUniqueLinks, 0);
            addMessage(Severity.INFO, "Link validation is disabled");
        }

    }

    private void addMessageForStatusCode(int responseCode, AtomicInteger numOfBrokenLinks, String url) {

        if (responseCode == 200 || responseCode == 302) {
        } // OK
        else if (responseCode < 400) {// 2XX and 3XX, redirections, empty content
            addMessage(Severity.WARNING, "URL: " + url + "     STATUS:" + responseCode);
        } else {// 4XX and 5XX, client/server errors
            numOfBrokenLinks.incrementAndGet();
            addMessage(Severity.ERROR, "URL: " + url + "    STATUS:" + responseCode);
        }
    }

    @Override
    public Score calculateScore(CMDInstanceReport report) {
        // it can influence the score, if one collection was done with enabled and the other without

        double score = report.urlReport.percOfValidLinks != null && !Double.isNaN(report.urlReport.percOfValidLinks) ? report.urlReport.percOfValidLinks : 0;
        return new Score(score, 1.0, "url-validation", msgs);
    }


    private URLReport createURLReport(int numOfLinks, int numOfBrokenLinks, int numOfUniqueLinks, int numOfCheckedLinks) {
        URLReport report = new URLReport();
        report.numOfLinks = numOfLinks;
        report.numOfBrokenLinks = numOfBrokenLinks;
        report.numOfUniqueLinks = numOfUniqueLinks;
        report.numOfCheckedLinks = numOfCheckedLinks;
        if (Configuration.HTTP_VALIDATION) {
//            report.percOfValidLinks = (numOfUniqueLinks - numOfBrokenLinks) / (double) numOfUniqueLinks;
            //replace unique links with checked links so that the math only covers the checked links and doesn't display wrong results
            report.percOfValidLinks = numOfCheckedLinks == 0 ? 0 : (numOfCheckedLinks - numOfBrokenLinks) / (double) numOfCheckedLinks;
        }
        return report;
    }

}
