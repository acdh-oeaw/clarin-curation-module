package eu.clarin.cmdi.curation.subprocessor;

import eu.clarin.cmdi.curation.cr.CRService;
import eu.clarin.cmdi.curation.cr.ProfileHeader;
import eu.clarin.cmdi.curation.entities.CMDIProfile;
import eu.clarin.cmdi.curation.exception.ProfileNotFoundException;
import eu.clarin.cmdi.curation.main.Configuration;
import eu.clarin.cmdi.curation.report.CMDIProfileReport;
import eu.clarin.cmdi.curation.report.Message;
import eu.clarin.cmdi.curation.report.Score;
import eu.clarin.cmdi.curation.report.Severity;

import java.util.ArrayList;
import java.util.Collection;

public class ProfileHeaderHandler {

	protected Collection<Message> msgs = null;

    public void process(CMDIProfile entity, CMDIProfileReport report) throws ProfileNotFoundException {
        String schemaLocation;
        boolean isLocalFile = false;

        if (entity.getSchemaLocation() != null && !entity.getSchemaLocation().isEmpty()) {
            schemaLocation = entity.getSchemaLocation();
        } else {
            //profileId = entity.getPath().toString();
            //isLocalFile = true;
            throw new ProfileNotFoundException("can' find profile " + entity);
        }

        CRService service = new CRService();

        if (schemaLocation.startsWith(Configuration.VLO_CONFIG.getComponentRegistryRESTURL()))
            report.header = service.getPublicProfiles()
                    .stream()
                    .filter(h -> h.getSchemaLocation().equals(schemaLocation))
                    .findFirst()
                    .orElse(null);


        if (report.header == null) {
            report.header = new ProfileHeader();
            report.header.setSchemaLocation(schemaLocation);
            report.header.setCmdiVersion(entity.getCmdiVersion());


            report.header.setPublic(false);
        }

        report.header.setLocalFile(isLocalFile);

        if (!report.header.isPublic())
            addMessage(Severity.ERROR, "Profile is not public");

        if (!service.isNameUnique(report.header.getName()))
            addMessage(Severity.WARNING, "The name: " + report.header.getName() + " of the profile is not unique");

        if (!service.isDescriptionUnique(report.header.getDescription()))
            addMessage(Severity.WARNING,
                    "The description: " + report.header.getDescription() + " of the profile is not unique");
    }

    public Score calculateScore(CMDIProfileReport report) {
        double score = report.header.isPublic() ? 1.0 : 0;
        return new Score(score, 1.0, "header-section", msgs);
    }

	protected void addMessage(Severity lvl, String message) {
		if (msgs == null) {
			msgs = new ArrayList<>();
		}
		msgs.add(new Message(lvl, message));
	}

}
