import { useEffect, useMemo, useState } from 'react';
import {
  BookOutlined,
  CheckCircleFilled,
  HeartFilled,
  HeartOutlined,
  ReadOutlined,
  ShareAltOutlined,
  ShoppingCartOutlined,
  StarFilled,
} from '@ant-design/icons';
import {
  Breadcrumb,
  Button,
  Empty,
  Form,
  Input,
  Rate,
  Skeleton,
  Tabs,
  Tag,
  message,
} from 'antd';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '@/auth/AuthContext';
import { useBorrowCart } from '@/cart/BorrowCartContext';
import { api, type ApiResponse } from '@/services/api';

type CopySummary = {
  total?: number;
  available?: number;
  borrowed?: number;
  hold?: number;
  lost?: number;
};

type Book = {
  id: number;
  title: string;
  subtitle?: string;
  description?: string;
  isbn13?: string;
  isbn10?: string;
  coverUrl?: string;
  publisherName?: string;
  publishYear?: number;
  languageName?: string;
  pages?: number;
  classificationCode?: string;
  tags?: string[];
  reference?: boolean;
  loanable?: boolean;
  loanDaysOverride?: number | null;
  avgRating?: number;
  ratingCount?: number;
  borrowCount?: number;
  authorNames?: string[];
  categoryNames?: string[];
  categoryIds?: number[];
  copySummary?: CopySummary;
};

type Copy = {
  id: number;
  barcode: string;
  copyNo?: number;
  locationCode?: string;
  status: string;
  condition?: string;
};

type Related = {
  id: number;
  title: string;
  coverUrl?: string;
  publisherName?: string;
  availableCopies?: number;
};

type Review = {
  id: number;
  rating: number;
  comment?: string;
  userName?: string;
  createdAt?: string;
};

function statusLabel(status: string) {
  const s = String(status || '').toUpperCase();
  if (s === 'AVAILABLE') return { text: 'Sẵn sàng', color: '#16a34a' };
  if (s === 'BORROWED' || s === 'ON_LOAN') return { text: 'Đang mượn', color: '#d97706' };
  if (s === 'RESERVED' || s === 'HOLD') return { text: 'Đặt trước', color: '#2563eb' };
  if (s === 'LOST') return { text: 'Mất', color: '#dc2626' };
  if (s === 'DAMAGED') return { text: 'Hư hỏng', color: '#dc2626' };
  return { text: status || '—', color: '#64748b' };
}

function conditionLabel(c?: string) {
  const s = String(c || '').toUpperCase();
  if (s === 'NEW') return 'Mới';
  if (s === 'GOOD') return 'Tốt';
  if (s === 'FAIR') return 'Khá';
  if (s === 'POOR') return 'Yếu';
  return c || '—';
}

