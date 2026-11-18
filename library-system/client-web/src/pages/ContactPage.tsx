import { Link } from 'react-router-dom';
import { Button, Form, Input, message } from 'antd';
import { EnvironmentOutlined, MailOutlined, PhoneOutlined } from '@ant-design/icons';

export function ContactPage() {
  const [form] = Form.useForm();

  return (
    <div>
      <div className="home-section-head">
        <div>
          <div className="eyebrow">HỖ TRỢ ĐỘC GIẢ</div>
          <h2 style={{ margin: 0, fontSize: 28, color: '#0b1f3a' }}>Liên hệ</h2>
          <p className="desc">
            Gửi câu hỏi về tra cứu, mượn trả hoặc đặt lịch hỗ trợ thủ thư — Thư viện số Đại học Giao
            thông vận tải.
          </p>
        </div>
      </div>

      <div className="home-split">
        <div className="home-panel">
          <h3 style={{ marginTop: 0 }}>Thông tin liên hệ</h3>
          <p style={{ color: '#475569' }}>
            <EnvironmentOutlined /> Hà Nội: Số 3 Cầu Giấy, Đống Đa
            <br />
            <EnvironmentOutlined /> TP.HCM: 450–451 Lê Văn Việt, Thủ Đức
          </p>
          <p style={{ color: '#475569' }}>
            <PhoneOutlined /> Hotline: (028) 3835 4409 (ext 114)
            <br />
            <MailOutlined /> thuvienso@utc.edu.vn
          </p>
          <p style={{ color: '#64748b', fontSize: 13, marginBottom: 0 }}>
            Giờ mở cửa: 07:30 – 20:30 (T2–T7) · Hỗ trợ online 08:00 – 17:00 (T2–T6)
          </p>
          <div style={{ marginTop: 16 }}>
            <Link to="/about">
              <Button>Giới thiệu thư viện</Button>
            </Link>
          </div>
        </div>

        <div className="home-panel">
          <h3 style={{ marginTop: 0 }}>Gửi yêu cầu hỗ trợ</h3>
          <Form
            form={form}
            layout="vertical"
            onFinish={() => {
              message.success('Đã ghi nhận yêu cầu (mô phỏng). Thủ thư sẽ phản hồi trong giờ hành chính.');
              form.resetFields();
            }}
          >
            <Form.Item name="name" label="Họ tên" rules={[{ required: true, message: 'Nhập họ tên' }]}>
              <Input placeholder="Nguyễn Văn A" />
            </Form.Item>
            <Form.Item
              name="email"
              label="Email"
              rules={[
                { required: true, message: 'Nhập email' },
                { type: 'email', message: 'Email không hợp lệ' },
              ]}
            >
              <Input placeholder="ban@utc.edu.vn" />
            </Form.Item>
            <Form.Item
              name="message"
              label="Nội dung"
              rules={[{ required: true, message: 'Nhập nội dung' }]}
            >
              <Input.TextArea rows={4} placeholder="Mô tả nhu cầu hỗ trợ…" />
            </Form.Item>
            <Button type="primary" htmlType="submit" style={{ background: '#0B1F3A' }}>
              Gửi liên hệ
            </Button>
          </Form>
        </div>
      </div>
    </div>
  );
}
