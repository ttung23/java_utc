# KIẾN TRÚC PHẦN MỀM VÀ THIẾT KẾ SERVICE / REPOSITORY INTERFACES

## 1. SƠ ĐỒ KIẾN TRÚC 3 TẦNG (3-TIER ARCHITECTURE)
```
[Presentation Layer] -> REST Controllers / Barcode Scanner Client
        |
[Business Logic Layer] -> LoanService, FineService, ReservationService, BookService
        |
[Persistence Layer] -> BookCopyRepository, LoanRepository, ReaderRepository (Oracle DB)
```

## 2. JAVA REPOSITORY INTERFACES (DATA ACCESS OBJECTS)
```java
public interface ReaderRepository {
    Optional<Reader> findById(String readerId);
    void updateBalanceFine(String readerId, double newBalance);
}

public interface BookCopyRepository {
    Optional<BookCopy> findByBarcode(String barcode);
    boolean updateStatus(String copyId, CopyStatus status);
}

public interface LoanRepository {
    Loan save(Loan loan);
    Optional<Loan> findActiveLoanByCopyId(String copyId);
    int countActiveLoansByReaderId(String readerId);
    void updateReturnStatus(String loanId, LocalDateTime returnDate, LoanStatus status);
}

public interface FineRepository {
    Fine save(Fine fine);
    List<Fine> findUnpaidFinesByReaderId(String readerId);
}
```

## 3. JAVA SERVICE INTERFACES
```java
public interface LoanService {
    LoanDTO createLoan(String readerId, String copyBarcode) throws Exception;
    ReturnResultDTO processReturn(String copyBarcode, boolean isDamaged, boolean isLost) throws Exception;
}

public interface FineService {
    double calculateOverdueFine(Loan loan);
    FineDTO createFine(String loanId, double amount, String reason);
    boolean payFine(String fineId, double amountPaid);
}
```

## 4. REST API ENDPOINTS SUGGESTION FOR SPRING BOOT
- `POST /api/v1/loans/borrow` - Lập phiếu mượn (Request body: `readerId`, `barcode`).
- `POST /api/v1/loans/return` - Nhận trả sách & xử lý sự cố (Request body: `barcode`, `isDamaged`, `isLost`).
- `GET /api/v1/readers/{id}/eligibility` - Kiểm tra độc giả có đủ điều kiện mượn sách.
- `POST /api/v1/reservations` - Đặt trước đầu sách.
- `GET /api/v1/reports/overdue` - Xuất báo cáo danh sách độc giả quá hạn.
