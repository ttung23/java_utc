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
  Tag,
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
  return items.map((item) => {
    if (item.type === 'group') {
      return {
        type: 'group',
        key: item.key,
        label: item.label.toUpperCase(),
        children: item.children ? toAntdItems(item.children) : undefined,
      };
    }
    return {
      key: item.path || item.key,
      icon: item.icon,
      label: item.path ? <Link to={item.path}>{item.label}</Link> : item.label,
      children: item.children ? toAntdItems(item.children) : undefined,
    };
  });
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
    <Layout style={{ minHeight: '100vh', background: '#F3F5F9' }}>
      <Sider
        collapsible
        collapsed={collapsed}
        trigger={null}
        width={248}
        style={{ background: '#0B1F3A' }}
      >
        <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
          <div
            style={{
              height: 64,
              padding: collapsed ? '0 8px' : '0 16px',
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              borderBottom: '1px solid rgba(255,255,255,0.08)',
            }}
          >
            <div
              style={{
                width: 34,
                height: 34,
                borderRadius: 8,
                background: 'linear-gradient(135deg,#1677ff,#69b1ff)',
                display: 'grid',
                placeItems: 'center',
                color: '#fff',
                fontWeight: 800,
                flexShrink: 0,
              }}
            >
              TV
            </div>
            {!collapsed && (
              <div style={{ lineHeight: 1.15, color: '#fff' }}>
                <div style={{ fontWeight: 800, fontSize: 13, letterSpacing: 0.4 }}>THƯ VIỆN SỐ</div>
                <div style={{ fontSize: 11, opacity: 0.65 }}>Portal quản trị</div>
              </div>
            )}
          </div>

          <div style={{ flex: 1, overflow: 'auto' }}>
            <Menu
              theme="dark"
              mode="inline"
              selectedKeys={[selectedKey]}
              items={menuItems}
              style={{ background: 'transparent', borderInlineEnd: 0, paddingTop: 8 }}
            />
          </div>

          {!collapsed && (
            <div
              style={{
                padding: '12px 16px',
                borderTop: '1px solid rgba(255,255,255,0.08)',
                color: 'rgba(255,255,255,0.75)',
                fontSize: 12,
              }}
            >
              <Space>
                <span
                  style={{
                    width: 8,
                    height: 8,
                    borderRadius: '50%',
                    background: '#52c41a',
                    display: 'inline-block',
                  }}
                />
                Hệ thống sẵn sàng
              </Space>
              <div style={{ marginTop: 4, opacity: 0.55 }}>Phiên bản 2.4.0</div>
            </div>
          )}
        </div>
      </Sider>

      <Layout>
        <Header
          style={{
            background: '#fff',
            padding: '0 20px',
            height: 60,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: `1px solid ${tokens.color.border}`,
            gap: 12,
          }}
        >
          <Space size="middle" style={{ flex: 1, minWidth: 0 }}>
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
              placeholder="Tìm nhanh sách, độc giả, mã vạch…"
              style={{ width: 420, maxWidth: '42vw' }}
            />
          </Space>
          <Space size="middle">
            <Tag color="processing" style={{ margin: 0 }}>
              Quầy 01 — Đang mở
            </Tag>
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
                    {user?.role === 'ADMIN' ? 'Quản trị viên' : 'Thủ thư'}
                  </Typography.Text>
                </div>
              </Space>
            </Dropdown>
          </Space>
        </Header>
        <Content style={{ margin: 16, minHeight: 280 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
