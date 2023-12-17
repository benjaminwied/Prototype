package com.github.cleverelephant.prototype;

import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

public class LuaInterpreter
{
    private Globals globals;

    public LuaInterpreter(Map<String, Object> context)
    {
        this(null, context);
    }

    public LuaInterpreter(Path basePath, Map<String, Object> context)
    {
        globals = JsePlatform.standardGlobals();
        for (Entry<String, Object> entry : context.entrySet())
            globals.set(entry.getKey(), CoerceJavaToLua.coerce(entry.getValue()));
        globals.set("prototypes", new LuaTable());

        if (basePath != null)
            globals.get("package").set("path", LuaString.valueOf(basePath.toString()) + "/?.lua");
    }

    public void runScript(String file)
    {
        if (file.endsWith(".lua"))
            file = file.substring(0, file.length() - 4);
        globals.load("require(\"" + file + "\")").call();
    }

    public ObjectNode computeData()
    {
        LuaTable prototypes = globals.get("prototypes").checktable();

        ObjectNode result = new ObjectNode(JsonNodeFactory.instance);

        LuaValue key = LuaValue.NIL;
        while (true) {
            Varargs data = prototypes.next(key);
            if (data == LuaValue.NIL)
                return result;
            key = data.arg(1);
            LuaTable prototype = data.arg(2).checktable();
            String c = prototype.get("class").checkjstring();
            JsonNode d = toJson(prototype.get("data").checktable());

            ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
            node.set(c, d);
            result.set(key.checkjstring(), node);
        }
    }

    public static JsonNode toJson(LuaTable table)
    {
        if (table.length() == 0)
            return tableToObject(table);
        return tableToArray(table);
    }

    private static JsonNode tableToArray(LuaTable table)
    {
        ArrayNode node = new ArrayNode(JsonNodeFactory.instance);

        LuaValue key = LuaValue.NIL;
        while (true) {
            Varargs data = table.next(key);
            if (data == LuaValue.NIL)
                return node;
            key = data.arg(1);
            LuaValue value = data.arg(2);

            if (value.istable())
                node.add(toJson(value.checktable()));
            else if (value.isboolean())
                node.add(value.checkboolean());
            else if (value.isint())
                node.add(value.checkint());
            else if (value.islong())
                node.add(value.checklong());
            else if (value.type() == LuaValue.TNUMBER)
                node.add(value.checkdouble());
            else if (value.isnil())
                node.addNull();
            else if (value.isstring())
                node.add(value.checkjstring());
            else if (value.isuserdata())
                node.addPOJO(value.touserdata());
            else
                throw new UnsupportedOperationException();
        }
    }

    private static JsonNode tableToObject(LuaTable table)
    {
        ObjectNode node = new ObjectNode(JsonNodeFactory.instance);

        LuaValue key = LuaValue.NIL;
        while (true) {
            Varargs data = table.next(key);
            if (data == LuaValue.NIL)
                return node;
            key = data.arg(1);
            LuaValue value = data.arg(2);

            if (value.istable())
                node.set(key.checkjstring(), toJson(value.checktable()));
            else if (value.isboolean())
                node.put(key.checkjstring(), value.checkboolean());
            else if (value.isint())
                node.put(key.checkjstring(), value.checkint());
            else if (value.islong())
                node.put(key.checkjstring(), value.checklong());
            else if (value.type() == LuaValue.TNUMBER)
                node.put(key.checkjstring(), value.checkdouble());
            else if (value.isnil())
                node.putNull(key.checkjstring());
            else if (value.isstring())
                node.put(key.checkjstring(), value.checkjstring());
            else if (value.isuserdata())
                node.putPOJO(key.checkjstring(), value.touserdata());
            else
                throw new UnsupportedOperationException();
        }
    }
}
