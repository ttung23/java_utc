import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  Progress,
  Row,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd';
import {
  BookOutlined,
  ClockCircleOutlined,
  DollarOutlined,
  ReadOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { Link } from 'react-router-dom';
import dayjs from 'dayjs';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { tokens, statusColorMap } from '@/styles/tokens';
import { useAuth } from '@/auth/AuthContext';
import { api, type ApiResponse } from '@/services/api';

type UrgentTask = {
  type: string;
  priority: string;
  title: string;
  description?: string;
  href: string;
  count?: number | string;
};

type Dashboard = {
  totalBooks?: number;
  totalCopies?: number;
  availableCopies?: number;
  borrowingItems?: number;
  overdueItems?: number;
  pendingOnline?: number;
  totalReaders?: number;
  unpaidFines?: number;
  borrowedToday?: number;
  returnedToday?: number;
  borrowedTodayCounter?: number | null;
  borrowedTodayOnline?: number | null;
  returnOnTimeRate?: number | null;
  demoDailyStats?: boolean;
  urgentTasks?: UrgentTask[];
  asOf?: string;
};

type OverdueRow = {
  itemId: number;
  slipCode?: string;
  readerName?: string;
  bookTitle?: string;
  barcode?: string;
  dueDate?: string;
  overdueDays?: number;
};

type OnlineSlip = {
  id: number;
  slipCode: string;
  readerName?: string;
  status: string;
  requestedAt?: string;
  items?: Array<{ bookTitle?: string }>;
};

type Reservation = {
  id: number;
  readerName?: string;
  bookTitle?: string;
  status: string;
  barcode?: string;
  readyUntil?: string;
  queuePosition?: number;
};

type TopBook = {
  id: number;
  title: string;
  borrowCount: number;
};

type Inventory = {
  byStatus?: Record<string, number>;
  total?: number;
};

type TrendPoint = { date: string; borrowed: number; returned: number };

const fmt = (n?: number | string | null) =>
  n == null || n === '' ? '—' : Number(n).toLocaleString('vi-VN');

const PIE_COLORS: Record<string, string> = {
  AVAILABLE: tokens.color.available,
  BORROWED: tokens.color.borrowing,
  RESERVED: tokens.color.pending,
  LOST: tokens.color.overdue,
  DAMAGED: '#fa8c16',
  WITHDRAWN: tokens.color.closed,
  IN_REPAIR: '#13c2c2',
};

const STATUS_VI: Record<string, string> = {
  AVAILABLE: 'Sẵn sàng',
  BORROWED: 'Đang mượn',
  RESERVED: 'Đặt trước',
  LOST: 'Mất',
  DAMAGED: 'Hư hỏng',
  WITHDRAWN: 'Đã thanh lý',
  IN_REPAIR: 'Đang sửa',
};

const PRIORITY_VI: Record<string, string> = {
  HIGH: 'Khẩn cấp',
  MEDIUM: 'Trung bình',
  LOW: 'Thấp',
};

export function DashboardPage() {
  const { user } = useAuth();
  const [kpi, setKpi] = useState<Dashboard>({});
  const [inventory, setInventory] = useState<Inventory>({});
  const [overdue, setOverdue] = useState<OverdueRow[]>([]);
  const [online, setOnline] = useState<OnlineSlip[]>([]);
  const [readyRes, setReadyRes] = useState<Reservation[]>([]);
  const [topBooks, setTopBooks] = useState<TopBook[]>([]);
  const [trend, setTrend] = useState<TrendPoint[]>([]);
  const [trendDemo, setTrendDemo] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const [dash, inv, od, onl, res, top, tr] = await Promise.all([
          api.get<ApiResponse<Dashboard>>('/reports/dashboard'),
          api.get<ApiResponse<Inventory>>('/reports/inventory'),
          api.get<ApiResponse<{ items: OverdueRow[] }>>('/reports/overdue'),
          api.get<ApiResponse<OnlineSlip[]>>('/borrows', {
            params: { channel: 'ONLINE', status: 'PENDING', page: 0, size: 10 },
          }),
          api.get<ApiResponse<Reservation[]>>('/reservations', { params: { page: 0, size: 50 } }),
          api.get<ApiResponse<{ items: TopBook[] }>>('/reports/top-books', { params: { limit: 8 } }),
          api.get<ApiResponse<{ points: TrendPoint[]; demo?: boolean }>>('/reports/borrow-trend', {
            params: { days: 15 },
          }),
        ]);
        setKpi(dash.data.data || {});
        setInventory(inv.data.data || {});
        setOverdue(od.data.data?.items || []);
        setOnline(onl.data.data || []);
        setReadyRes((res.data.data || []).filter((r) => r.status === 'READY').slice(0, 10));
        setTopBooks(top.data.data?.items || []);
        setTrend(tr.data.data?.points || []);
        setTrendDemo(Boolean(tr.data.data?.demo));
      } catch {
        message.error('Không tải được dashboard');
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, []);

  const totalCopies = Number(kpi.totalCopies || 0);
  const available = Number(kpi.availableCopies || 0);
  const borrowing = Number(kpi.borrowingItems || 0);
  const availPct = totalCopies > 0 ? Math.round((available / totalCopies) * 100) : 0;

  const urgentTasks = useMemo(() => {
    const fromApi = [...(kpi.urgentTasks || [])];
    if (readyRes.length > 0 && !fromApi.some((t) => t.type === 'READY_HOLD')) {
      fromApi.push({
        type: 'READY_HOLD',
        priority: 'MEDIUM',
        title: `${readyRes.length} đặt trước sẵn sàng đang giữ`,
        description: 'Cần giao sách hoặc hết hạn giữ chỗ',
        href: '/reservations',
        count: readyRes.length,
      });
    }
    const order = { HIGH: 0, MEDIUM: 1, LOW: 2 } as Record<string, number>;
    return fromApi.sort((a, b) => (order[a.priority] ?? 9) - (order[b.priority] ?? 9));
  }, [kpi.urgentTasks, readyRes]);

  const pieData = useMemo(
    () =>
      Object.entries(inventory.byStatus || {}).map(([status, count]) => ({
        key: status,
        name: STATUS_VI[status] || status,
        value: Number(count),
      })),
    [inventory],
  );

  const barData = useMemo(
    () =>
      topBooks.map((b) => ({
        name: b.title.length > 22 ? `${b.title.slice(0, 22)}…` : b.title,
        fullTitle: b.title,
        borrowCount: b.borrowCount,
      })),
    [topBooks],
  );

  const trendChart = useMemo(
    () =>
      trend.map((p, i, arr) => {
        const isToday = i === arr.length - 1;
        const dm = dayjs(p.date).format('DD/MM');
        return {
          ...p,
          isToday,
          label: isToday ? `Hôm nay (${dm})` : dm,
        };
      }),
    [trend],
  );

  const kpiCards = [
    {
      key: 'stock',
      label: 'KHO & ĐẦU SÁCH',
      icon: <BookOutlined />,
      color: tokens.color.primary,
      main: (
        <>
          <Typography.Title level={3} style={{ margin: 0 }}>
            {fmt(kpi.totalBooks)}
          </Typography.Title>
          <Typography.Text type="secondary">đầu sách · {fmt(kpi.totalCopies)} bản sao</Typography.Text>
          <div style={{ marginTop: 10 }}>
            <Typography.Text style={{ fontSize: 12 }}>
              Sẵn sàng {fmt(available)} · Đang mượn {fmt(borrowing)}
            </Typography.Text>
            <Progress
              percent={availPct}
              showInfo={false}
              strokeColor={tokens.color.available}
              trailColor="#e5e7eb"
              size="small"
              style={{ marginTop: 4 }}
            />
          </div>
        </>
      ),
    },
    {
      key: 'borrow-today',
      label: 'MƯỢN TRONG NGÀY',
      icon: <ReadOutlined />,
      color: tokens.color.borrowing,
      main: (
        <>
          <Typography.Title level={3} style={{ margin: 0 }}>
            {fmt(kpi.borrowedToday)}
          </Typography.Title>
          <Typography.Text type="secondary">
            Quầy {fmt(kpi.borrowedTodayCounter)} · Trực tuyến {fmt(kpi.borrowedTodayOnline)}
          </Typography.Text>
        </>
      ),
    },
    {
      key: 'return-today',
      label: 'TRẢ SÁCH HÔM NAY',
      icon: <CheckCircleOutlined />,
      color: tokens.color.success,
      main: (
        <>
          <Typography.Title level={3} style={{ margin: 0 }}>
            {fmt(kpi.returnedToday)}
          </Typography.Title>
          <Typography.Text type="secondary">
            {kpi.returnOnTimeRate != null
              ? `Đúng hạn ${Number(kpi.returnOnTimeRate).toFixed(1)}%`
              : 'lượt trả trong ngày'}
          </Typography.Text>
        </>
      ),
    },
    {
      key: 'overdue',
      label: 'SÁCH QUÁ HẠN',
      icon: <WarningOutlined />,
      color: tokens.color.overdue,
      main: (
        <>
          <Typography.Title level={3} style={{ margin: 0, color: tokens.color.overdue }}>
            {fmt(kpi.overdueItems)}
          </Typography.Title>
          <Typography.Text type="secondary">bản sách đang quá hạn</Typography.Text>
          <div style={{ marginTop: 8 }}>
            <Link to="/borrows/overdue">
              <Button size="small" danger>
                Xem ngay
              </Button>
            </Link>
          </div>
        </>
      ),
    },
    {
      key: 'pending',
      label: 'CHỜ DUYỆT TRỰC TUYẾN',
      icon: <ClockCircleOutlined />,
      color: tokens.color.pending,
      main: (
        <>
          <Typography.Title level={3} style={{ margin: 0, color: tokens.color.pending }}>
            {fmt(kpi.pendingOnline)}
          </Typography.Title>
          <Typography.Text type="secondary">yêu cầu đang chờ duyệt</Typography.Text>
          <div style={{ marginTop: 8 }}>
            <Link to="/borrows/online">
              <Button size="small" type="primary">
                Xử lý ngay
              </Button>
            </Link>
          </div>
        </>
      ),
    },
    {
      key: 'fines',
      label: 'PHẠT CHƯA THU',
      icon: <DollarOutlined />,
      color: '#722ed1',
      main: (
        <>
          <Typography.Title level={3} style={{ margin: 0 }}>
            {fmt(kpi.unpaidFines)}
          </Typography.Title>
          <Typography.Text type="secondary">đồng (tổng chưa thu)</Typography.Text>
          <div style={{ marginTop: 8 }}>
            <Link to="/fines">
              <Button size="small">Xem phạt</Button>
            </Link>
          </div>
        </>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Typography.Title level={3} style={{ marginTop: 0, marginBottom: 4 }}>
          Bảng điều khiển
        </Typography.Title>
        <Typography.Text type="secondary">
          Xin chào, {user?.fullName}
          {kpi.asOf ? ` · Cập nhật ${dayjs(kpi.asOf).format('DD/MM/YYYY')}` : ''}
          {kpi.totalReaders != null ? ` · ${fmt(kpi.totalReaders)} độc giả` : ''}
        </Typography.Text>
      </div>

      <Card
        loading={loading}
        size="small"
        style={{
          marginBottom: 16,
          borderColor: urgentTasks.length ? '#ffccc7' : undefined,
          background: urgentTasks.length ? '#fff7f6' : undefined,
        }}
        title={
          <Space>
            <ThunderboltOutlined style={{ color: tokens.color.overdue }} />
            <span>Tác vụ cần xử lý gấp</span>
            <Tag color={urgentTasks.length ? 'red' : 'green'}>{urgentTasks.length} việc</Tag>
          </Space>
        }
      >
        {urgentTasks.length === 0 ? (
          <Alert type="success" showIcon message="Không có tác vụ khẩn — hệ thống ổn định." />
        ) : (
          <Row gutter={[12, 12]}>
            {urgentTasks.map((t) => (
              <Col xs={24} md={12} xl={8} key={t.type}>
                <Card
                  size="small"
                  styles={{ body: { padding: 12 } }}
                  style={{
                    borderLeft: `4px solid ${t.priority === 'HIGH' ? tokens.color.overdue : tokens.color.pending}`,
                  }}
                >
                  <Space direction="vertical" size={4} style={{ width: '100%' }}>
                    <Space wrap>
                      <Tag color={t.priority === 'HIGH' ? 'red' : 'orange'}>
                        {PRIORITY_VI[t.priority] || t.priority}
                      </Tag>
                      <Typography.Text strong>{t.title}</Typography.Text>
                    </Space>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {t.description}
                    </Typography.Text>
                    <Link to={t.href}>
                      <Button size="small" type={t.priority === 'HIGH' ? 'primary' : 'default'} danger={t.priority === 'HIGH'}>
                        Xử lý ngay
                      </Button>
                    </Link>
                  </Space>
                </Card>
              </Col>
            ))}
          </Row>
        )}
      </Card>

      <Row gutter={[12, 12]}>
        {kpiCards.map((c) => (
          <Col xs={24} sm={12} lg={8} xl={4} key={c.key}>
            <Card
              loading={loading}
              size="small"
              styles={{ body: { minHeight: 140 } }}
              style={{ borderTop: `3px solid ${c.color}` }}
            >
              <Space style={{ marginBottom: 8 }}>
                <span style={{ color: c.color, fontSize: 16 }}>{c.icon}</span>
                <Typography.Text type="secondary" style={{ fontSize: 11, fontWeight: 600 }}>
                  {c.label}
                </Typography.Text>
              </Space>
              {c.main}
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} xl={14}>
          <Card
            loading={loading}
            title="Xu hướng mượn / trả 15 ngày"
            extra={
              trendDemo ? (
                <Tag color="blue">Dữ liệu minh họa</Tag>
              ) : (
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  Dữ liệu thực
                </Typography.Text>
              )
            }
          >
            <div style={{ width: '100%', height: 300 }}>
              <ResponsiveContainer>
                <BarChart
                  data={trendChart}
                  margin={{ top: 12, right: 12, left: 0, bottom: 8 }}
                  barCategoryGap="28%"
                  barGap={3}
                >
                  <defs>
                    <linearGradient id="borrowGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#3B82F6" stopOpacity={0.95} />
                      <stop offset="100%" stopColor="#3B82F6" stopOpacity={0.12} />
                    </linearGradient>
                    <linearGradient id="returnGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#93C5FD" stopOpacity={0.95} />
                      <stop offset="100%" stopColor="#93C5FD" stopOpacity={0.1} />
                    </linearGradient>
                    <linearGradient id="borrowTodayGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#1E3A8A" stopOpacity={1} />
                      <stop offset="100%" stopColor="#1E3A8A" stopOpacity={0.22} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="4 4" stroke="#E5E7EB" vertical={false} />
                  <XAxis
                    dataKey="label"
                    axisLine={false}
                    tickLine={false}
                    interval="preserveStartEnd"
                    minTickGap={36}
                    tick={({ x, y, payload }) => {
                      const isToday = String(payload.value).startsWith('Hôm nay');
                      return (
                        <text
                          x={x}
                          y={y + 12}
                          textAnchor="middle"
                          fill={isToday ? '#1E3A8A' : '#9CA3AF'}
                          fontSize={isToday ? 11 : 11}
                          fontWeight={isToday ? 600 : 400}
                        >
                          {payload.value}
                        </text>
                      );
                    }}
                  />
                  <YAxis hide allowDecimals={false} />
                  <Tooltip
                    cursor={{ fill: 'rgba(59,130,246,0.06)' }}
                    contentStyle={{ borderRadius: 8, borderColor: '#E5E7EB', fontSize: 12 }}
                    labelFormatter={(_, payload) =>
                      payload?.[0]?.payload?.date
                        ? dayjs(payload[0].payload.date).format('DD/MM/YYYY')
                        : ''
                    }
                    formatter={(v: number, name: string) => [fmt(v), name]}
                  />
                  <Legend
                    verticalAlign="top"
                    align="right"
                    iconType="circle"
                    wrapperStyle={{ fontSize: 12, paddingBottom: 8 }}
                  />
                  <Bar
                    dataKey="borrowed"
                    name="Mượn"
                    radius={[6, 6, 0, 0]}
                    maxBarSize={14}
                  >
                    {trendChart.map((d) => (
                      <Cell
                        key={`b-${d.date}`}
                        fill={d.isToday ? 'url(#borrowTodayGrad)' : 'url(#borrowGrad)'}
                      />
                    ))}
                  </Bar>
                  <Bar
                    dataKey="returned"
                    name="Trả"
                    fill="url(#returnGrad)"
                    radius={[6, 6, 0, 0]}
                    maxBarSize={14}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </Card>
        </Col>
        <Col xs={24} md={12} xl={5}>
          <Card loading={loading} title="Tồn kho theo trạng thái">
            <div style={{ width: '100%', height: 280 }}>
              <ResponsiveContainer>
                <PieChart>
                  <Pie
                    data={pieData}
                    dataKey="value"
                    nameKey="name"
                    innerRadius={55}
                    outerRadius={90}
                    paddingAngle={2}
                  >
                    {pieData.map((d) => (
                      <Cell
                        key={d.key}
                        fill={PIE_COLORS[d.key] || statusColorMap[d.key] || '#94a3b8'}
                      />
                    ))}
                  </Pie>
                  <Tooltip formatter={(v: number) => fmt(v)} />
                  <Legend wrapperStyle={{ fontSize: 11 }} />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </Card>
        </Col>
        <Col xs={24} md={12} xl={5}>
          <Card
            loading={loading}
            title="Sách mượn nhiều nhất"
            extra={
              <Link to="/reports">
                <Button type="link" size="small">
                  Xem thêm
                </Button>
              </Link>
            }
          >
            <div style={{ width: '100%', height: 280 }}>
              <ResponsiveContainer>
                <BarChart
                  data={barData}
                  layout="vertical"
                  margin={{ top: 4, right: 12, left: 4, bottom: 4 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" horizontal={false} />
                  <XAxis type="number" allowDecimals={false} tick={{ fontSize: 11 }} />
                  <YAxis type="category" dataKey="name" width={96} tick={{ fontSize: 10 }} />
                  <Tooltip
                    formatter={(v: number) => [fmt(v), 'Lượt mượn']}
                    labelFormatter={(_, p) => p?.[0]?.payload?.fullTitle || ''}
                  />
                  <Bar dataKey="borrowCount" name="Lượt mượn" fill={tokens.color.primary} radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={14}>
          <Card
            title="Hàng đợi xử lý"
            loading={loading}
            extra={
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                Trực tuyến · Quá hạn · Đặt trước
              </Typography.Text>
            }
          >
            <Tabs
              items={[
                {
                  key: 'online',
                  label: `Yêu cầu trực tuyến (${online.length})`,
                  children: (
                    <Table
                      size="small"
                      rowKey="id"
                      pagination={false}
                      dataSource={online}
                      locale={{ emptyText: 'Không có yêu cầu đang chờ duyệt' }}
                      columns={[
                        { title: 'Mã phiếu', dataIndex: 'slipCode', width: 120 },
                        { title: 'Độc giả', dataIndex: 'readerName' },
                        {
                          title: 'Sách',
                          render: (_, r) => r.items?.[0]?.bookTitle || '—',
                          ellipsis: true,
                        },
                        {
                          title: 'Gửi lúc',
                          dataIndex: 'requestedAt',
                          width: 140,
                          render: (v?: string) => (v ? dayjs(v).format('DD/MM HH:mm') : '—'),
                        },
                        {
                          title: '',
                          width: 90,
                          render: () => (
                            <Link to="/borrows/online">
                              <Button size="small" type="primary">
                                Xử lý
                              </Button>
                            </Link>
                          ),
                        },
                      ]}
                    />
                  ),
                },
                {
                  key: 'overdue',
                  label: `Sách quá hạn (${overdue.length})`,
                  children: (
                    <Table
                      size="small"
                      rowKey="itemId"
                      pagination={false}
                      dataSource={overdue.slice(0, 10)}
                      locale={{ emptyText: 'Không có sách quá hạn' }}
                      columns={[
                        { title: 'Phiếu', dataIndex: 'slipCode', width: 120 },
                        { title: 'Độc giả', dataIndex: 'readerName' },
                        { title: 'Sách', dataIndex: 'bookTitle', ellipsis: true },
                        { title: 'Hạn trả', dataIndex: 'dueDate', width: 110 },
                        {
                          title: 'Số ngày',
                          dataIndex: 'overdueDays',
                          width: 80,
                          render: (v?: number) => <Tag color="red">{v ?? '—'}</Tag>,
                        },
                        {
                          title: '',
                          width: 80,
                          render: () => (
                            <Link to="/returns">
                              <Button size="small">Trả</Button>
                            </Link>
                          ),
                        },
                      ]}
                    />
                  ),
                },
                {
                  key: 'ready',
                  label: `Đặt trước sẵn sàng (${readyRes.length})`,
                  children: (
                    <Table
                      size="small"
                      rowKey="id"
                      pagination={false}
                      dataSource={readyRes}
                      locale={{ emptyText: 'Không có đặt trước sẵn sàng' }}
                      columns={[
                        { title: 'Độc giả', dataIndex: 'readerName' },
                        { title: 'Sách', dataIndex: 'bookTitle', ellipsis: true },
                        { title: 'Mã vạch', dataIndex: 'barcode', width: 130 },
                        {
                          title: 'Giữ đến',
                          dataIndex: 'readyUntil',
                          width: 140,
                          render: (v?: string) => (v ? dayjs(v).format('DD/MM HH:mm') : '—'),
                        },
                        {
                          title: '',
                          width: 80,
                          render: () => (
                            <Link to="/reservations">
                              <Button size="small">Xem</Button>
                            </Link>
                          ),
                        },
                      ]}
                    />
                  ),
                },
              ]}
            />
          </Card>
        </Col>
        <Col xs={24} lg={10}>
          <Card title="Đầu sách được mượn nhiều" loading={loading}>
            <Table
              size="small"
              rowKey="id"
              pagination={false}
              dataSource={topBooks}
              locale={{ emptyText: 'Chưa có lượt mượn' }}
              columns={[
                {
                  title: 'Hạng',
                  width: 56,
                  render: (_: unknown, __: TopBook, i: number) => i + 1,
                },
                {
                  title: 'Sách',
                  dataIndex: 'title',
                  render: (t: string) => <Link to="/books">{t}</Link>,
                },
                {
                  title: 'Lượt mượn',
                  dataIndex: 'borrowCount',
                  width: 100,
                  render: (v: number) => <Tag color="blue">{v}</Tag>,
                },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
