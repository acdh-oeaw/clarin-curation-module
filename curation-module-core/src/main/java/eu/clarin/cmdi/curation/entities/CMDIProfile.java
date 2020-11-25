package eu.clarin.cmdi.curation.entities;

import eu.clarin.cmdi.curation.exception.ProfileNotFoundException;
import eu.clarin.cmdi.curation.processor.CMDIProfileProcessor;
import eu.clarin.cmdi.curation.report.CMDIProfileReport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

/**
 * Pojo of a profile file
 */

public class CMDIProfile {

    private String schemaLocation;
    private String cmdiVersion;
    protected Path path = null;

    public CMDIProfile(String schemaLocation, String cmdiVersion) {
        this.schemaLocation = schemaLocation;
        this.cmdiVersion = cmdiVersion;
    }

    public CMDIProfileReport generateReport() throws ExecutionException, ProfileNotFoundException, IOException {
        return new CMDIProfileProcessor().process(this);
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
