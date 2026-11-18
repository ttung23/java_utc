import { useEffect, useState } from 'react';
import { Card, Col, Row, Table, Typography, message } from 'antd';
import { api, type ApiResponse } from '@/services/api';

type NamedItem = { id: number; name: string; code?: string };

export function CatalogPage() {
  const [authors, setAuthors] = useState<NamedItem[]>([]);
  const [publishers, setPublishers] = useState<NamedItem[]>([]);
  const [categories, setCategories] = useState<NamedItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const [a, p, c] = await Promise.all([
          api.get<ApiResponse<NamedItem[]>>('/authors', { params: { page: 0, size: 10 } }),
          api.get<ApiResponse<NamedItem[]>>('/publishers', { params: { page: 0, size: 10 } }),
          api.get<ApiResponse<NamedItem[]>>('/categories', { params: { page: 0, size: 10 } }),
        ]);
        setAuthors(a.data.data || []);
        setPublishers(p.data.data || []);
        setCategories(c.data.data || []);
      } catch {
        message.error('Không tải được danh mục');
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, []);

  const columns = [
    { title: 'Mã', dataIndex: 'code', width: 100, render: (v: string) => v || '—' },
    { title: 'Tên', dataIndex: 'name' },
  ];

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Thể loại / Tác giả / NXB
      </Typography.Title>
      <Typography.Paragraph type="secondary">
        Dữ liệu từ API catalog (B2) — GET public.
      </Typography.Paragraph>
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={8}>
          <Card title="Thể loại" loading={loading}>
            <Table rowKey="id" size="small" pagination={false} columns={columns} dataSource={categories} />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="Tác giả" loading={loading}>
            <Table
              rowKey="id"
              size="small"
              pagination={false}
              columns={[{ title: 'Tên', dataIndex: 'name' }]}
              dataSource={authors}
            />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="Nhà xuất bản" loading={loading}>
            <Table
              rowKey="id"
              size="small"
              pagination={false}
              columns={[{ title: 'Tên', dataIndex: 'name' }]}
              dataSource={publishers}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
