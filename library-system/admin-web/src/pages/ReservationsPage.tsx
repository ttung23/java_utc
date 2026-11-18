import { useEffect, useState } from 'react';
import { Button, Card, Space, Table, Tag, Typography, message } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { api, type ApiResponse } from '@/services/api';
import { statusColorMap } from '@/styles/tokens';

type Row = {
  id: number;
  readerName?: string;
  bookTitle?: string;
  status: string;
  queuePosition?: number;
  barcode?: string;
  readyUntil?: string;
  createdAt?: string;
};

type Meta = { page: number; limit: number; total: number };

export function ReservationsPage() {
  const [rows, setRows] = useState<Row[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 20, total: 0 });
  const [loading, setLoading] = useState(false);

  const load = async (page = 0) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Row[]>>('/reservations', {
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
        Đặt trước
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => void load(meta.page)}>
            Tải lại
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
            { title: 'Độc giả', dataIndex: 'readerName' },
            { title: 'Sách', dataIndex: 'bookTitle' },
            {
              title: 'TT',
              dataIndex: 'status',
              width: 120,
              render: (v: string) => <Tag color={statusColorMap[v] || 'default'}>{v}</Tag>,
            },
            { title: 'Hàng đợi', dataIndex: 'queuePosition', width: 100 },
            { title: 'Barcode gán', dataIndex: 'barcode', width: 140 },
            { title: 'Giữ đến', dataIndex: 'readyUntil', width: 180 },
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
