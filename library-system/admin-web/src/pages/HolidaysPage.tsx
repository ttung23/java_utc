import { useEffect, useState } from 'react';
import {
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  Modal,
  Space,
  Switch,
  Table,
  Typography,
  message,
} from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { api, type ApiResponse } from '@/services/api';

type Holiday = {
  id: number;
  date: string;
  name: string;
  recurring?: boolean;
};

export function HolidaysPage() {
  const [rows, setRows] = useState<Holiday[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm();

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Holiday[]>>('/holidays');
      setRows(data.data || []);
    } catch {
      message.error('Không tải được ngày nghỉ');
    } finally {
      setLoading(false);
    }
  };

  const create = async () => {
    const values = await form.validateFields();
    try {
      await api.post('/holidays', {
        date: values.date.format('YYYY-MM-DD'),
        name: values.name,
        recurring: values.recurring ?? false,
      });
      message.success('Đã thêm');
      setOpen(false);
      form.resetFields();
      void load();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Thêm thất bại');
    }
  };

  const remove = async (id: number) => {
    try {
      await api.delete(`/holidays/${id}`);
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
        Ngày nghỉ
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
            {
              title: 'Ngày',
              dataIndex: 'date',
              width: 140,
              render: (v: string) => dayjs(v).format('DD/MM/YYYY'),
            },
            { title: 'Tên', dataIndex: 'name' },
            {
              title: 'Lặp hàng năm',
              dataIndex: 'recurring',
              width: 130,
              render: (v: boolean) => (v ? 'Có' : 'Không'),
            },
            {
              title: '',
              width: 100,
              render: (_, r) => (
                <Button size="small" danger onClick={() => void remove(r.id)}>
                  Xóa
                </Button>
              ),
            },
          ]}
        />
      </Card>

      <Modal title="Thêm ngày nghỉ" open={open} onCancel={() => setOpen(false)} onOk={() => void create()}>
        <Form form={form} layout="vertical" initialValues={{ recurring: false }}>
          <Form.Item name="date" label="Ngày" rules={[{ required: true }]}>
            <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" />
          </Form.Item>
          <Form.Item name="name" label="Tên" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="recurring" label="Lặp hàng năm" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
