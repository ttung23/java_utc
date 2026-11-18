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
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { api, type ApiResponse } from '@/services/api';
import { statusColorMap } from '@/styles/tokens';

type CardRow = {
  id: number;
  readerId: number;
  readerCode?: string;
  readerFullName?: string;
  cardNumber: string;
  barcode?: string;
  status: string;
  issuedAt?: string;
  expiresAt?: string;
};

type Meta = { page: number; limit: number; total: number };

export function CardsPage() {
  const [rows, setRows] = useState<CardRow[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 20, total: 0 });
  const [q, setQ] = useState('');
  const [status, setStatus] = useState<string | undefined>();
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm();

  const load = async (page = 0) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<CardRow[]>>('/cards', {
        params: { page, size: meta.limit, q: q || undefined, status },
      });
      setRows(data.data || []);
      const m = data.meta as Meta | undefined;
      setMeta({
        page: m?.page ?? page,
        limit: m?.limit ?? 20,
        total: m?.total ?? data.data?.length ?? 0,
      });
    } catch {
      message.error('Không tải được danh sách thẻ');
    } finally {
      setLoading(false);
    }
  };

  const issue = async () => {
    const values = await form.validateFields();
    try {
      await api.post('/cards', { readerId: values.readerId });
      message.success('Cấp thẻ thành công');
      setOpen(false);
      form.resetFields();
      void load(0);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Cấp thẻ thất bại');
    }
  };

  const renew = async (id: number) => {
    try {
      await api.patch(`/cards/${id}/renew`);
      message.success('Đã gia hạn');
      void load(meta.page);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Gia hạn thất bại');
    }
  };

  const reportLost = async (id: number) => {
    try {
      await api.patch(`/cards/${id}/report-lost`);
      message.success('Đã báo mất và cấp thẻ mới');
      void load(meta.page);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Thao tác thất bại');
    }
  };

  useEffect(() => {
    void load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status]);

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Thẻ thư viện
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }} wrap>
          <Input
            allowClear
            style={{ width: 260 }}
            placeholder="Tìm mã thẻ / barcode / độc giả"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onPressEnter={() => void load(0)}
          />
          <Select
            allowClear
            placeholder="Trạng thái"
            style={{ width: 160 }}
            value={status}
            onChange={setStatus}
            options={['ACTIVE', 'EXPIRED', 'LOST', 'SUSPENDED', 'REPLACED'].map((v) => ({
              value: v,
              label: v,
            }))}
          />
          <Button type="primary" onClick={() => void load(0)}>
            Tìm
          </Button>
          <Button icon={<ReloadOutlined />} onClick={() => void load(meta.page)} />
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setOpen(true)}>
            Cấp thẻ
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
            { title: 'Mã thẻ', dataIndex: 'cardNumber', width: 140 },
            { title: 'Barcode', dataIndex: 'barcode', width: 140 },
            { title: 'Độc giả', dataIndex: 'readerFullName' },
            { title: 'Mã ĐG', dataIndex: 'readerCode', width: 110 },
            {
              title: 'TT',
              dataIndex: 'status',
              width: 120,
              render: (v: string) => <Tag color={statusColorMap[v] || 'default'}>{v}</Tag>,
            },
            { title: 'Hết hạn', dataIndex: 'expiresAt', width: 180 },
            {
              title: '',
              width: 200,
              render: (_, r) =>
                r.status === 'ACTIVE' ? (
                  <Space>
                    <Button size="small" onClick={() => void renew(r.id)}>
                      Gia hạn
                    </Button>
                    <Button size="small" danger onClick={() => void reportLost(r.id)}>
                      Báo mất
                    </Button>
                  </Space>
                ) : null,
            },
          ]}
        />
      </Card>

      <Modal title="Cấp thẻ mới" open={open} onCancel={() => setOpen(false)} onOk={() => void issue()}>
        <Form form={form} layout="vertical">
          <Form.Item name="readerId" label="Reader ID" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={1} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
