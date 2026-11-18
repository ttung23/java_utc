import { useEffect, useState } from 'react';
import { Button, Card, Input, Modal, Space, Table, Tag, Typography, message } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { api, type ApiResponse } from '@/services/api';
import { statusColorMap } from '@/styles/tokens';

type Slip = {
  id: number;
  slipCode: string;
  readerName?: string;
  status: string;
  requestedAt?: string;
  pickupDeadline?: string;
  items?: Array<{ id: number; bookTitle?: string; barcode?: string; status: string }>;
};

type Meta = { page: number; limit: number; total: number };

export function BorrowOnlinePage() {
  const [rows, setRows] = useState<Slip[]>([]);
  const [meta, setMeta] = useState<Meta>({ page: 0, limit: 20, total: 0 });
  const [loading, setLoading] = useState(false);

  const load = async (page = 0) => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Slip[]>>('/borrows', {
        params: { page, size: meta.limit, channel: 'ONLINE', status: 'PENDING' },
      });
      setRows(data.data || []);
      const m = data.meta as Meta | undefined;
      setMeta({
        page: m?.page ?? page,
        limit: m?.limit ?? 20,
        total: m?.total ?? data.data?.length ?? 0,
      });
    } catch {
      message.error('Không tải được yêu cầu online');
    } finally {
      setLoading(false);
    }
  };

  const approve = async (id: number) => {
    try {
      await api.post(`/borrows/${id}/approve`, {});
      message.success('Đã duyệt');
      void load(meta.page);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Duyệt thất bại');
    }
  };

  const reject = (id: number) => {
    let reason = '';
    Modal.confirm({
      title: 'Từ chối yêu cầu',
      content: (
        <Input.TextArea
          rows={3}
          placeholder="Lý do từ chối"
          onChange={(e) => {
            reason = e.target.value;
          }}
        />
      ),
      onOk: async () => {
        if (!reason.trim()) {
          message.warning('Nhập lý do');
          return Promise.reject();
        }
        await api.post(`/borrows/${id}/reject`, { reason: reason.trim() });
        message.success('Đã từ chối');
        void load(meta.page);
      },
    });
  };

  const handover = (slip: Slip) => {
    let barcodes = '';
    Modal.confirm({
      title: `Giao sách — ${slip.slipCode}`,
      content: (
        <Input.TextArea
          rows={3}
          placeholder="Barcode, cách nhau bởi dấu phẩy hoặc xuống dòng"
          onChange={(e) => {
            barcodes = e.target.value;
          }}
        />
      ),
      onOk: async () => {
        const scannedBarcodes = barcodes
          .split(/[\n,]+/)
          .map((s) => s.trim())
          .filter(Boolean);
        if (!scannedBarcodes.length) {
          message.warning('Nhập barcode');
          return Promise.reject();
        }
        await api.post(`/borrows/${slip.id}/handover`, { scannedBarcodes });
        message.success('Đã giao sách');
        void load(meta.page);
      },
    });
  };

  useEffect(() => {
    void load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Yêu cầu mượn online
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => void load(meta.page)}>
            Tải lại
          </Button>
          <Typography.Text type="secondary">
            Mặc định lọc PENDING · ONLINE. Phiếu APPROVED dùng nút Giao sách ở danh sách phiếu.
          </Typography.Text>
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
            { title: 'Mã phiếu', dataIndex: 'slipCode', width: 140 },
            { title: 'Độc giả', dataIndex: 'readerName' },
            {
              title: 'TT',
              dataIndex: 'status',
              width: 120,
              render: (v: string) => <Tag color={statusColorMap[v] || 'default'}>{v}</Tag>,
            },
            { title: 'Yêu cầu lúc', dataIndex: 'requestedAt', width: 180 },
            {
              title: 'Thao tác',
              width: 260,
              render: (_, r) => (
                <Space wrap>
                  <Button size="small" type="primary" onClick={() => void approve(r.id)}>
                    Duyệt
                  </Button>
                  <Button size="small" danger onClick={() => reject(r.id)}>
                    Từ chối
                  </Button>
                  <Button size="small" onClick={() => handover(r)}>
                    Giao sách
                  </Button>
                </Space>
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
}
