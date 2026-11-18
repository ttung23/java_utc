import { Link } from 'react-router-dom';
import { Alert, Button, Collapse, Tag } from 'antd';
import {
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  FileProtectOutlined,
  ReadOutlined,
} from '@ant-design/icons';

const SECTIONS = [
  {
    key: '1',
    label: '1. Đối tượng được mượn',
    children: (
      <ul style={{ margin: 0, paddingLeft: 18, color: '#475569', lineHeight: 1.7 }}>
        <li>Sinh viên, học viên, giảng viên, viên chức đang có thẻ thư viện UTC còn hiệu lực.</li>
        <li>Tài khoản phải ở trạng thái Active; thẻ bị khóa / hết hạn / blacklist không được mượn.</li>
        <li>Khách vãng lai chỉ được đọc tại chỗ theo quy định của quầy thủ thư.</li>
      </ul>
    ),
  },
  {
    key: '2',
    label: '2. Hạn mức & thời hạn mượn',
    children: (
      <ul style={{ margin: 0, paddingLeft: 18, color: '#475569', lineHeight: 1.7 }}>
        <li>Số lượng sách / số ngày mượn lấy theo <strong>loại độc giả</strong> và cấu hình hệ thống (không hard-code).</li>
        <li>Sách tham khảo / chỉ đọc tại chỗ không được mang ra ngoài.</li>
        <li>Có thể gia hạn online nếu sách chưa bị đặt trước và chưa quá hạn (theo chính sách hiện hành).</li>
      </ul>
    ),
  },
  {
    key: '3',
    label: '3. Mượn online & nhận sách tại quầy',
    children: (
      <ol style={{ margin: 0, paddingLeft: 18, color: '#475569', lineHeight: 1.7 }}>
        <li>Đăng nhập cổng độc giả → chọn sách → gửi yêu cầu mượn online.</li>
        <li>Nhân viên duyệt → giữ sách → độc giả đến quầy nhận trong thời hạn quy định.</li>
        <li>Quá hạn nhận mà không đến: yêu cầu bị hủy, có thể tính lần không đến nhận (no-show).</li>
      </ol>
    ),
  },
  {
    key: '4',
    label: '4. Đặt trước (Hold)',
    children: (
      <ul style={{ margin: 0, paddingLeft: 18, color: '#475569', lineHeight: 1.7 }}>
        <li>Khi sách tạm hết bản vật lý, độc giả có thể đặt trước và vào hàng đợi.</li>
        <li>Khi đến lượt (READY), hệ thống thông báo; cần nhận trong thời gian giữ chỗ.</li>
        <li>Không nhận đúng hạn: hệ thống chuyển cho người tiếp theo trong hàng đợi.</li>
      </ul>
    ),
  },
  {
    key: '5',
    label: '5. Trả sách & quá hạn',
    children: (
      <ul style={{ margin: 0, paddingLeft: 18, color: '#475569', lineHeight: 1.7 }}>
        <li>Trả tại quầy đúng hạn ghi trên phiếu / ứng dụng.</li>
        <li>Quá hạn: phát sinh phí phạt theo ngày và mức cấu hình trong hệ thống.</li>
        <li>Nợ phạt chưa thanh toán có thể bị khóa quyền mượn mới.</li>
      </ul>
    ),
  },
  {
    key: '6',
    label: '6. Phạt hư hỏng / mất sách',
    children: (
      <ul style={{ margin: 0, paddingLeft: 18, color: '#475569', lineHeight: 1.7 }}>
        <li>Làm hỏng, viết/vẽ lên sách, làm mất: bồi thường theo quy định thư viện (giá sách / phí xử lý).</li>
        <li>Phạt được ghi nhận trên mục <Link to="/me/fines">Phạt của tôi</Link>; nộp tại quầy hoặc gửi yêu cầu thanh toán.</li>
      </ul>
    ),
  },
  {
    key: '7',
    label: '7. Giờ mở cửa',
    children: (
      <p style={{ margin: 0, color: '#475569', lineHeight: 1.7 }}>
        <ClockCircleOutlined /> Cơ sở Hà Nội &amp; TP.HCM: <strong>07:30 – 20:30</strong> (Thứ 2 – Thứ 7). Chủ nhật và ngày lễ theo thông báo trên mục Tin tức.
      </p>
    ),
  },
];

export function RulesPage() {
  return (
    <div>
      <div className="home-section-head">
        <div>
          <div className="eyebrow">NỘI QUY THƯ VIỆN UTC</div>
          <h2 style={{ margin: 0, fontSize: 28, color: '#0b1f3a' }}>Quy định mượn trả</h2>
          <p className="desc">
            Tóm tắt quyền và nghĩa vụ độc giả khi mượn, trả, đặt trước và xử lý phạt tại Thư viện số —
            Đại học Giao thông vận tải.
          </p>
        </div>
        <Tag icon={<FileProtectOutlined />} color="blue">
          Áp dụng toàn hệ thống
        </Tag>
      </div>

      <Alert
        type="info"
        showIcon
        icon={<ExclamationCircleOutlined />}
        style={{ marginBottom: 16, borderRadius: 12 }}
        message="Mức phạt, số ngày mượn, hạn mức lấy từ cấu hình hệ thống / loại thẻ — vui lòng xem trên tài khoản sau khi đăng nhập."
      />

      <div className="home-panel">
        <Collapse
          defaultActiveKey={['1', '2', '3']}
          items={SECTIONS}
          bordered={false}
          style={{ background: 'transparent' }}
        />
      </div>

      <div
        style={{
          marginTop: 16,
          display: 'flex',
          flexWrap: 'wrap',
          gap: 10,
        }}
      >
        <Link to="/books">
          <Button type="primary" icon={<ReadOutlined />} style={{ background: '#0B1F3A' }}>
            Bắt đầu tra cứu sách
          </Button>
        </Link>
        <Link to="/about">
          <Button>Giới thiệu thư viện</Button>
        </Link>
        <Link to="/news">
          <Button>Xem thông báo mới</Button>
        </Link>
      </div>
    </div>
  );
}
