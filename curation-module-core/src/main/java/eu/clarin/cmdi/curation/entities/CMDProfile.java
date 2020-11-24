package eu.clarin.cmdi.curation.entities;

import eu.clarin.cmdi.curation.exception.ProfileNotFoundException;
import eu.clarin.cmdi.curation.processor.CMDProfileProcessor;
import eu.clarin.cmdi.curation.processor.CollectionProcessor;
import eu.clarin.cmdi.curation.report.CMDProfileReport;
import eu.clarin.cmdi.curation.report.Report;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

/**
 * Pojo of a profile file
 */

public class CMDProfile {

    private String schemaLocation;
    private String cmdiVersion;
    protected Path path = null;

    public CMDProfile(String schemaLocation, String cmdiVersion) {
        this.schemaLocation = schemaLocation;
        this.cmdiVersion = cmdiVersion;
    }

    public CMDProfileReport generateReport() throws ExecutionException, ProfileNotFoundException, IOException {
        return new CMDProfileProcessor().process(this);
    }

    public String getCmdiVersion() {
        return cmdiVersion;
    }

    public String getSchemaLocation() {
        return this.schemaLocation;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "Profile: " + (path != null ? path.toString() : schemaLocation);
    }

}
