package org.fao.sola.admin.web.beans.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.DatabaseMetaData;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.context.ExternalContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.ibatis.session.Configuration;
import org.fao.sola.admin.web.beans.AbstractBackingBean;
import org.fao.sola.admin.web.beans.helpers.DateBean;
import org.fao.sola.admin.web.beans.helpers.ErrorKeys;
import org.fao.sola.admin.web.beans.helpers.FileSorter;
import org.fao.sola.admin.web.beans.helpers.MessageBean;
import org.fao.sola.admin.web.beans.helpers.MessageProvider;
import org.fao.sola.admin.web.beans.language.LanguageBean;
import org.sola.common.ConfigConstants;
import org.sola.common.DateUtility;
import org.sola.common.FileUtility;
import org.sola.common.StringUtility;
import org.sola.services.common.logging.LogUtility;
import org.sola.services.ejb.system.businesslogic.SystemEJBLocal;
import org.sola.services.ejbs.admin.businesslogic.AdminEJBLocal;

/**
 * Contains properties and methods to manage database backups
 */
@Named
@ViewScoped
public class DbBackupPageBean extends AbstractBackingBean {

    @EJB
    SystemEJBLocal systemEjb;

    @EJB
    AdminEJBLocal adminEjb;

    @Inject
    MessageProvider msgProvider;

    @Inject
    LanguageBean langBean;

    @Inject
    DateBean dateBean;

    @Inject
    MessageBean msgBean;

    private String dbHost;
    private String dbHostPort;
    private String dbUser;
    private String dbUtilitiesFolder;
    private String dbName;
    private final String dbBackupFolder = "sola_db_backups";
    private String fullPath;
    private String log;
    private String fileToRestore;
    private String userPassword;
    
    public String getDbHost() {
        return dbHost;
    }

    public String getDbHostPort() {
        return dbHostPort;
    }

    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getDbName() {
        return dbName;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public String getFileToRestore() {
        return fileToRestore;
    }

    public void setFileToRestore(String fileToRestore) {
        this.fileToRestore = fileToRestore;
    }

    public void init() {
        Configuration conf = adminEjb.getDbConfiguration();
        DatabaseMetaData meta;
        String url;
        try {
            meta = conf.getEnvironment().getDataSource().getConnection().getMetaData();
            url = meta.getURL();

            // Extract DB connection properties
            int hostIndex = url.indexOf("://") + 3;
            int portIndex = url.indexOf(":", hostIndex) + 1;
            int dbNameIndex = url.indexOf("/", portIndex) + 1;
            dbHost = url.substring(hostIndex, portIndex - 1);
            dbHostPort = url.substring(portIndex, dbNameIndex - 1);
            dbName = url.substring(dbNameIndex, url.indexOf("?", dbNameIndex));
            dbUser = meta.getUserName();
            dbUtilitiesFolder = systemEjb.getSetting(ConfigConstants.DB_UTILITIES_FOLDER, "");
        } catch (Exception ex) {
            LogUtility.log("Failed to get DB settings", ex);
            msgBean.setErrorMessage(msgProvider.getErrorMessage(ErrorKeys.DB_BACKUP_PAGE_FAILED_TO_GET_DB_SETTINGS));
        }
    }

    public File[] getBackups() {
        String path = getBackupFolder();
        if (path.equals("")) {
            return null;
        }

        File f = new File(path);
        File[] files = null;

        if (f.isDirectory()) {
            files = f.listFiles();
            Arrays.sort(files, new FileSorter());
        }
        return files;
    }

    private String getBackupFolder() {
        if (!StringUtility.isEmpty(fullPath)) {
            return fullPath;
        }

        if (StringUtility.isEmpty(dbHost) || StringUtility.isEmpty(dbName) || StringUtility.isEmpty(dbHostPort)) {
            return "";
        }

        // form the path to backup folder
        fullPath = System.getProperty("user.home") + System.getProperty("file.separator") + dbBackupFolder
                + System.getProperty("file.separator") + dbHost + dbHostPort + "_" + dbName;
        File f = new File(fullPath);
        if (!f.exists()) {
            f.mkdirs();
        }
        return fullPath;
    }

    public boolean isDbSettingsConfigured() {
        return !StringUtility.isEmpty(dbHost) && !StringUtility.isEmpty(dbHostPort) && !StringUtility.isEmpty(dbName);
    }

    public boolean isUtilityFolderConfigured() {
        return !StringUtility.isEmpty(dbUtilitiesFolder);
    }

    public String getFileSize(long size) {
        return FileUtility.formatFileSize(size);
    }

    public String getDate(long time) {
        return dateBean.formatDate(new Date(time));
    }

    public void backUp() throws Exception {
        String errors = "";
        log = "";

        if (StringUtility.isEmpty(dbUser)) {
            errors = msgProvider.getErrorMessage(ErrorKeys.DB_BACKUP_PAGE_FILL_USERNAME);
        }
        
        if (StringUtility.isEmpty(userPassword)) {
            errors = msgProvider.getErrorMessage(ErrorKeys.DB_BACKUP_PAGE_FILL_PASSWORD);
        }

        if (!StringUtility.isEmpty(errors)) {
            throw new Exception(errors);
        }

        try {
            // Generate backup file name
            Date date = Calendar.getInstance().getTime();
            String fileName = getBackupFolder() + System.getProperty("file.separator")
                    + dbName + "_" + DateUtility.formatDate(date, "yyyy-MM-dd_HHmmss") + ".bakup";

            // Do backup
            Process p;
            ProcessBuilder pb;
            
            pb = new ProcessBuilder(
                    (dbUtilitiesFolder + System.getProperty("file.separator") + "pg_dump").replace("\\", "\\\\"),
                    "--host", dbHost,
                    "--port", dbHostPort,
                    "--username", dbUser,
                    "--no-password",
                    "--format", "c",
                    "--blobs",
                    "--verbose",
                    "--file", fileName, dbName);
            
            Map<String, String> env = pb.environment();
            env.put("PGPASSWORD", userPassword);
            
            pb.redirectErrorStream(true);
            p = pb.start();
            InputStream is = p.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            String lineSeparator = System.getProperty("line.separator");

            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append(lineSeparator);
            }
            log = sb.toString();
        } catch (Exception e) {
            log = e.getMessage();
        }
    }