export function BookDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const { addItem, hasItem } = useBorrowCart();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [book, setBook] = useState<Book | null>(null);
  const [copies, setCopies] = useState<Copy[]>([]);
  const [related, setRelated] = useState<Related[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [fav, setFav] = useState(false);
  const [form] = Form.useForm();

  const load = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const [b, copiesRes, relatedRes, reviewsRes] = await Promise.all([
        api.get<ApiResponse<Book>>(`/books/${id}`),
        api.get<ApiResponse<Copy[]>>(`/books/${id}/copies`).catch(() => ({ data: { data: [] as Copy[] } })),
        api.get<ApiResponse<Related[]>>(`/books/${id}/related`).catch(() => ({ data: { data: [] as Related[] } })),
        api.get<ApiResponse<Review[]>>('/reviews', { params: { bookId: id } }).catch(() => ({
          data: { data: [] as Review[] },
        })),
      ]);
      setBook(b.data.data);
      setCopies(copiesRes.data.data || []);
      setRelated(relatedRes.data.data || []);
      setReviews(reviewsRes.data.data || []);
    } catch {
      message.error('Không tải được chi tiết sách');
      setBook(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const available = book?.copySummary?.available ?? copies.filter((c) => c.status === 'AVAILABLE').length;
  const total = book?.copySummary?.total ?? copies.length;
  const canBorrow = Boolean(book?.loanable !== false && !book?.reference && available > 0);

  const policyDays = book?.loanDaysOverride ?? 14;

  const authors = useMemo(() => book?.authorNames?.filter(Boolean) || [], [book]);

  const requireLogin = (action: string) => {
    if (!user) {
      message.warning(`Đăng nhập để ${action}`);
      navigate('/login');
      return false;
    }
    return true;
  };

  const toggleFav = async () => {
    if (!id || !requireLogin('yêu thích')) return;
    try {
      if (fav) {
        await api.delete(`/favorites/${id}`);
        setFav(false);
        message.success('Đã bỏ yêu thích');
      } else {
        await api.post(`/favorites/${id}`);
        setFav(true);
        message.success('Đã thêm yêu thích');
      }
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Thao tác thất bại');
    }
  };

  const addToCart = () => {
    if (!book || !id || !requireLogin('thêm vào giỏ mượn')) return;
    if (!canBorrow) {
      message.warning('Sách không thể thêm vào giỏ — hãy đặt trước nếu hết bản');
      return;
    }
    const ok = addItem({
      bookId: book.id,
      title: book.title,
      coverUrl: book.coverUrl,
      publisherName: book.publisherName,
      isbn13: book.isbn13,
      availableCopies: book.copySummary?.available,
      totalCopies: book.copySummary?.total,
      classificationCode: book.classificationCode,
      authorNames: book.authorNames,
    });
    if (!ok) {
      message.warning(hasItem(book.id) ? 'Sách đã có trong giỏ' : 'Giỏ đã đủ hạn mức (tối đa 5)');
      return;
    }
    message.success('Đã thêm vào giỏ mượn');
  };

  const reserve = async () => {
    if (!id || !requireLogin('đặt trước / giữ chỗ')) return;
    try {
      await api.post('/reservations', { bookId: Number(id) });
      message.success('Đã đặt trước thành công');
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Đặt trước thất bại');
    }
  };

  const share = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href);
      message.success('Đã sao chép link sách');
    } catch {
      message.info(window.location.href);
    }
  };

  const submitReview = async () => {
    if (!requireLogin('đánh giá')) return;
    const values = await form.validateFields();
    try {
      await api.post('/reviews', {
        bookId: Number(id),
        rating: values.rating,
        comment: values.comment,
      });
      message.success('Đã gửi đánh giá');
      form.resetFields();
      void load();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Gửi đánh giá thất bại');
    }
  };

  if (loading) {
    return <Skeleton active paragraph={{ rows: 12 }} />;
  }

  if (!book) {
    return <Empty description="Không tìm thấy sách" />;
  }

  const categoryLabel = book.categoryNames?.[0] || 'Danh mục';

  return (
    <div>
      <Breadcrumb
        className="bd-crumb"
        items={[
          { title: <Link to="/">Trang chủ</Link> },
          { title: <Link to="/books">Tra cứu &amp; Danh mục</Link> },
          {
            title: book.categoryIds?.[0] ? (
              <Link to={`/books?categoryId=${book.categoryIds[0]}`}>{categoryLabel}</Link>
            ) : (
              categoryLabel
            ),
          },
          { title: book.title },
        ]}
      />

      <section className="bd-top">
        <aside className="bd-cover-card">
          <div className="bd-cover">
            {book.coverUrl ? <img src={book.coverUrl} alt={book.title} /> : book.title}
          </div>
          <Button block icon={<ReadOutlined />} style={{ marginTop: 12 }}>
            Đọc thử / xem mô tả
          </Button>
          <div className="bd-side-actions">
            <Button icon={fav ? <HeartFilled style={{ color: '#dc2626' }} /> : <HeartOutlined />} onClick={() => void toggleFav()}>
              Yêu thích
            </Button>
            <Button icon={<BookOutlined />} onClick={() => message.info('Tủ sách cá nhân sẽ bổ sung sau')}>
              Tủ sách
            </Button>
            <Button icon={<ShareAltOutlined />} onClick={() => void share()}>
              Chia sẻ
            </Button>
            <Button onClick={() => navigate('/rules')}>Nội quy</Button>
          </div>
          <div className="bd-policy">
            <strong>Quy định mượn bản này</strong>
            • Thời hạn: {policyDays} ngày
            <br />
            • Gia hạn: tối đa theo loại thẻ (thường 2 lần)
            <br />
            • Phạt quá hạn: theo cấu hình hệ thống (ví dụ 2.000đ/ngày)
            <br />
            {book.reference ? '• Tài liệu chỉ đọc tại chỗ' : '• Được mang ra ngoài theo hạn mức thẻ'}
          </div>
        </aside>

        <div className="bd-main">
          <h1>{book.title}</h1>
          {book.subtitle ? <p className="bd-subtitle">{book.subtitle}</p> : null}

          <div className="bd-authors">
            {authors.length > 0 ? (
              authors.map((name, i) => (
                <span key={name}>
                  {i > 0 ? ', ' : 'Tác giả: '}
                  <Link to={`/books?q=${encodeURIComponent(name)}`}>{name}</Link>
                </span>
              ))
            ) : (
              <span style={{ color: '#64748b' }}>Tác giả: đang cập nhật</span>
            )}
          </div>

          <div className="bd-stats-row">
            <span>
              <StarFilled style={{ color: '#f59e0b' }} />{' '}
              <strong style={{ color: '#0f172a' }}>{Number(book.avgRating || 0).toFixed(1)}</strong>
              {' · '}
              {book.ratingCount ?? reviews.length} đánh giá
            </span>
            <span>{book.borrowCount ?? 0} lượt mượn</span>
            <span>
              ISBN: {book.isbn13 || book.isbn10 || '—'}
            </span>
          </div>

          <div className="bd-meta-grid">
            <div>
              <div className="k">Nhà xuất bản</div>
              <div className="v">
                {book.publisherName || '—'}
                {book.publishYear ? `, ${book.publishYear}` : ''}
              </div>
            </div>
            <div>
              <div className="k">Số trang</div>
              <div className="v">{book.pages ? `${book.pages} trang` : '—'}</div>
            </div>
            <div>
              <div className="k">Ngôn ngữ</div>
              <div className="v">{book.languageName || '—'}</div>
            </div>
            <div>
              <div className="k">Mã phân loại</div>
              <div className="v">{book.classificationCode || '—'}</div>
            </div>
          </div>

          <div className="bd-avail">
            <div>
              <div className="title">
                {canBorrow ? (
                  <>
                    <CheckCircleFilled /> Trạng thái lưu thông: Đang còn sách
                  </>
                ) : book.reference ? (
                  'Tài liệu chỉ đọc tại chỗ'
                ) : (
                  'Tạm hết sách vật lý'
                )}
              </div>
              <div className="sub">
                Còn {available}/{total} bản sẵn sàng
                {book.copySummary?.borrowed != null ? ` · Đang mượn ${book.copySummary.borrowed}` : ''}
              </div>
            </div>
            <div className="bd-avail-actions">
              {canBorrow ? (
                <Button
                  size="large"
                  icon={<ShoppingCartOutlined />}
                  onClick={() => (hasItem(book.id) ? navigate('/borrow-cart') : addToCart())}
                  style={{ background: '#fff', color: '#0b1f3a', fontWeight: 700 }}
                >
                  {hasItem(book.id) ? 'Đã trong giỏ — xem giỏ' : 'Thêm vào giỏ mượn'}
                </Button>
              ) : book.reference ? null : (
                <Button
                  size="large"
                  onClick={() => void reserve()}
                  style={{ background: '#fff', color: '#0b1f3a', fontWeight: 700 }}
                >
                  Đặt trước (Hold)
                </Button>
              )}
            </div>
          </div>
          {canBorrow ? (
            <div style={{ marginTop: 8, fontSize: 12, color: '#64748b' }}>
              Còn sách trên kệ → dùng <strong>mượn / giỏ mượn</strong>. Nút <strong>Đặt trước</strong> chỉ hiện khi
              tạm hết toàn bộ bản AVAILABLE.
            </div>
          ) : null}
        </div>
      </section>

      <section className="bd-section">
        <h2>Danh mục từng bản sao vật lý</h2>
        {copies.length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có dữ liệu bản sao" />
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="bd-copy-table">
              <thead>
                <tr>
                  <th>Mã vạch</th>
                  <th>Vị trí</th>
                  <th>Tình trạng vật lý</th>
                  <th>Chính sách</th>
                  <th>Trạng thái</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {copies.map((c) => {
                  const st = statusLabel(c.status);
                  const selectable = String(c.status).toUpperCase() === 'AVAILABLE' && canBorrow;
                  return (
                    <tr key={c.id}>
                      <td>
                        <code>{c.barcode}</code>
                      </td>
                      <td>{c.locationCode || '—'}</td>
                      <td>{conditionLabel(c.condition)}</td>
                      <td>{book.reference ? 'Chỉ đọc tại chỗ' : 'Được mang về'}</td>
                      <td style={{ color: st.color, fontWeight: 700 }}>{st.text}</td>
                      <td>
                        {selectable ? (
                          <Button size="small" type="link" onClick={addToCart}>
                            Chọn bản này
                          </Button>
                        ) : (
                          <span style={{ color: '#94a3b8' }}>—</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="bd-section">
        <Tabs
          items={[
            {
              key: 'summary',
              label: 'Tóm tắt & Giới thiệu',
              children: (
                <div style={{ color: '#334155', lineHeight: 1.7, whiteSpace: 'pre-wrap' }}>
                  {book.description || 'Chưa có mô tả chi tiết cho tài liệu này.'}
                  {book.tags && book.tags.length > 0 ? (
                    <div className="bd-tags">
                      {book.tags.map((t) => (
                        <Tag key={t} color="blue">
                          {t}
                        </Tag>
                      ))}
                    </div>
                  ) : null}
                </div>
              ),
            },
            {
              key: 'toc',
              label: 'Mục lục chi tiết',
              children: (
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description="Mục lục chi tiết sẽ được bổ sung khi có dữ liệu biên mục đầy đủ."
                />
              ),
            },
            {
              key: 'reviews',
              label: `Đánh giá & Bình luận (${reviews.length})`,
              children: (
                <div>
                  {reviews.length === 0 ? (
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có đánh giá" />
                  ) : (
                    <div style={{ display: 'grid', gap: 12, marginBottom: 16 }}>
                      {reviews.map((r) => (
                        <div
                          key={r.id}
                          style={{
                            border: '1px solid #e2e8f0',
                            borderRadius: 12,
                            padding: 12,
                          }}
                        >
                          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                            <strong>{r.userName || 'Ẩn danh'}</strong>
                            <Rate disabled value={r.rating} style={{ fontSize: 14 }} />
                          </div>
                          <p style={{ margin: '6px 0 0', color: '#475569' }}>{r.comment || '—'}</p>
                        </div>
                      ))}
                    </div>
                  )}
                  {user ? (
                    <Form form={form} layout="vertical" onFinish={() => void submitReview()}>
                      <Form.Item name="rating" label="Số sao" rules={[{ required: true, message: 'Chọn số sao' }]}>
                        <Rate />
                      </Form.Item>
                      <Form.Item name="comment" label="Nhận xét">
                        <Input.TextArea rows={3} placeholder="Chia sẻ trải nghiệm đọc…" />
                      </Form.Item>
                      <Button type="primary" htmlType="submit" style={{ background: '#0B1F3A' }}>
                        Gửi đánh giá
                      </Button>
                    </Form>
                  ) : (
                    <Button onClick={() => navigate('/login')}>Đăng nhập để đánh giá</Button>
                  )}
                </div>
              ),
            },
            {
              key: 'policy',
              label: 'Nội quy & Chính sách bảo quản',
              children: (
                <div style={{ color: '#475569', lineHeight: 1.7 }}>
                  <p>
                    Độc giả có trách nhiệm giữ gìn tài liệu, không viết/vẽ, không làm hư hỏng. Vi phạm sẽ
                    bị phạt theo quy định thư viện UTC.
                  </p>
                  <Link to="/rules">Xem đầy đủ quy định mượn trả →</Link>
                </div>
              ),
            },
          ]}
        />
      </section>

      <section className="bd-section">
        <div className="home-section-head" style={{ marginBottom: 12 }}>
          <h2 style={{ margin: 0 }}>Tài liệu cùng chuyên đề đề xuất</h2>
          <Link className="link" to="/books">
            Xem thêm
          </Link>
        </div>
        {related.length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có gợi ý liên quan" />
        ) : (
          <div className="bd-related-grid">
            {related.slice(0, 4).map((item) => (
              <Link key={item.id} to={`/books/${item.id}`} className="home-book-card">
                <div className="home-book-cover" style={{ minHeight: 160 }}>
                  {item.coverUrl ? <img src={item.coverUrl} alt={item.title} /> : item.title}
                </div>
                <div className="home-book-body">
                  <h3>{item.title}</h3>
                  <div className="author">{item.publisherName || '—'}</div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
