package eu.clarin.cmdi.curation.entities;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;


import eu.clarin.cmdi.curation.processor.CollectionProcessor;
import eu.clarin.cmdi.curation.report.CollectionReport;

/**
 * Pojo of a collection folder harvested from VLO
 */
public class CMDICollection {

    Deque<CMDIInstance> children;

    long numOfFiles;
    long maxFileSize = 0;
    long minFileSize = Long.MAX_VALUE;
    protected Path path = null;
    protected long size = 0;

    public CMDICollection(Path path) {
        this.path = path;
        children = new ArrayDeque<>();
    }

    public CollectionReport generateReport() {
        return new CollectionProcessor().process(this);
    }

    public void addChild(CMDIInstance child) {
        children.add(child);

        numOfFiles++;
        size += child.getSize();
        if (child.getSize() > maxFileSize)
            maxFileSize = child.getSize();
        if (child.getSize() < minFileSize)
            minFileSize = child.getSize();

    }

    public Deque<CMDIInstance> getChildren() {
        return children;
    }

    public long getNumOfFiles() {
        return numOfFiles;
    }

    public void setNumOfFiles(long numOfFiles) {
        this.numOfFiles = numOfFiles;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public long getMinFileSize() {
        return minFileSize;
    }

    public void setMinFileSize(long minFileSize) {
        this.minFileSize = minFileSize;
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

    @Override
    public String toString() {
        return "Collection: " + path.toString();
    }
}
