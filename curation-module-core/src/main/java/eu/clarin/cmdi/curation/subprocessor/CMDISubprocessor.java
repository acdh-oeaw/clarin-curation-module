/**
 * 
 */
package eu.clarin.cmdi.curation.subprocessor;

import com.ximpleware.VTDException;
import eu.clarin.cmdi.curation.entities.CMDIInstance;
import eu.clarin.cmdi.curation.exception.FileSizeException;
import eu.clarin.cmdi.curation.report.CMDIInstanceReport;
import eu.clarin.cmdi.curation.report.Message;
import eu.clarin.cmdi.curation.report.Severity;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

/**
 * Enables processing of cmdi entities and generating reports for different parts of the report. ex: file size validator, url validator etc.
 * It is overengineered and is not really necessary. Could have been much simpler.
 */
public abstract class CMDISubprocessor {

    protected Collection<Message> msgs = null;

    public abstract void process(CMDIInstance entity, CMDIInstanceReport report) throws IOException, ExecutionException, ParserConfigurationException, SAXException, TransformerException, FileSizeException, VTDException;

    protected void addMessage(Severity lvl, String message) {
        if (msgs == null) {
            msgs = new ArrayList<>();
        }
        msgs.add(new Message(lvl, message));
    }
}
