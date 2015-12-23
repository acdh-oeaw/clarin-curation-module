package eu.clarin.cmdi.curation.subprocessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ximpleware.AutoPilot;
import com.ximpleware.ParseException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;

import eu.clarin.cmdi.curation.entities.CMDIRecord;
import eu.clarin.cmdi.curation.report.Message;
import eu.clarin.cmdi.curation.report.Report;
import eu.clarin.cmdi.curation.report.Severity;

public class CMDIHeaderValidator extends CurationStep<CMDIRecord>{
	
	private static final Logger _logger = LoggerFactory.getLogger(CMDIHeaderValidator.class);
	
	private static final Pattern PROFILE_ID_PATTERN = Pattern.compile(".*(clarin.eu:cr1:p_[0-9]+).*");
	 
	private VTDNav navigator;
	
	Report report = new Report("CMDIHeaderReport");
	
	
	@Override
	public Report process(CMDIRecord entity){
		try{			
			parse(entity.getPath());		
			String profile = handleMdProfile();
			handleMdSelfLink();
			handleMdCollectionDisplyName();
			entity.setProfile(profile);
			navigator = null;
		}catch(Exception e){// we are throwing it only if file can not be parsed or profileId is missing
			report.addMessage(new Message(Severity.FATAL, e.getMessage()));
		}
		return report;
	}
	
	 
	 private void parse(Path cmdiRecord) throws Exception{
		 VTDGen parser = new VTDGen();
		 try {
			parser.setDoc(Files.readAllBytes(cmdiRecord));
			parser.parse(true);
			navigator = parser.getNav();
			parser = null;
		} catch (IOException | ParseException e) {
			throw new Exception("Errors while parsing " + cmdiRecord, e);
		}
		
	 }
	 
	 private String handleMdProfile() throws Exception{
		 //search in header first
		 String profile = xpath("/CMD/Header/MdProfile/text()");
		 if(profile == null){//not in header
			 report.addMessage(new Message(Severity.ERROR, "CMDI Record must contain CMD/Header/MdProfile tag with proile ID!"));
			 profile = extractProfileFromNameSpace();
			 if(profile != null)
				 throw new Exception("Profile can not be extracted from namespace!");
		 }		 
		 return profile;
	 }
	 
	 //try with schemaLocation/noNamespaceSchemaLocation attributes
	 private String extractProfileFromNameSpace() throws Exception{		 
		 String schema = null;		 
		 navigator.toElement(VTDNav.ROOT);
		 int index = navigator.getAttrValNS("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation");
		 if (index != -1) {
			 schema = navigator.toNormalizedString(index).split(" ")[1];
        } else {
            index = navigator.getAttrValNS("http://www.w3.org/2001/XMLSchema-instance", "noNamespaceSchemaLocation");
            if (index != -1)
                schema = navigator.toNormalizedString(index);
        }		 
        // extract profile ID
        if (schema != null) {
            Matcher m = PROFILE_ID_PATTERN.matcher(schema);
            if (m.find())
            	schema =  m.group(1);
        }        
        return schema;
	 }
	 
	 
	 private void handleMdCollectionDisplyName(){
		 String mdCollectionDisplayName = xpath("/CMD/Header/MdCollectionDisplayName/text()");
		 if(mdCollectionDisplayName == null || mdCollectionDisplayName.isEmpty())
			 report.addMessage(new Message(Severity.ERROR, "Value for MdCollectionDisplayName is missing"));	 
	 }
	 
	 private void handleMdSelfLink(){
		 String mdSelfLink = xpath("/CMD/Header/MdSelfLink/text()");
		 if(mdSelfLink == null || mdSelfLink.isEmpty())
			 report.addMessage(new Message(Severity.ERROR, "Value for MdCollectionDisplayName is missing"));
		 else{
			 if(!CMDIRecord.uniqueMDSelfLinks.add(mdSelfLink)){
				 report.addMessage(new Message(Severity.WARNING, "SelfLink: " + mdSelfLink + " is not unique"));
				 CMDIRecord.duplicateMDSelfLinks.add(mdSelfLink);
			 } 
		 } 
	 }
    
    private String xpath(String xpath){
    	String result = null;
    	try{
    		navigator.toElement(VTDNav.ROOT);
   		 	AutoPilot ap = new AutoPilot(navigator);
   		 	ap.declareXPathNameSpace("c", "http://www.clarin.eu/cmd/");
   		 	ap.selectXPath(xpath);
   		 	int index = ap.evalXPath();
   		 	if (index != -1)
   		 		result = navigator.toString(index).trim();
    	}catch(Exception e){
    		_logger.trace("Errors while performing xpath operation: {}", xpath, e);
    	}    	
    	return result;    	
    }

}
