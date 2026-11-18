package com.library.common.seed;

import com.library.catalog.entity.Author;
import com.library.catalog.entity.Book;
import com.library.catalog.entity.BookAuthor;
import com.library.catalog.entity.BookCategory;
import com.library.catalog.entity.BookCopy;
import com.library.catalog.entity.Category;
import com.library.catalog.entity.DocumentType;
import com.library.catalog.entity.Language;
import com.library.catalog.entity.Location;
import com.library.catalog.entity.Publisher;
import com.library.catalog.repository.AuthorRepository;
import com.library.catalog.repository.BookAuthorRepository;
import com.library.catalog.repository.BookCategoryRepository;
import com.library.catalog.repository.BookCopyRepository;
import com.library.catalog.repository.BookRepository;
import com.library.catalog.repository.CategoryRepository;
import com.library.catalog.repository.DocumentTypeRepository;
import com.library.catalog.repository.LanguageRepository;
import com.library.catalog.repository.LocationRepository;
import com.library.catalog.repository.PublisherRepository;
import com.library.common.enums.BookAuthorRole;
import com.library.common.enums.BookStatus;
import com.library.common.enums.CopyCondition;
import com.library.common.enums.CopyStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Bổ sung catalog sách thật (idempotent theo ISBN13).
 * Chạy mỗi lần start profile {@code dev} — không xóa data cũ.
 */
@Slf4j
@Component
@Profile("dev")
@Order(100)
@RequiredArgsConstructor
public class RealCatalogSeeder implements ApplicationRunner {

    private final LanguageRepository languageRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final LocationRepository locationRepository;
    private final BookRepository bookRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final BookCopyRepository bookCopyRepository;

    private record RealBook(
            String isbn13,
            String title,
            String subtitle,
            String author,
            String publisher,
            String categoryCode,
            String categoryName,
            int year,
            int pages,
            long price,
            String description,
            String[] tags,
            int copies
    ) {
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Language vi = languageRepository.findByCode("vi").orElseGet(() -> {
            Language l = new Language();
            l.setCode("vi");
            l.setName("Tiếng Việt");
            return languageRepository.save(l);
        });
        Language en = languageRepository.findByCode("en").orElseGet(() -> {
            Language l = new Language();
            l.setCode("en");
            l.setName("English");
            return languageRepository.save(l);
        });
        DocumentType bookType = documentTypeRepository.findByCode("BOOK").orElseGet(() -> {
            DocumentType t = new DocumentType();
            t.setCode("BOOK");
            t.setName("Sách");
            return documentTypeRepository.save(t);
        });
        Location loc = locationRepository.findByCodeAndDeletedAtIsNull("LOC-01").orElseGet(() -> {
            List<Location> all = locationRepository.findAll();
            if (!all.isEmpty()) return all.get(0);
            Location created = new Location();
            created.setCode("LOC-REAL");
            created.setZone("Khu A");
            created.setFloor("1");
            created.setRow("R1");
            created.setShelf("S1");
            created.setCapacity(200);
            return locationRepository.save(created);
        });

        int created = 0;
        int skipped = 0;
        for (RealBook rb : catalog()) {
            if (bookRepository.findByIsbn13AndDeletedAtIsNull(rb.isbn13()).isPresent()) {
                skipped++;
                continue;
            }
            Language lang = rb.tags().length > 0 && List.of(rb.tags()).contains("en") ? en : vi;
            Publisher publisher = ensurePublisher(rb.publisher());
            Author author = ensureAuthor(rb.author());
            Category category = ensureCategory(rb.categoryCode(), rb.categoryName());

            Book book = new Book();
            book.setIsbn13(rb.isbn13());
            book.setTitle(rb.title());
            book.setSubtitle(rb.subtitle());
            book.setPublisher(publisher);
            book.setPublishYear(rb.year());
            book.setLanguage(lang);
            book.setDocumentType(bookType);
            book.setPages(rb.pages());
            book.setPrice(BigDecimal.valueOf(rb.price()));
            book.setDescription(rb.description());
            book.setCoverUrl("https://placehold.co/240x360?text=" + encode(rb.title()));
            book.setTags(List.of(rb.tags()));
            book.setReference(false);
            book.setLoanable(true);
            book.setStatus(BookStatus.ACTIVE);
            book = bookRepository.save(book);

            BookAuthor ba = new BookAuthor();
            ba.setBookId(book.getId());
            ba.setAuthorId(author.getId());
            ba.setRole(BookAuthorRole.AUTHOR);
            ba.setSortOrder(0);
            bookAuthorRepository.save(ba);

            BookCategory bc = new BookCategory();
            bc.setBookId(book.getId());
            bc.setCategoryId(category.getId());
            bookCategoryRepository.save(bc);

            for (int c = 1; c <= rb.copies(); c++) {
                BookCopy copy = new BookCopy();
                copy.setBook(book);
                copy.setBarcode(String.format("REAL-%s-%02d", rb.isbn13().substring(9), c));
                copy.setCopyNo(c);
                copy.setLocation(loc);
                copy.setStatus(CopyStatus.AVAILABLE);
                copy.setCondition(CopyCondition.GOOD);
                copy.setPurchasePrice(book.getPrice());
                copy.setAcquiredAt(Instant.now());
                bookCopyRepository.save(copy);
            }
            created++;
        }
        log.info("RealCatalogSeeder: thêm {} sách thật, bỏ qua {} (đã có ISBN). Tổng books={}.",
                created, skipped, bookRepository.count());
    }

