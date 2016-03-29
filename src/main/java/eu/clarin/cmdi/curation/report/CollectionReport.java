package eu.clarin.cmdi.curation.report;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import eu.clarin.cmdi.curation.xml.ScoreAdapter;

/**
 * @author dostojic
 *
 */

@XmlRootElement(name = "collection-report")
@XmlAccessorType(XmlAccessType.FIELD)
public class CollectionReport implements Report<CollectionReport> {
	
	transient public double avgFacetCoverageByInstanceSum;

	@XmlAttribute(name = "max-score-collection")
	public double maxScore;
	
	@XmlAttribute(name = "max-score-instance")
	public final double maxScoreInstance = CMDInstanceReport.MAX_SCORE;

	// report fields
	public Long timeStamp = System.currentTimeMillis();

	@XmlJavaTypeAdapter(ScoreAdapter.class)
	public Double score;

	@XmlJavaTypeAdapter(ScoreAdapter.class)
	public Double avgScore;

	@XmlElement(name = "file-section")
	FileReport fileReport = new FileReport();

	@XmlElement(name = "header-section")
	HeaderReport headerReport = new HeaderReport();

	// ResProxies
	@XmlElement(name = "resProxy-section")
	ResProxyReport resProxyReport = new ResProxyReport();

	// XMLValidator
	@XmlElement(name = "xml-validation-section")
	XMLValidationReport xmlReport = new XMLValidationReport();

	// URL
	@XmlElement(name = "url-validation-section")
	URLValidationReport urlReport = new URLValidationReport();

	// Facets
	@XmlElement(name = "facet-section")
	FacetReport facetReport = new FacetReport();

	@XmlElementWrapper(name = "invalidFilesList")
	public List<String> invalidFile = null;

	public void addNewInvalid(String file) {
		if (invalidFile == null)
			invalidFile = new LinkedList<>();

		invalidFile.add(file);

	}

	public void handleProfile(String profile) {
		if (headerReport == null)
			headerReport = new HeaderReport();
		if (headerReport.profiles == null)
			headerReport.profiles = new Profiles();
		headerReport.profiles.handleProfile(profile);
	}

	public void handleProfile(Profile profile) {
		if (headerReport.profiles == null)
			headerReport.profiles = new Profiles();
		headerReport.profiles.handleProfile(profile);
	}

	@Override
	public void mergeWithParent(CollectionReport parentReport) {

		parentReport.score += score;

		// ResProxies

		parentReport.resProxyReport.totNumOfResProxies += resProxyReport.totNumOfResProxies;
		parentReport.resProxyReport.totNumOfResProxiesWithMime += resProxyReport.totNumOfResProxiesWithMime;
		parentReport.resProxyReport.totNumOfResProxiesWithReferences += resProxyReport.totNumOfResProxiesWithReferences;

		// XMLValidator
		parentReport.xmlReport.totNumOfXMLElements += xmlReport.totNumOfXMLElements;
		parentReport.xmlReport.totNumOfXMLSimpleElements += xmlReport.totNumOfXMLSimpleElements;
		parentReport.xmlReport.totNumOfXMLEmptyElement += xmlReport.totNumOfXMLEmptyElement;

		// URL
		parentReport.urlReport.totNumOfLinks += urlReport.totNumOfLinks;
		parentReport.urlReport.totNumOfUniqueLinks += urlReport.totNumOfUniqueLinks;
		parentReport.urlReport.totNumOfResProxiesLinks += urlReport.totNumOfResProxiesLinks;
		parentReport.urlReport.totNumOfBrokenLinks += urlReport.totNumOfBrokenLinks;

		// Facet
		parentReport.avgFacetCoverageByInstanceSum += avgFacetCoverageByInstanceSum;

		// Profiles
		for (Profile p : headerReport.profiles.profiles)
			parentReport.handleProfile(p);

		// MDSelfLinks
		if (headerReport.duplicatedMDSelfLink != null && !headerReport.duplicatedMDSelfLink.isEmpty()) {

			if (parentReport.headerReport.duplicatedMDSelfLink == null) {
				parentReport.headerReport.duplicatedMDSelfLink = new LinkedList();
			}

			for (String mdSelfLink : headerReport.duplicatedMDSelfLink)
				if (!parentReport.headerReport.duplicatedMDSelfLink.contains(mdSelfLink))
					parentReport.headerReport.duplicatedMDSelfLink.add(mdSelfLink);
		}

		// invalid files
		if (invalidFile != null) {
			for (String invalid : invalidFile)
				parentReport.addNewInvalid(invalid);
		}

	}

	@Override
	public double getMaxScore() {
		return fileReport.numOfFiles * CMDInstanceReport.MAX_SCORE;
	};

	@Override
	public double calculateScore() {
		avgScore = score / fileReport.numOfFiles;		
		return score;
	}
	
	@Override
	public boolean isValid() {
		return true;
	}

