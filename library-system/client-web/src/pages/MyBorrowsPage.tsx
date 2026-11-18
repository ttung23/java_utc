import { useEffect, useMemo, useState } from 'react';
import {
  CheckCircleFilled,
  ClockCircleOutlined,
  ExclamationCircleFilled,
  ReadOutlined,
  ReloadOutlined,
  ShoppingCartOutlined,
} from '@ant-design/icons';
import {
  Breadcrumb,
  Button,
  Empty,
  Pagination,
  Skeleton,
  Tabs,
  Tag,
  message,
} from 'antd';
import dayjs from 'dayjs';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/AuthContext';
import { api, type ApiResponse } from '@/services/api';

type SlipItem = {
  id: number;
  bookId?: number;
  bookTitle?: string;
  barcode?: string;
  dueDate?: string;
  status: string;
  overdue?: boolean;
  renewCount?: number;
  estimatedFine?: number;
};

type Slip = {
  id: number;
  slipCode: string;
  status: string;
  channel?: string;
  overdue?: boolean;
  createdAt?: string;
  requestedAt?: string;
  approvedAt?: string;
  borrowedAt?: string;
  pickupDeadline?: string;
  note?: string;
  rejectedReason?: string;
  items?: SlipItem[];
};

type Meta = { page: number; limit: number; total: number };
type TabKey = 'all' | 'request' | 'borrowing' | 'overdue' | 'returned' | 'closed';

function statusMeta(status: string) {
  const s = String(status || '').toUpperCase();
  switch (s) {
    case 'PENDING':
      return { label: 'Chờ duyệt', color: 'processing' as const };
    case 'APPROVED':
      return { label: 'Chờ nhận sách', color: 'blue' as const };
    case 'BORROWING':
      return { label: 'Đang mượn', color: 'success' as const };
    case 'PARTIALLY_RETURNED':
      return { label: 'Trả một phần', color: 'warning' as const };
    case 'RETURNED':
      return { label: 'Đã trả', color: 'default' as const };
    case 'REJECTED':
      return { label: 'Từ chối', color: 'error' as const };
    case 'CANCELLED':
      return { label: 'Đã hủy', color: 'default' as const };
    case 'EXPIRED':
      return { label: 'Hết hạn giữ', color: 'error' as const };
    default:
      return { label: status || '—', color: 'default' as const };
  }
}

function itemStatusLabel(status: string) {
  const s = String(status || '').toUpperCase();
  if (s === 'BORROWING') return 'Đang mượn';
  if (s === 'RETURNED') return 'Đã trả';
  if (s === 'PENDING' || s === 'HOLD') return 'Giữ chỗ';
  return status || '—';
}

function formatVnd(n?: number) {
  if (n == null || Number.isNaN(Number(n))) return '0đ';
  return `${Math.round(Number(n)).toLocaleString('vi-VN')}đ`;
}

function dueHint(dueDate?: string, overdue?: boolean) {
  if (!dueDate) return 'Chưa có hạn trả';
  const d = dayjs(dueDate);
  const left = d.startOf('day').diff(dayjs().startOf('day'), 'day');
  if (overdue || left < 0) return `Quá hạn ${Math.abs(left)} ngày`;
  if (left === 0) return 'Hết hạn hôm nay';
  if (left <= 3) return `Còn ${left} ngày — sắp đến hạn`;
  return `Còn ${left} ngày`;
}

