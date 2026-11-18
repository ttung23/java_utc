import {
  Alert,
  Button,
  Checkbox,
  Form,
  Input,
  Modal,
  Space,
  Tag,
  Typography,
  message,
} from 'antd';
import {
  BarcodeOutlined,
  CheckCircleFilled,
  LockOutlined,
  SafetyCertificateOutlined,
  SafetyOutlined,
} from '@ant-design/icons';
import { useState, type CSSProperties } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { AxiosError } from 'axios';
import { useAuth } from '@/auth/AuthContext';
import { api, type ApiResponse } from '@/services/api';

type LoginForm = { username: string; password: string; remember?: boolean };

const LIB_IMG =
  'https://images.unsplash.com/photo-1521587760476-6c12a4b040da?auto=format&fit=crop&w=1200&q=80';

const shell: CSSProperties = {
  minHeight: '100vh',
  display: 'grid',
  gridTemplateColumns: 'minmax(320px, 42%) minmax(0, 1fr)',
  background: '#fff',
};

const leftPane: CSSProperties = {
  background: 'linear-gradient(165deg, #0B1F33 0%, #132A40 48%, #1A334D 100%)',
  color: '#fff',
  padding: '28px 32px 24px',
  display: 'flex',
  flexDirection: 'column',
  gap: 20,
  position: 'relative',
  overflow: 'hidden',
};

function BrandMark() {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <div
        style={{
          width: 40,
          height: 40,
          borderRadius: 10,
          background: 'rgba(255,255,255,0.12)',
          display: 'grid',
          placeItems: 'center',
          border: '1px solid rgba(255,255,255,0.2)',
        }}
      >
        <SafetyOutlined style={{ fontSize: 20 }} />
      </div>
      <div>
        <Typography.Text strong style={{ color: '#fff', fontSize: 16, display: 'block', lineHeight: 1.2 }}>
          ĐHQG LIB
        </Typography.Text>
        <Typography.Text style={{ color: 'rgba(255,255,255,0.65)', fontSize: 11, letterSpacing: 0.4 }}>
          CỔNG THÔNG TIN & THƯ VIỆN SỐ ĐHQG
        </Typography.Text>
      </div>
    </div>
  );
}

