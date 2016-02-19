package eu.clarin.cmdi.curation.subprocessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.ParseException;
import com.ximpleware.VTDException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;

import eu.clarin.cmdi.curation.entities.CMDIInstance;
import eu.clarin.cmdi.curation.facets.FacetConceptMappingService;
import eu.clarin.cmdi.curation.facets.Profile2FacetMap;
import eu.clarin.cmdi.curation.facets.Profile2FacetMap.Facet;
import eu.clarin.cmdi.curation.report.CMDIInstanceReport;
import eu.clarin.cmdi.curation.report.CMDIInstanceReport.FacetValues;
import eu.clarin.cmdi.curation.report.CMDIInstanceReport.Facets;
import eu.clarin.cmdi.curation.report.CMDIInstanceReport.Instance;
import eu.clarin.cmdi.curation.report.CMDIInstanceReport.Profile;
import eu.clarin.cmdi.curation.report.Severity;

/**
 * @author dostojic
 *
 */
public class CMDIFaceting extends CMDISubprocessor {

    private static final Logger _logger = LoggerFactory.getLogger(CMDIFaceting.class);

    private static final String DEFAULT_LANGUAGE = "code:und";
    private static final String FACET_LANGUAGECODE = "languageCode";

    private Map<String, List<String>> facetValues = new LinkedHashMap<>();

    private VTDNav navigator;

    @Override
    public boolean process(CMDIInstance entity, CMDIInstanceReport report) {
	try {
	    parse(entity.getPath());

	    // coverage of profile	    

	    
	    FacetConceptMappingService service;
	    Profile2FacetMap profileMap;
	    try {
		service = FacetConceptMappingService.getInstance();
		profileMap = service.getMapping(report.profile);
	    } catch (Exception e) {
		_logger.error("Unable to obtain mapping for profile {}", report.profile, e);
		return false;
	    }

	    extractFacetValues(profileMap);
	    int totalNumOfFacets = service.getTotalNumOfFacets();
	    
	    Profile profileReport = new Profile();
	    profileReport.numOfCoveredFacets = profileMap.getMappings().size();
	    profileReport.notCovered = profileMap.getNotCovered();
	    profileReport.coverage = 1.0 * profileReport.numOfCoveredFacets / totalNumOfFacets;

	    Instance instance = new Instance();

	    instance.numOfCoveredFacets = facetValues.size();
	    instance.coverage = 1.0 * facetValues.size() / totalNumOfFacets;

	    List<FacetValues> vals = new LinkedList<>();
	    List<FacetValues> missingVals = new LinkedList<>();

	    for (String facet : profileMap.getFacetNames())
		if (facetValues.containsKey(facet))
		    vals.add(new FacetValues(facet, facetValues.get(facet)));
		else
		    missingVals.add(new FacetValues(facet, null));

	    instance.facetValues = vals;
	    instance.missingValues = missingVals;

	    Facets facets = new Facets();
	    facets.numOfFacets = totalNumOfFacets;

	    facets.profile = profileReport;

	    facets.instance = instance;

	    report.facets = facets;

	    return true;

	} catch (Exception e) {
	    report.addDetail(Severity.FATAL, e.getMessage());
	    return false;
	}
    }

    private void parse(Path cmdiRecord) throws Exception {
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

    private void extractFacetValues(Profile2FacetMap map) throws VTDException {
	Collection<Facet> facetList = map.getMappings();
	for (Facet facet : facetList) {
	    boolean matchedPattern = false;
	    List<String> patterns = facet.getPatterns();
	    for (String pattern : patterns) {
		matchedPattern = matchPattern(facet, pattern, facet.getAllowMultipleValues());
		if (matchedPattern && !facet.getAllowMultipleValues()) {
		    break;
		}
	    }

	    // using fallback patterns if extraction failed
	    if (matchedPattern == false) {
		for (String pattern : facet.getFallbackPatterns()) {
		    matchedPattern = matchPattern(facet, pattern, facet.getAllowMultipleValues());
		    if (matchedPattern && !facet.getAllowMultipleValues()) {
			break;
		    }
		}
	    }
	}
    }

    /**
     * Extracts content from CMDI file for a specific facet based on a single
     * XPath expression
     *
     * @param cmdiData
     *            representation of the CMDI document
     * @param nav
     *            VTD Navigator
     * @param config
     *            facet configuration
     * @param pattern
     *            XPath expression
     * @param allowMultipleValues
     *            information if multiple values are allowed in this facet
     * @return pattern matched a node in the CMDI file?
     * @throws VTDException
     */
    private boolean matchPattern(Facet facet, String pattern, Boolean allowMultipleValues) throws VTDException {
	final AutoPilot ap = new AutoPilot(navigator);
	ap.declareXPathNameSpace("c", "http://www.clarin.eu/cmd/");
	ap.selectXPath(pattern);

	boolean matchedPattern = false;
	int index = ap.evalXPath();

	LinkedList<String> values = new LinkedList<>();

	while (index != -1) {
	    matchedPattern = true;
	    if (navigator.getTokenType(index) == VTDNav.TOKEN_ATTR_NAME) {
		// if it is an attribute you need to add 1 to the index to get
		// the right value
		index++;
	    }
	    final String value = navigator.toString(index);
	    final String languageCode = extractLanguageCode(navigator);

	    // ignore non-English language names for facet LANGUAGE_CODE
	    if (facet.getName().equals(FACET_LANGUAGECODE) && !languageCode.equals("code:eng")
		    && !languageCode.equals("code:und")) {
		index = ap.evalXPath();
		continue;
	    }

	    if (value != null && !value.isEmpty() && !values.contains(value))
		values.add(value);

	    if (!allowMultipleValues)
		break;
	    index = ap.evalXPath();
	}

	if (!values.isEmpty())
	    facetValues.put(facet.getName(), values);
	return matchedPattern;
    }

    private String extractLanguageCode(VTDNav nav) throws NavException {
	// extract language code in xml:lang if available
	Integer langAttrIndex = nav.getAttrVal("xml:lang");
	String languageCode;
	if (langAttrIndex != -1) {
	    languageCode = nav.toString(langAttrIndex).trim();
	} else {
	    return DEFAULT_LANGUAGE;
	}

	return languageCode;
    }

}
