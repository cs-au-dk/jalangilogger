import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TransformHtmlLogFiles {
	private static final String startInstrumentedDirectory = "instrumentedHTMLFiles/";
	private static final String unchangedLogFileDirectory = "nodeJSServer/unchangedLogFiles/";
	private static final String changedLogFileDirectory = "JalangiLogFiles/";
	private static final String originalTestFileDirectory = "test/";
	
	private static ArrayList<Integer> inlineJSOffsetSourceLocationsLineNumbers = new ArrayList<Integer>();
	private static ArrayList<Integer> inlineJSOffsetSourceLocationsColumnNumbers = new ArrayList<Integer>();
		
	public static void main(String[] args) throws Exception {
		loopThroughAllFilesAndTransformThem("");
	}

	private static void loopThroughAllFilesAndTransformThem(String path) throws Exception {
		File[] faFiles = new File(unchangedLogFileDirectory + path).listFiles();
		for(File file: faFiles){
			if(file.isFile() && file.getName().endsWith(".log")){
				String logFileName = path + file.getName();
				String htmlFileName = file.getName().substring(0, file.getName().length() - 3) + "html";
				String originalHTMLFile = (originalTestFileDirectory + path + htmlFileName);
				String instrumentedDirectory = startInstrumentedDirectory + path + htmlFileName + "/";
				//Jalangi creates a folder of the same name as the file under instrumentation.
				removeCommasAndEmptyLinesAndDuplicates(logFileName);
				fillInlineJSOffsetSourceLocationLineNumbers(originalHTMLFile);
				transformFile(logFileName, originalHTMLFile, instrumentedDirectory);
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
		FileWriter writer = new FileWriter(newFileName);
		String line = reader.readLine();
		while(line != null){
			JSONObject readedJsonObj = new JSONObject(line);
			try {
				JSONObject JsonObjToBeWritten = iterateThroughJSONObjectAndChangeSL(readedJsonObj, originalHTMLFile, instrumentedDirectory);
				writer.write(JsonObjToBeWritten.toString() + "\r\n");
			} catch (IllegalSourceLocationException exception){} //If illegal sourceLocation (i.e. iid in Jalangi) just ignore this line
			line = reader.readLine();

		}
		reader.close();
		writer.close();
	}
	//Takes an JSONObject and iterates through it, to find all source locations and then change these to
	//fit with the sourcelocations from TAJS, and then returns the updated JSONobject, which should be written to file
	private static JSONObject iterateThroughJSONObjectAndChangeSL(JSONObject obj, String originalHTMLFile, String instrumentedDirectory) throws Exception{
		Iterator<String> keys = obj.keys();
		
		while(keys.hasNext()){
			String key = keys.next();
			Object next = obj.get(key);
			
			if(next instanceof String){
				//Do nothing.
			} else if(next instanceof JSONObject){
				JSONObject nextJSON = (JSONObject) next;
				if(key.equals("sourceLocation") || key.equals("allocationSite")){
					obj.put(key, transformSourceLocation(nextJSON, originalHTMLFile, instrumentedDirectory));
				} else {
					obj.put(key, iterateThroughJSONObjectAndChangeSL(nextJSON, originalHTMLFile, instrumentedDirectory));
				}
			} else if(next instanceof JSONArray){
				JSONArray nextJSONArray = (JSONArray) next;
				for(int i = 0; i < nextJSONArray.length(); i++){
					nextJSONArray.put(i, iterateThroughJSONObjectAndChangeSL(nextJSONArray.getJSONObject(i), originalHTMLFile, instrumentedDirectory));
				}
			} else {	
				throw new Exception("This should not happen - Unhandled class: " + next.getClass());		
			}
		}
		return obj;
	}

	//This converts jalangis source locations into TAJS source locations. Jalangi handles event-handler and
	//inline js source locations different than TAJS does.
	private static JSONObject transformSourceLocation(JSONObject obj, String originalHTMLFile, String instrumentedDirectory) throws Exception {
		String fileName = obj.getString("fileName");
		Integer lineNumber = obj.getInt("lineNumber");
		Integer columnNumber = obj.getInt("columnNumber");
		if(lineNumber == -1 && columnNumber == -1){ //in case Jalangi has sourcelocation iid.
			throw new IllegalSourceLocationException();
		}
		if(fileName.contains("inline-") && fileName.contains("_orig_.js")){
			int inlineNumber = findInlineNumber("inline-(.*)_orig_.js", fileName);
			int newLineNumber = lineNumber + inlineJSOffsetSourceLocationsLineNumbers.get(inlineNumber);
			int newColumnNumber = columnNumber + (lineNumber == 1 ? inlineJSOffsetSourceLocationsColumnNumbers.get(inlineNumber) : 0);
			obj.put("fileName", originalHTMLFile);
			obj.put("lineNumber", newLineNumber);
			obj.put("columnNumber", newColumnNumber);
		} else if(fileName.contains("event-handler-") && fileName.contains("_orig_.js")){
			int inlineNumber = findInlineNumber("event-handler-(.*)_orig_.js", fileName);
			String eventHandlerFile = instrumentedDirectory + "event-handler-" + inlineNumber + "_orig_.js";
			obj = updateSourceLocationFromEventFileToSLInOriginalFile(eventHandlerFile, originalHTMLFile, obj);	
		}
		return obj;
	}
	
	private static JSONObject updateSourceLocationFromEventFileToSLInOriginalFile(String eventHandlerFile, String originalHTMLFile, JSONObject obj) throws Exception {
		String lineToSearchForInOriginalFile = getLineToSearchForFromEventHandlerFile(eventHandlerFile, obj);
		BufferedReader reader = new BufferedReader(new FileReader(originalHTMLFile));
		String line = reader.readLine();
		int lineNumber = 1;
		boolean stringFound = false;
		boolean inScript = false;
		while(line != null){
			if(line.contains("<script>") || line.contains("<script type=\"text/javascript\">") || line.contains("<script type='text/javascript'>") || line.contains("<script language=\"javascript\">")){
				inScript = true;
			}
			if(line.contains("</script>")){
				inScript = false;
			}
			if(!inScript && line.contains(lineToSearchForInOriginalFile)){
				obj.put("fileName", originalHTMLFile);
				obj.put("lineNumber", lineNumber);
				obj.put("columnNumber", line.indexOf(lineToSearchForInOriginalFile) + obj.getInt("columnNumber"));
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


	private static String getLineToSearchForFromEventHandlerFile(String eventHandlerFile, JSONObject obj)
			throws JSONException, FileNotFoundException, IOException {
		int lineNumber = obj.getInt("lineNumber");
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
			if(line.contains("<script>")){
				inlineJSOffsetSourceLocationsLineNumbers.add(lineNumber);
				inlineJSOffsetSourceLocationsColumnNumbers.add("<script>".length() + line.indexOf("<script>"));
			}
			if(line.contains("<script type='text/javascript'>")){
				inlineJSOffsetSourceLocationsLineNumbers.add(lineNumber);
				inlineJSOffsetSourceLocationsColumnNumbers.add("<script type='text/javascript'>".length() + line.indexOf("<script type='text/javascript'>"));
			}
			if(line.contains("<script language=\"javascript\">")){
				inlineJSOffsetSourceLocationsLineNumbers.add(lineNumber);
				inlineJSOffsetSourceLocationsColumnNumbers.add("<script language=\"javascript\">".length() + line.indexOf("<script language=\"javascript\">"));
			}
			if(line.contains("<script type=\"text/javascript\">")){
				System.out.println(lineNumber);
				inlineJSOffsetSourceLocationsLineNumbers.add(lineNumber);
				inlineJSOffsetSourceLocationsColumnNumbers.add("<script type=\"text/javascript\">".length() + line.indexOf("<script type=\"text/javascript\">"));
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