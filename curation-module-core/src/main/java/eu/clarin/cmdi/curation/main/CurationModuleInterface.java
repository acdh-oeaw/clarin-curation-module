package eu.clarin.cmdi.curation.main;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.ximpleware.VTDException;
import eu.clarin.cmdi.curation.exception.ProfileNotFoundException;
import eu.clarin.cmdi.curation.exception.FileSizeException;
import eu.clarin.cmdi.curation.report.Report;
import org.xml.sax.SAXException;

/**
 * I'm not sure why this class exists. Most of the methods are one liners. These lines could have been called directly instead.
 */
public interface CurationModuleInterface {
	
    public Report processCMDProfile(String profileId) throws ExecutionException, ProfileNotFoundException, IOException;
    
    public Report processCMDProfile(Path path) throws IOException, IOException, ExecutionException, ProfileNotFoundException;
	
	
	public Report processCMDProfile(URL schemaLocation) throws IOException, ExecutionException, ProfileNotFoundException;
	
	/*
	 * throws Exception if file doesn't exist or is invalid
	 */
	public Report processCMDInstance(Path file) throws IOException, TransformerException, FileSizeException, ExecutionException, SAXException, VTDException, ParserConfigurationException;
	
	public Report processCMDInstance(URL url) throws IOException, FileSizeException, ExecutionException, TransformerException, SAXException, VTDException, ParserConfigurationException;
	
	public Report processCollection(Path path) throws IOException;
	
	public Report aggregateReports(Collection<Report> reports);
}
