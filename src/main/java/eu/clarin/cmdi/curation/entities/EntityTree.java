package eu.clarin.cmdi.curation.entities;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.cmdi.curation.utils.TimeUtils;

public class EntityTree implements FileVisitor<Path> {

    private static final Logger _logger = LoggerFactory.getLogger(EntityTree.class);

    private Collection curDir = null;
    private Collection root = null;

    private Stack<Collection> stack = new Stack<Collection>();
    

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
	_logger.trace("visiting {}", dir);
	if (curDir != null)
	    stack.push(curDir);

	curDir = new Collection(dir);

	return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
	CurationEntity entity;
	if (file.endsWith(".xsd"))
	    entity = new CMDProfile(file, attrs.size());
	else
	    entity = new CMDInstance(file, attrs.size());

	curDir.addChild(entity);
	return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
	throw new IOException("Failed to process " + file, exc);
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {

	_logger.trace("finished visiting {}, number of files: {}", dir, curDir.getNumOfFiles());

	long startTime = System.currentTimeMillis();
	// fire processors for children
	curDir.generateReport();
	long end = System.currentTimeMillis();
	_logger.info("validation for {} lasted {}", dir, TimeUtils.humanizeTime(end - startTime));
	
	
	if(!stack.empty()){
	    root = stack.pop();
	    root.addChild(curDir);
	    curDir = root;
	}else{
	    root = curDir;
	}
	
	//else: last elem
	
	return FileVisitResult.CONTINUE;
    }

    public Collection getRoot() {
	return root;
    }

    public void setRoot(Collection root) {
	this.root = root;
    }

}