package eu.clarin.cmdi.curation.subprocessor;

import eu.clarin.cmdi.curation.entities.CMDInstance;
import eu.clarin.cmdi.curation.exception.CategoryExceptionMapper;
import eu.clarin.cmdi.curation.main.Configuration;
import eu.clarin.cmdi.curation.report.CMDInstanceReport;
import eu.clarin.cmdi.curation.report.CMDInstanceReport.URLReport;
import eu.clarin.cmdi.curation.report.Score;
import eu.clarin.cmdi.curation.report.Severity;
import eu.clarin.cmdi.curation.utils.HTTPLinkChecker;
import eu.clarin.cmdi.rasa.DAO.CheckedLink;
import eu.clarin.cmdi.rasa.DAO.LinkToBeChecked;
import eu.clarin.cmdi.rasa.filters.impl.ACDHCheckedLinkFilter;
import eu.clarin.cmdi.rasa.helpers.statusCodeMapper.Category;
import eu.clarin.cmdi.vlo.importer.CMDIData;
import eu.clarin.cmdi.vlo.importer.Resource;
import eu.clarin.cmdi.vlo.importer.processor.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * This class extracts the urls from the harvest then checks the database for them. If they are in the database, i.e stormychecker has checked them, it aggregates the results into the url report.
 * If the urls from the harvest are not in the database, it adds them to the database to be checked by stormychecker.
 * The results of link checking are aggregated into statistics
 */
public class URLValidator extends CMDSubprocessor {

    private static final Logger logger = LoggerFactory.getLogger(URLValidator.class);

    @Override
    public void process(CMDInstance entity, CMDInstanceReport report) {
        //do nothing this is not used, not really good coding, is it???
    }