export function MyBorrowsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [rows, setRows] = useState<Slip[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 10, total: 0 });
  const [loading, setLoading] = useState(false);
  const [tab, setTab] = useState<TabKey>('all');
  const [actingId, setActingId] = useState<number | null>(null);

  const load = async (page = 0) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Slip[]>>('/borrows/me', {
        params: { page, size: meta.limit },
      });
      setRows(data.data || []);
      const m = data.meta as Meta | undefined;
      setMeta({
        page: m?.page ?? page,
        limit: m?.limit ?? 10,
        total: m?.total ?? data.data?.length ?? 0,
      });
    } catch {
      message.error('Không tải được phiếu mượn');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const filtered = useMemo(() => {
    return rows.filter((r) => {
      const s = String(r.status).toUpperCase();
      const hasOverdueItem = Boolean(r.overdue) || (r.items || []).some((i) => i.overdue);
      switch (tab) {
        case 'request':
          return s === 'PENDING' || s === 'APPROVED';
        case 'borrowing':
          return s === 'BORROWING' || s === 'PARTIALLY_RETURNED';
        case 'overdue':
          return hasOverdueItem || s === 'EXPIRED';
        case 'returned':
          return s === 'RETURNED';
        case 'closed':
          return s === 'CANCELLED' || s === 'REJECTED' || s === 'EXPIRED';
        default:
          return true;
      }
    });
  }, [rows, tab]);

  const stats = useMemo(() => {
    const request = rows.filter((r) => ['PENDING', 'APPROVED'].includes(String(r.status).toUpperCase())).length;
    const borrowing = rows.filter((r) =>
      ['BORROWING', 'PARTIALLY_RETURNED'].includes(String(r.status).toUpperCase()),
    ).length;
    const overdue = rows.filter(
      (r) => r.overdue || (r.items || []).some((i) => i.overdue),
    ).length;
    const returned = rows.filter((r) => String(r.status).toUpperCase() === 'RETURNED').length;
    return { request, borrowing, overdue, returned, total: meta.total };
  }, [rows, meta.total]);

  const cancel = async (id: number) => {
    setActingId(id);
    try {
      await api.post(`/borrows/${id}/cancel`, {});
      message.success('Đã hủy yêu cầu');
      void load(meta.page);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Hủy thất bại');
    } finally {
      setActingId(null);
    }
  };

  const renew = async (itemId: number) => {
    setActingId(itemId);
    try {
      await api.post(`/borrow-items/${itemId}/renew`, {});
      message.success('Gia hạn thành công');
      void load(meta.page);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Gia hạn thất bại');
    } finally {
      setActingId(null);
    }
  };

  return (
    <div>
      <Breadcrumb
        style={{ marginBottom: 12 }}
        items={[
          { title: <Link to="/">Trang chủ</Link> },
          { title: 'Tài khoản & Hoạt động độc giả' },
          { title: 'Sách đang mượn & Yêu cầu' },
        ]}
      />

      <div className="home-section-head">
        <div>
          <div className="eyebrow">CỔNG ĐỘC GIẢ UTC</div>
          <h2 style={{ margin: 0, fontSize: 26, color: '#0b1f3a' }}>Sách đang mượn &amp; Yêu cầu mượn</h2>
          <p className="desc">
            {user?.fullName} · Theo dõi yêu cầu online, hạn trả, gia hạn và hủy phiếu chờ nhận.
          </p>
        </div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <Button icon={<ReloadOutlined />} onClick={() => void load(meta.page)}>
            Làm mới
          </Button>
          <Button onClick={() => navigate('/borrow-cart')} icon={<ShoppingCartOutlined />}>
            Giỏ mượn
          </Button>
          <Button
            type="primary"
            icon={<ReadOutlined />}
            style={{ background: '#0B1F3A', color: '#fff', borderColor: '#0B1F3A' }}
            onClick={() => navigate('/books')}
          >
            Tra cứu thêm sách
          </Button>
        </div>
      </div>

      <div className="home-stats" style={{ marginTop: 0, marginBottom: 16 }}>
        <div className="home-stat-card">
          <div className="icon" style={{ background: '#dbeafe', color: '#1d4ed8' }}>
            <ClockCircleOutlined />
          </div>
          <div className="num">{stats.request}</div>
          <div className="label">Yêu cầu chờ duyệt / nhận</div>
        </div>
        <div className="home-stat-card">
          <div className="icon" style={{ background: '#dcfce7', color: '#16a34a' }}>
            <ReadOutlined />
          </div>
          <div className="num">{stats.borrowing}</div>
          <div className="label">Đang mượn</div>
        </div>
        <div className="home-stat-card">
          <div className="icon" style={{ background: '#fee2e2', color: '#dc2626' }}>
            <ExclamationCircleFilled />
          </div>
          <div className="num">{stats.overdue}</div>
          <div className="label">Có sách quá hạn</div>
        </div>
        <div className="home-stat-card">
          <div className="icon" style={{ background: '#f1f5f9', color: '#475569' }}>
            <CheckCircleFilled />
          </div>
          <div className="num">{stats.returned}</div>
          <div className="label">Đã trả (trang này)</div>
        </div>
      </div>

      <div className="home-panel">
        <Tabs
          activeKey={tab}
          onChange={(k) => setTab(k as TabKey)}
          items={[
            { key: 'all', label: `Tất cả (${rows.length})` },
            { key: 'request', label: `Yêu cầu (${stats.request})` },
            { key: 'borrowing', label: `Đang mượn (${stats.borrowing})` },
            { key: 'overdue', label: `Quá hạn (${stats.overdue})` },
            { key: 'returned', label: `Đã trả (${stats.returned})` },
            { key: 'closed', label: 'Đã hủy / từ chối' },
          ]}
        />

        {loading ? (
          <Skeleton active paragraph={{ rows: 8 }} />
        ) : filtered.length === 0 ? (
          <Empty description="Không có phiếu trong mục này">
            <Button type="primary" style={{ background: '#0B1F3A', color: '#fff' }} onClick={() => navigate('/books')}>
              Đi tra cứu OPAC
            </Button>
          </Empty>
        ) : (
          <div style={{ display: 'grid', gap: 12 }}>
            {filtered.map((slip) => {
              const st = statusMeta(slip.status);
              const canCancel = ['PENDING', 'APPROVED'].includes(String(slip.status).toUpperCase());
              const pickupLeft =
                slip.pickupDeadline != null
                  ? dayjs(slip.pickupDeadline).diff(dayjs(), 'hour')
                  : null;

              return (
                <article key={slip.id} className="borrow-card">
                  <div className="borrow-card-head">
                    <div>
                      <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
                        <strong style={{ fontSize: 16, color: '#0b1f3a' }}>{slip.slipCode}</strong>
                        <Tag color={st.color}>{st.label}</Tag>
                        {slip.channel ? <Tag>{slip.channel}</Tag> : null}
                        {slip.overdue ? <Tag color="error">Có quá hạn</Tag> : null}
                      </div>
                      <div className="meta">
                        Tạo: {slip.createdAt ? dayjs(slip.createdAt).format('DD/MM/YYYY HH:mm') : '—'}
                        {slip.pickupDeadline
                          ? ` · Hạn nhận: ${dayjs(slip.pickupDeadline).format('DD/MM/YYYY HH:mm')}`
                          : ''}
                        {pickupLeft != null && canCancel
                          ? pickupLeft >= 0
                            ? ` · Còn ~${pickupLeft}h giữ chỗ`
                            : ' · Đã quá hạn nhận'
                          : ''}
                      </div>
                      {slip.note ? <div className="meta">Ghi chú: {slip.note}</div> : null}
                      {slip.rejectedReason ? (
                        <div className="meta" style={{ color: '#dc2626' }}>
                          Lý do: {slip.rejectedReason}
                        </div>
                      ) : null}
                    </div>
                    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                      {canCancel ? (
                        <Button
                          danger
                          loading={actingId === slip.id}
                          onClick={() => void cancel(slip.id)}
                        >
                          Hủy yêu cầu
                        </Button>
                      ) : null}
                    </div>
                  </div>

                  <div className="borrow-items">
                    {(slip.items || []).length === 0 ? (
                      <div className="meta">Chưa có dòng sách trên phiếu.</div>
                    ) : (
                      (slip.items || []).map((item) => {
                        const canRenew =
                          String(item.status).toUpperCase() === 'BORROWING' &&
                          ['BORROWING', 'PARTIALLY_RETURNED'].includes(String(slip.status).toUpperCase());
                        return (
                          <div key={item.id} className="borrow-item">
                            <div style={{ flex: 1, minWidth: 0 }}>
                              {item.bookId ? (
                                <Link to={`/books/${item.bookId}`}>
                                  <strong>{item.bookTitle || `Sách #${item.bookId}`}</strong>
                                </Link>
                              ) : (
                                <strong>{item.bookTitle || '—'}</strong>
                              )}
                              <div className="meta">
                                {itemStatusLabel(item.status)}
                                {item.barcode ? ` · ${item.barcode}` : ''}
                                {item.renewCount != null ? ` · Đã gia hạn ${item.renewCount} lần` : ''}
                              </div>
                              <div
                                className="meta"
                                style={{
                                  color: item.overdue ? '#dc2626' : '#64748b',
                                  fontWeight: item.overdue ? 700 : 400,
                                }}
                              >
                                Hạn trả: {item.dueDate ? dayjs(item.dueDate).format('DD/MM/YYYY') : '—'}
                                {' · '}
                                {dueHint(item.dueDate, item.overdue)}
                                {item.overdue ? ` · Phạt dự kiến ${formatVnd(Number(item.estimatedFine || 0))}` : ''}
                              </div>
                            </div>
                            {canRenew ? (
                              <Button
                                type="primary"
                                style={{ background: '#0B1F3A', color: '#fff', borderColor: '#0B1F3A' }}
                                loading={actingId === item.id}
                                onClick={() => void renew(item.id)}
                              >
                                Gia hạn
                              </Button>
                            ) : null}
                          </div>
                        );
                      })
                    )}
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
            showSizeChanger={false}
            onChange={(p) => void load(p - 1)}
          />
        </div>
      </div>
    </div>
  );
}
