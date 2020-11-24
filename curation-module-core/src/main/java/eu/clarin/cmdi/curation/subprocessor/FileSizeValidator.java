package eu.clarin.cmdi.curation.subprocessor;

import eu.clarin.cmdi.curation.entities.CMDInstance;
import eu.clarin.cmdi.curation.instance_parser.InstanceParser;
import eu.clarin.cmdi.curation.exception.FileSizeException;
import eu.clarin.cmdi.curation.main.Configuration;
import eu.clarin.cmdi.curation.report.CMDInstanceReport;
import eu.clarin.cmdi.curation.report.CMDInstanceReport.FileReport;
import eu.clarin.cmdi.curation.report.Score;
import eu.clarin.cmdi.curation.report.Severity;
import eu.clarin.cmdi.curation.utils.FileNameEncoder;
import eu.clarin.cmdi.curation.vlo_extensions.CMDIDataImplFactory;
import eu.clarin.cmdi.curation.vlo_extensions.FacetMappingCacheFactory;
import eu.clarin.cmdi.vlo.LanguageCodeUtils;
import eu.clarin.cmdi.vlo.config.DefaultVloConfigFactory;
import eu.clarin.cmdi.vlo.config.FieldNameService;
import eu.clarin.cmdi.vlo.config.FieldNameServiceImpl;
import eu.clarin.cmdi.vlo.config.VloConfig;
import eu.clarin.cmdi.vlo.importer.CMDIData;
import eu.clarin.cmdi.vlo.importer.MetadataImporter;
import eu.clarin.cmdi.vlo.importer.ResourceStructureGraph;
import eu.clarin.cmdi.vlo.importer.VLOMarshaller;
import eu.clarin.cmdi.vlo.importer.mapping.FacetMappingFactory;
import eu.clarin.cmdi.vlo.importer.processor.CMDIDataProcessor;
import eu.clarin.cmdi.vlo.importer.processor.CMDIParserVTDXML;
import eu.clarin.cmdi.vlo.importer.processor.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates file sizes for collection and instance reports
 * All this class just to validate file sizes in the report? Couldn't have been a simple if check?
 */
public class FileSizeValidator extends CMDSubprocessor {
    private final static Logger logger = LoggerFactory.getLogger(FileSizeValidator.class);

    private static final Pattern _pattern = Pattern.compile("xmlns(:.+?)?=\"http(s)?://www.clarin.eu/cmd/(1)?");

    private static final CMDIDataProcessor<Map<String, List<ValueSet>>> _processor = getProcessor();


    private static CMDIDataProcessor<Map<String, List<ValueSet>>> getProcessor() {
        try {
            final VloConfig vloConfig = new DefaultVloConfigFactory().newConfig();

            final LanguageCodeUtils languageCodeUtils = new LanguageCodeUtils(vloConfig);

            final FieldNameService fieldNameService = new FieldNameServiceImpl(vloConfig);

            final CMDIDataImplFactory cmdiDataFactory = new CMDIDataImplFactory(fieldNameService);

            final VLOMarshaller marshaller = new VLOMarshaller();

            final FacetMappingFactory facetMappingFactory = FacetMappingCacheFactory.getInstance();

            return new CMDIParserVTDXML<>(
                    MetadataImporter.registerPostProcessors(vloConfig, fieldNameService, languageCodeUtils),
                    MetadataImporter.registerPostMappingFilters(fieldNameService),
                    vloConfig, facetMappingFactory, marshaller, cmdiDataFactory, fieldNameService, false);


        } catch (IOException ex) {
            logger.error("couldn't instatiate CMDIDataProcessor - so instance parsing won't work!");
            return null;
        }
    }

    private boolean isLatestVersion(Path path) throws IOException {
        String line = null;
        Matcher matcher;

        try (BufferedReader reader = Files.newBufferedReader(path)) {

            while ((line = reader.readLine()) != null)
                if ((matcher = _pattern.matcher(line)).find())
                    return matcher.group(3) != null;
        } catch (IOException ex) {
            logger.error("can't identitfy cmdi version by namespace", ex);
        }
        return false;
    }


    @Override
    public void process(CMDInstance entity, CMDInstanceReport report) throws IOException, TransformerException, FileSizeException {

        //convert cmdi 1.1 to 1.2 if necessary

        if (!isLatestVersion(entity.getPath())) {
            Path newPath = Files.createTempFile(null, ".xml");

            TransformerFactory factory = TransformerFactory.newInstance();
            Source xslt = new StreamSource(InstanceParser.class.getResourceAsStream("/xslt/cmd-record-1_1-to-1_2.xsl"));

            Transformer transformer = factory.newTransformer(xslt);
            transformer.transform(new StreamSource(entity.getPath().toFile()), new StreamResult(newPath.toFile()));

            this.addMessage(Severity.INFO, "tranformed cmdi version 1.1 into version 1.2");

            entity.setPath(newPath);
            entity.setSize(Files.size(newPath));
        }

        report.fileReport = new FileReport();
        report.fileReport.size = entity.getSize();
        if (entity.getUrl() != null) {
            report.fileReport.location = FileNameEncoder.encode(entity.getUrl());
        } else {
            int index;
            String path = entity.getPath().toString();

            if ((index = path.indexOf("/clarin")) != -1 || (index = path.indexOf("/europeana")) != -1) {
                report.fileReport.location = path.substring(index + 1);
            } else {
                report.fileReport.location = path;
            }

        }

        if (report.fileReport.size > Configuration.MAX_FILE_SIZE) {
            addMessage(Severity.FATAL, "The file size exceeds the limit allowed (" + Configuration.MAX_FILE_SIZE + "B)");
            //don't assess when assessing collections
            if (Configuration.COLLECTION_MODE) {
                throw new FileSizeException(entity.getPath().getFileName().toString(), report.fileReport.size);
            }
        }

        CMDIData<Map<String, List<ValueSet>>> cmdiData = null;
        try {
            cmdiData = _processor.process(entity.getPath().toFile(), new ResourceStructureGraph());
        } catch (Exception e) {
            throw new IOException(e);
        }


        entity.setCMDIData(cmdiData);

        //create xpath/value pairs only in instance mode
        if (!Configuration.COLLECTION_MODE) {

            InstanceParser transformer = new InstanceParser();

            logger.debug("parsing instance...");
            entity.setParsedInstance(transformer.parseIntance(Files.newInputStream(entity.getPath())));
            logger.debug("...done");

        }
    }

    public Score calculateScore() {
        //in case that size exceeds the limit msgs will be created and it will contain a single msg
        return new Score(msgs == null ? 1.0 : 0, 1.0, "file-size", msgs);
    }
}
