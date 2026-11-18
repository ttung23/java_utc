import { useEffect, useState } from 'react';
import { Button, Card, Space, Table, Tag, Typography, message } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { api, type ApiResponse } from '@/services/api';

type Slip = {
  id: number;
  slipCode: string;
  status: string;
  channel: string;
  createdAt?: string;
  items?: Array<{ bookTitle?: string; barcode?: string; dueDate?: string; status: string }>;
};

type Meta = { page: number; limit: number; total: number };

export function MyBorrowsPage() {
  const [rows, setRows] = useState<Slip[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 20, total: 0 });
  const [loading, setLoading] = useState(false);

  const load = async (page = 0) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Slip[]>>('/borrows/me', {
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
      message.error('Không tải được phiếu mượn');
    } finally {
      setLoading(false);
    }
  };

  const cancel = async (id: number) => {
    try {
      await api.post(`/borrows/${id}/cancel`, {});
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
        Phiếu mượn của tôi
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => void load(meta.page)} />
        </Space>
        <Table
          rowKey="id"
          loading={loading}
          dataSource={rows}
          expandable={{
            expandedRowRender: (r) => (
              <Table
                size="small"
                rowKey={(_, i) => String(i)}
                pagination={false}
                dataSource={r.items || []}
                columns={[
                  { title: 'Sách', dataIndex: 'bookTitle' },
                  { title: 'Barcode', dataIndex: 'barcode' },
                  { title: 'Hạn', dataIndex: 'dueDate' },
                  { title: 'TT', dataIndex: 'status' },
                ]}
              />
            ),
          }}
          pagination={{
            current: meta.page + 1,
            pageSize: meta.limit,
            total: meta.total,
            onChange: (p) => void load(p - 1),
          }}
          columns={[
            { title: 'Mã phiếu', dataIndex: 'slipCode', width: 140 },
            { title: 'Kênh', dataIndex: 'channel', width: 100 },
            {
              title: 'TT',
              dataIndex: 'status',
              width: 120,
              render: (v: string) => <Tag>{v}</Tag>,
            },
            { title: 'Ngày tạo', dataIndex: 'createdAt' },
            {
              title: '',
              width: 100,
              render: (_, r) =>
                r.status === 'PENDING' || r.status === 'APPROVED' ? (
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
