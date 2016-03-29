/**
 * 
 */
package eu.clarin.cmdi.curation.report;

import java.io.OutputStream;

/**
 * @author dostojic
 *
 */
public interface Report<R extends Report> {

	public void mergeWithParent(R parentReport);

	public void marshal(OutputStream os) throws Exception;

	public double calculateScore();
	
	public double getMaxScore();
	
	public boolean isValid();

}
