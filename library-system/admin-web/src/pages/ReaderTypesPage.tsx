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

type ReaderType = {
  id: number;
  code: string;
  name: string;
  maxBooks: number;
  loanDays: number;
  maxRenewals: number;
  renewDays: number;
  maxReservations: number;
  cardFee: number;
  cardValidityMonths: number;
  active: boolean;
};

export function ReaderTypesPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const [rows, setRows] = useState<ReaderType[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<ReaderType | null>(null);
  const [form] = Form.useForm();

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<ReaderType[]>>('/reader-types');
      setRows(data.data || []);
    } catch {
      message.error('Không tải được loại độc giả');
    } finally {
      setLoading(false);
    }
  };

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({
      maxBooks: 5,
      loanDays: 14,
      maxRenewals: 2,
      renewDays: 7,
      maxReservations: 3,
      cardFee: 0,
      cardValidityMonths: 12,
      active: true,
    });
    setOpen(true);
  };

  const openEdit = (row: ReaderType) => {
    setEditing(row);
    form.setFieldsValue(row);
    setOpen(true);
  };

  const save = async () => {
    const values = await form.validateFields();
    try {
      if (editing) {
        await api.put(`/reader-types/${editing.id}`, values);
        message.success('Đã cập nhật');
      } else {
        await api.post('/reader-types', values);
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
      await api.delete(`/reader-types/${id}`);
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
        Loại độc giả
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => void load()}>
            Tải lại
          </Button>
          {isAdmin ? (
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
              Thêm
            </Button>
          ) : null}
        </Space>
        <Table
          rowKey="id"
          loading={loading}
          dataSource={rows}
          pagination={false}
          columns={[
            { title: 'Mã', dataIndex: 'code', width: 110 },
            { title: 'Tên', dataIndex: 'name' },
            { title: 'Max sách', dataIndex: 'maxBooks', width: 90 },
            { title: 'Ngày mượn', dataIndex: 'loanDays', width: 100 },
            { title: 'Gia hạn', dataIndex: 'maxRenewals', width: 90 },
            { title: 'Đặt trước', dataIndex: 'maxReservations', width: 100 },
            {
              title: 'Phí thẻ',
              dataIndex: 'cardFee',
              width: 100,
              render: (v: number) => Number(v).toLocaleString('vi-VN'),
            },
            {
              title: 'TT',
              dataIndex: 'active',
              width: 90,
              render: (v: boolean) => <Tag color={v ? 'green' : 'default'}>{v ? 'ACTIVE' : 'OFF'}</Tag>,
            },
            {
              title: '',
              width: 160,
              render: (_, r) =>
                isAdmin ? (
                  <Space>
                    <Button size="small" onClick={() => openEdit(r)}>
                      Sửa
                    </Button>
                    <Button size="small" danger onClick={() => void remove(r.id)}>
                      Xóa
                    </Button>
                  </Space>
                ) : null,
            },
          ]}
        />
      </Card>

      <Modal
        title={editing ? 'Sửa loại độc giả' : 'Thêm loại độc giả'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => void save()}
        width={560}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="code" label="Mã" rules={[{ required: true }]}>
            <Input disabled={!!editing} />
          </Form.Item>
          <Form.Item name="name" label="Tên" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Space wrap style={{ width: '100%' }} size="middle">
            <Form.Item name="maxBooks" label="Max sách" rules={[{ required: true }]}>
              <InputNumber min={0} />
            </Form.Item>
            <Form.Item name="loanDays" label="Ngày mượn" rules={[{ required: true }]}>
              <InputNumber min={1} />
            </Form.Item>
            <Form.Item name="maxRenewals" label="Max gia hạn" rules={[{ required: true }]}>
              <InputNumber min={0} />
            </Form.Item>
            <Form.Item name="renewDays" label="Ngày gia hạn" rules={[{ required: true }]}>
              <InputNumber min={1} />
            </Form.Item>
            <Form.Item name="maxReservations" label="Max đặt trước" rules={[{ required: true }]}>
              <InputNumber min={0} />
            </Form.Item>
            <Form.Item name="cardFee" label="Phí thẻ" rules={[{ required: true }]}>
              <InputNumber min={0} />
            </Form.Item>
            <Form.Item name="cardValidityMonths" label="Hiệu lực (tháng)" rules={[{ required: true }]}>
              <InputNumber min={1} />
            </Form.Item>
            <Form.Item name="active" label="Active" valuePropName="checked">
              <Switch />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  );
}
