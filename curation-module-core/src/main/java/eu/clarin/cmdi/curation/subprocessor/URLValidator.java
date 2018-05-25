package eu.clarin.cmdi.curation.subprocessor;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import eu.clarin.cmdi.curation.entities.CMDInstance;
import eu.clarin.cmdi.curation.instance_parser.ParsedInstance;
import eu.clarin.cmdi.curation.instance_parser.ParsedInstance.InstanceNode;
import eu.clarin.cmdi.curation.io.HTTPLinkChecker;
import eu.clarin.cmdi.curation.main.Configuration;
import eu.clarin.cmdi.curation.report.CMDInstanceReport;
import eu.clarin.cmdi.curation.report.CMDInstanceReport.URLReport;
import eu.clarin.cmdi.curation.report.Score;
import eu.clarin.cmdi.curation.report.Severity;
import eu.clarin.cmdi.curation.utils.TimeUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dostojic
 */

public class URLValidator extends CMDSubprocessor {

    private final static Logger logger = LoggerFactory.getLogger(URLValidator.class);

    @Override
    public void process(CMDInstance entity, CMDInstanceReport report) {
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
            AtomicInteger numOfBrokenLinks = new AtomicInteger(0);
            links.stream().forEach(url -> {

                try {// check if URL is broken
                    int responseCode = new HTTPLinkChecker().checkLink(url, report, 0);//redirect follow level is current level, because this is the first request it is set to 0
                    if (responseCode == 200 || responseCode == 302) {
                    } // OK
                    else if (responseCode < 400) {// 2XX and 3XX, redirections, empty content
                        addMessage(Severity.WARNING, "URL: " + url + "     STATUS:" + responseCode);
                    } else {// 4XX and 5XX, client/server errors
                        numOfBrokenLinks.incrementAndGet();
                        addMessage(Severity.ERROR, "URL: " + url + "    STATUS:" + responseCode);
                    }

                } catch (HttpHostConnectException e) {
                    CMDInstanceReport.URLElement urlElement = new CMDInstanceReport.URLElement();
                    urlElement.message = "Connection refused";
                    urlElement.url = url;
                    urlElement.status = 0;
                    urlElement.contentType = null;
                    urlElement.byteSize = "0";
                    urlElement.timestamp = TimeUtils.humanizeToDate(System.currentTimeMillis());
                    urlElement.duration = "0 ms";
                    report.addURLElement(urlElement);

                    numOfBrokenLinks.incrementAndGet();
                    addMessage(Severity.ERROR, "URL: " + url + "    STATUS:" + e.toString());
                } catch (Exception e) {
                    logger.error("URLValidator shouldn't catch other exceptions, So it shouldn't come here!");
                    e.printStackTrace();
                    numOfBrokenLinks.incrementAndGet();
                    addMessage(Severity.ERROR, "URL: " + url + "    STATUS:" + e.toString());
                }
            });
            report.urlReport = createURLReport(numOfLinks, numOfBrokenLinks.get(), numOfUniqueLinks);
        } else {
            report.urlReport = createURLReport(numOfLinks, 0, numOfUniqueLinks);
            addMessage(Severity.INFO, "Link validation is disabled");
        }

    }

    @Override
    public Score calculateScore(CMDInstanceReport report) {
        // it can influence the score, if one collection was done with enabled and the other without

        double score = report.urlReport.percOfValidLinks != null && !Double.isNaN(report.urlReport.percOfValidLinks) ? report.urlReport.percOfValidLinks : 0;
        return new Score(score, 1.0, "url-validation", msgs);
    }

    private URLReport createURLReport(int numOfLinks, int numOfBrokenLinks, int numOfUniqueLinks) {
        URLReport report = new URLReport();
        report.numOfLinks = numOfLinks;
        report.numOfBrokenLinks = numOfBrokenLinks;
        report.numOfUniqueLinks = numOfUniqueLinks;
        if (Configuration.HTTP_VALIDATION) {
            report.percOfValidLinks = (numOfUniqueLinks - numOfBrokenLinks) / (double) numOfUniqueLinks;
        }
        return report;
    }

}
