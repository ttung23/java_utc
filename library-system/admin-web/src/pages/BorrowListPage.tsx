import { useEffect, useState } from 'react';
import { Button, Card, Select, Space, Table, Tag, Typography, message } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import { api, type ApiResponse, tokenStore } from '@/services/api';
import { statusColorMap } from '@/styles/tokens';

type Item = {
  id: number;
  bookTitle?: string;
  barcode?: string;
  status: string;
  dueDate?: string;
  overdue?: boolean;
};

type Slip = {
  id: number;
  slipCode: string;
  readerName?: string;
  readerCode?: string;
  channel: string;
  status: string;
  overdue?: boolean;
  borrowedAt?: string;
  createdAt?: string;
  items?: Item[];
};

type Meta = { page: number; limit: number; total: number };

export function BorrowListPage() {
  const [rows, setRows] = useState<Slip[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 20, total: 0 });
  const [status, setStatus] = useState<string | undefined>();
  const [channel, setChannel] = useState<string | undefined>();
  const [loading, setLoading] = useState(false);

  const load = async (page = 0) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Slip[]>>('/borrows', {
        params: { page, size: meta.limit, status, channel },
      });
      setRows(data.data || []);
      const m = data.meta as Meta | undefined;
      setMeta({
        page: m?.page ?? page,
        limit: m?.limit ?? 20,
        total: m?.total ?? data.data?.length ?? 0,
      });
    } catch {
      message.error('Không tải được danh sách phiếu mượn');
    } finally {
      setLoading(false);
    }
  };

  const printPdf = async (id: number) => {
    try {
      const res = await api.get(`/borrows/${id}/print`, { responseType: 'blob' });
      const url = URL.createObjectURL(res.data);
      window.open(url, '_blank');
    } catch {
      message.error('Không in được PDF');
    }
  };

  useEffect(() => {
    void load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, channel]);

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Danh sách phiếu mượn
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }} wrap>
          <Select
            allowClear
            placeholder="Trạng thái"
            style={{ width: 180 }}
            value={status}
            onChange={setStatus}
            options={[
              'PENDING',
              'APPROVED',
              'BORROWING',
              'PARTIAL_RETURNED',
              'RETURNED',
              'CANCELLED',
              'REJECTED',
              'EXPIRED',
            ].map((v) => ({ value: v, label: v }))}
          />
          <Select
            allowClear
            placeholder="Kênh"
            style={{ width: 140 }}
            value={channel}
            onChange={setChannel}
            options={[
              { value: 'COUNTER', label: 'Quầy' },
              { value: 'ONLINE', label: 'Online' },
            ]}
          />
          <Button icon={<ReloadOutlined />} onClick={() => void load(meta.page)}>
            Tải lại
          </Button>
          <Link to="/borrows/create">
            <Button type="primary">Tạo phiếu quầy</Button>
          </Link>
        </Space>
        <Table
          rowKey="id"
          loading={loading}
          dataSource={rows}
          expandable={{
            expandedRowRender: (r) => (
              <Table
                size="small"
                rowKey="id"
                pagination={false}
                dataSource={r.items || []}
                columns={[
                  { title: 'Sách', dataIndex: 'bookTitle' },
                  { title: 'Barcode', dataIndex: 'barcode' },
                  {
                    title: 'TT',
                    dataIndex: 'status',
                    render: (v: string) => <Tag color={statusColorMap[v] || 'default'}>{v}</Tag>,
                  },
                  { title: 'Hạn trả', dataIndex: 'dueDate' },
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
            { title: 'Độc giả', dataIndex: 'readerName' },
            { title: 'Kênh', dataIndex: 'channel', width: 100 },
            {
              title: 'Trạng thái',
              dataIndex: 'status',
              width: 140,
              render: (v: string, r) => (
                <Space>
                  <Tag color={statusColorMap[v] || 'default'}>{v}</Tag>
                  {r.overdue ? <Tag color="red">OVERDUE</Tag> : null}
                </Space>
              ),
            },
            { title: 'Ngày tạo', dataIndex: 'createdAt', width: 180 },
            {
              title: '',
              width: 100,
              render: (_, r) => (
                <Button
                  type="link"
                  onClick={() => void printPdf(r.id)}
                  disabled={!tokenStore.getAccess()}
                >
                  PDF
                </Button>
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
}
