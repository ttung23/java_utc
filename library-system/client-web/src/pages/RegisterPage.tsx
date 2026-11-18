import { Alert, Button, Card, Form, Input, Typography, message } from 'antd';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AxiosError } from 'axios';
import { authApi, type ApiResponse } from '@/services/api';

export function RegisterPage() {
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  return (
    <div style={{ minHeight: '70vh', display: 'grid', placeItems: 'center' }}>
      <Card style={{ width: 440, maxWidth: '100%' }}>
        <Typography.Title level={3}>Đăng ký tài khoản</Typography.Title>
        {error ? <Alert type="error" showIcon style={{ marginBottom: 16 }} message={error} /> : null}
        <Form
          layout="vertical"
          onFinish={async (values) => {
            setLoading(true);
            setError(null);
            try {
              await authApi.register({
                email: values.email,
                phone: values.phone,
                fullName: values.fullName,
                password: values.password,
              });
              message.success('Đăng ký thành công — vui lòng đăng nhập');
              navigate('/login');
            } catch (err) {
              const ax = err as AxiosError<ApiResponse<unknown>>;
              setError(ax.response?.data?.message || 'Đăng ký thất bại');
            } finally {
              setLoading(false);
            }
          }}
        >
          <Form.Item name="fullName" label="Họ tên" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="email" label="Email" rules={[{ required: true, type: 'email' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="phone" label="SĐT">
            <Input />
          </Form.Item>
          <Form.Item name="password" label="Mật khẩu" rules={[{ required: true, min: 6 }]}>
            <Input.Password />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>
            Đăng ký
          </Button>
        </Form>
        <Typography.Paragraph style={{ marginTop: 12, marginBottom: 0 }}>
          Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
        </Typography.Paragraph>
      </Card>
    </div>
  );
}
