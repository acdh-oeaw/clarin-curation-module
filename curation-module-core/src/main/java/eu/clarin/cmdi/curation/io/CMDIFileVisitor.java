package eu.clarin.cmdi.curation.io;

import eu.clarin.cmdi.curation.entities.CMDICollection;
import eu.clarin.cmdi.curation.entities.CMDIInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Stack;

/**
 * This class visits files of collections (CMDCollection)
 */
public class CMDIFileVisitor implements FileVisitor<Path> {

    private static final Logger logger = LoggerFactory.getLogger(CMDIFileVisitor.class);

    private CMDICollection collection = null;
    private CMDICollection root = null;

    private Stack<CMDICollection> stack = new Stack<>();


    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        logger.trace("visiting {}", dir);
        if (collection != null) {
            stack.push(collection);
        }

        collection = new CMDICollection(dir);

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        collection.addChild(new CMDIInstance(file, attrs.size()));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        throw new IOException("Failed to process " + file, exc);
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {

        logger.trace("finished visiting {}, number of files: {}", dir, collection.getNumOfFiles());

        root = collection;

        return FileVisitResult.CONTINUE;
    }

    public CMDICollection getRoot() {
        return root;
    }

    public void setRoot(CMDICollection root) {
        this.root = root;
    }

}