import { useEffect, useState } from 'react';
import { Button, Card, Form, Input, Space, Table, Typography, message } from 'antd';
import { ReloadOutlined, SaveOutlined } from '@ant-design/icons';
import { api, type ApiResponse } from '@/services/api';

type Setting = {
  key: string;
  value?: string | number | boolean;
  description?: string;
};

export function SettingsPage() {
  const [rows, setRows] = useState<Setting[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Setting[]>>('/settings');
      const list = data.data || [];
      setRows(list);
      const initial: Record<string, unknown> = {};
      list.forEach((s) => {
        initial[s.key] = typeof s.value === 'object' ? JSON.stringify(s.value) : s.value;
      });
      form.setFieldsValue(initial);
    } catch {
      message.error('Không tải được cấu hình');
    } finally {
      setLoading(false);
    }
  };

  const save = async () => {
    const values = form.getFieldsValue();
    setSaving(true);
    try {
      await api.put('/settings', values);
      message.success('Đã lưu cấu hình');
      void load();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Lưu thất bại');
    } finally {
      setSaving(false);
    }
  };

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Cấu hình hệ thống
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => void load()} loading={loading} />
          <Button type="primary" icon={<SaveOutlined />} loading={saving} onClick={() => void save()}>
            Lưu tất cả
          </Button>
        </Space>
        <Form form={form} layout="vertical">
          <Table
            rowKey="key"
            loading={loading}
            dataSource={rows}
            pagination={false}
            columns={[
              { title: 'Key', dataIndex: 'key', width: 220 },
              { title: 'Mô tả', dataIndex: 'description' },
              {
                title: 'Giá trị',
                dataIndex: 'key',
                width: 280,
                render: (key: string) => (
                  <Form.Item name={key} style={{ marginBottom: 0 }}>
                    <Input />
                  </Form.Item>
                ),
              },
            ]}
          />
        </Form>
      </Card>
    </div>
  );
}