    private static String encode(String title) {
        return title.length() > 28 ? title.substring(0, 28).replace(' ', '+') : title.replace(' ', '+');
    }

    private Publisher ensurePublisher(String name) {
        return publisherRepository.findFirstByDeletedAtIsNullAndNameIgnoreCase(name).orElseGet(() -> {
            Publisher p = new Publisher();
            p.setName(name);
            p.setAddress("Việt Nam");
            return publisherRepository.save(p);
        });
    }

    private Author ensureAuthor(String name) {
        return authorRepository.findFirstByDeletedAtIsNullAndNameIgnoreCase(name).orElseGet(() -> {
            Author a = new Author();
            a.setName(name);
            a.setNationality(name.matches(".*[A-Za-z].*") && !name.matches(".*(Nguyễn|Trần|Lê|Phạm|Hoàng|Võ|Đặng|Bùi|Đỗ|Hồ|Nam Cao|Kim Lân|Tô Hoài|Ngô).*")
                    ? "Quốc tế" : "Việt Nam");
            return authorRepository.save(a);
        });
    }

    private Category ensureCategory(String code, String name) {
        return categoryRepository.findByCodeAndDeletedAtIsNull(code).orElseGet(() -> {
            Category c = new Category();
            c.setCode(code);
            c.setName(name);
            c.setSortOrder(50);
            c.setActive(true);
            return categoryRepository.save(c);
        });
    }

