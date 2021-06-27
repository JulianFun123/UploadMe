package xyz.gojani.uploadme;

import org.javawebstack.framework.HttpController;
import org.javawebstack.framework.WebApplication;
import org.javawebstack.framework.config.Config;
import org.javawebstack.httpserver.Exchange;
import org.javawebstack.httpserver.HTTPServer;
import org.javawebstack.httpserver.handler.AfterRequestHandler;
import org.javawebstack.httpserver.handler.StaticFileHandler;
import org.javawebstack.httpserver.util.ResourceFileProvider;
import org.javawebstack.orm.ORM;
import org.javawebstack.orm.ORMConfig;
import org.javawebstack.orm.exception.ORMConfigurationException;
import org.javawebstack.orm.wrapper.MySQL;
import org.javawebstack.orm.wrapper.SQL;
import xyz.gojani.uploadme.controller.FileController;

import java.io.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class UploadMe extends WebApplication {
    public static UploadMe INSTANCE;
    protected void setupConfig(Config config) {
        Map<String, String> envMapping = new HashMap<>();
        envMapping.put("SERVER_NAME", "server.name");
        envMapping.put("FTP_URL", "ftp.url");
        envMapping.put("FTP_HOST", "ftp.host");
        envMapping.put("FTP_USER", "ftp.user");
        envMapping.put("FTP_PASSWORD", "ftp.password");
        envMapping.put("FTP_BASE_DIR", "ftp.basedir");
        envMapping.put("UPLOAD_MAX_SIZE", "file.size");
        envMapping.put("UPLOAD_TYPE", "upload.type");
        envMapping.put("UPLOAD_PATH", "upload.path");
        envMapping.put("UPLOAD_FRONTEND", "upload.frontend");
        envMapping.put("UPLOAD_NEW_PATH", "upload.new_path");
        envMapping.put("UPLOAD_NEW_DOMAIN", "upload.new_domain");

        envMapping.put("DATABASE_PREFIX", "database.prefix");
        config.addEnvKeyMapping(envMapping);
        config.addEnvFile(new File(".env"));
    }

    protected void setupModels(SQL sql) throws ORMConfigurationException {
        ORMConfig ormConfig = new ORMConfig().setTablePrefix(getConfig().get("database.prefix", "uploadme")+"_");

        ORM.register(xyz.gojani.uploadme.models.File.class.getPackage(), sql, ormConfig);
        ORM.autoMigrate();
    }

    protected void setupServer(HTTPServer httpServer) {
        httpServer.controller(HttpController.class, FileController.class.getPackage());
        ResourceFileProvider resourceFileProvider = new ResourceFileProvider(getClass().getClassLoader(), "static");
        StaticFileHandler staticFileHandler = new StaticFileHandler().add(resourceFileProvider);

        httpServer.get("/", e->{
            if (!Boolean.parseBoolean(getConfig().get("upload.frontend", "true")))
                return null;
            try {
                e.write(resourceFileProvider.getFile("index.html"));
            } catch (IOException ioException) { ioException.printStackTrace(); }
            return "";
        });

        httpServer.get("/{x}", e->{
            try {
                e.write(resourceFileProvider.getFile("file.html"));
            } catch (IOException ioException) { ioException.printStackTrace(); }
            return "";
        });

        httpServer.staticHandler("/", staticFileHandler);
        if (getConfig().get("upload.type", "local").equals("local")) {
            String dir = getConfig().get("upload.path", "uploads");
            new File(dir).mkdirs();
            if (new File(dir).exists()) {
                httpServer.staticDirectory("/uploads/", dir);
                httpServer.afterGet("/uploads/{path}", (exchange, o) -> {
                    if (exchange.rawResponse().getContentType().contains("html"))
                        exchange.rawResponse().setContentType("plain/text");
                    return o;
                });
            }
        }
    }

    public String uploadFile(InputStream inputStream, String pathName){
        String url = pathName;
        if (getConfig().get("upload.type", "local").equals("local")) {
            try {
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                FileOutputStream fileOutputStream = new FileOutputStream(getConfig().get("upload.path", "uploads")+"/"+pathName);
                url = getConfig().get("upload.new_domain", getConfig().get("server.name", "http://localhost"))+"/"+getConfig().get("upload.new_path", getConfig().get("upload.path", "uploads"))+"/"+pathName;
                fileOutputStream.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return url;
    }

    public static void main(String[] args) {
        INSTANCE = new UploadMe();
        INSTANCE.start();
    }
}
