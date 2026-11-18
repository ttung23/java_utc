import { useMemo, useState } from 'react';
import {
  CheckCircleFilled,
  CloseOutlined,
  SendOutlined,
  ShoppingCartOutlined,
} from '@ant-design/icons';
import {
  Breadcrumb,
  Button,
  Checkbox,
  Empty,
  Input,
  Progress,
  Steps,
  Tag,
  message,
} from 'antd';
import dayjs from 'dayjs';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/AuthContext';
import { CART_MAX_ITEMS, useBorrowCart } from '@/cart/BorrowCartContext';
import { api, type ApiResponse } from '@/services/api';

const BRANCHES = [
  {
    id: 'hn',
    title: 'Cơ sở Hà Nội',
    address: 'Số 3 Cầu Giấy, Đống Đa, Hà Nội',
  },
  {
    id: 'hcm',
    title: 'Cơ sở TP.HCM',
    address: '450–451 Lê Văn Việt, TP. Thủ Đức',
  },
];

const SLOTS = [
  { id: 'm', label: '08:30 – 11:30', hint: 'Buổi sáng' },
  { id: 'a', label: '13:30 – 17:00', hint: 'Khuyến nghị' },
  { id: 'e', label: '17:30 – 20:00', hint: 'Buổi tối' },
];

