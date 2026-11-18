import { useEffect, useState } from 'react';
import { Button, Card, Input, Space, Table, Tag, Typography, message } from 'antd';
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import { api, type ApiResponse } from '@/services/api';
import { statusColorMap } from '@/styles/tokens';

type ReaderRow = {
  id: number;
  readerCode: string;
  fullName: string;
  email?: string;
  phone?: string;
  readerTypeName?: string;
  cardNumber?: string;
  cardStatus?: string;
  blacklisted?: boolean;
  userStatus?: string;
};

type Meta = { page: number; limit: number; total: number };

export function ReadersPage() {
  const [rows, setRows] = useState<ReaderRow[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 20, total: 0 });
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(false);

  const load = async (page = 0, keyword = q) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<ReaderRow[]>>('/readers', {
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
      message.error('Không tải được danh sách độc giả');
    } finally {
      setLoading(false);
    }
  };

  const lookupCard = async () => {
    if (!q.trim()) {
      message.warning('Nhập mã thẻ / barcode');
      return;
    }
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<ReaderRow & { readerTypeName?: string }>>(
        '/readers/lookup',
        { params: { card: q.trim() } },
      );
      const d = data.data;
      setRows(
        d
          ? [
              {
                id: d.id,
                readerCode: d.readerCode,
                fullName: d.fullName,
                email: d.email,
                phone: d.phone,
                readerTypeName: d.readerTypeName,
                cardNumber: d.cardNumber,
                cardStatus: d.cardStatus,
                blacklisted: d.blacklisted,
                userStatus: d.userStatus,
              },
            ]
          : [],
      );
      setMeta({ page: 0, limit: 20, total: d ? 1 : 0 });
    } catch {
      message.error('Không tìm thấy thẻ');
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
        Danh sách độc giả
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }} wrap>
          <Input
            allowClear
            style={{ width: 340 }}
            prefix={<SearchOutlined />}
            placeholder="Tên / email / SĐT / mã thẻ / mã độc giả"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onPressEnter={() => void load(0)}
          />
          <Button type="primary" onClick={() => void load(0)}>
            Tìm kiếm
          </Button>
          <Button onClick={() => void lookupCard()}>Quét thẻ</Button>
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
            {
              title: 'Mã',
              dataIndex: 'readerCode',
              width: 120,
              render: (v, r) => <Link to={`/readers/${r.id}`}>{v}</Link>,
            },
            { title: 'Họ tên', dataIndex: 'fullName' },
            { title: 'Email', dataIndex: 'email', render: (v) => v || '—' },
            { title: 'SĐT', dataIndex: 'phone', width: 120, render: (v) => v || '—' },
            { title: 'Loại', dataIndex: 'readerTypeName', width: 140, render: (v) => v || '—' },
            { title: 'Thẻ', dataIndex: 'cardNumber', width: 140, render: (v) => v || '—' },
            {
              title: 'TT thẻ',
              dataIndex: 'cardStatus',
              width: 110,
              render: (s?: string) =>
                s ? <Tag color={statusColorMap[s] || 'default'}>{s}</Tag> : '—',
            },
            {
              title: 'Cờ',
              width: 100,
              render: (_, r) =>
                r.blacklisted ? <Tag color="red">Blacklist</Tag> : <Tag color="green">OK</Tag>,
            },
          ]}
        />
      </Card>
    </div>
  );
}
