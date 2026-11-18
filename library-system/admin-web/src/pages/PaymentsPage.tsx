import { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { DollarOutlined, ReloadOutlined } from '@ant-design/icons';
import { api, type ApiResponse } from '@/services/api';
import { statusColorMap } from '@/styles/tokens';
import { useAuth } from '@/auth/AuthContext';

type Payment = {
  id: number;
  receiptCode: string;
  readerId: number;
  readerName?: string;
  amount: number;
  method: string;
  status: string;
  paidAt?: string;
  note?: string;
  createdAt?: string;
};

type Meta = { page: number; limit: number; total: number };

export function PaymentsPage() {
  const { user } = useAuth();
  const [rows, setRows] = useState<Payment[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 20, total: 0 });
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm();

  const load = async (page = 0) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Payment[]>>('/payments', {
        params: { page, size: meta.limit },
      });
      setRows(data.data || []);
      const m = data.meta as Meta | undefined;
      setMeta({
        page: m?.page ?? page,
        limit: m?.limit ?? 20,
        total: m?.total ?? data.data?.length ?? 0,
      });
    } catch {
      message.error('Không tải được biên lai');
    } finally {
      setLoading(false);
    }
  };

  const collect = async () => {
    const values = await form.validateFields();
    const fineIds = String(values.fineIds || '')
      .split(/[\s,]+/)
      .map((s: string) => Number(s.trim()))
      .filter((n: number) => !Number.isNaN(n) && n > 0);
    try {
      await api.post('/payments', {
        readerId: values.readerId,
        fineIds,
        amount: values.amount,
        method: values.method,
        note: values.note,
      });
      message.success('Thu tiền thành công');
      setOpen(false);
      form.resetFields();
      void load(0);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Thu tiền thất bại');
    }
  };

  const confirm = async (id: number) => {
    try {
      await api.post(`/payments/${id}/confirm`);
      message.success('Đã xác nhận');
      void load(meta.page);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Xác nhận thất bại');
    }
  };

  const refund = (id: number) => {
    let note = '';
    Modal.confirm({
      title: 'Hoàn tiền biên lai',
      content: (
        <Input.TextArea
          rows={2}
          placeholder="Ghi chú hoàn tiền"
          onChange={(e) => {
            note = e.target.value;
          }}
        />
      ),
      onOk: async () => {
        await api.post(`/payments/${id}/refund`, { note });
        message.success('Đã hoàn tiền');
        void load(meta.page);
      },
    });
  };

  useEffect(() => {
    void load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Biên lai thanh toán
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => void load(meta.page)}>
            Tải lại
          </Button>
          <Button type="primary" icon={<DollarOutlined />} onClick={() => setOpen(true)}>
            Thu tiền phạt
          </Button>
        </Space>
        <Table
          rowKey="id"
          loading={loading}
          dataSource={rows}
          pagination={{
            current: meta.page + 1,
            pageSize: meta.limit,
            total: meta.total,
            onChange: (p) => void load(p - 1),
          }}
          columns={[
            { title: 'Mã biên lai', dataIndex: 'receiptCode', width: 140 },
            { title: 'Độc giả', dataIndex: 'readerName' },
            {
              title: 'Số tiền',
              dataIndex: 'amount',
              width: 120,
              render: (v: number) => Number(v).toLocaleString('vi-VN'),
            },
            { title: 'PT', dataIndex: 'method', width: 100 },
            {
              title: 'TT',
              dataIndex: 'status',
              width: 120,
              render: (v: string) => <Tag color={statusColorMap[v] || 'default'}>{v}</Tag>,
            },
            { title: 'Thời gian', dataIndex: 'paidAt', width: 180 },
            {
              title: '',
              width: 180,
              render: (_, r) => (
                <Space>
                  {r.status === 'PENDING' ? (
                    <Button size="small" type="primary" onClick={() => void confirm(r.id)}>
                      Xác nhận
                    </Button>
                  ) : null}
                  {user?.role === 'ADMIN' && r.status === 'PAID' ? (
                    <Button size="small" danger onClick={() => refund(r.id)}>
                      Hoàn
                    </Button>
                  ) : null}
                </Space>
              ),
            },
          ]}
        />
      </Card>

      <Modal
        title="Thu tiền phạt"
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => void collect()}
        okText="Thu"
      >
        <Form form={form} layout="vertical" initialValues={{ method: 'CASH' }}>
          <Form.Item name="readerId" label="Reader ID" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={1} />
          </Form.Item>
          <Form.Item
            name="fineIds"
            label="Fine IDs (cách nhau bởi dấu phẩy)"
            rules={[{ required: true }]}
          >
            <Input placeholder="1,2,3" />
          </Form.Item>
          <Form.Item name="amount" label="Số tiền" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="method" label="Phương thức" rules={[{ required: true }]}>
            <Select
              options={['CASH', 'TRANSFER', 'CARD', 'OTHER'].map((v) => ({ value: v, label: v }))}
            />
          </Form.Item>
          <Form.Item name="note" label="Ghi chú">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
