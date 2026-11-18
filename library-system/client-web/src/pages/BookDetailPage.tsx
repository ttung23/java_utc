import { useEffect, useState } from 'react';
import { Button, Card, Form, Input, InputNumber, List, Space, Typography, message } from 'antd';
import { HeartOutlined, HeartFilled } from '@ant-design/icons';
import { Link, useParams } from 'react-router-dom';
import { api, type ApiResponse } from '@/services/api';
import { useAuth } from '@/auth/AuthContext';

type Book = {
  id: number;
  title: string;
  description?: string;
  isbn13?: string;
  avgRating?: number;
};

type Review = { id: number; rating: number; comment?: string; userName?: string };

export function BookDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const [book, setBook] = useState<Book | null>(null);
  const [related, setRelated] = useState<Book[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [fav, setFav] = useState(false);
  const [form] = Form.useForm();

  const load = async () => {
    if (!id) return;
    try {
      const [b, r, rev] = await Promise.all([
        api.get<ApiResponse<Book>>(`/books/${id}`),
        api.get<ApiResponse<Book[]>>(`/books/${id}/related`),
        api.get<ApiResponse<Review[]>>('/reviews', { params: { bookId: id } }),
      ]);
      setBook(b.data.data);
      setRelated(r.data.data || []);
      setReviews(rev.data.data || []);
    } catch {
      message.error('Không tải được chi tiết sách');
    }
  };

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const toggleFav = async () => {
    if (!user || !id) {
      message.warning('Đăng nhập để yêu thích');
      return;
    }
    try {
      if (fav) {
        await api.delete(`/favorites/${id}`);
        setFav(false);
      } else {
        await api.post(`/favorites/${id}`);
        setFav(true);
      }
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Thao tác thất bại');
    }
  };

  const borrowOnline = async () => {
    if (!user || !id) {
      message.warning('Đăng nhập để mượn online');
      return;
    }
    try {
      await api.post('/borrows/online', {
        items: [{ bookId: Number(id) }],
      });
      message.success('Đã gửi yêu cầu mượn online');
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Gửi yêu cầu thất bại');
    }
  };

  const reserve = async () => {
    if (!user || !id) {
      message.warning('Đăng nhập để đặt trước');
      return;
    }
    try {
      await api.post('/reservations', { bookId: Number(id) });
      message.success('Đã đặt trước');
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Đặt trước thất bại');
    }
  };

  const submitReview = async () => {
    const values = await form.validateFields();
    try {
      await api.post('/reviews', {
        bookId: Number(id),
        rating: values.rating,
        comment: values.comment,
      });
      message.success('Đã gửi đánh giá');
      form.resetFields();
      void load();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      message.error(err.response?.data?.message || 'Gửi đánh giá thất bại');
    }
  };

  if (!book) return null;

  return (
    <div>
      <Typography.Title level={2} style={{ marginTop: 0 }}>
        {book.title}
      </Typography.Title>
      <Typography.Paragraph type="secondary">ISBN: {book.isbn13 || '—'}</Typography.Paragraph>
      <Typography.Paragraph>{book.description || 'Chưa có mô tả.'}</Typography.Paragraph>
      <Space wrap style={{ marginBottom: 24 }}>
        <Button type="primary" onClick={() => void borrowOnline()}>
          Mượn online
        </Button>
        <Button onClick={() => void reserve()}>Đặt trước</Button>
        <Button
          icon={fav ? <HeartFilled /> : <HeartOutlined />}
          onClick={() => void toggleFav()}
        >
          Yêu thích
        </Button>
      </Space>

      <Card title="Đánh giá" style={{ marginBottom: 16 }}>
        <List
          dataSource={reviews}
          locale={{ emptyText: 'Chưa có đánh giá' }}
          renderItem={(item) => (
            <List.Item>
              <List.Item.Meta
                title={`${item.userName || 'Ẩn danh'} · ${item.rating}/5`}
                description={item.comment}
              />
            </List.Item>
          )}
        />
        {user ? (
          <Form form={form} layout="inline" style={{ marginTop: 12 }} onFinish={() => void submitReview()}>
            <Form.Item name="rating" rules={[{ required: true }]}>
              <InputNumber min={1} max={5} placeholder="Sao" />
            </Form.Item>
            <Form.Item name="comment" style={{ flex: 1, minWidth: 200 }}>
              <Input placeholder="Nhận xét" />
            </Form.Item>
            <Button type="primary" htmlType="submit">
              Gửi
            </Button>
          </Form>
        ) : null}
      </Card>

      <Card title="Sách liên quan">
        <List
          dataSource={related}
          renderItem={(item) => (
            <List.Item>
              <Link to={`/books/${item.id}`}>{item.title}</Link>
            </List.Item>
          )}
        />
      </Card>
    </div>
  );
}
