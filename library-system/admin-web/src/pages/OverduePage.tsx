import { useCallback, useEffect, useMemo, useState, type CSSProperties } from 'react';
import {
  Alert,
  Button,
  Col,
  Input,
  Radio,
  Row,
  Select,
  Space,
  Tag,
  Typography,
  message,
} from 'antd';
import {
  ExclamationCircleFilled,
  PrinterOutlined,
  ReloadOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { api, type ApiResponse } from '@/services/api';
import { tokens } from '@/styles/tokens';

type OverdueItem = {
  id: number;
  bookId?: number;
  bookTitle?: string;
  barcode?: string;
  dueDate?: string;
  overdue?: boolean;
  estimatedFine?: number;
  renewCount?: number;
  status: string;
  slipId?: number;
  slipCode?: string;
  readerId?: number;
  readerName?: string;
  readerCode?: string;
  overdueDays?: number;
};

type LineState = {
  condition: string;
  note?: string;
  damageEstimate: number;
  selected: boolean;
};

type CaseGroup = {
  key: string;
  slipId?: number;
  slipCode: string;
  readerId?: number;
  readerName: string;
  readerCode?: string;
  items: OverdueItem[];
  maxOverdueDays: number;
  totalFine: number;
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

const DAMAGE_ESTIMATE: Record<string, number> = {
  DAMAGED_MINOR: 90000,
  DAMAGED_MAJOR: 180000,
  LOST: 250000,
};

const OVERDUE_PER_DAY = 2000;
const fmtMoney = (n: number) => `${Math.round(n).toLocaleString('vi-VN')}đ`;

export function OverduePage() {
  const [rows, setRows] = useState<OverdueItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [q, setQ] = useState('');
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const [lineState, setLineState] = useState<Record<number, LineState>>({});
  const [payMode, setPayMode] = useState<'COUNTER' | 'DEBT'>('COUNTER');
  const [payMethod, setPayMethod] = useState<'CASH' | 'QR' | 'CARD'>('CASH');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      // Ưu tiên API borrowing (đủ độc giả) rồi lọc quá hạn; fallback /borrows/overdue
      try {
        const { data } = await api.get<
          ApiResponse<
            Array<{
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
            }>
          >
        >('/returns/borrowing');
        const overdue = (data.data || [])
          .filter((i) => i.overdueDays > 0)
          .map((i) => ({
            id: i.itemId,
            bookId: i.bookId,
            bookTitle: i.bookTitle,
            barcode: i.barcode,
            dueDate: i.dueDate,
            overdue: true,
            estimatedFine: Number(i.estimatedFine || 0),
            renewCount: i.renewCount,
            status: 'BORROWING',
            slipId: i.slipId,
            slipCode: i.slipCode,
            readerId: i.readerId,
            readerName: i.readerName,
            overdueDays: i.overdueDays,
          }));
        setRows(overdue);
        initSelection(overdue);
      } catch {
        const { data } = await api.get<ApiResponse<OverdueItem[]>>('/borrows/overdue');
        const list = (data.data || []).map((i) => ({
          ...i,
          overdueDays:
            i.overdueDays ??
            (i.dueDate
              ? Math.max(0, dayjs().startOf('day').diff(dayjs(i.dueDate).startOf('day'), 'day'))
              : 0),
          estimatedFine: Number(i.estimatedFine || 0),
        }));
        setRows(list);
        initSelection(list);
      }
    } catch {
      message.error('Không tải được hồ sơ quá hạn');
    } finally {
      setLoading(false);
    }
  }, []);

  const initSelection = (list: OverdueItem[]) => {
    const groups = groupCases(list);
    setSelectedKey((curr) => {
      if (curr && groups.some((g) => g.key === curr)) return curr;
      return groups[0]?.key ?? null;
    });
    const state: Record<number, LineState> = {};
    for (const it of list) {
      state[it.id] = {
        condition: 'NORMAL',
        damageEstimate: 0,
        selected: true,
        note: undefined,
      };
    }
    setLineState(state);
  };

  useEffect(() => {
    void load();
  }, [load]);

  const cases = useMemo(() => {
    const keyword = q.trim().toLowerCase();
    const list = !keyword
      ? rows
      : rows.filter(
          (r) =>
            r.readerName?.toLowerCase().includes(keyword) ||
            r.readerCode?.toLowerCase().includes(keyword) ||
            r.slipCode?.toLowerCase().includes(keyword) ||
            r.bookTitle?.toLowerCase().includes(keyword) ||
            r.barcode?.toLowerCase().includes(keyword),
        );
    return groupCases(list);
  }, [rows, q]);

  const active = useMemo(
    () => cases.find((c) => c.key === selectedKey) || null,
    [cases, selectedKey],
  );

  const activeLines = useMemo(() => {
    if (!active) return [];
    return active.items.filter((i) => lineState[i.id]?.selected !== false);
  }, [active, lineState]);

  const overdueTotal = activeLines.reduce((s, i) => s + Number(i.estimatedFine || 0), 0);
  const damageTotal = activeLines.reduce(
    (s, i) => s + Number(lineState[i.id]?.damageEstimate || 0),
    0,
  );
  const grandTotal = overdueTotal + damageTotal;
  const damageCount = activeLines.filter((i) => {
    const c = lineState[i.id]?.condition;
    return c && c !== 'NORMAL';
  }).length;

  const updateLine = (id: number, patch: Partial<LineState>) => {
    setLineState((prev) => {
      const cur = prev[id] || { condition: 'NORMAL', damageEstimate: 0, selected: true };
      const next = { ...cur, ...patch };
      if (patch.condition != null) {
        next.damageEstimate = DAMAGE_ESTIMATE[patch.condition] || 0;
        if (patch.condition === 'NORMAL') next.note = undefined;
      }
      return { ...prev, [id]: next };
    });
  };

  const confirmReturn = async () => {
    if (!active || !activeLines.length) {
      message.warning('Chọn ít nhất một cuốn để trả');
      return;
    }
    for (const it of activeLines) {
      const st = lineState[it.id];
      if (
        st &&
        (st.condition === 'DAMAGED_MINOR' || st.condition === 'DAMAGED_MAJOR') &&
        !st.note?.trim()
      ) {
        message.warning(`Nhập mô tả hư hỏng cho: ${it.bookTitle}`);
        return;
      }
      if (!it.barcode) {
        message.warning('Có cuốn thiếu mã vạch');
        return;
      }
    }
    setSubmitting(true);
    try {
      await api.post('/returns', {
        collectPayment: payMode === 'COUNTER' && grandTotal > 0,
        items: activeLines.map((it) => ({
          copyBarcode: it.barcode,
          condition: lineState[it.id]?.condition || 'NORMAL',
          note: lineState[it.id]?.note,
        })),
      });
      message.success(
        grandTotal > 0
          ? `Đã trả sách và ghi nhận thu ${fmtMoney(grandTotal)}`
          : 'Đã trả sách thành công',
      );
      void load();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Trả sách thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault();
        void confirmReturn();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active, activeLines, lineState, payMode, grandTotal]);

  const controlCode = active
    ? `OVD-${active.maxOverdueDays}D-${String(active.slipCode || active.key).slice(-4)}`
    : '—';

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
            Mượn – Trả · Sách quá hạn
          </Typography.Text>
          <Typography.Title level={4} style={{ margin: '2px 0 0' }}>
            Hồ sơ trả sách quá hạn
          </Typography.Title>
        </div>
        <Button icon={<ReloadOutlined />} loading={loading} onClick={() => void load()}>
          Tải lại
        </Button>
      </div>

      {active && (
        <Alert
          type="error"
          showIcon
          icon={<ExclamationCircleFilled />}
          style={{ marginBottom: 14, borderRadius: 10 }}
          message={`Vi phạm lưu thông — quá hạn ${active.maxOverdueDays} ngày`}
          description={
            <span>
              {active.items.length} cuốn quá hạn · mức {fmtMoney(OVERDUE_PER_DAY)}/cuốn/ngày · mã kiểm
              soát <Typography.Text code>{controlCode}</Typography.Text>. Có thể tạm khóa quyền mượn
              mới đến khi thu phạt / trả đủ sách.
            </span>
          }
        />
      )}

      <Row gutter={[14, 14]}>
        <Col xs={24} lg={8}>
          <div style={{ ...panel, padding: 12, minHeight: 520 }}>
            <Input
              allowClear
              placeholder="Lọc độc giả / phiếu / sách…"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              style={{ marginBottom: 10 }}
            />
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {cases.length} hồ sơ quá hạn
            </Typography.Text>
            <div style={{ marginTop: 8, maxHeight: 560, overflow: 'auto' }}>
              {cases.length === 0 && (
                <Alert type="success" showIcon message="Không có sách quá hạn" />
              )}
              {cases.map((c) => {
                const activeCase = c.key === selectedKey;
                return (
                  <button
                    key={c.key}
                    type="button"
                    onClick={() => setSelectedKey(c.key)}
                    style={{
                      display: 'block',
                      width: '100%',
                      textAlign: 'left',
                      border: `1px solid ${activeCase ? '#FCA5A5' : tokens.color.border}`,
                      background: activeCase ? '#FEF2F2' : '#fff',
                      borderRadius: 10,
                      padding: 12,
                      marginBottom: 8,
                      cursor: 'pointer',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                      <Typography.Text strong>{c.slipCode}</Typography.Text>
                      <Tag color="error">+{c.maxOverdueDays} ngày</Tag>
                    </div>
                    <Typography.Text style={{ display: 'block', marginTop: 4 }}>
                      {c.readerName}
                    </Typography.Text>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {c.items.length} cuốn · phạt ~ {fmtMoney(c.totalFine)}
                    </Typography.Text>
                  </button>
                );
              })}
            </div>
          </div>
        </Col>

        <Col xs={24} lg={16}>
          {!active ? (
            <div style={{ ...panel, padding: 24 }}>
              <Alert type="info" showIcon message="Chọn một hồ sơ bên trái để xử lý trả / thu phạt" />
            </div>
          ) : (
            <Row gutter={[14, 14]}>
              <Col xs={24} xl={14}>
                <div style={{ ...panel, padding: 16, marginBottom: 14 }}>
                  <Space wrap style={{ marginBottom: 12 }}>
                    <Typography.Text strong style={{ fontSize: 16 }}>
                      {active.slipCode}
                    </Typography.Text>
                    <Tag color="error">Quá hạn {active.maxOverdueDays} ngày</Tag>
                    <Tag color="processing">Đang xử lý trả</Tag>
                  </Space>
                  <Row gutter={12}>
                    {[
                      {
                        label: 'Số cuốn',
                        value: `${activeLines.length}/${active.items.length}`,
                      },
                      {
                        label: 'Ngày quá hạn',
                        value: `${active.maxOverdueDays} ngày`,
                      },
                      {
                        label: 'Hư hỏng / mất',
                        value: `${damageCount} cuốn`,
                      },
                      {
                        label: 'Phạt quá hạn',
                        value: fmtMoney(overdueTotal),
                      },
                    ].map((m) => (
                      <Col span={6} key={m.label}>
                        <Typography.Text type="secondary" style={{ fontSize: 11 }}>
                          {m.label}
                        </Typography.Text>
                        <div>
                          <Typography.Text strong>{m.value}</Typography.Text>
                        </div>
                      </Col>
                    ))}
                  </Row>
                </div>

                <div style={{ ...panel, padding: 16 }}>
                  <Typography.Text
                    strong
                    style={{
                      display: 'block',
                      marginBottom: 12,
                      fontSize: 12,
                      color: '#64748B',
                      textTransform: 'uppercase',
                      letterSpacing: 0.4,
                    }}
                  >
                    Danh sách sách cần trả
                  </Typography.Text>
                  <Space direction="vertical" style={{ width: '100%' }} size={10}>
                    {active.items.map((it, idx) => {
                      const st = lineState[it.id] || {
                        condition: 'NORMAL',
                        damageEstimate: 0,
                        selected: true,
                      };
                      const overdueFine = Number(it.estimatedFine || 0);
                      const lineTotal = overdueFine + st.damageEstimate;
                      return (
                        <div
                          key={it.id}
                          style={{
                            border: `1px solid ${st.condition !== 'NORMAL' ? '#FDBA74' : '#FECACA'}`,
                            background: st.selected ? '#FFF7F7' : '#F8FAFC',
                            borderRadius: 10,
                            padding: 12,
                            opacity: st.selected ? 1 : 0.55,
                          }}
                        >
                          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                            <div>
                              <Typography.Text strong>
                                {idx + 1}. {it.bookTitle}
                              </Typography.Text>
                              <div>
                                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                  Mã vạch {it.barcode || '—'} · Hạn{' '}
                                  {it.dueDate ? dayjs(it.dueDate).format('DD/MM/YYYY') : '—'} · Quá
                                  hạn {it.overdueDays || 0} ngày
                                </Typography.Text>
                              </div>
                            </div>
                            <Button
                              size="small"
                              type={st.selected ? 'default' : 'primary'}
                              onClick={() => updateLine(it.id, { selected: !st.selected })}
                            >
                              {st.selected ? 'Bỏ khỏi phiên' : 'Thêm vào phiên'}
                            </Button>
                          </div>

                          {st.selected && (
                            <>
                              <Row gutter={12} style={{ marginTop: 10 }}>
                                <Col span={12}>
                                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                    Tình trạng vật lý
                                  </Typography.Text>
                                  <Select
                                    style={{ width: '100%', marginTop: 4 }}
                                    value={st.condition}
                                    options={CONDITIONS}
                                    onChange={(v) => updateLine(it.id, { condition: v })}
                                  />
                                </Col>
                                <Col span={12}>
                                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                    Phạt dòng này
                                  </Typography.Text>
                                  <div style={{ marginTop: 6 }}>
                                    <Typography.Text type="danger" strong>
                                      {fmtMoney(lineTotal)}
                                    </Typography.Text>
                                    <div>
                                      <Typography.Text type="secondary" style={{ fontSize: 11 }}>
                                        Quá hạn {fmtMoney(overdueFine)}
                                        {st.damageEstimate > 0
                                          ? ` + hư hỏng ${fmtMoney(st.damageEstimate)}`
                                          : ''}
                                      </Typography.Text>
                                    </div>
                                  </div>
                                </Col>
                              </Row>
                              {st.condition !== 'NORMAL' && (
                                <Input.TextArea
                                  style={{ marginTop: 8 }}
                                  rows={2}
                                  placeholder="Mô tả hư hỏng / mất (bắt buộc)…"
                                  value={st.note}
                                  onChange={(e) => updateLine(it.id, { note: e.target.value })}
                                />
                              )}
                            </>
                          )}
                        </div>
                      );
                    })}
                  </Space>
                </div>
              </Col>

              <Col xs={24} xl={10}>
                <div style={{ ...panel, padding: 16, marginBottom: 14 }}>
                  <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
                    Độc giả
                  </Typography.Text>
                  <Space align="start">
                    <div
                      style={{
                        width: 48,
                        height: 48,
                        borderRadius: 10,
                        background: 'linear-gradient(135deg,#7F1D1D,#EF4444)',
                        color: '#fff',
                        display: 'grid',
                        placeItems: 'center',
                        fontWeight: 800,
                      }}
                    >
                      {active.readerName.slice(0, 1).toUpperCase()}
                    </div>
                    <div>
                      <Typography.Text strong>{active.readerName}</Typography.Text>
                      <div>
                        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                          {active.readerCode || '—'} · {active.items.length} cuốn quá hạn
                        </Typography.Text>
                      </div>
                      <Tag icon={<WarningOutlined />} color="error" style={{ marginTop: 6 }}>
                        Cần xử lý trước khi mượn mới
                      </Tag>
                    </div>
                  </Space>
                </div>

                <div style={{ ...panel, padding: 16, marginBottom: 14 }}>
                  <Typography.Text strong style={{ display: 'block', marginBottom: 10 }}>
                    Tổng hợp phí cần thu
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
                    <div
                      style={{
                        borderTop: `1px dashed ${tokens.color.border}`,
                        paddingTop: 10,
                        display: 'flex',
                        justifyContent: 'space-between',
                      }}
                    >
                      <Typography.Text strong>Tổng thu</Typography.Text>
                      <Typography.Text strong style={{ fontSize: 18, color: '#B91C1C' }}>
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
                      <Radio value="COUNTER">Thu tiền phạt ngay tại quầy</Radio>
                      {payMode === 'COUNTER' && (
                        <Radio.Group
                          value={payMethod}
                          onChange={(e) => setPayMethod(e.target.value)}
                          optionType="button"
                          buttonStyle="solid"
                          style={{ marginLeft: 24 }}
                          options={[
                            { value: 'CASH', label: 'Tiền mặt' },
                            { value: 'QR', label: 'Chuyển khoản / QR' },
                            { value: 'CARD', label: 'Thẻ thư viện' },
                          ]}
                        />
                      )}
                      {payMode === 'COUNTER' && payMethod === 'QR' && grandTotal > 0 && (
                        <Alert
                          style={{ marginLeft: 24 }}
                          type="info"
                          showIcon
                          message="Chuyển khoản / mã QR"
                          description={`Số tiền ${fmtMoney(grandTotal)} · nội dung gợi ý: ${controlCode} ${active.readerCode || ''}`}
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
                    loading={submitting}
                    disabled={!activeLines.length}
                    onClick={() => void confirmReturn()}
                    style={{
                      height: 48,
                      fontWeight: 700,
                      background: '#0F172A',
                      borderColor: '#0F172A',
                    }}
                  >
                    {grandTotal > 0
                      ? `Xác nhận trả & thu ${fmtMoney(grandTotal)}`
                      : 'Xác nhận trả sách'}
                  </Button>
                  <Space.Compact style={{ width: '100%', marginTop: 10 }}>
                    <Button style={{ width: '50%' }} icon={<PrinterOutlined />}>
                      In biên lai phạt
                    </Button>
                    <Button style={{ width: '50%' }} icon={<PrinterOutlined />}>
                      In phiếu giữ chỗ
                    </Button>
                  </Space.Compact>
                </div>
              </Col>
            </Row>
          )}
        </Col>
      </Row>
    </div>
  );
}

function groupCases(items: OverdueItem[]): CaseGroup[] {
  const map = new Map<string, CaseGroup>();
  for (const it of items) {
    const key = String(it.slipId || it.slipCode || it.readerId || it.id);
    const existing = map.get(key);
    if (!existing) {
      map.set(key, {
        key,
        slipId: it.slipId,
        slipCode: it.slipCode || `PHIEU-${it.id}`,
        readerId: it.readerId,
        readerName: it.readerName || 'Không rõ độc giả',
        readerCode: it.readerCode,
        items: [it],
        maxOverdueDays: it.overdueDays || 0,
        totalFine: Number(it.estimatedFine || 0),
      });
    } else {
      existing.items.push(it);
      existing.maxOverdueDays = Math.max(existing.maxOverdueDays, it.overdueDays || 0);
      existing.totalFine += Number(it.estimatedFine || 0);
    }
  }
  return [...map.values()].sort((a, b) => b.maxOverdueDays - a.maxOverdueDays);
}