function LeftPanel() {
  return (
    <aside style={leftPane} className="auth-left">
      <BrandMark />
      <div>
        <span
          style={{
            display: 'inline-block',
            padding: '6px 12px',
            borderRadius: 999,
            background: 'rgba(255,255,255,0.08)',
            border: '1px solid rgba(255,255,255,0.12)',
            fontSize: 11,
            letterSpacing: 0.6,
            color: 'rgba(255,255,255,0.85)',
            marginBottom: 14,
          }}
        >
          HỆ THỐNG QUẢN TRỊ TRI THỨC HỌC THUẬT
        </span>
        <Typography.Title
          level={2}
          style={{ color: '#fff', margin: 0, fontWeight: 700, lineHeight: 1.25, fontSize: 28 }}
        >
          Cổng Tri Thức &amp; Nghiên Cứu ĐHQG
        </Typography.Title>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
        {[
          { label: 'Đầu sách số & in', value: '14,800+', hint: '~100% phân mục DDC' },
          { label: 'Bản sao khả dụng', value: '48,250', hint: 'Tích hợp mã RFID/Barcode' },
        ].map((c) => (
          <div
            key={c.label}
            style={{
              background: 'rgba(255,255,255,0.06)',
              border: '1px solid rgba(255,255,255,0.1)',
              borderRadius: 12,
              padding: '14px 14px 12px',
            }}
          >
            <Typography.Text style={{ color: 'rgba(255,255,255,0.55)', fontSize: 11 }}>
              {c.label}
            </Typography.Text>
            <Typography.Title level={3} style={{ color: '#fff', margin: '4px 0' }}>
              {c.value}
            </Typography.Title>
            <Typography.Text style={{ color: 'rgba(255,255,255,0.5)', fontSize: 11 }}>
              {c.hint}
            </Typography.Text>
          </div>
        ))}
      </div>

      <div
        style={{
          flex: 1,
          minHeight: 160,
          borderRadius: 14,
          overflow: 'hidden',
          border: '1px solid rgba(255,255,255,0.12)',
          position: 'relative',
        }}
      >
        <img
          src={LIB_IMG}
          alt="Thư viện"
          style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block', minHeight: 180 }}
        />
        <div
          style={{
            position: 'absolute',
            left: 0,
            right: 0,
            bottom: 0,
            padding: '10px 14px',
            background: 'linear-gradient(transparent, rgba(0,0,0,0.72))',
            fontSize: 12,
            color: 'rgba(255,255,255,0.9)',
          }}
        >
          Trung tâm Dữ liệu Học thuật &amp; Luận án Tiến sĩ ĐHQG
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
        {[
          { icon: <CheckCircleFilled />, title: 'VNU-SSO', sub: 'Đăng nhập tập trung' },
          { icon: <SafetyCertificateOutlined />, title: 'ISO/IEC 27001', sub: 'An toàn dữ liệu' },
          { icon: <LockOutlined />, title: 'MFA / 2FA', sub: 'Xác thực 2 lớp' },
        ].map((b) => (
          <div
            key={b.title}
            style={{
              background: 'rgba(255,255,255,0.05)',
              border: '1px solid rgba(255,255,255,0.1)',
              borderRadius: 10,
              padding: '10px 8px',
              textAlign: 'center',
            }}
          >
            <div style={{ color: '#93C5FD', marginBottom: 4, fontSize: 16 }}>{b.icon}</div>
            <div style={{ fontSize: 11, fontWeight: 600 }}>{b.title}</div>
            <div style={{ fontSize: 10, color: 'rgba(255,255,255,0.5)' }}>{b.sub}</div>
          </div>
        ))}
      </div>
    </aside>
  );
}

function TabSwitch({
  active,
  onChange,
}: {
  active: 'login' | 'register';
  onChange: (t: 'login' | 'register') => void;
}) {
  const btn = (key: 'login' | 'register'): CSSProperties => ({
    flex: 1,
    height: 44,
    borderRadius: 10,
    border: active === key ? '1px solid #BFDBFE' : '1px solid #E2E8F0',
    background: active === key ? '#fff' : '#F1F5F9',
    fontWeight: 600,
    cursor: 'pointer',
    boxShadow: active === key ? '0 1px 2px rgba(15,23,42,0.06)' : 'none',
    color: '#0F172A',
  });
  return (
    <div style={{ display: 'flex', gap: 10, marginBottom: 22 }}>
      <button type="button" style={btn('login')} onClick={() => onChange('login')}>
        Đăng Nhập
      </button>
      <button type="button" style={btn('register')} onClick={() => onChange('register')}>
        Đăng Ký Thẻ RFID <Tag color="blue" style={{ marginLeft: 6 }}>FR-1.1</Tag>
      </button>
    </div>
  );
}

