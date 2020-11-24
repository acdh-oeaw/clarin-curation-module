package eu.clarin.cmdi.curation.report;

import java.io.OutputStream;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import eu.clarin.cmdi.curation.xml.XMLMarshaller;

/**
 * When there is an error during report generation, this is created instead
 */
@XmlRootElement(name = "error-report")
@XmlAccessorType(XmlAccessType.FIELD)
public class ErrorReport implements Report<CollectionReport> {

    public String name;
    public String error;

    public ErrorReport(String name, String error) {
        this.name = name;
        this.error = error;
    }

    //for JAXB
    ErrorReport() {
    }

    @Override
    public void setParentName(String parentName) {
        //dont do anything, error reports dont have parent reports
    }

    @Override
    public String getParentName() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public void toXML(OutputStream os) {
        XMLMarshaller<ErrorReport> instanceMarshaller = new
                XMLMarshaller<>(ErrorReport.class);
        instanceMarshaller.marshal(this, os);
    }

    @Override
    public void addSegmentScore(Score segmentScore) {
        throw new UnsupportedOperationException();
    }


    /*
     * when processing collection we are adding the invalid records to the collection list
     */

    @Override
    public void mergeWithParent(CollectionReport parentReport) {
        if (parentReport.file == null) {
            parentReport.file = new ArrayList<>();
        }

        CollectionReport.InvalidFile invalidFile = new CollectionReport.InvalidFile();
        invalidFile.recordName = name;
        invalidFile.reason = error;
        parentReport.addInvalidFile(invalidFile);
    }

}