    public void process(CMDInstance entity, CMDInstanceReport report, String parentName) {

        CMDIData<Map<String, List<ValueSet>>> data = entity.getCMDIData();

        Map<String, Resource> urlMap = new HashMap<>();

        final AtomicLong numOfLinks = new AtomicLong(0);  //has to be final for use in lambda expression

        ArrayList<Resource> resources = new ArrayList<>();
        resources.addAll(data.getDataResources());
        resources.addAll(data.getLandingPageResources());
        resources.addAll(data.getMetadataResources());
        resources.addAll(data.getSearchPageResources());
        resources.addAll(data.getSearchResources());

        resources.stream()
                .filter(resource -> resource.getResourceName() != null && (resource.getResourceName().startsWith("http://") || resource.getResourceName().startsWith("https://")))
                .forEach(resource -> {
                    urlMap.computeIfAbsent(resource.getResourceName(), key -> resource);
                    numOfLinks.incrementAndGet();
                });

        String selfLink = (data.getDocument().get("_selfLink") != null && !data.getDocument().get("_selfLink").isEmpty()) ? data.getDocument().get("_selfLink").get(0).getValue() : "";

        //only add selfLink if url
        if (selfLink.startsWith("http://") || selfLink.startsWith("https://"))
            urlMap.computeIfAbsent(selfLink, key -> null);

        //these values are used to create instance url report and used for the score
        //collection statistics are done through the database
        long numOfUniqueLinks = urlMap.keySet().size();
        long numOfBrokenLinks = 0;
        long numOfUndeterminedLinks = 0;
        long numOfRestrictedAccessLinks = 0;
        long numOfBlockedByRobotsTxtLinks = 0;

        if (Configuration.COLLECTION_MODE) {

            List<LinkToBeChecked> linksToBeChecked = new ArrayList<>();
            List<String> linksToBeUpdated = new ArrayList<>();

            for (String url : urlMap.keySet()) {
                try {
                    Optional<LinkToBeChecked> linkToBeCheckedOptional = Configuration.linkToBeCheckedResource.get(url);
                    if (linkToBeCheckedOptional.isEmpty()) {//not in the database
                        String expectedMimeType = urlMap.get(url).getMimeType();
                        expectedMimeType = expectedMimeType == null ? "Not Specified" : expectedMimeType;

                        String finalRecord = report.getName();
                        String finalCollection = parentName != null ? parentName : finalRecord;

                        LinkToBeChecked linkToBeChecked = new LinkToBeChecked(url, finalRecord, finalCollection, expectedMimeType, Configuration.reportGenerationDate);

                        linksToBeChecked.add(linkToBeChecked);
                    } else {
                        linksToBeUpdated.add(url);//update the harvestDate of the link
                    }
                } catch (SQLException e) {
                    logger.error("Error when getting " + url + " from the database: " + e.getMessage());
                }
            }
//todo make these count queries directly to mysql
            //get the numbers of links directly from database instead of getting each link and adding it manually here
            try (Stream<CheckedLink> stream = Configuration.checkedLinkResource.get(Optional.of(new ACDHCheckedLinkFilter(parentName, report.getName(), Category.Broken)))) {
                numOfBrokenLinks = stream.count();
            } catch (SQLException e) {
                logger.error("Error when getting number of broken links from the database: " + e.getMessage());
            }

            try (Stream<CheckedLink> stream = Configuration.checkedLinkResource.get(Optional.of(new ACDHCheckedLinkFilter(parentName, report.getName(), Category.Undetermined)))) {
                numOfUndeterminedLinks = stream.count();
            } catch (SQLException e) {
                logger.error("Error when getting number of undetermined links from the database: " + e.getMessage());
            }

            try (Stream<CheckedLink> stream = Configuration.checkedLinkResource.get(Optional.of(new ACDHCheckedLinkFilter(parentName, report.getName(), Category.Restricted_Access)))) {
                numOfRestrictedAccessLinks = stream.count();
            } catch (SQLException e) {
                logger.error("Error when getting number of restricted access links from the database: " + e.getMessage());
            }

            try (Stream<CheckedLink> stream = Configuration.checkedLinkResource.get(Optional.of(new ACDHCheckedLinkFilter(parentName, report.getName(), Category.Blocked_By_Robots_txt)))) {
                numOfBlockedByRobotsTxtLinks = stream.count();
            } catch (SQLException e) {
                logger.error("Error when getting number of blocked by robots.txt links from the database: " + e.getMessage());
            }

            try {//update the harvest date for existing links
                Configuration.linkToBeCheckedResource.updateDate(linksToBeUpdated, Configuration.reportGenerationDate);
            } catch (SQLException e) {
                logger.error("Error when updating " + linksToBeUpdated + " to urls table: " + e.getMessage());
            }

            try {//save all links not in database in a batch to urls table
                Configuration.linkToBeCheckedResource.save(linksToBeChecked);
            } catch (SQLException e) {
                logger.error("Error when saving " + linksToBeChecked + " to urls table: " + e.getMessage());
            }

        } else {//instance mode

            HTTPLinkChecker httpLinkChecker = new HTTPLinkChecker(Configuration.TIMEOUT, Configuration.REDIRECT_FOLLOW_LIMIT, Configuration.USERAGENT);

            for (String url : urlMap.keySet()) {
                //first check if database has this url
                Optional<CheckedLink> checkedLinkOptional = null;
                CheckedLink checkedLink = new CheckedLink();
                try {
                    checkedLinkOptional = Configuration.checkedLinkResource.get(url);
                } catch (SQLException e) {
                    //error doesn't matter, treat it as if it is not in the database
                }
                if (checkedLinkOptional != null && !checkedLinkOptional.isEmpty()) {
                    checkedLink = checkedLinkOptional.get();
                } else {
                    try {
                        checkedLink = httpLinkChecker.checkLink(url);//redirect follow level is current level, because this is the first request it is set to 0
                    } catch (Exception e) {
                        checkedLink.setUrl(url);
                        //ugly hack: category enum in both projects is the same. cant use one in both because java versions dont match. need to update stormychecker to java 11
                        checkedLink.setCategory(Category.valueOf(CategoryExceptionMapper.getCategoryFromException(e, url).name()));
                        checkedLink.setMessage(e.getMessage());
                        checkedLink.setStatus(null);
                        checkedLink.setByteSize(0);
                        checkedLink.setTimestamp(new Timestamp(System.currentTimeMillis()));
                        checkedLink.setDuration(0);
                        checkedLink.setContentType(null);
                        checkedLink.setMethod("HEAD");//first HEAD request will produce the exception
                    }

                    String expectedMimeType = urlMap.get(url).getMimeType();
                    expectedMimeType = expectedMimeType == null ? "Not Specified" : expectedMimeType;
                    checkedLink.setExpectedMimeType(expectedMimeType);

                    checkedLink.setRecord(report.getName());
                }
                Category category = checkedLink.getCategory();
                if (category.equals(Category.Ok)) {
                    //do nothing
                } else if (category.equals(Category.Broken)) {
                    addMessage(Severity.ERROR, "Url: " + checkedLink.getUrl() + " Category:" + category);
                    numOfBrokenLinks++;
                } else if (category.equals(Category.Undetermined)) {
                    addMessage(Severity.WARNING, "Url: " + checkedLink.getUrl() + " Category:" + category);
                    numOfUndeterminedLinks++;
                } else if (category.equals(Category.Restricted_Access)) {
                    addMessage(Severity.WARNING, "Url: " + checkedLink.getUrl() + " Category:" + category);
                    numOfRestrictedAccessLinks++;
                } else {//blocked by robots.txt
                    addMessage(Severity.WARNING, "Url: " + checkedLink.getUrl() + " Category:" + category);
                    numOfBlockedByRobotsTxtLinks++;
                }

                CMDInstanceReport.URLElement urlElementReport = new CMDInstanceReport.URLElement().convertFromLinkCheckerURLElement(checkedLink);
                report.addURLElement(urlElementReport);


            }

        }
        report.urlReport = createInstanceURLReport(numOfLinks.get(), numOfUniqueLinks, numOfBrokenLinks, numOfUndeterminedLinks, numOfRestrictedAccessLinks, numOfBlockedByRobotsTxtLinks);

    }

