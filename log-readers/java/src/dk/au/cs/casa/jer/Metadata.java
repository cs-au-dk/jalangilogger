package dk.au.cs.casa.jer;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class Metadata {
    JSONRep jsonRep;
    public Metadata(String jsonMetadata) {
        Gson gson = new Gson();
        jsonRep = gson.fromJson(jsonMetadata, JSONRep.class);
    }

    public String getSha() {
        return jsonRep.sha;
    }

    public long getTime() {
        return jsonRep.time;
    }

    private class JSONRep {
        @SerializedName("sha")
        public String sha;

        @SerializedName("time")
        public long time;
    }
}
