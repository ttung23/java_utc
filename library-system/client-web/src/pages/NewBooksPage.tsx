import { useEffect, useState } from 'react';
import { CheckCircleFilled, EyeOutlined, ShoppingCartOutlined } from '@ant-design/icons';
import { Button, Empty, Skeleton, message } from 'antd';
import { Link, useNavigate } from 'react-router-dom';
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
};

const BADGES = ['MỚI VỀ', 'BESTSELLER', 'BẢN 2024', 'GIÁO TRÌNH', 'HOT PICK'];

export function NewBooksPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [books, setBooks] = useState<Book[]>([]);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const { data } = await api.get<ApiResponse<Book[]>>('/books/new');
        setBooks(data.data || []);
      } catch {
        message.error('Không tải được sách mới');
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, []);

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

  return (
    <div>
      <div className="home-section-head">
        <div>
          <div className="eyebrow">BỔ SUNG HẰNG TUẦN</div>
          <h2 style={{ margin: 0, fontSize: 28, color: '#0b1f3a' }}>Sách mới cập nhật</h2>
          <p className="desc">
            Tài liệu mới nhập kho theo yêu cầu bộ môn &amp; viện đào tạo Đại học Giao thông vận tải.
          </p>
        </div>
        <Link className="link" to="/books">
          Tra cứu toàn bộ danh mục
        </Link>
      </div>

      {loading ? (
        <Skeleton active paragraph={{ rows: 8 }} />
      ) : books.length === 0 ? (
        <Empty description="Chưa có sách mới" />
      ) : (
        <div className="home-book-grid">
          {books.map((book, idx) => (
            <article key={book.id} className="home-book-card">
              <div className="home-book-cover">
                <span className="home-book-badge">{BADGES[idx % BADGES.length]}</span>
                {book.coverUrl ? (
                  <img src={book.coverUrl} alt={book.title} />
                ) : (
                  book.title
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
    </div>
  );
}
