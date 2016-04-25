package ru.ifmo.ctddev.zemskov.implementor;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Allows to compile java sources to jar file.
 *
 * @author Kirill Zemskov
 */

public class JarCompiler {

    /**
     * Field that contains fields to files that will be compiled to .jar
     */
    private Path[] sourceFiles;

    /**
     * Contructor get files to use them for creating .jar
     * @param sourceFiles paths to files
     */

    public JarCompiler(final Path... sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    /**
     * Compiles files and sends them to <code>jarFile</code>.
     *
     * @param jarFile resulting path .jar file
     * @throws IOException       if some IO error occurs
     * @throws CompilerException if compiler fails to compile code
     */
    protected void compile(Path jarFile) throws IOException, CompilerException {
        Path buildDir;
        try {
            buildDir = Files.createTempDirectory("implementorBuild_tmp_");
        } catch (IOException e) {
            throw new IOException("Can't create temporary directory for class files", e);
        }
        compileClasses(buildDir);
        buildJar(buildDir, jarFile);
    }

    /**
     * Compiles classes and stores them at <code>buildDir</code>.
     *
     * @param buildDir path, to which we put built classes
     * @throws CompilerException if compiler fails to compile code
     */
    private void compileClasses(Path buildDir) throws CompilerException {
        ArrayList<String> files = new ArrayList<>(sourceFiles.length);
        for (Path file : sourceFiles) {
            files.add(file.toString());
        }
        int exitCode = runCompiler(files, buildDir);
        if (exitCode != 0) {
            throw new CompilerException("Compiler finished with exitCode " + exitCode);
        }
    }

    /**
     * Executes compiler on all files <code>files</code> and
     * stores output at <code>outDir</code>.
     *
     * @param files  list of strings, representing files to compile
     * @param outDir path, representing path to  place, where to put built classes
     * @return compiler's exit code
     */
    private int runCompiler(List<String> files, Path outDir) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No java compiler");
        }
        List<String> args = new ArrayList<>();
        args.addAll(files);
        args.add("-d");
        args.add(outDir.toString());
        return compiler.run(null, null, null, args.toArray(new String[args.size()]));
    }

    /**
     * Builds jar file from built classes at <code>buildDir</code> directory
     * and combines them into jar file <code>jarFile</code>.
     *
     * @param buildDir path, representing path to  directory where built files are located
     * @param jarFile  path, representing path to jar file, which must be created
     * @throws IOException if some IO error occurs
     */
    private void buildJar(Path buildDir, Path jarFile) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream jarOutputStream = new JarOutputStream(
                new BufferedOutputStream(new FileOutputStream(jarFile.toFile())), manifest)) {
            Files.walkFileTree(buildDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                    String entryName = buildDir.relativize(filePath).toString();
                    JarEntry entry = new JarEntry(entryName);
                    jarOutputStream.putNextEntry(entry);
                    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath.toFile()))) {
                        byte[] buffer = new byte[4096];
                        int c;
                        while ((c = bis.read(buffer)) >= 0) {
                            jarOutputStream.write(buffer, 0, c);
                        }
                    }
                    jarOutputStream.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Is thrown when compiler fails to compile generated implementations' files.
     */
    protected static class CompilerException extends Exception {

        /**
         * Construct exception
         * @param message text of exception
         */
        public CompilerException(String message) {
            super(message);
        }

    }

}
