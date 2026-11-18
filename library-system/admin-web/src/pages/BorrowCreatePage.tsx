import { useEffect, useMemo, useRef, useState, type CSSProperties } from 'react';
import {
  Alert,
  Button,
  Col,
  Form,
  Input,
  InputNumber,
  Progress,
  Row,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { InputRef } from 'antd';
import {
  CheckCircleFilled,
  CloseCircleFilled,
  DeleteOutlined,
  PlusOutlined,
  PrinterOutlined,
  ReloadOutlined,
  ScanOutlined,
  SearchOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useLocation, useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import { api, type ApiResponse } from '@/services/api';
import { tokens } from '@/styles/tokens';
import { readerTypeVi, statusVi } from '@/utils/viLabels';

type ReservationHandoff = {
  fromReservation?: boolean;
  reservationId?: number;
  readerHint?: string;
  barcodeHint?: string;
};

type ReaderLookup = {
  id: number;
  readerCode: string;
  fullName: string;
  studentCode?: string;
  readerTypeCode?: string;
  readerTypeName?: string;
  cardNumber?: string;
  cardBarcode?: string;
  cardStatus?: string;
  userStatus?: string;
  blacklisted?: boolean;
};

type Eligibility = {
  eligible: boolean;
  violations: Array<{ code: string; message: string }>;
};

type CopyLookup = {
  id: number;
  barcode: string;
  bookTitle?: string;
  locationCode?: string;
  status: string;
  condition?: string;
};

type Line = {
  key: string;
  copyId: number;
  barcode: string;
  bookTitle?: string;
  locationCode?: string;
  condition?: string;
};

const DEFAULT_MAX = 5;
const LOAN_DAYS = 14;

const panel: CSSProperties = {
  background: '#fff',
  borderRadius: 12,
  border: `1px solid ${tokens.color.border}`,
  boxShadow: '0 1px 2px rgba(15,23,42,0.04)',
};

export function BorrowCreatePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const handoff = (location.state as ReservationHandoff | null) || null;
  const [form] = Form.useForm();
  const scanRef = useRef<InputRef>(null);
  const [reader, setReader] = useState<ReaderLookup | null>(null);
  const [eligibility, setEligibility] = useState<Eligibility | null>(null);
  const [lines, setLines] = useState<Line[]>([]);
  const [barcode, setBarcode] = useState('');
  const [poolQ, setPoolQ] = useState('');
  const [pool, setPool] = useState<CopyLookup[]>([]);
  const [poolLoading, setPoolLoading] = useState(false);
  const [autoAdd, setAutoAdd] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [highlightBarcode, setHighlightBarcode] = useState<string | null>(null);
  const [allowHoldBarcode, setAllowHoldBarcode] = useState<string | null>(
    handoff?.barcodeHint?.trim() || null,
  );

  const sessionCode = useMemo(() => `QUAY-${dayjs().format('DDMM-HHmm')}`, []);
  const fillPct = Math.min(100, Math.round((lines.length / DEFAULT_MAX) * 100));
  const dueDate = dayjs().add(LOAN_DAYS, 'day').format('DD/MM/YYYY');

  const loadPool = async (keyword = poolQ) => {
    setPoolLoading(true);
    try {
      const { data } = await api.get<ApiResponse<CopyLookup[]>>('/copies', {
        params: { status: 'AVAILABLE', q: keyword.trim() || undefined, page: 0, size: 50 },
      });
      setPool(data.data || []);
    } catch {
      message.error('Không tải được danh sách bản sao sẵn sàng');
    } finally {
      setPoolLoading(false);
    }
  };

  useEffect(() => {
    void loadPool('');
    scanRef.current?.focus?.();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const filteredPool = useMemo(() => {
    const q = poolQ.trim().toLowerCase();
    if (!q) return pool;
    return pool.filter(
      (p) =>
        p.barcode?.toLowerCase().includes(q) ||
        p.bookTitle?.toLowerCase().includes(q) ||
        p.locationCode?.toLowerCase().includes(q),
    );
  }, [pool, poolQ]);

  const loadEligibility = async (readerId: number) => {
    try {
      const { data } = await api.get<ApiResponse<Eligibility>>(`/readers/${readerId}/eligibility`);
      setEligibility(data.data);
    } catch {
      setEligibility(null);
    }
  };

  const lookupReader = async () => {
    const card = (form.getFieldValue('card') as string | undefined)?.trim();
    if (!card) {
      message.warning('Nhập mã thẻ / mã vạch độc giả');
      return;
    }
    try {
      const { data } = await api.get<ApiResponse<ReaderLookup>>('/readers/lookup', {
        params: { card },
      });
      setReader(data.data);
      await loadEligibility(data.data.id);
      message.success(`Độc giả: ${data.data.fullName}`);
      scanRef.current?.focus?.();
    } catch {
      setReader(null);
      setEligibility(null);
      message.error('Không tìm thấy độc giả');
    }
  };

  const addCopyFromPool = (c: CopyLookup, holdOverride?: string | null) => {
    if (lines.some((l) => l.barcode === c.barcode)) {
      message.warning('Mã vạch đã có trong phiếu');
      setHighlightBarcode(c.barcode);
      return;
    }
    if (lines.length >= DEFAULT_MAX) {
      message.warning(`Đã đạt giới hạn ${DEFAULT_MAX} cuốn`);
      return;
    }
    const holdOk =
      c.status === 'HOLD' &&
      (holdOverride === c.barcode || allowHoldBarcode === c.barcode);
    if (c.status !== 'AVAILABLE' && !holdOk) {
      message.error(`Bản sao ${c.barcode} không sẵn sàng (${statusVi(c.status)})`);
      return;
    }
    setLines((prev) => [
      ...prev,
      {
        key: String(c.id),
        copyId: c.id,
        barcode: c.barcode,
        bookTitle: c.bookTitle,
        locationCode: c.locationCode,
        condition: c.condition,
      },
    ]);
    setHighlightBarcode(c.barcode);
  };

  const addCopy = async (codeRaw?: string) => {
    const code = (codeRaw ?? barcode).trim();
    if (!code) return;
    setHighlightBarcode(code);
    const fromPool = pool.find((p) => p.barcode === code);
    if (fromPool) {
      addCopyFromPool(fromPool);
      setBarcode('');
      return;
    }
    try {
      const { data } = await api.get<ApiResponse<CopyLookup>>('/copies/lookup', {
        params: { barcode: code },
      });
      addCopyFromPool(data.data);
      setBarcode('');
    } catch {
      message.error('Không tìm thấy mã vạch');
    }
  };

  useEffect(() => {
    if (!handoff?.fromReservation) return;
    const apply = async () => {
      if (handoff.readerHint) {
        form.setFieldsValue({ card: handoff.readerHint });
        try {
          const { data } = await api.get<ApiResponse<ReaderLookup>>('/readers/lookup', {
            params: { card: handoff.readerHint },
          });
          setReader(data.data);
          await loadEligibility(data.data.id);
        } catch {
          message.warning('Không tự tìm được độc giả từ đặt trước — nhập mã thẻ thủ công');
        }
      }
      if (handoff.barcodeHint) {
        setAllowHoldBarcode(handoff.barcodeHint);
        setBarcode(handoff.barcodeHint);
        try {
          const { data } = await api.get<ApiResponse<CopyLookup>>('/copies/lookup', {
            params: { barcode: handoff.barcodeHint },
          });
          addCopyFromPool(data.data, handoff.barcodeHint);
        } catch {
          message.warning('Không tự thêm được bản sao giữ chỗ — quét lại mã vạch');
        }
      }
      navigate(location.pathname, { replace: true, state: null });
    };
    void apply();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const clearSession = () => {
    setReader(null);
    setEligibility(null);
    setLines([]);
    setBarcode('');
    form.resetFields();
    scanRef.current?.focus?.();
  };

  const submit = async () => {
    if (!reader) {
      message.warning('Chọn độc giả trước');
      return;
    }
    if (eligibility && !eligibility.eligible) {
      message.error('Độc giả chưa đủ điều kiện mượn');
      return;
    }
    if (!lines.length) {
      message.warning('Thêm ít nhất một bản sao');
      return;
    }
    setSubmitting(true);
    try {
      const note = form.getFieldValue('note') as string | undefined;
      const deposit = form.getFieldValue('deposit') as number | undefined;
      const { data } = await api.post<ApiResponse<{ id: number; slipCode: string }>>(
        '/borrows/counter',
        {
          readerId: reader.id,
          items: lines.map((l) => ({ copyId: l.copyId })),
          note,
          deposit,
        },
      );
      message.success(`Đã tạo phiếu ${data.data.slipCode}`);
      navigate('/borrows');
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Tạo phiếu thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        clearSession();
      }
      if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault();
        void submit();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reader, lines, eligibility, submitting]);

  const checks = useMemo(() => {
    const hasCard = reader?.cardStatus === 'ACTIVE';
    const accountOk = !reader?.userStatus || reader.userStatus === 'ACTIVE';
    const notBlacklisted = !reader?.blacklisted;
    const underLimit = lines.length <= DEFAULT_MAX;
    const eligibleApi = eligibility?.eligible !== false;
    return [
      { ok: !!reader && hasCard, label: 'Thẻ thư viện đang hoạt động' },
      { ok: !!reader && accountOk, label: 'Tài khoản độc giả hợp lệ' },
      { ok: !!reader && notBlacklisted, label: 'Không trong danh sách hạn chế' },
      { ok: underLimit, label: `Trong hạn mức mượn (${lines.length}/${DEFAULT_MAX})` },
      { ok: !!reader && eligibleApi, label: 'Đủ điều kiện nghiệp vụ cho mượn' },
    ];
  }, [reader, eligibility, lines.length]);

  const canSubmit =
    !!reader && lines.length > 0 && (!eligibility || eligibility.eligible) && !submitting;

  return (
    <div style={{ maxWidth: 1480, margin: '0 auto' }}>
      {/* Thanh phiên làm việc */}
      <div
        style={{
          ...panel,
          padding: '10px 16px',
          marginBottom: 14,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 12,
          flexWrap: 'wrap',
        }}
      >
        <Space size="middle" wrap>
          <Typography.Text strong style={{ fontSize: 15 }}>
            Tạo phiếu mượn (quầy)
          </Typography.Text>
          <Tag
            style={{
              margin: 0,
              background: '#EEF4FF',
              borderColor: '#C7DBFF',
              color: '#1D4ED8',
              fontWeight: 600,
            }}
          >
            Phiên: {sessionCode}
          </Tag>
        </Space>
        <Space wrap>
          <Tag color="success" style={{ margin: 0 }}>
            Quầy 01 — Đang mở
          </Tag>
          <Button size="small" onClick={clearSession}>
            Hủy phiên (Esc)
          </Button>
        </Space>
      </div>

      <Row gutter={[14, 14]}>
        {/* Cột trái */}
        <Col xs={24} xl={15}>
          <div style={{ ...panel, padding: 16, marginBottom: 14 }}>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: 12,
                gap: 8,
                flexWrap: 'wrap',
              }}
            >
              <Space>
                <ScanOutlined style={{ color: tokens.color.primary }} />
                <Typography.Text strong>Quét mã vạch bản sao sách</Typography.Text>
              </Space>
              <Space>
                <Space size={6}>
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    Tự thêm
                  </Typography.Text>
                  <Switch size="small" checked={autoAdd} onChange={setAutoAdd} />
                </Space>
                <Button size="small" icon={<SearchOutlined />}>
                  Tra cứu theo tên
                </Button>
              </Space>
            </div>

            <div
              style={{
                display: 'flex',
                gap: 8,
                marginBottom: 14,
              }}
            >
              <Input
                ref={scanRef}
                size="large"
                value={barcode}
                onChange={(e) => setBarcode(e.target.value)}
                onPressEnter={() => {
                  if (autoAdd) void addCopy();
                }}
                placeholder="Đưa mã vạch vào đây rồi nhấn Enter…"
                prefix={<ScanOutlined style={{ color: '#94A3B8' }} />}
                style={{
                  flex: 1,
                  borderColor: '#93C5FD',
                  boxShadow: '0 0 0 3px rgba(59,130,246,0.12)',
                }}
              />
              <Button type="primary" size="large" onClick={() => void addCopy()}>
                Thêm
              </Button>
            </div>

            <div style={{ marginBottom: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                <Typography.Text style={{ fontSize: 13 }}>
                  {lines.length}/{DEFAULT_MAX} cuốn tối đa
                </Typography.Text>
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  {fillPct}%
                </Typography.Text>
              </div>
              <Progress
                percent={fillPct}
                showInfo={false}
                strokeColor={fillPct >= 100 ? '#EF4444' : '#2563EB'}
                trailColor="#E5E7EB"
                strokeWidth={8}
              />
            </div>

            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                gap: 8,
                marginBottom: 8,
                flexWrap: 'wrap',
                alignItems: 'center',
              }}
            >
              <Typography.Text strong>Bản sao sẵn sàng ({filteredPool.length})</Typography.Text>
              <Space wrap>
                <Input
                  allowClear
                  style={{ width: 240 }}
                  prefix={<SearchOutlined />}
                  placeholder="Lọc tên sách / mã vạch…"
                  value={poolQ}
                  onChange={(e) => setPoolQ(e.target.value)}
                  onPressEnter={() => void loadPool(poolQ)}
                />
                <Button onClick={() => void loadPool(poolQ)}>Tìm</Button>
                <Button icon={<ReloadOutlined />} onClick={() => void loadPool('')}>
                  Tải lại
                </Button>
              </Space>
            </div>
            <Typography.Paragraph type="secondary" style={{ marginTop: 0, fontSize: 12 }}>
              Hiện danh sách bản sao sẵn sàng. Bấm <strong>Thêm</strong> hoặc quét mã để đưa vào phiếu.
            </Typography.Paragraph>
            <Table
              size="small"
              rowKey="id"
              loading={poolLoading}
              dataSource={filteredPool}
              pagination={{ pageSize: 6, showSizeChanger: false }}
              style={{ marginBottom: 16 }}
              rowClassName={(r) =>
                highlightBarcode && r.barcode === highlightBarcode
                  ? 'ant-table-row-selected'
                  : lines.some((l) => l.barcode === r.barcode)
                    ? 'ant-table-row-selected'
                    : ''
              }
              locale={{ emptyText: 'Không có bản sao sẵn sàng' }}
              columns={[
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
                  title: 'Kệ',
                  dataIndex: 'locationCode',
                  width: 90,
                  render: (v?: string) => v || '—',
                },
                {
                  title: '',
                  width: 90,
                  render: (_: unknown, r: CopyLookup) => {
                    const added = lines.some((l) => l.barcode === r.barcode);
                    return (
                      <Button
                        size="small"
                        type={added ? 'default' : 'primary'}
                        disabled={added}
                        icon={<PlusOutlined />}
                        onClick={() => addCopyFromPool(r)}
                      >
                        {added ? 'Đã thêm' : 'Thêm'}
                      </Button>
                    );
                  },
                },
              ]}
            />

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
              Ấn phẩm trong phiếu hiện tại ({lines.length})
            </Typography.Text>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: '40px 1fr 160px 110px 40px',
                gap: 8,
                padding: '6px 8px',
                color: '#94A3B8',
                fontSize: 11,
                fontWeight: 600,
                textTransform: 'uppercase',
                borderBottom: `1px solid ${tokens.color.border}`,
              }}
            >
              <span>#</span>
              <span>Ấn phẩm & tác giả</span>
              <span>Mã vạch / vị trí kệ</span>
              <span>Tình trạng</span>
              <span />
            </div>

            {lines.length === 0 ? (
              <div
                style={{
                  padding: '36px 12px',
                  textAlign: 'center',
                  color: '#94A3B8',
                  background: '#F8FAFC',
                  borderRadius: 8,
                  marginTop: 8,
                }}
              >
                Chưa có ấn phẩm — hãy quét mã vạch bản sao
              </div>
            ) : (
              lines.map((r, i) => (
                <div
                  key={r.key}
                  style={{
                    display: 'grid',
                    gridTemplateColumns: '40px 1fr 160px 110px 40px',
                    gap: 8,
                    alignItems: 'center',
                    padding: '12px 8px',
                    borderBottom: `1px solid ${tokens.color.border}`,
                  }}
                >
                  <Typography.Text type="secondary">{i + 1}</Typography.Text>
                  <Space>
                    <div
                      style={{
                        width: 36,
                        height: 48,
                        borderRadius: 4,
                        background: 'linear-gradient(160deg,#1E3A8A,#60A5FA)',
                        color: '#fff',
                        fontSize: 10,
                        fontWeight: 700,
                        display: 'grid',
                        placeItems: 'center',
                        flexShrink: 0,
                      }}
                    >
                      SÁCH
                    </div>
                    <div style={{ minWidth: 0 }}>
                      <Typography.Text strong style={{ display: 'block' }} ellipsis>
                        {r.bookTitle || 'Không rõ tên sách'}
                      </Typography.Text>
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        Bản sao · {r.barcode}
                      </Typography.Text>
                    </div>
                  </Space>
                  <div>
                    <Typography.Text style={{ fontFamily: 'ui-monospace, monospace', fontSize: 12 }}>
                      {r.barcode}
                    </Typography.Text>
                    <div style={{ color: '#64748B', fontSize: 12 }}>
                      {r.locationCode || 'Chưa gán kệ'}
                    </div>
                  </div>
                  <Tag color="success" style={{ margin: 0, width: 'fit-content' }}>
                    {statusVi(r.condition || 'NORMAL')}
                  </Tag>
                  <Button
                    type="text"
                    danger
                    icon={<DeleteOutlined />}
                    onClick={() => setLines((prev) => prev.filter((l) => l.key !== r.key))}
                  />
                </div>
              ))
            )}
          </div>

          <div style={{ ...panel, padding: 16 }}>
            <Typography.Text strong style={{ display: 'block', marginBottom: 10 }}>
              Ghi chú phiếu mượn
            </Typography.Text>
            <Form form={form} layout="vertical">
              <Form.Item name="note" style={{ marginBottom: 12 }}>
                <Input.TextArea
                  rows={2}
                  placeholder="Ví dụ: độc giả yêu cầu gia hạn nếu chưa ai đặt trước…"
                />
              </Form.Item>
              <Form.Item name="deposit" label="Ký quỹ / đặt cọc" style={{ marginBottom: 0 }}>
                <InputNumber style={{ width: '100%' }} min={0} placeholder="0" addonAfter="đ" />
              </Form.Item>
            </Form>
          </div>
        </Col>

        {/* Cột phải */}
        <Col xs={24} xl={9}>
          <div style={{ ...panel, padding: 16, marginBottom: 14 }}>
            <Space style={{ marginBottom: 12 }}>
              <UserOutlined style={{ color: tokens.color.primary }} />
              <Typography.Text strong>Nhận diện độc giả</Typography.Text>
            </Space>
            <Form form={form} layout="vertical">
              <Form.Item name="card" style={{ marginBottom: 8 }}>
                <Input
                  size="large"
                  placeholder="Quét thẻ / nhập mã thẻ độc giả"
                  onPressEnter={() => void lookupReader()}
                  prefix={<SearchOutlined />}
                />
              </Form.Item>
              <Button type="primary" block onClick={() => void lookupReader()}>
                Tra cứu độc giả
              </Button>
            </Form>

            {reader ? (
              <div
                style={{
                  marginTop: 14,
                  padding: 12,
                  borderRadius: 10,
                  background: 'linear-gradient(180deg,#F8FBFF 0%,#F1F5F9 100%)',
                  border: '1px solid #E2E8F0',
                }}
              >
                <Space align="start" size={12}>
                  <div
                    style={{
                      width: 56,
                      height: 56,
                      borderRadius: 10,
                      background: 'linear-gradient(135deg,#1D4ED8,#38BDF8)',
                      color: '#fff',
                      display: 'grid',
                      placeItems: 'center',
                      fontWeight: 800,
                      fontSize: 20,
                    }}
                  >
                    {reader.fullName.slice(0, 1).toUpperCase()}
                  </div>
                  <div style={{ minWidth: 0 }}>
                    <Typography.Text strong style={{ fontSize: 15 }}>
                      {reader.fullName}
                    </Typography.Text>
                    <div style={{ marginTop: 4 }}>
                      <Tag color="blue">
                        {readerTypeVi(reader.readerTypeCode || reader.readerTypeName)}
                      </Tag>
                      <Tag color={reader.cardStatus === 'ACTIVE' ? 'success' : 'error'}>
                        {statusVi(reader.cardStatus)}
                      </Tag>
                    </div>
                    <Typography.Paragraph
                      type="secondary"
                      style={{ marginBottom: 0, marginTop: 6, fontSize: 12 }}
                    >
                      Mã ĐG: {reader.readerCode}
                      {reader.studentCode ? ` · MSSV: ${reader.studentCode}` : ''}
                      <br />
                      Mã thẻ: {reader.cardNumber || reader.cardBarcode || '—'}
                    </Typography.Paragraph>
                  </div>
                </Space>
              </div>
            ) : (
              <Alert
                style={{ marginTop: 12 }}
                type="info"
                showIcon
                message="Chưa nhận diện độc giả"
                description="Quét thẻ trước, sau đó quét từng bản sao sách."
              />
            )}
          </div>

          <div style={{ ...panel, padding: 16, marginBottom: 14 }}>
            <Typography.Text strong style={{ display: 'block', marginBottom: 12 }}>
              Kiểm định nghiệp vụ
            </Typography.Text>
            <Space direction="vertical" style={{ width: '100%' }} size={10}>
              {checks.map((c) => (
                <div
                  key={c.label}
                  style={{
                    display: 'flex',
                    gap: 8,
                    alignItems: 'flex-start',
                    opacity: reader ? 1 : 0.45,
                  }}
                >
                  {c.ok ? (
                    <CheckCircleFilled style={{ color: '#16A34A', marginTop: 2 }} />
                  ) : (
                    <CloseCircleFilled style={{ color: '#EF4444', marginTop: 2 }} />
                  )}
                  <Typography.Text style={{ color: c.ok ? tokens.color.text : '#B91C1C' }}>
                    {c.label}
                  </Typography.Text>
                </div>
              ))}
              {eligibility?.violations?.map((v) => (
                <Alert key={v.code} type="error" showIcon message={v.message} />
              ))}
            </Space>
          </div>

          <div
            style={{
              ...panel,
              padding: 16,
              background: 'linear-gradient(180deg,#FFFFFF 0%,#F8FAFF 100%)',
            }}
          >
            <Typography.Text strong style={{ display: 'block', marginBottom: 12 }}>
              Tóm tắt phiếu mượn
            </Typography.Text>
            <Space direction="vertical" style={{ width: '100%' }} size={8}>
              <Row justify="space-between">
                <Typography.Text type="secondary">Số ấn phẩm</Typography.Text>
                <Typography.Text strong>{lines.length}</Typography.Text>
              </Row>
              <Row justify="space-between">
                <Typography.Text type="secondary">Hạn trả dự kiến</Typography.Text>
                <Typography.Text strong>{lines.length ? dueDate : '—'}</Typography.Text>
              </Row>
              <Row justify="space-between">
                <Typography.Text type="secondary">Thời hạn mượn</Typography.Text>
                <Typography.Text strong>{LOAN_DAYS} ngày</Typography.Text>
              </Row>

              <Button
                type="primary"
                size="large"
                block
                loading={submitting}
                disabled={!canSubmit}
                onClick={() => void submit()}
                style={{
                  marginTop: 10,
                  height: 48,
                  fontWeight: 700,
                  letterSpacing: 0.3,
                  background: canSubmit ? '#1D4ED8' : undefined,
                }}
              >
                Xác nhận cho mượn
              </Button>

              <Space.Compact style={{ width: '100%' }}>
                <Button style={{ width: '50%' }} icon={<PrinterOutlined />} disabled={!canSubmit}>
                  In phiếu A5
                </Button>
                <Button style={{ width: '50%' }} onClick={() => message.info('Đã lưu nháp cục bộ')}>
                  Lưu nháp
                </Button>
              </Space.Compact>

              <Button block type="text" danger onClick={clearSession}>
                Hủy bỏ phiên làm việc này (Esc)
              </Button>
            </Space>
          </div>
        </Col>
      </Row>
    </div>
  );
}
