package buoi3.nhanvien;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<NhanVien> dsNV = new ArrayList<>();

        System.out.println("==================================================");
        System.out.println("         CHUONG TRINH QUAN LY NHAN VIEN          ");
        System.out.println("==================================================");

        int soLuong = 0;
        System.out.print("Nhap so luong nhan vien n: ");
        while (true) {
            try {
                soLuong = Integer.parseInt(scanner.nextLine().trim());
                if (soLuong > 0)
                    break;
                System.out.print("So luong nhan vien phai > 0. Nhap lai: ");
            } catch (NumberFormatException ex) {
                System.out.print("Vui long nhap so nguyen hop le: ");
            }
        }

        System.out.println("\n---------------- NHAP DANH SACH NHAN VIEN ----------------");
        for (int i = 0; i < soLuong; i++) {
            System.out.println("\n--- Nhan vien thu " + (i + 1) + " ---");
            NhanVien nhanVien = new NhanVien();
            nhanVien.nhap(scanner);
            dsNV.add(nhanVien);
        }

        System.out.println("\n================ DANH SACH NHAN VIEN VUA NHAP ================");
        inDanhSach(dsNV);

        System.out.println("\n========= NHAN VIEN CO SO NAM CONG TAC NHIEU NHAT =========");
        locTheoSNCTMax(dsNV);

        System.out.println("\n====== DANH SACH NHAN VIEN THEO THU TU LUONG GIAM DAN ======");
        sapXepVaInTheoLuong(dsNV);

        scanner.close();
    }

    static void inDanhSach(List<NhanVien> ds) {
        if (ds == null || ds.isEmpty()) {
            System.out.println("Danh sach trong.");
            return;
        }
        for (int idx = 0; idx < ds.size(); idx++) {
            System.out.printf("%2d. ", (idx + 1));
            ds.get(idx).xuat();
        }
    }

    static List<NhanVien> locTheoSNCTMax(List<NhanVien> ds) {
        if (ds == null || ds.isEmpty()) {
            System.out.println("Danh sach trong.");
            return new ArrayList<>();
        }

        int max = ds.get(0).getSnct();
        for (int i = 1; i < ds.size(); i++) {
            if (ds.get(i).getSnct() > max)
                max = ds.get(i).getSnct();
        }

        List<NhanVien> kq = new ArrayList<>();
        for (NhanVien nv : ds) {
            if (nv.getSnct() == max)
                kq.add(nv);
        }

        System.out.println("So nam cong tac nhieu nhat: " + max + " nam");
        System.out.println("Danh sach nhan vien dat tieu chi:");
        for (int i = 0; i < kq.size(); i++) {
            System.out.printf("%2d. ", (i + 1));
            kq.get(i).xuat();
        }
        return kq;
    }

    static void sapXepVaInTheoLuong(List<NhanVien> ds) {
        if (ds == null || ds.isEmpty()) {
            System.out.println("Danh sach trong.");
            return;
        }
        List<NhanVien> ban = new ArrayList<>(ds);
        ban.sort((a, b) -> Double.compare(b.tinhLuong(), a.tinhLuong()));
        inDanhSach(ban);
    }
}
