package eu.clarin.cmdi.curation.subprocessor;


import java.util.HashSet;

import java.io.IOException;

import java.util.List;
import java.util.Map;

import java.util.concurrent.ExecutionException;
import eu.clarin.cmdi.curation.entities.CMDIInstance;

import eu.clarin.cmdi.curation.report.CMDIInstanceReport;
import eu.clarin.cmdi.curation.report.FacetReport.Coverage;
import eu.clarin.cmdi.curation.vlo_extensions.FacetMappingCacheFactory;
import eu.clarin.cmdi.curation.report.Score;
import eu.clarin.cmdi.vlo.importer.mapping.FacetMapping;
import eu.clarin.cmdi.vlo.importer.processor.ValueSet;


public class CollectionInstanceFacetProcessor extends CMDISubprocessor {


    @Override
    public void process(CMDIInstance entity, CMDIInstanceReport report) throws IOException, ExecutionException {

        Map<String, List<ValueSet>> facetValuesMap = entity.getCMDIData().getDocument();
        
        /*
         * We need to know if a value is mapped to the origin facet. The facetValuesMap
         * has the name of the target facet key. When using cross facet mapping the
         * target facet is not the same as the origin facet. Therefore we extract the
         * origin facet from each the ValueSet and we can assume that for each origin
         * facet a value was mapped to this origin facet
         */
        HashSet<String> originFacetsWithValue = new HashSet<String>();      
        facetValuesMap.values().forEach(list -> list.forEach(valueSet -> originFacetsWithValue.add(valueSet.getOriginFacetConfig().getName())));

        entity.setParsedInstance(null);


        FacetMapping facetMapping;
        try {
            facetMapping = FacetMappingCacheFactory.getInstance().getFacetMapping(report.header);

            report.facets = new FacetReportCreator().createFacetReport(report.header, facetMapping);

            int numOfCoveredByIns = 0;

            for (Coverage coverage : report.facets.coverage) {
                if (!coverage.coveredByProfile)
                    continue; //we have to discuss if this should still be the case

                coverage.coveredByInstance =  originFacetsWithValue.contains(coverage.name);
                if (coverage.coveredByInstance)
                    numOfCoveredByIns++;
            }

            report.facets.instanceCoverage = report.facets.numOfFacets == 0 ? 0.0 : (numOfCoveredByIns / (double) report.facets.numOfFacets); //cast to double to get a double as result

        } finally {
            entity.setCMDIData(null);
            entity.setParsedInstance(null);
        }


    }

    public Score calculateScore(CMDIInstanceReport report) {
        return new Score(report.facets.instanceCoverage, 1.0, "facet-mapping", msgs);
    }
}
