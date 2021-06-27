package xyz.gojani.uploadme.models;

import org.apache.commons.lang3.RandomStringUtils;
import org.javawebstack.orm.Model;
import org.javawebstack.orm.annotation.Column;
import org.javawebstack.orm.annotation.Dates;

import java.sql.Timestamp;

@Dates
public class File extends Model {
    @Column(size = 10)
    public String id;

    @Column
    public String name;

    @Column
    public String url;

    @Column
    public String mimeType = "image/png";

    @Column(size = 50)
    public String editKey;

    @Column
    public Timestamp createdAt;
    @Column
    public Timestamp updatedAt;

    public File(){
        id = RandomStringUtils.randomAlphanumeric(10);
        editKey = RandomStringUtils.randomAlphanumeric(50);
    }

}
