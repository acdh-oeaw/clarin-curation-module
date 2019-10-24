package eu.clarin.routes;


import eu.clarin.helpers.FileManager;
import eu.clarin.helpers.LinkCheckerStatisticsHelper;
import eu.clarin.helpers.ResponseManager;

import eu.clarin.main.Configuration;
import org.apache.log4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.SQLException;

@Path("/statistics")
public class Statistics {

    private static final Logger _logger = Logger.getLogger(Statistics.class);

    @GET
    @Path("/")
    public Response getStatistics() {
        try {
            _logger.info("Statistics report requested.");
            String statistics = FileManager.readFile(Configuration.OUTPUT_DIRECTORY + "/html/statistics/LinkCheckerReport.html");

            return ResponseManager.returnHTML(200, statistics, null);
        } catch (IOException e) {
            _logger.error("Error when reading linkCheckerStatistics.html: ", e);
            return ResponseManager.returnServerError();
        }
    }

    @GET
    @Path("/{collectionName}/{status}")
    public Response getStatusStatsInit(@PathParam("collectionName") String collectionName, @PathParam("status") int status) {
        _logger.info("URL Table requested for collection " + collectionName);
        String urlStatistics = null;
        try {
            urlStatistics = LinkCheckerStatisticsHelper.createURLTable(collectionName, status);
        } catch (SQLException e) {
            _logger.error("Error in statistics: "+e.getMessage());
            return ResponseManager.returnServerError();
        }
        return ResponseManager.returnHTML(200, urlStatistics, null);
    }


    @GET
    @Path("/{collectionName}/{status}/{batchCount}")
    public Response getStatusStats(@PathParam("collectionName") String collectionName, @PathParam("status") int status, @PathParam("batchCount") int batchCount) {
        _logger.info("URL batch requested with count " + batchCount + " for collection " + collectionName);
        String urlBatchStatistics = null;
        try {
            urlBatchStatistics = LinkCheckerStatisticsHelper.getHtmlRowsInBatch(collectionName, status, batchCount);
        } catch (SQLException e) {
            _logger.error("Error in statistics: "+e.getMessage());
            return ResponseManager.returnServerError();
        }
        return ResponseManager.returnResponse(200, urlBatchStatistics, null);
    }

}
