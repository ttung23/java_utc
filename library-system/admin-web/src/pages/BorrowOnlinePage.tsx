import { useCallback, useEffect, useMemo, useState, type CSSProperties } from 'react';
import {
  Alert,
  Button,
  Col,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Tag,
  Typography,
  message,
} from 'antd';
import {
  CheckCircleFilled,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  PrinterOutlined,
  ReloadOutlined,
  ScanOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { api, type ApiResponse } from '@/services/api';
import { tokens } from '@/styles/tokens';
import { statusVi } from '@/utils/viLabels';

type SlipItem = {
  id: number;
  bookId?: number;
  bookTitle?: string;
  copyId?: number;
  barcode?: string;
  status: string;
};

type Slip = {
  id: number;
  slipCode: string;
  readerId?: number;
  readerName?: string;
  readerCode?: string;
  status: string;
  requestedAt?: string;
  pickupDeadline?: string;
  items?: SlipItem[];
};

type CopyOpt = {
  id: number;
  barcode: string;
  status: string;
  locationCode?: string;
  locationName?: string;
};

type AssignMap = Record<number, number | undefined>; // itemId -> copyId

const panel: CSSProperties = {
  background: '#fff',
  borderRadius: 12,
  border: `1px solid ${tokens.color.border}`,
  boxShadow: '0 1px 2px rgba(15,23,42,0.04)',
};

function statusTag(status: string) {
  if (status === 'PENDING') return <Tag color="gold">Chờ duyệt</Tag>;
  if (status === 'APPROVED') return <Tag color="blue">Đang giữ sách</Tag>;
  if (status === 'REJECTED') return <Tag color="red">Từ chối</Tag>;
  if (status === 'CANCELLED' || status === 'EXPIRED') return <Tag>{statusVi(status)}</Tag>;
  return <Tag>{statusVi(status)}</Tag>;
}

export function BorrowOnlinePage() {
  const [rows, setRows] = useState<Slip[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<Slip | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [copiesByBook, setCopiesByBook] = useState<Record<number, CopyOpt[]>>({});
  const [assign, setAssign] = useState<AssignMap>({});
  const [listQ, setListQ] = useState('');
  const [tab, setTab] = useState<'ALL' | 'PENDING' | 'APPROVED'>('ALL');

  const loadList = useCallback(async (preferId?: number | null) => {
    setLoading(true);
    try {
      const [pending, approved] = await Promise.all([
        api.get<ApiResponse<Slip[]>>('/borrows', {
          params: { channel: 'ONLINE', status: 'PENDING', page: 0, size: 50 },
        }),
        api.get<ApiResponse<Slip[]>>('/borrows', {
          params: { channel: 'ONLINE', status: 'APPROVED', page: 0, size: 50 },
        }),
      ]);
      const merged = [...(pending.data.data || []), ...(approved.data.data || [])].sort(
        (a, b) => dayjs(b.requestedAt || 0).valueOf() - dayjs(a.requestedAt || 0).valueOf(),
      );
      setRows(merged);
      setSelectedId((curr) => {
        const want = preferId !== undefined ? preferId : curr;
        if (want != null && merged.some((r) => r.id === want)) return want;
        return merged[0]?.id ?? null;
      });
    } catch {
      message.error('Không tải được yêu cầu mượn trực tuyến');
    } finally {
      setLoading(false);
    }
  }, []);

  const loadDetail = useCallback(async (id: number) => {
    setDetailLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Slip>>(`/borrows/${id}`);
      const slip = data.data;
      setDetail(slip);
      const nextAssign: AssignMap = {};
      const bookIds = new Set<number>();
      for (const it of slip.items || []) {
        if (it.bookId) bookIds.add(it.bookId);
        if (it.copyId) nextAssign[it.id] = it.copyId;
      }

      const map: Record<number, CopyOpt[]> = {};
      await Promise.all(
        [...bookIds].map(async (bookId) => {
          try {
            const res = await api.get<ApiResponse<CopyOpt[]>>(`/books/${bookId}/copies`);
            const assignedIds = new Set(
              Object.values(nextAssign).filter((x): x is number => x != null),
            );
            map[bookId] = (res.data.data || []).filter(
              (c) => c.status === 'AVAILABLE' || assignedIds.has(c.id) || c.status === 'HOLD',
            );
          } catch {
            map[bookId] = [];
          }
        }),
      );
      setCopiesByBook(map);

      if (slip.status === 'PENDING') {
        const auto: AssignMap = { ...nextAssign };
        for (const it of slip.items || []) {
          if (auto[it.id] || !it.bookId) continue;
          const first = (map[it.bookId] || []).find((c) => c.status === 'AVAILABLE');
          if (first) auto[it.id] = first.id;
        }
        setAssign(auto);
      } else {
        setAssign(nextAssign);
      }
    } catch {
      message.error('Không tải được chi tiết yêu cầu');
      setDetail(null);
    } finally {
      setDetailLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadList();
  }, [loadList]);

  useEffect(() => {
    if (selectedId != null) void loadDetail(selectedId);
  }, [selectedId, loadDetail]);

  const filteredRows = useMemo(() => {
    let list = rows;
    if (tab !== 'ALL') list = list.filter((r) => r.status === tab);
    const q = listQ.trim().toLowerCase();
    if (!q) return list;
    return list.filter(
      (r) =>
        r.slipCode?.toLowerCase().includes(q) ||
        r.readerName?.toLowerCase().includes(q) ||
        r.readerCode?.toLowerCase().includes(q),
    );
  }, [rows, tab, listQ]);

  const kpis = useMemo(() => {
    const pending = rows.filter((r) => r.status === 'PENDING').length;
    const holding = rows.filter((r) => r.status === 'APPROVED');
    const holdCopies = holding.reduce((s, r) => s + (r.items?.length || 0), 0);
    const expiring = holding.filter((r) => {
      if (!r.pickupDeadline) return false;
      const mins = dayjs(r.pickupDeadline).diff(dayjs(), 'minute');
      return mins >= 0 && mins <= 60;
    }).length;
    return { pending, holding: holding.length, holdCopies, expiring };
  }, [rows]);

  const approve = async () => {
    if (!detail || detail.status !== 'PENDING') return;
    const need = (detail.items || []).filter(
      (it) => it.status === 'REQUESTED' || !it.copyId,
    );
    const items = need
      .map((it) => ({ itemId: it.id, copyId: assign[it.id] }))
      .filter((x): x is { itemId: number; copyId: number } => x.copyId != null);
    if (items.length < need.length) {
      message.warning('Chọn mã vạch bản sao cho từng đầu sách trước khi duyệt');
      return;
    }
    try {
      await api.post(`/borrows/${detail.id}/approve`, { items });
      message.success('Đã duyệt và giữ sách');
      await loadList(detail.id);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Duyệt thất bại');
    }
  };

  const reject = () => {
    if (!detail) return;
    let reason = '';
    Modal.confirm({
      title: 'Từ chối yêu cầu',
      okText: 'Từ chối',
      okButtonProps: { danger: true },
      cancelText: 'Đóng',
      content: (
        <Input.TextArea
          rows={3}
          placeholder="Lý do từ chối (bắt buộc)"
          onChange={(e) => {
            reason = e.target.value;
          }}
        />
      ),
      onOk: async () => {
        if (!reason.trim()) {
          message.warning('Nhập lý do');
          return Promise.reject();
        }
        await api.post(`/borrows/${detail.id}/reject`, { reason: reason.trim() });
        message.success('Đã từ chối');
        setSelectedId(null);
        setDetail(null);
        void loadList();
      },
    });
  };

  const handover = () => {
    if (!detail || detail.status !== 'APPROVED') return;
    const defaultBarcodes = (detail.items || [])
      .map((i) => i.barcode)
      .filter(Boolean)
      .join('\n');
    let barcodes = defaultBarcodes;
    Modal.confirm({
      title: `Giao sách — ${detail.slipCode}`,
      okText: 'Xác nhận giao',
      cancelText: 'Đóng',
      content: (
        <div>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>
            Quét hoặc xác nhận các mã vạch đang giữ cho độc giả.
          </Typography.Paragraph>
          <Input.TextArea
            rows={4}
            defaultValue={defaultBarcodes}
            placeholder="Mỗi mã vạch một dòng"
            onChange={(e) => {
              barcodes = e.target.value;
            }}
          />
        </div>
      ),
      onOk: async () => {
        const scannedBarcodes = barcodes
          .split(/[\n,]+/)
          .map((s) => s.trim())
          .filter(Boolean);
        if (!scannedBarcodes.length) {
          message.warning('Nhập mã vạch');
          return Promise.reject();
        }
        await api.post(`/borrows/${detail.id}/handover`, { scannedBarcodes });
        message.success('Đã giao sách');
        setSelectedId(null);
        setDetail(null);
        void loadList();
      },
    });
  };

  const printPick = () => {
    if (!detail) return;
    message.info('In phiếu nhặt sách — dùng chức năng in PDF phiếu khi đã duyệt');
    if (detail.status === 'APPROVED') {
      void api
        .get(`/borrows/${detail.id}/print`, { responseType: 'blob' })
        .then((res) => {
          const url = URL.createObjectURL(res.data);
          window.open(url, '_blank');
        })
        .catch(() => message.error('Không in được PDF'));
    }
  };

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
            Mượn – Trả · Yêu cầu mượn trực tuyến
          </Typography.Text>
          <Typography.Title level={4} style={{ margin: '2px 0 0' }}>
            Xử lý hồ sơ & gắn kho
          </Typography.Title>
        </div>
        <Space wrap>
          <Button icon={<PrinterOutlined />} onClick={printPick}>
            Xuất phiếu nhặt sách
          </Button>
          <Button type="primary" icon={<ScanOutlined />} onClick={handover}>
            Quét mã nhận / giao sách
          </Button>
          <Button icon={<ReloadOutlined />} loading={loading} onClick={() => void loadList()}>
            Tải lại
          </Button>
        </Space>
      </div>

      <Row gutter={[12, 12]} style={{ marginBottom: 14 }}>
        {[
          {
            label: 'Chờ duyệt',
            value: kpis.pending,
            hint: 'yêu cầu mới',
            color: '#D97706',
            bg: '#FFFBEB',
          },
          {
            label: 'Đang giữ sách',
            value: kpis.holding,
            hint: `${kpis.holdCopies} bản sao tại quầy`,
            color: '#2563EB',
            bg: '#EFF6FF',
          },
          {
            label: 'Sắp hết hạn giữ',
            value: kpis.expiring,
            hint: 'trong 60 phút tới',
            color: '#DC2626',
            bg: '#FEF2F2',
          },
          {
            label: 'Tổng đang xử lý',
            value: rows.length,
            hint: 'chờ duyệt + đang giữ',
            color: '#0F172A',
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
        {/* Danh sách yêu cầu */}
        <Col xs={24} lg={9} xl={8}>
          <div style={{ ...panel, padding: 12, minHeight: 560 }}>
            <Input
              allowClear
              placeholder="Lọc mã phiếu / độc giả…"
              style={{ marginBottom: 10 }}
              value={listQ}
              onChange={(e) => setListQ(e.target.value)}
            />
            <Space style={{ marginBottom: 10 }} wrap>
              {(
                [
                  ['ALL', 'Tất cả'],
                  ['PENDING', 'Chờ duyệt'],
                  ['APPROVED', 'Đang giữ'],
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

            <div style={{ maxHeight: 520, overflow: 'auto' }}>
              {filteredRows.length === 0 && (
                <Alert type="info" showIcon message="Không có yêu cầu trong bộ lọc" />
              )}
              {filteredRows.map((r) => {
                const active = r.id === selectedId;
                const count = r.items?.length || 0;
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
                      <Typography.Text strong style={{ fontSize: 13 }}>
                        {r.slipCode}
                      </Typography.Text>
                      {statusTag(r.status)}
                    </div>
                    <Typography.Text style={{ display: 'block', marginTop: 4 }}>
                      {r.readerName || '—'}
                    </Typography.Text>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {count} đầu sách
                      {r.requestedAt ? ` · ${dayjs(r.requestedAt).format('DD/MM HH:mm')}` : ''}
                      {r.status === 'APPROVED' && r.pickupDeadline
                        ? ` · giữ đến ${dayjs(r.pickupDeadline).format('DD/MM HH:mm')}`
                        : ''}
                    </Typography.Text>
                  </button>
                );
              })}
            </div>
          </div>
        </Col>

        {/* Chi tiết */}
        <Col xs={24} lg={15} xl={16}>
          <div style={{ ...panel, padding: 16, minHeight: 560 }}>
            {!detail ? (
              <Alert
                type="info"
                showIcon
                message="Chọn một yêu cầu bên trái để xử lý"
                description="Danh sách sách và gán mã vạch sẽ hiện ở đây."
              />
            ) : (
              <>
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    gap: 12,
                    marginBottom: 16,
                    flexWrap: 'wrap',
                  }}
                >
                  <Space align="start" size={12}>
                    <div
                      style={{
                        width: 52,
                        height: 52,
                        borderRadius: 12,
                        background: 'linear-gradient(135deg,#1D4ED8,#38BDF8)',
                        color: '#fff',
                        display: 'grid',
                        placeItems: 'center',
                        fontWeight: 800,
                        fontSize: 18,
                      }}
                    >
                      {(detail.readerName || '?').slice(0, 1).toUpperCase()}
                    </div>
                    <div>
                      <Space wrap>
                        <Typography.Text strong style={{ fontSize: 16 }}>
                          {detail.readerName}
                        </Typography.Text>
                        {statusTag(detail.status)}
                      </Space>
                      <div>
                        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                          Mã ĐG: {detail.readerCode || '—'} · Phiếu {detail.slipCode}
                          {detail.requestedAt
                            ? ` · Gửi ${dayjs(detail.requestedAt).format('DD/MM/YYYY HH:mm')}`
                            : ''}
                        </Typography.Text>
                      </div>
                      {detail.status === 'APPROVED' && detail.pickupDeadline && (
                        <Tag icon={<ClockCircleOutlined />} color="processing" style={{ marginTop: 6 }}>
                          Giữ đến {dayjs(detail.pickupDeadline).format('HH:mm DD/MM/YYYY')}
                        </Tag>
                      )}
                    </div>
                  </Space>
                </div>

                <Typography.Text
                  strong
                  style={{
                    display: 'block',
                    marginBottom: 10,
                    fontSize: 12,
                    letterSpacing: 0.4,
                    color: '#64748B',
                    textTransform: 'uppercase',
                  }}
                >
                  Danh mục tài liệu ({detail.items?.length || 0})
                </Typography.Text>

                <Space direction="vertical" style={{ width: '100%' }} size={10}>
                  {(detail.items || []).map((it, idx) => {
                    const opts = it.bookId ? copiesByBook[it.bookId] || [] : [];
                    const available = opts.filter((c) => c.status === 'AVAILABLE');
                    const selectedCopy = opts.find((c) => c.id === assign[it.id]);
                    const matched =
                      detail.status === 'APPROVED'
                        ? !!it.barcode
                        : !!assign[it.id] && (selectedCopy?.status === 'AVAILABLE' || !!selectedCopy);

                    return (
                      <div
                        key={it.id}
                        style={{
                          border: `1px solid ${matched ? '#BBF7D0' : tokens.color.border}`,
                          background: matched ? '#F0FDF4' : '#fff',
                          borderRadius: 10,
                          padding: 12,
                        }}
                      >
                        <div style={{ display: 'flex', gap: 10, marginBottom: 8 }}>
                          <div
                            style={{
                              width: 36,
                              height: 48,
                              borderRadius: 4,
                              background: 'linear-gradient(160deg,#1E3A8A,#60A5FA)',
                              color: '#fff',
                              fontSize: 11,
                              fontWeight: 700,
                              display: 'grid',
                              placeItems: 'center',
                              flexShrink: 0,
                            }}
                          >
                            #{idx + 1}
                          </div>
                          <div style={{ flex: 1, minWidth: 0 }}>
                            <Typography.Text strong>{it.bookTitle || '—'}</Typography.Text>
                            <div>
                              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                {selectedCopy?.locationCode || selectedCopy?.locationName
                                  ? `Vị trí: ${[selectedCopy.locationName, selectedCopy.locationCode]
                                      .filter(Boolean)
                                      .join(' · ')}`
                                  : available[0]?.locationCode
                                    ? `Gợi ý kệ: ${available[0].locationCode}`
                                    : 'Chưa có thông tin kệ'}
                              </Typography.Text>
                            </div>
                            <Space style={{ marginTop: 4 }} wrap>
                              <Tag>{statusVi(it.status)}</Tag>
                              {matched ? (
                                <Tag icon={<CheckCircleFilled />} color="success">
                                  Đã khớp kho
                                </Tag>
                              ) : available.length === 0 ? (
                                <Tag icon={<WarningOutlined />} color="error">
                                  Hết bản sao
                                </Tag>
                              ) : (
                                <Tag icon={<ExclamationCircleOutlined />} color="warning">
                                  Chưa gán mã vạch
                                </Tag>
                              )}
                            </Space>
                          </div>
                        </div>

                        {detail.status === 'PENDING' ? (
                          <div>
                            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                              Bản sao / mã vạch được gán
                            </Typography.Text>
                            <Select
                              style={{ width: '100%', marginTop: 4 }}
                              placeholder={
                                available.length
                                  ? 'Chọn bản sao sẵn sàng'
                                  : 'Không còn bản sao sẵn sàng'
                              }
                              value={assign[it.id]}
                              disabled={!available.length && !assign[it.id]}
                              options={opts
                                .filter((c) => c.status === 'AVAILABLE' || c.id === assign[it.id])
                                .map((c) => ({
                                  value: c.id,
                                  label: `${c.barcode}${c.locationCode ? ` · ${c.locationCode}` : ''} (${statusVi(c.status)})`,
                                }))}
                              onChange={(v) => setAssign((prev) => ({ ...prev, [it.id]: v }))}
                            />
                          </div>
                        ) : (
                          <Typography.Text>
                            Mã vạch giữ:{' '}
                            <Typography.Text code>{it.barcode || '—'}</Typography.Text>
                          </Typography.Text>
                        )}
                      </div>
                    );
                  })}
                </Space>

                <div
                  style={{
                    marginTop: 16,
                    paddingTop: 14,
                    borderTop: `1px solid ${tokens.color.border}`,
                    display: 'flex',
                    gap: 8,
                    flexWrap: 'wrap',
                    justifyContent: 'flex-end',
                  }}
                >
                  {detail.status === 'PENDING' && (
                    <>
                      <Button danger onClick={reject}>
                        Từ chối yêu cầu
                      </Button>
                      <Button onClick={printPick}>In phiếu nhặt sách</Button>
                      <Button
                        type="primary"
                        size="large"
                        loading={detailLoading}
                        onClick={() => void approve()}
                        style={{ background: '#0F172A', borderColor: '#0F172A', fontWeight: 700 }}
                      >
                        Duyệt yêu cầu & giữ sách
                      </Button>
                    </>
                  )}
                  {detail.status === 'APPROVED' && (
                    <>
                      <Button onClick={printPick} icon={<PrinterOutlined />}>
                        In phiếu
                      </Button>
                      <Button
                        type="primary"
                        size="large"
                        icon={<ScanOutlined />}
                        onClick={handover}
                        style={{ background: '#0F172A', borderColor: '#0F172A', fontWeight: 700 }}
                      >
                        Xác nhận giao sách
                      </Button>
                    </>
                  )}
                </div>
              </>
            )}
          </div>
        </Col>
      </Row>
    </div>
  );
}
