package eu.clarin.cmdi.curation.utils;

/**
 * This class encodes file names into url friendly strings.
 * Seriously a whole class for a one liner???
 */
public class FileNameEncoder {

    public static String encode(String source){
        return source.replaceAll("[/.:]", "_");
    }
}
