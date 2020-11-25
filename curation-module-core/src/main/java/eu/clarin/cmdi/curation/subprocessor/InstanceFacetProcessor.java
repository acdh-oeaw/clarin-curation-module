package eu.clarin.cmdi.curation.subprocessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.ximpleware.*;
import eu.clarin.cmdi.curation.cr.CRService;
import eu.clarin.cmdi.curation.cr.profile_parser.CMDINode;
import eu.clarin.cmdi.curation.entities.CMDIInstance;
import eu.clarin.cmdi.curation.instance_parser.ParsedInstance;
import eu.clarin.cmdi.curation.instance_parser.ParsedInstance.InstanceNode;
import eu.clarin.cmdi.curation.main.Configuration;
import eu.clarin.cmdi.curation.report.CMDIInstanceReport;
import eu.clarin.cmdi.curation.report.Concept;
import eu.clarin.cmdi.curation.report.FacetReport.Coverage;
import eu.clarin.cmdi.curation.report.FacetReport.FacetValueStruct;
import eu.clarin.cmdi.curation.report.FacetReport.ValueNode;
import eu.clarin.cmdi.curation.vlo_extensions.FacetMappingCacheFactory;
import eu.clarin.cmdi.curation.xml.CMDIXPathService;
import eu.clarin.cmdi.curation.report.Score;
import eu.clarin.cmdi.curation.report.Severity;
import eu.clarin.cmdi.vlo.importer.mapping.FacetMapping;

