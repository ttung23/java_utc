import { useCallback, useEffect, useMemo, useState, type CSSProperties } from 'react';
import {
  Button,
  Col,
  Input,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import {
  PlusOutlined,
  PrinterOutlined,
  ReloadOutlined,
  ScanOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { Link, useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import { api, type ApiResponse, tokenStore } from '@/services/api';
import { tokens } from '@/styles/tokens';
import { statusVi } from '@/utils/viLabels';

type Item = {
  id: number;
  bookId?: number;
  bookTitle?: string;
  barcode?: string;
  status: string;
  dueDate?: string;
  overdue?: boolean;
  estimatedFine?: number;
  renewCount?: number;
  conditionOut?: string;
  borrowedAt?: string;
};

type Slip = {
  id: number;
  slipCode: string;
  readerId?: number;
  readerName?: string;
  readerCode?: string;
  channel: string;
  status: string;
  overdue?: boolean;
  borrowedAt?: string;
  createdAt?: string;
  requestedAt?: string;
  pickupDeadline?: string;
  note?: string;
  items?: Item[];
};

type Meta = { page: number; limit: number; total: number };

const panel: CSSProperties = {
  background: '#fff',
  borderRadius: 12,
  border: `1px solid ${tokens.color.border}`,
  boxShadow: '0 1px 2px rgba(15,23,42,0.04)',
};

const CHANNEL_VI: Record<string, string> = {
  COUNTER: 'Quầy',
  ONLINE: 'Cổng trực tuyến',
};

function slipStatusLabel(slip: Slip): { text: string; color: string } {
  if (slip.overdue || slip.items?.some((i) => i.overdue && i.status === 'BORROWING')) {
    return { text: 'Quá hạn', color: 'error' };
  }
  switch (slip.status) {
    case 'BORROWING':
      return { text: 'Đang mượn', color: 'processing' };
    case 'PENDING':
      return { text: 'Chờ duyệt', color: 'gold' };
    case 'APPROVED':
      return { text: 'Chờ nhận', color: 'warning' };
    case 'PARTIAL_RETURNED':
      return { text: 'Trả một phần', color: 'orange' };
    case 'RETURNED':
      return { text: 'Đã trả đủ', color: 'success' };
    case 'REJECTED':
      return { text: 'Từ chối', color: 'default' };
    case 'CANCELLED':
      return { text: 'Đã hủy', color: 'default' };
    case 'EXPIRED':
      return { text: 'Hết hạn giữ', color: 'default' };
    default:
      return { text: statusVi(slip.status), color: 'default' };
  }
}

function earliestDue(items?: Item[]): string | undefined {
  const dues = (items || [])
    .filter((i) => i.dueDate && (i.status === 'BORROWING' || i.status === 'HOLD'))
    .map((i) => i.dueDate!)
    .sort();
  return dues[0];
}

function dueHint(due?: string, overdue?: boolean): { text: string; danger: boolean } | null {
  if (!due) return null;
  const d = dayjs(due).startOf('day');
  const today = dayjs().startOf('day');
  const diff = d.diff(today, 'day');
  if (overdue || diff < 0) return { text: `Quá hạn ${Math.abs(diff)} ngày`, danger: true };
  if (diff === 0) return { text: 'Hết hạn hôm nay', danger: true };
  if (diff <= 2) return { text: `Còn ${diff} ngày`, danger: true };
  return { text: `Còn ${diff} ngày`, danger: false };
}

const fmtMoney = (n: number) => `${Math.round(n).toLocaleString('vi-VN')}đ`;

export function BorrowListPage() {
  const navigate = useNavigate();
  const [rows, setRows] = useState<Slip[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 15, total: 0 });
  const [status, setStatus] = useState<string | undefined>();
  const [channel, setChannel] = useState<string | undefined>();
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(false);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<Slip | null>(null);
  const [kpi, setKpi] = useState({
    total: 0,
    borrowing: 0,
    borrowingItems: 0,
    dueSoon: 0,
    overdueSlips: 0,
    overdueItems: 0,
    overdueFine: 0,
    holding: 0,
  });

  const loadSlips = useCallback(
    async (page = 0) => {
      setLoading(true);
      try {
        const isOverdueFilter = status === 'OVERDUE';
        const { data } = await api.get<ApiResponse<Slip[]>>('/borrows', {
          params: {
            page,
            size: meta.limit,
            status: isOverdueFilter ? 'BORROWING' : status || undefined,
            channel: channel || undefined,
            overdueOnly: isOverdueFilter ? true : undefined,
          },
        });
        const list = data.data || [];
        const m = data.meta as Meta | undefined;
        setRows(list);
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
        message.error('Không tải được danh sách phiếu mượn');
      } finally {
        setLoading(false);
      }
    },
    [meta.limit, status, channel],
  );

  const loadKpis = useCallback(async () => {
    try {
      const [all, borrowing, overdue, dueSoon, holding] = await Promise.all([
        api.get<ApiResponse<Slip[]>>('/borrows', { params: { page: 0, size: 1 } }),
        api.get<ApiResponse<Slip[]>>('/borrows', { params: { page: 0, size: 100, status: 'BORROWING' } }),
        api.get<ApiResponse<Item[]>>('/borrows/overdue'),
        api.get<ApiResponse<Item[]>>('/borrows/due-soon'),
        api.get<ApiResponse<Slip[]>>('/borrows', {
          params: { page: 0, size: 1, status: 'APPROVED', channel: 'ONLINE' },
        }),
      ]);
      const borrowSlips = borrowing.data.data || [];
      const overdueItems = overdue.data.data || [];
      const dueSoonItems = dueSoon.data.data || [];
      const overdueFine = overdueItems.reduce((s, i) => s + Number(i.estimatedFine || 0), 0);
      const overdueSlipIds = new Set(
        borrowSlips.filter((s) => s.overdue || s.items?.some((i) => i.overdue)).map((s) => s.id),
      );
      setKpi({
        total: (all.data.meta as Meta | undefined)?.total ?? 0,
        borrowing: (borrowing.data.meta as Meta | undefined)?.total ?? borrowSlips.length,
        borrowingItems: borrowSlips.reduce((s, r) => s + (r.items?.filter((i) => i.status === 'BORROWING').length || 0), 0),
        dueSoon: dueSoonItems.length,
        overdueSlips: overdueSlipIds.size || Math.min(overdueItems.length, borrowSlips.length),
        overdueItems: overdueItems.length,
        overdueFine,
        holding: (holding.data.meta as Meta | undefined)?.total ?? 0,
      });
    } catch {
      /* bỏ qua KPI */
    }
  }, []);

  const loadDetail = useCallback(async (id: number) => {
    try {
      const { data } = await api.get<ApiResponse<Slip>>(`/borrows/${id}`);
      setDetail(data.data);
    } catch {
      setDetail(null);
    }
  }, []);

  useEffect(() => {
    void loadSlips(0);
    void loadKpis();
  }, [loadSlips, loadKpis]);

  useEffect(() => {
    if (selectedId != null) void loadDetail(selectedId);
  }, [selectedId, loadDetail]);

  const filteredRows = useMemo(() => {
    const keyword = q.trim().toLowerCase();
    if (!keyword) return rows;
    return rows.filter(
      (r) =>
        r.slipCode?.toLowerCase().includes(keyword) ||
        r.readerName?.toLowerCase().includes(keyword) ||
        r.readerCode?.toLowerCase().includes(keyword),
    );
  }, [rows, q]);

  const printPdf = async (id: number) => {
    try {
      const res = await api.get(`/borrows/${id}/print`, { responseType: 'blob' });
      const url = URL.createObjectURL(res.data);
      window.open(url, '_blank');
    } catch {
      message.error('Không in được PDF');
    }
  };

  const renewItem = async (itemId: number) => {
    try {
      await api.post(`/borrow-items/${itemId}/renew`, {});
      message.success('Đã gia hạn');
      if (selectedId) void loadDetail(selectedId);
      void loadSlips(meta.page);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Gia hạn thất bại');
    }
  };

  const renewAll = async () => {
    if (!detail?.items?.length) return;
    const ids = detail.items.filter((i) => i.status === 'BORROWING').map((i) => i.id);
    if (!ids.length) {
      message.warning('Không có sách đang mượn để gia hạn');
      return;
    }
    let ok = 0;
    for (const id of ids) {
      try {
        await api.post(`/borrow-items/${id}/renew`, {});
        ok++;
      } catch {
        /* continue */
      }
    }
    if (ok) {
      message.success(`Đã gia hạn ${ok}/${ids.length} cuốn`);
      void loadDetail(detail.id);
      void loadSlips(meta.page);
    } else {
      message.error('Gia hạn thất bại');
    }
  };

  const selected = detail;
  const selectedLabel = selected ? slipStatusLabel(selected) : null;
  const selectedDue = selected ? earliestDue(selected.items) : undefined;
  const selectedFine = (selected?.items || []).reduce((s, i) => s + Number(i.estimatedFine || 0), 0);

  return (
    <div style={{ maxWidth: 1560, margin: '0 auto' }}>
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
            Mượn – Trả · Danh sách phiếu mượn
          </Typography.Text>
          <Typography.Title level={4} style={{ margin: '2px 0 0' }}>
            Quản lý lưu thông phiếu mượn
          </Typography.Title>
        </div>
        <Space wrap>
          <Link to="/returns">
            <Button icon={<ScanOutlined />}>Nhận trả sách</Button>
          </Link>
          <Link to="/borrows/create">
            <Button type="primary" icon={<PlusOutlined />}>
              Tạo phiếu mượn mới
            </Button>
          </Link>
          <Button icon={<ReloadOutlined />} onClick={() => { void loadSlips(meta.page); void loadKpis(); }}>
            Tải lại
          </Button>
        </Space>
      </div>

      <Row gutter={[12, 12]} style={{ marginBottom: 14 }}>
        {[
          {
            label: 'Tổng phiếu lưu thông',
            value: kpi.total,
            hint: 'mọi trạng thái',
            color: '#0F172A',
            bg: '#F8FAFC',
          },
          {
            label: 'Đang mượn',
            value: kpi.borrowing,
            hint: `${kpi.borrowingItems} cuốn · ${kpi.dueSoon} sắp đến hạn ≤ 2 ngày`,
            color: '#2563EB',
            bg: '#EFF6FF',
          },
          {
            label: 'Quá hạn',
            value: kpi.overdueSlips,
            hint: `${kpi.overdueItems} cuốn · phạt ~ ${fmtMoney(kpi.overdueFine)}`,
            color: '#DC2626',
            bg: '#FEF2F2',
          },
          {
            label: 'Chờ nhận / giữ chỗ',
            value: kpi.holding,
            hint: 'phiếu trực tuyến đã duyệt',
            color: '#D97706',
            bg: '#FFFBEB',
          },
        ].map((c) => (
          <Col xs={12} md={6} key={c.label}>
            <div style={{ ...panel, padding: 14, background: c.bg, borderColor: 'transparent' }}>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                {c.label}
              </Typography.Text>
              <Typography.Title level={3} style={{ margin: '4px 0', color: c.color }}>
                {c.value.toLocaleString('vi-VN')}
              </Typography.Title>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                {c.hint}
              </Typography.Text>
            </div>
          </Col>
        ))}
      </Row>

      <Row gutter={[14, 14]}>
        <Col xs={24} xl={15}>
          <div style={{ ...panel, padding: 14 }}>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                gap: 8,
                marginBottom: 12,
                flexWrap: 'wrap',
                alignItems: 'center',
              }}
            >
              <Typography.Text strong>
                Danh sách phiếu lưu thông ({meta.total.toLocaleString('vi-VN')})
              </Typography.Text>
              <Space wrap>
                <Input
                  allowClear
                  style={{ width: 220 }}
                  prefix={<SearchOutlined />}
                  placeholder="Mã phiếu / độc giả…"
                  value={q}
                  onChange={(e) => setQ(e.target.value)}
                />
                <Select
                  allowClear
                  placeholder="Trạng thái"
                  style={{ width: 160 }}
                  value={status}
                  onChange={setStatus}
                  options={[
                    { value: 'BORROWING', label: 'Đang mượn' },
                    { value: 'OVERDUE', label: 'Quá hạn' },
                    { value: 'APPROVED', label: 'Chờ nhận' },
                    { value: 'PENDING', label: 'Chờ duyệt' },
                    { value: 'PARTIAL_RETURNED', label: 'Trả một phần' },
                    { value: 'RETURNED', label: 'Đã trả đủ' },
                    { value: 'REJECTED', label: 'Từ chối' },
                    { value: 'CANCELLED', label: 'Đã hủy' },
                  ]}
                />
                <Select
                  allowClear
                  placeholder="Kênh"
                  style={{ width: 140 }}
                  value={channel}
                  onChange={setChannel}
                  options={[
                    { value: 'COUNTER', label: 'Quầy' },
                    { value: 'ONLINE', label: 'Trực tuyến' },
                  ]}
                />
              </Space>
            </div>

            <Table
              rowKey="id"
              size="middle"
              loading={loading}
              dataSource={filteredRows}
              pagination={{
                current: meta.page + 1,
                pageSize: meta.limit,
                total: meta.total,
                showSizeChanger: false,
                onChange: (p) => void loadSlips(p - 1),
              }}
              onRow={(r) => ({
                onClick: () => setSelectedId(r.id),
                style: {
                  cursor: 'pointer',
                  background: r.id === selectedId ? '#EFF6FF' : undefined,
                },
              })}
              columns={[
                {
                  title: 'Mã phiếu / kênh',
                  width: 160,
                  render: (_: unknown, r: Slip) => (
                    <div>
                      <Typography.Text strong>{r.slipCode}</Typography.Text>
                      <div>
                        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                          {CHANNEL_VI[r.channel] || r.channel}
                        </Typography.Text>
                      </div>
                    </div>
                  ),
                },
                {
                  title: 'Độc giả',
                  render: (_: unknown, r: Slip) => (
                    <div>
                      <Typography.Text>{r.readerName || '—'}</Typography.Text>
                      <div>
                        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                          {r.readerCode || '—'}
                        </Typography.Text>
                      </div>
                    </div>
                  ),
                },
                {
                  title: 'Số cuốn',
                  width: 80,
                  render: (_: unknown, r: Slip) => `${r.items?.length || 0} cuốn`,
                },
                {
                  title: 'Ngày mượn',
                  width: 110,
                  render: (_: unknown, r: Slip) =>
                    r.borrowedAt
                      ? dayjs(r.borrowedAt).format('DD/MM/YYYY')
                      : r.createdAt
                        ? dayjs(r.createdAt).format('DD/MM/YYYY')
                        : '—',
                },
                {
                  title: 'Hạn trả',
                  width: 150,
                  render: (_: unknown, r: Slip) => {
                    const due = earliestDue(r.items);
                    const hint = dueHint(due, r.overdue);
                    return (
                      <div>
                        <div>{due ? dayjs(due).format('DD/MM/YYYY') : '—'}</div>
                        {hint && (
                          <Typography.Text
                            style={{ fontSize: 12, color: hint.danger ? '#DC2626' : '#64748B' }}
                          >
                            {hint.text}
                          </Typography.Text>
                        )}
                      </div>
                    );
                  },
                },
                {
                  title: 'Trạng thái',
                  width: 120,
                  render: (_: unknown, r: Slip) => {
                    const s = slipStatusLabel(r);
                    return <Tag color={s.color}>{s.text}</Tag>;
                  },
                },
                {
                  title: 'Phạt',
                  width: 100,
                  render: (_: unknown, r: Slip) => {
                    const fine = (r.items || []).reduce((s, i) => s + Number(i.estimatedFine || 0), 0);
                    return fine > 0 ? (
                      <Typography.Text type="danger">{fmtMoney(fine)}</Typography.Text>
                    ) : (
                      <Typography.Text type="secondary">0đ</Typography.Text>
                    );
                  },
                },
                {
                  title: '',
                  width: 90,
                  render: (_: unknown, r: Slip) => (
                    <Button
                      type="link"
                      size="small"
                      icon={<PrinterOutlined />}
                      disabled={!tokenStore.getAccess()}
                      onClick={(e) => {
                        e.stopPropagation();
                        void printPdf(r.id);
                      }}
                    >
                      In
                    </Button>
                  ),
                },
              ]}
            />
          </div>
        </Col>

        <Col xs={24} xl={9}>
          <div style={{ ...panel, padding: 16, minHeight: 520, position: 'sticky', top: 16 }}>
            {!selected ? (
              <Typography.Text type="secondary">Chọn một phiếu để xem chi tiết</Typography.Text>
            ) : (
              <>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, marginBottom: 12 }}>
                  <div>
                    <Typography.Text strong style={{ fontSize: 15 }}>
                      {selected.slipCode}
                    </Typography.Text>
                    <div>
                      {selectedLabel && <Tag color={selectedLabel.color}>{selectedLabel.text}</Tag>}
                      <Tag>{CHANNEL_VI[selected.channel] || selected.channel}</Tag>
                    </div>
                  </div>
                  <Button size="small" icon={<PrinterOutlined />} onClick={() => void printPdf(selected.id)}>
                    In phiếu
                  </Button>
                </div>

                <div
                  style={{
                    padding: 12,
                    borderRadius: 10,
                    background: '#F8FAFC',
                    border: `1px solid ${tokens.color.border}`,
                    marginBottom: 14,
                  }}
                >
                  <Typography.Text strong>{selected.readerName}</Typography.Text>
                  <div>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {selected.readerCode || '—'} · Đang có {(selected.items || []).filter((i) => i.status === 'BORROWING').length} cuốn trên phiếu
                    </Typography.Text>
                  </div>
                  {selectedDue && (
                    <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block', marginTop: 4 }}>
                      Hạn sớm nhất: {dayjs(selectedDue).format('DD/MM/YYYY')}
                      {selectedFine > 0 ? ` · Phạt ước tính ${fmtMoney(selectedFine)}` : ''}
                    </Typography.Text>
                  )}
                </div>

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
                  Sách trên phiếu ({selected.items?.length || 0})
                </Typography.Text>

                <Space direction="vertical" style={{ width: '100%' }} size={8}>
                  {(selected.items || []).map((it, idx) => (
                    <div
                      key={it.id}
                      style={{
                        border: `1px solid ${it.overdue ? '#FECACA' : tokens.color.border}`,
                        background: it.overdue ? '#FFF7F7' : '#fff',
                        borderRadius: 10,
                        padding: 10,
                      }}
                    >
                      <Typography.Text strong>
                        {idx + 1}. {it.bookTitle || '—'}
                      </Typography.Text>
                      <div>
                        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                          {it.barcode ? `Mã vạch ${it.barcode}` : 'Chưa gán mã vạch'}
                          {it.dueDate ? ` · Hạn ${dayjs(it.dueDate).format('DD/MM/YYYY')}` : ''}
                        </Typography.Text>
                      </div>
                      <Space style={{ marginTop: 6 }} wrap>
                        <Tag color={it.overdue ? 'error' : 'default'}>{statusVi(it.status)}</Tag>
                        {Number(it.estimatedFine || 0) > 0 && (
                          <Tag color="red">{fmtMoney(Number(it.estimatedFine))}</Tag>
                        )}
                      </Space>
                      {it.status === 'BORROWING' && (
                        <Space style={{ marginTop: 8 }} wrap>
                          <Button size="small" onClick={() => void renewItem(it.id)}>
                            Gia hạn (+7 ngày)
                          </Button>
                          <Button
                            size="small"
                            type="primary"
                            onClick={() => navigate('/returns')}
                          >
                            Nhận trả
                          </Button>
                        </Space>
                      )}
                    </div>
                  ))}
                </Space>

                {selected.note && (
                  <div style={{ marginTop: 12 }}>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      Ghi chú thủ thư
                    </Typography.Text>
                    <Typography.Paragraph style={{ marginBottom: 0 }}>{selected.note}</Typography.Paragraph>
                  </div>
                )}

                <Space direction="vertical" style={{ width: '100%', marginTop: 16 }} size={8}>
                  <Button type="primary" block onClick={() => navigate('/returns')}>
                    Nhận trả toàn bộ sách
                  </Button>
                  <Button block onClick={() => void renewAll()}>
                    Gia hạn tất cả
                  </Button>
                  <Button block icon={<PrinterOutlined />} onClick={() => void printPdf(selected.id)}>
                    In biên bản / phiếu
                  </Button>
                  {selectedFine > 0 && (
                    <Link to="/fines">
                      <Button block danger>
                        Thu phạt ({fmtMoney(selectedFine)})
                      </Button>
                    </Link>
                  )}
                </Space>
              </>
            )}
          </div>
        </Col>
      </Row>
    </div>
  );
}
