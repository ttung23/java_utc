import { useEffect, useState } from 'react';
import { Button, Card, Space, Table, Tag, Typography, message } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { api, type ApiResponse } from '@/services/api';

type Fine = {
  id: number;
  type: string;
  amount: number;
  remaining: number;
  status: string;
  reason?: string;
};

type Meta = { page: number; limit: number; total: number };

export function MyFinesPage() {
  const [rows, setRows] = useState<Fine[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 20, total: 0 });
  const [loading, setLoading] = useState(false);

  const load = async (page = 0) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Fine[]>>('/fines/me', {
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
      message.error('Không tải được khoản phạt');
    } finally {
      setLoading(false);
    }
  };

  const requestPay = async (fine: Fine) => {
    try {
      await api.post('/payments/request', {
        fineIds: [fine.id],
        amount: fine.remaining,
      });
      message.success('Đã gửi yêu cầu nộp phạt');
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Gửi yêu cầu thất bại');
    }
  };

  useEffect(() => {
    void load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Phạt của tôi
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => void load(meta.page)} />
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
            { title: 'Loại', dataIndex: 'type', width: 120 },
            {
              title: 'Số tiền',
              dataIndex: 'amount',
              render: (v: number) => Number(v).toLocaleString('vi-VN'),
            },
            {
              title: 'Còn lại',
              dataIndex: 'remaining',
              render: (v: number) => Number(v).toLocaleString('vi-VN'),
            },
            {
              title: 'TT',
              dataIndex: 'status',
              render: (v: string) => <Tag>{v}</Tag>,
            },
            { title: 'Lý do', dataIndex: 'reason', ellipsis: true },
            {
              title: '',
              width: 140,
              render: (_, r) =>
                r.status === 'UNPAID' || r.status === 'PARTIAL' ? (
                  <Button size="small" type="primary" onClick={() => void requestPay(r)}>
                    Xin nộp
                  </Button>
                ) : null,
            },
          ]}
        />
      </Card>
    </div>
  );
}
