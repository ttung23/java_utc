import { useEffect, useState } from 'react';
import { Alert, Button, Card, Col, Descriptions, Row, Space, Tag, Typography, message } from 'antd';
import { Link, useParams } from 'react-router-dom';
import { api, type ApiResponse } from '@/services/api';
import { statusColorMap } from '@/styles/tokens';

type ReaderDetail = {
  id: number;
  readerCode: string;
  fullName: string;
  email?: string;
  phone?: string;
  readerTypeCode?: string;
  readerTypeName?: string;
  dob?: string;
  gender?: string;
  address?: string;
  idNumber?: string;
  studentCode?: string;
  organization?: string;
  note?: string;
  blacklisted?: boolean;
  blacklistReason?: string;
  userStatus?: string;
  cardNumber?: string;
  cardStatus?: string;
  cardExpiresAt?: string;
  cardBarcode?: string;
};

type Eligibility = {
  eligible: boolean;
  violations: Array<{ code: string; message: string }>;
};

export function ReaderDetailPage() {
  const { id } = useParams();
  const [reader, setReader] = useState<ReaderDetail | null>(null);
  const [eligibility, setEligibility] = useState<Eligibility | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    const load = async () => {
      setLoading(true);
      try {
        const [r, e] = await Promise.all([
          api.get<ApiResponse<ReaderDetail>>(`/readers/${id}`),
          api.get<ApiResponse<Eligibility>>(`/readers/${id}/eligibility`),
        ]);
        setReader(r.data.data);
        setEligibility(e.data.data);
      } catch {
        message.error('Không tải được hồ sơ độc giả');
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [id]);

  return (
    <div>
      <Space style={{ marginBottom: 12 }}>
        <Button>
          <Link to="/readers">← Danh sách</Link>
        </Button>
      </Space>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Hồ sơ độc giả
      </Typography.Title>
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={16}>
          <Card loading={loading}>
            {reader && (
              <Descriptions column={2} bordered size="small">
                <Descriptions.Item label="Mã độc giả">{reader.readerCode}</Descriptions.Item>
                <Descriptions.Item label="Họ tên">{reader.fullName}</Descriptions.Item>
                <Descriptions.Item label="Email">{reader.email || '—'}</Descriptions.Item>
                <Descriptions.Item label="SĐT">{reader.phone || '—'}</Descriptions.Item>
                <Descriptions.Item label="Loại">
                  {reader.readerTypeName || reader.readerTypeCode || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="TT tài khoản">
                  <Tag color={statusColorMap[reader.userStatus || ''] || 'default'}>
                    {reader.userStatus || '—'}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="CCCD">{reader.idNumber || '—'}</Descriptions.Item>
                <Descriptions.Item label="MSSV">{reader.studentCode || '—'}</Descriptions.Item>
                <Descriptions.Item label="Địa chỉ" span={2}>
                  {reader.address || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="Thẻ">{reader.cardNumber || '—'}</Descriptions.Item>
                <Descriptions.Item label="Barcode">{reader.cardBarcode || '—'}</Descriptions.Item>
                <Descriptions.Item label="TT thẻ">
                  {reader.cardStatus ? (
                    <Tag color={statusColorMap[reader.cardStatus] || 'default'}>{reader.cardStatus}</Tag>
                  ) : (
                    '—'
                  )}
                </Descriptions.Item>
                <Descriptions.Item label="Hết hạn thẻ">
                  {reader.cardExpiresAt
                    ? new Date(reader.cardExpiresAt).toLocaleDateString('vi-VN')
                    : '—'}
                </Descriptions.Item>
              </Descriptions>
            )}
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="Điều kiện mượn (eligibility)" loading={loading}>
            {eligibility && (
              <>
                {eligibility.eligible ? (
                  <Alert type="success" showIcon message="Đủ điều kiện mượn (kiểm tra cơ bản)" />
                ) : (
                  <Alert type="error" showIcon message="Chưa đủ điều kiện" />
                )}
                <ul style={{ marginTop: 12, paddingLeft: 18 }}>
                  {(eligibility.violations || []).map((v) => (
                    <li key={v.code}>
                      <Typography.Text type="danger">
                        [{v.code}] {v.message}
                      </Typography.Text>
                    </li>
                  ))}
                  {eligibility.eligible && (eligibility.violations || []).length === 0 && (
                    <li>
                      <Typography.Text type="secondary">Không có vi phạm BR-01/02 cơ bản</Typography.Text>
                    </li>
                  )}
                </ul>
              </>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
}