export function LoginPage() {
  const { user, loading, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [tab, setTab] = useState<'login' | 'register'>('login');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [forgotOpen, setForgotOpen] = useState(false);
  const [forgotEmail, setForgotEmail] = useState('');
  const [forgotLoading, setForgotLoading] = useState(false);

  if (!loading && user) {
    return <Navigate to="/" replace />;
  }

  const onLogin = async (values: LoginForm) => {
    setSubmitting(true);
    setError(null);
    try {
      await login(values.username.trim(), values.password);
      if (values.remember) {
        localStorage.setItem('lib.remember', '1');
      } else {
        localStorage.removeItem('lib.remember');
      }
      const from = (location.state as { from?: string } | null)?.from || '/';
      navigate(from, { replace: true });
    } catch (err) {
      const ax = err as AxiosError<ApiResponse<unknown>>;
      setError(ax.response?.data?.message || (err as Error).message || 'Đăng nhập thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  const onForgot = async () => {
    if (!forgotEmail.trim()) {
      message.warning('Nhập email nhận mã đặt lại');
      return;
    }
    setForgotLoading(true);
    try {
      await api.post('/auth/forgot-password', { email: forgotEmail.trim() });
      message.success('Nếu email tồn tại, mã đặt lại đã được gửi (hiệu lực ~30 phút)');
      setForgotOpen(false);
    } catch {
      message.error('Không gửi được yêu cầu đặt lại mật khẩu');
    } finally {
      setForgotLoading(false);
    }
  };

  return (
    <div className="auth-shell" style={shell}>
      <style>{`
        @media (max-width: 900px) {
          .auth-shell { grid-template-columns: 1fr !important; }
          .auth-left { display: none !important; }
        }
      `}</style>
      <LeftPanel />

      <main
        style={{
          display: 'flex',
          flexDirection: 'column',
          padding: '28px 40px 20px',
          minHeight: '100vh',
          background: '#fff',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
          <div style={{ flex: 1, maxWidth: 560 }}>
            <TabSwitch active={tab} onChange={setTab} />
          </div>
          <Typography.Text type="secondary" style={{ fontSize: 11, letterSpacing: 0.5 }}>
            SYS: M1-ONLINE
          </Typography.Text>
        </div>

        <div style={{ flex: 1, maxWidth: 560, width: '100%', margin: '0 auto' }}>
          {tab === 'login' ? (
            <>
              <Typography.Title level={3} style={{ marginTop: 4, marginBottom: 18, fontWeight: 700 }}>
                Cổng Xác Thực Độc Giả &amp; Thủ Thư
              </Typography.Title>

              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: 12,
                  padding: '12px 14px',
                  borderRadius: 12,
                  background: '#EFF6FF',
                  border: '1px solid #BFDBFE',
                  marginBottom: 18,
                }}
              >
                <Typography.Text style={{ color: '#1E3A8A' }}>
                  Cán bộ &amp; Sinh viên ĐHQG?
                </Typography.Text>
                <Button
                  type="primary"
                  icon={<CheckCircleFilled />}
                  onClick={() =>
                    message.info('VNU-SSO đang mô phỏng — dùng tài khoản thư viện bên dưới')
                  }
                  style={{ background: '#2563EB', borderColor: '#2563EB', fontWeight: 600 }}
                >
                  Đăng nhập VNU-SSO
                </Button>
              </div>

              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 12,
                  marginBottom: 16,
                  color: '#94A3B8',
                  fontSize: 11,
                  letterSpacing: 0.6,
                }}
              >
                <div style={{ flex: 1, height: 1, background: '#E2E8F0' }} />
                HOẶC SỬ DỤNG TÀI KHOẢN THƯ VIỆN
                <div style={{ flex: 1, height: 1, background: '#E2E8F0' }} />
              </div>

              {error && (
                <Alert type="error" showIcon style={{ marginBottom: 14 }} message={error} />
              )}

              <Form
                layout="vertical"
                onFinish={onLogin}
                requiredMark={false}
                initialValues={{ username: 'admin@lib.vn', remember: true }}
              >
                <Form.Item
                  name="username"
                  label="Mã Thẻ Thư Viện / MSSV / VNU Email"
                  rules={[{ required: true, message: 'Nhập mã thẻ, MSSV hoặc email' }]}
                >
                  <Input
                    size="large"
                    placeholder="VD: SV20210892 hoặc thuvien@vnu.edu.vn"
                    suffix={<BarcodeOutlined style={{ color: '#94A3B8' }} />}
                  />
                </Form.Item>
                <Form.Item
                  label={
                    <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
                      <span>Mật Khẩu Truy Cập</span>
                      <Typography.Link
                        style={{ fontSize: 12 }}
                        onClick={(e) => {
                          e.preventDefault();
                          setForgotOpen(true);
                        }}
                      >
                        Quên mật khẩu? (Reset 30p)
                      </Typography.Link>
                    </div>
                  }
                  name="password"
                  rules={[{ required: true, message: 'Nhập mật khẩu' }]}
                  style={{ marginBottom: 12 }}
                >
                  <Input.Password size="large" placeholder="••••••••" />
                </Form.Item>

                <Form.Item name="remember" valuePropName="checked" style={{ marginBottom: 10 }}>
                  <Checkbox>
                    Ghi nhớ đăng nhập trên máy này{' '}
                    <Tag style={{ marginLeft: 6 }}>Token 7 ngày (FR-1.4)</Tag>
                  </Checkbox>
                </Form.Item>

                <Alert
                  type="error"
                  showIcon
                  style={{
                    marginBottom: 18,
                    background: '#FEF2F2',
                    borderColor: '#FECACA',
                  }}
                  message={
                    <span>
                      Tự động khóa <strong>15 phút</strong> nếu nhập sai quá <strong>5 lần</strong>
                    </span>
                  }
                />

                <Button
                  type="primary"
                  htmlType="submit"
                  size="large"
                  block
                  loading={submitting}
                  style={{
                    height: 48,
                    fontWeight: 700,
                    background: '#0F172A',
                    borderColor: '#0F172A',
                  }}
                >
                  Đăng Nhập Vào Hệ Thống →
                </Button>
              </Form>

              <Typography.Paragraph type="secondary" style={{ marginTop: 14, fontSize: 12 }}>
                Demo quản trị: admin@lib.vn / Admin@123 · staff1@lib.vn / Staff@123
              </Typography.Paragraph>
            </>
          ) : (
            <RegisterStaffNotice onSwitchLogin={() => setTab('login')} />
          )}
        </div>

        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            gap: 12,
            flexWrap: 'wrap',
            paddingTop: 16,
            borderTop: '1px solid #F1F5F9',
            maxWidth: 560,
            width: '100%',
            margin: '24px auto 0',
          }}
        >
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            Hotline hỗ trợ: (028) 3835 4409
          </Typography.Text>
          <Space size="middle">
            <Typography.Link style={{ fontSize: 12 }}>Quy định thư viện</Typography.Link>
            <Typography.Link style={{ fontSize: 12 }}>Bảo mật</Typography.Link>
          </Space>
        </div>
      </main>

      <Modal
        title="Quên mật khẩu"
        open={forgotOpen}
        onCancel={() => setForgotOpen(false)}
        onOk={() => void onForgot()}
        confirmLoading={forgotLoading}
        okText="Gửi mã reset"
        cancelText="Đóng"
      >
        <Typography.Paragraph type="secondary">
          Nhập email đã đăng ký. Mã đặt lại có hiệu lực khoảng 30 phút.
        </Typography.Paragraph>
        <Input
          placeholder="email@vnu.edu.vn"
          value={forgotEmail}
          onChange={(e) => setForgotEmail(e.target.value)}
          size="large"
        />
      </Modal>
    </div>
  );
}

function RegisterStaffNotice({ onSwitchLogin }: { onSwitchLogin: () => void }) {
  return (
    <div>
      <Tag color="blue" style={{ marginBottom: 12 }}>
        HỒ SƠ CẤP THẺ THƯ VIỆN RFID SỐ (BR-31)
      </Tag>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Đăng Ký Cấp Mới Thẻ Độc Giả
      </Typography.Title>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="Cổng quản trị dành cho thủ thư / admin đã có tài khoản."
        description="Hồ sơ cấp thẻ RFID độc giả mới được gửi từ cổng độc giả (client). Sau khi gửi, trạng thái PENDING chờ thủ thư duyệt."
      />
      <Space>
        <Button type="primary" onClick={onSwitchLogin} style={{ background: '#0F172A', borderColor: '#0F172A' }}>
          Quay lại đăng nhập
        </Button>
        <Button
          onClick={() =>
            message.info('Mở cổng độc giả (thường :5174) → Đăng ký thẻ RFID để gửi hồ sơ PENDING')
          }
        >
          Hướng dẫn cổng độc giả
        </Button>
      </Space>
    </div>
  );
}
