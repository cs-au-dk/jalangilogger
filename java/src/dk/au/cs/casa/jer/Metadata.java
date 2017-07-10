package dk.au.cs.casa.jer;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class Metadata {

    JSONRep jsonRep;

    public Metadata(String jsonMetadata) {
        Gson gson = new Gson();
        jsonRep = gson.fromJson(jsonMetadata, JSONRep.class);
    }

    public Metadata() {
        jsonRep = new JSONRep();
    }

    public String getSha() {
        return jsonRep.sha;
    }

    public void setSha(String sha) {
        jsonRep.sha = sha;
    }

    public long getTime() {
        return jsonRep.time;
    }

    public void setTime(long time) {
        jsonRep.time = time;
    }

    public String getRoot() {
        return jsonRep.root;
    }

    public void setRoot(String root) {
        jsonRep.root = root;
    }

    public String getResult() {
        return jsonRep.result;
    }

    public void setResult(String result) {
        jsonRep.result = result;
    }

    public String getEnvironment() {
        return jsonRep.environment;
    }

    public void setEnvironment(String environment) {
        jsonRep.environment = environment;
    }

    public String getEnvironmentVersion() {
        return jsonRep.environmentVersion;
    }

    public void setEnvironmentVersion(String environmentVersion) {
        jsonRep.environmentVersion = environmentVersion;
    }

    public String getLogVersion() {
        return jsonRep.logVersion;
    }

    public void setLogVersion(String logVersion) {
        jsonRep.logVersion = logVersion;
    }

    public int getTimeLimit() {
        return jsonRep.timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        jsonRep.timeLimit = timeLimit;
    }

    private class JSONRep {

        @SerializedName("sha")
        public String sha;

        @SerializedName("time")
        public long time;

        @SerializedName("root")
        public String root;

        @SerializedName("result")
        public String result;

        @SerializedName("environment")
        public String environment;

        @SerializedName("environmentVersion")
        public String environmentVersion;

        @SerializedName("logVersion")
        public String logVersion;

        @SerializedName("timeLimit")
        public int timeLimit;
    }
}
