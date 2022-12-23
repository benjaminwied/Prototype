package com.github.cleverelephant.prototype;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.OptBoolean;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

@SuppressWarnings("javadoc")
@JsonTypeInfo(use = Id.CLASS, include = As.PROPERTY, property = "$prototypeClass")
public interface Prototype<T>
{
    Marker LOG_MARKER = MarkerFactory.getMarker("prototype");

    @JacksonInject(useInput = OptBoolean.FALSE, value = "name")
    String getName();
}
