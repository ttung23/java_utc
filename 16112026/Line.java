public class Line implements Shape {
    private Point start;
    private Point end;

    public Line() {
        this.start = new Point(0, 0);
        this.end   = new Point(1, 0);
    }

    public Line(double x1, double y1, double x2, double y2) {
        this.start = new Point(x1, y1);
        this.end   = new Point(x2, y2);
    }

    public Line(Point start, Point end) {
        this.start = new Point(start.getX(), start.getY());
        this.end   = new Point(end.getX(),   end.getY());
    }

    public Point getStart() { return start; }
    public void setStart(Point start) { this.start = start; }

    public Point getEnd() { return end; }
    public void setEnd(Point end) { this.end = end; }

    public double length() {
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public double area() {
        return 0;
    }

    @Override
    public double perimeter() {
        return length();
    }

    @Override
    public double distance() {
        double mx = (start.getX() + end.getX()) / 2;
        double my = (start.getY() + end.getY()) / 2;
        return Math.sqrt(mx * mx + my * my);
    }

    @Override
    public void move(double dx, double dy) {
        start.move(dx, dy);
        end.move(dx, dy);
    }

    @Override
    public void rotate(double angle) {
        start.rotate(angle);
        end.rotate(angle);
    }

    @Override
    public void zoom(double ratio) {
        start.zoom(ratio);
        end.zoom(ratio);
    }

    @Override
    public String toString() {
        return String.format("Line[start=%s, end=%s, length=%.2f]",
                start, end, length());
    }
}
