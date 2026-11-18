/** Nhãn tiếng Việt cho mã trạng thái / vai trò từ API. */
export const STATUS_VI: Record<string, string> = {
  ACTIVE: 'Đang hoạt động',
  AVAILABLE: 'Sẵn sàng',
  BORROWED: 'Đang mượn',
  BORROWING: 'Đang mượn',
  RESERVED: 'Đặt trước',
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  READY: 'Sẵn sàng nhận',
  INACTIVE: 'Ngừng hoạt động',
  LOCKED: 'Đã khóa',
  EXPIRED: 'Hết hạn',
  LOST: 'Mất',
  DAMAGED: 'Hư hỏng',
  WITHDRAWN: 'Đã thanh lý',
  IN_REPAIR: 'Đang sửa',
  PAID: 'Đã thanh toán',
  UNPAID: 'Chưa thanh toán',
  RETURNED: 'Đã trả',
  CANCELLED: 'Đã hủy',
  OVERDUE: 'Quá hạn',
  SUSPENDED: 'Tạm khóa',
  REPLACED: 'Đã thay thế',
  HIDDEN: 'Ẩn',
  NORMAL: 'Bình thường',
};

export const ROLE_VI: Record<string, string> = {
  ADMIN: 'Quản trị viên',
  STAFF: 'Nhân viên',
  READER: 'Độc giả',
};

export const READER_TYPE_VI: Record<string, string> = {
  STUDENT: 'Sinh viên',
  LECTURER: 'Giảng viên',
  TEACHER: 'Giảng viên',
  STAFF: 'Cán bộ',
  EXTERNAL: 'Bên ngoài',
  PUBLIC: 'Công chúng',
};

export function statusVi(code?: string | null): string {
  if (!code) return '—';
  return STATUS_VI[code] || code;
}

export function roleVi(code?: string | null): string {
  if (!code) return '—';
  return ROLE_VI[code] || code;
}

export function readerTypeVi(codeOrName?: string | null): string {
  if (!codeOrName) return '—';
  return READER_TYPE_VI[codeOrName] || codeOrName;
}
