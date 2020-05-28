
package com.inha.makko.model;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class KakaoCoord2AddressResponse {

    @SerializedName("meta")
    @Expose
    private Meta meta;
    @SerializedName("documents")
    @Expose
    private List<Document> documents = null;

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }

}
