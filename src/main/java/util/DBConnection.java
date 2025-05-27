package main.java.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mariadb://localhost:3306/cbt_db";
    private static final String USER = "root";        // MariaDB 사용자명
    private static final String PASS = "hj0811";    // 사용자 비밀번호

    private static Connection conn;

    private DBConnection() {
        // private 생성자 (싱글톤)
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
