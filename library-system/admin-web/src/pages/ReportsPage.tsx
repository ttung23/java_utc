import { useEffect, useState } from 'react';
import { Button, Card, Col, Row, Space, Table, Typography, message } from 'antd';
import { DownloadOutlined, ReloadOutlined } from '@ant-design/icons';
import { api, type ApiResponse } from '@/services/api';

type OverdueRow = {
  itemId: number;
  slipCode?: string;
  readerName?: string;
  bookTitle?: string;
  barcode?: string;
  dueDate?: string;
  overdueDays?: number;
};

type TopBook = { id: number; title: string; borrowCount: number };

export function ReportsPage() {
  const [overdue, setOverdue] = useState<OverdueRow[]>([]);
  const [topBooks, setTopBooks] = useState<TopBook[]>([]);
  const [inventory, setInventory] = useState<Record<string, number>>({});
  const [fines, setFines] = useState<{ unpaidTotal?: number }>({});
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const [o, t, i, f] = await Promise.all([
        api.get<ApiResponse<{ items: OverdueRow[] }>>('/reports/overdue'),
        api.get<ApiResponse<{ items: TopBook[] }>>('/reports/top-books', { params: { limit: 10 } }),
        api.get<ApiResponse<{ byStatus: Record<string, number> }>>('/reports/inventory'),
        api.get<ApiResponse<{ unpaidTotal: number }>>('/reports/fines'),
      ]);
      setOverdue(o.data.data?.items || []);
      setTopBooks(t.data.data?.items || []);
      setInventory(i.data.data?.byStatus || {});
      setFines(f.data.data || {});
    } catch {
      message.error('Không tải được báo cáo');
    } finally {
      setLoading(false);
    }
  };

  const exportOverdue = async () => {
    try {
      const res = await api.get('/reports/overdue/export', { responseType: 'blob' });
      const url = URL.createObjectURL(res.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'overdue.xlsx';
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      message.error('Xuất Excel thất bại');
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const invRows = Object.entries(inventory).map(([status, count]) => ({ status, count }));

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Báo cáo & Thống kê
      </Typography.Title>
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ReloadOutlined />} onClick={() => void load()} loading={loading}>
          Tải lại
        </Button>
        <Button icon={<DownloadOutlined />} onClick={() => void exportOverdue()}>
          Xuất quá hạn (Excel)
        </Button>
        <Typography.Text>
          Phạt chưa thu: <strong>{Number(fines.unpaidTotal || 0).toLocaleString('vi-VN')}</strong>
        </Typography.Text>
      </Space>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={14}>
          <Card title="Quá hạn">
            <Table
              rowKey="itemId"
              size="small"
              loading={loading}
              dataSource={overdue}
              pagination={{ pageSize: 8 }}
              columns={[
                { title: 'Phiếu', dataIndex: 'slipCode', width: 120 },
                { title: 'Độc giả', dataIndex: 'readerName' },
                { title: 'Sách', dataIndex: 'bookTitle' },
                { title: 'Hạn', dataIndex: 'dueDate', width: 110 },
                { title: 'Ngày', dataIndex: 'overdueDays', width: 70 },
              ]}
            />
          </Card>
        </Col>
        <Col xs={24} lg={10}>
          <Card title="Top sách mượn" style={{ marginBottom: 16 }}>
            <Table
              rowKey="id"
              size="small"
              loading={loading}
              dataSource={topBooks}
              pagination={false}
              columns={[
                { title: 'Sách', dataIndex: 'title' },
                { title: 'Lượt', dataIndex: 'borrowCount', width: 80 },
              ]}
            />
          </Card>
          <Card title="Tồn kho theo trạng thái">
            <Table
              rowKey="status"
              size="small"
              loading={loading}
              dataSource={invRows}
              pagination={false}
              columns={[
                { title: 'Trạng thái', dataIndex: 'status' },
                { title: 'Số lượng', dataIndex: 'count', width: 100 },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
