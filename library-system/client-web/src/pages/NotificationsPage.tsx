import { useEffect, useState } from 'react';
import { Button, Card, List, Space, Typography, message } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { api, type ApiResponse } from '@/services/api';

type Noti = {
  id: number;
  title?: string;
  body?: string;
  readAt?: string | null;
  createdAt?: string;
};

export function NotificationsPage() {
  const [rows, setRows] = useState<Noti[]>([]);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Noti[]>>('/notifications/me');
      setRows(data.data || []);
    } catch {
      message.error('Không tải được thông báo');
    } finally {
      setLoading(false);
    }
  };

  const markRead = async (id: number) => {
    try {
      await api.patch(`/notifications/${id}/read`);
      void load();
    } catch {
      message.error('Đánh dấu đọc thất bại');
    }
  };

  const readAll = async () => {
    try {
      await api.post('/notifications/read-all');
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
        Thông báo
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => void load()} />
          <Button onClick={() => void readAll()}>Đọc tất cả</Button>
        </Space>
        <List
          loading={loading}
          dataSource={rows}
          locale={{ emptyText: 'Không có thông báo' }}
          renderItem={(item) => (
            <List.Item
              actions={
                !item.readAt
                  ? [
                      <Button key="r" type="link" onClick={() => void markRead(item.id)}>
                        Đánh dấu đọc
                      </Button>,
                    ]
                  : undefined
              }
            >
              <List.Item.Meta
                title={
                  <Typography.Text strong={!item.readAt}>{item.title || 'Thông báo'}</Typography.Text>
                }
                description={item.body}
              />
            </List.Item>
          )}
        />
      </Card>
    </div>
  );
}
