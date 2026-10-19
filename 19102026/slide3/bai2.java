class Diem {
    private double x;
    private double y;

    public Diem(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() { return x; }
    public double getY() { return y; }

    public double khoangCach(Diem other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}

class TamGiac {
    private Diem A, B, C;

    public TamGiac(Diem A, Diem B, Diem C) {
        this.A = A;
        this.B = B;
        this.C = C;
    }

    public double chuVi() {
        return A.khoangCach(B) + B.khoangCach(C) + C.khoangCach(A);
    }

    public double dienTich() {
        double a = B.khoangCach(C);
        double b = C.khoangCach(A);
        double c = A.khoangCach(B);
        double s = (a + b + c) / 2;
        return Math.sqrt(s * (s - a) * (s - b) * (s - c));
    }
}

class ChuNhat {
    private double dai;
    private double rong;

    public ChuNhat(double dai, double rong) {
        this.dai = dai;
        this.rong = rong;
    }

    public double chuVi() {
        return 2 * (dai + rong);
    }

    public double dienTich() {
        return dai * rong;
    }
}

class HinhTron {
    private Diem O;
    private double banKinh;

    public HinhTron(Diem O, double banKinh) {
        this.O = O;
        this.banKinh = banKinh;
    }

    public double chuVi() {
        return 2 * Math.PI * banKinh;
    }

    public double dienTich() {
        return Math.PI * banKinh * banKinh;
    }
}

public class bai2 {
    public static void main(String[] args) {
        // 2 Tam giac
        TamGiac tg1 = new TamGiac(new Diem(0, 0), new Diem(3, 0), new Diem(0, 4));
        TamGiac tg2 = new TamGiac(new Diem(1, 1), new Diem(5, 1), new Diem(3, 5));

        // 2 Chu nhat
        ChuNhat cn1 = new ChuNhat(5, 3);
        ChuNhat cn2 = new ChuNhat(10, 7);

        // 2 Hinh tron
        HinhTron ht1 = new HinhTron(new Diem(0, 0), 5);
        HinhTron ht2 = new HinhTron(new Diem(1, 2), 3);

        System.out.println("=== TAM GIAC 1 ===");
        System.out.printf("Chu vi: %.2f%n", tg1.chuVi());
        System.out.printf("Dien tich: %.2f%n", tg1.dienTich());

        System.out.println("\n=== TAM GIAC 2 ===");
        System.out.printf("Chu vi: %.2f%n", tg2.chuVi());
        System.out.printf("Dien tich: %.2f%n", tg2.dienTich());

        System.out.println("\n=== CHU NHAT 1 (dai=5, rong=3) ===");
        System.out.printf("Chu vi: %.2f%n", cn1.chuVi());
        System.out.printf("Dien tich: %.2f%n", cn1.dienTich());

        System.out.println("\n=== CHU NHAT 2 (dai=10, rong=7) ===");
        System.out.printf("Chu vi: %.2f%n", cn2.chuVi());
        System.out.printf("Dien tich: %.2f%n", cn2.dienTich());

        System.out.println("\n=== HINH TRON 1 (ban kinh=5) ===");
        System.out.printf("Chu vi: %.2f%n", ht1.chuVi());
        System.out.printf("Dien tich: %.2f%n", ht1.dienTich());

        System.out.println("\n=== HINH TRON 2 (ban kinh=3) ===");
        System.out.printf("Chu vi: %.2f%n", ht2.chuVi());
        System.out.printf("Dien tich: %.2f%n", ht2.dienTich());
    }
}
