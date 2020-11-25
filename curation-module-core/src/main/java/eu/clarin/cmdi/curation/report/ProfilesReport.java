package eu.clarin.cmdi.curation.report;

import eu.clarin.cmdi.curation.xml.XMLMarshaller;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * pojo of profileS report, maps to xml
 * This is the file that the profiles tab in the web module reads from
 * So general statistics of profiles in xml format
 */
@XmlRootElement(name = "profiles")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProfilesReport implements Report<ProfilesReport> {


    @XmlElement(name = "profile")
    private List<Profile> profiles = new ArrayList<Profile>();

    @Override
    public void setParentName(String parentName) {


    }

    @Override
    public String getParentName() {

        return null;
    }

    @Override
    public String getName() {

        return "ProfilesReport";
    }

    @Override
    public boolean isValid() {

        return false;
    }

    @Override
    public void addSegmentScore(Score segmentScore) {


    }

    @Override
    public void toXML(OutputStream os) {
        XMLMarshaller<ProfilesReport> instanceMarshaller = new
                XMLMarshaller<>(ProfilesReport.class);
        instanceMarshaller.marshal(this, os);
    }

    @Override
    public void mergeWithParent(ProfilesReport parentReport) {


    }

    public void addReport(Report report) {
        if (report instanceof CMDIProfileReport) {

            this.profiles.add(new Profile((CMDIProfileReport) report));

        }
    }


    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Profile {

        @XmlAttribute
        private String id;
        @XmlElement
        private String name;
        @XmlElement
        private String reportName;
        @XmlElement
        private double score;
        @XmlElement
        private double facetCoverage;
        @XmlElement
        private double percOfElementsWithConcept;

        @XmlElementWrapper(name = "facets")
        @XmlElement(name = "facet")
        private List<Facet> facets = new ArrayList<Facet>();
        @XmlElement
        private double collectionUsage;
        @XmlElement
        private double instanceUsage;

        public Profile() {

        }

        public Profile(CMDIProfileReport report) {
            this.id = report.header.getId();
            this.name = report.header.getName();
            this.reportName = report.getName();
            this.score = report.score;
            this.facetCoverage = report.facet.profileCoverage;
            this.percOfElementsWithConcept = report.elements.percWithConcept;            
            this.collectionUsage = report.collectionUsage.size();           
            
            report.collectionUsage.forEach(usage -> this.instanceUsage+=usage.count);
            report.facet.coverage.forEach(f -> facets.add(new Facet(f.name, f.coveredByProfile)));
        }
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)

    private static class Facet {
        @XmlAttribute
        private String name;
        @XmlAttribute
        private boolean covered;

        public Facet() {

        }

        public Facet(String name, boolean covered) {
            this.name = name;
            this.covered = covered;
        }
    }
}
