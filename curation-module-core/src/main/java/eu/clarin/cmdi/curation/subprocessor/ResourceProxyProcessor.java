package eu.clarin.cmdi.curation.subprocessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.clarin.cmdi.curation.entities.CMDIInstance;
import eu.clarin.cmdi.curation.report.CMDIInstanceReport;
import eu.clarin.cmdi.curation.report.CMDIInstanceReport.ResProxyReport;
import eu.clarin.cmdi.curation.report.CMDIInstanceReport.ResourceType;
import eu.clarin.cmdi.curation.report.Score;
import eu.clarin.cmdi.vlo.importer.CMDIData;
import eu.clarin.cmdi.vlo.importer.Resource;
import eu.clarin.cmdi.vlo.importer.processor.ValueSet;

public class ResourceProxyProcessor extends CMDISubprocessor {

	@Override
	public void process(CMDIInstance entity, CMDIInstanceReport report) {
	    CMDIData<Map<String, List<ValueSet>>> data = entity.getCMDIData();
	    
		report.resProxyReport = new ResProxyReport();
		
		report.resProxyReport.numOfResProxies = 0;
		report.resProxyReport.numOfResourcesWithMime =  0;
		report.resProxyReport.numOfResProxiesWithReferences = 0;
		report.resProxyReport.resourceType = new ArrayList<ResourceType>();
		
		addResourceType(data.getDataResources(), report);
		addResourceType(data.getLandingPageResources(), report);
		addResourceType(data.getMetadataResources(), report);
		addResourceType(data.getSearchPageResources(), report);
		addResourceType(data.getSearchResources(), report);
			
		report.resProxyReport.percOfResourcesWithMime = report.resProxyReport.numOfResProxies > 0? 
				(double) report.resProxyReport.numOfResourcesWithMime / report.resProxyReport.numOfResProxies : 0;
		report.resProxyReport.percOfResProxiesWithReferences = report.resProxyReport.numOfResProxies > 0? 
				(double) report.resProxyReport.numOfResProxiesWithReferences / report.resProxyReport.numOfResProxies : 0;

		
	}
	
	private void addResourceType(List<Resource> resources, CMDIInstanceReport report) {
	    if(resources.isEmpty())
	        return;
	    
	    ResourceType resourceType = new ResourceType();
	    resourceType.type = resources.get(0).getType();
	    resourceType.count = resources.size();
	    
	    for(Resource resource : resources) {
            if(resource.getResourceName() != null && !resource.getResourceName().isEmpty())
                report.resProxyReport.numOfResProxiesWithReferences++;
            if(resource.getMimeType() != null && !resource.getMimeType().isEmpty())
                report.resProxyReport.numOfResourcesWithMime++;
            
            report.resProxyReport.numOfResProxies++;
	    }    
	    report.resProxyReport.resourceType.add(resourceType);  
	}

	public Score calculateScore(CMDIInstanceReport report) {
		double score = report.resProxyReport.percOfResourcesWithMime + report.resProxyReport.percOfResProxiesWithReferences;
		return new Score(score, 2.0, "cmd-res-proxy", msgs);		
	}

}
