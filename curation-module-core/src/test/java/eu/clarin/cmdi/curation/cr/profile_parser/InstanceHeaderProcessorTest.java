package eu.clarin.cmdi.curation.cr.profile_parser;

import eu.clarin.cmdi.curation.entities.CMDIInstance;
import eu.clarin.cmdi.curation.report.CMDIInstanceReport;
import eu.clarin.cmdi.curation.subprocessor.FileSizeValidator;
import eu.clarin.cmdi.curation.subprocessor.InstanceHeaderProcessor;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class InstanceHeaderProcessorTest extends TestBase {
    CMDIInstance entity;
    CMDIInstanceReport report;

    @Before
    public void load() {
        Path path;
        try {

            path = Paths.get(getClass().getClassLoader().getResource("cmdi/cmdi-1_2.xml").toURI());


            entity = new CMDIInstance(path, Files.size(path));

            report = new CMDIInstanceReport();

            FileSizeValidator fsv = new FileSizeValidator();

            fsv.process(entity, report);

            new InstanceHeaderProcessor().process(entity, report);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Test
    public void testHeader() {
        assertEquals("clarin.eu:cr1:p_1493735943947", this.report.header.getId());

        assertEquals("1.x", this.report.header.getCmdiVersion());
        assertEquals("CollBank", this.report.fileReport.collection);


    }

}
