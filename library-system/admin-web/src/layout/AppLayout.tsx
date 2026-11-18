import { useMemo, useState } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  Avatar,
  Badge,
  Dropdown,
  Input,
  Layout,
  Menu,
  Space,
  theme,
  Typography,
} from 'antd';
import type { MenuProps } from 'antd';
import {
  BellOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SearchOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useAuth } from '@/auth/AuthContext';
import { adminMenu, filterMenuByRole, type AppMenuItem } from '@/layout/menuItems';
import { tokens } from '@/styles/tokens';

const { Header, Sider, Content } = Layout;

type MenuItem = NonNullable<MenuProps['items']>[number];

function toAntdItems(items: AppMenuItem[]): MenuItem[] {
  return items.map((item) => ({
    key: item.path || item.key,
    icon: item.icon,
    label: item.path ? <Link to={item.path}>{item.label}</Link> : item.label,
    children: item.children ? toAntdItems(item.children) : undefined,
  }));
}

export function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const { token } = theme.useToken();

  const menuItems = useMemo(() => {
    const role = user?.role === 'ADMIN' ? 'ADMIN' : 'STAFF';
    return toAntdItems(filterMenuByRole(adminMenu, role));
  }, [user?.role]);

  const selectedKey = location.pathname === '/' ? '/' : location.pathname;

  return (
    <Layout style={{ minHeight: '100vh', background: tokens.color.bg }}>
      <Sider
        collapsible
        collapsed={collapsed}
        trigger={null}
        width={tokens.layout.sidebarWidth}
        style={{ background: '#001529' }}
      >
        <div
          style={{
            height: tokens.layout.topbarHeight,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontWeight: 700,
            letterSpacing: 0.3,
            borderBottom: '1px solid rgba(255,255,255,0.08)',
          }}
        >
          {collapsed ? 'TV' : 'Thư viện UTC'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          defaultOpenKeys={['borrow', 'catalog', 'readers']}
          items={menuItems}
          style={{ borderInlineEnd: 0 }}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: tokens.color.surface,
            padding: '0 20px',
            height: tokens.layout.topbarHeight,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: `1px solid ${tokens.color.border}`,
          }}
        >
          <Space>
            <button
              type="button"
              onClick={() => setCollapsed((v) => !v)}
              style={{
                border: 0,
                background: 'transparent',
                cursor: 'pointer',
                fontSize: 18,
                color: token.colorText,
              }}
              aria-label="Thu gọn menu"
            >
              {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            </button>
            <Input
              allowClear
              prefix={<SearchOutlined />}
              placeholder="Tìm sách / độc giả / phiếu (Ctrl+K)"
              style={{ width: 360, maxWidth: '40vw' }}
            />
          </Space>
          <Space size="middle">
            <Badge count={0} size="small">
              <BellOutlined style={{ fontSize: 18 }} />
            </Badge>
            <Dropdown
              menu={{
                items: [
                  {
                    key: 'profile',
                    icon: <UserOutlined />,
                    label: 'Hồ sơ',
                    onClick: () => navigate('/profile'),
                  },
                  {
                    key: 'logout',
                    icon: <LogoutOutlined />,
                    label: 'Đăng xuất',
                    onClick: () => void logout().then(() => navigate('/login')),
                  },
                ],
              }}
            >
              <Space style={{ cursor: 'pointer' }}>
                <Avatar style={{ backgroundColor: tokens.color.primary }} icon={<UserOutlined />} />
                <div style={{ lineHeight: 1.2 }}>
                  <Typography.Text strong>{user?.fullName}</Typography.Text>
                  <br />
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {user?.role}
                  </Typography.Text>
                </div>
              </Space>
            </Dropdown>
          </Space>
        </Header>
        <Content style={{ margin: 20 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