import eu.clarin.cmdi.vlo.importer.processor.ValueSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceFacetProcessor extends CMDISubprocessor {

    private final static Logger logger = LoggerFactory.getLogger(InstanceFacetProcessor.class);

    @Override
    public void process(CMDIInstance entity, CMDIInstanceReport report)
            throws IOException, VTDException, ExecutionException {

        // parse instance
        CMDIXPathService xmlService = new CMDIXPathService(entity.getPath());

        VTDNav nav = xmlService.getNavigator();

        Map<Integer, ValueNode> nodesMap = getValueNodesMap(entity, report, nav);

        logger.trace("nodes map: \n{}", nodesMap);

        facetsToNodes(entity, report, nodesMap, nav);

        report.facets.values = nodesMap.values();

        double numOfCoveredByIns = report.facets.coverage.stream().filter(facet -> facet.coveredByInstance).count();
        report.facets.instanceCoverage = numOfCoveredByIns / report.facets.numOfFacets;

    }

    private Map<Integer, ValueNode> getValueNodesMap(CMDIInstance entity, CMDIInstanceReport report, VTDNav nav)
            throws ExecutionException, VTDException {
        Map<Integer, ValueNode> nodesMap = new LinkedHashMap<Integer, ValueNode>();

        Map<String, CMDINode> elements = new CRService().getParsedProfile(report.header).getElements();
        ParsedInstance parsedInstance = entity.getParsedInstance();

        AutoPilot ap = new AutoPilot(nav);

        // create value nodes
        for (InstanceNode instanceNode : parsedInstance.getNodes()) {
            if (!instanceNode.getValue().isEmpty()) {
                ValueNode val = new ValueNode();
                val.xpath = instanceNode.getXpath();
                val.value = instanceNode.getValue();

                CMDINode node = elements.get(instanceNode.getXpath().replaceAll("\\[\\d\\]", ""));
                if (node != null && node.concept != null)
                    val.concept = new Concept(node.concept.uri, node.concept.prefLabel, node.concept.status);

                // determines the index for each xpath
                ap.selectXPath(instanceNode.getXpath().replaceAll("\\w+:", ""));
                nodesMap.put(ap.evalXPath(), val);
                ap.resetXPath();
            }
        }

        return nodesMap;
    }

    private void facetsToNodes(CMDIInstance entity, CMDIInstanceReport report, Map<Integer, ValueNode> nodesMap,
                               VTDNav nav) throws IOException, ExecutionException {

        FacetMapping facetMapping = FacetMappingCacheFactory.getInstance().getFacetMapping(report.header);

        Map<String, List<ValueSet>> facetValuesMap = entity.getCMDIData().getDocument();

        /*
         * We need to know if a value is mapped to the origin facet. The facetValuesMap
         * has the name of the target facet key. When using cross facet mapping the
         * target facet is not the same as the origin facet. Therefore we extract the
         * origin facet from each the ValueSet and we can assume that for each origin
         * facet a vlaue was mapped to this origin facet
         */
        HashSet<String> originFacetsWithValue = new HashSet<String>();
        facetValuesMap.values().forEach(
                list -> list.forEach(valueSet -> originFacetsWithValue.add(valueSet.getOriginFacetConfig().getName())));

        report.facets = new FacetReportCreator().createFacetReport(report.header, facetMapping);

        int numOfCoveredByIns = 0;

        for (String facetName : Configuration.FACETS) {
            List<ValueSet> values = facetValuesMap.get(facetName);

            if (values == null) // no values from instance for this facet
                continue;

            Coverage coverage = report.facets.coverage.stream().filter(c -> c.name.equals(facetName)).findFirst()
                    .orElse(null);

            if (coverage != null) {
                // lambda expression to ignore values set by cross facet mapping
                coverage.coveredByInstance = originFacetsWithValue.contains(facetName);

                if (coverage.coveredByInstance)
                    numOfCoveredByIns++;
            }

            // in the next step the value(s) have to be mapped to the right node

            Map<Integer, List<ValueSet>> facetMap = values.stream()
                    .collect(Collectors.groupingBy(ValueSet::getVtdIndex)); // groups valueSets by vtdIndex

            for (Map.Entry<Integer, List<ValueSet>> entry : facetMap.entrySet()) {

                if (nodesMap.containsKey(entry.getKey())) { // there is a node where the xpath has the same vtdIndex
                    ValueNode node = nodesMap.get(entry.getKey());

                    // initializing node.facet if not done already:
                    if (node.facet == null)
                        node.facet = new ArrayList<FacetValueStruct>();

                    node.facet.add(createFacetValueStruct(facetName, node.value,
                            entry.getValue().stream().map(valueSet -> valueSet.getValueLanguagePair().getLeft())
                                    .collect(Collectors.joining("; ")),
                            // entry.getValue().get(0).isDerived() //assumes that a facet isn't defined as
                            // origin and derived at the same time
                            entry.getValue().stream().anyMatch(ValueSet::isDerived), // assumes that a facet isn't
                                                                                     // defined as origin and derived at
                                                                                     // the same time
                            entry.getValue().stream().anyMatch(ValueSet::isResultOfValueMapping) // assumes that a facet
                                                                                                 // isn't defined as
                                                                                                 // origin and derived
                                                                                                 // at the same time
                    ));
                }

            }
            coverage.coveredByInstance = true; // one or more values for the specific facet
            numOfCoveredByIns++;

        }

        report.facets.instanceCoverage = report.facets.numOfFacets == 0 ? 0.0
                : (numOfCoveredByIns / (double) report.facets.numOfFacets); // cast to double to get a double as result

    }

    public Score calculateScore(CMDIInstanceReport report) {
        return new Score(report.facets.instanceCoverage, 1.0, "facet-mapping", msgs);
    }

    /*
     *
     */

    private FacetValueStruct createFacetValueStruct(String facetName, String value, String normalizedValue,
            boolean isDerived, boolean usesValueMapping) {

        FacetValueStruct facetNode = new FacetValueStruct();
        facetNode.name = facetName;
        facetNode.isDerived = isDerived ? true : null;
        facetNode.usesValueMapping = usesValueMapping ? true : null;
        facetNode.normalisedValue = value.equals(normalizedValue) ? null : normalizedValue;

        if (normalizedValue != null && !normalizedValue.trim().isEmpty() && !normalizedValue.equals("--")
                && !value.equals(normalizedValue)) {
            addMessage(Severity.INFO,
                    "Normalised value for facet " + facetName + ": '" + value + "' into '" + normalizedValue + "'");
            facetNode.normalisedValue = normalizedValue;
        }

        return facetNode;

    }
}
