/**
 *
 */
package eu.clarin.cmdi.curation.processor;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import eu.clarin.cmdi.curation.entities.CMDIProfile;
import eu.clarin.cmdi.curation.exception.ProfileNotFoundException;
import eu.clarin.cmdi.curation.report.CMDIProfileReport;
import eu.clarin.cmdi.curation.subprocessor.ProfileElementsHandler;
import eu.clarin.cmdi.curation.subprocessor.ProfileFacetHandler;
import eu.clarin.cmdi.curation.subprocessor.ProfileHeaderHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor of cmdi profiles, generates profile report
 */
public class CMDIProfileProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CMDIProfileProcessor.class);

    /**
     * generates cmdi profile report from a cmdprofile
     * @param profile pojo of profile
     * @return generated cmdi profile report
     * @throws ProfileNotFoundException profile not found
     * @throws ExecutionException
     * @throws IOException
     */
    public CMDIProfileReport process(CMDIProfile profile) throws ProfileNotFoundException, ExecutionException, IOException {

        long start = System.currentTimeMillis();

        CMDIProfileReport report = new CMDIProfileReport();
//        logger.info("Started report generation for profile: " + profile.getSchemaLocation());

        ProfileHeaderHandler profileHeaderHandler = new ProfileHeaderHandler();
        profileHeaderHandler.process(profile, report);
        report.addSegmentScore(profileHeaderHandler.calculateScore(report));

        ProfileElementsHandler profileElementsHandler = new ProfileElementsHandler();
        profileElementsHandler.process(report);
        report.addSegmentScore(profileElementsHandler.calculateScore(report));

        ProfileFacetHandler profileFacetHandler = new ProfileFacetHandler();
        profileFacetHandler.process(report);
        report.addSegmentScore(profileFacetHandler.calculateScore(report));

        long end = System.currentTimeMillis();
//        logger.info("It took " + TimeUtils.humanizeToTime(end - start) + " to generate the report for profile: " + profile.getSchemaLocation());

        return report;

    }

}
