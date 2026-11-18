import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Badge, Button, Dropdown, Input, Space } from 'antd';
import {
  BellOutlined,
  BookOutlined,
  LoginOutlined,
  LogoutOutlined,
  ReadOutlined,
  SearchOutlined,
  ShoppingCartOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useAuth } from '@/auth/AuthContext';
import { useBorrowCart } from '@/cart/BorrowCartContext';
import { FloatingCartBar } from '@/components/FloatingCartBar';
import '@/styles/client-shell.css';

const NAV = [
  { to: '/', label: 'Trang chủ', match: (p: string) => p === '/' },
  { to: '/books', label: 'Tra cứu & Danh mục', match: (p: string) => p.startsWith('/books') },
  { to: '/sach-moi', label: 'Sách mới', match: (p: string) => p === '/sach-moi' },
  { to: '/news', label: 'Tin tức & Hoạt động', match: (p: string) => p.startsWith('/news') },
  { to: '/rules', label: 'Quy định mượn trả', match: (p: string) => p === '/rules' },
  { to: '/about', label: 'Giới thiệu', match: (p: string) => p === '/about' || p === '/contact' },
];

export function ClientLayout() {
  const { user, logout } = useAuth();
  const { count } = useBorrowCart();
  const navigate = useNavigate();
  const location = useLocation();

  const userMenu = {
    items: [
      { key: 'borrows', label: 'Phiếu mượn', onClick: () => navigate('/me/borrows') },
      { key: 'reservations', label: 'Đặt trước', onClick: () => navigate('/me/reservations') },
      { key: 'fines', label: 'Phạt', onClick: () => navigate('/me/fines') },
      { key: 'favorites', label: 'Yêu thích', onClick: () => navigate('/me/favorites') },
      { key: 'notifications', label: 'Thông báo', onClick: () => navigate('/me/notifications') },
      { type: 'divider' as const },
      {
        key: 'logout',
        icon: <LogoutOutlined />,
        label: 'Đăng xuất',
        onClick: () => void logout().then(() => navigate('/')),
      },
    ],
  };

  return (
    <div className="cl-shell">
      <header className="cl-header">
        <div className="cl-header-inner">
          <Link to="/" className="cl-brand">
            <span className="cl-brand-mark">
              <BookOutlined />
            </span>
            <span className="cl-brand-text">
              <strong>THƯ VIỆN SỐ</strong>
              <span>Đại học Giao thông vận tải</span>
            </span>
          </Link>

          <nav className="cl-nav" aria-label="Điều hướng chính">
            {NAV.map((item) => (
              <Link
                key={item.to}
                to={item.to}
                className={item.match(location.pathname) ? 'active' : undefined}
              >
                {item.label}
              </Link>
            ))}
          </nav>

          <div className="cl-header-actions">
            <Input
              className="cl-header-search"
              allowClear
              size="middle"
              prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
              placeholder="Tìm tài liệu, ISBN..."
              suffix={<span style={{ color: '#94a3b8', fontSize: 11 }}>⌘K</span>}
              onPressEnter={(e) => {
                const q = (e.target as HTMLInputElement).value.trim();
                navigate(q ? `/books?q=${encodeURIComponent(q)}` : '/books');
              }}
            />

            <Badge count={count} size="small" color="#2563eb">
              <Button
                type="text"
                icon={<ShoppingCartOutlined style={{ fontSize: 18 }} />}
                onClick={() => navigate(user ? '/borrow-cart' : '/login')}
                aria-label="Giỏ mượn"
              />
            </Badge>
            <Badge dot={Boolean(user)}>
              <Button
                type="text"
                icon={<BellOutlined style={{ fontSize: 18 }} />}
                onClick={() => navigate(user ? '/me/notifications' : '/login')}
                aria-label="Thông báo"
              />
            </Badge>

            {user ? (
              <Dropdown menu={userMenu} placement="bottomRight">
                <button type="button" className="cl-user-chip" style={{ cursor: 'pointer' }}>
                  <span className="avatar">{user.fullName.charAt(0).toUpperCase()}</span>
                  <span className="meta">
                    <strong>{user.fullName}</strong>
                    <span>
                      {user.email.split('@')[0]} ·{' '}
                      <span style={{ color: '#16a34a' }}>Active</span>
                    </span>
                  </span>
                </button>
              </Dropdown>
            ) : (
              <Space>
                <Button icon={<LoginOutlined />} onClick={() => navigate('/login')}>
                  Đăng nhập
                </Button>
                <Button type="primary" icon={<UserOutlined />} onClick={() => navigate('/register')}>
                  Đăng ký
                </Button>
              </Space>
            )}
          </div>
        </div>
      </header>

      <main className="cl-main">
        <Outlet />
      </main>
      <FloatingCartBar />

      <footer className="cl-footer">
        <div className="cl-footer-inner">
          <div>
            <div className="cl-brand" style={{ marginBottom: 12 }}>
              <span className="cl-brand-mark">
                <BookOutlined />
              </span>
              <span className="cl-brand-text">
                <strong>THƯ VIỆN SỐ</strong>
                <span>Đại học Giao thông vận tải</span>
              </span>
            </div>
            <p>
              Hệ thống thư viện thông minh hỗ trợ tra cứu OPAC, mượn–trả trực tuyến và phục vụ học liệu
              số cho giảng viên, sinh viên UTC.
            </p>
            <p style={{ marginTop: 10, fontWeight: 600, color: '#0b1f3a' }}>ISO 9001:2015</p>
          </div>

          <div>
            <h4>Địa điểm & Thời gian</h4>
            <p>
              Cơ sở Hà Nội: Số 3 Cầu Giấy, Đống Đa, Hà Nội
              <br />
              Cơ sở TP.HCM: Số 450–451 Lê Văn Việt, TP. Thủ Đức
            </p>
            <p style={{ marginTop: 8 }}>
              Giờ mở cửa: <strong style={{ color: '#0f172a' }}>07:30 – 20:30</strong> (T2–T7)
            </p>
          </div>

          <div>
            <h4>Hỗ trợ độc giả</h4>
            <ul>
              <li>
                <Link to="/rules">Quy định mượn trả</Link>
              </li>
              <li>
                <Link to="/books">Tra cứu &amp; danh mục</Link>
              </li>
              <li>
                <Link to="/sach-moi">Sách mới</Link>
              </li>
              <li>
                <Link to="/news">Tin tức &amp; hoạt động</Link>
              </li>
              <li>
                <Link to="/me/fines">Gia hạn &amp; tiền phạt</Link>
              </li>
            </ul>
          </div>

          <div>
            <h4>Liên hệ</h4>
            <p>
              Hotline: (028) 3835 4409 (ext 114)
              <br />
              Email: thuvienso@utc.edu.vn
              <br />
              Hỗ trợ online: 08:00 – 17:00 (T2–T6)
            </p>
            <p style={{ marginTop: 10 }}>
              <ReadOutlined /> <Link to="/contact">Gửi yêu cầu hỗ trợ</Link>
            </p>
          </div>
        </div>

        <div className="cl-footer-bottom">
          <span>© 2025 Thư viện số — Đại học Giao thông vận tải (UTC). Giữ mọi quyền.</span>
          <div className="links">
            <Link to="/about">Giới thiệu</Link>
            <Link to="/rules">Nội quy</Link>
            <Link to="/contact">Liên hệ</Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
