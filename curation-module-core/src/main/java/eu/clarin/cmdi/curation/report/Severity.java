package eu.clarin.cmdi.curation.report;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Severity types for errors and warnings etc.
 */
@XmlType
@XmlEnum
public enum Severity {
	DEBUG(0, "DEBUG"),
	INFO(1, "INFO"), 
	WARNING(2, "WARNING"),
	ERROR(3, "ERROR"),
	FATAL(4, "FATAL"),
	NONE(5, "NONE");
	
    
    final int priority;
    final String label;
    
	private Severity(int priority, String label) {
		this.priority = priority;
		this.label = label;
	}


	public int getPriority() {
		return priority;
	}
}
