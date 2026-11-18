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
import { useAuth } from '@/auth/AuthContext';

type Fine = {
  id: number;
  readerId: number;
  readerName?: string;
  type: string;
  amount: number;
  paidAmount: number;
  remaining: number;
  status: string;
  reason?: string;
  createdAt?: string;
};

type Meta = { page: number; limit: number; total: number };

export function FinesPage() {
  const { user } = useAuth();
  const [rows, setRows] = useState<Fine[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 20, total: 0 });
  const [status, setStatus] = useState<string | undefined>('UNPAID');
  const [loading, setLoading] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [form] = Form.useForm();

  const load = async (page = 0) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Fine[]>>('/fines', {
        params: { page, size: meta.limit, status },
      });
      setRows(data.data || []);
      const m = data.meta as Meta | undefined;
      setMeta({
        page: m?.page ?? page,
        limit: m?.limit ?? 20,
        total: m?.total ?? data.data?.length ?? 0,
      });
    } catch {
      message.error('Không tải được khoản phạt');
    } finally {
      setLoading(false);
    }
  };

  const create = async () => {
    const values = await form.validateFields();
    try {
      await api.post('/fines', values);
      message.success('Đã tạo phạt');
      setCreateOpen(false);
      form.resetFields();
      void load(0);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Tạo thất bại');
    }
  };

  const waive = (fine: Fine) => {
    let reason = '';
    let amount: number | undefined;
    Modal.confirm({
      title: `Miễn/giảm phạt #${fine.id}`,
      content: (
        <Space direction="vertical" style={{ width: '100%' }}>
          <Input.TextArea
            rows={2}
            placeholder="Lý do"
            onChange={(e) => {
              reason = e.target.value;
            }}
          />
          <InputNumber
            style={{ width: '100%' }}
            min={0}
            max={Number(fine.remaining)}
            placeholder="Số tiền miễn (để trống = miễn toàn bộ remaining)"
            onChange={(v) => {
              amount = typeof v === 'number' ? v : undefined;
            }}
          />
        </Space>
      ),
      onOk: async () => {
        if (!reason.trim()) {
          message.warning('Nhập lý do');
          return Promise.reject();
        }
        await api.post(`/fines/${fine.id}/waive`, { reason: reason.trim(), amount });
        message.success('Đã miễn/giảm');
        void load(meta.page);
      },
    });
  };

  useEffect(() => {
    void load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status]);

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Khoản phạt
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }} wrap>
          <Select
            allowClear
            placeholder="Trạng thái"
            style={{ width: 160 }}
            value={status}
            onChange={setStatus}
            options={['UNPAID', 'PARTIAL', 'PAID', 'WAIVED'].map((v) => ({ value: v, label: v }))}
          />
          <Button icon={<ReloadOutlined />} onClick={() => void load(meta.page)}>
            Tải lại
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
            Tạo phạt
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
            { title: 'ID', dataIndex: 'id', width: 70 },
            { title: 'Độc giả', dataIndex: 'readerName' },
            { title: 'Loại', dataIndex: 'type', width: 120 },
            {
              title: 'Số tiền',
              dataIndex: 'amount',
              width: 110,
              render: (v: number) => Number(v).toLocaleString('vi-VN'),
            },
            {
              title: 'Còn lại',
              dataIndex: 'remaining',
              width: 110,
              render: (v: number) => Number(v).toLocaleString('vi-VN'),
            },
            {
              title: 'TT',
              dataIndex: 'status',
              width: 110,
              render: (v: string) => <Tag color={statusColorMap[v] || 'default'}>{v}</Tag>,
            },
            { title: 'Lý do', dataIndex: 'reason', ellipsis: true },
            {
              title: '',
              width: 110,
              render: (_, r) =>
                user?.role === 'ADMIN' && (r.status === 'UNPAID' || r.status === 'PARTIAL') ? (
                  <Button size="small" onClick={() => waive(r)}>
                    Miễn/giảm
                  </Button>
                ) : null,
            },
          ]}
        />
      </Card>

      <Modal
        title="Tạo phạt thủ công"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={() => void create()}
        okText="Tạo"
      >
        <Form form={form} layout="vertical">
          <Form.Item name="readerId" label="Reader ID" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={1} />
          </Form.Item>
          <Form.Item name="type" label="Loại" rules={[{ required: true }]} initialValue="OTHER">
            <Select
              options={['OVERDUE', 'DAMAGE', 'LOST', 'OTHER'].map((v) => ({ value: v, label: v }))}
            />
          </Form.Item>
          <Form.Item name="amount" label="Số tiền" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="reason" label="Lý do">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
