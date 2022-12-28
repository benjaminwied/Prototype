package com.github.cleverelephant.prototypetest;

import com.github.cleverelephant.prototype.Prototype;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface TestPrototype extends Prototype<String>
{
    @JsonProperty("a")
    String a();
    int b();
    boolean c();
    String d();
    String[] array();
    double doubleValue();
    List<SimpleContainer> generic();

    default String $d()
    {
        return "abc";
    }

    class SimpleContainer
    {
        private int x;
        private String y;

        public SimpleContainer()
        {
        }

        public SimpleContainer(int x, String y)
        {
            this.x = x;
            this.y = y;
        }

        public int getX()
        {
            return x;
        }

        public void setX(int x)
        {
            this.x = x;
        }

        public String getY()
        {
            return y;
        }

        public void setY(String y)
        {
            this.y = y;
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("SimpleContainer [x=").append(x).append(", y=").append(y).append("]");
            return builder.toString();
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(x, y);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if ((obj == null) || (getClass() != obj.getClass()))
                return false;
            SimpleContainer other = (SimpleContainer) obj;
            return x == other.x && Objects.equals(y, other.y);
        }

    }
}
