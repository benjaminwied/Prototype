package com.github.cleverelephant.prototype;

import java.util.List;
import java.util.Optional;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.script.LuaScriptEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuaInterpreter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LuaInterpreter.class);
    public static final LuaInterpreter INSTANCE = new LuaInterpreter();
    private LuaScriptEngine scriptEngine;

    private LuaInterpreter()
    {
        scriptEngine = new LuaScriptEngine();
    }

    public void updatePrototype(PrototypeDefinition definition, String script)
    {
        try {
            definition.getScripts().add(scriptEngine.compile(script));
        } catch (ScriptException e) {
            LOGGER.error("Error while updating prototype", e);
        }
    }

    public void updatePrototype(String name, String script)
    {
        Optional<PrototypeDefinition> optional = PrototypeRegistry.getDefinition(name);
        PrototypeDefinition definition = optional.orElseGet(PrototypeDefinition::new);
        updatePrototype(definition, script);
    }

    public JsonNode evalPrototypeDefinition(PrototypeDefinition definition)
    {
        try {
            Bindings bindings = runScripts(definition.getScripts());

            JsonNode node = toJson((LuaTable) bindings.get("data"));
            String className = (String) bindings.get("class");

            ObjectNode result = new ObjectNode(JsonNodeFactory.instance);
            result.set(className, node);
            return result;
        } catch (ScriptException e) {
            LOGGER.error("Error while updating prototype", e);
            return null;
        }
    }

    private Bindings runScripts(List<CompiledScript> scripts) throws ScriptException
    {
        Bindings bindings = scriptEngine.createBindings();
        bindings.putAll(PrototypeContext.getCurrentContext());

        for (CompiledScript script : scripts)
            script.eval(bindings);

        return bindings;
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
