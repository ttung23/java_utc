import {
  Alert,
  Button,
  Checkbox,
  Col,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import {
  BarcodeOutlined,
  CheckCircleFilled,
  InboxOutlined,
  InfoCircleOutlined,
  LockOutlined,
  SafetyCertificateOutlined,
  SafetyOutlined,
} from '@ant-design/icons';
import { useEffect, useState, type CSSProperties } from 'react';
import { Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { AxiosError } from 'axios';
import { useAuth } from '@/auth/AuthContext';
import { api, authApi, type ApiResponse } from '@/services/api';

type LoginForm = { username: string; password: string; remember?: boolean };

const LIB_IMG =
  'https://images.unsplash.com/photo-1521587760476-6c12a4b040da?auto=format&fit=crop&w=1200&q=80';

const READER_TYPES = [
  { value: 'STUDENT', label: 'Sinh viên Đại học chính quy' },
  { value: 'PUPIL', label: 'Học sinh' },
  { value: 'TEACHER', label: 'Giảng viên / Cán bộ' },
  { value: 'PUBLIC', label: 'Khách vãng lai / Cộng đồng' },
  { value: 'VIP', label: 'VIP / Thành viên đặc biệt' },
];

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
          UTC LIB
        </Typography.Text>
        <Typography.Text style={{ color: 'rgba(255,255,255,0.65)', fontSize: 11, letterSpacing: 0.4 }}>
          CỔNG THÔNG TIN & THƯ VIỆN SỐ UTC
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
          Cổng Tri Thức &amp; Nghiên Cứu UTC
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
          Trung tâm Dữ liệu Học thuật — Đại học Giao thông vận tải
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
        {[
          { icon: <CheckCircleFilled />, title: 'UTC-SSO', sub: 'Đăng nhập tập trung' },
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
    <div style={{ display: 'flex', gap: 10, marginBottom: 18 }}>
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
  const [params] = useSearchParams();
  const [tab, setTab] = useState<'login' | 'register'>(
    params.get('tab') === 'register' ? 'register' : 'login',
  );
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [regLoading, setRegLoading] = useState(false);
  const [forgotOpen, setForgotOpen] = useState(false);
  const [forgotEmail, setForgotEmail] = useState('');
  const [forgotLoading, setForgotLoading] = useState(false);
  const [photoName, setPhotoName] = useState<string | null>(null);

  useEffect(() => {
    if (params.get('tab') === 'register') setTab('register');
  }, [params]);

  if (!loading && user) return <Navigate to="/" replace />;

  const onLogin = async (values: LoginForm) => {
    setSubmitting(true);
    setError(null);
    try {
      await login(values.username.trim(), values.password);
      if (values.remember) localStorage.setItem('lib.remember', '1');
      else localStorage.removeItem('lib.remember');
      const from = (location.state as { from?: string } | null)?.from || '/';
      navigate(from, { replace: true });
    } catch (err) {
      const ax = err as AxiosError<ApiResponse<unknown>>;
      setError(ax.response?.data?.message || (err as Error).message || 'Đăng nhập thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  const onRegister = async (values: Record<string, string>) => {
    setRegLoading(true);
    setError(null);
    try {
      await authApi.register({
        fullName: values.fullName,
        email: values.email,
        phone: values.phone,
        password: values.password,
        idNumber: values.idNumber,
        studentCode: values.studentCode,
        readerTypeCode: values.readerTypeCode,
      });
      message.success(
        'Đã gửi hồ sơ — tài khoản PENDING chờ duyệt. Mã theo dõi sẽ gửi email (nếu cấu hình mail).',
      );
      setTab('login');
      navigate('/login', { replace: true });
    } catch (err) {
      const ax = err as AxiosError<ApiResponse<unknown>>;
      setError(ax.response?.data?.message || 'Gửi hồ sơ thất bại');
    } finally {
      setRegLoading(false);
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
    <div style={shell}>
      <style>{`
        @media (max-width: 900px) {
          .auth-shell { grid-template-columns: 1fr !important; }
          .auth-left { display: none !important; }
        }
      `}</style>
      <div className="auth-shell" style={{ display: 'contents' }}>
        <LeftPanel />
        <main
          style={{
            display: 'flex',
            flexDirection: 'column',
            padding: '28px 36px 20px',
            minHeight: '100vh',
            background: '#fff',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ flex: 1, maxWidth: 640 }}>
              <TabSwitch
                active={tab}
                onChange={(t) => {
                  setError(null);
                  setTab(t);
                  navigate(t === 'register' ? '/login?tab=register' : '/login', { replace: true });
                }}
              />
            </div>
            <Typography.Text type="secondary" style={{ fontSize: 11, letterSpacing: 0.5 }}>
              SYS: M1-ONLINE
            </Typography.Text>
          </div>

          <div style={{ flex: 1, maxWidth: 640, width: '100%', margin: '0 auto' }}>
            {error ? (
              <Alert type="error" showIcon style={{ marginBottom: 14 }} message={error} />
            ) : null}

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
                    flexWrap: 'wrap',
                  }}
                >
                  <Typography.Text style={{ color: '#1E3A8A' }}>
                    Cán bộ &amp; Sinh viên UTC?
                  </Typography.Text>
                  <Button
                    type="primary"
                    icon={<CheckCircleFilled />}
                    onClick={() =>
                      message.info('UTC-SSO đang mô phỏng — dùng tài khoản thư viện bên dưới')
                    }
                    style={{ background: '#2563EB', borderColor: '#2563EB', fontWeight: 600 }}
                  >
                    Đăng nhập UTC-SSO
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

                <Form
                  layout="vertical"
                  onFinish={onLogin}
                  requiredMark={false}
                  initialValues={{ username: 'reader1@lib.vn', remember: true }}
                >
                  <Form.Item
                    name="username"
                    label="Mã Thẻ Thư Viện / MSSV / Email UTC"
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
                    style={{ marginBottom: 18, background: '#FEF2F2', borderColor: '#FECACA' }}
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
                  Demo độc giả: reader1@lib.vn / User@123
                </Typography.Paragraph>
              </>
            ) : (
              <>
                <Tag color="blue" style={{ marginBottom: 10 }}>
                  HỒ SƠ CẤP THẺ THƯ VIỆN RFID SỐ (BR-31)
                </Tag>
                <Typography.Title level={3} style={{ marginTop: 0, marginBottom: 6 }}>
                  Đăng Ký Cấp Mới Thẻ Độc Giả
                </Typography.Title>
                <Typography.Paragraph type="secondary" style={{ marginBottom: 18 }}>
                  Dành cho sinh viên / cán bộ mới — hồ sơ ở trạng thái{' '}
                  <Tag color="orange">PENDING</Tag> chờ thủ thư duyệt cấp thẻ.
                </Typography.Paragraph>

                <Form
                  layout="vertical"
                  requiredMark={false}
                  onFinish={onRegister}
                  initialValues={{ readerTypeCode: 'STUDENT' }}
                >
                  <Row gutter={12}>
                    <Col xs={24} md={12}>
                      <Form.Item
                        name="fullName"
                        label="Họ và Tên Khai Sinh"
                        rules={[{ required: true, message: 'Nhập họ tên' }]}
                      >
                        <Input size="large" placeholder="Nguyễn Văn An" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item
                        name="idNumber"
                        label="Số Căn Cước (CCCD / ĐDCN)"
                        rules={[{ required: true, message: 'Nhập CCCD' }]}
                      >
                        <Input size="large" placeholder="07920100xxxx" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item
                        name="studentCode"
                        label="Mã Số Sinh Viên (MSSV) / CB"
                        rules={[{ required: true, message: 'Nhập MSSV / mã CB' }]}
                      >
                        <Input size="large" placeholder="211270xx" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item
                        name="readerTypeCode"
                        label="Đối Tượng Độc Giả"
                        rules={[{ required: true }]}
                      >
                        <Select size="large" options={READER_TYPES} />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item
                        name="email"
                        label="Email Nhận Thông Báo"
                        rules={[{ required: true, type: 'email', message: 'Email không hợp lệ' }]}
                      >
                        <Input size="large" placeholder="an.nguyen@edu.vn" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item
                        name="phone"
                        label="Số Điện Thoại Liên Hệ"
                        rules={[{ required: true, message: 'Nhập số điện thoại' }]}
                      >
                        <Input size="large" placeholder="0918xxxxxx" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item
                        name="password"
                        label="Mật Khẩu Đăng Nhập"
                        rules={[
                          { required: true, message: 'Nhập mật khẩu' },
                          {
                            pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/,
                            message: '≥ 8 ký tự, gồm chữ hoa, chữ thường và số',
                          },
                        ]}
                      >
                        <Input.Password size="large" placeholder="VD: LibUser@123" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item label="Ảnh Chân Dung In Thẻ (3×4)">
                        <Upload.Dragger
                          accept="image/jpeg,image/png"
                          maxCount={1}
                          beforeUpload={(file) => {
                            if (file.size > 2 * 1024 * 1024) {
                              message.error('Ảnh tối đa 2MB');
                              return Upload.LIST_IGNORE;
                            }
                            setPhotoName(file.name);
                            message.success(`Đã chọn ${file.name} (lưu cục bộ — BE chưa nhận file)`);
                            return false;
                          }}
                          onRemove={() => setPhotoName(null)}
                          style={{ padding: '4px 0' }}
                        >
                          <p className="ant-upload-drag-icon" style={{ marginBottom: 4 }}>
                            <InboxOutlined />
                          </p>
                          <p style={{ margin: 0, fontSize: 12 }}>
                            JPEG/PNG ≤ 2MB · nền xanh/trắng {photoName ? `· ${photoName}` : ''}
                          </p>
                          <Button size="small" style={{ marginTop: 8 }}>
                            Chọn Tệp
                          </Button>
                        </Upload.Dragger>
                      </Form.Item>
                    </Col>
                  </Row>

                  <Alert
                    type="info"
                    showIcon
                    icon={<InfoCircleOutlined />}
                    style={{ marginBottom: 16 }}
                    message="Sau khi gửi, mã theo dõi hồ sơ sẽ được gửi qua email (khi mail server bật)."
                  />

                  <Button
                    type="primary"
                    htmlType="submit"
                    size="large"
                    block
                    loading={regLoading}
                    style={{
                      height: 48,
                      fontWeight: 700,
                      background: '#0F172A',
                      borderColor: '#0F172A',
                    }}
                  >
                    Gửi Hồ Sơ Xét Duyệt Cấp Thẻ
                  </Button>
                </Form>
              </>
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
              maxWidth: 640,
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
      </div>

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
