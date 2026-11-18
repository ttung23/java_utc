import { useCallback, useEffect, useMemo, useState, type CSSProperties } from 'react';
import {
  Alert,
  Button,
  Col,
  Input,
  Row,
  Space,
  Tag,
  Typography,
  message,
} from 'antd';
import {
  ClockCircleOutlined,
  PrinterOutlined,
  ReloadOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import { api, type ApiResponse } from '@/services/api';
import { tokens } from '@/styles/tokens';
import { statusVi } from '@/utils/viLabels';

type Reservation = {
  id: number;
  code?: string;
  readerId?: number;
  readerName?: string;
  readerCode?: string;
  bookId?: number;
  bookTitle?: string;
  status: string;
  queuePosition?: number;
  assignedCopyId?: number;
  barcode?: string;
  locationCode?: string;
  readyAt?: string;
  readyUntil?: string;
  createdAt?: string;
};

type Meta = { page: number; limit: number; total: number };

const panel: CSSProperties = {
  background: '#fff',
  borderRadius: 12,
  border: `1px solid ${tokens.color.border}`,
  boxShadow: '0 1px 2px rgba(15,23,42,0.04)',
};

function statusTag(status: string, readyUntil?: string) {
  if (status === 'READY' && readyUntil) {
    const hours = dayjs(readyUntil).diff(dayjs(), 'hour', true);
    if (hours >= 0 && hours < 12) {
      return <Tag color="error">Sắp hết hạn giữ</Tag>;
    }
    return <Tag color="success">Sẵn sàng nhận</Tag>;
  }
  if (status === 'READY') return <Tag color="success">Sẵn sàng nhận</Tag>;
  if (status === 'WAITING') return <Tag color="processing">Đang chờ bản sao</Tag>;
  if (status === 'CANCELLED') return <Tag>Đã hủy</Tag>;
  if (status === 'EXPIRED') return <Tag color="default">Hết hạn giữ</Tag>;
  if (status === 'FULFILLED') return <Tag color="blue">Đã giao</Tag>;
  return <Tag>{statusVi(status)}</Tag>;
}

function remainLabel(readyUntil?: string): string | null {
  if (!readyUntil) return null;
  const end = dayjs(readyUntil);
  const now = dayjs();
  if (end.isBefore(now)) return 'Đã hết hạn giữ';
  const totalMin = end.diff(now, 'minute');
  const h = Math.floor(totalMin / 60);
  const m = totalMin % 60;
  return `Còn ${h}giờ ${String(m).padStart(2, '0')}phút`;
}

export function ReservationsPage() {
  const navigate = useNavigate();
  const [rows, setRows] = useState<Reservation[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 50, total: 0 });
  const [loading, setLoading] = useState(false);
  const [q, setQ] = useState('');
  const [tab, setTab] = useState<'ALL' | 'READY' | 'WAITING' | 'URGENT'>('ALL');
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [queue, setQueue] = useState<Reservation[]>([]);

  const load = useCallback(async (page = 0) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Reservation[]>>('/reservations', {
        params: { page, size: meta.limit },
      });
      const list = data.data || [];
      setRows(list);
      const m = data.meta as Meta | undefined;
      setMeta({
        page: m?.page ?? page,
        limit: m?.limit ?? meta.limit,
        total: m?.total ?? list.length,
      });
      setSelectedId((curr) => {
        if (curr != null && list.some((r) => r.id === curr)) return curr;
        return list[0]?.id ?? null;
      });
    } catch {
      message.error('Không tải được đặt trước');
    } finally {
      setLoading(false);
    }
  }, [meta.limit]);

  useEffect(() => {
    void load(0);
  }, [load]);

  const selected = useMemo(
    () => rows.find((r) => r.id === selectedId) || null,
    [rows, selectedId],
  );

  useEffect(() => {
    if (!selected?.bookId) {
      setQueue([]);
      return;
    }
    void api
      .get<ApiResponse<Reservation[]>>('/reservations/queue', {
        params: { bookId: selected.bookId },
      })
      .then((res) => setQueue(res.data.data || []))
      .catch(() => {
        // Fallback: lọc WAITING cùng sách từ list
        setQueue(
          rows
            .filter((r) => r.bookId === selected.bookId && r.status === 'WAITING')
            .sort((a, b) => (a.queuePosition || 0) - (b.queuePosition || 0)),
        );
      });
  }, [selected, rows]);

  const filtered = useMemo(() => {
    let list = rows;
    if (tab === 'READY') list = list.filter((r) => r.status === 'READY');
    if (tab === 'WAITING') list = list.filter((r) => r.status === 'WAITING');
    if (tab === 'URGENT') {
      list = list.filter((r) => {
        if (r.status !== 'READY' || !r.readyUntil) return false;
        const h = dayjs(r.readyUntil).diff(dayjs(), 'hour', true);
        return h >= 0 && h < 12;
      });
    }
    const keyword = q.trim().toLowerCase();
    if (!keyword) return list;
    return list.filter(
      (r) =>
        r.readerName?.toLowerCase().includes(keyword) ||
        r.bookTitle?.toLowerCase().includes(keyword) ||
        r.barcode?.toLowerCase().includes(keyword) ||
        String(r.id).includes(keyword) ||
        r.code?.toLowerCase().includes(keyword),
    );
  }, [rows, tab, q]);

  const kpis = useMemo(() => {
    const ready = rows.filter((r) => r.status === 'READY');
    const waiting = rows.filter((r) => r.status === 'WAITING');
    const urgent = ready.filter((r) => {
      if (!r.readyUntil) return false;
      const h = dayjs(r.readyUntil).diff(dayjs(), 'hour', true);
      return h >= 0 && h < 12;
    });
    const cancelledMonth = rows.filter((r) => {
      if (r.status !== 'CANCELLED' && r.status !== 'EXPIRED') return false;
      return dayjs(r.createdAt).isSame(dayjs(), 'month');
    });
    const titles = new Set(waiting.map((r) => r.bookId).filter(Boolean));
    return {
      ready: ready.length,
      waiting: waiting.length,
      waitingTitles: titles.size,
      urgent: urgent.length,
      cancelledMonth: cancelledMonth.length,
    };
  }, [rows]);

  const cancel = async (id: number) => {
    try {
      await api.post(`/reservations/${id}/cancel`);
      message.success('Đã hủy đặt trước / trả kho');
      void load(meta.page);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Hủy thất bại');
    }
  };

  const extend = async (id: number) => {
    try {
      await api.post(`/reservations/${id}/extend`, null, { params: { hours: 24 } });
      message.success('Đã gia hạn giữ chỗ 24 giờ');
      void load(meta.page);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Gia hạn thất bại');
    }
  };

  const fulfill = (r: Reservation) => {
    if (r.status !== 'READY' || !r.barcode) {
      message.warning('Chỉ giao khi đã sẵn sàng nhận và có mã vạch');
      return;
    }
    message.success('Chuyển sang tạo phiếu mượn quầy với bản sao đang giữ');
    navigate('/borrows/create', {
      state: {
        fromReservation: true,
        reservationId: r.id,
        readerHint: r.readerCode || r.readerName,
        barcodeHint: r.barcode,
      },
    });
  };

  const remain = selected ? remainLabel(selected.readyUntil) : null;

  return (
    <div style={{ maxWidth: 1480, margin: '0 auto' }}>
      <div
        style={{
          ...panel,
          padding: '12px 16px',
          marginBottom: 14,
          display: 'flex',
          justifyContent: 'space-between',
          gap: 12,
          flexWrap: 'wrap',
          alignItems: 'center',
        }}
      >
        <div>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            Mượn – Trả · Đặt trước
          </Typography.Text>
          <Typography.Title level={4} style={{ margin: '2px 0 0' }}>
            Hàng đợi đặt trước & giữ chỗ
          </Typography.Title>
        </div>
        <Space wrap>
          <Tag color="success">FIFO đang hoạt động</Tag>
          <Button icon={<ReloadOutlined />} loading={loading} onClick={() => void load(meta.page)}>
            Tải lại
          </Button>
        </Space>
      </div>

      <Row gutter={[12, 12]} style={{ marginBottom: 14 }}>
        {[
          {
            label: 'Sẵn sàng nhận',
            value: kpis.ready,
            hint: 'sách đang giữ tại quầy',
            color: '#16A34A',
            bg: '#F0FDF4',
          },
          {
            label: 'Đang chờ bản sao',
            value: kpis.waiting,
            hint: `${kpis.waitingTitles} đầu sách trong hàng đợi`,
            color: '#2563EB',
            bg: '#EFF6FF',
          },
          {
            label: 'Sắp hết hạn giữ (< 12h)',
            value: kpis.urgent,
            hint: 'cần nhắc độc giả đến nhận',
            color: '#DC2626',
            bg: '#FEF2F2',
          },
          {
            label: 'Hủy / hết hạn tháng này',
            value: kpis.cancelledMonth,
            hint: 'theo dữ liệu đã tải',
            color: '#64748B',
            bg: '#F8FAFC',
          },
        ].map((c) => (
          <Col xs={12} md={6} key={c.label}>
            <div style={{ ...panel, padding: 14, background: c.bg, borderColor: 'transparent' }}>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                {c.label}
              </Typography.Text>
              <Typography.Title level={3} style={{ margin: '4px 0', color: c.color }}>
                {c.value}
              </Typography.Title>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                {c.hint}
              </Typography.Text>
            </div>
          </Col>
        ))}
      </Row>

      <Row gutter={[14, 14]}>
        <Col xs={24} lg={14}>
          <div style={{ ...panel, padding: 14, minHeight: 560 }}>
            <Space wrap style={{ marginBottom: 12 }}>
              <Input
                allowClear
                style={{ width: 240 }}
                placeholder="Lọc độc giả / sách / mã…"
                value={q}
                onChange={(e) => setQ(e.target.value)}
              />
              {(
                [
                  ['ALL', 'Tất cả'],
                  ['READY', 'Sẵn sàng'],
                  ['WAITING', 'Đang chờ'],
                  ['URGENT', 'Sắp hết hạn'],
                ] as const
              ).map(([k, label]) => (
                <Button
                  key={k}
                  size="small"
                  type={tab === k ? 'primary' : 'default'}
                  onClick={() => setTab(k)}
                >
                  {label}
                </Button>
              ))}
            </Space>

            <div style={{ maxHeight: 560, overflow: 'auto' }}>
              {filtered.length === 0 && (
                <Alert type="info" showIcon message="Không có đặt trước trong bộ lọc" />
              )}
              {filtered.map((r) => {
                const active = r.id === selectedId;
                const remainRow = remainLabel(r.readyUntil);
                return (
                  <button
                    key={r.id}
                    type="button"
                    onClick={() => setSelectedId(r.id)}
                    style={{
                      display: 'block',
                      width: '100%',
                      textAlign: 'left',
                      border: `1px solid ${active ? '#93C5FD' : tokens.color.border}`,
                      background: active ? '#EFF6FF' : '#fff',
                      borderRadius: 10,
                      padding: 12,
                      marginBottom: 8,
                      cursor: 'pointer',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                      <Typography.Text strong>
                        {r.code || `RES-${r.id}`}
                      </Typography.Text>
                      {statusTag(r.status, r.readyUntil)}
                    </div>
                    <Typography.Text style={{ display: 'block', marginTop: 4 }} ellipsis>
                      {r.bookTitle}
                    </Typography.Text>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {r.readerName}
                      {r.status === 'WAITING' && r.queuePosition != null
                        ? ` · Hàng đợi #${r.queuePosition}`
                        : ''}
                      {r.locationCode ? ` · ${r.locationCode}` : ''}
                      {remainRow ? ` · ${remainRow}` : ''}
                    </Typography.Text>
                  </button>
                );
              })}
            </div>
          </div>
        </Col>

        <Col xs={24} lg={10}>
          <div style={{ ...panel, padding: 16, minHeight: 560, position: 'sticky', top: 16 }}>
            {!selected ? (
              <Alert type="info" showIcon message="Chọn một yêu cầu đặt trước để xử lý" />
            ) : (
              <>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, marginBottom: 12 }}>
                  <div>
                    <Typography.Text strong style={{ fontSize: 15 }}>
                      {selected.code || `RES-${selected.id}`}
                    </Typography.Text>
                    <div>{statusTag(selected.status, selected.readyUntil)}</div>
                  </div>
                </div>

                <div
                  style={{
                    padding: 12,
                    borderRadius: 10,
                    background: '#F8FAFC',
                    border: `1px solid ${tokens.color.border}`,
                    marginBottom: 12,
                  }}
                >
                  <Typography.Text strong>{selected.readerName}</Typography.Text>
                  <div>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {selected.readerCode || '—'}
                    </Typography.Text>
                  </div>
                </div>

                <Typography.Text strong style={{ display: 'block', marginBottom: 4 }}>
                  {selected.bookTitle}
                </Typography.Text>
                <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 12 }}>
                  {selected.barcode ? `Mã vạch ${selected.barcode}` : 'Chưa gán bản sao'}
                  {selected.locationCode ? ` · Vị trí ${selected.locationCode}` : ''}
                </Typography.Text>

                {selected.status === 'READY' && (
                  <Alert
                    style={{ marginBottom: 12 }}
                    type={remain?.includes('hết hạn') ? 'error' : 'warning'}
                    showIcon
                    icon={<ClockCircleOutlined />}
                    message={remain || 'Đang giữ chỗ'}
                    description={
                      selected.readyUntil
                        ? `Giữ đến ${dayjs(selected.readyUntil).format('HH:mm DD/MM/YYYY')}`
                        : undefined
                    }
                  />
                )}

                {(selected.status === 'READY' || selected.status === 'WAITING') && (
                  <div style={{ marginBottom: 14 }}>
                    <Typography.Text
                      strong
                      style={{
                        display: 'block',
                        marginBottom: 8,
                        fontSize: 12,
                        color: '#64748B',
                        textTransform: 'uppercase',
                        letterSpacing: 0.4,
                      }}
                    >
                      Hàng đợi FIFO cùng đầu sách
                    </Typography.Text>
                    {selected.status === 'READY' && (
                      <div
                        style={{
                          padding: '8px 10px',
                          borderRadius: 8,
                          background: '#F0FDF4',
                          border: '1px solid #BBF7D0',
                          marginBottom: 6,
                        }}
                      >
                        <Typography.Text strong>1. {selected.readerName}</Typography.Text>
                        <Typography.Text type="secondary"> (đang giữ)</Typography.Text>
                      </div>
                    )}
                    {queue.length === 0 && selected.status === 'WAITING' && (
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        Bạn đang ở vị trí #{selected.queuePosition ?? '—'} trong hàng đợi.
                      </Typography.Text>
                    )}
                    {queue.map((qRow, idx) => (
                      <div
                        key={qRow.id}
                        style={{
                          padding: '8px 10px',
                          borderRadius: 8,
                          background: qRow.id === selected.id ? '#EFF6FF' : '#fff',
                          border: `1px solid ${tokens.color.border}`,
                          marginBottom: 6,
                        }}
                      >
                        <Typography.Text>
                          {selected.status === 'READY' ? idx + 2 : idx + 1}. {qRow.readerName}
                        </Typography.Text>
                        {qRow.queuePosition != null && (
                          <Typography.Text type="secondary"> · #{qRow.queuePosition}</Typography.Text>
                        )}
                      </div>
                    ))}
                  </div>
                )}

                <Space direction="vertical" style={{ width: '100%' }} size={8}>
                  {selected.status === 'READY' && (
                    <Button
                      type="primary"
                      size="large"
                      block
                      onClick={() => fulfill(selected)}
                      style={{ height: 46, fontWeight: 700, background: '#0F172A', borderColor: '#0F172A' }}
                    >
                      Xác nhận giao sách & tạo phiếu mượn
                    </Button>
                  )}
                  <Space.Compact style={{ width: '100%' }}>
                    <Button
                      style={{ width: '33%' }}
                      icon={<PrinterOutlined />}
                      onClick={() => message.info('In nhãn giữ chỗ — dùng máy in quầy')}
                    >
                      In nhãn
                    </Button>
                    <Button
                      style={{ width: '34%' }}
                      disabled={selected.status !== 'READY'}
                      onClick={() => void extend(selected.id)}
                    >
                      Gia hạn 24h
                    </Button>
                    <Button
                      style={{ width: '33%' }}
                      danger
                      disabled={selected.status !== 'READY' && selected.status !== 'WAITING'}
                      icon={<WarningOutlined />}
                      onClick={() => void cancel(selected.id)}
                    >
                      Hủy & trả kho
                    </Button>
                  </Space.Compact>
                </Space>
              </>
            )}
          </div>
        </Col>
      </Row>
    </div>
  );
}
