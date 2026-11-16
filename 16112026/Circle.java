public class Circle implements Shape {
    private Point center;
    private double radius;

    public Circle() {
        this.center = new Point(0, 0);
        this.radius = 1;
    }

    public Circle(double x, double y, double radius) {
        this.center = new Point(x, y);
        this.radius = radius;
    }

    public Circle(Point center, double radius) {
        this.center = new Point(center.getX(), center.getY());
        this.radius = radius;
    }

    public Point getCenter() { return center; }
    public void setCenter(Point center) { this.center = center; }

    public double getRadius() { return radius; }
    public void setRadius(double radius) {
        if (radius > 0) this.radius = radius;
    }

    @Override
    public double area() {
        return Math.PI * radius * radius;
    }

    @Override
    public double perimeter() {
        return 2 * Math.PI * radius;
    }

    @Override
    public double distance() {
        return center.distance();
    }

    @Override
    public void move(double dx, double dy) {
        center.move(dx, dy);
    }

    @Override
    public void rotate(double angle) {
        center.rotate(angle);
    }

    @Override
    public void zoom(double ratio) {
        this.radius *= ratio;
        center.zoom(ratio);
    }

    @Override
    public String toString() {
        return String.format("Circle[center=%s, radius=%.2f, area=%.2f, perimeter=%.2f]",
                center, radius, area(), perimeter());
    }
}
