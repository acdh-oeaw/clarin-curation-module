package eu.clarin.web.views;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.vaadin.annotations.DesignRoot;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.v7.shared.ui.label.ContentMode;
import com.vaadin.v7.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.v7.ui.VerticalLayout;

import eu.clarin.cmdi.curation.entities.CurationEntity.CurationEntityType;
import eu.clarin.cmdi.curation.main.CurationModule;
import eu.clarin.cmdi.curation.report.CMDInstanceReport;
import eu.clarin.cmdi.curation.report.Report;
import eu.clarin.web.MainUI;
import eu.clarin.web.Shared;
import eu.clarin.web.components.LinkButton;
import eu.clarin.web.utils.XSLTTransformer;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

@DesignRoot
public class ResultView extends Panel implements View {

    private enum SourceType {PROFILE_ID, URL, FILE}

    ;

    private VerticalLayout sideMenu;

    Label label;
    StreamResource xmlReport;


    XSLTTransformer transformer = new XSLTTransformer();

    public ResultView() {
        setSizeFull();
        label = new Label();
        label.setContentMode(ContentMode.HTML);

        setContent(label);

        xmlReport = new StreamResource(null, "report.xml");
        xmlReport.setMIMEType("application/xml");
        xmlReport.setCacheTime(0);

        sideMenu = new VerticalLayout();
        LinkButton export = new LinkButton("Download Report (xml)");
        sideMenu.addComponent(export);

        FileDownloader fileDownloader = new FileDownloader(xmlReport);

        fileDownloader.extend(export);

//		BrowserWindowOpener popup = new BrowserWindowOpener(xmlPopulatedReport);
//		popup.setFeatures("");
//		popup.extend(bXML);	

    }

    @Override
    public void enter(ViewChangeEvent event) {
        int first = event.getParameters().indexOf('/');
        int sec = event.getParameters().indexOf('/', first + 1);

        String curationType = event.getParameters().substring(0, first);
        String source = event.getParameters().substring(first + 1, sec);
        String value = event.getParameters().substring(sec + 1);

        SourceType sourceType = null;
        switch (source) {
            case "id":
                sourceType = SourceType.PROFILE_ID;
                break;
            case "url":
                sourceType = SourceType.URL;
                break;
            case "file":
                sourceType = SourceType.FILE;
                break;
        }

        switch (curationType) {
            case "instance":
                curate(CurationEntityType.INSTANCE, sourceType, value);
                break;
            case "profile":
                curate(CurationEntityType.PROFILE, sourceType, value);
                break;
            case "collection":
                curate(CurationEntityType.COLLECTION, sourceType, value);
                break;
        }

        ((MainUI) getUI()).setCustomMenu(sideMenu);
    }


    private void curate(CurationEntityType curationType, SourceType sourceType, String input) {
        Report r = null;
        try {
            switch (curationType) {
                case INSTANCE:
                    switch (sourceType) {
                        case FILE:
                            Path cmdiFile = Paths.get(System.getProperty("java.io.tmpdir"), input);
                            r = new CurationModule().processCMDInstance(cmdiFile);
                            if (r instanceof CMDInstanceReport)
                                ((CMDInstanceReport) r).fileReport.location = cmdiFile.getFileName().toString();
                            Files.delete(cmdiFile);
                            break;
                        case URL:
                            r = new CurationModule().processCMDInstance(new URL(input));
                            break;
                    }
                    break;
                case PROFILE:
                    switch (sourceType) {
                        case PROFILE_ID:
                            r = new CurationModule().processCMDProfile(input);
                            break;
                        case URL:
                            r = new CurationModule().processCMDProfile(new URL(input));
                            break;
                    }
                    break;
                case COLLECTION:
                    r = Shared.getCollectionReport(input);
                    break;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            r.toXML(out);


            ByteArrayOutputStream result;
            if (out.size() > 5000000) {//bigger than 5 mb
                System.out.println("Report is bigger than 5 mb, doing stax trimming.");

                result = trimURLS(out.toByteArray());

//                System.out.println(result.toString());
            } else {
                result = out;
            }


            label.setValue(transformer.transform(curationType, result.toString()));


            xmlReport.setStreamSource(new StreamSource() {
                @Override
                public InputStream getStream() {
                    return new ByteArrayInputStream(result.toByteArray());
                }
            });


        } catch (Exception e) {
            e.printStackTrace();

            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String msg = "Error while assesing " + curationType.toString().toLowerCase() + " from " + input + "!\n"
                    + errors.toString();
            label.setValue("<pre>" + msg + "</pre>");
        }
    }

    //this method trims the irl list into 50 urls to save from memory consumption when processing the xml.
    //otherwise reports can grow to 80 mbs.
    private ByteArrayOutputStream trimURLS(byte[] in) throws XMLStreamException, IOException {

        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader =
                inputFactory.createXMLEventReader(new ByteArrayInputStream(in));

        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        XMLEventWriter writer = outputFactory.createXMLEventWriter(result);

        XMLEventFactory  eventFactory = XMLEventFactory.newInstance();


        int urlCount = 0;
        boolean saveURL=false;
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event.getEventType() == XMLStreamConstants.START_ELEMENT) {//only startelement is needed because urls are empty elements


                if (event.asStartElement().getName().getLocalPart().equalsIgnoreCase("url")) {
                    if (urlCount < 50) {
                        saveURL=true;
                        writer.add(event);
                    }


                } else if (event.asStartElement().getName().getLocalPart().equalsIgnoreCase("single-url-report")) {

                    writer.add(event);
                    event = eventFactory.createAttribute
                            ("trim", "true");
                    writer.add(event);

                } else {
                    writer.add(event);
                }

            } else if (event.getEventType() == XMLStreamConstants.END_ELEMENT) {

                if (event.asEndElement().getName().getLocalPart().equalsIgnoreCase("url")) {
                    if (urlCount < 50) {
                        saveURL=false;
                        writer.add(event);
                    }
                    urlCount++;

                } else {
                    writer.add(event);
                }

            }else{
                if(urlCount<50){
                    writer.add(event);
                }else{
                    if(saveURL){
                        writer.add(event);
                    }
                }

            }

        }

        writer.flush();
        writer.close();

        //this is just to see what the xml looks like after stax transformation(its normally not saved anywhere)
//        FileOutputStream fos = null;
//        fos = new FileOutputStream(new File(path to xml file));
//        result.writeTo(fos);
//        fos.close();


        return result;
    }
}
