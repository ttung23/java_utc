import { useEffect, useState } from 'react';
import { Button, Card, Space, Table, Tag, Typography, message } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { api, type ApiResponse } from '@/services/api';

type OverdueItem = {
  id: number;
  bookTitle?: string;
  barcode?: string;
  dueDate?: string;
  overdue?: boolean;
  estimatedFine?: number;
  renewCount?: number;
  status: string;
};

export function OverduePage() {
  const [rows, setRows] = useState<OverdueItem[]>([]);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<OverdueItem[]>>('/borrows/overdue');
      setRows(data.data || []);
    } catch {
      message.error('Không tải được danh sách quá hạn');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Sách quá hạn
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => void load()}>
            Tải lại
          </Button>
        </Space>
        <Table
          rowKey="id"
          loading={loading}
          dataSource={rows}
          columns={[
            { title: 'Sách', dataIndex: 'bookTitle' },
            { title: 'Barcode', dataIndex: 'barcode', width: 140 },
            { title: 'Hạn trả', dataIndex: 'dueDate', width: 120 },
            {
              title: 'Phạt ước tính',
              dataIndex: 'estimatedFine',
              width: 130,
              render: (v?: number) => Number(v || 0).toLocaleString('vi-VN'),
            },
            { title: 'Gia hạn', dataIndex: 'renewCount', width: 90 },
            {
              title: 'TT',
              dataIndex: 'status',
              width: 120,
              render: (v: string) => <Tag color="red">{v}</Tag>,
            },
          ]}
        />
      </Card>
    </div>
  );
}
