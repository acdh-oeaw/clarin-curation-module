package eu.clarin.routes;

import eu.clarin.helpers.FileManager;
import eu.clarin.helpers.ResponseManager;
import eu.clarin.main.Configuration;
import org.apache.log4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * This class returns the generated instance reports as html or xml.
 */
@Path("/instance")
public class Instance {

    private static final Logger logger = Logger.getLogger(Instance.class);

    @GET
    @Path("/{instanceName}")
    public Response getInstance(@PathParam("instanceName") String instanceName) {

        String[] split = instanceName.split("\\.");
        if (split.length != 2) {
            return ResponseManager.returnError(400, "Instance name must end with either '.xml' or '.html'.");
        }
        String extension = split[1];
        String location;

        try {

            switch (extension) {
                case "xml":
                    location = Configuration.OUTPUT_DIRECTORY + "/xml/instances/";
                    String instanceXML = FileManager.readFile(location + instanceName);
                    return ResponseManager.returnResponse(200, instanceXML, MediaType.TEXT_XML);
                case "html":
                    location = Configuration.OUTPUT_DIRECTORY + "/html/instances/";
                    String instanceHTML = FileManager.readFile(location + instanceName);

                    //replace to put the url based on the server (this way xml and html files are not server url dependent)
                    String xmlLink = Configuration.BASE_URL + "instance/" + split[0] + ".xml";
                    xmlLink = "<a href='"+xmlLink+"'>"+xmlLink+"</a>";
                    instanceHTML = instanceHTML.replaceFirst(Pattern.quote("selfURLPlaceHolder"), xmlLink);

                    return ResponseManager.returnHTML(200, instanceHTML);
                default:
                    return ResponseManager.returnError(400, "Instance name must end with either '.xml' or '.html'.");
            }
        } catch (IOException e) {
            logger.error("There was an error reading the instance: " + instanceName, e);
            return ResponseManager.returnError(404, "The instance " + instanceName + " doesn't exist.");
        }

    }
}
