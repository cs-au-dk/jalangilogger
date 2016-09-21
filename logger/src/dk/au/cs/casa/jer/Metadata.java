package dk.au.cs.casa.jer;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class Metadata {
    JSONRep jsonRep;
    public Metadata(String jsonMetadata) {
        Gson gson = new Gson();
        jsonRep = gson.fromJson(jsonMetadata, JSONRep.class);
    }

    public Metadata () {
         jsonRep = new JSONRep();
    }

    public void setSha(String sha) {
        jsonRep.sha = sha;
    }

    public void setTime(long time) {
        jsonRep.time = time;
    }

    public void setRoot(String root) {
        jsonRep.root = root;
    }

    public void setResult(String result) {
        jsonRep.result = result;
    }

    public String getSha() {
        return jsonRep.sha;
    }

    public long getTime() {
        return jsonRep.time;
    }

    public String getRoot() {
        return jsonRep.root;
    }

    public String getResult() {
        return jsonRep.result;
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
    }
}
