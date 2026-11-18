import { useState } from 'react';
import {
  Button,
  Card,
  Checkbox,
  Input,
  Select,
  Space,
  Table,
  Typography,
  message,
} from 'antd';
import { api, type ApiResponse } from '@/services/api';

type Preview = {
  itemId: number;
  slipId: number;
  slipCode: string;
  readerId: number;
  readerName: string;
  bookTitle: string;
  barcode: string;
  dueDate: string;
  overdueDays: number;
  estimatedFine: number;
  renewCount: number;
};

type Line = Preview & { condition: string; note?: string };

export function ReturnsPage() {
  const [barcode, setBarcode] = useState('');
  const [lines, setLines] = useState<Line[]>([]);
  const [collectPayment, setCollectPayment] = useState(true);
  const [loading, setLoading] = useState(false);

  const preview = async () => {
    const code = barcode.trim();
    if (!code) return;
    if (lines.some((l) => l.barcode === code)) {
      message.warning('Barcode đã có trong danh sách trả');
      return;
    }
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<Preview>>('/returns/preview', {
        params: { barcode: code },
      });
      const p = data.data;
      setLines((prev) => [...prev, { ...p, condition: 'NORMAL' }]);
      setBarcode('');
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Không preview được');
    } finally {
      setLoading(false);
    }
  };

  const confirm = async () => {
    if (!lines.length) {
      message.warning('Chưa có sách để trả');
      return;
    }
    setLoading(true);
    try {
      await api.post('/returns', {
        collectPayment,
        items: lines.map((l) => ({
          copyBarcode: l.barcode,
          condition: l.condition,
          note: l.note,
        })),
      });
      message.success('Trả sách thành công');
      setLines([]);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Trả sách thất bại');
    } finally {
      setLoading(false);
    }
  };

  const totalFine = lines.reduce((s, l) => s + Number(l.estimatedFine || 0), 0);

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Trả sách
      </Typography.Title>
      <Card>
        <Space style={{ marginBottom: 16 }} wrap>
          <Input
            style={{ width: 300 }}
            placeholder="Quét barcode bản sao"
            value={barcode}
            onChange={(e) => setBarcode(e.target.value)}
            onPressEnter={() => void preview()}
          />
          <Button type="primary" loading={loading} onClick={() => void preview()}>
            Preview
          </Button>
          <Checkbox checked={collectPayment} onChange={(e) => setCollectPayment(e.target.checked)}>
            Thu phạt ngay (nếu có)
          </Checkbox>
        </Space>
        <Table
          rowKey="itemId"
          loading={loading}
          dataSource={lines}
          pagination={false}
          columns={[
            { title: 'Phiếu', dataIndex: 'slipCode', width: 120 },
            { title: 'Độc giả', dataIndex: 'readerName' },
            { title: 'Sách', dataIndex: 'bookTitle' },
            { title: 'Barcode', dataIndex: 'barcode', width: 140 },
            { title: 'Hạn', dataIndex: 'dueDate', width: 110 },
            { title: 'Quá hạn (ngày)', dataIndex: 'overdueDays', width: 120 },
            {
              title: 'Phạt dự kiến',
              dataIndex: 'estimatedFine',
              width: 120,
              render: (v: number) => Number(v || 0).toLocaleString('vi-VN'),
            },
            {
              title: 'Tình trạng',
              dataIndex: 'condition',
              width: 160,
              render: (v: string, r) => (
                <Select
                  style={{ width: 140 }}
                  value={v}
                  options={[
                    { value: 'NORMAL', label: 'Bình thường' },
                    { value: 'DAMAGED_MINOR', label: 'Hỏng nhẹ' },
                    { value: 'DAMAGED_MAJOR', label: 'Hỏng nặng' },
                    { value: 'LOST', label: 'Mất' },
                  ]}
                  onChange={(val) =>
                    setLines((prev) =>
                      prev.map((l) => (l.itemId === r.itemId ? { ...l, condition: val } : l)),
                    )
                  }
                />
              ),
            },
            {
              title: '',
              width: 80,
              render: (_, r) => (
                <Button
                  type="link"
                  danger
                  onClick={() => setLines((prev) => prev.filter((l) => l.itemId !== r.itemId))}
                >
                  Bỏ
                </Button>
              ),
            },
          ]}
        />
        <Space style={{ marginTop: 16 }}>
          <Typography.Text>
            Tổng phạt dự kiến: <strong>{totalFine.toLocaleString('vi-VN')}</strong>
          </Typography.Text>
          <Button type="primary" disabled={!lines.length} loading={loading} onClick={() => void confirm()}>
            Xác nhận trả
          </Button>
        </Space>
      </Card>
    </div>
  );
}
