package eu.clarin.cmdi.curation.report;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.configuration.SystemConfiguration;

import eu.clarin.cmdi.curation.main.Configuration;

/**
 * @author dostojic
 *
 */

@XmlRootElement(name = "instance-report")
@XmlAccessorType(XmlAccessType.FIELD)
public class CMDInstanceReport implements Report<CollectionReport> {
	
	public static final double MAX_SCORE = 11;
	
	public transient boolean isValid = true;

	@XmlAttribute(name = "max-score")
	public final double maxScore = MAX_SCORE + CMDProfileReport.MAX_SCORE;

	// transient variables used in score calculation

	// file
	public transient boolean sizeExceeded = false;
	// schema
	public transient boolean schemaAvailable = false;
	public transient boolean schemaInCCR = false;

	// profile
	public transient boolean mdProfileExists = true;
	public transient boolean mdCollectionDispExists = true;
	public transient boolean mdSelfLinkExists = true;

	// for passing values
	public transient int numOfLinks = 0;
	public transient int numOfResProxiesLinks = 0;
	public transient int numOfUniqueLinks = 0;

	public transient List<Message> xmlErrors;
	
	public Long timeStamp = System.currentTimeMillis();
	
	@XmlElement(name = "score-profile")
	public Double profileScore;
	@XmlElement(name = "score-instance")
	public Double instanceScore;
	@XmlElement(name = "score-total")
	public Double totalScore;

	// sub reports **************************************

	// Header
	@XmlElement(name = "header-section")
	HeaderReport headerReport;

	// file
	@XmlElement(name = "file-section")
	public FileReport fileReport;

	// ResProxy
	@XmlElement(name = "resProxy-section")
	ResProxyReport resProxyReport;

	// XMLValidator
	@XmlElement(name = "xml-validation-section")
	XMLReport xmlReport;

	// URL
	@XmlElement(name = "url-validation-section")
	URLReport urlReport;

	// facets
	public FacetReport facets;

	@Override
	public double getMaxScore() {
		return MAX_SCORE;
	};

	@Override
	public double calculateScore() {
		instanceScore = 0.0;
		
		if(!isValid){
			return instanceScore;
		}

		// fileSize
		if (!sizeExceeded)
			instanceScore += 1; // * weight

		// schema
		double sectionScore = 0;
		if (schemaAvailable)
			sectionScore += 1; // * weight
		if (schemaInCCR)
			sectionScore += 1; // * weight

		instanceScore += sectionScore; // * schema factor

		sectionScore = 0;
		if (mdProfileExists)
			sectionScore += 1;
		if (mdCollectionDispExists)
			sectionScore += 1;
		if (mdSelfLinkExists)
			sectionScore += 1;
		instanceScore += sectionScore; // * header factor

		sectionScore = 0;

		sectionScore += resProxyReport.percOfResProxiesWithMime + resProxyReport.percOfResProxiesWithReferences;

		instanceScore += sectionScore; // * resProxy factor

		instanceScore += xmlReport.percOfPopulatedElements; // * xmlValidation factor
		//we don't take into account errors and warnings from xml parser
		
		sectionScore = 0;
		// it can influence the score, if one collection was done with enabled
		// and the other without
		if (!Double.isNaN(urlReport.percOfValidLinks)) {
			sectionScore += urlReport.percOfValidLinks;
		} // else

		instanceScore += sectionScore; // * urlValidation factor

		instanceScore += facets.instance.coverage;// *facetCoverage factor

		totalScore = instanceScore + profileScore; 
		return totalScore;

	}

	@Override
	public void mergeWithParent(CollectionReport parentReport) {

		if (!isValid) {
			parentReport.addNewInvalid(fileReport.location);
		}

		parentReport.score += totalScore;

		// ResProxies
		parentReport.resProxyReport.totNumOfResProxies += resProxyReport.numOfResProxies;
		parentReport.resProxyReport.totNumOfResProxiesWithMime += resProxyReport.numOfResProxiesWithMime;
		parentReport.resProxyReport.totNumOfResProxiesWithReferences += resProxyReport.numOfResProxiesWithReferences;

		// XMLValidator
		parentReport.xmlReport.totNumOfXMLElements += xmlReport.numOfXMLElements;
		parentReport.xmlReport.totNumOfXMLSimpleElements += xmlReport.numOfXMLSimpleElements;
		parentReport.xmlReport.totNumOfXMLEmptyElement += xmlReport.numOfXMLEmptyElement;

		// URL
		parentReport.urlReport.totNumOfLinks += urlReport.numOfLinks;
		parentReport.urlReport.totNumOfUniqueLinks += urlReport.numOfUniqueLinks;
		parentReport.urlReport.totNumOfResProxiesLinks += urlReport.numOfResProxiesLinks;
		parentReport.urlReport.totNumOfBrokenLinks += urlReport.numOfBrokenLinks;

		// Facet
		if (facets != null && facets.instance != null)
			parentReport.avgFacetCoverageByInstanceSum += facets.instance.coverage;

		parentReport.handleProfile(headerReport.profile);

	}

