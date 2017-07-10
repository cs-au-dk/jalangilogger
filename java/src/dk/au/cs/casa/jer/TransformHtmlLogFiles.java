package dk.au.cs.casa.jer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransformHtmlLogFiles {

	private static final Gson gson = new Gson(); // TODO use typed version!!! (this is from a json.org -> gson rewrite. It should be changed when then rewrite is tested.)

	private static final String startInstrumentedDirectory = "instrumentedHTMLFiles/";
	private static final String unchangedLogFileDirectory = "nodeJSServer/UnchangedLogFiles/";
	private static final String changedLogFileDirectory = "JalangiLogFiles/";
	private static final String originalTestFileDirectory = "test/";
	
	private static ArrayList<Integer> inlineJSOffsetSourceLocationsLineNumbers = new ArrayList<Integer>();
	private static ArrayList<Integer> inlineJSOffsetSourceLocationsColumnNumbers = new ArrayList<Integer>();
		
	public static void main(String[] args) throws Exception {
		loopThroughAllFilesAndTransformThem("");
		System.out.println("All files has been transformed");
	}

	private static void loopThroughAllFilesAndTransformThem(String path) throws Exception {
		File[] faFiles = new File(unchangedLogFileDirectory + path).listFiles();
		for(File file: faFiles){
			if(file.isFile() && file.getName().endsWith(".log")){
				try {
					String logFileName = path + file.getName();
					String htmlFileName = file.getName().substring(0, file.getName().length() - 3) + "html";
					String originalHTMLFile = (originalTestFileDirectory + path + htmlFileName);
					String instrumentedDirectory = startInstrumentedDirectory + path + htmlFileName + "/";
					//Jalangi creates a folder of the same name as the file under instrumentation.
					removeCommasAndEmptyLinesAndDuplicates(logFileName);
					fillInlineJSOffsetSourceLocationLineNumbers(originalHTMLFile);
					transformFile(logFileName, originalHTMLFile, instrumentedDirectory);
				} catch (FileNotFoundException e){
					e.printStackTrace();
					continue;
				}
			} else if (file.isDirectory()){
				loopThroughAllFilesAndTransformThem(path + file.getName() + "/");
			}
		}
	}

	private static void removeCommasAndEmptyLinesAndDuplicates(String file) throws Exception{
		BufferedReader reader = new BufferedReader(new FileReader(unchangedLogFileDirectory + file));
		String line = reader.readLine();
		List<String> linesInNewFile = new ArrayList<String>();
		while(line != null){
			if(line.startsWith(",")){
				line = line.substring(1); //Remove comma
			}
			if(line.trim().equals("")){
				line = reader.readLine();
				continue; //Don't add empty lines
			}
			
			if(!line.startsWith("{\"entryKind\":")){
				throw new Exception("Line should start with {\"entryKind\":");
			}
			if(!linesInNewFile.contains(line)) 
				linesInNewFile.add(line); //Only add lines which is not duplicate
			
			line = reader.readLine();
		}
		reader.close();
		//Now linesInNewFile contains all the unique objects.
		FileWriter writer = new FileWriter(unchangedLogFileDirectory + file);
		for(String obj : linesInNewFile){
			writer.write(obj + "\r\n");
		}
		writer.close();	
	}

	private static void transformFile(String logFile, String originalHTMLFile, String instrumentedDirectory) throws Exception { //UnchangedLogFiles/fileName
		BufferedReader reader = new BufferedReader(new FileReader(unchangedLogFileDirectory + logFile));
		//Make directories to the changed log file
		String newFileName = changedLogFileDirectory + logFile;
		new File(newFileName.substring(0, newFileName.lastIndexOf("/"))).mkdirs();
		ArrayList<String> linesToBeWritten = new ArrayList<String>();
		String line = reader.readLine();
		while(line != null){
			JsonObject readedJsonObj = gson.fromJson(line, JsonObject.class);
			try {
				JsonObject JsonObjToBeWritten = iterateThroughJsonObjectAndChangeSL(readedJsonObj, originalHTMLFile, instrumentedDirectory);
				linesToBeWritten.add(JsonObjToBeWritten.toString() + "\r\n");
			} catch (IllegalSourceLocationException exception){} //If illegal sourceLocation (i.e. iid in Jalangi) just ignore this line
			line = reader.readLine();
		}
		reader.close();
		FileWriter writer = new FileWriter(newFileName);
		for(String lineToBeWritten : linesToBeWritten){
			writer.write(lineToBeWritten);
		}
		writer.close();
	}
	//Takes an JsonObject and iterates through it, to find all source locations and then change these to
	//fit with the sourcelocations from TAJS, and then returns the updated JSONobject, which should be written to file
	private static JsonObject iterateThroughJsonObjectAndChangeSL(JsonObject obj, String originalHTMLFile, String instrumentedDirectory) throws Exception{
		Iterator<Map.Entry<String, JsonElement>> properties = obj.entrySet().iterator(); // XXX this weird iterator style is from a json.org -> gson rewrite. It should be changed when then rewrite is tested.

		while (properties.hasNext()) {
			Map.Entry<String, JsonElement> property = properties.next();
			String key = property.getKey();
			Object next = property.getValue();

			if(next instanceof JsonPrimitive){
				//Do nothing.
			} else if(next instanceof JsonObject){
				JsonObject nextJSON = (JsonObject) next;
				if(key.equals("sourceLocation") || key.equals("allocationSite")){
					obj.add(key, transformSourceLocation(nextJSON, originalHTMLFile, instrumentedDirectory));
				} else {
					obj.add(key, iterateThroughJsonObjectAndChangeSL(nextJSON, originalHTMLFile, instrumentedDirectory));
				}
			} else if(next instanceof JsonArray){
				JsonArray nextJsonArray = (JsonArray) next;
				for(int i = 0; i < nextJsonArray.size(); i++){
					nextJsonArray.set(i, iterateThroughJsonObjectAndChangeSL(nextJsonArray.get(i).getAsJsonObject(), originalHTMLFile, instrumentedDirectory));
				}
			} else {	
				throw new Exception("This should not happen - Unhandled class: " + next.getClass());		
			}
		}
		return obj;
	}

	//This converts jalangis source locations into TAJS source locations. Jalangi handles event-handler and
	//inline js source locations different than TAJS does.
	private static JsonObject transformSourceLocation(JsonObject obj, String originalHTMLFile, String instrumentedDirectory) throws Exception {
		String fileName = obj.get("fileName").getAsString();
		Integer lineNumber = obj.get("lineNumber").getAsInt();
		Integer columnNumber = obj.get("columnNumber").getAsInt();
//		if(lineNumber == -1 && columnNumber == -1){ //in case Jalangi has sourcelocation iid.
//			throw new IllegalSourceLocationException();
//		}
		if(fileName.contains("inline-") && fileName.contains("_orig_.js")){
			int inlineNumber = findInlineNumber("inline-(.*)_orig_.js", fileName);
			int newLineNumber = lineNumber + inlineJSOffsetSourceLocationsLineNumbers.get(inlineNumber);
			int newColumnNumber = columnNumber + (lineNumber == 1 ? inlineJSOffsetSourceLocationsColumnNumbers.get(inlineNumber) : 0);
			obj.addProperty("fileName", originalHTMLFile);
			obj.addProperty("lineNumber", newLineNumber);
			obj.addProperty("columnNumber", newColumnNumber);
		} else if(fileName.contains("event-handler-") && fileName.contains("_orig_.js")){
			int inlineNumber = findInlineNumber("event-handler-(.*)_orig_.js", fileName);
			String eventHandlerFile = instrumentedDirectory + "event-handler-" + inlineNumber + "_orig_.js";
			obj = updateSourceLocationFromEventFileToSLInOriginalFile(eventHandlerFile, originalHTMLFile, obj);	
		}
		return obj;
	}
	
	private static JsonObject updateSourceLocationFromEventFileToSLInOriginalFile(String eventHandlerFile, String originalHTMLFile, JsonObject obj) throws Exception {
		String lineToSearchForInOriginalFile = getLineToSearchForFromEventHandlerFile(eventHandlerFile, obj);
		BufferedReader reader = new BufferedReader(new FileReader(originalHTMLFile));
		String line = reader.readLine();
		int lineNumber = 1;
		boolean stringFound = false;
		boolean inScript = false;
		while(line != null){
			int scriptStartIndex = 0;
			if(line.contains("<script")){
				scriptStartIndex = line.indexOf("<script");
				inScript = true;
			}
			if(line.contains("</script>")){
				inScript = false;
			}
			line = line.replace("&lt;", "<").replace("&gt;", ">");
			if((!inScript || line.indexOf(lineToSearchForInOriginalFile) < scriptStartIndex) && line.contains(lineToSearchForInOriginalFile)){
				obj.addProperty("fileName", originalHTMLFile);
				obj.addProperty("lineNumber", lineNumber);
				obj.addProperty("columnNumber", line.indexOf(lineToSearchForInOriginalFile) + obj.get("columnNumber").getAsInt());
				stringFound = true;		
			}
			lineNumber++;
			line = reader.readLine();
		}
		if(!stringFound){
			throw new Exception("Eventhandler part of code not found in original file");
		}
		return obj;
	}


	private static String getLineToSearchForFromEventHandlerFile(String eventHandlerFile, JsonObject obj) throws IOException {
		int lineNumber = obj.get("lineNumber").getAsInt();
		BufferedReader reader = new BufferedReader(new FileReader(eventHandlerFile));
		
		String line = reader.readLine();
		lineNumber--;
		while(lineNumber > 0){
			line = reader.readLine();
			lineNumber--;
		}
		reader.close();
		return line;
	}


	private static Integer findInlineNumber(String regexp, String fileName){
		Pattern pattern = Pattern.compile(regexp);
		Matcher matcher = pattern.matcher(fileName);
		matcher.find();
		return Integer.parseInt(matcher.group(1));
	}
	//Takes the original HTML file and fills in JSOffset (both line and column numbers) of the inline scripts occurring in the HTML file
	private static void fillInlineJSOffsetSourceLocationLineNumbers(String file) throws Exception {
		inlineJSOffsetSourceLocationsLineNumbers = new ArrayList<Integer>();
		inlineJSOffsetSourceLocationsColumnNumbers = new ArrayList<Integer>();
		
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		int lineNumber = 0;
		while(line != null){
			Pattern pattern = Pattern.compile("<script.*>");
			Matcher matcher = pattern.matcher(line);

			if(matcher.find()){
				String match = matcher.group();
				inlineJSOffsetSourceLocationsLineNumbers.add(lineNumber);
				inlineJSOffsetSourceLocationsColumnNumbers.add(match.length() + line.indexOf(match));
			}

			line = reader.readLine();
			lineNumber++;
		}	
		reader.close();
	}

	
}
class IllegalSourceLocationException extends Exception {
	public IllegalSourceLocationException() {}
    public IllegalSourceLocationException(String message)
    {
       super(message);
    }
}