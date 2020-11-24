package eu.clarin.cmdi.curation.subprocessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.ValidatorHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import eu.clarin.cmdi.curation.cr.CRService;
import eu.clarin.cmdi.curation.entities.CMDInstance;
import eu.clarin.cmdi.curation.report.CMDInstanceReport;
import eu.clarin.cmdi.curation.report.Message;
import eu.clarin.cmdi.curation.report.Score;
import eu.clarin.cmdi.curation.report.Severity;
import eu.clarin.cmdi.curation.xml.CMDErrorHandler;

/**
 * Validates instances for xml. is xml valid, are there empty elements, etc.
 */
public class XMLValidator extends CMDSubprocessor {

    static final Logger logger = LoggerFactory.getLogger(XMLValidator.class);

    int numOfXMLElements = 0;
    int numOfXMLSimpleElements = 0;
    int numOfXMLEmptyElement = 0;
    boolean valid = true;


    @Override
    public void process(CMDInstance entity, CMDInstanceReport report) throws ExecutionException, IOException, ParserConfigurationException, SAXException {

            ValidatorHandler schemaValidator = new CRService().getSchema(report.header).newValidatorHandler();
            msgs = new ArrayList<>();
            schemaValidator.setErrorHandler(new CMDErrorHandler(report, msgs));
            schemaValidator.setContentHandler(new CMDIInstanceContentHandler(entity, report));
            // setValidationFeatures(schemaValidator);
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setNamespaceAware(true);

            SAXParser parser = parserFactory.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            reader.setContentHandler(schemaValidator);
            reader.parse(new InputSource(entity.getPath().toUri().toString()));

            report.xmlPopulatedReport = new CMDInstanceReport.XMLPopulatedReport();
            report.xmlPopulatedReport.numOfXMLElements = numOfXMLElements;
            report.xmlPopulatedReport.numOfXMLSimpleElements = numOfXMLSimpleElements;
            report.xmlPopulatedReport.numOfXMLEmptyElement = numOfXMLEmptyElement;
            report.xmlPopulatedReport.percOfPopulatedElements = (numOfXMLSimpleElements - numOfXMLEmptyElement) / (double) numOfXMLSimpleElements;

            report.xmlValidityReport = new CMDInstanceReport.XMLValidityReport();
            
            for (Message m : msgs) {

                if (m.getLvl().equals(Severity.FATAL) || m.getLvl().equals(Severity.ERROR)) {
                    if(report.xmlValidityReport.issues.size() < 3)
                        report.xmlValidityReport.issues.add(m.getMessage());
                    valid = false;
                }
            }

            
            report.xmlValidityReport.valid = valid;




    }

    public Score calculateScore(CMDInstanceReport report) {

        return new Score(report.xmlPopulatedReport.percOfPopulatedElements, 1.0, "xml-populated-validation", new ArrayList<>());
    }

    //This method is extra because xmlValidator gives two seperate scores.
    //The architecture was designed for one score per validator apparently. But I inherited the code and can't deal with the whole architecture. This might bite me in the ass later but whatever.
    public Score calculateValidityScore() {
        List<Message> list = new ArrayList<>(msgs);

        Collections.sort(list, (m1, m2) -> m2.getLvl().getPriority() - m1.getLvl().getPriority());
        // we don't take into account errors and warnings from xml parser
        return new Score(valid ? 1.0 : 0.0, 1.0, "xml-validation", list);
    }

    //set custom xerces features here, if u really need them
//    private void setValidationFeatures(ValidatorHandler validator) {
//		try {
//		    validator.setFeature("http://apache.org/xml/features/warn-on-duplicate-entitydef", true);
//		    validator.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
//		    validator.setFeature("http://xml.org/sax/features/validation", true);
//		    validator.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
//		} catch (Exception e) {
//		    logger.warn("feature is not supported", e);
//		}
//    }


    class CMDIInstanceContentHandler extends DefaultHandler {

        CMDInstance instance;
        CMDInstanceReport report;

        String curElem;
        boolean elemWithValue;

        Locator locator;

        // for handling attributes
        // private TypeInfoProvider provider;
        // public CMDIContentHandler(TypeInfoProvider provider) {
        // this.provider = provider;
        // }

        public CMDIInstanceContentHandler(CMDInstance instance, CMDInstanceReport report) {
            this.instance = instance;
            this.report = report;
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        /**
         * Receive notification of the start of an element.
         */
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

            numOfXMLElements++;
            curElem = qName;

            // handle attributes
            // TypeInfo etype = provider.getElementTypeInfo();
            // //sb.append(" of type {" + etype.getTypeNamespace() + '}' +
            // etype.getTypeName());
            // for (int a=0; a<attributes.getLength(); a++) {
            // TypeInfo atype = provider.getAttributeTypeInfo(a);
            // boolean spec = provider.isSpecified(a);
            // //sb.append("Attribute " + attributes.getQName(a) + (spec ? "
            // (specified)" : (" (defaulted)")));
            // if (atype == null) {
            // //sb.append(" of unknown type");
            // } else {
            // //sb.append(" of type {" + atype.getTypeNamespace() + '}' +
            // atype.getTypeName());
            // }
            // }
        }

        /**
         * Receive notification of the end of an element.
         */
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (curElem.equals(qName)) {// is a simple elem
                numOfXMLSimpleElements++;
                if (!elemWithValue) {// does it have a value
                    numOfXMLEmptyElement++;
                    String msg = "Empty element <" + qName + "> was found on line " + locator.getLineNumber();
                    addMessage(Severity.WARNING, msg);
                }
            }

            elemWithValue = false;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            elemWithValue = true;
        }

        @Override
        public void endDocument() throws SAXException {
            //do nothing
        }
    }

}
