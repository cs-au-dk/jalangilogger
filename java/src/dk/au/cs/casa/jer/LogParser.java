package dk.au.cs.casa.jer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dk.au.cs.casa.jer.entries.*;
import dk.au.cs.casa.jer.entries.AllocationSiteObjectDescription;
import dk.au.cs.casa.jer.entries.BuiltinObjectDescription;
import dk.au.cs.casa.jer.entries.CallEntry;
import dk.au.cs.casa.jer.entries.ConcreteStringDescription;
import dk.au.cs.casa.jer.entries.DynamicCodeEntry;
import dk.au.cs.casa.jer.entries.FunctionEntry;
import dk.au.cs.casa.jer.entries.FunctionExitEntry;
import dk.au.cs.casa.jer.entries.IEntry;
import dk.au.cs.casa.jer.entries.ObjectDescription;
import dk.au.cs.casa.jer.entries.OtherDescription;
import dk.au.cs.casa.jer.entries.OtherObjectDescription;
import dk.au.cs.casa.jer.entries.OtherSymbolDescription;
import dk.au.cs.casa.jer.entries.PrefixStringDescription;
import dk.au.cs.casa.jer.entries.SourceLocation;
import dk.au.cs.casa.jer.entries.ValueDescription;
import dk.au.cs.casa.jer.entries.VariableOrPropertyEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Parser for an entire ValueLogger log file.
 */
public class LogParser {

    private final RawLogFile rawLogFile;

    private LinkedHashSet<IEntry> entries = null;

    private Metadata metadata = null;

    public LogParser(RawLogFile rawLogFile) {
        this.rawLogFile = rawLogFile;
    }

    private static Gson makeGsonParser() {
        Map<String, Object> cache = new HashMap<>();
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(VariableOrPropertyEntry.class, (JsonDeserializer<VariableOrPropertyEntry>) (json, typeOfT, context) ->
                parseJsonWithCache(json, context, LogParser::makeVariableOrPropertyFromJson, cache));
        builder.registerTypeAdapter(SourceLocation.class, (JsonDeserializer<SourceLocation>) (json, typeOfT, context) ->
                parseJsonWithCache(json, context, LogParser::makeSourceLocationFromJson, cache));
        builder.registerTypeAdapter(ValueDescription.class, (JsonDeserializer<ValueDescription>) (json, typeOfT, context) ->
                parseJsonWithCache(json, context, LogParser::makeValueDescriptionFromJSON, cache));
        builder.registerTypeAdapter(ObjectDescription.class, (JsonDeserializer<ObjectDescription>) (json, typeOfT, context) ->
                parseJsonWithCache(json, context, LogParser::makeObjectDescriptionFromJson, cache));
        builder.registerTypeAdapter(IEntry.class, (JsonDeserializer<IEntry>) (json, typeOfT, context) ->
                parseJsonWithCache(json, context, LogParser::makeIEntryFromJson, cache));
        return builder.create();
    }

    private static IEntry makeIEntryFromJson(JsonElement json, JsonDeserializationContext ctx) {
        String entryKind = json.getAsJsonObject().get("entryKind").getAsString();
        switch (entryKind) {
            case "dynamic-code":
                return ctx.deserialize(json, DynamicCodeEntry.class);
            case "read-variable":
            case "write-variable":
            case "read-property":
            case "write-property":
                return ctx.deserialize(json, VariableOrPropertyEntry.class);
            case "function-exit":
                return ctx.deserialize(json, FunctionExitEntry.class);
            case "function-entry":
                return ctx.deserialize(json, FunctionEntry.class);
            case "call":
                return ctx.deserialize(json, CallEntry.class);
            default:
                throw new RuntimeException("Unhandled case: " + entryKind);
        }
    }

    private static ObjectDescription makeObjectDescriptionFromJson(JsonElement json, JsonDeserializationContext ctx) {
        JsonObject obj = json.getAsJsonObject();
        String objectKind = obj.get("objectKind").getAsString();
        switch (objectKind) {
            case "allocation-site":
                return ctx.deserialize(obj, AllocationSiteObjectDescription.class);
            case "builtin":
                return ctx.deserialize(obj, BuiltinObjectDescription.class);
            case "other-symbol":
                return ctx.deserialize(obj, OtherSymbolDescription.class);
            case "other":
                return ctx.deserialize(obj, OtherObjectDescription.class);
            default:
                throw new RuntimeException("Unhandled case: " + objectKind);
        }
    }

    private static VariableOrPropertyEntry makeVariableOrPropertyFromJson(JsonElement json, JsonDeserializationContext context) {
        JsonObject obj = json.getAsJsonObject();
        int index = obj.has("index") ? context.deserialize(obj.get("index"), Integer.class) : -1;
        SourceLocation sourceLocation = context.deserialize(obj.get("sourceLocation"), SourceLocation.class);
        ValueDescription name = context.deserialize(obj.get("name"), ValueDescription.class);
        ValueDescription base = context.deserialize(obj.get("base"), ValueDescription.class);
        ValueDescription value = context.deserialize(obj.get("value"), ValueDescription.class);
        return new VariableOrPropertyEntry(index, sourceLocation, name, base, value);
    }

    private static SourceLocation makeSourceLocationFromJson(JsonElement json, JsonDeserializationContext context) {
        JsonObject obj = json.getAsJsonObject();
        return new SourceLocation(obj.get("lineNumber").getAsInt(), obj.get("columnNumber").getAsInt(), getFileName(obj));
    }

    @SuppressWarnings("unchecked")
    private static <T> T parseJsonWithCache(JsonElement json, JsonDeserializationContext context, BiFunction<JsonElement, JsonDeserializationContext, T> makeValueDescriptionFromJSON, Map<String, Object> cache) {
        String rawString = json.toString();
        if (!cache.containsKey(rawString)) {
            return makeValueDescriptionFromJSON.apply(json, context);
        }
        return (T) cache.get(rawString);
    }

    private static ValueDescription makeValueDescriptionFromJSON(JsonElement json, JsonDeserializationContext context) {
        JsonObject obj = json.getAsJsonObject();
        String valueKind = obj.get("valueKind").getAsString();
        switch (valueKind) {
            case "concrete-string":
                return new ConcreteStringDescription(obj.get("value").getAsString());
            case "prefix-string":
                return new PrefixStringDescription(obj.get("value").getAsString());
            case "abstract-primitive":
                return new OtherDescription(obj.get("value").getAsString());
            case "abstract-object":
                return context.deserialize(obj.get("value"), ObjectDescription.class);
            default:
                throw new RuntimeException("Unhandled case: " + valueKind);
        }
    }

    private static LinkedHashSet<IEntry> parseEntries(List<String> logFileEntries) {
        LinkedHashSet<IEntry> entries = new LinkedHashSet<>();
        Gson gson = makeGsonParser();
        for (String line : logFileEntries) {
            try {
                IEntry e = gson.fromJson(line, IEntry.class);
                entries.add(e);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println(String.format("Error during parsing of line: %n%s", line));
            }
        }
        return entries;
    }

    private static String getFileName(JsonObject obj) {
        return obj.get("fileName").getAsString().replace("_orig_", "");
    }

    public LinkedHashSet<IEntry> getEntries() {
        if (entries == null) {
            List<String> linesWithoutMeta = rawLogFile.getLines().subList(1, rawLogFile.getLines().size());
            entries = parseEntries(linesWithoutMeta);
        }
        return entries;
    }

    public Metadata getMetadata() {
        if (metadata == null) {
            //First line contains the metadata
            metadata = new Metadata(rawLogFile.getLines().get(0));
        }
        return metadata;
    }
}

