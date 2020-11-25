package eu.clarin.cmdi.curation.xml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.ParseException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;

public class CMDIXPathService {

    private static final Logger logger = LoggerFactory.getLogger(CMDIXPathService.class);

    private VTDNav navigator = null;

    public CMDIXPathService(Path path) throws IOException, ParseException {
        VTDGen parser = new VTDGen();

        parser.setDoc(Files.readAllBytes(path));
        parser.parse(false);
        navigator = parser.getNav();
        parser = null;

    }

    public CMDIXPathService(String url) throws Exception {
        VTDGen parser = new VTDGen();

        if (!parser.parseHttpUrl(url, true))
            throw new Exception("Errors while parsing " + url);

        navigator = parser.getNav();
        parser = null;
    }

    public VTDNav getNavigator() {
        return navigator;
    }

    public String getXPathValue(String xpath) throws Exception {
        String result = null;
        try {
            AutoPilot ap = new AutoPilot(reset());
            ap.selectXPath(xpath);
            int index = ap.evalXPath();
            if (index != -1)
                result = navigator.toString(index).trim();
        } catch (Exception e) {
            throw new Exception("Errors while performing xpath operation:" + xpath, e);
        }
        return result;
    }

    public String getXPathAttrValue(String xpath, String attributeName) throws Exception {
        String result = null;
        try {
            AutoPilot ap = new AutoPilot(reset());
            ap.selectXPath(xpath);
            int index = ap.evalXPath();
            if (index != -1)
                result = navigator.toString(navigator.getAttrVal(attributeName)).trim();
        } catch (Exception e) {
            throw new Exception("Errors while performing xpath operation:" + xpath, e);
        }
        return result;
    }

    public Collection<String> getXPathValues(String xpath) throws Exception {

        Collection<String> result = new ArrayList<>();
        try {
            AutoPilot ap = new AutoPilot(reset());
            ap.selectXPath(xpath);
            while (true) {
                int index = ap.evalXPath();
                if (index != -1)
                    result.add(navigator.toString(index).trim());
                else
                    break;
            }
        } catch (Exception e) {
            throw new Exception("Errors while performing xpath operation:" + xpath, e);
        }

        return result;
    }

    public VTDNav reset() throws NavException {
        navigator.toElement(VTDNav.ROOT);
        return navigator;
    }
}
