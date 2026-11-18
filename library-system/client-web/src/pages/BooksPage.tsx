import { useEffect, useState } from 'react';
import { Card, Input, List, Typography, message } from 'antd';
import { Link } from 'react-router-dom';
import { api, type ApiResponse } from '@/services/api';

type Book = {
  id: number;
  title: string;
  subtitle?: string;
  coverUrl?: string;
  avgRating?: number;
  borrowCount?: number;
};

type Meta = { page: number; limit: number; total: number };

export function BooksPage() {
  const [q, setQ] = useState('');
  const [rows, setRows] = useState<Book[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 12, total: 0 });
  const [loading, setLoading] = useState(false);

  const load = async (page = 0, keyword = q) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Book[]>>('/books', {
        params: { q: keyword || undefined, page, size: meta.limit },
      });
      setRows(data.data || []);
      const m = data.meta as Meta | undefined;
      setMeta({
        page: m?.page ?? page,
        limit: m?.limit ?? 12,
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
        Tra cứu sách
      </Typography.Title>
      <Input.Search
        allowClear
        placeholder="Tìm theo tên / ISBN / tác giả"
        enterButton="Tìm"
        size="large"
        style={{ marginBottom: 16, maxWidth: 560 }}
        value={q}
        onChange={(e) => setQ(e.target.value)}
        onSearch={() => void load(0)}
      />
      <List
        loading={loading}
        grid={{ gutter: 16, xs: 1, sm: 2, md: 3 }}
        dataSource={rows}
        pagination={{
          current: meta.page + 1,
          pageSize: meta.limit,
          total: meta.total,
          onChange: (p) => void load(p - 1),
        }}
        renderItem={(item) => (
          <List.Item>
            <Link to={`/books/${item.id}`}>
              <Card hoverable title={item.title}>
                <Typography.Paragraph type="secondary" ellipsis={{ rows: 2 }}>
                  {item.subtitle || '—'}
                </Typography.Paragraph>
                <Typography.Text type="secondary">
                  Rating: {item.avgRating ?? '—'} · Mượn: {item.borrowCount ?? 0}
                </Typography.Text>
              </Card>
            </Link>
          </List.Item>
        )}
      />
    </div>
  );
}
