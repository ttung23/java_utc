import { useEffect, useMemo, useState, type CSSProperties } from 'react';
import {
  Button,
  Col,
  Drawer,
  Form,
  Image,
  Input,
  InputNumber,
  Row,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import {
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import type { UploadProps } from 'antd';
import { api, type ApiResponse } from '@/services/api';
import { tokens, statusColorMap } from '@/styles/tokens';
import { useAuth } from '@/auth/AuthContext';

type BookRow = {
  id: number;
  title: string;
  isbn13?: string;
  publisherName?: string;
  publishYear?: number;
  status: string;
  coverUrl?: string;
  totalCopies: number;
  availableCopies: number;
  avgRating?: number;
};

type BookDetail = {
  id: number;
  title: string;
  subtitle?: string;
  isbn13?: string;
  isbn10?: string;
  publisherId?: number;
  publisherName?: string;
  publishYear?: number;
  languageId?: number;
  languageName?: string;
  documentTypeId?: number;
  pages?: number;
  price?: number;
  description?: string;
  coverUrl?: string;
  classificationCode?: string;
  tags?: string[];
  reference?: boolean;
  loanable?: boolean;
  loanDaysOverride?: number | null;
  status?: string;
  authorIds?: number[];
  authorNames?: string[];
  categoryIds?: number[];
  categoryNames?: string[];
  copySummary?: { total?: number; available?: number; borrowed?: number };
};

type Opt = { id: number; name: string; code?: string };
type Meta = { page: number; limit: number; total: number };

const panel: CSSProperties = {
  background: '#fff',
  borderRadius: 12,
  border: `1px solid ${tokens.color.border}`,
  boxShadow: '0 1px 2px rgba(15,23,42,0.04)',
};

function coverSrc(url?: string) {
  if (!url) return undefined;
  if (url.startsWith('http')) return url;
  return url;
}

export function BooksPage() {
  const { user } = useAuth();
  const [rows, setRows] = useState<BookRow[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 20, total: 0 });
  const [q, setQ] = useState('');
  const [statusFilter, setStatusFilter] = useState<string | undefined>();
  const [loading, setLoading] = useState(false);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<BookDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();
  const [publishers, setPublishers] = useState<Opt[]>([]);
  const [languages, setLanguages] = useState<Opt[]>([]);
  const [categories, setCategories] = useState<Opt[]>([]);
  const [authors, setAuthors] = useState<Opt[]>([]);

  const loadLookups = async () => {
    try {
      const [p, l, c, a] = await Promise.all([
        api.get<ApiResponse<Opt[]>>('/publishers', { params: { page: 0, size: 200 } }),
        api.get<ApiResponse<Opt[]>>('/languages', { params: { page: 0, size: 100 } }),
        api.get<ApiResponse<Opt[]>>('/categories', { params: { page: 0, size: 200 } }),
        api.get<ApiResponse<Opt[]>>('/authors', { params: { page: 0, size: 200 } }),
      ]);
      setPublishers(p.data.data || []);
      setLanguages(l.data.data || []);
      setCategories(c.data.data || []);
      setAuthors(a.data.data || []);
    } catch {
      // optional
    }
  };

  const load = async (page = 0, keyword = q) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<BookRow[]>>('/books', {
        params: {
          q: keyword || undefined,
          status: statusFilter || undefined,
          page,
          size: meta.limit,
          sort: 'title,asc',
        },
      });
      setRows(data.data || []);
      const m = data.meta as Meta | undefined;
      setMeta({
        page: m?.page ?? page,
        limit: m?.limit ?? 20,
        total: m?.total ?? data.data?.length ?? 0,
      });
    } catch {
      message.error('Không tải được danh sách sách');
    } finally {
      setLoading(false);
    }
  };

  const loadDetail = async (id: number) => {
    setDetailLoading(true);
    try {
      const { data } = await api.get<ApiResponse<BookDetail>>(`/books/${id}`);
      setDetail(data.data);
      setSelectedId(id);
    } catch {
      message.error('Không tải được chi tiết sách');
      setDetail(null);
    } finally {
      setDetailLoading(false);
    }
  };

  useEffect(() => {
    void load(0);
    void loadLookups();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    void load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter]);

  const kpis = useMemo(() => {
    const active = rows.filter((r) => r.status === 'ACTIVE').length;
    const low = rows.filter((r) => r.availableCopies <= 1 && r.totalCopies > 0).length;
    const out = rows.filter((r) => r.availableCopies === 0 && r.totalCopies > 0).length;
    return { total: meta.total, active, low, out };
  }, [rows, meta.total]);

  const openCreate = () => {
    setEditingId(null);
    form.resetFields();
    form.setFieldsValue({
      loanable: true,
      reference: false,
      status: 'ACTIVE',
      initialCopies: 1,
    });
    setDrawerOpen(true);
  };

  const openEdit = async (id: number) => {
    try {
      const { data } = await api.get<ApiResponse<BookDetail>>(`/books/${id}`);
      const b = data.data;
      setEditingId(id);
      form.setFieldsValue({
        title: b.title,
        subtitle: b.subtitle,
        isbn13: b.isbn13,
        isbn10: b.isbn10,
        publisherId: b.publisherId,
        publishYear: b.publishYear,
        languageId: b.languageId,
        documentTypeId: b.documentTypeId,
        pages: b.pages,
        price: b.price,
        description: b.description,
        coverUrl: b.coverUrl,
        classificationCode: b.classificationCode,
        tags: b.tags,
        reference: b.reference ?? false,
        loanable: b.loanable ?? true,
        loanDaysOverride: b.loanDaysOverride,
        status: b.status || 'ACTIVE',
        authorIds: b.authorIds || [],
        categoryIds: b.categoryIds || [],
      });
      setDrawerOpen(true);
      setDetail(b);
      setSelectedId(id);
    } catch {
      message.error('Không mở được form sửa');
    }
  };

  const save = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      const body = {
        ...values,
        tags: Array.isArray(values.tags)
          ? values.tags
          : typeof values.tags === 'string'
            ? values.tags
                .split(',')
                .map((t: string) => t.trim())
                .filter(Boolean)
            : values.tags,
      };
      if (editingId) {
        await api.put(`/books/${editingId}`, body);
        message.success('Đã cập nhật đầu sách');
        await loadDetail(editingId);
      } else {
        const { data } = await api.post<ApiResponse<BookDetail>>('/books', body);
        message.success('Đã tạo đầu sách');
        if (data.data?.id) {
          await loadDetail(data.data.id);
        }
      }
      setDrawerOpen(false);
      void load(meta.page);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Lưu thất bại');
    } finally {
      setSaving(false);
    }
  };

  const uploadCover: UploadProps['customRequest'] = async (options) => {
    const id = editingId || selectedId;
    if (!id) {
      message.warning('Lưu sách trước rồi mới upload bìa');
      options.onError?.(new Error('no id'));
      return;
    }
    const file = options.file as File;
    const formData = new FormData();
    formData.append('file', file);
    try {
      const { data } = await api.post<ApiResponse<BookDetail>>(`/books/${id}/cover`, formData);
      message.success('Đã cập nhật ảnh bìa');
      form.setFieldValue('coverUrl', data.data?.coverUrl);
      setDetail(data.data);
      options.onSuccess?.(data);
      void load(meta.page);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } }; message?: string };
      message.error(err.response?.data?.message || 'Upload bìa thất bại');
      options.onError?.(e as Error);
    }
  };

  const removeBook = async (id: number) => {
    if (user?.role !== 'ADMIN') {
      message.warning('Chỉ ADMIN được ẩn/xóa sách');
      return;
    }
    try {
      await api.delete(`/books/${id}`);
      message.success('Đã ẩn/xóa sách');
      if (selectedId === id) {
        setSelectedId(null);
        setDetail(null);
      }
      void load(meta.page);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Xóa thất bại');
    }
  };

  return (
    <div style={{ maxWidth: 1480, margin: '0 auto' }}>
      <div
        style={{
          ...panel,
          padding: '12px 16px',
          marginBottom: 14,
          display: 'flex',
          justifyContent: 'space-between',
          gap: 12,
          flexWrap: 'wrap',
          alignItems: 'center',
        }}
      >
        <div>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            Quản lý sách &amp; kho · Đầu sách
          </Typography.Text>
          <Typography.Title level={4} style={{ margin: '2px 0 0' }}>
            Quản lý đầu sách (biên mục &amp; ảnh bìa)
          </Typography.Title>
        </div>
        <Space wrap>
          <Button icon={<ReloadOutlined />} onClick={() => void load(meta.page)}>
            Tải lại
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            Thêm đầu sách
          </Button>
        </Space>
      </div>

      <Row gutter={[12, 12]} style={{ marginBottom: 14 }}>
        {[
          { label: 'Tổng đầu sách', value: kpis.total, hint: 'theo bộ lọc' },
          { label: 'ACTIVE (trang)', value: kpis.active, hint: 'đang hiển thị' },
          { label: 'Sắp hết bản', value: kpis.low, hint: '≤ 1 bản sẵn' },
          { label: 'Hết bản sẵn', value: kpis.out, hint: '0 AVAILABLE' },
        ].map((k) => (
          <Col xs={12} md={6} key={k.label}>
            <div style={{ ...panel, padding: 14 }}>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                {k.label}
              </Typography.Text>
              <div style={{ fontSize: 26, fontWeight: 700, color: '#0f172a', lineHeight: 1.2 }}>
                {k.value}
              </div>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                {k.hint}
              </Typography.Text>
            </div>
          </Col>
        ))}
      </Row>

      <Row gutter={[14, 14]}>
        <Col xs={24} lg={15}>
          <div style={{ ...panel, padding: 14 }}>
            <Space style={{ marginBottom: 12 }} wrap>
              <Input
                allowClear
                style={{ width: 280 }}
                prefix={<SearchOutlined />}
                placeholder="Tìm tên / ISBN"
                value={q}
                onChange={(e) => setQ(e.target.value)}
                onPressEnter={() => void load(0)}
              />
              <Select
                allowClear
                placeholder="Trạng thái"
                style={{ width: 160 }}
                value={statusFilter}
                onChange={(v) => setStatusFilter(v)}
                options={[
                  { value: 'ACTIVE', label: 'ACTIVE' },
                  { value: 'HIDDEN', label: 'HIDDEN' },
                  { value: 'LOST', label: 'LOST' },
                ]}
              />
              <Button type="primary" onClick={() => void load(0)}>
                Tìm kiếm
              </Button>
            </Space>

            <Table
              rowKey="id"
              loading={loading}
              dataSource={rows}
              size="middle"
              rowSelection={{
                type: 'radio',
                selectedRowKeys: selectedId ? [selectedId] : [],
                onChange: (keys) => {
                  const id = Number(keys[0]);
                  if (id) void loadDetail(id);
                },
              }}
              onRow={(r) => ({
                onClick: () => void loadDetail(r.id),
                style: { cursor: 'pointer' },
              })}
              pagination={{
                current: meta.page + 1,
                pageSize: meta.limit,
                total: meta.total,
                showSizeChanger: false,
                onChange: (p) => void load(p - 1),
              }}
              columns={[
                {
                  title: 'Bìa',
                  width: 64,
                  render: (_, r) =>
                    r.coverUrl ? (
                      <Image
                        src={coverSrc(r.coverUrl)}
                        width={40}
                        height={52}
                        style={{ objectFit: 'cover', borderRadius: 6 }}
                        preview={false}
                      />
                    ) : (
                      <div
                        style={{
                          width: 40,
                          height: 52,
                          borderRadius: 6,
                          background: '#e2e8f0',
                        }}
                      />
                    ),
                },
                { title: 'Tên sách', dataIndex: 'title' },
                { title: 'ISBN', dataIndex: 'isbn13', width: 130, render: (v) => v || '—' },
                { title: 'NXB', dataIndex: 'publisherName', width: 140, render: (v) => v || '—' },
                { title: 'Năm', dataIndex: 'publishYear', width: 70, render: (v) => v || '—' },
                {
                  title: 'Tồn',
                  width: 90,
                  render: (_, r) => (
                    <span style={{ color: r.availableCopies === 0 ? '#dc2626' : '#16a34a', fontWeight: 600 }}>
                      {r.availableCopies}/{r.totalCopies}
                    </span>
                  ),
                },
                {
                  title: 'TT',
                  dataIndex: 'status',
                  width: 100,
                  render: (status: string) => (
                    <Tag color={statusColorMap[status] || 'default'}>{status}</Tag>
                  ),
                },
                {
                  title: '',
                  width: 90,
                  render: (_, r) => (
                    <Button
                      size="small"
                      icon={<EditOutlined />}
                      onClick={(e) => {
                        e.stopPropagation();
                        void openEdit(r.id);
                      }}
                    >
                      Sửa
                    </Button>
                  ),
                },
              ]}
            />
          </div>
        </Col>

        <Col xs={24} lg={9}>
          <div style={{ ...panel, padding: 14, minHeight: 420 }}>
            <Typography.Title level={5} style={{ marginTop: 0 }}>
              Chi tiết đầu sách
            </Typography.Title>
            {detailLoading ? (
              <Typography.Text type="secondary">Đang tải…</Typography.Text>
            ) : !detail ? (
              <Typography.Text type="secondary">Chọn một dòng để xem / sửa.</Typography.Text>
            ) : (
              <div>
                <div style={{ display: 'flex', gap: 12, marginBottom: 12 }}>
                  {detail.coverUrl ? (
                    <Image
                      src={coverSrc(detail.coverUrl)}
                      width={96}
                      height={128}
                      style={{ objectFit: 'cover', borderRadius: 8 }}
                    />
                  ) : (
                    <div
                      style={{
                        width: 96,
                        height: 128,
                        borderRadius: 8,
                        background: '#e2e8f0',
                        display: 'grid',
                        placeItems: 'center',
                        color: '#64748b',
                        fontSize: 12,
                        textAlign: 'center',
                        padding: 8,
                      }}
                    >
                      Chưa có ảnh bìa
                    </div>
                  )}
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <Typography.Title level={5} style={{ margin: '0 0 6px' }}>
                      {detail.title}
                    </Typography.Title>
                    {detail.subtitle ? (
                      <Typography.Paragraph type="secondary" style={{ marginBottom: 6 }}>
                        {detail.subtitle}
                      </Typography.Paragraph>
                    ) : null}
                    <Tag color={statusColorMap[detail.status || ''] || 'default'}>
                      {detail.status || '—'}
                    </Tag>
                    <div style={{ marginTop: 8, fontSize: 13, color: '#64748b' }}>
                      ISBN: {detail.isbn13 || detail.isbn10 || '—'}
                      <br />
                      NXB: {detail.publisherName || '—'}
                      {detail.publishYear ? ` · ${detail.publishYear}` : ''}
                      <br />
                      Tồn: {detail.copySummary?.available ?? '—'}/{detail.copySummary?.total ?? '—'}
                    </div>
                  </div>
                </div>

                <Typography.Paragraph
                  ellipsis={{ rows: 4, expandable: true, symbol: 'xem thêm' }}
                  style={{ color: '#475569' }}
                >
                  {detail.description || 'Chưa có mô tả.'}
                </Typography.Paragraph>

                <div style={{ marginBottom: 10, fontSize: 13 }}>
                  <strong>Tác giả:</strong> {(detail.authorNames || []).join(', ') || '—'}
                  <br />
                  <strong>Thể loại:</strong> {(detail.categoryNames || []).join(', ') || '—'}
                </div>

                <Space wrap>
                  <Button type="primary" icon={<EditOutlined />} onClick={() => void openEdit(detail.id)}>
                    Sửa thông tin / ảnh
                  </Button>
                  <Upload
                    accept="image/*"
                    showUploadList={false}
                    customRequest={uploadCover}
                  >
                    <Button icon={<UploadOutlined />}>Upload ảnh bìa</Button>
                  </Upload>
                  {user?.role === 'ADMIN' ? (
                    <Button danger onClick={() => void removeBook(detail.id)}>
                      Ẩn/xóa
                    </Button>
                  ) : null}
                </Space>
              </div>
            )}
          </div>
        </Col>
      </Row>

      <Drawer
        title={editingId ? `Sửa đầu sách #${editingId}` : 'Thêm đầu sách mới'}
        width={560}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        destroyOnClose
        extra={
          <Space>
            <Button onClick={() => setDrawerOpen(false)}>Hủy</Button>
            <Button type="primary" loading={saving} onClick={() => void save()}>
              Lưu
            </Button>
          </Space>
        }
      >
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="Tên sách" rules={[{ required: true, message: 'Nhập tên sách' }]}>
            <Input placeholder="VD: Clean Code" />
          </Form.Item>
          <Form.Item name="subtitle" label="Phụ đề">
            <Input />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="isbn13" label="ISBN-13">
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="isbn10" label="ISBN-10">
                <Input />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="publisherId" label="Nhà xuất bản">
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={publishers.map((p) => ({ value: p.id, label: p.name }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="publishYear" label="Năm XB">
                <InputNumber style={{ width: '100%' }} min={1000} max={2100} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="languageId" label="Ngôn ngữ">
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={languages.map((l) => ({ value: l.id, label: l.name }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="pages" label="Số trang">
                <InputNumber style={{ width: '100%' }} min={1} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="classificationCode" label="Mã phân loại / DDC">
            <Input placeholder="VD: 005.1" />
          </Form.Item>
          <Form.Item name="authorIds" label="Tác giả">
            <Select
              mode="multiple"
              allowClear
              showSearch
              optionFilterProp="label"
              options={authors.map((a) => ({ value: a.id, label: a.name }))}
            />
          </Form.Item>
          <Form.Item name="categoryIds" label="Thể loại">
            <Select
              mode="multiple"
              allowClear
              showSearch
              optionFilterProp="label"
              options={categories.map((c) => ({ value: c.id, label: c.name }))}
            />
          </Form.Item>
          <Form.Item name="description" label="Mô tả">
            <Input.TextArea rows={4} />
          </Form.Item>
          <Form.Item name="coverUrl" label="URL ảnh bìa (hoặc upload sau khi lưu)">
            <Input placeholder="/uploads/covers/..." />
          </Form.Item>
          {editingId ? (
            <Form.Item label="Upload ảnh bìa mới">
              <Upload accept="image/*" showUploadList={false} customRequest={uploadCover}>
                <Button icon={<UploadOutlined />}>Chọn file ảnh</Button>
              </Upload>
            </Form.Item>
          ) : (
            <Form.Item name="initialCopies" label="Số bản sao ban đầu">
              <InputNumber style={{ width: '100%' }} min={0} />
            </Form.Item>
          )}
          <Row gutter={12}>
            <Col span={8}>
              <Form.Item name="status" label="Trạng thái">
                <Select
                  options={[
                    { value: 'ACTIVE', label: 'ACTIVE' },
                    { value: 'HIDDEN', label: 'HIDDEN' },
                    { value: 'LOST', label: 'LOST' },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="loanable" label="Cho mượn" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="reference" label="Chỉ đọc tại chỗ" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="loanDaysOverride" label="Số ngày mượn (override, để trống = theo loại ĐG)">
            <InputNumber style={{ width: '100%' }} min={1} />
          </Form.Item>
          <Form.Item name="price" label="Giá (VND)">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="tags" label="Tags (phẩy hoặc chọn)">
            <Select mode="tags" placeholder="Agile, OOP, ..." />
          </Form.Item>
        </Form>
      </Drawer>
    </div>
  );
}
