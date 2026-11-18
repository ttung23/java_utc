import type { ReactNode } from 'react';
import {
  BookOutlined,
  DashboardOutlined,
  DollarOutlined,
  FileTextOutlined,
  ReadOutlined,
  SettingOutlined,
  TeamOutlined,
  BarChartOutlined,
} from '@ant-design/icons';

export type AppMenuItem = {
  key: string;
  label: string;
  icon?: ReactNode;
  path?: string;
  roles?: Array<'ADMIN' | 'STAFF'>;
  children?: AppMenuItem[];
};

/** Menu theo SPEC 10.1 */
export const adminMenu: AppMenuItem[] = [
  { key: 'dashboard', label: 'Dashboard', icon: <DashboardOutlined />, path: '/' },
  {
    key: 'borrow',
    label: 'Mượn – Trả',
    icon: <ReadOutlined />,
    children: [
      { key: 'borrow-create', label: 'Tạo phiếu mượn (quầy)', path: '/borrows/create' },
      { key: 'returns', label: 'Trả sách', path: '/returns' },
      { key: 'borrow-online', label: 'Yêu cầu mượn online', path: '/borrows/online' },
      { key: 'borrow-list', label: 'Danh sách phiếu mượn', path: '/borrows' },
      { key: 'overdue', label: 'Sách quá hạn', path: '/borrows/overdue' },
      { key: 'reservations', label: 'Đặt trước', path: '/reservations' },
    ],
  },
  {
    key: 'fines',
    label: 'Phạt & Thu tiền',
    icon: <DollarOutlined />,
    children: [
      { key: 'fine-list', label: 'Khoản phạt', path: '/fines' },
      { key: 'payments', label: 'Biên lai thanh toán', path: '/payments' },
    ],
  },
  {
    key: 'readers',
    label: 'Độc giả',
    icon: <TeamOutlined />,
    children: [
      { key: 'reader-list', label: 'Danh sách độc giả', path: '/readers' },
      { key: 'cards', label: 'Thẻ thư viện', path: '/cards' },
      { key: 'reader-types', label: 'Loại độc giả', path: '/reader-types', roles: ['ADMIN'] },
    ],
  },
  {
    key: 'catalog',
    label: 'Kho sách',
    icon: <BookOutlined />,
    children: [
      { key: 'books', label: 'Đầu sách', path: '/books' },
      { key: 'copies', label: 'Bản sao & Barcode', path: '/copies' },
      { key: 'categories', label: 'Thể loại / Tác giả / NXB', path: '/catalog' },
      { key: 'locations', label: 'Vị trí kệ', path: '/locations' },
    ],
  },
  {
    key: 'content',
    label: 'Nội dung',
    icon: <FileTextOutlined />,
    children: [
      { key: 'news', label: 'Tin tức', path: '/news' },
      { key: 'banners', label: 'Banner', path: '/banners' },
    ],
  },
  { key: 'reports', label: 'Báo cáo & Thống kê', icon: <BarChartOutlined />, path: '/reports' },
  {
    key: 'system',
    label: 'Hệ thống',
    icon: <SettingOutlined />,
    roles: ['ADMIN'],
    children: [
      { key: 'staff', label: 'Nhân viên', path: '/staff', roles: ['ADMIN'] },
      { key: 'settings', label: 'Cấu hình', path: '/settings', roles: ['ADMIN'] },
      { key: 'holidays', label: 'Ngày nghỉ', path: '/holidays', roles: ['ADMIN'] },
      { key: 'audit', label: 'Nhật ký hoạt động', path: '/audit-logs', roles: ['ADMIN'] },
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
    .filter((item) => !item.children || item.children.length > 0 || item.path);
}
