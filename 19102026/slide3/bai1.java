class Diem {
    private double x;
    private double y;

    public Diem(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
}

class DuongThang {
    private Diem A;
    private Diem B;

    public DuongThang(Diem A, Diem B) {
        this.A = A;
        this.B = B;
    }

    public Diem getA() { return A; }
    public Diem getB() { return B; }

    public double doDai() {
        double dx = B.getX() - A.getX();
        double dy = B.getY() - A.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
}

public class bai1 {
    public static void main(String[] args) {
        Diem A = new Diem(0, 0);
        Diem B = new Diem(3, 4);

        DuongThang dt = new DuongThang(A, B);

        System.out.println("Diem A: (" + dt.getA().getX() + ", " + dt.getA().getY() + ")");
        System.out.println("Diem B: (" + dt.getB().getX() + ", " + dt.getB().getY() + ")");
        System.out.println("Do dai duong thang AB: " + dt.doDai());
    }
}
