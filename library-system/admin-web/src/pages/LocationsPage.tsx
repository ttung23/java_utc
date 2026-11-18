import { useEffect, useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Modal, Space, Table, Typography, message } from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { api, type ApiResponse } from '@/services/api';

type Location = {
  id: number;
  code: string;
  zone?: string;
  floor?: string;
  row?: string;
  shelf?: string;
  slot?: string;
  capacity?: number;
  note?: string;
};

type Meta = { page: number; limit: number; total: number };

export function LocationsPage() {
  const [rows, setRows] = useState<Location[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 20, total: 0 });
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Location | null>(null);
  const [form] = Form.useForm();

  const load = async (page = 0) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Location[]>>('/locations', {
        params: { page, size: meta.limit, q: q || undefined },
      });
      setRows(data.data || []);
      const m = data.meta as Meta | undefined;
      setMeta({
        page: m?.page ?? page,
        limit: m?.limit ?? 20,
        total: m?.total ?? data.data?.length ?? 0,
      });
    } catch {
      message.error('Không tải được vị trí kệ');
    } finally {
      setLoading(false);
    }
  };

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    setOpen(true);
  };

  const openEdit = (row: Location) => {
    setEditing(row);
    form.setFieldsValue(row);
    setOpen(true);
  };

  const save = async () => {
    const values = await form.validateFields();
    try {
      if (editing) {
        await api.put(`/locations/${editing.id}`, values);
        message.success('Đã cập nhật');
      } else {
        await api.post('/locations', values);
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
      await api.delete(`/locations/${id}`);
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
        Vị trí kệ
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }} wrap>
          <Input
            allowClear
            style={{ width: 240 }}
            placeholder="Tìm mã / zone"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onPressEnter={() => void load(0)}
          />
          <Button type="primary" onClick={() => void load(0)}>
            Tìm
          </Button>
          <Button icon={<ReloadOutlined />} onClick={() => void load(meta.page)} />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            Thêm
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
            { title: 'Mã', dataIndex: 'code', width: 120 },
            { title: 'Khu', dataIndex: 'zone', width: 100 },
            { title: 'Tầng', dataIndex: 'floor', width: 80 },
            { title: 'Dãy', dataIndex: 'row', width: 80 },
            { title: 'Kệ', dataIndex: 'shelf', width: 80 },
            { title: 'Ô', dataIndex: 'slot', width: 80 },
            { title: 'Sức chứa', dataIndex: 'capacity', width: 90 },
            { title: 'Ghi chú', dataIndex: 'note', ellipsis: true },
            {
              title: '',
              width: 160,
              render: (_, r) => (
                <Space>
                  <Button size="small" onClick={() => openEdit(r)}>
                    Sửa
                  </Button>
                  <Button size="small" danger onClick={() => void remove(r.id)}>
                    Xóa
                  </Button>
                </Space>
              ),
            },
          ]}
        />
      </Card>

      <Modal
        title={editing ? 'Sửa vị trí' : 'Thêm vị trí'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => void save()}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="code" label="Mã" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="zone" label="Khu">
            <Input />
          </Form.Item>
          <Form.Item name="floor" label="Tầng">
            <Input />
          </Form.Item>
          <Form.Item name="row" label="Dãy">
            <Input />
          </Form.Item>
          <Form.Item name="shelf" label="Kệ">
            <Input />
          </Form.Item>
          <Form.Item name="slot" label="Ô">
            <Input />
          </Form.Item>
          <Form.Item name="capacity" label="Sức chứa">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="note" label="Ghi chú">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
