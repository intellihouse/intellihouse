package org.openhab.binding.intellihouse.jdo;

import static java.util.Objects.*;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombiClassLoader extends ClassLoader {

    private final Logger logger = LoggerFactory.getLogger(CombiClassLoader.class);
    private final List<ClassLoader> delegates;

    public CombiClassLoader(final List<ClassLoader> delegates) {
        this.delegates = new ArrayList<>(requireNonNull(delegates, "delegates"));
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        for (final ClassLoader delegate : delegates) {
            try {
                Class<?> clazz = delegate.loadClass(name);
                return clazz;
            } catch (ClassNotFoundException x) {
                logger.trace(x.toString(), x);
            } catch (NoClassDefFoundError x) {
                logger.trace(x.toString(), x);
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    public URL getResource(String name) {
        for (final ClassLoader delegate : delegates) {
            URL resource = delegate.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        final List<URL> result = new LinkedList<>();
        for (final ClassLoader delegate : delegates) {
            final Enumeration<URL> resources = delegate.getResources(name);
            while (resources.hasMoreElements()) {
                result.add(resources.nextElement());
            }
        }
        return new IteratorEnumeration<>(result.iterator());
    }

    protected static class IteratorEnumeration<E> implements Enumeration<E> {
        private final Iterator<E> iterator;

        public IteratorEnumeration(Iterator<E> iterator) {
            this.iterator = requireNonNull(iterator, "iterator");
        }

        @Override
        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        @Override
        public E nextElement() {
            return iterator.next();
        }
    }

    @Override
    protected URL findResource(String name) {
        throw new UnsupportedOperationException("Not implemented! Should never be called!");
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        throw new UnsupportedOperationException("Not implemented! Should never be called!");
    }

    @Override
    protected String findLibrary(String libname) {
        throw new UnsupportedOperationException("Not implemented! Should never be called!");
    }
}
