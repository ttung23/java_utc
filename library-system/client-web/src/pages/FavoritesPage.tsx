import { useEffect, useMemo, useState } from 'react';
import {
  CheckCircleFilled,
  ExportOutlined,
  HeartFilled,
  ShoppingCartOutlined,
  StarFilled,
} from '@ant-design/icons';
import {
  Breadcrumb,
  Button,
  Checkbox,
  Empty,
  Input,
  Rate,
  Skeleton,
  Tabs,
  Tag,
  message,
} from 'antd';
import { Link, useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import { useAuth } from '@/auth/AuthContext';
import { useBorrowCart } from '@/cart/BorrowCartContext';
import { api, type ApiResponse } from '@/services/api';

type Fav = {
  bookId: number;
  bookTitle?: string;
  title?: string;
  coverUrl?: string;
  isbn13?: string;
  avgRating?: number;
  ratingCount?: number;
  borrowCount?: number;
  publishYear?: number;
  classificationCode?: string;
  publisherName?: string;
  createdAt?: string;
  availableCopies?: number;
  totalCopies?: number;
};

type MyReview = {
  id: number;
  bookId: number;
  bookTitle?: string;
  coverUrl?: string;
  rating: number;
  comment?: string;
  createdAt?: string;
};

type SuggestBook = {
  id: number;
  title: string;
  coverUrl?: string;
  publisherName?: string;
  availableCopies?: number;
  avgRating?: number;
};

export function FavoritesPage() {
  const { user } = useAuth();
  const { addItem, hasItem } = useBorrowCart();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [favs, setFavs] = useState<Fav[]>([]);
  const [reviews, setReviews] = useState<MyReview[]>([]);
  const [suggests, setSuggests] = useState<SuggestBook[]>([]);
  const [q, setQ] = useState('');
  const [onlyAvailable, setOnlyAvailable] = useState(false);
  const [tab, setTab] = useState('wishlist');

  const load = async () => {
    setLoading(true);
    try {
      const [favRes, revRes, popRes] = await Promise.all([
        api.get<ApiResponse<Fav[]>>('/favorites/me'),
        api.get<ApiResponse<MyReview[]>>('/reviews/me').catch(() => ({ data: { data: [] as MyReview[] } })),
        api.get<ApiResponse<SuggestBook[]>>('/books/popular').catch(() => ({ data: { data: [] as SuggestBook[] } })),
      ]);
      const base = favRes.data.data || [];
      const enriched = await Promise.all(
        base.map(async (f) => {
          try {
            const { data } = await api.get<
              ApiResponse<{
                copySummary?: { available?: number; total?: number };
                coverUrl?: string;
                isbn13?: string;
                avgRating?: number;
                publisherName?: string;
                classificationCode?: string;
              }>
            >(`/books/${f.bookId}`);
            return {
              ...f,
              coverUrl: f.coverUrl || data.data?.coverUrl,
              isbn13: f.isbn13 || data.data?.isbn13,
              avgRating: f.avgRating ?? data.data?.avgRating,
              publisherName: f.publisherName || data.data?.publisherName,
              classificationCode: f.classificationCode || data.data?.classificationCode,
              availableCopies: data.data?.copySummary?.available ?? 0,
              totalCopies: data.data?.copySummary?.total ?? 0,
            } satisfies Fav;
          } catch {
            return { ...f, availableCopies: 0, totalCopies: 0 };
          }
        }),
      );
      setFavs(enriched);
      setReviews(revRes.data.data || []);
      setSuggests((popRes.data.data || []).slice(0, 6));
    } catch {
      message.error('Không tải được danh sách yêu thích');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const filtered = useMemo(() => {
    const key = q.trim().toLowerCase();
    return favs.filter((f) => {
      if (onlyAvailable && (f.availableCopies ?? 0) <= 0) return false;
      if (!key) return true;
      const hay = `${f.bookTitle || f.title || ''} ${f.publisherName || ''} ${f.isbn13 || ''}`.toLowerCase();
      return hay.includes(key);
    });
  }, [favs, q, onlyAvailable]);

  const stats = useMemo(() => {
    const available = favs.filter((f) => (f.availableCopies ?? 0) > 0).length;
    const low = favs.filter((f) => {
      const a = f.availableCopies ?? 0;
      return a > 0 && a <= 3;
    }).length;
    const out = favs.filter((f) => (f.availableCopies ?? 0) <= 0).length;
    const avg =
      reviews.length === 0
        ? 0
        : reviews.reduce((s, r) => s + Number(r.rating || 0), 0) / reviews.length;
    return { available, low, out, avg };
  }, [favs, reviews]);

  const remove = async (bookId: number) => {
    try {
      await api.delete(`/favorites/${bookId}`);
      message.success('Đã bỏ yêu thích');
      void load();
    } catch {
      message.error('Thao tác thất bại');
    }
  };

  const addOne = (f: Fav) => {
    if ((f.availableCopies ?? 0) <= 0) {
      navigate(`/books/${f.bookId}`);
      return;
    }
    const ok = addItem({
      bookId: f.bookId,
      title: f.bookTitle || f.title || `Sách #${f.bookId}`,
      coverUrl: f.coverUrl,
      publisherName: f.publisherName,
      isbn13: f.isbn13,
      availableCopies: f.availableCopies,
      totalCopies: f.totalCopies,
      classificationCode: f.classificationCode,
    });
    if (!ok) {
      message.warning(hasItem(f.bookId) ? 'Sách đã có trong giỏ' : 'Giỏ đã đủ hạn mức');
      return;
    }
    message.success('Đã thêm vào giỏ mượn');
  };

  const addAllAvailable = () => {
    const available = filtered.filter((f) => (f.availableCopies ?? 0) > 0 && !hasItem(f.bookId));
    if (available.length === 0) {
      message.info('Không còn sách sẵn sàng để thêm');
      return;
    }
    let added = 0;
    for (const f of available) {
      const ok = addItem({
        bookId: f.bookId,
        title: f.bookTitle || f.title || `Sách #${f.bookId}`,
        coverUrl: f.coverUrl,
        publisherName: f.publisherName,
        isbn13: f.isbn13,
        availableCopies: f.availableCopies,
        totalCopies: f.totalCopies,
        classificationCode: f.classificationCode,
      });
      if (ok) added += 1;
      else break;
    }
    message.success(`Đã thêm ${added} sách vào giỏ mượn`);
  };

  const reserve = async (bookId: number) => {
    try {
      await api.post('/reservations', { bookId });
      message.success('Đã đặt trước');
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Đặt trước thất bại');
    }
  };

  if (loading) return <Skeleton active paragraph={{ rows: 12 }} />;

  return (
    <div>
      <Breadcrumb
        style={{ marginBottom: 12 }}
        items={[
          { title: <Link to="/">Trang chủ</Link> },
          { title: 'Tài khoản & Hoạt động độc giả' },
          { title: 'Đánh giá & Danh sách yêu thích' },
        ]}
      />

      <div className="home-section-head">
        <div>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
            <h2 style={{ margin: 0, fontSize: 26, color: '#0b1f3a' }}>Đánh Giá Sách &amp; Danh Sách Yêu Thích</h2>
            <Tag color="blue">Thẻ Độc Giả Chuẩn</Tag>
          </div>
          <p className="desc">
            {user?.fullName} · {user?.email}
          </p>
        </div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <Button icon={<ExportOutlined />} onClick={() => message.info('Xuất danh mục sẽ bổ sung sau')}>
            Xuất danh mục đọc
          </Button>
          <Button type="primary" style={{ background: '#0B1F3A', color: '#fff' }} onClick={() => navigate('/books')}>
            Tra cứu thêm sách
          </Button>
        </div>
      </div>

      <div className="home-stats" style={{ marginTop: 0, marginBottom: 16 }}>
        <div className="home-stat-card">
          <div className="icon" style={{ background: '#ffe4e6', color: '#e11d48' }}>
            <HeartFilled />
          </div>
          <div className="num">{String(favs.length).padStart(2, '0')}</div>
          <div className="label">
            Sách đã lưu · {stats.available} sẵn · {stats.low} sắp hết · {stats.out} hết
          </div>
        </div>
        <div className="home-stat-card">
          <div className="icon" style={{ background: '#fef3c7', color: '#d97706' }}>
            <StarFilled />
          </div>
          <div className="num">{String(reviews.length).padStart(2, '0')}</div>
          <div className="label">Đánh giá đã đóng góp · TB {stats.avg.toFixed(1)}★</div>
        </div>
        <div className="home-stat-card">
          <div className="icon" style={{ background: '#dbeafe', color: '#1d4ed8' }}>
            <CheckCircleFilled />
          </div>
          <div className="num">{reviews.length * 3}</div>
          <div className="label">Tương tác cộng đồng (ước lượng)</div>
        </div>
        <div className="home-stat-card">
          <div className="icon" style={{ background: '#dcfce7', color: '#16a34a' }}>
            <CheckCircleFilled />
          </div>
          <div className="num">100%</div>
          <div className="label">Xác thực mượn sách (BR)</div>
        </div>
      </div>

      <div className="home-panel">
        <Tabs
          activeKey={tab}
          onChange={setTab}
          items={[
            {
              key: 'wishlist',
              label: `Danh Sách Yêu Thích (${favs.length})`,
              children: (
                <div>
                  <div
                    style={{
                      display: 'flex',
                      flexWrap: 'wrap',
                      gap: 10,
                      alignItems: 'center',
                      marginBottom: 12,
                    }}
                  >
                    <Input.Search
                      allowClear
                      placeholder="Tìm tên sách, tác giả, ISBN trong Wishlist"
                      style={{ flex: 1, minWidth: 220 }}
                      value={q}
                      onChange={(e) => setQ(e.target.value)}
                    />
                    <Checkbox checked={onlyAvailable} onChange={(e) => setOnlyAvailable(e.target.checked)}>
                      Chỉ sách đang sẵn sàng trên kệ ({stats.available})
                    </Checkbox>
                    <Button
                      type="primary"
                      icon={<ShoppingCartOutlined />}
                      style={{ background: '#0B1F3A', color: '#fff', borderColor: '#0B1F3A' }}
                      onClick={addAllAvailable}
                    >
                      Thêm tất cả sách có sẵn vào Giỏ mượn
                    </Button>
                  </div>

                  <div
                    style={{
                      marginBottom: 14,
                      padding: 12,
                      borderRadius: 10,
                      background: '#eff6ff',
                      color: '#1e3a8a',
                      fontSize: 13,
                    }}
                  >
                    <strong>Mẹo thủ thư:</strong> sách trong giỏ sau khi duyệt được giữ khoảng 24–48 giờ tại quầy.
                    Ưu tiên mượn các đầu sách sắp hết để tránh hết hàng.
                  </div>

                  {filtered.length === 0 ? (
                    <Empty description="Chưa có sách yêu thích phù hợp">
                      <Button type="primary" onClick={() => navigate('/books')}>
                        Đi tra cứu OPAC
                      </Button>
                    </Empty>
                  ) : (
                    <div className="wish-grid">
                      {filtered.map((f) => {
                        const available = f.availableCopies ?? 0;
                        const total = f.totalCopies ?? 0;
                        const low = available > 0 && available <= 3;
                        const out = available <= 0;
                        const title = f.bookTitle || f.title || `Sách #${f.bookId}`;
                        return (
                          <article key={f.bookId} className="wish-card">
                            <div className="wish-cover">
                              {f.coverUrl ? <img src={f.coverUrl} alt={title} /> : <span>{title}</span>}
                              <button type="button" className="heart" onClick={() => void remove(f.bookId)}>
                                <HeartFilled />
                              </button>
                            </div>
                            <div className="wish-body">
                              <Link to={`/books/${f.bookId}`}>
                                <h3>{title}</h3>
                              </Link>
                              <div className="meta">{f.publisherName || '—'}{f.publishYear ? ` · ${f.publishYear}` : ''}</div>
                              <div className="meta">
                                <StarFilled style={{ color: '#f59e0b' }} /> {Number(f.avgRating || 0).toFixed(1)}
                                {f.classificationCode ? ` · ${f.classificationCode}` : ''}
                              </div>
                              <div
                                className="stock"
                                style={{ color: out ? '#dc2626' : low ? '#d97706' : '#16a34a' }}
                              >
                                {out
                                  ? `Tạm hết sách trên kệ (0${total ? `/${total}` : ''} bản)`
                                  : low
                                    ? `Còn ${available} bản (Sắp hết)`
                                    : `Còn ${available}${total ? `/${total}` : ''} bản sẵn sàng`}
                              </div>
                              <div className="meta">ISBN: {f.isbn13 || '—'}</div>
                              <div className="actions">
                                {out ? (
                                  <Button type="primary" onClick={() => void reserve(f.bookId)}>
                                    Đặt trước sách (Reserve)
                                  </Button>
                                ) : (
                                  <Button
                                    type="primary"
                                    icon={<ShoppingCartOutlined />}
                                    style={{ background: '#0B1F3A', color: '#fff', borderColor: '#0B1F3A' }}
                                    onClick={() => (hasItem(f.bookId) ? navigate('/borrow-cart') : addOne(f))}
                                  >
                                    {hasItem(f.bookId)
                                      ? 'Đã trong giỏ'
                                      : low
                                        ? 'Mượn ngay (Ưu tiên)'
                                        : 'Thêm vào giỏ mượn'}
                                  </Button>
                                )}
                                <Button onClick={() => navigate(`/books/${f.bookId}`)}>Chi tiết</Button>
                              </div>
                            </div>
                          </article>
                        );
                      })}
                    </div>
                  )}
                </div>
              ),
            },
            {
              key: 'reviews',
              label: `Đánh Giá Của Tôi (${reviews.length})`,
              children:
                reviews.length === 0 ? (
                  <Empty description="Bạn chưa có đánh giá nào" />
                ) : (
                  <div style={{ display: 'grid', gap: 12 }}>
                    {reviews.map((r) => (
                      <div key={r.id} className="wish-review">
                        <div>
                          <Link to={`/books/${r.bookId}`}>
                            <strong>{r.bookTitle || `Sách #${r.bookId}`}</strong>
                          </Link>
                          <div style={{ marginTop: 4 }}>
                            <Rate disabled value={r.rating} style={{ fontSize: 14 }} />
                          </div>
                          <p style={{ margin: '6px 0 0', color: '#475569' }}>{r.comment || '—'}</p>
                          <div style={{ fontSize: 12, color: '#94a3b8', marginTop: 4 }}>
                            {r.createdAt ? dayjs(r.createdAt).format('DD/MM/YYYY HH:mm') : ''}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                ),
            },
            {
              key: 'suggest',
              label: 'Gợi Ý Riêng Cho Bạn',
              children:
                suggests.length === 0 ? (
                  <Empty description="Chưa có gợi ý" />
                ) : (
                  <div className="wish-grid">
                    {suggests.map((b) => (
                      <article key={b.id} className="wish-card">
                        <div className="wish-cover">
                          {b.coverUrl ? <img src={b.coverUrl} alt={b.title} /> : <span>{b.title}</span>}
                        </div>
                        <div className="wish-body">
                          <Link to={`/books/${b.id}`}>
                            <h3>{b.title}</h3>
                          </Link>
                          <div className="meta">{b.publisherName || '—'}</div>
                          <div className="actions">
                            <Button type="primary" style={{ background: '#0B1F3A', color: '#fff' }} onClick={() => navigate(`/books/${b.id}`)}>
                              Xem chi tiết
                            </Button>
                          </div>
                        </div>
                      </article>
                    ))}
                  </div>
                ),
            },
          ]}
        />
      </div>
    </div>
  );
}
