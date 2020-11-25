package eu.clarin.routes;

import eu.clarin.cmdi.curation.main.CurationModule;
import eu.clarin.cmdi.curation.main.Main;
import eu.clarin.cmdi.curation.report.CMDIInstanceReport;
import eu.clarin.cmdi.curation.report.CMDIProfileReport;
import eu.clarin.cmdi.curation.report.ErrorReport;
import eu.clarin.cmdi.curation.report.Report;
import eu.clarin.cmdi.curation.utils.FileNameEncoder;
import eu.clarin.cmdi.curation.utils.HTTPLinkChecker;
import eu.clarin.helpers.FileManager;
import eu.clarin.helpers.ResponseManager;
import eu.clarin.main.Configuration;
import org.apache.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.nio.file.Paths;

/**
 * This route is used to curate instances from the homepage (validator navbar button). It processes the sent instance file and returns the generated report from it.
 */
@Path("/curate")
public class Curate {

    private static final Logger logger = Logger.getLogger(Curate.class);

    @GET
    @Path("/")
    public Response getInstanceQueryParam(@QueryParam("url-input") String urlStr) {

        if (urlStr == null || urlStr.isEmpty()) {
            return ResponseManager.returnError(400, "Input URL can't be empty.");
        }

        //just to make it more user friendly ignore leading and trailing spaces
        urlStr = urlStr.trim();

        boolean url = true;

        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
            if (urlStr.startsWith("www")) {
                urlStr = "http://" + urlStr;
            } else {
                url = false;
            }
        }

        //on collection reports, there are invalid records table, which have a "validate file" button.
        //This sends the relative location of the records instead of the full url so i make up for it here.
        //See if the given parameter url-input resloves to a file location in the system
        //this is a workaround hack but i dont have time for it and want the frontend to work without problems
        //so this is the current state. if you, who are reading this have free time, by all means fix it.
        String path;
        String resultFileName;
        if (!url) {
            path = Configuration.RECORDS_PATH + "/" + urlStr;//here it is regarded as a path instead of url.
            resultFileName = urlStr.split("/")[urlStr.split("/").length-1];
            if(!FileManager.exists(path)){
                return ResponseManager.returnError(400, "Given URL is invalid");
            }//else go down and curate the file
        } else {
            resultFileName = System.currentTimeMillis() + "_" + FileNameEncoder.encode(urlStr);
            path = System.getProperty("java.io.tmpdir") + "/" + resultFileName;

            HTTPLinkChecker linkChecker = new HTTPLinkChecker();
            try {
                linkChecker.download(urlStr, new File(path));
            } catch (IOException e) {
                return ResponseManager.returnError(400, "Either the given URL is invalid or unreachable from our servers right now. Try downloading the instance and uploading it directly via drag and drop.");
            }
        }