    public void download(String fileName) throws IOException {
        if (StringUtility.isEmpty(fileName)) {
            return;
        }

        ExternalContext ec = getExtContext();

        File file = new File(getBackupFolder() + System.getProperty("file.separator") + fileName);

        if (!file.exists()) {
            return;
        }

        ec.responseReset();
        ec.setResponseContentType("application/octet-stream");
        ec.setResponseContentLength((int)file.length());
        ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        InputStream fis = new FileInputStream(file);
        OutputStream output = ec.getResponseOutputStream();
        
        byte[] buf = new byte[1024];
        int bytesRead;
        while ((bytesRead = fis.read(buf)) > 0) {
            output.write(buf, 0, bytesRead);
        }
        fis.close();

        output.write(buf);
        output.flush();
        output.close();

        getContext().responseComplete();
    }

    public void deleteFile(File f) {
        try {
            f.delete();
        } catch (Exception e) {
            msgBean.setErrorMessage(String.format(msgProvider.getErrorMessage(ErrorKeys.DB_BACKUP_PAGE_FAILED_DELETE_FILE), e.getMessage()));
        }
    }

    public void restore() throws Exception {
        String errors = "";
        log = "";

        if (StringUtility.isEmpty(dbUser)) {
            errors = msgProvider.getErrorMessage(ErrorKeys.DB_BACKUP_PAGE_FILL_USERNAME);
        }

        if (StringUtility.isEmpty(userPassword)) {
            errors = msgProvider.getErrorMessage(ErrorKeys.DB_BACKUP_PAGE_FILL_PASSWORD);
        }
        
        if (StringUtility.isEmpty(fileToRestore)) {
            errors = msgProvider.getErrorMessage(ErrorKeys.DB_BACKUP_PAGE_SELECT_FILE_TO_RESTORE);
        }

        if (!StringUtility.isEmpty(errors)) {
            throw new Exception(errors);
        }

        try {
            // Do restore
            Process p;
            ProcessBuilder pb;
            pb = new ProcessBuilder(
                    (dbUtilitiesFolder + System.getProperty("file.separator") + "pg_restore").replace("\\", "\\\\"),
                    "--host", dbHost,
                    "--port", dbHostPort,
                    "--username", dbUser,
                    "--dbname", dbName,
                    "--clean",
                    "--no-password",
                    "--verbose",
                    getBackupFolder() + System.getProperty("file.separator") + fileToRestore);
            
            Map<String, String> env = pb.environment();
            env.put("PGPASSWORD", userPassword);
            
            pb.redirectErrorStream(true);
            p = pb.start();
            InputStream is = p.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            String lineSeparator = System.getProperty("line.separator");

            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append(lineSeparator);
            }
            log = sb.toString();
        } catch (Exception e) {
            log = e.getMessage();
        }
    }
}
