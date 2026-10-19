public class bai2 {
    static int[] mangTong(int[] a, int[] b) {
        int maxLen = Math.max(a.length, b.length);
        int[] result = new int[maxLen];

        for (int i = 0; i < maxLen; i++) {
            int valA = (i < a.length) ? a[i] : 0;
            int valB = (i < b.length) ? b[i] : 0;
            result[i] = valA + valB;
        }

        return result;
    }

    static void inMang(int[] arr, String ten) {
        System.out.print(ten + " = ");
        for (int i = 0; i < arr.length; i++) {
            System.out.print(arr[i]);
            if (i < arr.length - 1) System.out.print(", ");
        }
        System.out.println("");
    }

    public static void main(String[] args) {
        int[] a = {1, 2, 3, 4, 5};
        int[] b = {10, 20, 30};

        int[] tong = mangTong(a, b);

        inMang(a, "Mang 1");
        inMang(b, "Mang 2");
        inMang(tong, "Mang tong");
    }
}
