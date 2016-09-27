package dk.au.cs.casa.jer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.empty;

/**
 * Transforms source locations in a log file:
 * <ul>
 * <li>replaces absolute paths with relative paths</li>
 * <li>re-inlines eventhandlers & inline-scripts</li>
 * </ul>
 */
public class LogFileTransformer {

    private static final Gson gson = new Gson(); // TODO use typed version!!! (this is from a json.org -> gson rewrite. It should be changed when then rewrite is tested.)

    private final Path root;

    private final Path instrumentationRoot;

    private final Path rootRelativeMain;

    public LogFileTransformer(Path root, Path instrumentationRoot, Path rootRelativeMain) {
        this.root = root;
        this.instrumentationRoot = instrumentationRoot;
        this.rootRelativeMain = rootRelativeMain;
    }

    public List<String> transform(List<String> inputLog) throws IOException {
        Map<Integer, SourcePosition> inlineJSOffsetSourceLocations = fillInlineJSOffsetSourceLocationLineNumbers(root.resolve(rootRelativeMain));
        return transform(inlineJSOffsetSourceLocations, inputLog);
    }

    private List<String> transform(Map<Integer, SourcePosition> inlineJSOffsetSourceLocations, List<String> lines) {
        return lines.stream().flatMap(line -> {
            try {
                JsonObject untransformedJSON = gson.fromJson(line, JsonObject.class);
                JsonObject transformedJSON = iterateThroughJsonObjectAndChangeSL(untransformedJSON, inlineJSOffsetSourceLocations);
                String transformedLine = transformedJSON.toString();
                return Stream.of(transformedLine);
            } catch (Exception e) {
                System.err.println("Something went wrong during transformation of line: " + line + ": "  + e);
                //throw new RuntimeException("Something went wrong during transformation of line: " + line, e);
                return Stream.empty();
            }
        }).collect(Collectors.toList());
    }

    //Takes an JsonObject and iterates through it, to find all source locations and then change these to
    //fit with the sourcelocations from TAJS, and then returns the updated JSONobject, which should be written to file
    private JsonObject iterateThroughJsonObjectAndChangeSL(JsonObject obj, Map<Integer, SourcePosition> inlineJSOffsetSourceLocations) {
        Iterator<Map.Entry<String, JsonElement>> properties = obj.entrySet().iterator(); // XXX this weird iterator style is from a json.org -> gson rewrite. It should be changed when then rewrite is tested.

        while (properties.hasNext()) {
            Map.Entry<String, JsonElement> property = properties.next();
            String key = property.getKey();
            Object next = property.getValue();

            if (next instanceof JsonPrimitive) {
                //Do nothing.
            } else if (next instanceof JsonObject) {
                JsonObject nextJSON = (JsonObject) next;
                if (key.equals("sourceLocation") || key.equals("allocationSite")) {
                    obj.add(key, transformSourceLocation(nextJSON, inlineJSOffsetSourceLocations));
                } else {
                    obj.add(key, iterateThroughJsonObjectAndChangeSL(nextJSON, inlineJSOffsetSourceLocations));
                }
            } else if (next instanceof JsonArray) {
                JsonArray nextJsonArray = (JsonArray) next;
                for (int i = 0; i < nextJsonArray.size(); i++) {
                    nextJsonArray.set(i, iterateThroughJsonObjectAndChangeSL(nextJsonArray.get(i).getAsJsonObject(), inlineJSOffsetSourceLocations));
                }
            } else {
                throw new IllegalStateException("This should not happen - Unhandled class: " + next.getClass());
            }
        }
        return obj;
    }

    //This converts jalangis source locations into TAJS source locations. Jalangi handles event-handler and
    //inline js source locations different than TAJS does.
    private JsonObject transformSourceLocation(JsonObject obj, Map<Integer, SourcePosition> inlineJSOffsetSourceLocations) {
        String fileName = obj.get("fileName").getAsString();
        Integer lineNumber = obj.get("lineNumber").getAsInt();
        Integer columnNumber = obj.get("columnNumber").getAsInt();
//		if(lineNumber == -1 && columnNumber == -1){ //in case Jalangi has sourcelocation iid.
//			throw new IllegalSourceLocationException();
//		}
        Path fileNamePath = Paths.get(fileName);
        final String correctedFileName = fileNamePath.getFileName().toString().replace("_orig_.js", ".js");
        int inlineScriptNumber = findInlineNumber("inline-(.*).js", correctedFileName);
        int inlineHandlerNumber = findInlineNumber("event-handler-(.*).js", correctedFileName);
        if (inlineScriptNumber != -1) {
            SourcePosition sourcePosition = inlineJSOffsetSourceLocations.get(inlineScriptNumber);
            int newLineNumber = lineNumber + sourcePosition.line;
            int newColumnNumber = columnNumber + (lineNumber == 1 ? sourcePosition.column : 0);
            obj.addProperty("fileName", rootRelativeMain.getFileName().toString());
            obj.addProperty("lineNumber", newLineNumber);
            obj.addProperty("columnNumber", newColumnNumber);
        } else if (inlineHandlerNumber != -1) {
            String eventhandlerFileName = "event-handler-" + inlineHandlerNumber + "_orig_.js";
            Path eventHandlerFile = instrumentationRoot.resolve(rootRelativeMain).getParent().resolve(eventhandlerFileName);
            obj = updateSourceLocationFromEventFileToSLInOriginalFile(eventHandlerFile, obj);
        } else if (fileNamePath.isAbsolute()) {
            final Path relativized = instrumentationRoot.relativize(fileNamePath.getParent()).resolve(correctedFileName);
            Path rootRelativeFile = root.resolve(relativized);
            Path main = root.resolve(rootRelativeMain);
            final Path mainRelativeFile = main.getParent().relativize(rootRelativeFile);
            obj.addProperty("fileName", mainRelativeFile.toString());
        }
        return obj;
    }

