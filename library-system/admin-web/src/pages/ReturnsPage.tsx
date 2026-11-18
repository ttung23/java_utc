import { useEffect, useMemo, useRef, useState, type CSSProperties } from 'react';
import {
  Alert,
  Button,
  Col,
  Input,
  Radio,
  Row,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { InputRef } from 'antd';
import {
  CameraOutlined,
  DeleteOutlined,
  PlusOutlined,
  PrinterOutlined,
  ReloadOutlined,
  ScanOutlined,
  SearchOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { api, type ApiResponse } from '@/services/api';
import { tokens } from '@/styles/tokens';

type Preview = {
  itemId: number;
  slipId: number;
  slipCode: string;
  readerId: number;
  readerName: string;
  bookId?: number;
  bookTitle: string;
  barcode: string;
  dueDate: string;
  overdueDays: number;
  estimatedFine: number;
  renewCount: number;
};

type Line = Preview & {
  condition: string;
  note?: string;
  damageEstimate: number;
};

type ReservationHint = {
  id: number;
  readerName?: string;
  bookTitle?: string;
  status: string;
  readyUntil?: string;
};

const panel: CSSProperties = {
  background: '#fff',
  borderRadius: 12,
  border: `1px solid ${tokens.color.border}`,
  boxShadow: '0 1px 2px rgba(15,23,42,0.04)',
};

const CONDITIONS = [
  { value: 'NORMAL', label: 'Bình thường' },
  { value: 'DAMAGED_MINOR', label: 'Hư hỏng nhẹ' },
  { value: 'DAMAGED_MAJOR', label: 'Hư hỏng nặng' },
  { value: 'LOST', label: 'Mất sách' },
];

const OVERDUE_PER_DAY = 2000;
/** Ước tính UI khi chọn hư hỏng — BE tính chính xác lúc xác nhận */
const DAMAGE_ESTIMATE: Record<string, number> = {
  DAMAGED_MINOR: 24000,
  DAMAGED_MAJOR: 60000,
  LOST: 150000,
};

const fmtMoney = (n: number) => `${Math.round(n).toLocaleString('vi-VN')}đ`;

export function ReturnsPage() {
  const scanRef = useRef<InputRef>(null);
  const [barcode, setBarcode] = useState('');
  const [poolQ, setPoolQ] = useState('');
  const [pool, setPool] = useState<Preview[]>([]);
  const [poolLoading, setPoolLoading] = useState(false);
  const [lines, setLines] = useState<Line[]>([]);
  const [autoEnter, setAutoEnter] = useState(true);
  const [payMode, setPayMode] = useState<'COUNTER' | 'DEBT'>('COUNTER');
  const [payMethod, setPayMethod] = useState<'CASH' | 'QR' | 'CARD'>('CASH');
  const [loading, setLoading] = useState(false);
  const [holdHints, setHoldHints] = useState<ReservationHint[]>([]);
  const [highlightBarcode, setHighlightBarcode] = useState<string | null>(null);

  const sessionCode = useMemo(() => `TRA-${dayjs().format('DDMM-HHmm')}`, []);

  const loadPool = async () => {
    setPoolLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Preview[]>>('/returns/borrowing');
      setPool(data.data || []);
    } catch {
      // Fallback nếu BE chưa restart: lấy phiếu đang mượn
      try {
        const { data } = await api.get<
          ApiResponse<
            Array<{
              slipCode: string;
              readerId: number;
              readerName?: string;
              items?: Array<{
                id: number;
                bookId?: number;
                bookTitle?: string;
                barcode?: string;
                dueDate?: string;
                overdue?: boolean;
                estimatedFine?: number;
                renewCount?: number;
                status: string;
              }>;
            }>
          >
        >('/borrows', { params: { status: 'BORROWING', page: 0, size: 100 } });
        const flat: Preview[] = [];
        for (const slip of data.data || []) {
          for (const it of slip.items || []) {
            if (it.status !== 'BORROWING' || !it.barcode) continue;
            const due = it.dueDate ? dayjs(it.dueDate) : dayjs();
            const overdueDays = Math.max(0, dayjs().startOf('day').diff(due.startOf('day'), 'day'));
            flat.push({
              itemId: it.id,
              slipId: 0,
              slipCode: slip.slipCode,
              readerId: slip.readerId,
              readerName: slip.readerName || '—',
              bookId: it.bookId,
              bookTitle: it.bookTitle || '—',
              barcode: it.barcode,
              dueDate: it.dueDate || '',
              overdueDays,
              estimatedFine: Number(it.estimatedFine || (overdueDays > 0 ? overdueDays * 2000 : 0)),
              renewCount: it.renewCount || 0,
            });
          }
        }
        setPool(flat);
      } catch {
        message.error('Không tải được danh sách sách đang mượn');
      }
    } finally {
      setPoolLoading(false);
    }
  };

  useEffect(() => {
    void loadPool();
    scanRef.current?.focus?.();
  }, []);

  const filteredPool = useMemo(() => {
    const q = poolQ.trim().toLowerCase();
    if (!q) return pool;
    return pool.filter(
      (p) =>
        p.barcode?.toLowerCase().includes(q) ||
        p.bookTitle?.toLowerCase().includes(q) ||
        p.readerName?.toLowerCase().includes(q) ||
        p.slipCode?.toLowerCase().includes(q),
    );
  }, [pool, poolQ]);

  const reader = useMemo(() => {
    if (!lines.length) return null;
    return {
      id: lines[0].readerId,
      name: lines[0].readerName,
      slipCodes: [...new Set(lines.map((l) => l.slipCode))],
    };
  }, [lines]);

  const overdueTotal = lines.reduce((s, l) => s + Number(l.estimatedFine || 0), 0);
  const damageTotal = lines.reduce((s, l) => s + Number(l.damageEstimate || 0), 0);
  const grandTotal = overdueTotal + damageTotal;

  const addFromPreview = (p: Preview) => {
    if (!p.barcode) {
      message.warning('Mục này thiếu mã vạch');
      return;
    }
    if (lines.some((l) => l.barcode === p.barcode)) {
      message.warning('Mã vạch đã có trong danh sách trả');
      setHighlightBarcode(p.barcode);
      return;
    }
    if (lines.length && lines[0].readerId !== p.readerId) {
      message.error('Chỉ nhận trả cùng một độc giả trong một phiên');
      return;
    }
    setLines((prev) => [
      ...prev,
      { ...p, condition: 'NORMAL', damageEstimate: 0, note: undefined },
    ]);
    setHighlightBarcode(p.barcode);
    void loadHoldHints(p.bookId);
  };

  const preview = async (codeRaw?: string) => {
    const code = (codeRaw ?? barcode).trim();
    if (!code) return;
    setHighlightBarcode(code);
    const fromPool = pool.find((p) => p.barcode === code);
    if (fromPool) {
      addFromPreview(fromPool);
      setBarcode('');
      return;
    }
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Preview>>('/returns/preview', {
        params: { barcode: code },
      });
      addFromPreview(data.data);
      setBarcode('');
      scanRef.current?.focus?.();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Không tìm thấy sách đang mượn với mã này');
    } finally {
      setLoading(false);
    }
  };

  const loadHoldHints = async (bookId?: number) => {
    if (!bookId) return;
    try {
      const { data } = await api.get<ApiResponse<ReservationHint[]>>('/reservations', {
        params: { page: 0, size: 20 },
      });
      const list = (data.data || []).filter(
        (r) =>
          (r.status === 'WAITING' || r.status === 'READY' || r.status === 'QUEUED') &&
          r.bookTitle,
      );
      // Gợi ý đặt trước liên quan (không lọc chắc bookId nếu API chưa có)
      setHoldHints(list.slice(0, 2));
    } catch {
      /* bỏ qua */
    }
  };

  const updateLine = (itemId: number, patch: Partial<Line>) => {
    setLines((prev) =>
      prev.map((l) => {
        if (l.itemId !== itemId) return l;
        const next = { ...l, ...patch };
        if (patch.condition != null) {
          next.damageEstimate = DAMAGE_ESTIMATE[patch.condition] || 0;
          if (patch.condition === 'NORMAL') next.note = undefined;
        }
        return next;
      }),
    );
  };

  const clearSession = () => {
    setLines([]);
    setBarcode('');
    setHoldHints([]);
    setPayMode('COUNTER');
    setPayMethod('CASH');
    scanRef.current?.focus?.();
  };

  const confirm = async () => {
    if (!lines.length) {
      message.warning('Chưa có sách để trả');
      return;
    }
    for (const l of lines) {
      if (
        (l.condition === 'DAMAGED_MINOR' || l.condition === 'DAMAGED_MAJOR') &&
        !l.note?.trim()
      ) {
        message.warning(`Nhập mô tả hư hỏng cho sách: ${l.bookTitle}`);
        return;
      }
    }
    setLoading(true);
    try {
      await api.post('/returns', {
        collectPayment: payMode === 'COUNTER' && grandTotal > 0,
        items: lines.map((l) => ({
          copyBarcode: l.barcode,
          condition: l.condition,
          note: l.note,
        })),
      });
      message.success('Trả sách thành công');
      clearSession();
      void loadPool();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Trả sách thất bại');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        clearSession();
      }
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
        e.preventDefault();
        void confirm();
      }
      if (e.key === 'F2') {
        e.preventDefault();
        scanRef.current?.focus?.();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lines, payMode, grandTotal, loading]);

  return (
    <div style={{ maxWidth: 1480, margin: '0 auto' }}>
      <div
        style={{
          ...panel,
          padding: '10px 16px',
          marginBottom: 14,
          display: 'flex',
          justifyContent: 'space-between',
          gap: 12,
          flexWrap: 'wrap',
          alignItems: 'center',
        }}
      >
        <div>
          <Typography.Title level={4} style={{ margin: 0 }}>
            Trả sách & xử lý phạt tại quầy
          </Typography.Title>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            Phiên {sessionCode} · Quầy 01
          </Typography.Text>
        </div>
        <Space wrap>
          <Tag color="success" style={{ margin: 0 }}>
            Quầy đang mở
          </Tag>
          <Button size="small" onClick={clearSession}>
            Hủy phiên (Esc)
          </Button>
        </Space>
      </div>

      <Row gutter={[14, 14]}>
        <Col xs={24} xl={15}>
          {/* Banner quét */}
          <div
            style={{
              ...panel,
              padding: 16,
              marginBottom: 14,
              background: 'linear-gradient(120deg,#1D4ED8 0%,#3B82F6 55%,#60A5FA 100%)',
              border: 'none',
              color: '#fff',
            }}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                gap: 8,
                marginBottom: 10,
                flexWrap: 'wrap',
              }}
            >
              <Space>
                <ScanOutlined />
                <Typography.Text strong style={{ color: '#fff' }}>
                  Quét mã vạch trên bìa sách cần trả
                </Typography.Text>
              </Space>
              <Space>
                <Space size={6}>
                  <Typography.Text style={{ color: 'rgba(255,255,255,0.85)', fontSize: 12 }}>
                    Tự nhận
                  </Typography.Text>
                  <Switch size="small" checked={autoEnter} onChange={setAutoEnter} />
                </Space>
                <Button size="small" icon={<CameraOutlined />}>
                  Máy quét / camera
                </Button>
              </Space>
            </div>
            <div style={{ display: 'flex', gap: 8 }}>
              <Input
                ref={scanRef}
                size="large"
                value={barcode}
                onChange={(e) => setBarcode(e.target.value)}
                onPressEnter={() => {
                  if (autoEnter) void preview();
                }}
                placeholder="Đưa mã vạch bản sao vào đây rồi nhấn Enter…"
                style={{ flex: 1 }}
                prefix={<ScanOutlined style={{ color: '#64748B' }} />}
              />
              <Button size="large" loading={loading} onClick={() => void preview()}>
                Nhận trả
              </Button>
            </div>
          </div>

          {holdHints.length > 0 && (
            <Alert
              style={{ marginBottom: 14, borderRadius: 10 }}
              type="warning"
              showIcon
              icon={<WarningOutlined />}
              message="Đầu sách có người đặt trước (quy tắc giữ chỗ)"
              description={
                <div>
                  {holdHints.map((h) => (
                    <div key={h.id} style={{ marginBottom: 4 }}>
                      <strong>{h.bookTitle}</strong>
                      {h.readerName ? ` · giữ cho ${h.readerName}` : ''}
                      {h.readyUntil
                        ? ` đến ${dayjs(h.readyUntil).format('HH:mm DD/MM')}`
                        : ''}
                    </div>
                  ))}
                  <Button size="small" icon={<PrinterOutlined />} style={{ marginTop: 6 }}>
                    In phiếu giữ chỗ
                  </Button>
                </div>
              }
            />
          )}

          {/* Danh sách đang mượn — chọn hoặc quét để đưa vào phiên trả */}
          <div style={{ ...panel, padding: 16, marginBottom: 14 }}>
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
                Sách đang mượn ({filteredPool.length}/{pool.length})
              </Typography.Text>
              <Space wrap>
                <Input
                  allowClear
                  style={{ width: 260 }}
                  prefix={<SearchOutlined />}
                  placeholder="Lọc độc giả / sách / mã vạch…"
                  value={poolQ}
                  onChange={(e) => setPoolQ(e.target.value)}
                />
                <Button icon={<ReloadOutlined />} onClick={() => void loadPool()}>
                  Tải lại
                </Button>
              </Space>
            </div>
            <Typography.Paragraph type="secondary" style={{ marginTop: 0, fontSize: 12 }}>
              Hiện toàn bộ sách đang mượn. Bấm <strong>Thêm trả</strong> hoặc quét mã vạch để đưa vào
              phiên xử lý bên dưới.
            </Typography.Paragraph>
            <Table
              size="small"
              rowKey="itemId"
              loading={poolLoading}
              dataSource={filteredPool}
              pagination={{ pageSize: 8, showSizeChanger: false }}
              scroll={{ x: 720 }}
              rowClassName={(r) =>
                highlightBarcode && r.barcode === highlightBarcode
                  ? 'ant-table-row-selected'
                  : lines.some((l) => l.barcode === r.barcode)
                    ? 'ant-table-row-selected'
                    : ''
              }
              locale={{ emptyText: 'Chưa có sách đang mượn trong hệ thống' }}
              columns={[
                {
                  title: 'Độc giả',
                  dataIndex: 'readerName',
                  ellipsis: true,
                  width: 140,
                },
                {
                  title: 'Sách',
                  dataIndex: 'bookTitle',
                  ellipsis: true,
                },
                {
                  title: 'Mã vạch',
                  dataIndex: 'barcode',
                  width: 130,
                  render: (v: string) => (
                    <Typography.Text style={{ fontFamily: 'ui-monospace, monospace', fontSize: 12 }}>
                      {v}
                    </Typography.Text>
                  ),
                },
                {
                  title: 'Hạn trả',
                  dataIndex: 'dueDate',
                  width: 110,
                  render: (v: string) => (v ? dayjs(v).format('DD/MM/YYYY') : '—'),
                },
                {
                  title: 'Trạng thái',
                  width: 130,
                  render: (_: unknown, r: Preview) =>
                    r.overdueDays > 0 ? (
                      <Tag color="error">Quá hạn {r.overdueDays} ngày</Tag>
                    ) : (
                      <Tag color="success">Trong hạn</Tag>
                    ),
                },
                {
                  title: '',
                  width: 100,
                  fixed: 'right',
                  render: (_: unknown, r: Preview) => {
                    const added = lines.some((l) => l.barcode === r.barcode);
                    return (
                      <Button
                        size="small"
                        type={added ? 'default' : 'primary'}
                        disabled={added}
                        icon={<PlusOutlined />}
                        onClick={() => addFromPreview(r)}
                      >
                        {added ? 'Đã thêm' : 'Thêm trả'}
                      </Button>
                    );
                  },
                },
              ]}
            />
          </div>

          <div style={{ ...panel, padding: 16, marginBottom: 14 }}>
            <Typography.Text
              strong
              style={{
                display: 'block',
                marginBottom: 12,
                fontSize: 12,
                letterSpacing: 0.4,
                color: '#64748B',
                textTransform: 'uppercase',
              }}
            >
              Phiên đang tiếp nhận trả ({lines.length})
            </Typography.Text>

            {lines.length === 0 ? (
              <div
                style={{
                  padding: '40px 12px',
                  textAlign: 'center',
                  color: '#94A3B8',
                  background: '#F8FAFC',
                  borderRadius: 8,
                }}
              >
                Chưa có sách — quét mã vạch bản sao đang mượn để bắt đầu
              </div>
            ) : (
              <Space direction="vertical" style={{ width: '100%' }} size={12}>
                {lines.map((l, idx) => {
                  const overdue = Number(l.overdueDays || 0) > 0;
                  return (
                    <div
                      key={l.itemId}
                      style={{
                        border: `1px solid ${overdue ? '#FECACA' : tokens.color.border}`,
                        borderRadius: 10,
                        padding: 14,
                        background: overdue ? '#FFF7F7' : '#fff',
                      }}
                    >
                      <div
                        style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          gap: 8,
                          marginBottom: 10,
                        }}
                      >
                        <Space align="start">
                          <div
                            style={{
                              width: 40,
                              height: 54,
                              borderRadius: 4,
                              background: overdue
                                ? 'linear-gradient(160deg,#7F1D1D,#EF4444)'
                                : 'linear-gradient(160deg,#1E3A8A,#60A5FA)',
                              color: '#fff',
                              fontSize: 10,
                              fontWeight: 700,
                              display: 'grid',
                              placeItems: 'center',
                            }}
                          >
                            #{idx + 1}
                          </div>
                          <div>
                            <Typography.Text strong>{l.bookTitle}</Typography.Text>
                            <div>
                              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                Mã vạch {l.barcode} · Phiếu {l.slipCode} · Hạn{' '}
                                {dayjs(l.dueDate).format('DD/MM/YYYY')}
                              </Typography.Text>
                            </div>
                            <Space size={6} style={{ marginTop: 6 }} wrap>
                              {overdue ? (
                                <Tag color="error">Quá hạn {l.overdueDays} ngày</Tag>
                              ) : (
                                <Tag color="success">Đúng hạn</Tag>
                              )}
                              {l.condition !== 'NORMAL' && (
                                <Tag color="warning">Hư hỏng / mất bản sao</Tag>
                              )}
                            </Space>
                          </div>
                        </Space>
                        <Button
                          type="text"
                          danger
                          icon={<DeleteOutlined />}
                          onClick={() =>
                            setLines((prev) => prev.filter((x) => x.itemId !== l.itemId))
                          }
                        />
                      </div>

                      <Row gutter={[12, 12]}>
                        <Col xs={24} md={12}>
                          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                            Tình trạng vật lý khi trả
                          </Typography.Text>
                          <Select
                            style={{ width: '100%', marginTop: 4 }}
                            value={l.condition}
                            options={CONDITIONS}
                            onChange={(val) => updateLine(l.itemId, { condition: val })}
                          />
                        </Col>
                        <Col xs={24} md={12}>
                          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                            Phạt phát sinh (ước tính)
                          </Typography.Text>
                          <div style={{ marginTop: 6 }}>
                            {overdue && (
                              <div style={{ color: '#B91C1C', fontWeight: 600 }}>
                                Quá hạn: {l.overdueDays} ngày × {fmtMoney(OVERDUE_PER_DAY)} ={' '}
                                {fmtMoney(Number(l.estimatedFine || 0))}
                              </div>
                            )}
                            {l.damageEstimate > 0 && (
                              <div style={{ color: '#C2410C', fontWeight: 600 }}>
                                Hư hỏng / mất: {fmtMoney(l.damageEstimate)}
                                <Typography.Text type="secondary" style={{ fontSize: 11 }}>
                                  {' '}
                                  (ước tính giao diện)
                                </Typography.Text>
                              </div>
                            )}
                            {!overdue && l.damageEstimate === 0 && (
                              <Typography.Text type="secondary">Không phát sinh phạt</Typography.Text>
                            )}
                          </div>
                        </Col>
                      </Row>

                      {(l.condition === 'DAMAGED_MINOR' ||
                        l.condition === 'DAMAGED_MAJOR' ||
                        l.condition === 'LOST') && (
                        <Input.TextArea
                          style={{ marginTop: 10 }}
                          rows={2}
                          placeholder="Mô tả hư hỏng / mất (bắt buộc với hư hỏng)…"
                          value={l.note}
                          onChange={(e) => updateLine(l.itemId, { note: e.target.value })}
                        />
                      )}
                    </div>
                  );
                })}
              </Space>
            )}
          </div>

          <div
            style={{
              ...panel,
              padding: '10px 14px',
              display: 'flex',
              justifyContent: 'space-between',
              gap: 8,
              flexWrap: 'wrap',
              alignItems: 'center',
              background: '#F8FAFC',
            }}
          >
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              Đang nhận {lines.length} cuốn
              {reader ? ` · Độc giả ${reader.name}` : ''} · Mã phiên {sessionCode}
            </Typography.Text>
            <Typography.Text type="secondary" style={{ fontSize: 11 }}>
              F2: ô mã vạch · Ctrl+S: xác nhận · Esc: hủy
            </Typography.Text>
          </div>
        </Col>

        <Col xs={24} xl={9}>
          <div style={{ ...panel, padding: 16, marginBottom: 14 }}>
            <Typography.Text strong style={{ display: 'block', marginBottom: 10 }}>
              Độc giả đang trả
            </Typography.Text>
            {reader ? (
              <div
                style={{
                  padding: 12,
                  borderRadius: 10,
                  background: 'linear-gradient(180deg,#F8FBFF,#F1F5F9)',
                  border: '1px solid #E2E8F0',
                }}
              >
                <Space align="start">
                  <div
                    style={{
                      width: 48,
                      height: 48,
                      borderRadius: 10,
                      background: 'linear-gradient(135deg,#0F172A,#334155)',
                      color: '#fff',
                      display: 'grid',
                      placeItems: 'center',
                      fontWeight: 800,
                    }}
                  >
                    {reader.name.slice(0, 1).toUpperCase()}
                  </div>
                  <div>
                    <Typography.Text strong>{reader.name}</Typography.Text>
                    <div>
                      <Tag color="success">Đang trả sách</Tag>
                    </div>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      Phiếu: {reader.slipCodes.join(', ')}
                    </Typography.Text>
                  </div>
                </Space>
              </div>
            ) : (
              <Alert
                type="info"
                showIcon
                message="Chưa có độc giả"
                description="Quét sách đang mượn — hệ thống tự nhận diện độc giả từ phiếu."
              />
            )}
          </div>

          <div style={{ ...panel, padding: 16, marginBottom: 14 }}>
            <Typography.Text strong style={{ display: 'block', marginBottom: 12 }}>
              Tổng hợp phí phạt phát sinh
            </Typography.Text>
            <Space direction="vertical" style={{ width: '100%' }} size={8}>
              <Row justify="space-between">
                <Typography.Text type="secondary">Phạt quá hạn</Typography.Text>
                <Typography.Text>{fmtMoney(overdueTotal)}</Typography.Text>
              </Row>
              <Row justify="space-between">
                <Typography.Text type="secondary">Phạt hư hỏng / mất</Typography.Text>
                <Typography.Text>{fmtMoney(damageTotal)}</Typography.Text>
              </Row>
              <Row justify="space-between">
                <Typography.Text type="secondary">Phí xử lý</Typography.Text>
                <Typography.Text type="success">Miễn</Typography.Text>
              </Row>
              <div
                style={{
                  borderTop: `1px dashed ${tokens.color.border}`,
                  paddingTop: 10,
                  display: 'flex',
                  justifyContent: 'space-between',
                }}
              >
                <Typography.Text strong>Tổng</Typography.Text>
                <Typography.Text strong style={{ fontSize: 18, color: '#0F172A' }}>
                  {fmtMoney(grandTotal)}
                </Typography.Text>
              </div>
            </Space>
          </div>

          <div style={{ ...panel, padding: 16, marginBottom: 14 }}>
            <Typography.Text strong style={{ display: 'block', marginBottom: 10 }}>
              Hình thức thu
            </Typography.Text>
            <Radio.Group
              value={payMode}
              onChange={(e) => setPayMode(e.target.value)}
              style={{ width: '100%' }}
            >
              <Space direction="vertical" style={{ width: '100%' }}>
                <Radio value="COUNTER" style={{ width: '100%' }}>
                  Thu tiền phạt ngay tại quầy
                </Radio>
                {payMode === 'COUNTER' && (
                  <Radio.Group
                    value={payMethod}
                    onChange={(e) => setPayMethod(e.target.value)}
                    optionType="button"
                    buttonStyle="solid"
                    style={{ marginLeft: 24, marginBottom: 4 }}
                    options={[
                      { value: 'CASH', label: 'Tiền mặt' },
                      { value: 'QR', label: 'Mã QR' },
                      { value: 'CARD', label: 'Thẻ thư viện' },
                    ]}
                  />
                )}
                <Radio value="DEBT">Ghi nợ vào tài khoản độc giả</Radio>
              </Space>
            </Radio.Group>
          </div>

          <div style={{ ...panel, padding: 16 }}>
            <Button
              type="primary"
              size="large"
              block
              loading={loading}
              disabled={!lines.length}
              onClick={() => void confirm()}
              style={{
                height: 48,
                fontWeight: 700,
                background: '#0F172A',
                borderColor: '#0F172A',
              }}
            >
              Xác nhận hoàn tất & in biên lai
            </Button>
            <Space.Compact style={{ width: '100%', marginTop: 10 }}>
              <Button style={{ width: '50%' }} icon={<PrinterOutlined />}>
                In lại biên lai
              </Button>
              <Button
                style={{ width: '50%' }}
                onClick={() => message.info('Chức năng giảm/miễn phạt sẽ mở ở bước tiếp theo')}
              >
                Giảm / miễn phạt
              </Button>
            </Space.Compact>
            <Button block type="text" danger style={{ marginTop: 6 }} onClick={clearSession}>
              Hủy bỏ phiên làm việc (Esc)
            </Button>
          </div>
        </Col>
      </Row>
    </div>
  );
}
