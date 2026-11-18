import { useEffect, useState } from 'react';
import { Button, Card, List, Typography, message } from 'antd';
import { Link } from 'react-router-dom';
import { api, type ApiResponse } from '@/services/api';

type Fav = { bookId: number; title?: string; bookTitle?: string };

export function FavoritesPage() {
  const [rows, setRows] = useState<Fav[]>([]);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Fav[]>>('/favorites/me');
      setRows(data.data || []);
    } catch {
      message.error('Không tải được yêu thích');
    } finally {
      setLoading(false);
    }
  };

  const remove = async (bookId: number) => {
    try {
      await api.delete(`/favorites/${bookId}`);
      message.success('Đã bỏ yêu thích');
      void load();
    } catch {
      message.error('Thao tác thất bại');
    }
  };

  useEffect(() => {
    void load();
  }, []);

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Sách yêu thích
      </Typography.Title>
      <Card loading={loading}>
        <List
          dataSource={rows}
          locale={{ emptyText: 'Chưa có sách yêu thích' }}
          renderItem={(item) => (
            <List.Item
              actions={[
                <Button key="rm" type="link" danger onClick={() => void remove(item.bookId)}>
                  Bỏ
                </Button>,
              ]}
            >
              <Link to={`/books/${item.bookId}`}>{item.bookTitle || item.title || `Book #${item.bookId}`}</Link>
            </List.Item>
          )}
        />
      </Card>
    </div>
  );
}
