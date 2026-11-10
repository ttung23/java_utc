# TỰ ĐIỂN DỮ LIỆU VÀ ĐIỀU KIỆN MẪU JAVA CLASS (DOMAIN ENTITIES)

## 1. JAVA ENUMS
```java
public enum CopyStatus {
    Available, Loaned, Reserved, Damaged, Lost
}

public enum ReaderType {
    STUDENT(5, 14),
    LECTURER(10, 30);

    private final int maxBooks;
    private final int maxBorrowDays;

    ReaderType(int maxBooks, int maxBorrowDays) {
        this.maxBooks = maxBooks;
        this.maxBorrowDays = maxBorrowDays;
    }
    public int getMaxBooks() { return maxBooks; }
    public int getMaxBorrowDays() { return maxBorrowDays; }
}

public enum LoanStatus {
    BORROWED, RETURNED, OVERDUE, DAMAGED_RETURN, LOST_RETURN
}

public enum PaymentStatus {
    Unpaid, Paid
}

public enum ReservationStatus {
    Pending, Fulfilled, Expired, Cancelled
}
```

## 2. CORE DOMAIN CLASSES & BUSINESS METHODS

### Class Reader
```java
public class Reader {
    private String readerId;
    private String fullName;
    private ReaderType type;
    private LocalDate expiryDate;
    private double balanceFine;
    private String status; // ACTIVE, EXPIRED, BLOCKED

    // Business Logic Method
    public boolean canBorrow(int currentBorrowedCount) {
        if (!"ACTIVE".equalsIgnoreCase(status)) return false;
        if (expiryDate == null || expiryDate.isBefore(LocalDate.now())) return false;
        if (balanceFine > 50000.0) return false;
        if (currentBorrowedCount >= type.getMaxBooks()) return false;
        return true;
    }
}
```

### Class BookCopy
```java
public class BookCopy {
    private String copyId;
    private String isbn;
    private String barcode;
    private LocalDate acquisitionDate;
    private double price;
    private CopyStatus status;

    public boolean isAvailable() {
        return CopyStatus.Available.equals(this.status);
    }
}
```

### Class Loan
```java
public class Loan {
    private String loanId;
    private String readerId;
    private String copyId;
    private LocalDateTime borrowDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private LoanStatus status;

    // Tính số ngày trễ hạn thực tế
    public long calculateOverdueDays() {
        LocalDateTime checkPoint = (returnDate != null) ? returnDate : LocalDateTime.now();
        if (checkPoint.isAfter(dueDate)) {
            return java.time.duration.Duration.between(dueDate, checkPoint).toDays();
        }
        return 0;
    }
}
```

### Class Fine
```java
public class Fine {
    private String fineId;
    private String loanId;
    private String reason;
    private double fineAmount;
    private PaymentStatus paymentStatus;
    private LocalDateTime paymentDate;

    public static double CALCULATE_OVERDUE_FINE(long overdueDays) {
        return overdueDays * 5000.0;
    }
}
```
