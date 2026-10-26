class PhanSo {
    // ===== Thuoc tinh =====
    private int tuSo;    // T
    private int mauSo;   // M

    // ===== Ham mac dinh =====
    public PhanSo() {
        this.tuSo  = 0;
        this.mauSo = 1;
    }

    // ===== Ham tao co doi =====
    public PhanSo(int tuSo, int mauSo) {
        if (mauSo == 0) {
            System.out.println("Mau so khong the bang 0! Dat mac dinh 1.");
            mauSo = 1;
        }
        if (mauSo < 0) {
            tuSo  = -tuSo;
            mauSo = -mauSo;
        }
        this.tuSo  = tuSo;
        this.mauSo = mauSo;
    }

    // ===== Getters Setters =====
    public int getTuSo()  { return tuSo; }
    public int getMauSo() { return mauSo; }

    public void setTuSo(int tuSo)   { this.tuSo = tuSo; }
    public void setMauSo(int mauSo) {
        if (mauSo == 0) { System.out.println("Mau so khong the bang 0!"); return; }
        this.mauSo = mauSo;
    }

    // ===== toString =====
    @Override
    public String toString() {
        if (mauSo == 1) return tuSo + "";
        return tuSo + "/" + mauSo;
    }

    private int ucln(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    // ===== Rut gon phan so =====
    public PhanSo rutGon() {
        int g = ucln(Math.abs(tuSo), mauSo);
        return new PhanSo(tuSo / g, mauSo / g);
    }

    // ===== Tinh tong =====
    public PhanSo cong(PhanSo other) {
        int t = this.tuSo * other.mauSo + other.tuSo * this.mauSo;
        int m = this.mauSo * other.mauSo;
        return new PhanSo(t, m).rutGon();
    }

    // ===== Tinh hieu =====
    public PhanSo tru(PhanSo other) {
        int t = this.tuSo * other.mauSo - other.tuSo * this.mauSo;
        int m = this.mauSo * other.mauSo;
        return new PhanSo(t, m).rutGon();
    }

    // ===== Tinh tich =====
    public PhanSo nhan(PhanSo other) {
        int t = this.tuSo * other.tuSo;
        int m = this.mauSo * other.mauSo;
        return new PhanSo(t, m).rutGon();
    }

    // ===== Tinh thuong =====
    public PhanSo chia(PhanSo other) {
        if (other.tuSo == 0) {
            System.out.println("Khong the chia cho 0!");
            return null;
        }
        int t = this.tuSo * other.mauSo;
        int m = this.mauSo * other.tuSo;
        return new PhanSo(t, m).rutGon();
    }

    public int soSanh(PhanSo other) {
        return this.tuSo * other.mauSo - other.tuSo * this.mauSo;
    }

    public boolean laSoNguyen() {
        return tuSo % mauSo == 0;
    }

    public double giaTriThuc() {
        return (double) tuSo / mauSo;
    }
}

// ===== Chuong trinh chinh =====
public class bai1 {
    public static void main(String[] args) {

        // ---------- Phan 1: Kiem tra phep toan 2 phan so ----------
        PhanSo p1 = new PhanSo(1, 2);  // 1/2
        PhanSo p2 = new PhanSo(1, 3);  // 1/3

        System.out.println("===== KIEM TRA PHEP TOAN =====");
        System.out.println("p1 = " + p1);
        System.out.println("p2 = " + p2);
        System.out.println("Tong   : " + p1.cong(p2));
        System.out.println("Hieu   : " + p1.tru(p2));
        System.out.println("Tich   : " + p1.nhan(p2));
        System.out.println("Thuong : " + p1.chia(p2));

        int cmp = p1.soSanh(p2);
        if      (cmp > 0) System.out.println("p1 > p2");
        else if (cmp < 0) System.out.println("p1 < p2");
        else              System.out.println("p1 = p2");

        System.out.println("Rut gon 4/8: " + new PhanSo(4, 8).rutGon());

        // ---------- Phan 2: Tap hop phan so ----------
        PhanSo[] tapPhanSo = {
            new PhanSo(3, 4),   // 3/4
            new PhanSo(5, 2),   // 5/2
            new PhanSo(7, 3),   // 7/3
            new PhanSo(2, 1),   // 2   (so nguyen)
            new PhanSo(-1, 6),  // -1/6
            new PhanSo(8, 4),   // 2   (so nguyen)
            new PhanSo(11, 5),  // 11/5
        };

        System.out.println("\n===== TAP HOP PHAN SO =====");
        for (PhanSo ps : tapPhanSo) {
            System.out.println("  " + ps);
        }

        // --- Tim phan so lon nhat ---
        PhanSo lonNhat = tapPhanSo[0];
        for (int i = 1; i < tapPhanSo.length; i++) {
            if (tapPhanSo[i].soSanh(lonNhat) > 0) {
                lonNhat = tapPhanSo[i];
            }
        }
        System.out.println("\nPhan so lon nhat: " + lonNhat);

        // --- Tinh tong cac phan so ---
        PhanSo tong = new PhanSo(0, 1);
        for (PhanSo ps : tapPhanSo) {
            tong = tong.cong(ps);
        }
        System.out.println("Tong cac phan so: " + tong);

        // --- Xac dinh so phan so KHONG phai so nguyen ---
        int demKhongNguyen = 0;
        System.out.println("\nCac phan so KHONG phai so nguyen:");
        for (PhanSo ps : tapPhanSo) {
            if (!ps.laSoNguyen()) {
                System.out.println("  " + ps);
                demKhongNguyen++;
            }
        }
        System.out.println("So luong phan so khong phai so nguyen: " + demKhongNguyen);
    }
}
