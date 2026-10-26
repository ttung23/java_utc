// ===== Lop Diem2D =====
class Diem2D {
    protected double x;
    protected double y;

    // Ham tao mac dinh
    public Diem2D() {
        this.x = 0;
        this.y = 0;
    }

    // Ham tao co doi
    public Diem2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // Getters & Setters
    public double getX() { return x; }
    public double getY() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }

    // Tinh khoang cach giua 2 diem 2D
    public double khoangCach(Diem2D other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // Xac dinh diem doi xung qua goc toa do: (-x, -y)
    public Diem2D doiXung() {
        return new Diem2D(-this.x, -this.y);
    }

    // Tinh tien diem di mot vi tri voi do lech dx, dy
    public void tinhTien(double dx, double dy) {
        this.x += dx;
        this.y += dy;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}

// ===== Lop Diem3D ke thua Diem2D =====
class Diem3D extends Diem2D {
    private double z;

    // Ham tao mac dinh
    public Diem3D() {
        super();
        this.z = 0;
    }

    // Ham tao co doi
    public Diem3D(double x, double y, double z) {
        super(x, y);
        this.z = z;
    }

    // Getter & Setter
    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }

    // Tinh khoang cach giua 2 diem 3D
    public double khoangCach(Diem3D other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // Dinh nghia lai: diem doi xung qua goc toa do 3D: (-x, -y, -z)
    @Override
    public Diem2D doiXung() {
        return new Diem3D(-this.x, -this.y, -this.z);
    }

    // Tinh tien diem 3D voi do lech dx, dy, dz
    public void tinhTien(double dx, double dy, double dz) {
        super.tinhTien(dx, dy);
        this.z += dz;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}

// ===== Chuong trinh chinh =====
public class bai2 {
    public static void main(String[] args) {

        // Danh sach diem 2D
        Diem2D[] ds2D = {
            new Diem2D(1, 2),
            new Diem2D(4, 6),
            new Diem2D(-3, 5),
            new Diem2D(0, 8),
        };

        // Danh sach diem 3D
        Diem3D[] ds3D = {
            new Diem3D(1, 2, 3),
            new Diem3D(4, 6, 8),
            new Diem3D(-1, 3, 5),
        };

        // --- Tinh tong khoang cach cac diem 2D (tu diem truoc den diem sau) ---
        System.out.println("===== DANH SACH DIEM 2D =====");
        for (Diem2D d : ds2D) System.out.println("  " + d);

        double tongKC2D = 0;
        for (int i = 0; i < ds2D.length - 1; i++) {
            tongKC2D += ds2D[i].khoangCach(ds2D[i + 1]);
        }
        System.out.printf("Tong khoang cach cac diem 2D: %.4f%n", tongKC2D);

        // --- Tinh tong khoang cach cac diem 3D ---
        System.out.println("\n===== DANH SACH DIEM 3D =====");
        for (Diem3D d : ds3D) System.out.println("  " + d);

        double tongKC3D = 0;
        for (int i = 0; i < ds3D.length - 1; i++) {
            tongKC3D += ds3D[i].khoangCach(ds3D[i + 1]);
        }
        System.out.printf("Tong khoang cach cac diem 3D: %.4f%n", tongKC3D);

        // --- Dua cac diem doi xung vao danh sach va hien thi ---
        System.out.println("\n===== DIEM DOI XUNG 2D =====");
        Diem2D[] doiXung2D = new Diem2D[ds2D.length];
        for (int i = 0; i < ds2D.length; i++) {
            doiXung2D[i] = ds2D[i].doiXung();
            System.out.println("  " + ds2D[i] + " -> doi xung: " + doiXung2D[i]);
        }

        System.out.println("\n===== DIEM DOI XUNG 3D =====");
        Diem2D[] doiXung3D = new Diem2D[ds3D.length];
        for (int i = 0; i < ds3D.length; i++) {
            doiXung3D[i] = ds3D[i].doiXung();
            System.out.println("  " + ds3D[i] + " -> doi xung: " + doiXung3D[i]);
        }

        // --- Demo tinh tien ---
        System.out.println("\n===== TINH TIEN =====");
        Diem2D p2 = new Diem2D(3, 4);
        System.out.println("Diem 2D truoc: " + p2);
        p2.tinhTien(2, -1);
        System.out.println("Sau tinh tien (dx=2, dy=-1): " + p2);

        Diem3D p3 = new Diem3D(1, 2, 3);
        System.out.println("Diem 3D truoc: " + p3);
        p3.tinhTien(1, 1, 2);
        System.out.println("Sau tinh tien (dx=1, dy=1, dz=2): " + p3);
    }
}
