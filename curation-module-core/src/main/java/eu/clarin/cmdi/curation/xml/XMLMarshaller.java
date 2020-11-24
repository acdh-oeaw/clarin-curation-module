/**
 * 
 */
package eu.clarin.cmdi.curation.xml;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * Marshaller for xml
 */
public class XMLMarshaller<T> {

	final Class<T> typeParamClass;

	public XMLMarshaller(Class<T> typeParamClass) {
		this.typeParamClass = typeParamClass;
	}

	@SuppressWarnings("unchecked")
    public T unmarshal(InputStream is) throws JAXBException {
		JAXBContext jc = JAXBContext.newInstance(typeParamClass);
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		return (T) unmarshaller.unmarshal(is);
	}

	public void marshal(T object, OutputStream os)  {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(typeParamClass);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8");

			jaxbMarshaller.marshal(object, os);

		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}

}
