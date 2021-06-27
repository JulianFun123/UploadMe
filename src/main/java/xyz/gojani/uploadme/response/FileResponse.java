package xyz.gojani.uploadme.response;

import java.sql.Timestamp;

public class FileResponse extends ActionResponse {
    public String id;
    public String name;
    public String url;
    public String mimeType = "image/png";
    public Timestamp createdAt;
    public Timestamp updatedAt;
}
