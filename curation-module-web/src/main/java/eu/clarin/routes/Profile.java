package eu.clarin.routes;

import eu.clarin.helpers.FileManager;
import eu.clarin.helpers.HTMLHelpers.NavbarButton;
import eu.clarin.helpers.ResponseManager;
import eu.clarin.main.Configuration;
import org.apache.log4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Path("/profile")
public class Profile {

    private static final Logger _logger = Logger.getLogger(Profile.class);

    @GET
    @Path("/{profileName}")
    public Response getProfile(@PathParam("profileName") String profileName) {

        String[] split = profileName.split(".");
        if (split.length != 2) {
            return ResponseManager.returnError(400, "Profile name must end with either xml or html.");
        }
        String extension = split[1];

        String location;
        try {
            switch (extension) {
                case "xml":
                    location = Configuration.OUTPUT_DIRECTORY + "/xml/profiles/";
                    String profileXML = FileManager.readFile(location + profileName);
                    return ResponseManager.returnResponse(200, profileXML, MediaType.TEXT_XML);
                case "html":
                    location = Configuration.OUTPUT_DIRECTORY + "/html/profiles/";
                    String profileHTML = FileManager.readFile(location + profileName);
                    return ResponseManager.returnHTML(200, profileHTML, null);
                default:
                    return ResponseManager.returnError(400, "Profile name must end with either xml or html.");
            }
        } catch (IOException e) {
            _logger.error("There was an error reading the profile: " + profileName);
            return ResponseManager.returnError(404, "The profile " + profileName + " doesn't exist.");
        }
    }

    @GET
    @Path("/table")
    public Response getProfilesTable() {
        try {
            String profiles = FileManager.readFile(Configuration.OUTPUT_DIRECTORY + "/html/profiles/ProfilesReport.html");

            return ResponseManager.returnHTML(200, profiles, new NavbarButton("/profile/tsv", "Export as TSV"));
        } catch (IOException e) {
            _logger.error("Error when reading ProfilesReport.html: ", e);
            return ResponseManager.returnServerError();
        }

    }

    @GET
    @Path("/tsv")
    public Response getProfilesTSV() {
        String profilesTSVPath = Configuration.OUTPUT_DIRECTORY + "/tsv/profiles/ProfilesReport.tsv";

        final InputStream fileInStream;
        try {
            fileInStream = new FileInputStream(profilesTSVPath);
            return ResponseManager.returnFile(200, fileInStream, "text/tab-separated-values", "ProfilesReport.tsv");
        } catch (FileNotFoundException e) {
            _logger.error("There was an error getting the profilesReport.tsv file: ", e);
            return ResponseManager.returnServerError();
        }
    }


}
