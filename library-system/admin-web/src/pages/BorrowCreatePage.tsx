import { useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Space,
  Table,
  Typography,
  message,
} from 'antd';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { api, type ApiResponse } from '@/services/api';

type ReaderLookup = {
  id: number;
  readerCode: string;
  fullName: string;
  cardNumber?: string;
  cardStatus?: string;
};

type CopyLookup = {
  id: number;
  barcode: string;
  bookTitle?: string;
  status: string;
};

type Line = { key: string; copyId: number; barcode: string; bookTitle?: string };

export function BorrowCreatePage() {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [reader, setReader] = useState<ReaderLookup | null>(null);
  const [lines, setLines] = useState<Line[]>([]);
  const [barcode, setBarcode] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const lookupReader = async () => {
    const card = (form.getFieldValue('card') as string | undefined)?.trim();
    if (!card) {
      message.warning('Nhập mã thẻ / barcode độc giả');
      return;
    }
    try {
      const { data } = await api.get<ApiResponse<ReaderLookup>>('/readers/lookup', {
        params: { card },
      });
      setReader(data.data);
      message.success(`Độc giả: ${data.data.fullName}`);
    } catch {
      setReader(null);
      message.error('Không tìm thấy độc giả');
    }
  };

  const addCopy = async () => {
    const code = barcode.trim();
    if (!code) return;
    if (lines.some((l) => l.barcode === code)) {
      message.warning('Barcode đã có trong phiếu');
      return;
    }
    try {
      const { data } = await api.get<ApiResponse<CopyLookup>>('/copies/lookup', {
        params: { barcode: code },
      });
      const c = data.data;
      if (c.status !== 'AVAILABLE') {
        message.error(`Bản sao ${c.barcode} không AVAILABLE (${c.status})`);
        return;
      }
      setLines((prev) => [
        ...prev,
        { key: String(c.id), copyId: c.id, barcode: c.barcode, bookTitle: c.bookTitle },
      ]);
      setBarcode('');
    } catch {
      message.error('Không tìm thấy barcode');
    }
  };

  const submit = async () => {
    if (!reader) {
      message.warning('Chọn độc giả trước');
      return;
    }
    if (!lines.length) {
      message.warning('Thêm ít nhất một bản sao');
      return;
    }
    setSubmitting(true);
    try {
      const note = form.getFieldValue('note') as string | undefined;
      const deposit = form.getFieldValue('deposit') as number | undefined;
      const { data } = await api.post<ApiResponse<{ id: number; slipCode: string }>>(
        '/borrows/counter',
        {
          readerId: reader.id,
          items: lines.map((l) => ({ copyId: l.copyId })),
          note,
          deposit,
        },
      );
      message.success(`Đã tạo phiếu ${data.data.slipCode}`);
      navigate('/borrows');
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Tạo phiếu thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Tạo phiếu mượn (quầy)
      </Typography.Title>
      <Card title="Độc giả" style={{ marginBottom: 16 }}>
        <Form form={form} layout="inline">
          <Form.Item name="card">
            <Input
              style={{ width: 280 }}
              placeholder="Mã thẻ / barcode"
              onPressEnter={() => void lookupReader()}
            />
          </Form.Item>
          <Button type="primary" onClick={() => void lookupReader()}>
            Tra cứu
          </Button>
        </Form>
        {reader && (
          <Typography.Paragraph style={{ marginTop: 12, marginBottom: 0 }}>
            <strong>{reader.fullName}</strong> · {reader.readerCode}
            {reader.cardNumber ? ` · thẻ ${reader.cardNumber}` : ''}
            {reader.cardStatus ? ` · ${reader.cardStatus}` : ''}
          </Typography.Paragraph>
        )}
      </Card>

      <Card title="Bản sao" style={{ marginBottom: 16 }}>
        <Space style={{ marginBottom: 12 }} wrap>
          <Input
            style={{ width: 280 }}
            placeholder="Quét / nhập barcode"
            value={barcode}
            onChange={(e) => setBarcode(e.target.value)}
            onPressEnter={() => void addCopy()}
          />
          <Button icon={<PlusOutlined />} onClick={() => void addCopy()}>
            Thêm
          </Button>
        </Space>
        <Table
          rowKey="key"
          size="small"
          pagination={false}
          dataSource={lines}
          columns={[
            { title: 'Barcode', dataIndex: 'barcode' },
            { title: 'Sách', dataIndex: 'bookTitle' },
            {
              title: '',
              width: 60,
              render: (_, r) => (
                <Button
                  type="text"
                  danger
                  icon={<DeleteOutlined />}
                  onClick={() => setLines((prev) => prev.filter((l) => l.key !== r.key))}
                />
              ),
            },
          ]}
        />
      </Card>

      <Card>
        <Form form={form} layout="vertical" style={{ maxWidth: 420 }}>
          <Form.Item name="deposit" label="Đặt cọc (nếu có)">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="note" label="Ghi chú">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Button type="primary" loading={submitting} onClick={() => void submit()}>
            Tạo phiếu mượn
          </Button>
        </Form>
      </Card>
    </div>
  );
}
