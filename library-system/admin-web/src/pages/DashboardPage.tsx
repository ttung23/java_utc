import { useEffect, useState } from 'react';
import { Card, Col, Row, Statistic, Typography, message } from 'antd';
import {
  BookOutlined,
  TeamOutlined,
  ReadOutlined,
  WarningOutlined,
  DollarOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons';
import { tokens } from '@/styles/tokens';
import { useAuth } from '@/auth/AuthContext';
import { api, type ApiResponse } from '@/services/api';

type Dashboard = {
  totalBooks?: number;
  totalCopies?: number;
  availableCopies?: number;
  borrowingItems?: number;
  overdueItems?: number;
  pendingOnline?: number;
  totalReaders?: number;
  unpaidFines?: number;
};

export function DashboardPage() {
  const { user } = useAuth();
  const [kpi, setKpi] = useState<Dashboard>({});
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const { data } = await api.get<ApiResponse<Dashboard>>('/reports/dashboard');
        setKpi(data.data || {});
      } catch {
        message.error('Không tải được dashboard');
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, []);

  const cards = [
    {
      title: 'Tổng đầu sách',
      value: kpi.totalBooks ?? '—',
      icon: <BookOutlined />,
      color: tokens.color.primary,
    },
    {
      title: 'Đang mượn',
      value: kpi.borrowingItems ?? '—',
      icon: <ReadOutlined />,
      color: tokens.color.borrowing,
    },
    {
      title: 'Quá hạn',
      value: kpi.overdueItems ?? '—',
      icon: <WarningOutlined />,
      color: tokens.color.overdue,
    },
    {
      title: 'Độc giả',
      value: kpi.totalReaders ?? '—',
      icon: <TeamOutlined />,
      color: tokens.color.success,
    },
    {
      title: 'Yêu cầu online',
      value: kpi.pendingOnline ?? '—',
      icon: <ClockCircleOutlined />,
      color: tokens.color.pending,
    },
    {
      title: 'Phạt chưa thu',
      value: kpi.unpaidFines != null ? Number(kpi.unpaidFines).toLocaleString('vi-VN') : '—',
      icon: <DollarOutlined />,
      color: tokens.color.danger,
    },
  ];

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Dashboard
      </Typography.Title>
      <Typography.Paragraph type="secondary">
        Xin chào, {user?.fullName}.
      </Typography.Paragraph>
      <Row gutter={[16, 16]}>
        {cards.map((item) => (
          <Col xs={24} sm={12} lg={8} xl={4} key={item.title}>
            <Card loading={loading}>
              <Statistic
                title={item.title}
                value={item.value}
                prefix={<span style={{ color: item.color }}>{item.icon}</span>}
              />
            </Card>
          </Col>
        ))}
      </Row>
      <Card style={{ marginTop: 16 }} title="Tóm tắt kho">
        <Typography.Text>
          Bản sao: <strong>{kpi.totalCopies ?? '—'}</strong> · Available:{' '}
          <strong>{kpi.availableCopies ?? '—'}</strong>
        </Typography.Text>
      </Card>
    </div>
  );
}
