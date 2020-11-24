package eu.clarin.helpers;

import eu.clarin.helpers.HTMLHelpers.HtmlManipulator;
import eu.clarin.helpers.HTMLHelpers.NavbarButton;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * This class helps with repsonses going out from the web application. It is a convenience class to help with status codes, response bodies, headers etc.
 */
public final class ResponseManager {

    private static final Logger logger = Logger.getLogger(ResponseManager.class);

    public static Response returnFile(int status, InputStream fileStream, String type, String fileName) {
        return Response.status(status).entity(fileStream).type(type).header("content-disposition", "attachment; filename = " + fileName).build();
    }

    public static Response returnResponse(int status, Object entity, String mediaType) {
        return Response.status(status).entity(entity).type(mediaType).build();
    }

    public static Response returnHTML(int status, String content) {

        try {
            return Response.status(status).entity(HtmlManipulator.addContentToGenericHTML(content)).type(MediaType.TEXT_HTML).build();
        } catch (IOException e) {
            logger.error("Error reading generic.html");
            return returnServerError();
        }
    }

    public static Response returnHTML(int status, String content, NavbarButton button) {

        try {
            return Response.status(status).entity(HtmlManipulator.addContentToGenericHTML(content, button)).type(MediaType.TEXT_HTML).build();
        } catch (IOException e) {
            logger.error("Error reading generic.html");
            return returnServerError();
        }
    }



    public static Response returnError(int status, String message) {

        String error = "<h2>There was an error:</h2>";
        return returnHTML(status, error + message);
    }

    public static Response returnServerError() {
        return returnError(500, "There was a server error. Please report it on <a href='https://github.com/clarin-eric/clarin-curation-module/issues'>github</a>.");
    }

    public static Response redirect(String URL) {
        return Response.seeOther(URI.create(URL)).build();
    }

    public static Response permanentRedirect(String URL) {
        return Response.status(301).header("Location",URL).build();
    }

    public static Response returnImageResponse(int status, BufferedImage image, String extension, String mimeType) {

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, extension, baos);
            byte[] imageData = baos.toByteArray();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageData);

            return Response.status(status).entity(byteArrayInputStream).type(mimeType).build();
        } catch (IOException e) {
            logger.error("Error while sending image response.");
            return returnServerError();
        }

    }

    public static Response returnTextResponse(int status, String text, String extension) {
        return Response.status(status).entity(text).type(extension).build();
    }
}
