import java.sql.*;

public final class DBUtils {

    // Database connection details
    private static String url = "jdbc:mysql://localhost:3306/project";
    private static String appUsername = "JavaApp";
    private static String appPassword = "JavaAppPass!123";

    // Method to establish a connection to the database
    public static Connection establishConnection(){
        Connection con = null;
        try{            
            con = DriverManager.getConnection(url, appUsername, appPassword);
            System.out.println("Connection Successful");
        }catch(SQLException e){
            System.out.println(e.getMessage());
        }
        return con;
    }

    // Method to close the connection to the database
    public static void closeConnection(Connection con,Statement stmt){
        try{
            stmt.close();
            con.close();
            System.out.println("Connection is closed");        
        }catch(SQLException e){
            e.getMessage();
        }
    }
}