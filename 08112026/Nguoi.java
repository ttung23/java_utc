package buoi3.nhanvien;

import java.util.Scanner;

public class Nguoi {
    private String ten;
    private int namSinh;

    public Nguoi() {
        this.ten = "";
        this.namSinh = 0;
    }

    public Nguoi(String ten, int namSinh) {
        this.ten = ten;
        this.namSinh = namSinh;
    }

    public String getHoTen() {
        return ten;
    }

    public void setHoTen(String ten) {
        this.ten = ten;
    }

    public int getTuoi() {
        return namSinh;
    }

    public void setTuoi(int namSinh) {
        this.namSinh = namSinh;
    }

    public void nhap(Scanner scanner) {
        System.out.print("Nhap ho ten: ");
        ten = scanner.nextLine();
        System.out.print("Nhap tuoi: ");
        while (true) {
            try {
                int val = Integer.parseInt(scanner.nextLine().trim());
                if (val >= 0) {
                    this.namSinh = val;
                    break;
                }
                System.out.print("Tuoi phai >= 0. Nhap lai: ");
            } catch (NumberFormatException ex) {
                System.out.print("Tuoi khong hop le. Nhap lai: ");
            }
        }
    }

    public void xuat() {
        System.out.println("Ho ten: " + ten + ", Tuoi: " + namSinh);
    }

    @Override
    public String toString() {
        return "Nguoi{hoTen='" + ten + "', tuoi=" + namSinh + "}";
    }
}
