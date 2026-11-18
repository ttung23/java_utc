import { useEffect, useMemo, useState } from 'react';
import {
  ArrowRightOutlined,
  CalendarOutlined,
  CheckCircleFilled,
  ClockCircleOutlined,
  DatabaseOutlined,
  EyeOutlined,
  PhoneOutlined,
  ReadOutlined,
  RightOutlined,
  ShoppingCartOutlined,
  TeamOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { Button, Empty, Select, Skeleton, Tag, message } from 'antd';
import { Link, useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import { useAuth } from '@/auth/AuthContext';
import { api, type ApiResponse } from '@/services/api';

type Book = {
  id: number;
  title: string;
  coverUrl?: string;
  publisherName?: string;
  publishYear?: number;
  availableCopies?: number;
  totalCopies?: number;
  avgRating?: number;
};

type News = {
  id: number;
  title: string;
  slug?: string;
  publishedAt?: string;
  content?: string;
  coverUrl?: string;
};

type Category = {
  id: number;
  name: string;
  code?: string;
  description?: string;
};

type Slip = {
  id: number;
  status: string;
  items?: Array<{ dueDate?: string; status: string }>;
};

type Fine = { id: number; amount?: number; status?: string };
type Reservation = { id: number; status: string };

const KEYWORDS = [
  'Data Science',
  'Trí tuệ nhân tạo',
  'Kinh tế lượng',
  'Luật thương mại',
  'Giáo trình Toán',
];

const CATEGORY_STYLES = [
  { bg: '#0B1F3A', tags: ['Lập trình', 'AI', 'An ninh mạng'] },
  { bg: '#1D4ED8', tags: ['Tài chính', 'Quản trị', 'Marketing'] },
  { bg: '#059669', tags: ['IELTS', 'Soft skills', 'Communication'] },
  { bg: '#0369A1', tags: ['Luật', 'Xã hội', 'Chính trị'] },
  { bg: '#111827', tags: ['Luận văn', 'Nghiên cứu', 'Khóa luận'] },
  { bg: '#7C3AED', tags: ['Toán', 'Vật lý', 'Hóa học'] },
];

const BADGES = ['MỚI VỀ', 'BESTSELLER', 'BẢN 2024', 'GIÁO TRÌNH', 'HOT PICK'];

const EVENTS = [
  {
    month: 'THÁNG 11',
    day: '02',
    title: 'Workshop tra cứu IEEE / Scopus & quản lý trích dẫn',
    place: 'Phòng Lab A2 · 14:00–16:30',
    cta: 'Đăng ký tham gia (Miễn phí)',
    primary: true,
  },
  {
    month: 'THÁNG 11',
    day: '15',
    title: 'Book Exchange Week & Cultural Reading Seminar',
    place: 'Sảnh Thư viện CS1 · Cả ngày',
    cta: 'Xem chi tiết chương trình',
    primary: false,
  },
];

function formatVnd(n: number) {
  return `${Math.round(n).toLocaleString('vi-VN')}đ`;
}

function coverFallback(title: string) {
  return title.length > 42 ? `${title.slice(0, 40)}…` : title;
}

export function HomePage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [q, setQ] = useState('');
  const [docType, setDocType] = useState('all');
  const [loading, setLoading] = useState(true);
  const [newBooks, setNewBooks] = useState<Book[]>([]);
  const [popular, setPopular] = useState<Book[]>([]);
  const [news, setNews] = useState<News[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [activeBorrow, setActiveBorrow] = useState(0);
  const [nearestDue, setNearestDue] = useState<string | null>(null);
  const [fineDebt, setFineDebt] = useState(0);
  const [readyReservations, setReadyReservations] = useState(0);
  const [popularTab, setPopularTab] = useState<'all' | 'it' | 'eco'>('all');

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const [n, p, newsRes, cats] = await Promise.all([
          api.get<ApiResponse<Book[]>>('/books/new'),
          api.get<ApiResponse<Book[]>>('/books/popular'),
          api.get<ApiResponse<News[]>>('/news/public'),
          api.get<ApiResponse<Category[]>>('/categories'),
        ]);
        setNewBooks(n.data.data || []);
        setPopular(p.data.data || []);
        setNews(newsRes.data.data || []);
        setCategories((cats.data.data || []).slice(0, 6));
      } catch {
        message.error('Không tải được trang chủ');
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, []);

  useEffect(() => {
    if (!user) {
      setActiveBorrow(0);
      setNearestDue(null);
      setFineDebt(0);
      setReadyReservations(0);
      return;
    }
    const loadMe = async () => {
      try {
        const [borrows, fines, reservations] = await Promise.all([
          api.get<ApiResponse<Slip[]>>('/borrows/me', { params: { page: 0, size: 50 } }),
          api.get<ApiResponse<Fine[]>>('/fines/me', { params: { page: 0, size: 50 } }),
          api.get<ApiResponse<Reservation[]>>('/reservations/me', { params: { page: 0, size: 50 } }),
        ]);
        const slips = borrows.data.data || [];
        const active = slips.filter((s) =>
          ['ACTIVE', 'OVERDUE', 'APPROVED', 'PENDING', 'READY'].includes(String(s.status).toUpperCase()),
        );
        setActiveBorrow(active.length);
        const dues = active
          .flatMap((s) => s.items || [])
          .map((i) => i.dueDate)
          .filter(Boolean)
          .sort();
        setNearestDue(dues[0] || null);

        const unpaid = (fines.data.data || []).filter((f) =>
          ['UNPAID', 'PENDING', 'OPEN'].includes(String(f.status || 'UNPAID').toUpperCase()),
        );
        setFineDebt(unpaid.reduce((sum, f) => sum + Number(f.amount || 0), 0));

        setReadyReservations(
          (reservations.data.data || []).filter((r) =>
            ['READY', 'AVAILABLE'].includes(String(r.status).toUpperCase()),
          ).length,
        );
      } catch {
        // Không chặn trang chủ nếu API cá nhân lỗi
      }
    };
    void loadMe();
  }, [user]);

  const dueHint = useMemo(() => {
    if (!nearestDue) return 'Chưa có hạn trả';
    const d = dayjs(nearestDue);
    const left = d.diff(dayjs(), 'day');
    if (left < 0) return `Quá hạn ${Math.abs(left)} ngày`;
    return `Còn ${left} ngày`;
  }, [nearestDue]);

  const onSearch = () => {
    const params = new URLSearchParams();
    if (q.trim()) params.set('q', q.trim());
    if (docType !== 'all') params.set('type', docType);
    navigate(`/books${params.toString() ? `?${params}` : ''}`);
  };

  const borrowOnline = async (bookId: number) => {
    if (!user) {
      message.warning('Đăng nhập để mượn sách');
      navigate('/login');
      return;
    }
    try {
      await api.post('/borrows/online', { items: [{ bookId }] });
      message.success('Đã gửi yêu cầu mượn online');
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Không gửi được yêu cầu mượn');
    }
  };

  const reserve = async (bookId: number) => {
    if (!user) {
      message.warning('Đăng nhập để đặt trước');
      navigate('/login');
      return;
    }
    try {
      await api.post('/reservations', { bookId });
      message.success('Đã đặt trước thành công');
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Đặt trước thất bại');
    }
  };

  const popularList = popular.slice(0, 4);

  return (
    <div>
      {/* Hero */}
      <section className="home-hero">
        <div className="home-pill">CỔNG TRA CỨU TRỰC TUYẾN OPAC V2.4</div>
        <h1>
          Khám Phá Tri Thức
          <br />&amp; Kho Học Liệu Số
        </h1>
        <p className="sub">
          Hơn 14.800 đầu sách, giáo trình và tài liệu nghiên cứu sẵn sàng phục vụ giảng dạy – học tập
          và nghiên cứu khoa học.
        </p>

        <div className="home-search">
          <Select
            value={docType}
            onChange={setDocType}
            options={[
              { value: 'all', label: 'Tất cả tài liệu' },
              { value: 'book', label: 'Sách in' },
              { value: 'ebook', label: 'Tài liệu số' },
              { value: 'thesis', label: 'Luận văn' },
            ]}
            variant="borderless"
          />
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && onSearch()}
            placeholder="Nhập tên sách, ISBN, tác giả, từ khóa…"
            style={{
              border: 0,
              outline: 'none',
              fontSize: 15,
              padding: '10px 8px',
              width: '100%',
              background: 'transparent',
            }}
          />
          <Button type="primary" size="large" onClick={onSearch} style={{ background: '#0B1F3A' }}>
            Tra Cứu <ArrowRightOutlined />
          </Button>
        </div>

        <div className="home-keywords">
          {KEYWORDS.map((k) => (
            <button key={k} type="button" onClick={() => navigate(`/books?q=${encodeURIComponent(k)}`)}>
              {k}
            </button>
          ))}
        </div>

        <div className="home-quick">
          <div className="home-quick-card">
            <div className="label">Phân loại</div>
            <div className="value">Tra cứu mã DDC</div>
          </div>
          <div className="home-quick-card">
            <div className="label">Lọc nhanh</div>
            <div className="value">
              <CheckCircleFilled style={{ color: '#16a34a' }} /> Sách đang có sẵn
            </div>
          </div>
          <div className="home-quick-card">
            <div className="label">Quy chế</div>
            <div className="value">Chỉ đọc tại chỗ</div>
          </div>
          <div className="home-quick-card">
            <div className="label">Thời gian làm việc</div>
            <div className="value">
              <ClockCircleOutlined /> 07:30 – 20:30
            </div>
          </div>
        </div>
      </section>

      {/* Reader dashboard */}
      <section className="home-reader">
        {user ? (
          <>
            <div>
              <div className="name-row">
                <strong>{user.fullName}</strong>
                <Tag color="blue">{user.email.split('@')[0].toUpperCase()}</Tag>
                <Tag color="success">Thẻ Hợp Lệ</Tag>
              </div>
              <div className="hint">Độc giả · Hạn mức mượn theo loại thẻ hiện hành</div>
            </div>
            <div className="home-reader-stats">
              <div className="stat">
                <div className="k">ĐANG MƯỢN</div>
                <div className="v">{activeBorrow}/5</div>
                <div className="s">phiếu / hạn mức</div>
              </div>
              <div className="stat">
                <div className="k">HẠN TRẢ GẦN NHẤT</div>
                <div className="v">{nearestDue ? dayjs(nearestDue).format('DD/MM') : '—'}</div>
                <div className="s">{dueHint}</div>
              </div>
              <div className="stat">
                <div className="k">TIỀN PHẠT NỢ</div>
                <div className="v" style={{ fontSize: 16 }}>
                  {formatVnd(fineDebt)}
                </div>
                <div className="s">{fineDebt > 0 ? 'Cần thanh toán' : 'Không nợ'}</div>
              </div>
              <div className="stat">
                <div className="k">ĐẶT TRƯỚC SẴN SÀNG</div>
                <div className="v">{readyReservations}</div>
                <div className="s">nhận tại quầy</div>
              </div>
            </div>
            <div className="home-reader-actions">
              <Button onClick={() => navigate('/me/borrows')}>Gia Hạn Nhanh</Button>
              <Button type="primary" icon={<ShoppingCartOutlined />} onClick={() => navigate('/me/borrows')}>
                Giỏ Mượn
              </Button>
            </div>
          </>
        ) : (
          <>
            <div>
              <div className="name-row">
                <strong>Chào mừng độc giả</strong>
                <Tag>Khách</Tag>
              </div>
              <div className="hint">Đăng nhập để xem hạn mức mượn, phạt và đặt trước của bạn.</div>
            </div>
            <div className="home-reader-stats">
              <div className="stat">
                <div className="k">OPAC</div>
                <div className="v" style={{ fontSize: 16 }}>
                  Online
                </div>
                <div className="s">Tra cứu 24/7</div>
              </div>
              <div className="stat">
                <div className="k">MƯỢN ONLINE</div>
                <div className="v" style={{ fontSize: 16 }}>
                  Có
                </div>
                <div className="s">Sau khi đăng nhập</div>
              </div>
              <div className="stat">
                <div className="k">GIỜ MỞ CỬA</div>
                <div className="v" style={{ fontSize: 16 }}>
                  07:30
                </div>
                <div className="s">đến 20:30</div>
              </div>
              <div className="stat">
                <div className="k">HỖ TRỢ</div>
                <div className="v" style={{ fontSize: 16 }}>
                  Thủ thư
                </div>
                <div className="s">Chat / Hotline</div>
              </div>
            </div>
            <div className="home-reader-actions">
              <Button onClick={() => navigate('/register')}>Đăng ký thẻ</Button>
              <Button type="primary" onClick={() => navigate('/login')}>
                Đăng nhập
              </Button>
            </div>
          </>
        )}
      </section>

      {/* Categories */}
      <section className="home-section">
        <div className="home-section-head">
          <div>
            <div className="eyebrow">PHÂN LOẠI TRI THỨC</div>
            <h2>Thể Loại &amp; Bộ Sưu Tập Nổi Bật</h2>
          </div>
          <Link className="link" to="/books">
            Xem toàn bộ sơ đồ phân loại DDC <RightOutlined />
          </Link>
        </div>
        {loading ? (
          <Skeleton active paragraph={{ rows: 4 }} />
        ) : categories.length === 0 ? (
          <Empty description="Chưa có danh mục" />
        ) : (
          <div className="home-cat-grid">
            {categories.map((cat, idx) => {
              const style = CATEGORY_STYLES[idx % CATEGORY_STYLES.length];
              return (
                <Link
                  key={cat.id}
                  to={`/books?categoryId=${cat.id}`}
                  className="home-cat-card"
                >
                  <div className="home-cat-icon" style={{ background: style.bg }}>
                    <ReadOutlined />
                  </div>
                  <div>
                    <h3>{cat.name}</h3>
                    <div className="home-cat-tags">
                      {style.tags.map((t) => (
                        <span key={t}>{t}</span>
                      ))}
                    </div>
                    <div className="home-cat-meta">
                      {cat.code ? `Mã ${cat.code}` : 'Danh mục OPAC'} · Xem đầu sách
                    </div>
                  </div>
                  <RightOutlined style={{ color: '#94a3b8' }} />
                </Link>
              );
            })}
          </div>
        )}
      </section>

      {/* New books */}
      <section className="home-section">
        <div className="home-section-head">
          <div>
            <h2 style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <span
                style={{
                  width: 4,
                  height: 22,
                  background: '#1d4ed8',
                  borderRadius: 4,
                  display: 'inline-block',
                }}
              />
              Sách Mới Cập Nhật Tuần Này
            </h2>
            <p className="desc">Tài liệu nghiệp vụ bổ sung theo yêu cầu bộ môn &amp; viện đào tạo</p>
          </div>
          <Link className="link" to="/sach-moi">
            Xem tất cả
          </Link>
        </div>
        {loading ? (
          <Skeleton active paragraph={{ rows: 6 }} />
        ) : newBooks.length === 0 ? (
          <Empty description="Chưa có sách mới" />
        ) : (
          <div className="home-book-grid">
            {newBooks.slice(0, 5).map((book, idx) => (
              <article key={book.id} className="home-book-card">
                <div className="home-book-cover">
                  <span className="home-book-badge">{BADGES[idx % BADGES.length]}</span>
                  {book.coverUrl ? (
                    <img src={book.coverUrl} alt={book.title} />
                  ) : (
                    coverFallback(book.title)
                  )}
                  <span className="home-book-ddc">
                    {book.publishYear ? `Năm ${book.publishYear}` : 'OPAC'}
                  </span>
                </div>
                <div className="home-book-body">
                  <h3>{book.title}</h3>
                  <div className="author">{book.publisherName || 'Nhà xuất bản đang cập nhật'}</div>
                  <div className="avail">
                    <CheckCircleFilled /> Còn {book.availableCopies ?? 0}
                    {book.totalCopies != null ? `/${book.totalCopies}` : ''} bản sẵn sàng
                  </div>
                  <div className="home-book-actions">
                    <Button size="small" icon={<EyeOutlined />} onClick={() => navigate(`/books/${book.id}`)}>
                      Chi tiết
                    </Button>
                    <Button
                      size="small"
                      type="primary"
                      icon={<ShoppingCartOutlined />}
                      onClick={() => void borrowOnline(book.id)}
                    />
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      {/* Popular */}
      <section className="home-section">
        <div className="home-popular">
          <div className="home-section-head" style={{ marginBottom: 12 }}>
            <div>
              <div className="eyebrow">XU HƯỚNG HỌC TẬP</div>
              <h2>Tài Liệu Mượn Nhiều Nhất Tháng {dayjs().format('M')}</h2>
            </div>
            <div className="home-popular-tabs">
              <button
                type="button"
                className={popularTab === 'all' ? 'active' : undefined}
                onClick={() => setPopularTab('all')}
              >
                Toàn bộ trường
              </button>
              <button
                type="button"
                className={popularTab === 'it' ? 'active' : undefined}
                onClick={() => setPopularTab('it')}
              >
                Khối Kỹ thuật &amp; CNTT
              </button>
              <button
                type="button"
                className={popularTab === 'eco' ? 'active' : undefined}
                onClick={() => setPopularTab('eco')}
              >
                Khối Kinh tế &amp; Quản trị
              </button>
            </div>
          </div>

          {loading ? (
            <Skeleton active paragraph={{ rows: 5 }} />
          ) : popularList.length === 0 ? (
            <Empty description="Chưa có dữ liệu phổ biến" />
          ) : (
            popularList.map((book, idx) => {
              const available = (book.availableCopies ?? 0) > 0;
              return (
                <div key={book.id} className="home-popular-row">
                  <div className="home-rank">{String(idx + 1).padStart(2, '0')}</div>
                  {book.coverUrl ? (
                    <img className="home-thumb" src={book.coverUrl} alt={book.title} />
                  ) : (
                    <div className="home-thumb">{coverFallback(book.title)}</div>
                  )}
                  <div>
                    <h3>{book.title}</h3>
                    <div className="meta">
                      {book.publisherName || '—'}
                      {book.publishYear ? ` · ${book.publishYear}` : ''}
                    </div>
                  </div>
                  <div className="home-borrow-count borrow-count">
                    {Math.max(12, Math.round(Number(book.avgRating || 4) * 20) + (5 - idx) * 11)} lượt
                    mượn
                  </div>
                  <div
                    className="status"
                    style={{
                      fontSize: 12,
                      fontWeight: 700,
                      color: available ? '#16a34a' : '#dc2626',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {available
                      ? `Còn ${book.availableCopies} cuốn tại chỗ`
                      : 'Tạm hết sách vật lý'}
                  </div>
                  <div className="action">
                    {available ? (
                      <Button type="primary" onClick={() => void borrowOnline(book.id)}>
                        Mượn Sách
                      </Button>
                    ) : (
                      <Button onClick={() => void reserve(book.id)}>Đặt Trước (Hold)</Button>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>
      </section>

      {/* News + Events */}
      <section className="home-split" id="tin-tuc">
        <div className="home-panel">
          <div className="home-panel-head">
            <h3>
              <ThunderboltOutlined style={{ color: '#1d4ed8' }} /> Thông Báo Độc Giả
            </h3>
            <Link to="/news" className="link">
              Tất cả thông báo
            </Link>
          </div>
          {news.length === 0 ? (
            <Empty description="Chưa có tin" image={Empty.PRESENTED_IMAGE_SIMPLE} />
          ) : (
            news.slice(0, 2).map((item, idx) => (
              <div key={item.id} className="home-news-item">
                <div className="top">
                  <span className="tag">{idx === 0 ? 'QUY ĐỊNH' : 'DỊCH VỤ'}</span>
                  <span>
                    {item.publishedAt ? dayjs(item.publishedAt).format('DD/MM/YYYY') : '—'}
                  </span>
                </div>
                <h4>
                  <Link to={`/news/${item.slug || item.id}`}>{item.title}</Link>
                </h4>
                <p>
                  {item.content
                    ? item.content.replace(/<[^>]+>/g, '').slice(0, 120) + '…'
                    : 'Xem chi tiết thông báo trên cổng độc giả.'}
                </p>
              </div>
            ))
          )}
        </div>

        <div className="home-panel">
          <div className="home-panel-head">
            <h3>
              <CalendarOutlined style={{ color: '#059669' }} /> Hoạt Động &amp; Workshop
            </h3>
            <Link className="link" to="/news?tab=events">
              Lịch hoạt động
            </Link>
          </div>
          {EVENTS.map((ev) => (
            <div key={ev.day + ev.title} className="home-event">
              <div className="home-event-date">
                <div className="m">{ev.month}</div>
                <div className="d">{ev.day}</div>
              </div>
              <div>
                <h4 style={{ margin: '0 0 4px', fontSize: 14 }}>{ev.title}</h4>
                <div style={{ fontSize: 12, color: '#64748b', marginBottom: 8 }}>{ev.place}</div>
                {ev.primary ? (
                  <Button size="small" type="primary" style={{ background: '#0B1F3A' }}>
                    {ev.cta}
                  </Button>
                ) : (
                  <Link className="link" to="/news?tab=events">
                    {ev.cta}
                  </Link>
                )}
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Stats */}
      <section className="home-stats">
        <div className="home-stat-card">
          <div className="icon" style={{ background: '#dbeafe', color: '#1d4ed8' }}>
            <ReadOutlined />
          </div>
          <div className="num">14.820</div>
          <div className="label">Đầu sách danh mục OPAC</div>
        </div>
        <div className="home-stat-card">
          <div className="icon" style={{ background: '#ede9fe', color: '#7c3aed' }}>
            <DatabaseOutlined />
          </div>
          <div className="num">48.250</div>
          <div className="label">Bản sao vật lý lưu kho</div>
        </div>
        <div className="home-stat-card">
          <div className="icon" style={{ background: '#ffedd5', color: '#ea580c' }}>
            <TeamOutlined />
          </div>
          <div className="num">3.200+</div>
          <div className="label">Độc giả thường xuyên</div>
        </div>
        <div className="home-stat-card">
          <div className="icon" style={{ background: '#dcfce7', color: '#16a34a' }}>
            <CheckCircleFilled />
          </div>
          <div className="num">99,8%</div>
          <div className="label">Tỷ lệ phục vụ chính xác</div>
        </div>
      </section>

      {/* CTA */}
      <section className="home-cta">
        <div>
          <div className="tag">HỖ TRỢ ĐỘC GIẢ TRỰC TUYẾN</div>
          <h3>Cần trợ giúp tra cứu tài liệu hiếm hoặc bài báo học thuật?</h3>
          <p>
            Thủ thư hỗ trợ chiến lược tìm kiếm Boolean, MeSH và chuẩn trích dẫn APA / IEEE — phản hồi
            trong giờ hành chính.
          </p>
        </div>
        <div className="home-cta-actions">
          <Button size="large" icon={<ReadOutlined />} onClick={() => navigate(user ? '/me/notifications' : '/login')}>
            Chat với Thủ Thư
          </Button>
          <Button
            size="large"
            type="primary"
            icon={<PhoneOutlined />}
            href="tel:02838354409"
            style={{ background: '#1d4ed8' }}
          >
            Hotline: (028) 3835 4409
          </Button>
        </div>
      </section>
    </div>
  );
}
