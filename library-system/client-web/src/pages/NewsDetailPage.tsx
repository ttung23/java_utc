import { useEffect, useState } from 'react';
import { Card, Typography, message } from 'antd';
import { useParams } from 'react-router-dom';
import { api, type ApiResponse } from '@/services/api';

type News = { id: number; title: string; content?: string; publishedAt?: string };

export function NewsDetailPage() {
  const { slug } = useParams();
  const [news, setNews] = useState<News | null>(null);

  useEffect(() => {
    const load = async () => {
      if (!slug) return;
      try {
        const { data } = await api.get<ApiResponse<News>>(`/news/slug/${slug}`);
        setNews(data.data);
      } catch {
        message.error('Không tải được tin');
      }
    };
    void load();
  }, [slug]);

  if (!news) return null;

  return (
    <Card>
      <Typography.Title level={2} style={{ marginTop: 0 }}>
        {news.title}
      </Typography.Title>
      <Typography.Paragraph type="secondary">{news.publishedAt}</Typography.Paragraph>
      <Typography.Paragraph style={{ whiteSpace: 'pre-wrap' }}>{news.content}</Typography.Paragraph>
    </Card>
  );
}
