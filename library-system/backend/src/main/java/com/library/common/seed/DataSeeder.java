package com.library.common.seed;

import com.library.borrow.entity.BorrowSlip;
import com.library.borrow.entity.BorrowSlipItem;
import com.library.borrow.entity.Reservation;
import com.library.borrow.repository.BorrowSlipItemRepository;
import com.library.borrow.repository.BorrowSlipRepository;
import com.library.borrow.repository.ReservationRepository;
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
import com.library.cms.entity.Banner;
import com.library.cms.entity.News;
import com.library.cms.repository.BannerRepository;
import com.library.cms.repository.NewsRepository;
import com.library.common.enums.BookAuthorRole;
import com.library.common.enums.BookStatus;
import com.library.common.enums.BorrowChannel;
import com.library.common.enums.BorrowItemStatus;
import com.library.common.enums.BorrowSlipStatus;
import com.library.common.enums.CardStatus;
import com.library.common.enums.CopyCondition;
import com.library.common.enums.CopyStatus;
import com.library.common.enums.FineStatus;
import com.library.common.enums.FineType;
import com.library.common.enums.NewsStatus;
import com.library.common.enums.ReservationStatus;
import com.library.common.enums.UserRole;
import com.library.common.enums.UserStatus;
import com.library.fine.entity.Fine;
import com.library.fine.repository.FineRepository;
import com.library.notification.entity.NotificationTemplate;
import com.library.notification.repository.NotificationTemplateRepository;
import com.library.reader.entity.LibraryCard;
import com.library.reader.entity.Reader;
import com.library.reader.entity.ReaderType;
import com.library.reader.repository.LibraryCardRepository;
import com.library.reader.repository.ReaderRepository;
import com.library.reader.repository.ReaderTypeRepository;
import com.library.settings.entity.Holiday;
import com.library.settings.entity.Setting;
import com.library.settings.repository.HolidayRepository;
import com.library.settings.repository.SettingRepository;
import com.library.user.entity.StaffProfile;
import com.library.user.entity.User;
import com.library.user.repository.StaffProfileRepository;
import com.library.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String ADMIN_EMAIL = "admin@lib.vn";

    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final ReaderTypeRepository readerTypeRepository;
    private final ReaderRepository readerRepository;
    private final LibraryCardRepository libraryCardRepository;
    private final SettingRepository settingRepository;
    private final HolidayRepository holidayRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
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
    private final BorrowSlipRepository borrowSlipRepository;
    private final BorrowSlipItemRepository borrowSlipItemRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;
    private final NewsRepository newsRepository;
    private final BannerRepository bannerRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmailIgnoreCase(ADMIN_EMAIL).isPresent()) {
            log.info("DataSeeder: admin@lib.vn đã tồn tại — bỏ qua seed.");
            return;
        }
        log.info("DataSeeder: bắt đầu seed dữ liệu mẫu...");

        seedSettings();
        List<ReaderType> readerTypes = seedReaderTypes();
        seedHolidays();
        seedNotificationTemplates();

        User admin = seedUser(ADMIN_EMAIL, "0901000001", "Quản trị viên", UserRole.ADMIN, UserStatus.ACTIVE, "Admin@123");
        User staff1 = seedUser("staff1@lib.vn", "0901000002", "Nhân viên Một", UserRole.STAFF, UserStatus.ACTIVE, "Staff@123");
        User staff2 = seedUser("staff2@lib.vn", "0901000003", "Nhân viên Hai", UserRole.STAFF, UserStatus.ACTIVE, "Staff@123");
        seedStaffProfile(staff1, "NV001", "Thủ thư");
        seedStaffProfile(staff2, "NV002", "Thủ thư");

        List<Reader> readers = seedReaders(readerTypes);
        Language vi = seedLanguage("vi", "Tiếng Việt");
        seedLanguage("en", "English");
        DocumentType bookType = seedDocumentType("BOOK", "Sách");
        List<Category> categories = seedCategories();
        List<Author> authors = seedAuthors();
        List<Publisher> publishers = seedPublishers();
        List<Location> locations = seedLocations();
        List<Book> books = seedBooks(vi, bookType, publishers, authors, categories, locations);
        seedBorrowsReservationsFines(readers, books, admin);
        seedNewsAndBanners(admin);

        log.info("DataSeeder: hoàn tất (users={}, books={}, copies={}).",
                userRepository.count(), bookRepository.count(), bookCopyRepository.count());
    }

    private void seedSettings() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("library_name", "Thư viện UTC");
        defaults.put("address", "Số 3 Cầu Giấy, Hà Nội");
        defaults.put("phone", "024-3766-XXXX");
        defaults.put("email", "lib@utc.edu.vn");
        defaults.put("open_hours", "07:30-17:00");
        defaults.put("weekend_days", List.of("SUNDAY"));
        defaults.put("hold_hours", 48);
        defaults.put("overdue_fee_per_day", 2000);
        defaults.put("grace_days", 0);
        defaults.put("max_unpaid_fine", 50000);
        defaults.put("block_borrow_if_overdue", true);
        defaults.put("due_soon_reminder_days", 2);
        defaults.put("overdue_reminder_interval_days", 3);
        defaults.put("lost_book_multiplier", 2);
        defaults.put("lost_processing_fee", 20000);
        defaults.put("no_show_limit", 3);
        defaults.put("allow_online_borrow", true);
        defaults.put("allow_self_renew", true);
        defaults.put("default_currency", "VND");
        defaults.put("date_format", "DD/MM/YYYY");
        defaults.put("fine_cap_by_price", true);
        defaults.put("skip_holidays_in_fine", false);
        defaults.put("damaged_minor_percent", 20);
        defaults.put("damaged_major_percent", 60);
        defaults.put("card_validity_months", 12);

        for (Map.Entry<String, Object> e : defaults.entrySet()) {
            if (!settingRepository.existsByKey(e.getKey())) {
                Setting s = new Setting();
                s.setKey(e.getKey());
                s.setValue(objectMapper.valueToTree(e.getValue()));
                s.setDescription("Mặc định SPEC 5.9: " + e.getKey());
                settingRepository.save(s);
            }
        }
    }

    private List<ReaderType> seedReaderTypes() {
        Object[][] rows = {
                {"STUDENT", "Sinh viên", 5, 14, 2, 7, 3, 20000, 12},
                {"PUPIL", "Học sinh", 3, 14, 1, 7, 2, 10000, 12},
                {"TEACHER", "Giảng viên/Cán bộ", 10, 30, 3, 14, 5, 0, 24},
                {"PUBLIC", "Khách vãng lai/Cộng đồng", 3, 10, 1, 5, 2, 50000, 12},
                {"VIP", "VIP/Thành viên đặc biệt", 15, 45, 3, 15, 8, 100000, 24}
        };
        List<ReaderType> list = new ArrayList<>();
        for (Object[] r : rows) {
            ReaderType type = readerTypeRepository.findByCode((String) r[0]).orElseGet(ReaderType::new);
            type.setCode((String) r[0]);
            type.setName((String) r[1]);
            type.setMaxBooks((Integer) r[2]);
            type.setLoanDays((Integer) r[3]);
            type.setMaxRenewals((Integer) r[4]);
            type.setRenewDays((Integer) r[5]);
            type.setMaxReservations((Integer) r[6]);
            type.setCardFee(BigDecimal.valueOf((Integer) r[7]));
            type.setCardValidityMonths((Integer) r[8]);
            type.setActive(true);
            list.add(readerTypeRepository.save(type));
        }
        return list;
    }

    private void seedHolidays() {
        int year = LocalDate.now(VN).getYear();
        List<Object[]> holidays = List.of(
                new Object[]{LocalDate.of(year, 1, 1), "Tết Dương lịch", true},
                new Object[]{LocalDate.of(year, 1, 28), "Tết Nguyên đán (ước lượng)", false},
                new Object[]{LocalDate.of(year, 1, 29), "Tết Nguyên đán (ước lượng)", false},
                new Object[]{LocalDate.of(year, 1, 30), "Tết Nguyên đán (ước lượng)", false},
                new Object[]{LocalDate.of(year, 4, 30), "Ngày Giải phóng miền Nam", true},
                new Object[]{LocalDate.of(year, 5, 1), "Ngày Quốc tế Lao động", true},
                new Object[]{LocalDate.of(year, 9, 2), "Quốc khánh", true},
                new Object[]{LocalDate.of(year, 4, 18), "Giỗ Tổ Hùng Vương (ước lượng dương lịch)", false}
        );
        for (Object[] h : holidays) {
            Holiday holiday = new Holiday();
            holiday.setDate((LocalDate) h[0]);
            holiday.setName((String) h[1]);
            holiday.setRecurring((Boolean) h[2]);
            holidayRepository.save(holiday);
        }
    }

    private void seedNotificationTemplates() {
        seedTemplate("BORROW_APPROVED", "Yêu cầu mượn được duyệt", "Phiếu {{slipCode}} đã được duyệt. Vui lòng đến lấy trước {{deadline}}.", "EMAIL");
        seedTemplate("DUE_SOON", "Sắp đến hạn trả sách", "Sách {{title}} sẽ đến hạn vào {{dueDate}}.", "EMAIL");
        seedTemplate("OVERDUE", "Sách quá hạn", "Sách {{title}} đã quá hạn {{days}} ngày.", "EMAIL");
        seedTemplate("RESERVATION_READY", "Sách đặt trước sẵn sàng", "Sách {{title}} đã sẵn sàng đến {{readyUntil}}.", "EMAIL");
        seedTemplate("PASSWORD_RESET", "Đặt lại mật khẩu", "Mã đặt lại mật khẩu: {{token}}", "EMAIL");
    }

    private void seedTemplate(String code, String subject, String body, String channel) {
        if (notificationTemplateRepository.existsByCode(code)) {
            return;
        }
        NotificationTemplate t = new NotificationTemplate();
        t.setCode(code);
        t.setSubject(subject);
        t.setBody(body);
        t.setChannel(channel);
        t.setActive(true);
        notificationTemplateRepository.save(t);
    }

    private User seedUser(String email, String phone, String fullName, UserRole role, UserStatus status, String rawPassword) {
        User user = new User();
        user.setEmail(email);
        user.setPhone(phone);
        user.setFullName(fullName);
        user.setRole(role);
        user.setStatus(status);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setEmailVerifiedAt(Instant.now());
        user.setFailedLoginCount(0);
        return userRepository.save(user);
    }

    private void seedStaffProfile(User user, String code, String position) {
        StaffProfile profile = new StaffProfile();
        profile.setUser(user);
        profile.setStaffCode(code);
        profile.setPosition(position);
        profile.setHireDate(LocalDate.now(VN).minusYears(1));
        profile.setShift("Sáng");
        staffProfileRepository.save(profile);
    }

    private List<Reader> seedReaders(List<ReaderType> types) {
        List<Reader> readers = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            ReaderType type = types.get((i - 1) % types.size());
            UserStatus status = (i == 7) ? UserStatus.LOCKED : UserStatus.ACTIVE;
            User user = seedUser(
                    "reader" + i + "@lib.vn",
                    String.format("0902%06d", i),
                    "Độc giả " + i,
                    UserRole.USER,
                    status,
                    "User@123"
            );
            if (status == UserStatus.LOCKED) {
                user.setLockedUntil(null);
                userRepository.save(user);
            }

            Reader reader = new Reader();
            reader.setUser(user);
            reader.setReaderCode(String.format("RD-%06d", i));
            reader.setReaderType(type);
            reader.setDob(LocalDate.of(1995 + (i % 10), (i % 12) + 1, (i % 27) + 1));
            reader.setGender(i % 2 == 0 ? "FEMALE" : "MALE");
            reader.setAddress("Hà Nội");
            reader.setIdNumber(String.format("001199%06d", i));
            reader.setStudentCode(type.getCode().equals("STUDENT") ? "SV" + i : null);
            reader.setRegisteredAt(Instant.now().minus(30L * i, ChronoUnit.DAYS));
            reader = readerRepository.save(reader);

            LibraryCard card = new LibraryCard();
            card.setReader(reader);
            card.setCardNumber(String.format("TV-%d-%06d", LocalDate.now(VN).getYear(), i));
            card.setBarcode("CARD-" + i);
            card.setIssuedAt(Instant.now().minus(20L * i, ChronoUnit.DAYS));
            if (i == 5) {
                card.setStatus(CardStatus.EXPIRED);
                card.setExpiresAt(Instant.now().minus(10, ChronoUnit.DAYS));
            } else {
                card.setStatus(CardStatus.ACTIVE);
                card.setExpiresAt(Instant.now().plus(365, ChronoUnit.DAYS));
            }
            card.setFeePaid(type.getCardFee());
            libraryCardRepository.save(card);
            readers.add(reader);
        }
        return readers;
    }

    private Language seedLanguage(String code, String name) {
        return languageRepository.findByCode(code).orElseGet(() -> {
            Language language = new Language();
            language.setCode(code);
            language.setName(name);
            return languageRepository.save(language);
        });
    }

    private DocumentType seedDocumentType(String code, String name) {
        return documentTypeRepository.findByCode(code).orElseGet(() -> {
            DocumentType type = new DocumentType();
            type.setCode(code);
            type.setName(name);
            return documentTypeRepository.save(type);
        });
    }

    private List<Category> seedCategories() {
        String[] roots = {"Khoa học", "Công nghệ", "Văn học", "Kinh tế", "Lịch sử"};
        List<Category> all = new ArrayList<>();
        int sort = 1;
        for (String rootName : roots) {
            Category root = new Category();
            root.setCode("CAT-" + sort);
            root.setName(rootName);
            root.setSortOrder(sort++);
            root = categoryRepository.save(root);
            all.add(root);
            for (int c = 1; c <= 2; c++) {
                Category child = new Category();
                child.setParent(root);
                child.setCode(root.getCode() + "-" + c);
                child.setName(rootName + " - Nhóm " + c);
                child.setSortOrder(c);
                child = categoryRepository.save(child);
                all.add(child);
            }
        }
        return all;
    }

    private List<Author> seedAuthors() {
        List<Author> authors = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            Author author = new Author();
            author.setName("Tác giả " + i);
            author.setNationality(i % 2 == 0 ? "Việt Nam" : "Quốc tế");
            author.setBirthYear(1950 + (i % 40));
            authors.add(authorRepository.save(author));
        }
        return authors;
    }

    private List<Publisher> seedPublishers() {
        List<Publisher> publishers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Publisher publisher = new Publisher();
            publisher.setName("NXB Mẫu " + i);
            publisher.setAddress("Hà Nội");
            publishers.add(publisherRepository.save(publisher));
        }
        return publishers;
    }

    private List<Location> seedLocations() {
        List<Location> locations = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            Location location = new Location();
            location.setCode("LOC-" + String.format("%02d", i));
            location.setZone("Khu " + ((i - 1) / 4 + 1));
            location.setFloor(String.valueOf((i - 1) % 3 + 1));
            location.setRow("R" + i);
            location.setShelf("S" + i);
            location.setCapacity(100);
            locations.add(locationRepository.save(location));
        }
        return locations;
    }

    private List<Book> seedBooks(Language language, DocumentType documentType,
                                 List<Publisher> publishers, List<Author> authors,
                                 List<Category> categories, List<Location> locations) {
        List<Book> books = new ArrayList<>();
        int copySeq = 1;
        for (int i = 1; i <= 200; i++) {
            Book book = new Book();
            book.setIsbn13(String.format("978604%07d", i));
            book.setTitle("Sách mẫu " + i);
            book.setPublisher(publishers.get((i - 1) % publishers.size()));
            book.setPublishYear(2000 + (i % 25));
            book.setLanguage(language);
            book.setDocumentType(documentType);
            book.setPages(100 + i);
            book.setPrice(BigDecimal.valueOf(50000 + i * 1000L));
            book.setDescription("Mô tả sách mẫu " + i);
            book.setCoverUrl("https://placehold.co/200x300?text=Book+" + i);
            book.setTags(List.of("seed", "mau", "book-" + i));
            book.setReference(false);
            book.setLoanable(true);
            book.setStatus(BookStatus.ACTIVE);
            book = bookRepository.save(book);

            BookAuthor ba = new BookAuthor();
            ba.setBookId(book.getId());
            ba.setAuthorId(authors.get((i - 1) % authors.size()).getId());
            ba.setRole(BookAuthorRole.AUTHOR);
            ba.setSortOrder(0);
            bookAuthorRepository.save(ba);

            BookCategory bc = new BookCategory();
            bc.setBookId(book.getId());
            bc.setCategoryId(categories.get((i - 1) % categories.size()).getId());
            bookCategoryRepository.save(bc);

            int copiesPerBook = (i <= 100) ? 3 : 2;
            for (int c = 1; c <= copiesPerBook; c++) {
                BookCopy copy = new BookCopy();
                copy.setBook(book);
                copy.setBarcode(String.format("BC-%05d-%02d", i, c));
                copy.setCopyNo(c);
                copy.setLocation(locations.get((copySeq - 1) % locations.size()));
                copy.setStatus(CopyStatus.AVAILABLE);
                copy.setCondition(CopyCondition.GOOD);
                copy.setPurchasePrice(book.getPrice());
                copy.setAcquiredAt(Instant.now().minus(i, ChronoUnit.DAYS));
                bookCopyRepository.save(copy);
                copySeq++;
            }
            books.add(book);
        }
        return books;
    }

    private void seedBorrowsReservationsFines(List<Reader> readers, List<Book> books, User staff) {
        BorrowSlipStatus[] statuses = {
                BorrowSlipStatus.PENDING,
                BorrowSlipStatus.APPROVED,
                BorrowSlipStatus.BORROWING,
                BorrowSlipStatus.RETURNED,
                BorrowSlipStatus.REJECTED
        };
        for (int i = 0; i < statuses.length; i++) {
            Reader reader = readers.get(i);
            Book book = books.get(i);
            List<BookCopy> copies = bookCopyRepository.findByBook_Id(book.getId());
            BookCopy copy = copies.isEmpty() ? null : copies.get(0);

            BorrowSlip slip = new BorrowSlip();
            slip.setSlipCode(String.format("PM-%06d", i + 1));
            slip.setReader(reader);
            slip.setChannel(i % 2 == 0 ? BorrowChannel.COUNTER : BorrowChannel.ONLINE);
            slip.setStatus(statuses[i]);
            slip.setRequestedAt(Instant.now().minus(5, ChronoUnit.DAYS));
            if (statuses[i] == BorrowSlipStatus.APPROVED || statuses[i] == BorrowSlipStatus.BORROWING
                    || statuses[i] == BorrowSlipStatus.RETURNED) {
                slip.setApprovedAt(Instant.now().minus(4, ChronoUnit.DAYS));
                slip.setApprovedBy(staff);
            }
            if (statuses[i] == BorrowSlipStatus.BORROWING || statuses[i] == BorrowSlipStatus.RETURNED) {
                slip.setBorrowedAt(Instant.now().minus(3, ChronoUnit.DAYS));
                slip.setIssuedBy(staff);
            }
            if (statuses[i] == BorrowSlipStatus.REJECTED) {
                slip.setRejectedReason("Không đủ điều kiện");
            }
            slip = borrowSlipRepository.save(slip);

            BorrowSlipItem item = new BorrowSlipItem();
            item.setSlip(slip);
            item.setBook(book);
            item.setCopy(copy);
            if (statuses[i] == BorrowSlipStatus.BORROWING) {
                item.setStatus(BorrowItemStatus.BORROWING);
                item.setBorrowedAt(slip.getBorrowedAt());
                item.setDueDate(LocalDate.now(VN).plusDays(7));
                if (copy != null) {
                    copy.setStatus(CopyStatus.BORROWED);
                    bookCopyRepository.save(copy);
                }
            } else if (statuses[i] == BorrowSlipStatus.RETURNED) {
                item.setStatus(BorrowItemStatus.RETURNED);
                item.setBorrowedAt(slip.getBorrowedAt());
                item.setDueDate(LocalDate.now(VN).minusDays(1));
                item.setReturnedAt(Instant.now().minus(1, ChronoUnit.DAYS));
                item.setReturnedBy(staff);
            } else if (statuses[i] == BorrowSlipStatus.APPROVED) {
                item.setStatus(BorrowItemStatus.HOLD);
                if (copy != null) {
                    copy.setStatus(CopyStatus.HOLD);
                    bookCopyRepository.save(copy);
                }
            } else if (statuses[i] == BorrowSlipStatus.REJECTED) {
                item.setStatus(BorrowItemStatus.CANCELLED);
            } else {
                item.setStatus(BorrowItemStatus.REQUESTED);
            }
            item = borrowSlipItemRepository.save(item);

            if (i == 2) {
                Fine fine = new Fine();
                fine.setReader(reader);
                fine.setSlipItem(item);
                fine.setType(FineType.OVERDUE);
                fine.setAmount(BigDecimal.valueOf(10000));
                fine.setPaidAmount(BigDecimal.ZERO);
                fine.setStatus(FineStatus.UNPAID);
                fine.setOverdueDays(5);
                fine.setReason("Seed phạt quá hạn");
                fineRepository.save(fine);
            }
        }

        Reservation reservation = new Reservation();
        reservation.setReader(readers.get(10));
        reservation.setBook(books.get(50));
        reservation.setStatus(ReservationStatus.WAITING);
        reservation.setQueuePosition(1);
        reservationRepository.save(reservation);

        Reservation ready = new Reservation();
        ready.setReader(readers.get(11));
        ready.setBook(books.get(51));
        ready.setStatus(ReservationStatus.READY);
        ready.setQueuePosition(1);
        ready.setReadyAt(Instant.now());
        ready.setReadyUntil(Instant.now().plus(2, ChronoUnit.DAYS));
        reservationRepository.save(ready);
    }

    private void seedNewsAndBanners(User admin) {
        for (int i = 1; i <= 5; i++) {
            News news = new News();
            news.setTitle("Tin tức thư viện " + i);
            news.setSlug("tin-tuc-thu-vien-" + i);
            news.setCoverUrl("https://placehold.co/800x400?text=News+" + i);
            news.setContent("Nội dung tin tức mẫu số " + i);
            news.setStatus(NewsStatus.PUBLISHED);
            news.setPinned(i == 1);
            news.setPublishedAt(Instant.now().minus(i, ChronoUnit.DAYS));
            news.setAuthor(admin);
            newsRepository.save(news);
        }
        for (int i = 1; i <= 3; i++) {
            Banner banner = new Banner();
            banner.setImageUrl("https://placehold.co/1200x300?text=Banner+" + i);
            banner.setLink("/news");
            banner.setSortOrder(i);
            banner.setActive(true);
            bannerRepository.save(banner);
        }
    }
}
