package eu.clarin.cmdi.curation.instance_parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import eu.clarin.cmdi.curation.instance_parser.ParsedInstance.InstanceNode;

/**
 * @author Davor
 * The InstanceParser uses XSLT to transform the CMDInstance into a list of xpath/value pairs follwing the pattern <xpath>=<value>. 
 * These pairs are stored in a List of InstanceNode
 *
 */
public class InstanceParser {
	
	private static Transformer transformer = null;
	
	static{
		TransformerFactory factory = TransformerFactory.newInstance();
		Source xslt = new StreamSource(InstanceParser.class.getResourceAsStream("/xslt/instanceTransformer.xsl"));		
		try {
			transformer = factory.newTransformer(xslt);
		} catch (TransformerConfigurationException e) {
			throw new RuntimeException("Unable to open instanceTransformer.xsl", e);
		}
	}
	
	public ParsedInstance parseIntance(InputStream is) throws TransformerException, IOException{
		Source xml = new StreamSource(is);
		StringWriter writer = new StringWriter();
		Result out = new StreamResult(writer);
		synchronized (transformer) {
			transformer.transform(xml, out);
		}
		
		String line;
		String record = null;
		Collection<InstanceNode> nodes = new ArrayList<>();
		BufferedReader br = new BufferedReader(new StringReader(writer.toString()));
		while((line = br.readLine()) != null){
			//new recordName, save old
			if(line.startsWith("/cmd:CMD")){
				if(record != null){
					int equalInd = record.indexOf('=');
					String value = record.substring(equalInd + 1);
					if(!value.isEmpty()){
						String xpath = record.substring(0, equalInd);
						nodes.add(new InstanceNode(xpath, value));
					}
				}
				record = line;
			}else{
				record += line;
			}				
		}
			
//		Pattern pattern = Pattern.compile("(/cmd:CMD.+?)=\\{(.+?)\\}", Pattern.DOTALL);
//		Matcher matcher = pattern.matcher(writer.toString());
//		while (matcher.find()) {
//		    nodes.add(new InstanceNode(matcher.group(1), matcher.group(2)));
//		}
		return new ParsedInstance(nodes);
	}
}
