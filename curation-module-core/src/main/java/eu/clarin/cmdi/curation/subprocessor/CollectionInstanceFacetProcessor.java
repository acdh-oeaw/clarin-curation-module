package eu.clarin.cmdi.curation.subprocessor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import eu.clarin.cmdi.curation.entities.CMDInstance;

import eu.clarin.cmdi.curation.report.CMDInstanceReport;
import eu.clarin.cmdi.curation.report.FacetReport.Coverage;
import eu.clarin.cmdi.curation.vlo_extensions.FacetMappingCacheFactory;
import eu.clarin.cmdi.curation.report.Score;
import eu.clarin.cmdi.vlo.importer.mapping.FacetMapping;
import eu.clarin.cmdi.vlo.importer.processor.ValueSet;


public class CollectionInstanceFacetProcessor extends CMDSubprocessor {
	

	@Override
	public void process(CMDInstance entity, CMDInstanceReport report) throws Exception {
	    
	    Map<String, List<ValueSet>> facetValuesMap = entity.getCMDIData().getDocument();

        /*
         * We need to know if a value is mapped to the origin facet. The facetValuesMap
         * has the name of the target facet key. When using cross facet mapping the
         * target facet is not the same as the origin facet. Therefore we extract the
         * origin facet from each the ValueSet and we can assume that for each origin
         * facet a vlaue was mapped to this origin facet
         */
	    HashSet<String> originFacetsWithValue = new HashSet<String>();	    
	    facetValuesMap.values().forEach(list -> list.forEach(valueSet -> originFacetsWithValue.add(valueSet.getOriginFacetConfig().getName())));

		entity.setParsedInstance(null);
		
	
		FacetMapping facetMapping;
		try{
			facetMapping = FacetMappingCacheFactory.getInstance().getFacetMapping(report.header);
		
		
			report.facets = new FacetReportCreator().createFacetReport(report.header, facetMapping);
			
			int numOfCoveredByIns = 0;
			
			for(Coverage coverage : report.facets.coverage) {
			    if(!coverage.coveredByProfile)
			        continue; 
			    
			    coverage.coveredByInstance =  originFacetsWithValue.contains(coverage.name);
			    if(coverage.coveredByInstance)
			        numOfCoveredByIns++;
			}
			

			report.facets.instanceCoverage = report.facets.numOfFacets == 0? 0.0:(numOfCoveredByIns / (double)report.facets.numOfFacets); //cast to double to get a double as result


		}
		catch (Exception e) {
			throw new Exception("Unable to obtain mapping for " + entity, e);
		}
		finally{
		    entity.setCMDIData(null);
		    entity.setParsedInstance(null);
		};

	}
	
	@Override
	public Score calculateScore(CMDInstanceReport report) {
		return new Score(report.facets.instanceCoverage, 1.0, "facet-mapping", msgs);
	}
}
