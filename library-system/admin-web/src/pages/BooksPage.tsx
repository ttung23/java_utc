import { useEffect, useState } from 'react';
import { Button, Card, Input, Space, Table, Tag, Typography, message } from 'antd';
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { api, type ApiResponse } from '@/services/api';
import { statusColorMap } from '@/styles/tokens';

type BookRow = {
  id: number;
  title: string;
  isbn13?: string;
  publisherName?: string;
  publishYear?: number;
  status: string;
  totalCopies: number;
  availableCopies: number;
  avgRating?: number;
};

type Meta = { page: number; limit: number; total: number };

export function BooksPage() {
  const [rows, setRows] = useState<BookRow[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 20, total: 0 });
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(false);

  const load = async (page = 0, keyword = q) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<BookRow[]>>('/books', {
        params: { q: keyword || undefined, page, size: meta.limit, sort: 'title,asc' },
      });
      setRows(data.data || []);
      const m = data.meta as Meta | undefined;
      setMeta({
        page: m?.page ?? page,
        limit: m?.limit ?? 20,
        total: m?.total ?? data.data?.length ?? 0,
      });
    } catch {
      message.error('Không tải được danh sách sách');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Đầu sách
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }} wrap>
          <Input
            allowClear
            style={{ width: 320 }}
            prefix={<SearchOutlined />}
            placeholder="Tìm tên / ISBN"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onPressEnter={() => void load(0)}
          />
          <Button type="primary" onClick={() => void load(0)}>
            Tìm kiếm
          </Button>
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
            showSizeChanger: false,
            onChange: (p) => void load(p - 1),
          }}
          columns={[
            { title: 'ID', dataIndex: 'id', width: 70 },
            { title: 'Tên sách', dataIndex: 'title' },
            { title: 'ISBN', dataIndex: 'isbn13', width: 140, render: (v) => v || '—' },
            { title: 'NXB', dataIndex: 'publisherName', width: 160, render: (v) => v || '—' },
            { title: 'Năm', dataIndex: 'publishYear', width: 80, render: (v) => v || '—' },
            {
              title: 'Tồn',
              width: 110,
              render: (_, r) => `${r.availableCopies}/${r.totalCopies}`,
            },
            {
              title: 'Trạng thái',
              dataIndex: 'status',
              width: 120,
              render: (status: string) => (
                <Tag color={statusColorMap[status] || 'default'}>{status}</Tag>
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
}
