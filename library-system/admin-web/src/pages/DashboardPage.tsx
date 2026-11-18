import { Card, Col, Row, Statistic, Typography } from 'antd';
import {
  BookOutlined,
  TeamOutlined,
  ReadOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import { tokens } from '@/styles/tokens';
import { useAuth } from '@/auth/AuthContext';

const kpis = [
  { title: 'Tổng đầu sách', value: '—', icon: <BookOutlined />, color: tokens.color.primary },
  { title: 'Đang mượn', value: '—', icon: <ReadOutlined />, color: tokens.color.borrowing },
  { title: 'Quá hạn', value: '—', icon: <WarningOutlined />, color: tokens.color.overdue },
  { title: 'Độc giả', value: '—', icon: <TeamOutlined />, color: tokens.color.success },
];

export function DashboardPage() {
  const { user } = useAuth();

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Dashboard
      </Typography.Title>
      <Typography.Paragraph type="secondary">
        Xin chào, {user?.fullName}. KPI sẽ lấy từ API báo cáo khi module report sẵn sàng.
      </Typography.Paragraph>
      <Row gutter={[16, 16]}>
        {kpis.map((item) => (
          <Col xs={24} sm={12} lg={6} key={item.title}>
            <Card>
              <Statistic
                title={item.title}
                value={item.value}
                prefix={<span style={{ color: item.color }}>{item.icon}</span>}
              />
            </Card>
          </Col>
        ))}
      </Row>
      <Card style={{ marginTop: 16 }} title="Việc cần xử lý">
        <Typography.Text type="secondary">
          Danh sách yêu cầu online / quá hạn / đặt trước sẽ hiển thị tại đây sau khi có API borrow.
        </Typography.Text>
      </Card>
    </div>
  );
}
