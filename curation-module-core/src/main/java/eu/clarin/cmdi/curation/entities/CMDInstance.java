package eu.clarin.cmdi.curation.entities;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.ximpleware.VTDException;
import eu.clarin.cmdi.curation.instance_parser.ParsedInstance;
import eu.clarin.cmdi.curation.exception.FileSizeException;
import eu.clarin.cmdi.curation.processor.CMDInstanceProcessor;
import eu.clarin.cmdi.curation.report.CMDInstanceReport;
import eu.clarin.cmdi.vlo.importer.CMDIData;
import eu.clarin.cmdi.vlo.importer.processor.ValueSet;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Pojo of a CMD Record File harvested from VLO
 */
public class CMDInstance {


    public static Collection<String> mdSelfLinks = Collections.synchronizedCollection(new HashSet<>());
    public static Collection<String> duplicateMDSelfLink = Collections.synchronizedCollection(new HashSet<>());

    private ParsedInstance parsedInstance = null;

    private CMDIData<Map<String, List<ValueSet>>> cmdiData;

    protected Path path = null;
    protected long size = 0;
	protected String url;


    public CMDInstance(Path path) {
        this.path = path;
    }

    public CMDInstance(Path path, long size) {
        this.path = path;
        this.size = size;
    }

    public CMDInstanceReport generateReport(String parentName) throws TransformerException, FileSizeException, IOException, ExecutionException, ParserConfigurationException, SAXException, VTDException {
        return new CMDInstanceProcessor().process(this, parentName);
    }


    public ParsedInstance getParsedInstance() {
        return parsedInstance;
    }

    public void setParsedInstance(ParsedInstance parsedInstance) {
        this.parsedInstance = parsedInstance;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

    @Override
    public String toString() {
        int cnt = path.getNameCount();
        String name = path.getName(cnt - 1).toString();
        if (cnt > 1)
            name = path.getName(cnt - 2) + "/" + name;
        return "CMD Instance: " + name;
    }

    public CMDIData<Map<String, List<ValueSet>>> getCMDIData() {
        return this.cmdiData;
    }

    public void setCMDIData(CMDIData<Map<String, List<ValueSet>>> cmdiData) {
        this.cmdiData = cmdiData;
    }
}
