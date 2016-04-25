package ru.ifmo.ctddev.zemskov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Processes one class and generates an implementation for it
 * through {@link #implement}
 * <p>
 * Expected usage is the following:
 * <blockquote><pre>
 *     new ImplementCreator(MyClass.class, "MyClassImpl", new FileWriter(new File("MyClassImpl.java")))
 * </pre></blockquote><p>
 *
 * @author Kirill Zemskov
 * @version 0.03a Close Alpha
 * @see ru.ifmo.ctddev.zemskov.implementor.Implementor
 */
public class ImplementCreator {
    /**
     * Expanded tab.
     */

    private final String TAB = "    ";

    /**
     * Line separator of current OS.
     * <p>
     * Equivalent to <code>System.lineSeparator()</code>
     */
    private final String LS = System.lineSeparator();

    /**
     * Initial class.
     */
    private final Class<?> clazz;

    /**
     * New class' name.
     */
    private final String newClassName;

    /**
     * Place, where to write new class.
     */
    private final Appendable out;

    /**
     * Class constructor, specifying which class to implement (<code>clazz</code>), name to give to the implementation
     * (<code>newClassName</code> and place, where to write it (<code>out</code>).
     *
     * @param clazz        class to implement
     * @param newClassName name of class to be generated
     * @param out          where the implementation will be placed
     * @throws ImplerException If any of the following is true:
     *                         <ul>
     *                         <li> {@code clazz} is primitive.
     *                         <li> {@code clazz} is final.
     *                         </ul>
     */
    public ImplementCreator(Class clazz, String newClassName, Appendable out) throws ImplerException {
        if (clazz.isPrimitive()) {
            throw new ImplerException("Can't implement primitives");
        }
        if (Modifier.isFinal(clazz.getModifiers())) {
            throw new ImplerException("Can't implement final class");
        }
        this.clazz = clazz;
        this.newClassName = newClassName;
        this.out = out;
    }

    /**
     * Produces code implementing current class or interface {@link #clazz} and
     * places it to {@link #out}.
     * <p>
     * Produced code will be correct Java code and will be successfully compiled
     * by Java compiler. New class' package will be equal to initial class' package
     * It will super all available constructors from parent, or, if there is no any
     * constructor (all parents are interfaces), it will leave default constructor.
     * All abstract methods will be overridden. No fields or new methods will be
     * generated.
     *
     * @throws IOException     if couldn't write to <code>out</code>
     * @throws ImplerException if there is no correct implementation for <code>clazz</code>
     * @see #writePackage()
     * @see #writeClassDeclaration()
     * @see #writeMethods()
     */
    public void implement() throws IOException, ImplerException {
        writePackage();
        writeClassDeclaration();
        writeConstructors();
        writeMethods();
        out.append("}");
    }

    /**
     * Writes implementation's class declaration to {@link #out}
     * <p>
     * Copies all parents modifiers excluding <code>abstract</code>.
     *
     * @throws IOException if couldn't write to <code>out</code>
     * @see Class#getModifiers()
     * @see #writeModifiers(int, int)
     */
    private void writeClassDeclaration() throws IOException {
        if (!clazz.isInterface()) {
            writeModifiers(clazz.getModifiers(), Modifier.interfaceModifiers());
        } else {
            writeModifiers(clazz.getModifiers(), Modifier.classModifiers());
        }
        out.append("class ");
        out.append(newClassName);
        out.append(clazz.isInterface() ? " implements " : " extends ");
        out.append(clazz.getCanonicalName());
        out.append(" {").append(LS);
    }

    /**
     * Writes implementation's class constructors to {@link #out}
     * <p>
     * Supers all non-private constructors, declared in parent. Copies all parent
     * constructor exceptions and passes default values to it.
     *
     * @throws IOException     if couldn't write to <code>out</code>
     * @throws ImplerException if there is no non-private constructor in {@link #clazz} and it's
     *                         not an interface
     * @see #writeParameters(Parameter[], boolean)
     * @see #writeModifiers(int, int)
     */
    private void writeConstructors() throws IOException, ImplerException {
        int conCount = 0;
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (!Modifier.isPrivate(constructor.getModifiers())) {
                conCount++;
                Parameter[] params = constructor.getParameters();
                out.append(TAB);
                writeModifiers(constructor.getModifiers(), Modifier.constructorModifiers());
                writeHead("", newClassName, params, constructor.getExceptionTypes());
                out.append("{").append(LS);
                out.append(TAB).append(TAB);
                out.append("super(");
                writeParameters(params, false);
                out.append(");").append(LS);
                out.append(TAB).append("}");
                out.append(LS).append(LS);
            }
        }

        if (conCount == 0 && clazz.getDeclaredConstructors().length > 0) {
            throw new ImplerException("No accessible constructors");
        }
    }

    /**
     * Get all non-private declared methods from class and his ancestors using recursion and push them in TreeMap
     *
     * @param base    Current class for getting all his non-private methods
     * @param methods Where we save methods we got
     */
    private void getMethods(Class<?> base, Set<HashebleMethod> methods) {
        for (Method method : base.getDeclaredMethods()) {
            if (!Modifier.isPrivate(method.getModifiers())) {
                methods.add(new HashebleMethod(method));
            }
        }
        if (base.getSuperclass() != null) {
            getMethods(base.getSuperclass(), methods);
        }
    }

    /**
     * Writes implementation class' declaration to {@link #out}
     * <p>
     * Overrides all abstract methods from ancestors. Copies access modifier
     * from ancestor method and returns default value for return type. Copies
     * all exceptions, declared in ancestor method
     *
     * @throws IOException if couldn't write to <code>out</code>
     * @see #defaultReturnValue(Type)
     * @see #writeParameters(Parameter[], boolean)
     * @see #writeModifiers(int, int)
     * @see #getMethods(Class, Set)
     */
    private void writeMethods() throws IOException {
        HashSet<HashebleMethod> methods = new HashSet<>();
        for (Method method : clazz.getDeclaredMethods()) {
            methods.add(new HashebleMethod(method));
        }
        for (Method method : clazz.getMethods()) {
            methods.add(new HashebleMethod(method));
        }
        if (clazz.getSuperclass() != null) {
            getMethods(clazz.getSuperclass(), methods);
        }
        methods.removeIf(method -> !Modifier.isAbstract(method.m.getModifiers()));
        for (HashebleMethod method : methods) {
            out.append(TAB);
            writeModifiers(method.m.getModifiers(), Modifier.methodModifiers());
            Parameter[] params = method.m.getParameters();
            writeHead(method.m.getReturnType().getCanonicalName(), method.m.getName(), params, method.m.getExceptionTypes());
            if (!Modifier.isNative(method.m.getModifiers())) {
                out.append("{").append(LS);
                if (!method.m.getReturnType().equals(Void.TYPE)) {
                    out.append(TAB).append(TAB).append("return ");
                    out.append(defaultReturnValue(method.m.getReturnType()));
                    out.append(';').append(LS);
                }
                out.append(TAB).append("}");
            } else {
                out.append(";");
            }
            out.append(LS).append(LS);
        }
    }

    /**
     * Writes header for constructor or method to {@link #out}
     *
     * @param retType return type for methods, empty string for constructors
     * @param name    name of constructor/class
     * @param params  parameters of constructor/class
     * @param excs    exceptions of constructor/class
     * @throws IOException if couldn't write to <code>out</code>
     * @see #writeParameters(Parameter[], boolean)
     */
    private void writeHead(String retType, String name, Parameter[] params, Class<?>[] excs) throws IOException {
        out.append(retType).append(" ");
        out.append(name).append("(");
        writeParameters(params, true);
        out.append(")");
        if (excs.length != 0) {
            out.append(" throws ");
            for (int i = 0; i < excs.length; i++) {
                out.append(excs[i].getCanonicalName());
                if (i < excs.length - 1) {
                    out.append(", ");
                }
            }
        }
    }

    /**
     * Writes parameters of constructor or method to {@link #out}
     *
     * @param params     parameters of constructor/class
     * @param writeTypes true if need to write types of parametrs, false if only names of parametrs
     * @throws IOException if couldn't write to <code>out</code>
     */
    private void writeParameters(Parameter[] params, boolean writeTypes) throws IOException {
        for (Integer i = 0; i < params.length; i++) {
            if (writeTypes) {
                out.append(params[i].getType().getCanonicalName()).append(" ");
            }
            out.append("param").append(i.toString());
            if (i < params.length - 1) {
                out.append(", ");
            }
        }
    }

    /**
     * Writes modifiers of constructor, class or method to {@link #out}
     *
     * @param mod  all modifiers of class
     * @param type all modifiers avaliable for class/constructor/method
     * @throws IOException if couldn't write to <code>out</code>
     */
    public void writeModifiers(int mod, int type) throws IOException {
        out.append(Modifier.toString(mod & ~Modifier.ABSTRACT & type)).append(" ");
    }

    /**
     * Gets some correct value for <code>type</code>
     * <p>
     * It returns only <code>0</code>, <code>false</code> and <code>null</code> really.
     *
     * @param type type, which default value to get
     * @return string, representing some correct value for specified type
     */
    private String defaultReturnValue(Type type) {
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isPrimitive()) {
                return clazz.equals(Boolean.TYPE) ? "false" : "0";
            } else {
                return "null";
            }
        } else {
            return "null";
        }
    }

    /**
     * Writes implementation's package name to {@link #out}
     *
     * @throws IOException if couldn't write to <code>out</code>
     */
    private void writePackage() throws IOException {
        if (clazz.getPackage() != null) {
            out.append("package ").append(clazz.getPackage().getName()).append(";").append(LS).append(LS);
        }
    }

    /**
     * Wrapper on Method, make it easy too use with HashSet
     */
    private class HashebleMethod {
        public final Method m;

        private HashebleMethod(Method m) {
            this.m = m;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof HashebleMethod)) {
                return false;
            }
            Method m2 = ((HashebleMethod) obj).m;

            return m.getName().equals(m2.getName()) && Arrays.equals(m.getParameterTypes(), m2.getParameterTypes());
        }

        @Override
        public int hashCode() {
            int hash = m.getName().hashCode();
            for (Parameter p : m.getParameters()) {
                hash ^= p.getType().hashCode();
            }
            return hash;
        }
    }
}