package eu.clarin.routes;

import eu.clarin.helpers.FileManager;
import eu.clarin.helpers.ResponseManager;
import eu.clarin.main.Configuration;
import org.apache.log4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * This route returns the faq text
 */
@Path("/faq")
public class Faq {

    private static final Logger logger = Logger.getLogger(Faq.class);

    @GET
    @Path("/")
    public Response getFaq() {
        try {
            String faq = FileManager.readFile(Configuration.VIEW_RESOURCES_PATH + "/html/faq.html");

            return ResponseManager.returnHTML(200, faq);
        } catch (IOException e) {
            logger.error("Error when reading faq.html: ", e);
            return ResponseManager.returnServerError();
        }
    }

    @GET
    @Path("/faq.md")
    public Response getFaqMD() {

        String faqJsonPath = Configuration.VIEW_RESOURCES_PATH + "/markdown/faq.md";
        final InputStream fileInStream;
        try {
            fileInStream = new FileInputStream(faqJsonPath);
            return ResponseManager.returnFile(200, fileInStream, "text/markdown", "faq.md");

        } catch (FileNotFoundException e) {
            logger.error("There was an error getting the faq.md file: ", e);
            return ResponseManager.returnServerError();
        }
    }
}