	public void calculateAverageValues() {

		// file
		fileReport.avgSize = fileReport.size / fileReport.numOfFiles;

		// ResProxies
		resProxyReport.avgNumOfResProxies = (double) resProxyReport.totNumOfResProxies / fileReport.numOfFiles;
		resProxyReport.avgNumOfResProxiesWithMime = (double) resProxyReport.totNumOfResProxiesWithMime
				/ fileReport.numOfFiles;
		resProxyReport.avgNumOfResProxiesWithReferences = (double) resProxyReport.totNumOfResProxiesWithReferences
				/ fileReport.numOfFiles;

		// XMLValidator
		xmlReport.avgNumOfXMLElements = (double) xmlReport.totNumOfXMLElements / fileReport.numOfFiles;
		xmlReport.avgNumOfXMLSimpleElements = (double) xmlReport.totNumOfXMLSimpleElements / fileReport.numOfFiles;
		xmlReport.avgXMLEmptyElement = (double) xmlReport.totNumOfXMLEmptyElement / fileReport.numOfFiles;

		xmlReport.avgRateOfPopulatedElements = (double) (xmlReport.totNumOfXMLSimpleElements
				- xmlReport.totNumOfXMLEmptyElement) / fileReport.numOfFiles;

		// URL
		urlReport.avgNumOfLinks = (double) urlReport.totNumOfLinks / fileReport.numOfFiles;
		urlReport.avgNumOfUniqueLinks = (double) urlReport.totNumOfUniqueLinks / fileReport.numOfFiles;
		urlReport.avgNumOfResProxiesLinks = (double) urlReport.totNumOfResProxiesLinks / fileReport.numOfFiles;
		urlReport.avgNumOfBrokenLinks = 1.0 * (double) urlReport.totNumOfBrokenLinks / fileReport.numOfFiles;

		urlReport.avgNumOfValidLinks = (double) (urlReport.totNumOfUniqueLinks - urlReport.totNumOfBrokenLinks)
				/ fileReport.numOfFiles;

		// Facets
		if (facetReport == null)
			facetReport = new FacetReport();
		facetReport.avgFacetCoverageByInstance = (double) avgFacetCoverageByInstanceSum / fileReport.numOfFiles;

	}

	@Override
	public void marshal(OutputStream os) throws Exception {
		try {

			JAXBContext jaxbContext = JAXBContext.newInstance(CollectionReport.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			jaxbMarshaller.marshal(this, os);

		} catch (JAXBException e) {
			e.printStackTrace();
		}

	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	static class FileReport {
		String provider;
		long numOfFiles = 0;
		long size;
		long avgSize;
		long minFileSize;
		long maxFileSize;
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	static class HeaderReport {
		@XmlElementWrapper(name = "duplicatedMDSelfLinks")
		public List<String> duplicatedMDSelfLink = null;
		public Profiles profiles = null;
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	static class ResProxyReport {
		int totNumOfResProxies;
		double avgNumOfResProxies;
		int totNumOfResProxiesWithMime;
		double avgNumOfResProxiesWithMime;
		int totNumOfResProxiesWithReferences;
		double avgNumOfResProxiesWithReferences;

	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	static class XMLValidationReport {
		int totNumOfXMLElements;
		double avgNumOfXMLElements;
		int totNumOfXMLSimpleElements;
		double avgNumOfXMLSimpleElements;
		int totNumOfXMLEmptyElement;
		double avgXMLEmptyElement;
		double avgRateOfPopulatedElements;
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	static class URLValidationReport {
		int totNumOfLinks;
		double avgNumOfLinks;
		int totNumOfUniqueLinks;
		double avgNumOfUniqueLinks;
		int totNumOfResProxiesLinks;
		double avgNumOfResProxiesLinks;
		int totNumOfBrokenLinks;
		double avgNumOfBrokenLinks;
		double avgNumOfValidLinks;
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	static class FacetReport {
		double avgFacetCoverageByInstance;
	}

	@XmlRootElement(name = "profiles")
	@XmlAccessorType(XmlAccessType.FIELD)
	static class Profiles {

		@XmlAttribute(name = "count")
		public int totNumOfProfiles = 0;

		public List<Profile> profiles;

		public void handleProfile(String profile) {
			if (profiles == null)
				profiles = new LinkedList<>();

			for (Profile p : profiles)
				if (p.name.equals(profile)) {
					p.count++;
					return;
				}
			Profile p = new Profile();
			p.count = 1;
			p.name = profile;

			profiles.add(p);
			totNumOfProfiles++;
		}

		public void handleProfile(Profile profile) {
			if (profiles == null)
				profiles = new LinkedList<>();

			for (Profile p : profiles)
				if (p.name.equals(profile.name)) {
					p.count += profile.count;
					return;
				}

			profiles.add(profile);
			totNumOfProfiles++;
		}

	}

	@XmlRootElement(name = "profile")
	@XmlAccessorType(XmlAccessType.FIELD)
	static class Profile {

		@XmlAttribute
		public String name;

		@XmlAttribute
		public int count;
	}

	public void addFileReport(String provider, long numOfFiles, long size, long minSize, long maxSize) {
		fileReport.provider = provider;
		fileReport.numOfFiles = numOfFiles;
		fileReport.size = size;
		fileReport.minFileSize = minSize;
		fileReport.maxFileSize = maxSize;

	}

	public void addSelfLinks(List<String> duplicatedMDSelfLink) {
		if (headerReport == null) {
			headerReport = new HeaderReport();
			headerReport.duplicatedMDSelfLink = duplicatedMDSelfLink;
		}

	}

}