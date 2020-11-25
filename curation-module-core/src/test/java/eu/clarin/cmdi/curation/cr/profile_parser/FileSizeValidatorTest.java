package eu.clarin.cmdi.curation.cr.profile_parser;
import org.junit.Before;

import org.junit.Test;

import eu.clarin.cmdi.curation.entities.CMDIInstance;
import eu.clarin.cmdi.curation.report.CMDIInstanceReport;
import eu.clarin.cmdi.curation.subprocessor.FileSizeValidator;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSizeValidatorTest extends TestBase{
    CMDIInstance entity;
    
    @Before
    public void init() {
        Path path;
        try {
            
            path = Paths.get(getClass().getClassLoader().getResource("cmdi/cmdi-1_2.xml").toURI());

            entity = new CMDIInstance(path, Files.size(path));
            
            CMDIInstanceReport report = new CMDIInstanceReport();
            
            FileSizeValidator fsv = new FileSizeValidator();
            
            fsv.process(entity, report);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
    
    @Test
    public void testFileSettings() {
            assertTrue(entity.getPath().toString().endsWith("cmdi/cmdi-1_2.xml"));
            assertEquals(9925, entity.getSize());

    }
    
    
    @Test
    public void testCmdiData() {
        assertNotNull(entity.getCMDIData());

        assertEquals(1, entity.getCMDIData().getDataResources().size());
        
        assertEquals(1, entity.getCMDIData().getLandingPageResources().size());
        
        assertEquals(1, entity.getCMDIData().getSearchPageResources().size());
        
        assertEquals(0, entity.getCMDIData().getMetadataResources().size());
        
        assertEquals(1, entity.getCMDIData().getDocument().get("id").size());
        
        assertEquals("hdl_58_21.11114_47_COLL-0000-000B-CAB3-7", entity.getCMDIData().getDocument().get("id").get(0).getValue()); 

    }

}
