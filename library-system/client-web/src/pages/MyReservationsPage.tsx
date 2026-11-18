import { useEffect, useState } from 'react';
import { Button, Card, Space, Table, Tag, Typography, message } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { api, type ApiResponse } from '@/services/api';

type Row = {
  id: number;
  bookTitle?: string;
  status: string;
  queuePosition?: number;
  readyUntil?: string;
};

type Meta = { page: number; limit: number; total: number };

export function MyReservationsPage() {
  const [rows, setRows] = useState<Row[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 20, total: 0 });
  const [loading, setLoading] = useState(false);

  const load = async (page = 0) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Row[]>>('/reservations/me', {
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
      message.error('Không tải được đặt trước');
    } finally {
      setLoading(false);
    }
  };

  const cancel = async (id: number) => {
    try {
      await api.post(`/reservations/${id}/cancel`);
      message.success('Đã hủy');
      void load(meta.page);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Hủy thất bại');
    }
  };

  useEffect(() => {
    void load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Đặt trước của tôi
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
            { title: 'Sách', dataIndex: 'bookTitle' },
            { title: 'TT', dataIndex: 'status', render: (v: string) => <Tag>{v}</Tag> },
            { title: 'Hàng đợi', dataIndex: 'queuePosition', width: 100 },
            { title: 'Giữ đến', dataIndex: 'readyUntil' },
            {
              title: '',
              width: 100,
              render: (_, r) =>
                r.status === 'WAITING' || r.status === 'READY' ? (
                  <Button size="small" danger onClick={() => void cancel(r.id)}>
                    Hủy
                  </Button>
                ) : null,
            },
          ]}
        />
      </Card>
    </div>
  );
}
