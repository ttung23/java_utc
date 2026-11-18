import type { ReactNode } from 'react';
import {
  AuditOutlined,
  BankOutlined,
  BarChartOutlined,
  BookOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  DollarOutlined,
  ReadOutlined,
  SettingOutlined,
  TeamOutlined,
} from '@ant-design/icons';

export type AppMenuItem = {
  key: string;
  label: string;
  icon?: ReactNode;
  path?: string;
  type?: 'group' | 'item';
  roles?: Array<'ADMIN' | 'STAFF'>;
  children?: AppMenuItem[];
};

/** Menu bám UI portal quản trị (ảnh mock). */
export const adminMenu: AppMenuItem[] = [
  {
    key: 'grp-overview',
    label: 'Tổng quan',
    type: 'group',
    children: [
      { key: 'dashboard', label: 'Bảng điều khiển', icon: <DashboardOutlined />, path: '/' },
    ],
  },
  {
    key: 'grp-borrow',
    label: 'Mượn – Trả',
    type: 'group',
    children: [
      { key: 'borrow-create', label: 'Tạo phiếu mượn (quầy)', icon: <ReadOutlined />, path: '/borrows/create' },
      { key: 'returns', label: 'Trả sách & xử lý phạt', path: '/returns' },
      { key: 'borrow-online', label: 'Xử lý yêu cầu mượn trực tuyến', path: '/borrows/online' },
      { key: 'borrow-list', label: 'Danh sách phiếu lưu thông', path: '/borrows' },
      { key: 'overdue', label: 'Hồ sơ trả quá hạn', path: '/borrows/overdue' },
      { key: 'reservations', label: 'Đặt trước', path: '/reservations' },
    ],
  },
  {
    key: 'grp-catalog',
    label: 'Quản lý sách & kho',
    type: 'group',
    children: [
      { key: 'books', label: 'Quản lý đầu sách', icon: <BookOutlined />, path: '/books' },
      { key: 'copies', label: 'Bản sao & mã vạch', path: '/copies' },
      { key: 'catalog', label: 'Danh mục & kệ sách', path: '/catalog' },
      { key: 'locations', label: 'Vị trí kệ', path: '/locations' },
      { key: 'receipts', label: 'Nhập sách', icon: <DatabaseOutlined />, path: '/inventory/receipts' },
      { key: 'stocktake', label: 'Kiểm kê kho', icon: <AuditOutlined />, path: '/inventory/stocktake' },
    ],
  },
  {
    key: 'grp-reader-finance',
    label: 'Độc giả & tài chính',
    type: 'group',
    children: [
      { key: 'readers', label: 'Độc giả & thẻ', icon: <TeamOutlined />, path: '/readers' },
      { key: 'cards', label: 'Thẻ thư viện', path: '/cards' },
      { key: 'fines', label: 'Phạt & thu tiền', icon: <DollarOutlined />, path: '/fines' },
      { key: 'payments', label: 'Biên lai thanh toán', path: '/payments' },
    ],
  },
  {
    key: 'grp-admin',
    label: 'Quản trị',
    type: 'group',
    children: [
      { key: 'reports', label: 'Báo cáo & thống kê', icon: <BarChartOutlined />, path: '/reports' },
      { key: 'settings', label: 'Cài đặt hệ thống', icon: <SettingOutlined />, path: '/settings', roles: ['ADMIN'] },
      { key: 'staff', label: 'Nhân viên', icon: <BankOutlined />, path: '/staff', roles: ['ADMIN'] },
      { key: 'holidays', label: 'Ngày nghỉ', path: '/holidays', roles: ['ADMIN'] },
      { key: 'audit', label: 'Nhật ký hoạt động', path: '/audit-logs', roles: ['ADMIN'] },
      { key: 'reader-types', label: 'Loại độc giả', path: '/reader-types', roles: ['ADMIN'] },
      { key: 'news', label: 'Tin tức', path: '/news' },
      { key: 'banners', label: 'Ảnh banner', path: '/banners' },
    ],
  },
];

export function filterMenuByRole(items: AppMenuItem[], role: 'ADMIN' | 'STAFF'): AppMenuItem[] {
  return items
    .filter((item) => !item.roles || item.roles.includes(role))
    .map((item) => ({
      ...item,
      children: item.children ? filterMenuByRole(item.children, role) : undefined,
    }))
    .filter((item) => {
      if (item.type === 'group') return (item.children?.length ?? 0) > 0;
      return !item.children || item.children.length > 0 || !!item.path;
    });
}