    public Score calculateScore(CMDInstanceReport report) {
        // it can influence the score, if one collection was done with enabled and the other without

        double score = report.urlReport.percOfValidLinks != null && !Double.isNaN(report.urlReport.percOfValidLinks) ? report.urlReport.percOfValidLinks : 0;
        return new Score(score, 1.0, "url-validation", msgs);
    }

    private URLReport createInstanceURLReport(long numOfLinks, long numOfUniqueLinks, long numOfBrokenLinks, long numOfUndeterminedLinks, long numOfRestrictedAccessLinks, long numOfBlockedByRobotsTxtLinks) {
        URLReport report = new URLReport();
        report.numOfLinks = numOfLinks;

        //all links are checked in instances
        report.numOfCheckedLinks = numOfLinks;
        report.numOfUndeterminedLinks = numOfUndeterminedLinks;
        report.numOfRestrictedAccessLinks = numOfRestrictedAccessLinks;
        report.numOfBlockedByRobotsTxtLinks = numOfBlockedByRobotsTxtLinks;
        report.numOfUniqueLinks = numOfUniqueLinks;
        report.numOfBrokenLinks = numOfBrokenLinks;

        long numOfCheckedUndeterminedRemoved = numOfLinks - numOfUndeterminedLinks;
        report.percOfValidLinks = numOfLinks == 0 ? 0 : (numOfCheckedUndeterminedRemoved - numOfBrokenLinks) / (double) numOfCheckedUndeterminedRemoved;

        return report;
    }

    private URLReport createDisabledURLReport(long numOfLinks) {
        URLReport report = new URLReport();
        report.numOfLinks = numOfLinks;

        return report;
    }

}
