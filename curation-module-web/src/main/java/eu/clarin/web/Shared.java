package eu.clarin.web;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import eu.clarin.cmdi.curation.cr.CRService;
import eu.clarin.cmdi.curation.cr.ProfileHeader;
import eu.clarin.cmdi.curation.entities.CMDProfile;
import eu.clarin.cmdi.curation.main.Configuration;
import eu.clarin.cmdi.curation.main.CurationModule;
import eu.clarin.cmdi.curation.report.CMDProfileReport;
import eu.clarin.cmdi.curation.report.CollectionReport;
import eu.clarin.cmdi.curation.report.ErrorReport;
import eu.clarin.cmdi.curation.report.Report;
import eu.clarin.cmdi.curation.xml.XMLMarshaller;
import eu.clarin.web.data.CollectionStatistics;
import eu.clarin.web.data.PublicProfile;
import eu.clarin.web.utils.LinkCheckerStatisticsHelper;
import eu.clarin.web.utils.StaxParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import javax.xml.stream.*;

public class Shared {

    public static Path REPORTS_FOLDER = null; //make it configurable, or use from config

    public static List<CollectionReport> collections;
    public static List<CollectionStatistics> collectionStatistics;
    public static List<PublicProfile> publicProfiles;
    public static Collection<String> facetNames;

    static final Logger _logger = LoggerFactory.getLogger(Shared.class);

    public static void init() {
        REPORTS_FOLDER = Configuration.OUTPUT_DIRECTORY.resolve("collections");
        //init facetNames

        facetNames = Configuration.FACETS;
        initPublicProfiles();
        initCollections();
        initCollectionStatistics();
        initLinkCheckerStatistics();
    }

    public static CollectionReport getCollectionReport(final String name) {
        return collections.stream().filter(c -> c.fileReport.provider.equals(name)).findFirst().get();
    }

    private static void initPublicProfiles() {

        //todo dont init if file exists, read from file if it exists
        List<ProfileHeader> profiles = (List<ProfileHeader>) new CRService().getPublicProfiles();

        publicProfiles = profiles.stream().map(p -> {//.subList(0, 10)
            Map<String, Boolean> facetMap = new LinkedHashMap<>();
            facetNames.forEach(name -> facetMap.put(name, false));
            try {
                Report genericReport = new CurationModule().processCMDProfile(p.getId());
                if (genericReport instanceof ErrorReport) {
                    _logger.error("There was an error when creating the report: " + ((ErrorReport) genericReport).getName());
                    return new PublicProfile(p.getId(), p.getName(), -1, -1, -1, facetMap);
                }

                CMDProfileReport report = (CMDProfileReport) genericReport;
                report.facet.coverage.stream().filter(f -> f.coveredByProfile).map(f -> f.name).forEach(f -> facetMap.put(f, true));

                XMLMarshaller<CMDProfileReport> marshaller = new XMLMarshaller<>(CMDProfileReport.class);

                String path = Configuration.OUTPUT_DIRECTORY + "/profiles/" + p.getId() + ".xml";
                marshaller.marshal(report, Files.newOutputStream(Paths.get(path)));

                return new PublicProfile(p.getId(), p.getName(), report.score, report.facet.profileCoverage, report.elements.percWithConcept, facetMap);
            } catch (InterruptedException e) {
                _logger.error("InterruptedException: " + e.getMessage());
                return new PublicProfile(p.getId(), p.getName(), -1, -1, -1, facetMap);
            } catch (IOException e) {
                _logger.error("IOException when saving profile: " + e.getMessage());
                return new PublicProfile(p.getId(), p.getName(), -1, -1, -1, facetMap);
            }
        }).collect(Collectors.toList());

    }

    private static void initCollections() {
        collections = new ArrayList<>();
        XMLMarshaller<CollectionReport> marshaller = new XMLMarshaller<>(CollectionReport.class);
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(REPORTS_FOLDER)) {
            for (Path path : ds) {

                try {
                    collections.add(marshaller.unmarshal(Files.newInputStream(path)));
                } catch (JAXBException | NumberFormatException e) {
                    _logger.error("Can't read from collection report: " + path + " :" + e.getMessage());
                    //keep the for loop going to read the other collections
                }

            }
        } catch (IOException e) {
            _logger.error("Can't read the collections directory: " + e.getMessage());
        }
    }

    private static void initCollectionStatistics() {
        collectionStatistics = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(REPORTS_FOLDER)) {

            for (Path path : ds) {

                _logger.info("Parsing collection with stax: " + path.getFileName());

                InputStream inputStream = Files.newInputStream(path);

                String provider = path.getFileName().toString().split("\\.")[0];

                try {
                    CollectionStatistics cs = StaxParser.handleCollectionXMLs(inputStream, provider);
                    collectionStatistics.add(cs);

                } catch (XMLStreamException e) {
                    _logger.error("XML stream exception from report: " + path + " :" + e.getMessage());
                    //keep the for loop going to read the other collections
                } catch (Exception e) {
                    _logger.error("Exception caused by report: " + path + ", cause: " + e.getCause() + ", message: " + e.getMessage() + "\n" +
                            "If the message and cause of the exception are null, it is a good idea to use e.printStackTrace() to determine the real cause.");
                    //keep the for loop going to read the other collections
                }


            }


        } catch (IOException e) {
            _logger.error("Can't read the collections directory: " + e.getMessage());
        }
    }

    private static void initLinkCheckerStatistics() {

        LinkCheckerStatisticsHelper helper = new LinkCheckerStatisticsHelper();

        String html = helper.createHTML();

        File folder = new File(Configuration.OUTPUT_DIRECTORY.toString() + "/statistics");
        folder.mkdirs();
        File statistics = new File(folder.getPath() + "/linkCheckerStatistics.html");
        try {
            Files.deleteIfExists(statistics.toPath());
        } catch (IOException e) {
            _logger.error("Problem deleting linkCheckerStatistics.html. Maybe delete it manually?");
        }

        try (PrintStream ps = new PrintStream(Files.newOutputStream(statistics.toPath()))) {

            ps.println(html);

            _logger.info("linkchecker statistics html file has been created.");
        } catch (IOException e) {
            _logger.error("Problem writing to the statistics.html");
        }
    }

}
