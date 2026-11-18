import { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { api, type ApiResponse } from '@/services/api';
import { useAuth } from '@/auth/AuthContext';

type Banner = {
  id: number;
  imageUrl: string;
  link?: string;
  sortOrder?: number;
  active?: boolean;
};

export function BannersPage() {
  const { user } = useAuth();
  const [rows, setRows] = useState<Banner[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Banner | null>(null);
  const [form] = Form.useForm();

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Banner[]>>('/banners');
      setRows(data.data || []);
    } catch {
      message.error('Không tải được banner');
    } finally {
      setLoading(false);
    }
  };

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ sortOrder: 0, active: true });
    setOpen(true);
  };

  const openEdit = (row: Banner) => {
    setEditing(row);
    form.setFieldsValue(row);
    setOpen(true);
  };

  const save = async () => {
    const values = await form.validateFields();
    try {
      if (editing) {
        await api.put(`/banners/${editing.id}`, values);
        message.success('Đã cập nhật');
      } else {
        await api.post('/banners', values);
        message.success('Đã tạo');
      }
      setOpen(false);
      void load();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Lưu thất bại');
    }
  };

  const remove = async (id: number) => {
    try {
      await api.delete(`/banners/${id}`);
      message.success('Đã xóa');
      void load();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Xóa thất bại');
    }
  };

  useEffect(() => {
    void load();
  }, []);

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Banner
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => void load()} />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            Thêm
          </Button>
        </Space>
        <Table
          rowKey="id"
          loading={loading}
          dataSource={rows}
          pagination={false}
          columns={[
            {
              title: 'Ảnh',
              dataIndex: 'imageUrl',
              width: 100,
              render: (v: string) =>
                v ? <img src={v} alt="" style={{ width: 64, height: 36, objectFit: 'cover' }} /> : '—',
            },
            { title: 'URL ảnh', dataIndex: 'imageUrl', ellipsis: true },
            { title: 'Link', dataIndex: 'link', ellipsis: true },
            { title: 'Thứ tự', dataIndex: 'sortOrder', width: 90 },
            {
              title: 'TT',
              dataIndex: 'active',
              width: 90,
              render: (v: boolean) => <Tag color={v ? 'green' : 'default'}>{v ? 'ON' : 'OFF'}</Tag>,
            },
            {
              title: '',
              width: 160,
              render: (_, r) => (
                <Space>
                  <Button size="small" onClick={() => openEdit(r)}>
                    Sửa
                  </Button>
                  {user?.role === 'ADMIN' ? (
                    <Button size="small" danger onClick={() => void remove(r.id)}>
                      Xóa
                    </Button>
                  ) : null}
                </Space>
              ),
            },
          ]}
        />
      </Card>

      <Modal
        title={editing ? 'Sửa banner' : 'Thêm banner'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => void save()}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="imageUrl" label="Image URL" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="link" label="Link">
            <Input />
          </Form.Item>
          <Form.Item name="sortOrder" label="Thứ tự">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="active" label="Active" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
