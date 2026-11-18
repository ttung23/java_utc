import { useEffect, useState } from 'react';
import { Button, Card, Input, Space, Table, Tag, Typography, message } from 'antd';
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { api, type ApiResponse } from '@/services/api';
import { statusColorMap } from '@/styles/tokens';

type CopyRow = {
  id: number;
  bookId: number;
  bookTitle?: string;
  barcode: string;
  copyNo: number;
  status: string;
  condition?: string;
  locationCode?: string;
  locationName?: string;
};

type Meta = { page: number; limit: number; total: number };

export function CopiesPage() {
  const [rows, setRows] = useState<CopyRow[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 20, total: 0 });
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(false);

  const load = async (page = 0, keyword = q) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<CopyRow[]>>('/copies', {
        params: { q: keyword || undefined, page, size: meta.limit },
      });
      setRows(data.data || []);
      const m = data.meta as Meta | undefined;
      setMeta({
        page: m?.page ?? page,
        limit: m?.limit ?? 20,
        total: m?.total ?? data.data?.length ?? 0,
      });
    } catch {
      message.error('Không tải được danh sách bản sao');
    } finally {
      setLoading(false);
    }
  };

  const lookup = async () => {
    if (!q.trim()) {
      message.warning('Nhập barcode để tra cứu');
      return;
    }
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<CopyRow>>('/copies/lookup', {
        params: { barcode: q.trim() },
      });
      setRows(data.data ? [data.data] : []);
      setMeta({ page: 0, limit: 20, total: data.data ? 1 : 0 });
    } catch {
      message.error('Không tìm thấy barcode');
      setRows([]);
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
        Bản sao & Barcode
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }} wrap>
          <Input
            allowClear
            style={{ width: 320 }}
            prefix={<SearchOutlined />}
            placeholder="Barcode hoặc tên sách"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onPressEnter={() => void load(0)}
          />
          <Button type="primary" onClick={() => void load(0)}>
            Tìm kiếm
          </Button>
          <Button onClick={() => void lookup()}>Tra barcode</Button>
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
            { title: 'Barcode', dataIndex: 'barcode', width: 160 },
            { title: 'STT', dataIndex: 'copyNo', width: 70 },
            {
              title: 'Sách',
              dataIndex: 'bookTitle',
              render: (v, r) => v || `#${r.bookId}`,
            },
            {
              title: 'Vị trí',
              render: (_, r) => r.locationCode || r.locationName || '—',
              width: 120,
            },
            {
              title: 'Trạng thái',
              dataIndex: 'status',
              width: 130,
              render: (status: string) => (
                <Tag color={statusColorMap[status] || 'default'}>{status}</Tag>
              ),
            },
            { title: 'Tình trạng', dataIndex: 'condition', width: 110, render: (v) => v || '—' },
          ]}
        />
      </Card>
    </div>
  );
}
