import { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { api, type ApiResponse } from '@/services/api';
import { statusColorMap } from '@/styles/tokens';

type Staff = {
  id: number;
  email?: string;
  fullName?: string;
  phone?: string;
  role?: string;
  status?: string;
  staffCode?: string;
  position?: string;
  shift?: string;
};

export function StaffPage() {
  const [rows, setRows] = useState<Staff[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm();

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Staff[]>>('/staff');
      setRows(data.data || []);
    } catch {
      message.error('Không tải được nhân viên');
    } finally {
      setLoading(false);
    }
  };

  const create = async () => {
    const values = await form.validateFields();
    try {
      await api.post('/staff', values);
      message.success('Đã thêm nhân viên');
      setOpen(false);
      form.resetFields();
      void load();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Thêm thất bại');
    }
  };

  const setStatus = async (id: number, status: string) => {
    try {
      await api.patch(`/staff/${id}/status`, { status });
      message.success('Đã cập nhật trạng thái');
      void load();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Cập nhật thất bại');
    }
  };

  const resetPassword = (id: number) => {
    let password = '';
    Modal.confirm({
      title: 'Reset mật khẩu',
      content: (
        <Input.Password
          placeholder="Mật khẩu mới"
          onChange={(e) => {
            password = e.target.value;
          }}
        />
      ),
      onOk: async () => {
        if (!password || password.length < 6) {
          message.warning('Mật khẩu tối thiểu 6 ký tự');
          return Promise.reject();
        }
        await api.post(`/staff/${id}/reset-password`, { password });
        message.success('Đã reset mật khẩu');
      },
    });
  };

  useEffect(() => {
    void load();
  }, []);

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Nhân viên
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => void load()} />
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setOpen(true)}>
            Thêm
          </Button>
        </Space>
        <Table
          rowKey="id"
          loading={loading}
          dataSource={rows}
          columns={[
            { title: 'Mã NV', dataIndex: 'staffCode', width: 100 },
            { title: 'Họ tên', dataIndex: 'fullName' },
            { title: 'Email', dataIndex: 'email' },
            { title: 'SĐT', dataIndex: 'phone', width: 120 },
            { title: 'Vị trí', dataIndex: 'position', width: 120 },
            { title: 'Role', dataIndex: 'role', width: 100 },
            {
              title: 'TT',
              dataIndex: 'status',
              width: 110,
              render: (v: string) => <Tag color={statusColorMap[v] || 'default'}>{v}</Tag>,
            },
            {
              title: '',
              width: 260,
              render: (_, r) => (
                <Space wrap>
                  {r.status === 'ACTIVE' ? (
                    <Button size="small" onClick={() => void setStatus(r.id, 'INACTIVE')}>
                      Khóa
                    </Button>
                  ) : (
                    <Button size="small" type="primary" onClick={() => void setStatus(r.id, 'ACTIVE')}>
                      Mở
                    </Button>
                  )}
                  <Button size="small" onClick={() => resetPassword(r.id)}>
                    Reset MK
                  </Button>
                </Space>
              ),
            },
          ]}
        />
      </Card>

      <Modal title="Thêm nhân viên" open={open} onCancel={() => setOpen(false)} onOk={() => void create()}>
        <Form form={form} layout="vertical" initialValues={{ role: 'STAFF' }}>
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
          <Form.Item name="staffCode" label="Mã NV">
            <Input />
          </Form.Item>
          <Form.Item name="position" label="Vị trí">
            <Input />
          </Form.Item>
          <Form.Item name="shift" label="Ca">
            <Input />
          </Form.Item>
          <Form.Item name="role" label="Role">
            <Select options={[{ value: 'STAFF', label: 'STAFF' }, { value: 'ADMIN', label: 'ADMIN' }]} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
