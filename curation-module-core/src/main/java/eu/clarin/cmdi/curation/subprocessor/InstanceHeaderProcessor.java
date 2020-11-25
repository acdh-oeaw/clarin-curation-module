package eu.clarin.cmdi.curation.subprocessor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;

import eu.clarin.cmdi.curation.cr.CRService;
import eu.clarin.cmdi.curation.entities.CMDIInstance;
import eu.clarin.cmdi.curation.main.Configuration;
import eu.clarin.cmdi.curation.report.CMDIInstanceReport;
import eu.clarin.cmdi.curation.report.Score;
import eu.clarin.cmdi.curation.report.Severity;
import eu.clarin.cmdi.vlo.importer.processor.ValueSet;


public class InstanceHeaderProcessor extends CMDISubprocessor {

    boolean missingSchema = false;
    boolean schemaInCR = false;
    boolean missingMdprofile = false;
    boolean invalidMdprofile = false;
    boolean missingMdCollectionDisplayName = false;
    boolean missingMdSelfLink = false;

    @Override
    public void process(CMDIInstance entity, CMDIInstanceReport report) throws IOException, ExecutionException {
        Map<String, List<ValueSet>> keyValuesMap = entity.getCMDIData().getDocument();

        CRService crService = new CRService();

        String schemaLocation = keyValuesMap.containsKey("curation_schemaLocation") && !keyValuesMap.get("curation_schemaLocation").isEmpty() ? keyValuesMap.get("curation_schemaLocation").get(0).getValue() : null;

        String profileIdFromSchema = null;

        if (schemaLocation != null) {
            String[] schemaLocationArray = schemaLocation.split(" ");
            schemaLocation = schemaLocationArray[schemaLocationArray.length - 1];

            profileIdFromSchema = extractProfile(schemaLocation);
        } else {
            schemaLocation = keyValuesMap.containsKey("curation_noNamespaceSchemaLocation") && !keyValuesMap.get("curation_noNamespaceSchemaLocation").isEmpty() ? keyValuesMap.get("curation_noNamespaceSchemaLocation").get(0).getValue() : null;
        }

        String mdProfile = keyValuesMap.containsKey("curation_mdProfile") && !keyValuesMap.get("curation_mdProfile").isEmpty() ? keyValuesMap.get("curation_mdProfile").get(0).getValue() : null;

        String mdCollectionDisplayName = keyValuesMap.containsKey("collection") && !keyValuesMap.get("collection").isEmpty() ? keyValuesMap.get("collection").get(0).getValue() : null;

        String mdSelfLink = keyValuesMap.containsKey("_selfLink") && !keyValuesMap.get("_selfLink").isEmpty() ? keyValuesMap.get("_selfLink").get(0).getValue() : null;

        missingSchema = (schemaLocation == null);
        missingMdprofile = (mdProfile == null);
        missingMdCollectionDisplayName = (mdCollectionDisplayName == null);
        missingMdSelfLink = (mdSelfLink == null);

        if (missingSchema && missingMdprofile)
            throw new IOException("Unable to process " + entity + ", both schema and profile are not specified");

        if (missingSchema) {
            schemaLocation = Configuration.VLO_CONFIG.getComponentRegistryProfileSchema(mdProfile);
            addMessage(Severity.ERROR, "Attribute schemaLocation is missing. " + mdProfile + " is assumed");
        } else
            schemaInCR = crService.isSchemaCRResident(schemaLocation);

        //now obsolete
/*		if (cmdVersion != null && !cmdVersion.isEmpty() && !cmdVersion.equals("1.2"))
			addMessage(Severity.WARNING, "Current CMDI version is 1.2 but this recordName is using " + cmdVersion);*/

        if (!missingMdprofile) {
            if (!keyValuesMap.get("curation_mdProfile").get(0).getValue().matches(CRService.PROFILE_ID_FORMAT)) {
                invalidMdprofile = true;
                addMessage(Severity.ERROR, "Format for value in the element /cmd:CMD/cmd:Header/cmd:MdProfile must be: clarin.eu:cr1:p_xxxxxxxxxxxxx!");
//				mdprofile = extractProfile(mdprofile);
            }

            if (profileIdFromSchema != null && !mdProfile.equals(profileIdFromSchema)) {
                invalidMdprofile = true;
                addMessage(Severity.ERROR, "ProfileId from CMD/Header/MdProfile: " + mdProfile + " and from schemaLocation: " + profileIdFromSchema + " must match!");
                mdProfile = profileIdFromSchema; //it is important to continue with ID from schemalocation otherwise cache will create extra entry
            }
        }

        if (missingMdprofile) {
            addMessage(Severity.ERROR, "Value for CMD/Header/MdProfile is missing or invalid");
        }

        if (missingMdCollectionDisplayName)
            addMessage(Severity.ERROR, "Value for CMD/Header/MdCollectionDisplayName is missing");

        if (missingMdSelfLink) {
            addMessage(Severity.ERROR, "Value for CMD/Header/MdSelfLink is missing");
        } else if (Configuration.COLLECTION_MODE) {// collect mdSelfLinks when assessing collection
            if (!CMDIInstance.mdSelfLinks.add(mdSelfLink))
                CMDIInstance.duplicateMDSelfLink.add(mdSelfLink);
        }

        // at this point profile will be processed and cached
        report.header = crService.createProfileHeader(schemaLocation, "1.x", false);
        report.fileReport.collection = mdCollectionDisplayName;

        report.profileScore = crService.getScore(report.header);

        report.addSegmentScore(new Score(report.profileScore, CRService.PROFILE_MAX_SCORE, "profiles-score", null));

    }

    public Score calculateScore(CMDIInstanceReport report) {
        double score = 0;

        //schema exists
        if (!missingSchema) {
            score++; // * weight
            if (schemaInCR)//schema comes from Component Registry
                score++;
            else
                addMessage(Severity.INFO, "Schema not from component registry. Using default schema " + Configuration.VLO_CONFIG.getComponentRegistryProfileSchema(report.header.getId()));
        }

        //mdprofile exists and in correct format
        if (!missingMdprofile && !invalidMdprofile)
            score++;
        if (!missingMdCollectionDisplayName)
            score++;
        if (!missingMdSelfLink)
            score++;

        return new Score(score, 5.0, "cmd-header-schema", msgs);
    }


    private String extractProfile(String str) {
        Matcher m = CRService.PROFILE_ID_PATTERN.matcher(str);
        return m.find() ? m.group() : null;

    }

}
