import { useState } from 'react';
import { Button, Card, Descriptions, Form, Input, Typography, message } from 'antd';
import { useAuth } from '@/auth/AuthContext';
import { api } from '@/services/api';

export function ProfilePage() {
  const { user } = useAuth();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const changePassword = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      await api.post('/auth/change-password', {
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      });
      message.success('Đã đổi mật khẩu');
      form.resetFields();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Đổi mật khẩu thất bại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Hồ sơ
      </Typography.Title>
      <Card style={{ marginBottom: 16 }}>
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="Họ tên">{user?.fullName}</Descriptions.Item>
          <Descriptions.Item label="Email">{user?.email}</Descriptions.Item>
          <Descriptions.Item label="SĐT">{user?.phone || '—'}</Descriptions.Item>
          <Descriptions.Item label="Vai trò">{user?.role}</Descriptions.Item>
          <Descriptions.Item label="Trạng thái">{user?.status}</Descriptions.Item>
        </Descriptions>
      </Card>
      <Card title="Đổi mật khẩu" style={{ maxWidth: 480 }}>
        <Form form={form} layout="vertical" onFinish={() => void changePassword()}>
          <Form.Item name="oldPassword" label="Mật khẩu hiện tại" rules={[{ required: true }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item name="newPassword" label="Mật khẩu mới" rules={[{ required: true, min: 6 }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item
            name="confirm"
            label="Xác nhận"
            dependencies={['newPassword']}
            rules={[
              { required: true },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) return Promise.resolve();
                  return Promise.reject(new Error('Mật khẩu không khớp'));
                },
              }),
            ]}
          >
            <Input.Password />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={loading}>
            Lưu
          </Button>
        </Form>
      </Card>
    </div>
  );
}
