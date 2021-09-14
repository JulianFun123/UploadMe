package xyz.gojani.uploadme.controller;

import org.apache.tika.Tika;
import org.eclipse.jetty.server.Request;
import org.javawebstack.framework.HttpController;
import org.javawebstack.httpserver.Exchange;
import org.javawebstack.httpserver.helper.MimeType;
import org.javawebstack.httpserver.router.annotation.PathPrefix;
import org.javawebstack.httpserver.router.annotation.params.Path;
import org.javawebstack.httpserver.router.annotation.verbs.Delete;
import org.javawebstack.httpserver.router.annotation.verbs.Get;
import org.javawebstack.httpserver.router.annotation.verbs.Post;
import org.javawebstack.orm.Repo;
import xyz.gojani.uploadme.UploadMe;
import xyz.gojani.uploadme.models.File;
import xyz.gojani.uploadme.response.ActionResponse;
import xyz.gojani.uploadme.response.FileResponse;
import xyz.gojani.uploadme.response.FileUploadResponse;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

public class FileController extends HttpController {

    public final String[] MIME_TYPES = new String[]{"image/svg+xml", "text/javascript", "text/html", "text/csv",
            "text/css", "application/json",
            "video/x-flv", "video/mp4", "application/x-mpegURL", "video/MP2T", "video/3gpp", "video/quicktime", "video/x-msvideo", "video/x-ms-wmv",
    };

    @Post("/")
    @Post("/api/v1/file")
    public FileUploadResponse upload(Exchange exchange) throws ServletException, IOException {
        FileUploadResponse fileUploadResponse = new FileUploadResponse();

        exchange.enableMultipart(System.getProperty("java.io.tmpdir"), UploadMe.INSTANCE.getConfig().getInt("file.size", -1), 4048576);
        Part f = exchange.rawRequest().getPart("f");

        File file = new File();
        file.name = f.getSubmittedFileName();
        if (file.name == null)
            file.name = "file";

        InputStream inputStream = f.getInputStream();
        String givenContentType = f.getContentType();
        Tika tika = new Tika();
        file.mimeType = tika.detect(inputStream);

        if (givenContentType != null) {
            for (String mimeType : MIME_TYPES) {
                if (mimeType.equals(givenContentType))
                    file.mimeType = givenContentType;
            }
        }
        file.name = file.name.replaceAll("[^A-Za-z0-9]", "");

        try {
            file.name += "."+MimeType.byMimeType(file.mimeType).getExtensions().get(0).replaceAll("\\.\\.", ".");
        } catch (NullPointerException ignored) {}

        String filePathName = file.id+"_"+file.name;

        file.url = UploadMe.INSTANCE.uploadFile(inputStream, filePathName);
        file.save();
        fileUploadResponse.success = true;
        fileUploadResponse.id  = file.id;
        fileUploadResponse.key = file.editKey;
        return fileUploadResponse;
    }


    @Get("/api/v1/file/{id}")
    public FileResponse getFile(Exchange exchange/*, @Path("id") String id*/){
        FileResponse fileResponse = new FileResponse();
        String id = exchange.path("id");
        File file = Repo.get(File.class).where("id",id).first();
        if (file != null) {
            fileResponse.success = true;
            fileResponse.name = file.name;
            fileResponse.id = file.id;
            fileResponse.url = file.url;
            fileResponse.mimeType = file.mimeType;
            fileResponse.createdAt = file.createdAt;
            fileResponse.updatedAt = file.updatedAt;
        }

        return fileResponse;
    }

    @Delete("/api/v1/file/{id}")
    public ActionResponse deleteFile(Exchange exchange, @Path("id") String id){
        ActionResponse response = new FileResponse();
        File file = Repo.get(File.class).where("id",id).first();

        if (file != null && file.editKey.equals(exchange.path("key"))) {
            // file.url = UploadMe.INSTANCE.deleteFile(inputStream, filePathName);
            file.delete();
            response.success = true;
        }

        return response;
    }
}
