import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {

    private static Shape randomShape(Random rnd) {
        int type = rnd.nextInt(5);
        double range = 10.0;

        switch (type) {
            case 0:
                return new Point(rnd(rnd, range), rnd(rnd, range));
            case 1:
                return new Circle(rnd(rnd, range), rnd(rnd, range),
                                  1 + rnd.nextDouble() * 9);
            case 2:
                return new Line(rnd(rnd, range), rnd(rnd, range),
                                rnd(rnd, range), rnd(rnd, range));
            case 3:
                return new Triangle(
                        rnd(rnd, range), rnd(rnd, range),
                        rnd(rnd, range), rnd(rnd, range),
                        rnd(rnd, range), rnd(rnd, range));
            default:
                return new Rectangle(rnd(rnd, range), rnd(rnd, range),
                                     1 + rnd.nextDouble() * 9,
                                     1 + rnd.nextDouble() * 9);
        }
    }

    private static double rnd(Random r, double range) {
        return (r.nextDouble() * 2 - 1) * range;
    }

    private static void displayList(List<Shape> shapes) {
        System.out.println("\n========== DANH SACH CAC HINH ==========");
        for (int i = 0; i < shapes.size(); i++) {
            System.out.printf("[%2d] %s%n", i + 1, shapes.get(i));
        }
    }

    private static void calcTotals(List<Shape> shapes) {
        double totalArea = 0, totalPerimeter = 0;
        for (Shape s : shapes) {
            totalArea      += s.area();
            totalPerimeter += s.perimeter();
        }
        System.out.println("\n========== TONG KET ==========");
        System.out.printf("Tong dien tich : %.4f%n", totalArea);
        System.out.printf("Tong chu vi    : %.4f%n", totalPerimeter);
    }

    private static void findMinMax(List<Shape> shapes) {
        Shape maxShape = null, minShape = null;
        double maxArea = -1, minArea = Double.MAX_VALUE;

        for (Shape s : shapes) {
            double a = s.area();
            if (a > 0) {
                if (a > maxArea) { maxArea = a; maxShape = s; }
                if (a < minArea) { minArea = a; minShape = s; }
            }
        }

        System.out.println("\n========== DIEN TICH LON NHAT / NHO NHAT ==========");
        if (maxShape != null) {
            System.out.printf("Lon nhat (%.4f): %s%n", maxArea, maxShape);
            System.out.printf("Nho nhat (%.4f): %s%n", minArea, minShape);
        } else {
            System.out.println("Khong co hinh nao co dien tich > 0.");
        }
    }

    private static void zoomAll(List<Shape> shapes, double ratio) {
        System.out.printf("%n========== PHONG TO RATIO = %.2f ==========%n", ratio);
        for (Shape s : shapes) {
            s.zoom(ratio);
        }
        System.out.println("Sau khi zoom:");
        for (int i = 0; i < shapes.size(); i++) {
            System.out.printf("[%2d] %s%n", i + 1, shapes.get(i));
        }
    }

    public static void main(String[] args) {
        Random rnd = new Random(42);
        int n = 10;

        List<Shape> shapes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            shapes.add(randomShape(rnd));
        }

        displayList(shapes);
        calcTotals(shapes);
        findMinMax(shapes);
        zoomAll(shapes, 2.0);
    }
}