    private List<RealBook> catalog() {
        return List.of(
                // Văn học Việt
                rb("9786041234001", "Số đỏ", null, "Vũ Trọng Phụng", "NXB Văn học",
                        "VH-VN", "Văn học Việt Nam", 2019, 280, 85000,
                        "Tiểu thuyết trào phúng kinh điển về xã hội Việt Nam đầu thế kỷ 20.",
                        tags("van-hoc", "viet-nam", "kinh-dien"), 3),
                rb("9786041234002", "Chí Phèo", "Tập truyện ngắn", "Nam Cao", "NXB Văn học",
                        "VH-VN", "Văn học Việt Nam", 2018, 160, 65000,
                        "Các truyện ngắn hiện thực phê phán tiêu biểu của Nam Cao.",
                        tags("van-hoc", "truyen-ngan"), 3),
                rb("9786041234003", "Tắt đèn", null, "Ngô Tất Tố", "NXB Hội Nhà văn",
                        "VH-VN", "Văn học Việt Nam", 2017, 220, 72000,
                        "Hiện thực nông thôn Bắc Bộ trước Cách mạng tháng Tám.",
                        tags("van-hoc", "hien-thuc"), 2),
                rb("9786041234004", "Vợ nhặt", null, "Kim Lân", "NXB Kim Đồng",
                        "VH-VN", "Văn học Việt Nam", 2020, 96, 45000,
                        "Truyện ngắn về nạn đói 1945.",
                        tags("van-hoc", "kim-dong"), 4),
                rb("9786041234005", "Dế Mèn phiêu lưu ký", null, "Tô Hoài", "NXB Kim Đồng",
                        "VH-TN", "Thiếu nhi", 2021, 200, 55000,
                        "Tác phẩm thiếu nhi kinh điển của văn học Việt Nam.",
                        tags("thieu-nhi", "kinh-dien"), 5),
                rb("9786041234006", "Nhật ký trong tù", null, "Hồ Chí Minh", "NXB Chính trị Quốc gia",
                        "VH-VN", "Văn học Việt Nam", 2016, 312, 90000,
                        "Tập thơ chữ Hán viết trong thời gian bị giam giữ tại Trung Quốc.",
                        tags("tho", "lich-su"), 2),
                rb("9786041234007", "Truyện Kiều", null, "Nguyễn Du", "NXB Giáo dục Việt Nam",
                        "VH-VN", "Văn học Việt Nam", 2022, 360, 98000,
                        "Kiệt tác văn học cổ điển Việt Nam.",
                        tags("tho", "co-dien"), 4),
                rb("9786041234008", "Ông già và biển cả", "The Old Man and the Sea", "Ernest Hemingway", "NXB Văn học",
                        "VH-TG", "Văn học thế giới", 2019, 128, 68000,
                        "Tiểu thuyết ngắn đoạt giải Nobel của Hemingway.",
                        tags("van-hoc", "dich", "en"), 3),

                // CNTT / Khoa học máy tính
                rb("9786041234101", "Nhập môn lập trình Python", "Dành cho sinh viên năm nhất", "Nguyễn Thanh Tùng", "NXB Đại học Quốc gia Hà Nội",
                        "CNTT", "Công nghệ thông tin", 2023, 420, 145000,
                        "Giáo trình Python cơ bản đến ứng dụng: cấu trúc dữ liệu, file, OOP.",
                        tags("python", "lap-trinh", "giao-trinh"), 5),
                rb("9786041234102", "Cấu trúc dữ liệu và giải thuật", null, "Đỗ Xuân Lôi", "NXB Khoa học và Kỹ thuật",
                        "CNTT", "Công nghệ thông tin", 2021, 480, 165000,
                        "Các cấu trúc dữ liệu nền tảng và độ phức tạp thuật toán.",
                        tags("dsa", "giao-trinh"), 4),
                rb("9786041234103", "Cơ sở dữ liệu", null, "Nguyễn Kim Anh", "NXB Giáo dục Việt Nam",
                        "CNTT", "Công nghệ thông tin", 2020, 350, 135000,
                        "Mô hình quan hệ, SQL, chuẩn hóa, giao dịch.",
                        tags("database", "sql"), 4),
                rb("9786041234104", "Mạng máy tính", "Computer Networks", "Andrew S. Tanenbaum", "NXB Thông tin và Truyền thông",
                        "CNTT", "Công nghệ thông tin", 2019, 960, 320000,
                        "Giáo trình mạng máy tính kinh điển (bản dịch/tham khảo).",
                        tags("network", "en"), 3),
                rb("9786041234105", "Hệ điều hành", null, "Nguyễn Hồng Sơn", "NXB Bách khoa Hà Nội",
                        "CNTT", "Công nghệ thông tin", 2022, 390, 155000,
                        "Tiến trình, đồng bộ, quản lý bộ nhớ, file system.",
                        tags("os", "giao-trinh"), 3),
                rb("9786041234106", "Công nghệ phần mềm", null, "Roger S. Pressman", "NXB Lao động",
                        "CNTT", "Công nghệ thông tin", 2018, 720, 280000,
                        "Quy trình phát triển phần mềm, yêu cầu, thiết kế, kiểm thử.",
                        tags("se", "en"), 3),
                rb("9786041234107", "Trí tuệ nhân tạo", "Một cách tiếp cận hiện đại", "Stuart Russell", "NXB ĐHQG TP.HCM",
                        "CNTT", "Công nghệ thông tin", 2021, 1100, 450000,
                        "Giáo trình AI nền tảng (tham khảo bản Việt hóa).",
                        tags("ai", "en"), 2),
                rb("9786041234108", "An toàn thông tin", null, "Nguyễn Đức Thái", "NXB Thông tin và Truyền thông",
                        "CNTT", "Công nghệ thông tin", 2023, 310, 148000,
                        "Mật mã, xác thực, tấn mạng, chính sách ATTT.",
                        tags("security"), 3),
                rb("9786041234109", "Lập trình Java cơ bản", null, "Phạm Văn Ất", "NXB Giáo dục Việt Nam",
                        "CNTT", "Công nghệ thông tin", 2022, 400, 140000,
                        "Cú pháp Java, OOP, exception, collections.",
                        tags("java", "lap-trinh"), 5),
                rb("9786041234110", "Spring Boot in Action", null, "Craig Walls", "Manning / NXB TT&TT",
                        "CNTT", "Công nghệ thông tin", 2020, 380, 390000,
                        "Xây dựng ứng dụng web với Spring Boot (tài liệu tiếng Anh).",
                        tags("spring", "java", "en"), 2),

                // Toán / Khoa học cơ bản
                rb("9786041234201", "Giải tích 1", null, "Nguyễn Đình Trí", "NXB Giáo dục Việt Nam",
                        "TOAN", "Toán học", 2021, 450, 125000,
                        "Giới hạn, đạo hàm, tích phân một biến — giáo trình đại học.",
                        tags("toan", "giao-trinh"), 4),
                rb("9786041234202", "Đại số tuyến tính", null, "Nguyễn Hữu Việt Hưng", "NXB ĐHQG Hà Nội",
                        "TOAN", "Toán học", 2019, 380, 130000,
                        "Ma trận, không gian vector, ánh xạ tuyến tính.",
                        tags("toan", "dai-so"), 3),
                rb("9786041234203", "Xác suất thống kê", null, "Đào Hữu Hồ", "NXB ĐHQG Hà Nội",
                        "TOAN", "Toán học", 2020, 320, 118000,
                        "Xác suất, biến ngẫu nhiên, ước lượng, kiểm định giả thuyết.",
                        tags("toan", "thong-ke"), 3),
                rb("9786041234204", "Vật lý đại cương A1", null, "Lương Duyên Bình", "NXB Giáo dục Việt Nam",
                        "VL", "Vật lý", 2018, 360, 110000,
                        "Cơ học, nhiệt học dành cho khối kỹ thuật.",
                        tags("vat-ly"), 3),

                // Kinh tế / Quản trị
                rb("9786041234301", "Kinh tế vi mô", null, "Nguyễn Văn Công", "NXB Lao động – Xã hội",
                        "KT", "Kinh tế", 2021, 410, 142000,
                        "Cung cầu, đàn hồi, hành vi người tiêu dùng, thị trường.",
                        tags("kinh-te", "vi-mo"), 3),
                rb("9786041234302", "Kinh tế vĩ mô", null, "Vũ Kim Dũng", "NXB Thống kê",
                        "KT", "Kinh tế", 2020, 390, 138000,
                        "GDP, lạm phát, thất nghiệp, chính sách tài khóa – tiền tệ.",
                        tags("kinh-te", "vi-mo"), 3),
                rb("9786041234303", "Quản trị học", null, "Nguyễn Thị Liên Diệp", "NXB Lao động",
                        "QTKD", "Quản trị kinh doanh", 2022, 340, 125000,
                        "Chức năng quản trị: hoạch định, tổ chức, lãnh đạo, kiểm soát.",
                        tags("quan-tri"), 3),
                rb("9786041234304", "Marketing căn bản", null, "Philip Kotler", "NXB Lao động – Xã hội",
                        "QTKD", "Quản trị kinh doanh", 2019, 520, 210000,
                        "Nguyên lý marketing hiện đại (tham khảo).",
                        tags("marketing", "en"), 2),
                rb("9786041234305", "Kế toán tài chính", null, "Võ Văn Nhị", "NXB Tài chính",
                        "KT", "Kinh tế", 2023, 480, 160000,
                        "Hạch toán tài sản, nguồn vốn, báo cáo tài chính.",
                        tags("ke-toan"), 3),

                // Giao thông / Xây dựng (phù hợp UTC)
                rb("9786041234401", "Cơ học kết cấu", null, "Lê Ngọc Hồng", "NXB Xây dựng",
                        "XD", "Xây dựng", 2020, 520, 185000,
                        "Phân tích kết cấu thanh, khung, dầm liên tục.",
                        tags("xay-dung", "utc"), 3),
                rb("9786041234402", "Kết cấu bê tông cốt thép", null, "Phan Quang Minh", "NXB Xây dựng",
                        "XD", "Xây dựng", 2021, 560, 195000,
                        "Thiết kế cấu kiện BTCT theo TCVN.",
                        tags("xay-dung", "btct"), 3),
                rb("9786041234403", "Đường ô tô", "Tập 1", "Nguyễn Xuân Trục", "NXB Giao thông vận tải",
                        "GTVT", "Giao thông vận tải", 2019, 440, 175000,
                        "Khảo sát, thiết kế hình học đường ô tô.",
                        tags("giao-thong", "utc"), 3),
                rb("9786041234404", "Cầu thép", null, "Nguyễn Viết Trung", "NXB Giao thông vận tải",
                        "GTVT", "Giao thông vận tải", 2018, 380, 168000,
                        "Cấu tạo và tính toán cầu thép.",
                        tags("cau", "utc"), 2),
                rb("9786041234405", "Quy hoạch giao thông đô thị", null, "Trần Thị Mỹ Diệu", "NXB Xây dựng",
                        "GTVT", "Giao thông vận tải", 2022, 300, 155000,
                        "Nguyên lý quy hoạch vận tải đô thị bền vững.",
                        tags("quy-hoach", "utc"), 2),

                // Ngoại ngữ / Kỹ năng
                rb("9786041234501", "English Grammar in Use", "Intermediate", "Raymond Murphy", "Cambridge / NXB Trẻ",
                        "NN", "Ngoại ngữ", 2019, 380, 250000,
                        "Ngữ pháp tiếng Anh trung cấp (tài liệu tiếng Anh).",
                        tags("english", "en"), 4),
                rb("9786041234502", "Oxford Advanced Learner's Dictionary", "10th Edition", "A S Hornby", "Oxford / NXB Trẻ",
                        "NN", "Ngoại ngữ", 2020, 1900, 520000,
                        "Từ điển Anh – Anh dành cho người học nâng cao.",
                        tags("dictionary", "en"), 2),
                rb("9786041234503", "Kỹ năng mềm cho sinh viên", null, "Nguyễn Thị Mỹ Lộc", "NXB ĐHQG Hà Nội",
                        "KN", "Kỹ năng sống", 2021, 240, 89000,
                        "Giao tiếp, làm việc nhóm, quản lý thời gian, thuyết trình.",
                        tags("ky-nang"), 3),

                // Lịch sử / Xã hội
                rb("9786041234601", "Lịch sử Đảng Cộng sản Việt Nam", null, "Hội đồng Trung ương", "NXB Chính trị Quốc gia",
                        "LS", "Lịch sử", 2021, 520, 120000,
                        "Giáo trình lý luận chính trị đại học.",
                        tags("lich-su", "chinh-tri"), 3),
                rb("9786041234602", "Pháp luật đại cương", null, "Lê Minh Tâm", "NXB Công an nhân dân",
                        "PL", "Pháp luật", 2022, 360, 115000,
                        "Nhà nước và pháp luật, ngành luật cơ bản.",
                        tags("phap-luat"), 3),
                rb("9786041234603", "Triết học Mác – Lênin", null, "Bộ Giáo dục và Đào tạo", "NXB Chính trị Quốc gia",
                        "LLCT", "Lý luận chính trị", 2021, 400, 98000,
                        "Giáo trình bắt buộc khối đại học.",
                        tags("triet-hoc"), 4),

                // Sách thêm CNTT thực dụng
                rb("9786041234111", "Clean Code", "A Handbook of Agile Software Craftsmanship", "Robert C. Martin", "Prentice Hall / NXB TT&TT",
                        "CNTT", "Công nghệ thông tin", 2018, 464, 420000,
                        "Viết mã sạch, đặt tên, hàm, class, testing.",
                        tags("clean-code", "en"), 2),
                rb("9786041234112", "Design Patterns", "Elements of Reusable Object-Oriented Software", "Erich Gamma", "Addison-Wesley",
                        "CNTT", "Công nghệ thông tin", 2017, 395, 480000,
                        "23 mẫu thiết kế GoF kinh điển.",
                        tags("design-patterns", "en"), 2),
                rb("9786041234113", "Docker Deep Dive", null, "Nigel Poulton", "Independently published",
                        "CNTT", "Công nghệ thông tin", 2023, 350, 360000,
                        "Container, image, compose, orchestration cơ bản.",
                        tags("docker", "devops", "en"), 2),
                rb("9786041234114", "React nhanh và thực chiến", null, "Nguyễn Văn Dương", "NXB Thanh niên",
                        "CNTT", "Công nghệ thông tin", 2024, 280, 175000,
                        "Hooks, router, gọi API, triển khai frontend hiện đại.",
                        tags("react", "frontend"), 3),
                rb("9786041234115", "PostgreSQL thực hành", null, "Trần Minh Tuấn", "NXB Thông tin và Truyền thông",
                        "CNTT", "Công nghệ thông tin", 2023, 310, 168000,
                        "Cài đặt, SQL nâng cao, index, backup, tối ưu truy vấn.",
                        tags("postgresql", "database"), 3)
        );
    }

    private static RealBook rb(
            String isbn, String title, String subtitle, String author, String publisher,
            String catCode, String catName, int year, int pages, long price,
            String desc, String[] tags, int copies
    ) {
        return new RealBook(isbn, title, subtitle, author, publisher, catCode, catName, year, pages, price, desc, tags, copies);
    }

    private static String[] tags(String... t) {
        return t;
    }
}
