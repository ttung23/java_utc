public class Triangle implements Shape {
    private Point a;
    private Point b;
    private Point c;

    public Triangle() {
        this.a = new Point(0, 0);
        this.b = new Point(1, 0);
        this.c = new Point(0, 1);
    }

    public Triangle(Point a, Point b, Point c) {
        this.a = new Point(a.getX(), a.getY());
        this.b = new Point(b.getX(), b.getY());
        this.c = new Point(c.getX(), c.getY());
    }

    public Triangle(double x1, double y1,
                    double x2, double y2,
                    double x3, double y3) {
        this.a = new Point(x1, y1);
        this.b = new Point(x2, y2);
        this.c = new Point(x3, y3);
    }

    public Point getA() { return a; }
    public void setA(Point a) { this.a = a; }

    public Point getB() { return b; }
    public void setB(Point b) { this.b = b; }

    public Point getC() { return c; }
    public void setC(Point c) { this.c = c; }

    private double sideAB() {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double sideBC() {
        double dx = c.getX() - b.getX();
        double dy = c.getY() - b.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double sideCA() {
        double dx = a.getX() - c.getX();
        double dy = a.getY() - c.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public double area() {
        double ab = sideAB(), bc = sideBC(), ca = sideCA();
        double s = (ab + bc + ca) / 2;
        return Math.sqrt(s * (s - ab) * (s - bc) * (s - ca));
    }

    @Override
    public double perimeter() {
        return sideAB() + sideBC() + sideCA();
    }

    @Override
    public double distance() {
        double gx = (a.getX() + b.getX() + c.getX()) / 3;
        double gy = (a.getY() + b.getY() + c.getY()) / 3;
        return Math.sqrt(gx * gx + gy * gy);
    }

    @Override
    public void move(double dx, double dy) {
        a.move(dx, dy);
        b.move(dx, dy);
        c.move(dx, dy);
    }

    @Override
    public void rotate(double angle) {
        a.rotate(angle);
        b.rotate(angle);
        c.rotate(angle);
    }

    @Override
    public void zoom(double ratio) {
        a.zoom(ratio);
        b.zoom(ratio);
        c.zoom(ratio);
    }

    @Override
    public String toString() {
        return String.format("Triangle[A=%s, B=%s, C=%s, area=%.2f, perimeter=%.2f]",
                a, b, c, area(), perimeter());
    }
}
