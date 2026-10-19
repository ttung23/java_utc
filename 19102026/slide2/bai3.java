import java.util.Scanner;

public class bai3 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Nhap chuoi so nguyen: ");
        String chuoi = sc.nextLine().trim();

        String[] parts = chuoi.split("\\s+");

        int soPhanTu = parts.length;
        int tong = 0;

        for (String part : parts) {
            tong += Integer.parseInt(part);
        }

        System.out.println("So phan tu: " + soPhanTu + "; Tong: " + tong);

        sc.close();
    }
}
