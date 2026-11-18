import { Alert, Button, Card, Form, Input, Typography } from 'antd';
import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { AxiosError } from 'axios';
import { useAuth } from '@/auth/AuthContext';
import type { ApiResponse } from '@/services/api';

export function LoginPage() {
  const { user, loading, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (!loading && user) return <Navigate to="/" replace />;

  return (
    <div style={{ minHeight: '70vh', display: 'grid', placeItems: 'center' }}>
      <Card style={{ width: 400, maxWidth: '100%' }}>
        <Typography.Title level={3}>Đăng nhập độc giả</Typography.Title>
        {error ? <Alert type="error" showIcon style={{ marginBottom: 16 }} message={error} /> : null}
        <Form
          layout="vertical"
          onFinish={async (values) => {
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
          }}
          initialValues={{ username: 'reader1@lib.vn' }}
        >
          <Form.Item name="username" label="Email / SĐT" rules={[{ required: true }]}>
            <Input size="large" />
          </Form.Item>
          <Form.Item name="password" label="Mật khẩu" rules={[{ required: true }]}>
            <Input.Password size="large" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block size="large" loading={submitting}>
            Đăng nhập
          </Button>
        </Form>
        <Typography.Paragraph type="secondary" style={{ marginTop: 12, marginBottom: 0 }}>
          Demo: reader1@lib.vn / User@123 · <Link to="/register">Đăng ký</Link>
        </Typography.Paragraph>
      </Card>
    </div>
  );
}
