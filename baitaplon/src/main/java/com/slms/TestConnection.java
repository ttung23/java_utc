package com.slms;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestConnection {
    public static void main(String[] args) {
        String[] urls = {
            "jdbc:oracle:thin:@localhost:1521/FREE",
            "jdbc:oracle:thin:@localhost:1521:FREE",
            "jdbc:oracle:thin:@localhost:1521/FREEPDB1"
        };

        System.out.println("==================================================");
        System.out.println("KIEM TRA KET NOI ORACLE 23ai CHI TIET:");
        System.out.println("==================================================");

        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (Exception e) {
            System.out.println("Loi nap Driver: " + e.getMessage());
            return;
        }

        for (String url : urls) {
            System.out.println("\n--> Thu URL: " + url + " voi slms_user / slms_password");
            try (Connection conn = DriverManager.getConnection(url, "slms_user", "slms_password")) {
                System.out.println("    [THANH CONG!] Da ket noi duoc toi URL: " + url);
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM BOOKS")) {
                    if (rs.next()) {
                        System.out.println("    [DATA OK!] Bang BOOKS co so ban ghi: " + rs.getInt(1));
                    }
                } catch (Exception e) {
                    System.out.println("    [CANH BAO] Ket noi duoc nhung chua co bang BOOKS: " + e.getMessage());
                }

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT category_id, category_name FROM CATEGORIES ORDER BY category_id")) {
                    System.out.println("    [CATEGORIES IN DATABASE]:");
                    while (rs.next()) {
                        System.out.println("      - " + rs.getString(1) + ": " + rs.getString(2));
                    }
                } catch (Exception e) {
                    System.out.println("    [CANH BAO] Loi doc bang CATEGORIES: " + e.getMessage());
                }

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT staff_id, username, full_name, role, avatar FROM STAFF_USERS")) {
                    System.out.println("    [STAFF USERS WITH AVATAR]:");
                    while (rs.next()) {
                        System.out.println("      - " + rs.getString("staff_id") + " | " + rs.getString("username") + " | " + rs.getString("full_name") + " | " + rs.getString("role") + " | avatar=" + rs.getString("avatar"));
                    }
                } catch (Exception e) {
                    System.out.println("    [CANH BAO] Loi doc STAFF_USERS: " + e.getMessage());
                }

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT shelf_id, shelf_name FROM SHELVES ORDER BY shelf_id")) {
                    System.out.println("    [SHELVES IN DATABASE]:");
                    while (rs.next()) {
                        System.out.println("      - " + rs.getString(1) + ": " + rs.getString(2));
                    }
                } catch (Exception e) {
                    System.out.println("    [CANH BAO] Loi doc bang SHELVES: " + e.getMessage());
                }

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT isbn, title, category_id, shelf_id, location FROM BOOKS FETCH FIRST 5 ROWS ONLY")) {
                    System.out.println("    [BOOKS SAMPLE WITH SHELF_ID]:");
                    while (rs.next()) {
                        System.out.println("      - " + rs.getString("isbn") + " | " + rs.getString("title") + " | shelf_id=" + rs.getString("shelf_id") + " | location=" + rs.getString("location"));
                    }
                } catch (Exception e) {
                    System.out.println("    [CANH BAO] Loi doc BOOKS: " + e.getMessage());
                }
                return;
            } catch (Exception e) {
                System.out.println("    [THAT BAI] Ma loi: " + e.getMessage());
            }
        }
    }
}

