package eu.clarin.cmdi.curation.report;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.*;

import eu.clarin.cmdi.curation.cr.ProfileHeader;
import eu.clarin.cmdi.curation.main.Configuration;
import eu.clarin.cmdi.curation.report.CollectionReport.FacetCollectionStruct;
import eu.clarin.cmdi.curation.utils.TimeUtils;
import eu.clarin.cmdi.curation.xml.XMLMarshaller;

/**
 * @author dostojic
 */

@XmlRootElement(name = "instance-report")
@XmlAccessorType(XmlAccessType.FIELD)
public class CMDInstanceReport implements Report<CollectionReport> {

    public String parentName;

    @XmlAttribute
    public Double score = 0.0;

    @XmlAttribute(name = "ins-score")
    public Double instanceScore = 0.0;

    @XmlAttribute(name = "pfl-score")
    public Double profileScore = 0.0;

    @XmlAttribute(name = "max-score")
    public double maxScore;

    @XmlAttribute(name = "score-percentage")
    public double scorePercentage;

    @XmlAttribute
    public String timeStamp = TimeUtils.humanizeToDate(System.currentTimeMillis());

    // sub reports **************************************

    // Header
    @XmlElement(name = "profile-section")
    public ProfileHeader header;
    
    // Transformation section
    @XmlElement(name = "transformation-section")
    public TransformationReport transformationReport;

    // file
    @XmlElement(name = "file-section")
    public FileReport fileReport;

    // ResProxy
    @XmlElement(name = "resProxy-section")
    public ResProxyReport resProxyReport;

    // XMLPopulatedValidator
    @XmlElement(name = "xml-populated-section")
    public XMLPopulatedReport xmlPopulatedReport;

    // XMLValidityValidator
    @XmlElement(name = "xml-validation-section")
    public XMLValidityReport xmlValidityReport;

    // URL
    @XmlElement(name = "url-validation-section")
    public URLReport urlReport;

    // facets
    @XmlElement(name = "facets-section")
    public FacetReport facets;

    //scores
    @XmlElementWrapper(name = "score-section")
    @XmlElement(name = "score")
    public Collection<Score> segmentScores;

    // URLs
    @XmlElementWrapper(name = "single-url-report")
    public Collection<URLElement> url;

    @XmlRootElement
    public static class URLElement {
        @XmlValue
        public String url;

        @XmlAttribute(name = "method")
        public String method;

        @XmlAttribute(name = "message")
        public String message;

        @XmlAttribute(name = "http-status")
        public int status;

        @XmlAttribute(name = "content-type")
        public String contentType;

        @XmlAttribute(name = "byte-size")
        public String byteSize;

        @XmlAttribute(name = "request-duration")
        public String duration;//either duration in milliseconds or 'timeout'

        @XmlAttribute(name = "timestamp")
        public String timestamp;

        @XmlAttribute(name = "redirectCount")
        public int redirectCount;

        public URLElement convertFromLinkCheckerURLElement(eu.clarin.curation.linkchecker.urlElements.URLElement urlElement) {
            url = urlElement.getUrl();
            method = urlElement.getMethod();
            message = urlElement.getMessage();
            status = urlElement.getStatus();
            contentType = urlElement.getContentType();
            byteSize = urlElement.getByteSize();
            duration = TimeUtils.humanizeToTime(urlElement.getDuration());
            timestamp = TimeUtils.humanizeToDate(urlElement.getTimestamp());
            redirectCount = urlElement.getRedirectCount();

            return this;
        }
    }

    public void addURLElement(URLElement urlElementToBeAdded) {
        if (this.url == null) {
            this.url = new ArrayList<>();
        }
        this.url.add(urlElementToBeAdded);
    }

    @Override
    public String getName() {
        if (fileReport.location != null && fileReport.location.contains(".xml")) {
            String normalisedPath = fileReport.location.replace('\\', '/');
            return normalisedPath.substring(normalisedPath.lastIndexOf('/') + 1, normalisedPath.lastIndexOf('.'));
        } else {
            return fileReport.location;
        }

    }

    @Override
    public String getParentName(){
        return parentName;
    }

    @Override
    public void setParentName(String parentName){
        this.parentName=parentName;
    }


