package com.github.cleverelephant.prototype.materialization;

import com.github.cleverelephant.prototype.Prototype;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAnnotation
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MyAnnotation.class);

    private final Class<? extends Annotation> annotationType;
    private final Map<String, Object> values;

    public MyAnnotation(Class<? extends Annotation> annotationType)
    {
        if (!annotationType.isAnnotation())
            throw new IllegalArgumentException("Type not an annotation: " + annotationType);

        this.annotationType = annotationType;
        values = new HashMap<>();

        /* Add default values */
        for (Method method : annotationType.getDeclaredMethods())
            values.put(method.getName(), method.getDefaultValue());
    }

    public MyAnnotation(Annotation annotation)
    {
        annotationType = annotation.annotationType();
        values = new HashMap<>();

        /* Add annotation values */
        for (Method method : annotationType.getDeclaredMethods())
            try {
                values.put(method.getName(), method.invoke(annotation));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new UnsupportedOperationException(e);
            }
    }

    public MyAnnotation(MyAnnotation annotation)
    {
        annotationType = annotation.annotationType;
        values = new HashMap<>(annotation.values);
    }

    public boolean isDefaulMapping(String key)
    {
        try {
            Object def = annotationType.getDeclaredMethod(key).getDefaultValue();
            return Objects.equals(def, values.get(key));
        } catch (NoSuchMethodException | SecurityException e) {
            LOGGER.trace(Prototype.LOG_MARKER, "", e);
            return !values.containsKey(key);
        }
    }

    public Object getDefaultMapping(String key)
    {
        try {
            return annotationType.getDeclaredMethod(key).getDefaultValue();
        } catch (NoSuchMethodException | SecurityException e) {
            LOGGER.warn(
                    Prototype.LOG_MARKER, "no annotation property {} found for annotation type {}", key, annotationType,
                    e
            );
            return null;
        }
    }

    public MyAnnotation addMapping(String key, Object value)
    {
        values.put(key, value);
        return this;
    }

    public Class<? extends Annotation> getAnnotationType()
    {
        return annotationType;
    }

    public Map<String, Object> getValues()
    {
        return values;
    }
}
