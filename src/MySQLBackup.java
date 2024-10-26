import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class MySQLBackup {
    
    // Method to perform a backup of the MySQL database
    public static void performBackup(String mysqlUser, String mysqlPassword, String zipPassword, String backupPath) {
        try {
            
            // Create a backup file name with the current date and create a zip file name
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String backupName = "project_backup_" + dateFormat.format(new Date()) + ".sql";
            String zipName = "project_backup_" + dateFormat.format(new Date()) + ".zip";

            // Create a process to run the mysqldump command to backup the database
            ProcessBuilder dumpProcessBuilder = new ProcessBuilder(
                    "C:/xampp/mysql/bin/mysqldump", "--user=" + mysqlUser, "--password=" + mysqlPassword, "project"
            );
            dumpProcessBuilder.redirectOutput(new File(backupPath + "/" + backupName));
            Process dumpProcess = dumpProcessBuilder.start();
            int dumpExitCode = dumpProcess.waitFor();
            if (dumpExitCode != 0) {
                System.out.println("Backup failed: error during dump creation");
                return;
            } else {
                System.out.println("Backup successful");
            }

            // Create a process to run the 7-Zip command to zip the backup file
            ProcessBuilder zipProcessBuilder = new ProcessBuilder("C:/Program Files/7-Zip/7z.exe", "a", "-tzip", "-p" + zipPassword, backupPath + "/" + zipName, backupPath + "/" + backupName);
            Process zipProcess = zipProcessBuilder.start();
            int zipExitCode = zipProcess.waitFor();
            if (zipExitCode != 0) {
                System.out.println("Backup failed: error during zip creation");
                return;
            } else {
                System.out.println("Zip successful");
            }

            // Delete the original backup file
            File originalBackupFile = new File(backupPath + "/" + backupName);
            if (!originalBackupFile.delete()) {
                System.out.println("Backup failed: error during original file deletion");
                return;
            } else {
                System.out.println("Original file deleted");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}