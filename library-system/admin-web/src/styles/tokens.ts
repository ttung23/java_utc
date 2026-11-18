/** Design tokens — SPEC 10.1 (chưa có ảnh UI, theo mô tả trạng thái). */
export const tokens = {
  color: {
    primary: '#1677ff',
    success: '#52c41a',
    warning: '#faad14',
    danger: '#ff4d4f',
    info: '#1677ff',
    borrowing: '#1677ff',
    available: '#52c41a',
    pending: '#faad14',
    overdue: '#ff4d4f',
    closed: '#8c8c8c',
    bg: '#f5f7fb',
    surface: '#ffffff',
    border: '#e5e7eb',
    text: '#1f2937',
    textSecondary: '#6b7280',
  },
  layout: {
    sidebarWidth: 240,
    sidebarCollapsedWidth: 80,
    topbarHeight: 56,
  },
  radius: {
    sm: 6,
    md: 8,
    lg: 12,
  },
} as const;

export const statusColorMap: Record<string, string> = {
  ACTIVE: tokens.color.success,
  AVAILABLE: tokens.color.available,
  PAID: tokens.color.success,
  PENDING: tokens.color.pending,
  INACTIVE: tokens.color.closed,
  LOCKED: tokens.color.overdue,
  APPROVED: tokens.color.pending,
  BORROWING: tokens.color.borrowing,
  OVERDUE: tokens.color.overdue,
  UNPAID: tokens.color.overdue,
  LOST: tokens.color.overdue,
  RETURNED: tokens.color.closed,
  CANCELLED: tokens.color.closed,
  EXPIRED: tokens.color.closed,
  HIDDEN: tokens.color.closed,
};
