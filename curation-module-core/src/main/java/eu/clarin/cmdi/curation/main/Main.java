package eu.clarin.cmdi.curation.main;

import eu.clarin.cmdi.curation.cr.ProfileHeader;
import eu.clarin.cmdi.curation.cr.PublicProfiles;
import eu.clarin.cmdi.curation.entities.CurationEntityType;
import eu.clarin.cmdi.curation.exception.UncaughtExceptionHandler;
import eu.clarin.cmdi.curation.report.CMDProfileReport;
import eu.clarin.cmdi.curation.report.CollectionReport;
import eu.clarin.cmdi.curation.report.CollectionsReport;
import eu.clarin.cmdi.curation.report.LinkCheckerReport;
import eu.clarin.cmdi.curation.report.ProfilesReport;
import eu.clarin.cmdi.curation.report.Report;
import eu.clarin.cmdi.curation.utils.FileNameEncoder;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Starter class of the program
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * Reads the cli parameters and then executes the report generation
     * @param args cli parameters
     * @throws Exception all the exceptions...something went wrong during the run, check the cause
     */
    public static void main(String[] args) throws Exception {

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());//to log uncaught exceptions

        CommandLineParser parser = new DefaultParser();

        Options helpOptions = createHelpOption();

        Options options = createOptions();

        CommandLine cmd = null;

        try {
            cmd = parser.parse(helpOptions, args);
            if (cmd.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("curation module", options);
                return;

                /*
                 usage: curation module
                 -c                 curate a collection
                 -config <file>     a path to the configuration file
                 -i                 curate an instance
                 -id <profilesId>   Space separated CLARIN profile IDs in format:
                                    clarin.eu:cr1:p_xxx
                 -p                 curate a profile
                 -path <path>       Space separated paths to file or folder to be curated
                 -url <url>         Space separated urls to profile or instance to be
                                    curated
                * */
            }
        } catch (org.apache.commons.cli.ParseException e) {
            //do nothing
        }

        cmd = parser.parse(options, args);

        // init configuration file
        if (cmd.hasOption("config")) {
            Configuration.init(cmd.getOptionValue("config"));
        } else {
            Configuration.initDefault();
        }

        Configuration.enableProfileLoadTimer = false;

        CurationModule curator = new CurationModule();
        CurationEntityType type = null;

        if (cmd.hasOption("p")) {// profile
            type = CurationEntityType.PROFILE;
            if (cmd.hasOption("id")) {
                Configuration.OUTPUT_DIRECTORY = null;
                for (String id : cmd.getOptionValues("id"))
                    dumpAsXML(curator.processCMDProfile(id), type);
            } else if (cmd.hasOption("url")) {
                Configuration.OUTPUT_DIRECTORY = null;
                for (String url : cmd.getOptionValues("url"))
                    dumpAsXML(curator.processCMDProfile(new URL(url)), type);
            } else {
                throw new Exception("Only id and url options are allowed for profiles curation");
            }
        } else if (cmd.hasOption("i")) {// instance
            type = CurationEntityType.INSTANCE;
            if (cmd.hasOption("url")) {
                for (String url : cmd.getOptionValues("url"))
                    dumpAsXML(curator.processCMDInstance(new URL(url)), type);
            } else if (cmd.hasOption("path")) {
                for (String path : cmd.getOptionValues("path"))
                    dumpAsXML(curator.processCMDInstance(Paths.get(path)), type);
            } else
                throw new Exception("Only path and url options are allowed for instances curation");

        } else if (cmd.hasOption("c")) {// collection
            type = CurationEntityType.COLLECTION;
            Configuration.COLLECTION_MODE = true;
            if (cmd.hasOption("path")) {
                Report<?> report;

                for (String path : cmd.getOptionValues("path")) {
                    report = curator.processCollection(Paths.get(path));

                    dumpAsXML(report, type);
                    dumpAsHTML(report, type);
                }
            } else
                throw new Exception("Only path is allowed for collection curation");
        } else if (cmd.hasOption("r")) {// public profiles and collections
            Configuration.COLLECTION_MODE = true;

            // generating reports for public profiles
            // reports are stored in a map to add profile usage in the next step
            Map<String, Report<?>> profileReports = new HashMap<String, Report<?>>();

            for (ProfileHeader pHeader : PublicProfiles.createPublicProfiles()) {
                profileReports.put(pHeader.getId(), curator.processCMDProfile(pHeader.getId()));
            }

            Report<?> report;
            if (cmd.hasOption("path")) {

                CollectionsReport collectionsReport = new CollectionsReport();
                LinkCheckerReport linkCheckerReport = new LinkCheckerReport();

                logger.info("Processing collections: Generating reports...");

                for (String path : cmd.getOptionValues("path")) {
                    //dump(curator.processCollection(Paths.get(path)), type);
                    for (File file : new File(path).listFiles()) {
//                        logger.info("Starting report generation for collection: " + file.toPath());
                        report = curator.processCollection(file.toPath());

                        dumpAsXML(report, CurationEntityType.COLLECTION);
                        dumpAsHTML(report, CurationEntityType.COLLECTION);

                        collectionsReport.addReport(report);
                        linkCheckerReport.addReport(report);

                        if (report instanceof CollectionReport) { //no ErrorReport
                            CollectionReport collectionReport = (CollectionReport) report;

                            for (CollectionReport.Profile profile : collectionReport.headerReport.profiles.profiles) {
                                profileReports.computeIfPresent(profile.name, (key, cmdProfileReport) -> {
                                    if (cmdProfileReport instanceof CMDProfileReport)
                                        ((CMDProfileReport) cmdProfileReport).addCollectionUsage(collectionReport.fileReport.provider, profile.count);
                                    return cmdProfileReport;
                                });
                            }
                        }

                    }
                }
                logger.info("Creating collections table...");

                // dumping the collections table
                dumpAsXML(collectionsReport, CurationEntityType.COLLECTION);
                dumpAsHTML(collectionsReport, CurationEntityType.COLLECTION);
                dumpAsTSV(collectionsReport, CurationEntityType.COLLECTION);

                logger.info("Creating collections table finished.");

                logger.info("Creating profiles table...");

                //now dumping the public profile reports
                ProfilesReport profilesReport = new ProfilesReport();

                for (Report<?> cmdProfileReport : profileReports.values()) {
                    profilesReport.addReport(cmdProfileReport);
                    dumpAsXML(cmdProfileReport, CurationEntityType.PROFILE);
                    dumpAsHTML(cmdProfileReport, CurationEntityType.PROFILE);
                }

                //dumping the profiles table
                dumpAsXML(profilesReport, CurationEntityType.PROFILE);
                dumpAsHTML(profilesReport, CurationEntityType.PROFILE);
                dumpAsTSV(profilesReport, CurationEntityType.PROFILE);
                logger.info("Creating profiles table finished..");

                logger.info("Creating statistics table...");
                //dumping the linkchecker statistics table
                dumpAsXML(linkCheckerReport, CurationEntityType.STATISTICS);
                dumpAsHTML(linkCheckerReport, CurationEntityType.STATISTICS);
                logger.info("Creating statistics table finished.");

                logger.info("Deleting old links if there are any...");
                deleteOldLinks();

                Configuration.tearDown();
            } else
                throw new Exception("Only path is allowed for curation of collections root");
        } else
            throw new Exception("Curation module can curate profiles (-p), instances (-i), collection (-c) or collection root (-r)");
    }

    //After each run of the harvest, if some links are in the database but not in the new harvest, they are deleted (moved to history) from the database
    private static void deleteOldLinks() {
        try {
            //delete any urls that have the harvestDate column older than this current one.
            //they are already updated during the run.
            int rows = Configuration.linkToBeCheckedResource.deleteOldLinks(Configuration.reportGenerationDate);
            logger.info("Deleted " + rows + " rows from the table, because they were old (not harvested/found in the records during current report generation).");
        } catch (SQLException e) {
            logger.error("Error when deleting old links: " + e.getMessage());
        }
    }


    //helper method to marshall pojo reports into xml
    private static void dumpAsXML(Report<?> report, CurationEntityType type) throws Exception {
        JAXBContext jc = JAXBContext.newInstance(report.getClass());

        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8");

        if (Configuration.SAVE_REPORT && Configuration.OUTPUT_DIRECTORY != null) {
            Path path = Configuration.OUTPUT_DIRECTORY.resolve("xml");
            switch (type) {
                case PROFILE:
                    path = path.resolve("profiles");
                    break;
                case INSTANCE:
                    path = path.resolve("instances");
                    break;
                case COLLECTION:
                    path = path.resolve("collections");
                    break;
                case STATISTICS:
                    path = path.resolve("statistics");
            }

            Files.createDirectories(path);
            String filename = FileNameEncoder.encode(report.getName()) + ".xml";
            path = path.resolve(filename);

            marshaller.marshal(report, Files.newOutputStream(path));

        } else {//print to console
            marshaller.marshal(report, System.out);

            System.out.println("-----------------------------------------------------------------");

        }


    }
    //helper method to marshall pojo reports into html via xslt
    private static void dumpAsHTML(Report<?> report, CurationEntityType type) throws TransformerException, JAXBException, IOException {
        Path path = Configuration.OUTPUT_DIRECTORY.resolve("html");

        switch (type) {
            case PROFILE:
                path = path.resolve("profiles");
                break;
            case INSTANCE:
                path = path.resolve("instances");
                break;
            case COLLECTION:
                path = path.resolve("collections");
                break;
            case STATISTICS:
                path = path.resolve("statistics");
        }

        Files.createDirectories(path);
        String filename = FileNameEncoder.encode(report.getName()) + ".html";
        path = path.resolve(filename);

        TransformerFactory factory = TransformerFactory.newInstance();

        Source xslt = new StreamSource(Main.class.getResourceAsStream("/xslt/" + report.getClass().getSimpleName() + "2HTML.xsl"));

        Transformer transformer = factory.newTransformer(xslt);
        transformer.transform(new JAXBSource(JAXBContext.newInstance(report.getClass()), report), new StreamResult(path.toFile()));


    }

    //helper method to marshall pojo reports into tsv
    private static void dumpAsTSV(Report<?> report, CurationEntityType type) throws TransformerException, JAXBException, IOException {
        Path path = Configuration.OUTPUT_DIRECTORY.resolve("tsv");

        switch (type) {
            case PROFILE:
                path = path.resolve("profiles");
                break;
            case INSTANCE:
                path = path.resolve("instances");
                break;
            case COLLECTION:
                path = path.resolve("collections");
                break;
        }

        Files.createDirectories(path);
        String filename = FileNameEncoder.encode(report.getName()) + ".tsv";
        path = path.resolve(filename);

        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslt = new StreamSource(Main.class.getResourceAsStream("/xslt/" + report.getClass().getSimpleName() + "2TSV.xsl"));

        Transformer transformer = factory.newTransformer(xslt);
        transformer.transform(new JAXBSource(JAXBContext.newInstance(report.getClass()), report), new StreamResult(path.toFile()));
    }

    private static Options createHelpOption() {
        Option help = new Option("help", "print this message");
        Options options = new Options();
        options.addOption(help);
        return options;
    }

    //create cli options
    private static Options createOptions() {

        Option configurationFile = Option.builder("config").argName("file").
                hasArg().required(false).desc("a path to the configuration file").build();


        Option profileCuration = Option.builder("p").desc("curate a profile").build();

        Option instanceCuration = Option.builder("i").desc("curate an instance").build();

        Option collectionCuration = Option.builder("c").desc("curate a collection").build();

        Option resultsCuration = Option.builder("r").desc("curate all collections from a folder").build();

        OptionGroup curationGroup = new OptionGroup();
        curationGroup.addOption(profileCuration).addOption(instanceCuration).addOption(collectionCuration).addOption(resultsCuration);
        curationGroup.setRequired(true);

        Option paramId = Option.builder("id").argName("profilesId").hasArgs().
                numberOfArgs(Option.UNLIMITED_VALUES).desc("Space separated CLARIN profile IDs in format: clarin.eu:cr1:p_xxx").build();

        Option paramPath = Option.builder("path").argName("path").hasArgs().
                numberOfArgs(Option.UNLIMITED_VALUES).desc("Space separated paths to file or folder to be curated").build();

        Option paramUrl = Option.builder("url").argName("url").hasArgs().
                numberOfArgs(Option.UNLIMITED_VALUES).desc("Space separated urls to profile or instance to be curated").build();

        OptionGroup curationInputParams = new OptionGroup();
        curationInputParams.addOption(paramId).addOption(paramPath).addOption(paramUrl);
        curationInputParams.setRequired(true);

        Options options = new Options();
        options.addOption(configurationFile);
        options.addOptionGroup(curationGroup);
        options.addOptionGroup(curationInputParams);

        return options;
    }

    private static OutputStream getOutputStream(Report report, CurationEntityType type) throws IOException {
        if (Configuration.SAVE_REPORT && Configuration.OUTPUT_DIRECTORY != null) {
            Path path = null;
            switch (type) {
                case PROFILE:
                    path = Configuration.OUTPUT_DIRECTORY.resolve("profiles");
                    break;
                case INSTANCE:
                    path = Configuration.OUTPUT_DIRECTORY.resolve("instances");
                    break;
                case COLLECTION:
                    path = Configuration.OUTPUT_DIRECTORY.resolve("collections");
                    break;
            }

            Files.createDirectories(path);
            path = path.resolve(report.getName() + ".xml");
            return Files.newOutputStream(path);
        }

        return System.out;

    }
}
