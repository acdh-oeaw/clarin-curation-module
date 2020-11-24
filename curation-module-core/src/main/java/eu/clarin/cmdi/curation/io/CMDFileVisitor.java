package eu.clarin.cmdi.curation.io;

import eu.clarin.cmdi.curation.entities.CMDCollection;
import eu.clarin.cmdi.curation.entities.CMDInstance;
import eu.clarin.cmdi.curation.utils.TimeUtils;
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
public class CMDFileVisitor implements FileVisitor<Path> {

    private static final Logger logger = LoggerFactory.getLogger(CMDFileVisitor.class);

    private CMDCollection collection = null;
    private CMDCollection root = null;

    private Stack<CMDCollection> stack = new Stack<>();


    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        logger.trace("visiting {}", dir);
        if (collection != null) {
            stack.push(collection);
        }

        collection = new CMDCollection(dir);

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        collection.addChild(new CMDInstance(file, attrs.size()));
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

    public CMDCollection getRoot() {
        return root;
    }

    public void setRoot(CMDCollection root) {
        this.root = root;
    }

}