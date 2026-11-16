public interface Shape {
    double area();
    double perimeter();
    double distance();
    void move(double dx, double dy);
    void rotate(double angle);
    void zoom(double ratio);
    String toString();
}
