package buoi3.nhanvien;

import java.util.Scanner;

public class NhanVien extends Nguoi {
    public static final double LUONG_CB = 1.5;

    private int namCongTac;
    private double heSoLuong;

    public NhanVien() {
        super();
        namCongTac = 0;
        heSoLuong = 1.0;
    }

    public NhanVien(String ten, int tuoi, int soNam, double heSo) {
        super(ten, tuoi);
        this.namCongTac = soNam;
        this.heSoLuong = heSo;
    }

    public int getSnct() {
        return namCongTac;
    }

    public void setSnct(int soNam) {
        this.namCongTac = soNam;
    }

    public double getHsl() {
        return heSoLuong;
    }

    public void setHsl(double hs) {
        this.heSoLuong = hs;
    }

    public int getSoNamCongTac() {
        return namCongTac;
    }

    public void setSoNamCongTac(int soNam) {
        this.namCongTac = soNam;
    }

    public double getHeSoLuong() {
        return heSoLuong;
    }

    public void setHeSoLuong(double hs) {
        this.heSoLuong = hs;
    }

    public double tinhPhuCap() {
        return namCongTac * 0.2;
    }

    public double tinhLuong() {
        return LUONG_CB * heSoLuong + tinhPhuCap();
    }

    public double tinhTienLuong() {
        return tinhLuong();
    }

    public double tinhLuongVND() {
        return tinhLuong() * 1000000;
    }

    @Override
    public void nhap(Scanner scanner) {
        super.nhap(scanner);

        System.out.print("Nhap so nam cong tac (snct): ");
        while (true) {
            try {
                int tmp = Integer.parseInt(scanner.nextLine().trim());
                if (tmp < 0) {
                    System.out.print("So nam cong tac phai >= 0. Nhap lai: ");
                    continue;
                }
                namCongTac = tmp;
                break;
            } catch (NumberFormatException ex) {
                System.out.print("So nam cong tac khong hop le. Nhap lai: ");
            }
        }

        System.out.print("Nhap he so luong (hsl): ");
        while (true) {
            try {
                double tmp = Double.parseDouble(scanner.nextLine().trim());
                if (tmp <= 0) {
                    System.out.print("He so luong phai > 0. Nhap lai: ");
                    continue;
                }
                heSoLuong = tmp;
                break;
            } catch (NumberFormatException ex) {
                System.out.print("He so luong khong hop le. Nhap lai: ");
            }
        }
    }

    @Override
    public void xuat() {
        System.out.printf("Ho ten: %-18s | Tuoi: %2d | SNCT: %2d nam | HSL: %4.2f | Phu cap: %5.2f trieu | Tien luong: %6.2f trieu (%,.0f VND)%n",
                getHoTen(), getTuoi(), namCongTac, heSoLuong, tinhPhuCap(), tinhLuong(), tinhLuongVND());
    }

    @Override
    public String toString() {
        return "NhanVien{" +
                "hoTen='" + getHoTen() + '\'' +
                ", tuoi=" + getTuoi() +
                ", snct=" + namCongTac +
                ", hsl=" + heSoLuong +
                ", phuCap=" + String.format("%.2f", tinhPhuCap()) + " trieu" +
                ", tienLuong=" + String.format("%.2f", tinhLuong()) + " trieu (" + String.format("%,.0f", tinhLuongVND()) + " VND)" +
                '}';
    }
}
