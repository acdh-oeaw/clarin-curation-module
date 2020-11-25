package eu.clarin.cmdi.curation.processor;

import eu.clarin.cmdi.curation.entities.CMDICollection;
import eu.clarin.cmdi.curation.report.CollectionReport;
import eu.clarin.cmdi.curation.subprocessor.CollectionAggregator;
import eu.clarin.cmdi.curation.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor for cmdi collections, generates collection report
 */
public class CollectionProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CollectionProcessor.class);

    /**
     * generates collection report from cmdi collection
     * @param collection pojo of collection
     * @return generated collection
     */
    public CollectionReport process(CMDICollection collection) {

        long start = System.currentTimeMillis();

        CollectionReport report = new CollectionReport();
        logger.info("Started report generation for collection: " + collection.getPath());

        CollectionAggregator collectionAggregator = null;
        try {
            collectionAggregator = new CollectionAggregator();

            collectionAggregator.process(collection, report);
            report.addSegmentScore(collectionAggregator.calculateScore(report));


        } catch (Exception e) {
            logger.error("Exception when processing " + collectionAggregator.getClass().toString() + " : " + e.getMessage());
//            logger.error("here is stack trace: ",e);
            addInvalidFile(report, e);
        }

        long end = System.currentTimeMillis();
        logger.info("It took " + TimeUtils.humanizeToTime(end - start) + " to generate the report for collection: " + report.getName());

        return report;
    }

    private void addInvalidFile(CollectionReport report, Exception e) {
        CollectionReport.InvalidFile invalidFile = new CollectionReport.InvalidFile();
        invalidFile.recordName = e.getMessage();
        invalidFile.reason = e.getCause()==null?null:e.getCause().getMessage();
        report.addInvalidFile(invalidFile);
    }

}
