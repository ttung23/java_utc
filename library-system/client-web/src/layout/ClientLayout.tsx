import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Button, Layout, Menu, Space, Typography } from 'antd';
import {
  BookOutlined,
  BellOutlined,
  HeartOutlined,
  HomeOutlined,
  LoginOutlined,
  LogoutOutlined,
  ReadOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useAuth } from '@/auth/AuthContext';

const { Header, Content, Footer } = Layout;

export function ClientLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const selected = location.pathname.startsWith('/books')
    ? '/books'
    : location.pathname.startsWith('/me')
      ? location.pathname
      : location.pathname;

  return (
    <Layout style={{ minHeight: '100vh', background: '#f8fafc' }}>
      <Header
        style={{
          background: '#0b1f3a',
          display: 'flex',
          alignItems: 'center',
          gap: 24,
          paddingInline: 24,
        }}
      >
        <Typography.Title level={4} style={{ color: '#fff', margin: 0, whiteSpace: 'nowrap' }}>
          <Link to="/" style={{ color: '#fff' }}>
            Thư viện UTC
          </Link>
        </Typography.Title>
        <Menu
          theme="dark"
          mode="horizontal"
          selectedKeys={[selected]}
          style={{ flex: 1, background: 'transparent', minWidth: 0 }}
          items={[
            { key: '/', icon: <HomeOutlined />, label: <Link to="/">Trang chủ</Link> },
            { key: '/books', icon: <BookOutlined />, label: <Link to="/books">Tra cứu</Link> },
            ...(user
              ? [
                  {
                    key: '/me/borrows',
                    icon: <ReadOutlined />,
                    label: <Link to="/me/borrows">Mượn</Link>,
                  },
                  {
                    key: '/me/reservations',
                    label: <Link to="/me/reservations">Đặt trước</Link>,
                  },
                  {
                    key: '/me/fines',
                    label: <Link to="/me/fines">Phạt</Link>,
                  },
                  {
                    key: '/me/favorites',
                    icon: <HeartOutlined />,
                    label: <Link to="/me/favorites">Yêu thích</Link>,
                  },
                  {
                    key: '/me/notifications',
                    icon: <BellOutlined />,
                    label: <Link to="/me/notifications">Thông báo</Link>,
                  },
                ]
              : []),
          ]}
        />
        <Space>
          {user ? (
            <>
              <Button type="text" style={{ color: '#fff' }} icon={<UserOutlined />}>
                {user.fullName}
              </Button>
              <Button
                type="default"
                icon={<LogoutOutlined />}
                onClick={() => void logout().then(() => navigate('/'))}
              >
                Thoát
              </Button>
            </>
          ) : (
            <Button type="primary" icon={<LoginOutlined />} onClick={() => navigate('/login')}>
              Đăng nhập
            </Button>
          )}
        </Space>
      </Header>
      <Content style={{ padding: '24px 16px', maxWidth: 1100, width: '100%', margin: '0 auto' }}>
        <Outlet />
      </Content>
      <Footer style={{ textAlign: 'center' }}>Thư viện UTC · Cổng độc giả</Footer>
    </Layout>
  );
}
