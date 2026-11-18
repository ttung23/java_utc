import { useEffect, useState } from 'react';
import { Button, Card, Space, Table, Typography, message } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { api, type ApiResponse } from '@/services/api';

type AuditRow = {
  id: number;
  action?: string;
  entityType?: string;
  entityId?: string | number;
  createdAt?: string;
};

type SpringPage = {
  content: AuditRow[];
  totalElements: number;
  number: number;
  size: number;
};

export function AuditLogsPage() {
  const [rows, setRows] = useState<AuditRow[]>([]);
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);

  const load = async (p = 0) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<SpringPage | AuditRow[]>>('/audit-logs', {
        params: { page: p, size },
      });
      const payload = data.data;
      if (payload && !Array.isArray(payload) && 'content' in payload) {
        setRows(payload.content || []);
        setTotal(payload.totalElements || 0);
        setPage(payload.number ?? p);
      } else {
        setRows((payload as AuditRow[]) || []);
        setTotal(((payload as AuditRow[]) || []).length);
        setPage(p);
      }
    } catch {
      message.error('Không tải được nhật ký');
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
        Nhật ký hoạt động
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => void load(page)} />
        </Space>
        <Table
          rowKey="id"
          loading={loading}
          dataSource={rows}
          pagination={{
            current: page + 1,
            pageSize: size,
            total,
            onChange: (p) => void load(p - 1),
          }}
          columns={[
            { title: 'ID', dataIndex: 'id', width: 80 },
            { title: 'Action', dataIndex: 'action', width: 180 },
            { title: 'Entity', dataIndex: 'entityType', width: 140 },
            { title: 'Entity ID', dataIndex: 'entityId', width: 120 },
            { title: 'Thời gian', dataIndex: 'createdAt' },
          ]}
        />
      </Card>
    </div>
  );
}
