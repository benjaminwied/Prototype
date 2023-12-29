/**
 * MIT License
 *
 * Copyright (c) 2023 Benjamin Wied
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.github.cleverelephant.prototypetest;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.cleverelephant.prototype.Prototype;

@SuppressWarnings("javadoc")
public class TestPrototype extends Prototype<String>
{
    public static class SimpleContainer
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

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            SimpleContainer other = (SimpleContainer) obj;
            return x == other.x && Objects.equals(y, other.y);
        }

        public int getX()
        {
            return x;
        }

        public String getY()
        {
            return y;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(x, y);
        }

        public void setX(int x)
        {
            this.x = x;
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

    }

    @JsonProperty("a")
    public String a;
    public int b;
    public boolean c;
    public String d = "abc";
    public String[] array;
    public double doubleValue;
    public List<SimpleContainer> generic;

    @Override
    public String build()
    {
        throw new UnsupportedOperationException();
    }
}