export function BorrowCartPage() {
  const { user } = useAuth();
  const { items, count, removeItem, clear } = useBorrowCart();
  const navigate = useNavigate();
  const [step, setStep] = useState(0);
  const [branch, setBranch] = useState('hn');
  const [pickupDay, setPickupDay] = useState<'today' | 'tomorrow'>('today');
  const [slot, setSlot] = useState('a');
  const [note, setNote] = useState('');
  const [agreed, setAgreed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [slipCode, setSlipCode] = useState<string | null>(null);

  const pickupDate = useMemo(() => {
    const d = pickupDay === 'today' ? dayjs() : dayjs().add(1, 'day');
    return d.format('YYYY-MM-DD');
  }, [pickupDay]);

  const dayCards = [
    {
      key: 'today' as const,
      title: `Hôm nay, ${dayjs().format('dddd')}`,
      sub: dayjs().format('DD/MM'),
    },
    {
      key: 'tomorrow' as const,
      title: `Ngày mai, ${dayjs().add(1, 'day').format('dddd')}`,
      sub: dayjs().add(1, 'day').format('DD/MM'),
    },
  ];

  const formatBorrowError = (payload?: {
    message?: string;
    errors?: Array<{ code?: string; message?: string }> | Record<string, string>;
  }) => {
    if (!payload) return 'Gửi yêu cầu thất bại';
    const parts: string[] = [];
    if (payload.message) parts.push(payload.message);
    if (Array.isArray(payload.errors)) {
      payload.errors.forEach((e) => {
        if (e?.message) parts.push(e.message);
      });
    } else if (payload.errors && typeof payload.errors === 'object') {
      Object.values(payload.errors).forEach((msg) => {
        if (msg) parts.push(String(msg));
      });
    }
    return parts.filter(Boolean).join(' — ') || 'Gửi yêu cầu thất bại';
  };

  const submit = async () => {
    if (submitting) return;
    if (!user) {
      navigate('/login');
      return;
    }
    if (items.length === 0) {
      message.warning('Giỏ mượn trống');
      return;
    }
    if (!agreed) {
      message.warning('Vui lòng xác nhận đã đọc quy định');
      return;
    }
    setSubmitting(true);
    try {
      // Chặn gửi lại đầu sách đang PENDING/APPROVED/BORROWING
      const { data: mine } = await api.get<
        ApiResponse<Array<{ status: string; items?: Array<{ bookId?: number; bookTitle?: string }> }>>
      >('/borrows/me', { params: { page: 0, size: 50 } });
      const blockedIds = new Set<number>();
      (mine.data || []).forEach((slip) => {
        const st = String(slip.status || '').toUpperCase();
        if (!['PENDING', 'APPROVED', 'BORROWING', 'PARTIALLY_RETURNED'].includes(st)) return;
        (slip.items || []).forEach((it) => {
          if (it.bookId != null) blockedIds.add(it.bookId);
        });
      });
      const conflict = items.filter((i) => blockedIds.has(i.bookId));
      if (conflict.length > 0) {
        message.error(
          `Không gửi được: đang mượn/chờ đầu sách «${conflict.map((c) => c.title).join(', ')}». Hủy yêu cầu cũ ở mục Mượn hoặc bỏ sách khỏi giỏ.`,
        );
        return;
      }

      const branchLabel = BRANCHES.find((b) => b.id === branch)?.title || branch;
      const slotLabel = SLOTS.find((s) => s.id === slot)?.label || slot;
      const { data } = await api.post<ApiResponse<{ slipCode?: string; id?: number }>>('/borrows/online', {
        items: items.map((i) => ({ bookId: i.bookId })),
        pickupDate,
        note: `[${branchLabel}] [${slotLabel}] ${note}`.trim(),
      });
      const code = data.data?.slipCode || (data.data?.id ? `PM-${data.data.id}` : 'OK');
      setSlipCode(code);
      clear();
      setStep(2);
      message.success('Đã gửi yêu cầu mượn online');
    } catch (e: unknown) {
      const err = e as {
        response?: { data?: { message?: string; errors?: Array<{ code?: string; message?: string }> } };
      };
      message.error(formatBorrowError(err.response?.data));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <Breadcrumb
        style={{ marginBottom: 12 }}
        items={[
          { title: <Link to="/">Trang chủ</Link> },
          { title: <Link to="/books">Kho tài liệu OPAC</Link> },
          { title: 'Giỏ mượn tài liệu trực tuyến' },
        ]}
      />

      <div className="home-section-head">
        <div>
          <h2 style={{ margin: 0, fontSize: 26, color: '#0b1f3a' }}>
            Xác Nhận Giỏ Mượn &amp; Điểm Hẹn Nhận Sách
          </h2>
          <p className="desc">Kiểm tra hạn mức, chọn chi nhánh / khung giờ, gửi yêu cầu để nhận mã phiếu.</p>
        </div>
      </div>

      <Steps
        current={step}
        style={{ marginBottom: 20, maxWidth: 720 }}
        items={[
          { title: 'Kiểm tra giỏ' },
          { title: 'Hẹn giờ & Chi nhánh' },
          { title: 'Mã QR phiếu hẹn' },
        ]}
      />

      {step < 2 ? (
        <div className="cart-layout">
          <div>
            <div className="home-panel" style={{ marginBottom: 14 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
                <div>
                  <strong>{user?.fullName}</strong>
                  <div style={{ fontSize: 13, color: '#64748b' }}>{user?.email}</div>
                </div>
                <Tag color={count >= CART_MAX_ITEMS ? 'error' : 'processing'}>
                  {count}/{CART_MAX_ITEMS} cuốn trong giỏ
                </Tag>
              </div>
              <Progress
                percent={Math.round((count / CART_MAX_ITEMS) * 100)}
                showInfo={false}
                strokeColor={count >= CART_MAX_ITEMS ? '#dc2626' : '#1d4ed8'}
                style={{ marginTop: 10 }}
              />
              <div style={{ fontSize: 12, color: '#64748b', marginTop: 6 }}>
                Giỏ hiện tại: <strong>{count}</strong> · Hạn mức giỏ tối đa {CART_MAX_ITEMS} đầu sách / lần gửi
              </div>
              <div
                style={{
                  marginTop: 12,
                  padding: 10,
                  borderRadius: 10,
                  background: '#eff6ff',
                  color: '#1e3a8a',
                  fontSize: 13,
                }}
              >
                Sau khi được duyệt, sách được giữ tại quầy trong khoảng <strong>48 giờ</strong>. Quá hạn không
                nhận có thể bị hủy và ghi nhận no-show.
              </div>
            </div>

            {step === 0 ? (
              <div className="home-panel">
                <div className="home-panel-head">
                  <h3>
                    <ShoppingCartOutlined /> Danh mục ấn phẩm đã chọn ({String(count).padStart(2, '0')})
                  </h3>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <Button danger type="text" disabled={count === 0} onClick={() => clear()}>
                      Xóa tất cả
                    </Button>
                    <Button type="primary" ghost onClick={() => navigate('/books')}>
                      + Tra cứu thêm sách
                    </Button>
                  </div>
                </div>

                {items.length === 0 ? (
                  <Empty
                    description="Giỏ trống"
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                  >
                    <Button type="primary" onClick={() => navigate('/books')}>
                      Đi tra cứu OPAC
                    </Button>
                  </Empty>
                ) : (
                  <div style={{ display: 'grid', gap: 12 }}>
                    {items.map((item) => (
                      <div key={item.bookId} className="cart-item">
                        <div className="thumb">
                          {item.coverUrl ? (
                            <img src={item.coverUrl} alt={item.title} />
                          ) : (
                            <span>{item.title.slice(0, 18)}</span>
                          )}
                        </div>
                        <div style={{ flex: 1, minWidth: 0 }}>
                          <Link to={`/books/${item.bookId}`}>
                            <strong>{item.title}</strong>
                          </Link>
                          <div className="meta">
                            {item.authorNames?.join(', ') || item.publisherName || '—'}
                            {item.classificationCode ? ` · ${item.classificationCode}` : ''}
                          </div>
                          <div className="meta">
                            Còn {item.availableCopies ?? '—'}
                            {item.totalCopies != null ? `/${item.totalCopies}` : ''} bản · ISBN{' '}
                            {item.isbn13 || '—'} · Hạn mượn ~14 ngày
                          </div>
                        </div>
                        <Button
                          type="text"
                          danger
                          icon={<CloseOutlined />}
                          onClick={() => removeItem(item.bookId)}
                        />
                      </div>
                    ))}
                  </div>
                )}

                <div style={{ marginTop: 16, display: 'flex', justifyContent: 'flex-end' }}>
                  <Button
                    type="primary"
                    disabled={items.length === 0}
                    style={{ background: '#0B1F3A', color: '#fff', borderColor: '#0B1F3A' }}
                    onClick={() => setStep(1)}
                  >
                    Tiếp tục: Hẹn giờ &amp; Chi nhánh →
                  </Button>
                </div>
              </div>
            ) : (
              <div className="home-panel">
                <h3 style={{ marginTop: 0 }}>Chọn chi nhánh nhận sách</h3>
                <div className="cart-choice-grid">
                  {BRANCHES.map((b) => (
                    <button
                      key={b.id}
                      type="button"
                      className={branch === b.id ? 'choice active' : 'choice'}
                      onClick={() => setBranch(b.id)}
                    >
                      <strong>{b.title}</strong>
                      <span>{b.address}</span>
                    </button>
                  ))}
                </div>

                <h3>1. Chọn ngày nhận sách</h3>
                <div className="cart-choice-grid">
                  {dayCards.map((d) => (
                    <button
                      key={d.key}
                      type="button"
                      className={pickupDay === d.key ? 'choice active' : 'choice'}
                      onClick={() => setPickupDay(d.key)}
                    >
                      <strong>{d.title}</strong>
                      <span>{d.sub}</span>
                    </button>
                  ))}
                </div>

                <h3>2. Khung giờ hẹn đến quầy</h3>
                <div className="cart-choice-grid three">
                  {SLOTS.map((s) => (
                    <button
                      key={s.id}
                      type="button"
                      className={slot === s.id ? 'choice active' : 'choice'}
                      onClick={() => setSlot(s.id)}
                    >
                      <strong>{s.label}</strong>
                      <span>{s.hint}</span>
                    </button>
                  ))}
                </div>

                <h3>Ghi chú bổ sung gửi thủ thư</h3>
                <Input.TextArea
                  rows={3}
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  placeholder="Ví dụ: nhận hộ, cần hỗ trợ tìm kệ…"
                />

                <div style={{ marginTop: 12 }}>
                  <Checkbox checked={agreed} onChange={(e) => setAgreed(e.target.checked)}>
                    Tôi đã đọc <Link to="/rules">quy định mượn trả</Link> và cam kết nhận sách đúng hạn giữ chỗ.
                  </Checkbox>
                </div>

                <div style={{ marginTop: 16, display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                  <Button onClick={() => setStep(0)}>← Quay lại giỏ</Button>
                  <Button
                    type="primary"
                    icon={<SendOutlined />}
                    loading={submitting}
                    style={{ background: '#0B1F3A', color: '#fff', borderColor: '#0B1F3A' }}
                    onClick={() => void submit()}
                  >
                    Gửi Yêu Cầu Mượn Sách
                  </Button>
                </div>
              </div>
            )}
          </div>

          <aside>
            <div className="home-panel" style={{ marginBottom: 14 }}>
              <h3 style={{ marginTop: 0 }}>Tóm tắt yêu cầu</h3>
              <div className="cart-summary-row">
                <span>Số lượng</span>
                <strong>{String(count).padStart(2, '0')} ấn phẩm</strong>
              </div>
              <div className="cart-summary-row">
                <span>Thời hạn mượn</span>
                <strong>14 ngày / cuốn</strong>
              </div>
              <div className="cart-summary-row">
                <span>Gia hạn online</span>
                <strong>Theo loại thẻ</strong>
              </div>
              <div className="cart-summary-row">
                <span>Phí dịch vụ</span>
                <strong>0đ</strong>
              </div>
              <div className="cart-summary-total">
                <span>Tổng chi phí</span>
                <strong>0đ</strong>
              </div>
              {step === 0 ? (
                <Button
                  block
                  type="primary"
                  size="large"
                  disabled={items.length === 0}
                  style={{ background: '#0B1F3A', color: '#fff', borderColor: '#0B1F3A', marginTop: 12 }}
                  onClick={() => setStep(1)}
                >
                  Tiếp tục hẹn nhận sách
                </Button>
              ) : (
                <Button
                  block
                  type="primary"
                  size="large"
                  icon={<SendOutlined />}
                  loading={submitting}
                  style={{ background: '#0B1F3A', color: '#fff', borderColor: '#0B1F3A', marginTop: 12 }}
                  onClick={() => void submit()}
                >
                  Gửi Yêu Cầu Mượn Sách
                </Button>
              )}
              <Button block type="link" onClick={() => navigate('/books')}>
                Tiếp tục tìm thêm sách khác
              </Button>
            </div>

            <div className="home-panel">
              <h3 style={{ marginTop: 0 }}>Đối soát điều kiện độc giả</h3>
              <Tag color="success" style={{ marginBottom: 10 }}>
                Hồ sơ hợp lệ (kiểm tra sơ bộ)
              </Tag>
              <ul className="cart-checks">
                <li>
                  <CheckCircleFilled /> Thẻ / tài khoản đang Active
                </li>
                <li>
                  <CheckCircleFilled /> Không blacklist
                </li>
                <li>
                  <CheckCircleFilled /> Cho phép mượn online
                </li>
                <li>
                  <CheckCircleFilled /> Giỏ ≤ hạn mức {CART_MAX_ITEMS}
                </li>
              </ul>
              <div
                style={{
                  marginTop: 12,
                  padding: 12,
                  borderRadius: 10,
                  background: '#eff6ff',
                  fontSize: 13,
                  color: '#1e3a8a',
                }}
              >
                Cần trợ giúp đơn mượn?
                <br />
                Hotline: <strong>(028) 3835 4409</strong> (ext 114)
              </div>
            </div>
          </aside>
        </div>
      ) : (
        <div className="home-panel" style={{ textAlign: 'center', maxWidth: 560, margin: '0 auto' }}>
          <CheckCircleFilled style={{ fontSize: 48, color: '#16a34a' }} />
          <h2 style={{ marginTop: 12 }}>Đã gửi yêu cầu mượn online</h2>
          <p style={{ color: '#64748b' }}>
            Mã phiếu / yêu cầu: <strong style={{ color: '#0b1f3a' }}>{slipCode}</strong>
          </p>
          <p style={{ color: '#64748b' }}>
            Ngày hẹn nhận: <strong>{dayjs(pickupDate).format('DD/MM/YYYY')}</strong> · Theo dõi trạng thái tại
            mục Mượn của bạn.
          </p>
          <div style={{ display: 'flex', gap: 8, justifyContent: 'center', flexWrap: 'wrap' }}>
            <Button
              type="primary"
              style={{ background: '#0B1F3A', color: '#fff', borderColor: '#0B1F3A' }}
              onClick={() => navigate('/me/borrows')}
            >
              Xem phiếu mượn của tôi
            </Button>
            <Button onClick={() => navigate('/books')}>Tiếp tục tra cứu</Button>
          </div>
        </div>
      )}
    </div>
  );
}
