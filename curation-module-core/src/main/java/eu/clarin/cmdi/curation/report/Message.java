
package eu.clarin.cmdi.curation.report;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Messages for errors and warnings in reports
 */

@XmlRootElement(name = "details")
@XmlAccessorType(XmlAccessType.FIELD)
public class Message {
    
    @XmlAttribute
    Severity lvl;    
    @XmlAttribute
    String message; 
    
    public Message(){}

    public Message(Severity lvl, String message) {
		super();
		this.lvl = lvl;
		this.message = message;
    }

    public Severity getLvl() {
        return lvl;
    }

    public void setLvl(Severity lvl) {
        this.lvl = lvl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    

    @Override
    public String toString() {
        return lvl.label + " - " + message;
    }

}