    @Override
    public void mergeWithParent(CollectionReport parentReport) {

        parentReport.score += score;
        if (score > parentReport.insMaxScore)
            parentReport.insMaxScore = score;

        if (score < parentReport.insMinScore)
            parentReport.insMinScore = score;

        parentReport.maxPossibleScoreInstance = maxScore;

        // ResProxies
        parentReport.resProxyReport.totNumOfResProxies += resProxyReport.numOfResProxies;

        parentReport.resProxyReport.totNumOfResourcesWithMime += resProxyReport.numOfResourcesWithMime;
        parentReport.resProxyReport.totNumOfResProxiesWithReferences += resProxyReport.numOfResProxiesWithReferences;

        // XMLPopulatedValidator
        parentReport.xmlPopulatedReport.totNumOfXMLElements += xmlPopulatedReport.numOfXMLElements;
        parentReport.xmlPopulatedReport.totNumOfXMLSimpleElements += xmlPopulatedReport.numOfXMLSimpleElements;
        parentReport.xmlPopulatedReport.totNumOfXMLEmptyElement += xmlPopulatedReport.numOfXMLEmptyElement;

        // XMLValidator
        parentReport.xmlValidationReport.totNumOfRecords += 1;
        parentReport.xmlValidationReport.totNumOfValidRecords += xmlValidityReport.valid ? 1 : 0;
        if (!xmlValidityReport.valid) {
            parentReport.xmlValidationReport.record.add(this.getName());
        }

        if (Configuration.HTTP_VALIDATION) {
            parentReport.urlReport.totNumOfLinks += urlReport.numOfLinks;
            parentReport.urlReport.totNumOfUniqueLinks += urlReport.numOfUniqueLinks;
            parentReport.urlReport.totNumOfBrokenLinks += urlReport.numOfBrokenLinks;
            parentReport.urlReport.totNumOfCheckedLinks += urlReport.numOfCheckedLinks;
        }

        // Facet
        facets.coverage.stream()
                .filter(facet -> facet.coveredByInstance)
                .map(facet -> facet.name)
                .forEach(coveredFacet -> {
                    FacetCollectionStruct parFacet = parentReport.facetReport.facet.stream().filter(f -> f.name.equals(coveredFacet)).findFirst().orElse(null);
                    if (parFacet != null) {//in case of derived facet parFacet will be null
                        parFacet.cnt++;
                    }
                });

        parentReport.handleProfile(header.id, profileScore);

        // urls
        if (this.url != null) {
            if (parentReport.url == null)
                parentReport.url = new ArrayList<>();
            parentReport.url.addAll(this.url);
        }

    }

    @Override
    public void toXML(OutputStream os) throws Exception {
        XMLMarshaller<CMDInstanceReport> instanceMarshaller = new XMLMarshaller<>(CMDInstanceReport.class);
        instanceMarshaller.marshal(this, os);
    }

    @Override
    public boolean isValid() {
        return segmentScores.stream().filter(Score::hasFatalMsg).findFirst().orElse(null) == null;
    }

    @Override
    public void addSegmentScore(Score segmentScore) {
        if (segmentScores == null)
            segmentScores = new ArrayList<>();

        segmentScores.add(segmentScore);
        maxScore += segmentScore.maxScore;
        score += segmentScore.score;
        scorePercentage = score / maxScore;
        if (!segmentScore.segment.equals("profiles-score"))
            instanceScore += segmentScore.score;

    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class TransformationReport {

        public TransformationReport() {
        }
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FileReport {
        public String location;
        public long size;
        public String collection;

        public FileReport() {
        }
    }


    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ResProxyReport {
        public int numOfResProxies;
        public int numOfResourcesWithMime;
        public Double percOfResourcesWithMime;
        public int numOfResProxiesWithReferences;
        public Double percOfResProxiesWithReferences;

        @XmlElementWrapper(name = "resourceTypes")
        public Collection<ResourceType> resourceType;

        public ResProxyReport() {
        }

    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ResourceType {
        @XmlAttribute
        public String type;

        @XmlAttribute
        public int count;

    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class XMLPopulatedReport {
        public int numOfXMLElements;
        public int numOfXMLSimpleElements;
        public int numOfXMLEmptyElement;
        public Double percOfPopulatedElements;

        public XMLPopulatedReport() {
        }
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class XMLValidityReport {
        public boolean valid;

        public XMLValidityReport() {
        }
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class URLReport {
        public int numOfLinks;
        public int numOfUniqueLinks;
        public int numOfCheckedLinks;
        public int numOfBrokenLinks;
        public Double percOfValidLinks;

        public URLReport() {
        }

    }

}
