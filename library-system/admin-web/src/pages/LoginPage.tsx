import { Alert, Button, Card, Form, Input, Typography } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { AxiosError } from 'axios';
import { useAuth } from '@/auth/AuthContext';
import { tokens } from '@/styles/tokens';
import type { ApiResponse } from '@/services/api';

type LoginForm = { username: string; password: string };

export function LoginPage() {
  const { user, loading, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (!loading && user) {
    return <Navigate to="/" replace />;
  }

  const onFinish = async (values: LoginForm) => {
    setSubmitting(true);
    setError(null);
    try {
      await login(values.username.trim(), values.password);
      const from = (location.state as { from?: string } | null)?.from || '/';
      navigate(from, { replace: true });
    } catch (err) {
      const ax = err as AxiosError<ApiResponse<unknown>>;
      setError(ax.response?.data?.message || (err as Error).message || 'Đăng nhập thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'grid',
        placeItems: 'center',
        background: `linear-gradient(160deg, #0b1f3a 0%, ${tokens.color.primary} 55%, #93c5fd 100%)`,
        padding: 16,
      }}
    >
      <Card style={{ width: 420, maxWidth: '100%', borderRadius: tokens.radius.lg }} bordered={false}>
        <Typography.Title level={3} style={{ marginBottom: 4 }}>
          Đăng nhập quản trị
        </Typography.Title>
        <Typography.Paragraph type="secondary">
          Hệ thống quản lý thư viện — chỉ dành cho STAFF/ADMIN
        </Typography.Paragraph>
        {error && (
          <Alert type="error" showIcon style={{ marginBottom: 16 }} message={error} />
        )}
        <Form layout="vertical" onFinish={onFinish} requiredMark={false} initialValues={{ username: 'admin@lib.vn' }}>
          <Form.Item
            name="username"
            label="Email / SĐT"
            rules={[{ required: true, message: 'Nhập email hoặc số điện thoại' }]}
          >
            <Input size="large" prefix={<UserOutlined />} placeholder="admin@lib.vn" />
          </Form.Item>
          <Form.Item
            name="password"
            label="Mật khẩu"
            rules={[{ required: true, message: 'Nhập mật khẩu' }]}
          >
            <Input.Password size="large" prefix={<LockOutlined />} placeholder="••••••••" />
          </Form.Item>
          <Button type="primary" htmlType="submit" size="large" block loading={submitting}>
            Đăng nhập
          </Button>
        </Form>
        <Typography.Paragraph type="secondary" style={{ marginTop: 16, marginBottom: 0, fontSize: 12 }}>
          Tài khoản demo: admin@lib.vn / Admin@123
        </Typography.Paragraph>
      </Card>
    </div>
  );
}
