package eu.clarin.routes;

import eu.clarin.helpers.ResponseManager;
import eu.clarin.main.Configuration;
import org.apache.log4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * This class returns the database dump used by the vlo. It is only the status table from the database.
 */
@Path("/mysqlDump.sql.gz")
public class MysqlDump {

    private static final Logger logger = Logger.getLogger(MysqlDump.class);

    @GET
    @Path("/")
    public Response getDump(){

        try {
            String dumpPath = Configuration.OUTPUT_DIRECTORY+"/dump/status.sql.gz";
            final InputStream fileInStream = new FileInputStream(dumpPath);

            return ResponseManager.returnFile(200,fileInStream,"application/gzip","mysqlDump.sql.gz");

        } catch (FileNotFoundException e) {
            logger.error("There was an error getting the mysql dump file: ",e);
            return ResponseManager.returnServerError();
        }

    }

}
