
package ru.ifmo.ctddev.zemskov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Implementation of interface {@link info.kgeorgiy.java.advanced.implementor.Impler}
 * and {@link info.kgeorgiy.java.advanced.implementor.JarImpler}
 *
 * @author Kirill Zemskov
 * @see ru.ifmo.ctddev.zemskov.implementor.ImplementCreator
 */
public class Implementor implements JarImpler {
    /**
     * Implements class, puts it's java code in path, relative to root
     *
     * @param token class to implement
     * @param root  file, containing root of directory where to put generated file
     * @return file, where source (.java) code was placed
     * @throws ImplerException if there is no correct implementation for <code>token</code>
     * @see #implement(Class, java.nio.file.Path)
     */

    public Path implementWithFile(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Null arguments");
        }
        final String path = root + File.separator + token.getName().replace(".", File.separator) + "Impl.java";
        File file = new File(path).getAbsoluteFile();
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new ImplerException("Couldn't create dirs");
        }
        try (FileWriter out = new FileWriter(file, true)) {
            ImplementCreator implementCreator = new ImplementCreator(token, token.getSimpleName() + "Impl", out);
            implementCreator.implement();
        } catch (IOException e) {
            throw new ImplerException("Couldn't open output file");
        }
        return file.toPath();
    }

    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path implDir;
        try {
            implDir = Files.createTempDirectory("implementor_tmp_");
        } catch (IOException e) {
            throw new ImplerException("Can't create temporary directory for implementation (source) files", e);
        }
        Path file = implementWithFile(token, implDir);
        JarCompiler jarCompiler = new JarCompiler(file);
        try {
            jarCompiler.compile(jarFile);
        } catch (IOException | JarCompiler.CompilerException e) {
            throw new ImplerException(e.getMessage(), e);
        }

    }

    @Override
    public void implement(Class<?> token, Path path) throws ImplerException {
        implementWithFile(token, path);
    }

    /**
     * Default constructor
     */
    public Implementor(){

    }


}