        try {
            String content = FileManager.readFile(path);
            return curate(content, resultFileName, path);

        } catch (IOException | TransformerException | JAXBException e) {
            logger.error("There was a problem generating the report: ", e);
            return ResponseManager.returnServerError();
        }
    }

    //this is for drag and drop instance form
    @POST
    @Path("/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response postInstance(@FormDataParam("file") InputStream fileInputStream,
                                 @FormDataParam("file") FormDataContentDisposition fileMetaData) {

        try {
            String uploadFileName = fileMetaData.getFileName();
            if (uploadFileName == null || uploadFileName.isEmpty()) {
                return ResponseManager.returnError(400, "Uploaded file name can't be empty.");
            }

            String content = FileManager.readInputStream(fileInputStream);

            String resultFileName = System.currentTimeMillis() + "_" + uploadFileName;
            String tempPath = System.getProperty("java.io.tmpdir") + "/" + resultFileName;
            FileManager.writeToFile(tempPath, content);

            return curate(content, resultFileName, tempPath);
        } catch (IOException | TransformerException | JAXBException e) {
            logger.error("There was a problem generating the report: ", e);
            return ResponseManager.returnServerError();
        }
    }


    private Response curate(String content, String resultFileName, String fileLocation) throws TransformerException, JAXBException, IOException {
//        String tempPath = System.getProperty("java.io.tmpdir") + "/" + resultFileName;

        Report<?> report;
        try {
            CurationModule cm = new CurationModule();

            if (content.substring(0, 200).contains("xmlns:xs=")) {//it's a profile
                if (!content.substring(0, 200).contains("http://www.clarin.eu/cmd/1")) //but not a valid cmdi 1.2 profile
                    throw new Exception("profile has no cmdi 1.2 namespace declaration");
                else
                    report = cm.processCMDProfile(Paths.get(fileLocation).toUri().toURL());
            } else { // no profile - so processed as CMDI instance
                report = cm.processCMDInstance(Paths.get(fileLocation));
            }
        } catch (MalformedURLException e) {
            return ResponseManager.returnError(400, "Input URL is malformed.");

        } catch (Exception e) {
            logger.error("There was an exception processing the cmdi instance: " ,e);
            return ResponseManager.returnError(400, "There was a problem when processing the input. Please make sure to upload a valid cmdi file.");
        }

        if (report instanceof ErrorReport) {
            return ResponseManager.returnError(400, ((ErrorReport) report).error);
        }

        setFileLocation(report, fileLocation);

        resultFileName = resultFileName.split("\\.")[0];
        save(report, resultFileName);

        String resultURL = Configuration.BASE_URL;
        if (report instanceof CMDIProfileReport) {
            resultURL = resultURL + "profile/";
        } else if (report instanceof CMDIInstanceReport) {
            resultURL = resultURL + "instance/";
        }


        resultURL = resultURL + resultFileName + ".html";

        return ResponseManager.redirect(resultURL);
    }

    //this is needed, because the schema/record location is only known when a url is given.
    //when a file is uploaded, it is not known.
    //fileLocation can't be null because it is checked before
    private void setFileLocation(Report<?> report, String fileLocation) {

        if (report instanceof CMDIInstanceReport) {
            if (fileLocation.startsWith("http://") || fileLocation.startsWith("https://")) {
                ((CMDIInstanceReport) report).fileReport.location = fileLocation;
            } else {
                ((CMDIInstanceReport) report).fileReport.location = "Uploaded file name: " + fileLocation;
            }
        } else if (report instanceof CMDIProfileReport) {
            if (fileLocation.startsWith("http://") || fileLocation.startsWith("https://")) {
                ((CMDIProfileReport) report).header.setSchemaLocation(fileLocation);
            } else {
                ((CMDIProfileReport) report).header.setSchemaLocation("N/A");
            }
        }

    }


    //saves xml report and html representation into the file system
    private void save(Report<?> report, String resultName) throws IOException, JAXBException, TransformerException {

        String xmlPath;
        if (report instanceof CMDIProfileReport) {
            xmlPath = Configuration.OUTPUT_DIRECTORY + "/xml/profiles/";
        } else if (report instanceof CMDIInstanceReport) {
            xmlPath = Configuration.OUTPUT_DIRECTORY + "/xml/instances/";
        } else {
            throw new IOException("Result wasn't a profile or instance Report. Should not come here.");
        }

        xmlPath = xmlPath + FileNameEncoder.encode(resultName) + ".xml";
        String marshallResult = FileManager.marshall(report);
        FileManager.writeToFile(xmlPath, marshallResult);

        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslt = new StreamSource(Main.class.getResourceAsStream("/xslt/" + report.getClass().getSimpleName() + "2HTML.xsl"));

        Transformer transformer = factory.newTransformer(xslt);

        StringWriter writeBuffer = new StringWriter();

        StreamResult result = new StreamResult(writeBuffer);

        transformer.transform(new JAXBSource(JAXBContext.newInstance(report.getClass()), report), result);

        String htmlPath = null;
        if (report instanceof CMDIProfileReport) {
            htmlPath = Configuration.OUTPUT_DIRECTORY + "/html/profiles/";
        } else if (report instanceof CMDIInstanceReport) {
            htmlPath = Configuration.OUTPUT_DIRECTORY + "/html/instances/";
        }
        htmlPath = htmlPath + resultName + ".html";
        FileManager.writeToFile(htmlPath, writeBuffer.toString());

    }
}
