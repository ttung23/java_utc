import { useEffect, useState } from 'react';
import { Card, Col, List, Row, Typography, message } from 'antd';
import { Link } from 'react-router-dom';
import { api, type ApiResponse } from '@/services/api';

type Book = { id: number; title: string; coverUrl?: string; avgRating?: number };
type News = { id: number; title: string; slug?: string; publishedAt?: string };
type Banner = { id: number; imageUrl: string; link?: string };

export function HomePage() {
  const [newBooks, setNewBooks] = useState<Book[]>([]);
  const [popular, setPopular] = useState<Book[]>([]);
  const [news, setNews] = useState<News[]>([]);
  const [banners, setBanners] = useState<Banner[]>([]);

  useEffect(() => {
    const load = async () => {
      try {
        const [n, p, newsRes, b] = await Promise.all([
          api.get<ApiResponse<Book[]>>('/books/new'),
          api.get<ApiResponse<Book[]>>('/books/popular'),
          api.get<ApiResponse<News[]>>('/news/public'),
          api.get<ApiResponse<Banner[]>>('/banners/public'),
        ]);
        setNewBooks(n.data.data || []);
        setPopular(p.data.data || []);
        setNews(newsRes.data.data || []);
        setBanners(b.data.data || []);
      } catch {
        message.error('Không tải được trang chủ');
      }
    };
    void load();
  }, []);

  return (
    <div>
      <Typography.Title level={2} style={{ marginTop: 0 }}>
        Cổng độc giả
      </Typography.Title>
      {banners[0] ? (
        <a href={banners[0].link || '#'} style={{ display: 'block', marginBottom: 24 }}>
          <img
            src={banners[0].imageUrl}
            alt="banner"
            style={{ width: '100%', maxHeight: 220, objectFit: 'cover', borderRadius: 12 }}
          />
        </a>
      ) : null}

      <Row gutter={[16, 16]}>
        <Col xs={24} md={16}>
          <Card title="Sách mới" extra={<Link to="/books">Xem tất cả</Link>}>
            <List
              grid={{ gutter: 12, column: 2 }}
              dataSource={newBooks.slice(0, 6)}
              renderItem={(item) => (
                <List.Item>
                  <Link to={`/books/${item.id}`}>
                    <Card size="small" hoverable>
                      {item.title}
                    </Card>
                  </Link>
                </List.Item>
              )}
            />
          </Card>
          <Card title="Phổ biến" style={{ marginTop: 16 }}>
            <List
              dataSource={popular.slice(0, 8)}
              renderItem={(item) => (
                <List.Item>
                  <Link to={`/books/${item.id}`}>{item.title}</Link>
                </List.Item>
              )}
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card title="Tin tức">
            <List
              dataSource={news.slice(0, 8)}
              renderItem={(item) => (
                <List.Item>
                  <Link to={`/news/${item.slug || item.id}`}>{item.title}</Link>
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
