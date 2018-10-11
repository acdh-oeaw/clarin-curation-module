package eu.clarin.cmdi.curation.entities;

import java.nio.file.Path;

import eu.clarin.cmdi.curation.processor.AbstractProcessor;
import eu.clarin.cmdi.curation.processor.CMDProfileProcessor;

/**
 * @author dostojic
 *
 */

public class CMDProfile extends CurationEntity {

	private String schemaLocation;
	private String cmdiVersion;

	public CMDProfile(String schemaLocation, String cmdiVersion) {
		super(null);
		this.schemaLocation = schemaLocation;
		this.cmdiVersion = cmdiVersion;
	}

/*	public CMDProfile(Path path, String cmdiVersion) {
		super(path);
		this.cmdiVersion = cmdiVersion;
	}	*/
	
	public String getSchemaLocation() {
		return schemaLocation;
	}

	public String getCmdiVersion() {
		return cmdiVersion;
	}

	@Override
	protected AbstractProcessor getProcessor() {
		return new CMDProfileProcessor();
	}
	
	@Override
	public String toString() {
		return "Profile: " + (path != null? path.toString() : schemaLocation);
	}

}
