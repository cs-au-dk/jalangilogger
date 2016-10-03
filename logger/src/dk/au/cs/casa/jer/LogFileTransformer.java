package dk.au.cs.casa.jer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transforms source locations in a log file:
 * <ul>
 * <li>replaces absolute paths with relative paths</li>
 * <li>re-inlines eventhandlers & inline-scripts</li>
 * </ul>
 */
public class LogFileTransformer {

    private final Path root;

    private final Path instrumentationRoot;

    private final Path rootRelativeMain;

    public LogFileTransformer(Path root, Path instrumentationRoot, Path rootRelativeMain) {
        this.root = root;
        this.instrumentationRoot = instrumentationRoot;
        this.rootRelativeMain = rootRelativeMain;
    }

    public Path transform(Path inputLog) throws IOException {
        Map<Integer, SourcePosition> inlineJSOffsetSourceLocations = fillInlineJSOffsetSourceLocationLineNumbers(root.resolve(rootRelativeMain));
        Path modified = null;
        try {
            modified = transformFile(inlineJSOffsetSourceLocations, inputLog);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return modified;
    }

    private Path transformFile(Map<Integer, SourcePosition> inlineJSOffsetSourceLocations, Path inputLog) throws IOException, JSONException { //UnchangedLogFiles/fileName
        BufferedReader reader = new BufferedReader(new FileReader(inputLog.toFile()));
        //Make directories to the changed log file
        ArrayList<String> linesToBeWritten = new ArrayList<>();
        String line = reader.readLine();
        while (line != null) {
            JSONObject readedJsonObj = new JSONObject(line);

            JSONObject JsonObjToBeWritten = iterateThroughJSONObjectAndChangeSL(readedJsonObj, inlineJSOffsetSourceLocations);
            linesToBeWritten.add(JsonObjToBeWritten.toString());

            line = reader.readLine();
        }
        reader.close();
        Path modified = inputLog.getParent().resolve(inputLog.getFileName().toString() + ".transformed");
        Files.write(modified, linesToBeWritten);
        return modified;
    }

    //Takes an JSONObject and iterates through it, to find all source locations and then change these to
    //fit with the sourcelocations from TAJS, and then returns the updated JSONobject, which should be written to file
    private JSONObject iterateThroughJSONObjectAndChangeSL(JSONObject obj, Map<Integer, SourcePosition> inlineJSOffsetSourceLocations) throws IOException, JSONException {
        Iterator<String> keys = obj.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            Object next = obj.get(key);

            if (next instanceof String) {
                //Do nothing.
            } else if (next instanceof JSONObject) {
                JSONObject nextJSON = (JSONObject) next;
                if (key.equals("sourceLocation") || key.equals("allocationSite")) {
                    obj.put(key, transformSourceLocation(nextJSON, inlineJSOffsetSourceLocations));
                } else {
                    obj.put(key, iterateThroughJSONObjectAndChangeSL(nextJSON, inlineJSOffsetSourceLocations));
                }
            } else if (next instanceof JSONArray) {
                JSONArray nextJSONArray = (JSONArray) next;
                for (int i = 0; i < nextJSONArray.length(); i++) {
                    nextJSONArray.put(i, iterateThroughJSONObjectAndChangeSL(nextJSONArray.getJSONObject(i), inlineJSOffsetSourceLocations));
                }
            } else {
                throw new IllegalStateException("This should not happen - Unhandled class: " + next.getClass());
            }
        }
        return obj;
    }

    //This converts jalangis source locations into TAJS source locations. Jalangi handles event-handler and
    //inline js source locations different than TAJS does.
    private JSONObject transformSourceLocation(JSONObject obj, Map<Integer, SourcePosition> inlineJSOffsetSourceLocations) throws IOException, JSONException {
        String fileName = obj.getString("fileName");
        Integer lineNumber = obj.getInt("lineNumber");
        Integer columnNumber = obj.getInt("columnNumber");
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
            obj.put("fileName", rootRelativeMain.getFileName().toString());
            obj.put("lineNumber", newLineNumber);
            obj.put("columnNumber", newColumnNumber);
        } else if (inlineHandlerNumber != -1) {
            String eventhandlerFileName = "event-handler-" + inlineHandlerNumber + "_orig_.js";
            Path eventHandlerFile = instrumentationRoot.resolve(rootRelativeMain).getParent().resolve(eventhandlerFileName);
            obj = updateSourceLocationFromEventFileToSLInOriginalFile(eventHandlerFile, obj);
        } else if (fileNamePath.isAbsolute()) {
            final Path relativized = instrumentationRoot.relativize(fileNamePath.getParent()).resolve(correctedFileName);
            Path rootRelativeFile = root.resolve(relativized);
            Path main = root.resolve(rootRelativeMain);
            final Path mainRelativeFile = main.getParent().relativize(rootRelativeFile);
            obj.put("fileName", mainRelativeFile.toString());
        }
        return obj;
    }

    private JSONObject updateSourceLocationFromEventFileToSLInOriginalFile(Path eventHandlerFile, JSONObject obj) throws IOException, JSONException {
        String lineToSearchForInOriginalFile = getLineToSearchForFromEventHandlerFile(eventHandlerFile, obj);
        BufferedReader reader = new BufferedReader(new FileReader(root.resolve(rootRelativeMain).toFile()));
        String line = reader.readLine();
        int lineNumber = 1;
        boolean stringFound = false;
        boolean inScript = false;
        while (line != null) {
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
                obj.put("fileName", rootRelativeMain.getFileName().toString());
                obj.put("lineNumber", lineNumber);
                obj.put("columnNumber", line.indexOf(lineToSearchForInOriginalFile) + obj.getInt("columnNumber"));
                stringFound = true;
            }
            lineNumber++;
            line = reader.readLine();
        }
        if (!stringFound) {
            throw new IllegalStateException("Eventhandler part of code not found in original file");
        }
        return obj;
    }

    private String getLineToSearchForFromEventHandlerFile(Path eventHandlerFile, JSONObject obj)
            throws JSONException, IOException {
        int lineNumber = obj.getInt("lineNumber");
        BufferedReader reader = new BufferedReader(new FileReader(eventHandlerFile.toFile()));

        String line = reader.readLine();
        lineNumber--;
        while (lineNumber > 0) {
            line = reader.readLine();
            lineNumber--;
        }
        reader.close();
        return line;
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
        String line = reader.readLine();
        int lineNumber = 0;
        while (line != null) {
            Pattern pattern = Pattern.compile("<script.*>");
            Matcher matcher = pattern.matcher(line);

            if (matcher.find()) {
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

