import { Link } from 'react-router-dom';
import { Button, Card, Col, Row, Tag } from 'antd';
import {
  ClockCircleOutlined,
  EnvironmentOutlined,
  MailOutlined,
  PhoneOutlined,
  ReadOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';

export function AboutPage() {
  return (
    <div>
      <div className="home-section-head">
        <div>
          <div className="eyebrow">THƯ VIỆN SỐ UTC</div>
          <h2 style={{ margin: 0, fontSize: 28, color: '#0b1f3a' }}>Giới thiệu</h2>
          <p className="desc">
            Hệ thống thư viện thông minh phục vụ đào tạo, nghiên cứu và học tập của Đại học Giao thông
            vận tải (UTC).
          </p>
        </div>
        <Tag icon={<SafetyCertificateOutlined />} color="processing">
          ISO 9001:2015
        </Tag>
      </div>

      <div className="home-panel" style={{ marginBottom: 16 }}>
        <h3 style={{ marginTop: 0, color: '#0b1f3a' }}>
          <ReadOutlined /> Sứ mệnh
        </h3>
        <p style={{ color: '#475569', lineHeight: 1.7, marginBottom: 0 }}>
          Cung cấp kho học liệu in &amp; số, hỗ trợ tra cứu OPAC, mượn–trả trực tuyến, đặt trước và
          tư vấn thông tin học thuật cho sinh viên, giảng viên, viên chức UTC. Thư viện hướng tới trải
          nghiệm phục vụ nhanh, minh bạch và đúng quy định học đường.
        </p>
      </div>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} md={12}>
          <Card
            title={
              <span>
                <EnvironmentOutlined /> Cơ sở Hà Nội
              </span>
            }
            style={{ borderRadius: 14, height: '100%' }}
          >
            <p style={{ color: '#475569', marginBottom: 8 }}>
              Số 3 Cầu Giấy, Đống Đa, Hà Nội
            </p>
            <p style={{ color: '#64748b', margin: 0, fontSize: 13 }}>
              <ClockCircleOutlined /> 07:30 – 20:30 (T2–T7)
            </p>
          </Card>
        </Col>
        <Col xs={24} md={12}>
          <Card
            title={
              <span>
                <EnvironmentOutlined /> Cơ sở TP. Hồ Chí Minh
              </span>
            }
            style={{ borderRadius: 14, height: '100%' }}
          >
            <p style={{ color: '#475569', marginBottom: 8 }}>
              Số 450–451 Lê Văn Việt, TP. Thủ Đức, TP.HCM
            </p>
            <p style={{ color: '#64748b', margin: 0, fontSize: 13 }}>
              <ClockCircleOutlined /> 07:30 – 20:30 (T2–T7)
            </p>
          </Card>
        </Col>
      </Row>

      <div className="home-panel" id="lien-he" style={{ marginBottom: 16 }}>
        <h3 style={{ marginTop: 0, color: '#0b1f3a' }}>Liên hệ</h3>
        <Row gutter={[16, 12]}>
          <Col xs={24} sm={8}>
            <div style={{ fontSize: 13, color: '#64748b' }}>Hotline</div>
            <div style={{ fontWeight: 700 }}>
              <PhoneOutlined /> (028) 3835 4409 (ext 114)
            </div>
          </Col>
          <Col xs={24} sm={8}>
            <div style={{ fontSize: 13, color: '#64748b' }}>Email</div>
            <div style={{ fontWeight: 700 }}>
              <MailOutlined /> thuvienso@utc.edu.vn
            </div>
          </Col>
          <Col xs={24} sm={8}>
            <div style={{ fontSize: 13, color: '#64748b' }}>Hỗ trợ online</div>
            <div style={{ fontWeight: 700 }}>08:00 – 17:00 (T2–T6)</div>
          </Col>
        </Row>
      </div>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
        <Link to="/rules">
          <Button type="primary" style={{ background: '#0B1F3A' }}>
            Xem quy định mượn trả
          </Button>
        </Link>
        <Link to="/news">
          <Button>Tin tức &amp; hoạt động</Button>
        </Link>
        <Link to="/books">
          <Button>Tra cứu OPAC</Button>
        </Link>
        <Link to="/contact">
          <Button type="link">Trang liên hệ chi tiết →</Button>
        </Link>
      </div>
    </div>
  );
}