    private JsonObject updateSourceLocationFromEventFileToSLInOriginalFile(Path eventHandlerFile, JsonObject obj) {
        String lineToSearchForInOriginalFile = getLineToSearchForFromEventHandlerFile(eventHandlerFile, obj);
        try (BufferedReader reader = new BufferedReader(new FileReader(root.resolve(rootRelativeMain).toFile()))) {
            int lineNumber = 1;
            boolean stringFound = false;
            boolean inScript = false;
            String line;
            while ((line = reader.readLine()) != null) {
                int scriptStartIndex = 0;
                if (line.contains("<script")) {
                    scriptStartIndex = line.indexOf("<script");
                    inScript = true;
                }
                if (line.contains("</script>")) {
                    inScript = false;
                }
                line = line.replace("&lt;", "<").replace("&gt;", ">");
                if ((!inScript || line.indexOf(lineToSearchForInOriginalFile) < scriptStartIndex) && line.contains(lineToSearchForInOriginalFile)) {
                    obj.addProperty("fileName", rootRelativeMain.getFileName().toString());
                    obj.addProperty("lineNumber", lineNumber);
                    obj.addProperty("columnNumber", line.indexOf(lineToSearchForInOriginalFile) + obj.get("columnNumber").getAsInt());
                    stringFound = true;
                }
                lineNumber++;
            }
            if (!stringFound) {
                throw new IllegalStateException("Eventhandler part of code not found in original file");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return obj;
    }

    private String getLineToSearchForFromEventHandlerFile(Path eventHandlerFile, JsonObject obj) {
        int lineNumber = obj.get("lineNumber").getAsInt();
        try (BufferedReader reader = new BufferedReader(new FileReader(eventHandlerFile.toFile()))) {
            String line = reader.readLine();
            lineNumber--;
            while (lineNumber > 0) {
                line = reader.readLine();
                lineNumber--;
            }
            return line;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int findInlineNumber(String regexp, String fileName) {
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return -1;
    }

    //Takes the original HTML file and fills in JSOffset (both line and column numbers) of the inline scripts occurring in the HTML file
    private Map<Integer, SourcePosition> fillInlineJSOffsetSourceLocationLineNumbers(Path mainFile) throws IOException {
        Map<Integer, SourcePosition> inlineJSOffsetSourceLocations = new HashMap<>();
        if (mainFile.toString().endsWith(".js")) {
            return inlineJSOffsetSourceLocations;
        }
        BufferedReader reader = new BufferedReader(new FileReader(mainFile.toFile()));

        Pattern pattern = Pattern.compile("<script[^<.]*>");
        Pattern typePattern = Pattern.compile("type=['\"]text\\/javascript['\"]");
        Pattern anyTypePattern = Pattern.compile("type=");
        Pattern externalPattern = Pattern.compile("<script.*src=.*>");

        String line = reader.readLine();
        int lineNumber = 0;
        while (line != null) {
            Matcher matcher = pattern.matcher(line);
            Matcher typeMatcher = typePattern.matcher(line);
            Matcher anyTypeMatcher = anyTypePattern.matcher(line);
            Matcher externalMatcher = externalPattern.matcher(line);

            if (matcher.find() && (typeMatcher.find() || !anyTypeMatcher.find()) && !externalMatcher.matches())  {
                String match = matcher.group();
                int columnNumber = match.length() + line.indexOf(match);
                inlineJSOffsetSourceLocations.put(inlineJSOffsetSourceLocations.size(), new SourcePosition(lineNumber, columnNumber));
            }

            line = reader.readLine();
            lineNumber++;
        }
        reader.close();
        return inlineJSOffsetSourceLocations;
    }

    private static class SourcePosition {

        public final int line;

        public final int column;

        public SourcePosition(int line, int column) {
            this.line = line;
            this.column = column;
        }

        @Override
        public String toString() {
            return String.format("%d:%d", line, column);
        }
    }
}

