import { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { api, type ApiResponse } from '@/services/api';
import { useAuth } from '@/auth/AuthContext';

type News = {
  id: number;
  title: string;
  slug?: string;
  content?: string;
  coverUrl?: string;
  pinned?: boolean;
  status?: string;
  publishedAt?: string;
};

type Meta = { page: number; limit: number; total: number };

export function NewsPage() {
  const { user } = useAuth();
  const [rows, setRows] = useState<News[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 20, total: 0 });
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<News | null>(null);
  const [form] = Form.useForm();

  const load = async (page = 0) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<News[]>>('/news', {
        params: { page, size: meta.limit },
      });
      setRows(data.data || []);
      const m = data.meta as Meta | undefined;
      setMeta({
        page: m?.page ?? page,
        limit: m?.limit ?? 20,
        total: m?.total ?? data.data?.length ?? 0,
      });
    } catch {
      message.error('Không tải được tin tức');
    } finally {
      setLoading(false);
    }
  };

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ status: 'DRAFT', pinned: false });
    setOpen(true);
  };

  const openEdit = (row: News) => {
    setEditing(row);
    form.setFieldsValue(row);
    setOpen(true);
  };

  const save = async () => {
    const values = await form.validateFields();
    try {
      if (editing) {
        await api.put(`/news/${editing.id}`, values);
        message.success('Đã cập nhật');
      } else {
        await api.post('/news', values);
        message.success('Đã tạo');
      }
      setOpen(false);
      void load(editing ? meta.page : 0);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Lưu thất bại');
    }
  };

  const remove = async (id: number) => {
    try {
      await api.delete(`/news/${id}`);
      message.success('Đã xóa');
      void load(meta.page);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Xóa thất bại');
    }
  };

  useEffect(() => {
    void load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Tin tức
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => void load(meta.page)} />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            Viết tin
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
            onChange: (p) => void load(p - 1),
          }}
          columns={[
            { title: 'Tiêu đề', dataIndex: 'title' },
            { title: 'Slug', dataIndex: 'slug', width: 180 },
            {
              title: 'TT',
              dataIndex: 'status',
              width: 110,
              render: (v: string) => <Tag>{v}</Tag>,
            },
            {
              title: 'Ghim',
              dataIndex: 'pinned',
              width: 80,
              render: (v: boolean) => (v ? 'Có' : '—'),
            },
            {
              title: '',
              width: 160,
              render: (_, r) => (
                <Space>
                  <Button size="small" onClick={() => openEdit(r)}>
                    Sửa
                  </Button>
                  {user?.role === 'ADMIN' ? (
                    <Button size="small" danger onClick={() => void remove(r.id)}>
                      Xóa
                    </Button>
                  ) : null}
                </Space>
              ),
            },
          ]}
        />
      </Card>

      <Modal
        title={editing ? 'Sửa tin' : 'Viết tin'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => void save()}
        width={720}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="Tiêu đề" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="content" label="Nội dung" rules={[{ required: true }]}>
            <Input.TextArea rows={8} />
          </Form.Item>
          <Form.Item name="coverUrl" label="Cover URL">
            <Input />
          </Form.Item>
          <Form.Item name="status" label="Trạng thái" rules={[{ required: true }]}>
            <Select
              options={['DRAFT', 'PUBLISHED', 'HIDDEN'].map((v) => ({ value: v, label: v }))}
            />
          </Form.Item>
          <Form.Item name="pinned" label="Ghim" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
