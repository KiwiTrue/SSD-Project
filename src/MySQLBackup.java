import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class MySQLBackup {
    // Add these constants for paths
    private static final String MYSQL_PATH = "C:/xampp/mysql/bin/mysqldump.exe";
    private static final String SEVEN_ZIP_PATH = "C:/Program Files/7-Zip/7z.exe";
    
    public static void performBackup(String mysqlUser, String mysqlPassword, String zipPassword, String backupPath) {
        try {
            // Verify mysqldump exists
            File mysqldumpFile = new File(MYSQL_PATH);
            if (!mysqldumpFile.exists()) {
                throw new IOException("mysqldump not found at: " + MYSQL_PATH);
            }

            // Verify 7zip exists
            File sevenZipFile = new File(SEVEN_ZIP_PATH);
            if (!sevenZipFile.exists()) {
                throw new IOException("7-Zip not found at: " + SEVEN_ZIP_PATH);
            }

            // Create backup directory if it doesn't exist
            File backupDir = new File(backupPath);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            // Create backup file names with timestamps
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = dateFormat.format(new Date());
            String backupName = String.format("project_backup_%s.sql", timestamp);
            String zipName = String.format("project_backup_%s.zip", timestamp);
            
            String fullBackupPath = backupPath + File.separator + backupName;
            String fullZipPath = backupPath + File.separator + zipName;

            // Create mysqldump process with full path
            ProcessBuilder dumpBuilder = new ProcessBuilder(
                MYSQL_PATH,
                "--user=" + mysqlUser,
                "--password=" + mysqlPassword,
                "--databases",
                "project",
                "--result-file=" + fullBackupPath
            );

            // Execute mysqldump
            Process dumpProcess = dumpBuilder.start();
            int dumpResult = dumpProcess.waitFor();
            
            if (dumpResult == 0) {
                // Create 7zip process with full path
                ProcessBuilder zipBuilder = new ProcessBuilder(
                    SEVEN_ZIP_PATH,
                    "a",
                    "-tzip",
                    "-p" + zipPassword,
                    fullZipPath,
                    fullBackupPath
                );

                // Execute 7zip
                Process zipProcess = zipBuilder.start();
                int zipResult = zipProcess.waitFor();
                
                if (zipResult == 0) {
                    // Delete the original SQL file after successful zip
                    new File(fullBackupPath).delete();
                    System.out.println("Backup completed successfully: " + fullZipPath);
                } else {
                    System.err.println("Error creating zip file");
                }
            } else {
                System.err.println("Error creating database dump");
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Backup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}