/**
 *
 */
package eu.clarin.cmdi.curation.processor;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import eu.clarin.cmdi.curation.entities.CMDProfile;
import eu.clarin.cmdi.curation.exception.ProfileNotFoundException;
import eu.clarin.cmdi.curation.report.CMDProfileReport;
import eu.clarin.cmdi.curation.subprocessor.ProfileElementsHandler;
import eu.clarin.cmdi.curation.subprocessor.ProfileFacetHandler;
import eu.clarin.cmdi.curation.subprocessor.ProfileHeaderHandler;
import eu.clarin.cmdi.curation.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor of cmd profiles, generates profile report
 */
public class CMDProfileProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CMDProfileProcessor.class);

    /**
     * generates cmd profile report from a cmdprofile
     * @param profile pojo of profile
     * @return generated cmd profile report
     * @throws ProfileNotFoundException profile not found
     * @throws ExecutionException
     * @throws IOException
     */
    public CMDProfileReport process(CMDProfile profile) throws ProfileNotFoundException, ExecutionException, IOException {

        long start = System.currentTimeMillis();

        CMDProfileReport report = new CMDProfileReport();
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