	@Override
	public void marshal(OutputStream os) throws Exception {
		try {

			JAXBContext jaxbContext = JAXBContext.newInstance(CMDInstanceReport.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			jaxbMarshaller.marshal(this, os);

		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean isValid() {
		return isValid;
	}

	public String getProfile() {
		return headerReport.profile;
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class FileReport {
		public String location;
		public long size;

		@XmlElementWrapper(name = "details")
		List<Message> messages = null;

		public FileReport(String location, long size, List<Message> messages) {
			this.location = location;
			this.size = size;
			this.messages = messages;
		}

		public FileReport() {
		};
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	static class HeaderReport {
		String profile;

		@XmlElementWrapper(name = "details")
		List<Message> messages = null;

		public HeaderReport(String profile, List<Message> messages) {
			this.profile = profile;
			this.messages = messages;
		}

		public HeaderReport() {
		};
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	static class ResProxyReport {
		int numOfResProxies;
		int numOfResProxiesWithMime;
		Double percOfResProxiesWithMime;
		int numOfResProxiesWithReferences;
		Double percOfResProxiesWithReferences;

		@XmlElementWrapper(name = "resourceTypes")
		List<ResourceType> resourceType;

		// @XmlElementWrapper(name = "details") List<Message> messages = null;

		public ResProxyReport(int numOfResProxies, int numOfResProxiesWithMime, double percOfResProxiesWithMime,
				int numOfResProxiesWithReferences, double percOfResProxiesWithReferences,
				List<ResourceType> resourceTypes) {
			this.numOfResProxies = numOfResProxies;
			this.numOfResProxiesWithMime = numOfResProxiesWithMime;
			this.percOfResProxiesWithMime = percOfResProxiesWithMime;
			this.numOfResProxiesWithReferences = numOfResProxiesWithReferences;
			this.percOfResProxiesWithReferences = percOfResProxiesWithReferences;
			this.resourceType = resourceTypes;
		}

		public ResProxyReport() {
		}

	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	static class ResourceType {
		@XmlAttribute
		String type;

		@XmlAttribute
		int count;

	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	static class XMLReport {
		int numOfXMLElements;
		int numOfXMLSimpleElements;
		int numOfXMLEmptyElement;
		Double percOfPopulatedElements;

		@XmlElementWrapper(name = "details")
		List<Message> messages = null;

		public XMLReport(int numOfXMLElements, int numOfXMLSimpleElements, int numOfXMLEmptyElement,
				double percOfPopulatedElements, List<Message> messages) {
			this.numOfXMLElements = numOfXMLElements;
			this.numOfXMLSimpleElements = numOfXMLSimpleElements;
			this.numOfXMLEmptyElement = numOfXMLEmptyElement;
			this.percOfPopulatedElements = percOfPopulatedElements;
			this.messages = messages;
		}

		public XMLReport() {
		}
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	static class URLReport {
		int numOfLinks;
		int numOfUniqueLinks;
		int numOfResProxiesLinks;
		int numOfBrokenLinks;
		Double percOfValidLinks;

		@XmlElementWrapper(name = "details")
		List<Message> messages = null;

		public URLReport(int numOfLinks, int numOfUniqueLinks, int numOfResProxiesLinks, int numOfBrokenLinks,
				double percOfValidLinks, List<Message> messages) {
			this.numOfLinks = numOfLinks;
			this.numOfUniqueLinks = numOfUniqueLinks;
			this.numOfResProxiesLinks = numOfResProxiesLinks;
			this.numOfBrokenLinks = numOfBrokenLinks;
			this.percOfValidLinks = percOfValidLinks;
			this.messages = messages;
		}

		public URLReport() {
		}

	}

	public void addFileReport(String path, long size, List<Message> messages) {
		fileReport = new FileReport(path, size, messages);
	}

	public void addHeaderReport(String profile, List<Message> messages) {
		headerReport = new HeaderReport(profile, messages);
	}

	public void addResProxyReport(int numOfResProxies, int numOfResProxiesWithMime, int numOfResProxiesWithReferences,
			Map<String, Integer> resourceTypeMap) {

		double percOfResProxiesWithMime = 0;
		double percOfResProxiesWithReferences = 0;

		if (numOfResProxies > 0) {
			percOfResProxiesWithMime = (double) numOfResProxiesWithMime / numOfResProxies;
			percOfResProxiesWithReferences = (double) numOfResProxiesWithReferences / numOfResProxies;
		}

		// prepare resTypes
		List<ResourceType> resourceTypes = null;
		if (!resourceTypeMap.isEmpty()) {
			resourceTypes = new LinkedList<>();
			for (Entry<String, Integer> resType : resourceTypeMap.entrySet()) {
				ResourceType res = new ResourceType();
				res.type = resType.getKey();
				res.count = resType.getValue();
				resourceTypes.add(res);
			}
		}

		resProxyReport = new ResProxyReport(numOfResProxies, numOfResProxiesWithMime, percOfResProxiesWithMime,
				numOfResProxiesWithReferences, percOfResProxiesWithReferences, resourceTypes);
	}

	public void addXmlReport(int numOfXMLElements, int numOfXMLSimpleElements, int numOfXMLEmptyElement,
			List<Message> messages) {

		double percOfPopulatedElements = (numOfXMLSimpleElements - numOfXMLEmptyElement)
				/ (double) numOfXMLSimpleElements;
		xmlReport = new XMLReport(numOfXMLElements, numOfXMLSimpleElements, numOfXMLEmptyElement,
				percOfPopulatedElements, messages);
	}

	public void addURLReport(int numOfBrokenLinks, List<Message> messages) {

		double percOfValidLinks = Double.NaN;
		if (Configuration.HTTP_VALIDATION)
			percOfValidLinks = (numOfUniqueLinks - numOfBrokenLinks) / (double) numOfUniqueLinks;
		urlReport = new URLReport(numOfLinks, numOfUniqueLinks, numOfResProxiesLinks, numOfBrokenLinks,
				percOfValidLinks, messages);
	}
}
