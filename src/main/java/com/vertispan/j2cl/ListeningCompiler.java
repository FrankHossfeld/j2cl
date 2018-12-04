package com.vertispan.j2cl;

import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.j2cl.frontend.FrontendUtils;

/**
 * Simple "dev mode" for j2cl+closure, based on the existing bash script. Lots of room for improvement, this
 * isn't intended to be a proposal, just another experiment on the way to one.
 * <p>
 * Assumptions:
 * o The js-compatible JRE is already on the java classpath (need not be on js). Probably not a good one, but
 * on the other hand, we may want to allow changing out the JRE (or skipping it) in favor of something else.
 * o A JS entrypoint already exists. Probably safe, should get some APT going soon as discussed, at least to
 * try it out.
 * <p>
 * Things about this I like:
 * o Treat both jars and jszips as classpaths (ease of dependency system integrations)
 * o Annotation processors are (or should be) run as an IDE would do, so all kinds of changes are picked up. I
 * think I got it right to pick up generated classes changes too...
 * <p>
 * Not so good:
 * o Not correctly recompiling classes that require it based on dependencies
 * o Not at all convinced my javac wiring is correct
 * o Polling for changes
 */
public class ListeningCompiler {

    private final static Logger LOGGER = Logger.getLogger(ListeningCompiler.class.getName());

    public static void run(Gwt3Options options) throws IOException, InterruptedException, ExecutionException {
        LOGGER.setLevel(Level.INFO);
        LOGGER.info("Setup SingleCompiler");
        SingleCompiler.setup(options);
        FileTime lastModified = FileTime.fromMillis(0);
        LOGGER.info("Begin listening");
        while (true) {
            // currently polling for changes.
            // block until changes instead? easy to replace with filewatcher, just watch out for java9/osx issues...

            long pollStarted = System.currentTimeMillis();
            FileTime newerThan = lastModified;
            List<FrontendUtils.FileInfo> modifiedJavaFiles = SingleCompiler.getModifiedJavaFiles(newerThan);
            long pollTime = System.currentTimeMillis() - pollStarted;
            // don't replace this until the loop finishes successfully, so we know the last time we started a successful compile
            FileTime nextModifiedIfSuccessful = FileTime.fromMillis(System.currentTimeMillis());
            if (modifiedJavaFiles.isEmpty()) {
                Thread.sleep(100);
                continue;
            }
            SingleCompiler.compile(modifiedJavaFiles);
            LOGGER.info("Recompile of " + modifiedJavaFiles.size() + " source classes finished in " + (System.currentTimeMillis() - nextModifiedIfSuccessful.to(TimeUnit.MILLISECONDS)) + "ms");
            LOGGER.info("poll: " + pollTime + "millis");
            lastModified = nextModifiedIfSuccessful;
        }
    }
}
