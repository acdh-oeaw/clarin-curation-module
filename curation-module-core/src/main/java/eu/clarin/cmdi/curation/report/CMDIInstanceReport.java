package eu.clarin.cmdi.curation.report;

import eu.clarin.cmdi.curation.cr.ProfileHeader;
import eu.clarin.cmdi.curation.report.CollectionReport.FacetCollectionStruct;
import eu.clarin.cmdi.curation.report.CollectionReport.Record;
import eu.clarin.cmdi.curation.utils.CategoryColor;
import eu.clarin.cmdi.curation.utils.TimeUtils;
import eu.clarin.cmdi.curation.xml.XMLMarshaller;
import eu.clarin.cmdi.rasa.DAO.CheckedLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.*;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Pojo of report, maps to xml
 */
@XmlRootElement(name = "instance-report")
@XmlAccessorType(XmlAccessType.FIELD)
public class CMDIInstanceReport implements Report<CollectionReport> {

    private static final Logger logger = LoggerFactory.getLogger(CMDIInstanceReport.class);

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

        @XmlAttribute(name = "category")
        public String category;

        @XmlAttribute(name = "method")
        public String method;

        @XmlAttribute(name = "message")
        public String message;

        @XmlAttribute(name = "http-status")
        public int status;

        @XmlAttribute(name = "content-type")
        public String contentType;

        @XmlAttribute(name = "expected-content-type")
        public String expectedContentType;

        @XmlAttribute(name = "byte-size")
        public String byteSize;

        @XmlAttribute(name = "request-duration")
        public String duration;//either duration in milliseconds or 'timeout'

        @XmlAttribute(name = "timestamp")
        public String timestamp;

        @XmlAttribute(name = "color-code")
        public String colorCode;

        public URLElement convertFromLinkCheckerURLElement(CheckedLink checkedLink) {
            url = checkedLink.getUrl();
            method = checkedLink.getMethod();
            status = checkedLink.getStatus();
            category = checkedLink.getCategory().name();
            contentType = checkedLink.getContentType();
            expectedContentType = checkedLink.getExpectedMimeType();
            byteSize = String.valueOf(checkedLink.getByteSize());
            duration = TimeUtils.humanizeToTime(checkedLink.getDuration());
            timestamp = TimeUtils.humanizeToDate(checkedLink.getTimestamp());
            colorCode = CategoryColor.getColor(checkedLink.getCategory());

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
    public String getParentName() {
        return parentName;
    }

    @Override
    public void setParentName(String parentName) {
        this.parentName = parentName;
    }


    @Override
    public void mergeWithParent(CollectionReport parentReport) {

        mergeWithParent(parentReport, this);
    }

    private static synchronized void mergeWithParent(CollectionReport parentReport, CMDIInstanceReport instanceReport) {
        parentReport.score += instanceReport.score;
        if (instanceReport.score > parentReport.insMaxScore)
            parentReport.insMaxScore = instanceReport.score;

        if (instanceReport.score < parentReport.insMinScore)
            parentReport.insMinScore = instanceReport.score;

        parentReport.maxPossibleScoreInstance = instanceReport.maxScore;

        // ResProxies
        parentReport.resProxyReport.totNumOfResProxies += instanceReport.resProxyReport.numOfResProxies;

        parentReport.resProxyReport.totNumOfResourcesWithMime += instanceReport.resProxyReport.numOfResourcesWithMime;
        parentReport.resProxyReport.totNumOfResProxiesWithReferences += instanceReport.resProxyReport.numOfResProxiesWithReferences;

        // XMLPopulatedValidator
        parentReport.xmlPopulatedReport.totNumOfXMLElements += instanceReport.xmlPopulatedReport.numOfXMLElements;
        parentReport.xmlPopulatedReport.totNumOfXMLSimpleElements += instanceReport.xmlPopulatedReport.numOfXMLSimpleElements;
        parentReport.xmlPopulatedReport.totNumOfXMLEmptyElement += instanceReport.xmlPopulatedReport.numOfXMLEmptyElement;

        // XMLValidator
        parentReport.xmlValidationReport.totNumOfRecords += 1;
        parentReport.xmlValidationReport.totNumOfValidRecords += instanceReport.xmlValidityReport.valid ? 1 : 0;
        if (!instanceReport.xmlValidityReport.valid) {
            Record record = new Record();
            record.name = instanceReport.fileReport.location;
            record.issues = instanceReport.xmlValidityReport.issues;
            parentReport.xmlValidationReport.record.add(record);
        }

        //will be done through database
//        parentReport.urlReport.totNumOfLinks += instanceReport.urlReport.numOfLinks;
//        parentReport.urlReport.totNumOfBrokenLinks += instanceReport.urlReport.numOfBrokenLinks;
//        parentReport.urlReport.totNumOfCheckedLinks += instanceReport.urlReport.numOfCheckedLinks;

        // Facet
        instanceReport.facets.coverage.stream()
                .filter(facet -> facet.coveredByInstance)
                .map(facet -> facet.name)
                .forEach(coveredFacet -> {
                    FacetCollectionStruct parFacet = parentReport.facetReport.facet.stream().filter(f -> f.name.equals(coveredFacet)).findFirst().orElse(null);
                    if (parFacet != null) {//in case of derived facet parFacet will be null
                        parFacet.cnt++;
                    }
                });

        parentReport.handleProfile(instanceReport.header.getId(), instanceReport.profileScore);

    }


    @Override
    public void toXML(OutputStream os) {
        XMLMarshaller<CMDIInstanceReport> instanceMarshaller = new XMLMarshaller<>(CMDIInstanceReport.class);
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
        scorePercentage = maxScore == 0.0 ? 0.0 : score / maxScore;
        if (!segmentScore.segment.equals("profiles-score"))
            instanceScore += segmentScore.score;

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
        public Collection<String> issues = new ArrayList<String>();

        public XMLValidityReport() {
        }
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class URLReport {
        public long numOfLinks;
        public long numOfUniqueLinks;
        public long numOfCheckedLinks;
        public long numOfUndeterminedLinks;
        public long numOfRestrictedAccessLinks;
        public long numOfBlockedByRobotsTxtLinks;
        public long numOfBrokenLinks;
        public Double percOfValidLinks;

        public URLReport() {
        }

    }

}
