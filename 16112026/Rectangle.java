public class Rectangle implements Shape {
    private Point origin;
    private double width;
    private double height;

    public Rectangle() {
        this.origin = new Point(0, 0);
        this.width  = 1;
        this.height = 1;
    }

    public Rectangle(double x, double y, double width, double height) {
        this.origin = new Point(x, y);
        this.width  = width;
        this.height = height;
    }

    public Rectangle(Point origin, double width, double height) {
        this.origin = new Point(origin.getX(), origin.getY());
        this.width  = width;
        this.height = height;
    }

    public Point getOrigin() { return origin; }
    public void setOrigin(Point origin) { this.origin = origin; }

    public double getWidth() { return width; }
    public void setWidth(double width) {
        if (width > 0) this.width = width;
    }

    public double getHeight() { return height; }
    public void setHeight(double height) {
        if (height > 0) this.height = height;
    }

    @Override
    public double area() {
        return width * height;
    }

    @Override
    public double perimeter() {
        return 2 * (width + height);
    }

    @Override
    public double distance() {
        double cx = origin.getX() + width  / 2;
        double cy = origin.getY() + height / 2;
        return Math.sqrt(cx * cx + cy * cy);
    }

    @Override
    public void move(double dx, double dy) {
        origin.move(dx, dy);
    }

    @Override
    public void rotate(double angle) {
        double cx = origin.getX() + width  / 2;
        double cy = origin.getY() + height / 2;
        double rad = Math.toRadians(angle);
        double newCx = cx * Math.cos(rad) - cy * Math.sin(rad);
        double newCy = cx * Math.sin(rad) + cy * Math.cos(rad);
        origin.setX(newCx - width  / 2);
        origin.setY(newCy - height / 2);
    }

    @Override
    public void zoom(double ratio) {
        origin.zoom(ratio);
        this.width  *= ratio;
        this.height *= ratio;
    }

    @Override
    public String toString() {
        return String.format("Rectangle[origin=%s, width=%.2f, height=%.2f, area=%.2f, perimeter=%.2f]",
                origin, width, height, area(), perimeter());
    }
}
