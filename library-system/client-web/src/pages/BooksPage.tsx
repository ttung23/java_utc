import { useEffect, useMemo, useState } from 'react';
import {
  AppstoreOutlined,
  EyeOutlined,
  FilterOutlined,
  ShoppingCartOutlined,
  StarFilled,
  UnorderedListOutlined,
} from '@ant-design/icons';
import {
  Breadcrumb,
  Button,
  Checkbox,
  Empty,
  Input,
  Pagination,
  Radio,
  Select,
  Skeleton,
  Tag,
  message,
} from 'antd';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/AuthContext';
import { useBorrowCart } from '@/cart/BorrowCartContext';
import { api, type ApiResponse } from '@/services/api';

type Book = {
  id: number;
  title: string;
  subtitle?: string;
  coverUrl?: string;
  avgRating?: number;
  borrowCount?: number;
  availableCopies?: number;
  totalCopies?: number;
  publisherName?: string;
  publishYear?: number;
  isbn13?: string;
  classificationCode?: string;
  authorNames?: string[];
};

type Category = { id: number; name: string; code?: string; children?: Category[] };
type Language = { id: number; name: string; code?: string };
type Meta = { page: number; limit: number; total: number };

export function BooksPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { addItem, hasItem } = useBorrowCart();

  const [q, setQ] = useState(searchParams.get('q') || '');
  const [rows, setRows] = useState<Book[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [languages, setLanguages] = useState<Language[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 12, total: 0 });
  const [loading, setLoading] = useState(false);
  const [view, setView] = useState<'grid' | 'list'>('grid');

  const availableOnly = searchParams.get('available') === '1';
  const categoryId = searchParams.get('category') || searchParams.get('categoryId') || undefined;
  const languageId = searchParams.get('language') || undefined;
  const year = searchParams.get('year') || undefined;
  const sort = searchParams.get('sort') || 'relevance';
  const page = Number(searchParams.get('page') || '0');

  const loadFilters = async () => {
    try {
      const [cats, langs] = await Promise.all([
        api.get<ApiResponse<Category[]>>('/categories', { params: { size: 50 } }),
        api.get<ApiResponse<Language[]>>('/languages', { params: { size: 50 } }).catch(() => ({
          data: { data: [] as Language[] },
        })),
      ]);
      setCategories(cats.data.data || []);
      setLanguages(langs.data.data || []);
    } catch {
      // ignore filter load errors
    }
  };

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Book[]>>('/books', {
        params: {
          q: q || undefined,
          category: categoryId || undefined,
          language: languageId || undefined,
          year: year || undefined,
          available: availableOnly ? true : undefined,
          page,
          size: meta.limit,
          sort: sort === 'relevance' ? undefined : sort,
        },
      });
      setRows(data.data || []);
      const m = data.meta as Meta | undefined;
      setMeta({
        page: m?.page ?? page,
        limit: m?.limit ?? 12,
        total: m?.total ?? data.data?.length ?? 0,
      });
    } catch {
      message.error('Không tải được danh sách sách');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadFilters();
  }, []);

  useEffect(() => {
    setQ(searchParams.get('q') || '');
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  const patchParams = (patch: Record<string, string | undefined>) => {
    const next = new URLSearchParams(searchParams);
    Object.entries(patch).forEach(([k, v]) => {
      if (v === undefined || v === '') next.delete(k);
      else next.set(k, v);
    });
    if (!('page' in patch)) next.set('page', '0');
    setSearchParams(next);
  };

  const activeFilters = useMemo(() => {
    const tags: { key: string; label: string; clear: () => void }[] = [];
    if (q) tags.push({ key: 'q', label: q, clear: () => patchParams({ q: undefined }) });
    if (availableOnly)
      tags.push({
        key: 'available',
        label: 'Chỉ còn sách sẵn có',
        clear: () => patchParams({ available: undefined }),
      });
    if (categoryId) {
      const cat = categories.find((c) => String(c.id) === String(categoryId));
      tags.push({
        key: 'category',
        label: cat?.name || `Danh mục #${categoryId}`,
        clear: () => patchParams({ category: undefined, categoryId: undefined }),
      });
    }
    if (year) tags.push({ key: 'year', label: `Năm ${year}`, clear: () => patchParams({ year: undefined }) });
    if (languageId) {
      const lang = languages.find((l) => String(l.id) === String(languageId));
      tags.push({
        key: 'language',
        label: lang?.name || `Ngôn ngữ #${languageId}`,
        clear: () => patchParams({ language: undefined }),
      });
    }
    return tags;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [q, availableOnly, categoryId, year, languageId, categories, languages]);

  const addToCart = (book: Book) => {
    if (!user) {
      message.warning('Đăng nhập để thêm vào giỏ mượn');
      navigate('/login');
      return;
    }
    if ((book.availableCopies ?? 0) <= 0) {
      message.warning('Sách tạm hết — hãy đặt trước ở trang chi tiết');
      return;
    }
    const ok = addItem({
      bookId: book.id,
      title: book.title,
      coverUrl: book.coverUrl,
      publisherName: book.publisherName,
      isbn13: book.isbn13,
      availableCopies: book.availableCopies,
      totalCopies: book.totalCopies,
      classificationCode: book.classificationCode,
      authorNames: book.authorNames,
    });
    if (!ok) {
      message.warning(hasItem(book.id) ? 'Sách đã có trong giỏ' : 'Giỏ đã đủ hạn mức (tối đa 5)');
      return;
    }
    message.success('Đã thêm vào giỏ mượn');
  };

  const yearOptions = [
    { label: '2024 (Mới)', value: '2024' },
    { label: '2023', value: '2023' },
    { label: '2022', value: '2022' },
    { label: '2021', value: '2021' },
    { label: '2020', value: '2020' },
  ];

  return (
    <div>
      <Breadcrumb
        style={{ marginBottom: 12 }}
        items={[
          { title: <Link to="/">Trang chủ</Link> },
          { title: 'Tra cứu & Danh mục tài liệu' },
        ]}
      />

      <div className="home-section-head" style={{ marginBottom: 14 }}>
        <div>
          <h2 style={{ margin: 0, fontSize: 26, color: '#0b1f3a' }}>Tra Cứu Học Liệu &amp; Danh Mục OPAC</h2>
          <p className="desc">Lọc theo tình trạng, thể loại, năm xuất bản và ngôn ngữ.</p>
        </div>
        <Tag color="blue">{meta.total.toLocaleString('vi-VN')} kết quả</Tag>
      </div>

      <div className="opac-toolbar">
        <Input.Search
          allowClear
          size="large"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onSearch={(value) => patchParams({ q: value || undefined })}
          placeholder="Tìm theo tên sách, ISBN, tác giả…"
          style={{ flex: 1, minWidth: 220 }}
        />
        <Select
          value={sort}
          style={{ width: 200 }}
          onChange={(v) => patchParams({ sort: v })}
          options={[
            { value: 'relevance', label: 'Độ liên quan cao nhất' },
            { value: 'title', label: 'Tiêu đề A-Z' },
            { value: 'year', label: 'Năm xuất bản' },
            { value: 'borrowCount', label: 'Mượn nhiều' },
          ]}
        />
        <Button.Group>
          <Button
            type={view === 'grid' ? 'primary' : 'default'}
            icon={<AppstoreOutlined />}
            onClick={() => setView('grid')}
          />
          <Button
            type={view === 'list' ? 'primary' : 'default'}
            icon={<UnorderedListOutlined />}
            onClick={() => setView('list')}
          />
        </Button.Group>
      </div>

      {activeFilters.length > 0 ? (
        <div className="opac-chips">
          {activeFilters.map((f) => (
            <Tag key={f.key} closable onClose={f.clear} color="processing">
              {f.label}
            </Tag>
          ))}
          <Button type="link" size="small" onClick={() => setSearchParams({})}>
            Xóa lọc
          </Button>
        </div>
      ) : null}

      <div className="opac-layout">
        <aside className="opac-sidebar">
          <div className="opac-side-block">
            <h4>
              <FilterOutlined /> Trạng thái bản đọc
            </h4>
            <Checkbox
              checked={availableOnly}
              onChange={(e) => patchParams({ available: e.target.checked ? '1' : undefined })}
            >
              Có sẵn trên kệ
            </Checkbox>
            <div style={{ marginTop: 8 }}>
              <Checkbox checked={!availableOnly} onChange={() => patchParams({ available: undefined })}>
                Tất cả tài liệu
              </Checkbox>
            </div>
          </div>

          <div className="opac-side-block">
            <h4>Phân loại / Thể loại</h4>
            <Radio.Group
              value={categoryId || ''}
              onChange={(e) =>
                patchParams({
                  category: e.target.value || undefined,
                  categoryId: undefined,
                })
              }
              style={{ display: 'grid', gap: 6 }}
            >
              <Radio value="">Tất cả danh mục</Radio>
              {categories.slice(0, 12).map((c) => (
                <Radio key={c.id} value={String(c.id)}>
                  {c.code ? `${c.code} · ` : ''}
                  {c.name}
                </Radio>
              ))}
            </Radio.Group>
          </div>

          <div className="opac-side-block">
            <h4>Năm xuất bản</h4>
            <div className="opac-year-pills">
              {yearOptions.map((y) => (
                <button
                  key={y.value}
                  type="button"
                  className={year === y.value ? 'active' : undefined}
                  onClick={() =>
                    patchParams({ year: year === y.value ? undefined : y.value })
                  }
                >
                  {y.label}
                </button>
              ))}
            </div>
          </div>

          {languages.length > 0 ? (
            <div className="opac-side-block">
              <h4>Ngôn ngữ tài liệu</h4>
              <Radio.Group
                value={languageId || ''}
                onChange={(e) => patchParams({ language: e.target.value || undefined })}
                style={{ display: 'grid', gap: 6 }}
              >
                <Radio value="">Tất cả</Radio>
                {languages.slice(0, 8).map((l) => (
                  <Radio key={l.id} value={String(l.id)}>
                    {l.name}
                  </Radio>
                ))}
              </Radio.Group>
            </div>
          ) : null}
        </aside>

        <div>
          {loading ? (
            <Skeleton active paragraph={{ rows: 10 }} />
          ) : rows.length === 0 ? (
            <Empty description="Không có kết quả phù hợp" />
          ) : (
            <div className={view === 'grid' ? 'opac-grid' : 'opac-list'}>
              {rows.map((book) => {
                const available = (book.availableCopies ?? 0) > 0;
                const inCart = hasItem(book.id);
                return (
                  <article key={book.id} className="opac-card">
                    <div className="opac-card-cover">
                      {book.coverUrl ? (
                        <img src={book.coverUrl} alt={book.title} />
                      ) : (
                        <span>{book.title}</span>
                      )}
                      <span className="ddc">{book.classificationCode || book.publishYear || 'OPAC'}</span>
                      <span className={`badge ${available ? 'ok' : 'out'}`}>
                        {available ? 'Có sẵn trên kệ' : 'Tạm hết'}
                      </span>
                    </div>
                    <div className="opac-card-body">
                      <Link to={`/books/${book.id}`}>
                        <h3>{book.title}</h3>
                      </Link>
                      <div className="meta">
                        {book.publisherName || '—'}
                        {book.publishYear ? ` · ${book.publishYear}` : ''}
                      </div>
                      <div className="meta">
                        <StarFilled style={{ color: '#f59e0b' }} /> {Number(book.avgRating || 0).toFixed(1)}
                        {' · '}
                        {book.borrowCount ?? 0} lượt mượn
                      </div>
                      <div className="stock" style={{ color: available ? '#16a34a' : '#dc2626' }}>
                        Tồn kho: {book.availableCopies ?? 0}
                        {book.totalCopies != null ? ` / ${book.totalCopies}` : ''} bản sẵn sàng
                      </div>
                      <div className="meta">ISBN: {book.isbn13 || '—'}</div>
                      <div className="actions">
                        {available ? (
                          <Button
                            type="primary"
                            icon={<ShoppingCartOutlined />}
                            style={{
                              background: '#0B1F3A',
                              color: '#fff',
                              borderColor: '#0B1F3A',
                              opacity: inCart ? 0.92 : 1,
                            }}
                            onClick={() => (inCart ? navigate('/borrow-cart') : addToCart(book))}
                          >
                            {inCart ? 'Đã trong giỏ' : 'Thêm vào giỏ mượn'}
                          </Button>
                        ) : (
                          <Button type="primary" onClick={() => navigate(`/books/${book.id}`)}>
                            Đặt trước (Reservation)
                          </Button>
                        )}
                        <Button icon={<EyeOutlined />} onClick={() => navigate(`/books/${book.id}`)} />
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>
          )}

          <div style={{ marginTop: 16, display: 'flex', justifyContent: 'center' }}>
            <Pagination
              current={meta.page + 1}
              pageSize={meta.limit}
              total={meta.total}
              onChange={(p) => {
                const next = new URLSearchParams(searchParams);
                next.set('page', String(p - 1));
                setSearchParams(next);
              }}
              showSizeChanger={false}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
