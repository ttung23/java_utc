import { useEffect, useMemo, useState } from 'react';
import {
  CalendarOutlined,
  EnvironmentOutlined,
  ReadOutlined,
  RightOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { Button, Empty, Input, Segmented, Skeleton, Tag, message } from 'antd';
import { Link, useSearchParams } from 'react-router-dom';
import dayjs from 'dayjs';
import { api, type ApiResponse } from '@/services/api';

type News = {
  id: number;
  title: string;
  slug?: string;
  content?: string;
  coverUrl?: string;
  pinned?: boolean;
  publishedAt?: string;
};

type TabKey = 'all' | 'news' | 'events';

const EVENTS = [
  {
    id: 'e1',
    month: 'THÁNG 11',
    day: '02',
    title: 'Workshop tra cứu IEEE / Scopus & quản lý trích dẫn',
    place: 'Phòng Lab A2 · Cơ sở Hà Nội',
    time: '14:00 – 16:30',
    tag: 'Workshop',
    free: true,
  },
  {
    id: 'e2',
    month: 'THÁNG 11',
    day: '15',
    title: 'Book Exchange Week & Cultural Reading Seminar',
    place: 'Sảnh Thư viện CS1 · Cả ngày',
    time: '08:00 – 17:00',
    tag: 'Sự kiện',
    free: true,
  },
  {
    id: 'e3',
    month: 'THÁNG 12',
    day: '05',
    title: 'Hướng dẫn mượn liên chi nhánh & gia hạn online',
    place: 'Phòng đọc tầng 2 · Cơ sở TP.HCM',
    time: '09:30 – 11:00',
    tag: 'Hướng dẫn',
    free: true,
  },
];

function excerpt(htmlOrText?: string, max = 160) {
  if (!htmlOrText) return 'Xem chi tiết trên cổng độc giả.';
  const text = htmlOrText.replace(/<[^>]+>/g, '').trim();
  return text.length > max ? `${text.slice(0, max)}…` : text;
}

export function NewsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const tabParam = searchParams.get('tab');
  const initialTab: TabKey =
    tabParam === 'events' || tabParam === 'news' || tabParam === 'all' ? tabParam : 'all';
  const [tab, setTab] = useState<TabKey>(initialTab);
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(true);
  const [news, setNews] = useState<News[]>([]);

  useEffect(() => {
    if (tabParam === 'events' || tabParam === 'news' || tabParam === 'all') {
      setTab(tabParam);
    }
  }, [tabParam]);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const { data } = await api.get<ApiResponse<News[]>>('/news/public');
        setNews(data.data || []);
      } catch {
        message.error('Không tải được tin tức');
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, []);

  const filteredNews = useMemo(() => {
    const key = q.trim().toLowerCase();
    if (!key) return news;
    return news.filter(
      (n) =>
        n.title.toLowerCase().includes(key) ||
        (n.content || '').toLowerCase().includes(key),
    );
  }, [news, q]);

  const filteredEvents = useMemo(() => {
    const key = q.trim().toLowerCase();
    if (!key) return EVENTS;
    return EVENTS.filter(
      (e) =>
        e.title.toLowerCase().includes(key) ||
        e.place.toLowerCase().includes(key) ||
        e.tag.toLowerCase().includes(key),
    );
  }, [q]);

  const showNews = tab === 'all' || tab === 'news';
  const showEvents = tab === 'all' || tab === 'events';

  return (
    <div>
      <div className="home-section-head" style={{ marginTop: 4 }}>
        <div>
          <div className="eyebrow">CỔNG ĐỘC GIẢ UTC</div>
          <h2 style={{ margin: 0, fontSize: 28, color: '#0b1f3a' }}>Tin tức &amp; Hoạt động</h2>
          <p className="desc">
            Thông báo thư viện, quy định mới và lịch workshop / sự kiện dành cho sinh viên, giảng viên.
          </p>
        </div>
      </div>

      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: 12,
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: 18,
        }}
      >
        <Segmented
          value={tab}
          onChange={(v) => {
            const next = v as TabKey;
            setTab(next);
            setSearchParams(next === 'all' ? {} : { tab: next });
          }}
          options={[
            { label: 'Tất cả', value: 'all' },
            { label: 'Tin tức', value: 'news' },
            { label: 'Hoạt động', value: 'events' },
          ]}
        />
        <Input.Search
          allowClear
          placeholder="Tìm theo tiêu đề, nội dung…"
          style={{ maxWidth: 320 }}
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
      </div>

      {loading ? <Skeleton active paragraph={{ rows: 8 }} /> : null}

      {!loading && showNews ? (
        <section className="home-panel" style={{ marginBottom: 16 }}>
          <div className="home-panel-head">
            <h3>
              <ThunderboltOutlined style={{ color: '#1d4ed8' }} /> Thông báo độc giả
            </h3>
            <span style={{ fontSize: 13, color: '#64748b' }}>{filteredNews.length} tin</span>
          </div>

          {filteredNews.length === 0 ? (
            <Empty description="Chưa có tin phù hợp" />
          ) : (
            <div style={{ display: 'grid', gap: 12 }}>
              {filteredNews.map((item, idx) => (
                <Link
                  key={item.id}
                  to={`/news/${item.slug || item.id}`}
                  style={{
                    display: 'grid',
                    gridTemplateColumns: item.coverUrl ? '140px 1fr' : '1fr',
                    gap: 14,
                    padding: 14,
                    border: '1px solid #e2e8f0',
                    borderRadius: 12,
                    background: '#fff',
                  }}
                >
                  {item.coverUrl ? (
                    <img
                      src={item.coverUrl}
                      alt=""
                      style={{
                        width: '100%',
                        height: 96,
                        objectFit: 'cover',
                        borderRadius: 10,
                        background: '#e2e8f0',
                      }}
                    />
                  ) : null}
                  <div>
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 6 }}>
                      <Tag color="blue">{idx % 2 === 0 ? 'QUY ĐỊNH' : 'DỊCH VỤ'}</Tag>
                      {item.pinned ? <Tag color="gold">Ghim</Tag> : null}
                      <span style={{ fontSize: 12, color: '#64748b' }}>
                        {item.publishedAt ? dayjs(item.publishedAt).format('DD/MM/YYYY') : '—'}
                      </span>
                    </div>
                    <h3 style={{ margin: '0 0 6px', fontSize: 16, color: '#0f172a' }}>{item.title}</h3>
                    <p style={{ margin: 0, fontSize: 13, color: '#64748b', lineHeight: 1.55 }}>
                      {excerpt(item.content)}
                    </p>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, marginTop: 8, color: '#1d4ed8', fontSize: 13, fontWeight: 600 }}>
                      Đọc tiếp <RightOutlined />
                    </span>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </section>
      ) : null}

      {!loading && showEvents ? (
        <section className="home-panel">
          <div className="home-panel-head">
            <h3>
              <CalendarOutlined style={{ color: '#059669' }} /> Lịch hoạt động &amp; workshop
            </h3>
            <span style={{ fontSize: 13, color: '#64748b' }}>{filteredEvents.length} sự kiện</span>
          </div>

          {filteredEvents.length === 0 ? (
            <Empty description="Chưa có hoạt động phù hợp" />
          ) : (
            <div style={{ display: 'grid', gap: 12 }}>
              {filteredEvents.map((ev) => (
                <article
                  key={ev.id}
                  style={{
                    display: 'grid',
                    gridTemplateColumns: '72px 1fr auto',
                    gap: 14,
                    alignItems: 'center',
                    padding: 14,
                    border: '1px solid #e2e8f0',
                    borderRadius: 12,
                    background: '#fff',
                  }}
                >
                  <div className="home-event-date">
                    <div className="m">{ev.month}</div>
                    <div className="d">{ev.day}</div>
                  </div>
                  <div>
                    <div style={{ marginBottom: 4 }}>
                      <Tag color="green">{ev.tag}</Tag>
                      {ev.free ? <Tag>Miễn phí</Tag> : null}
                    </div>
                    <h3 style={{ margin: '0 0 6px', fontSize: 15 }}>{ev.title}</h3>
                    <div style={{ fontSize: 12, color: '#64748b', display: 'flex', flexWrap: 'wrap', gap: 12 }}>
                      <span>
                        <EnvironmentOutlined /> {ev.place}
                      </span>
                      <span>
                        <CalendarOutlined /> {ev.time}
                      </span>
                    </div>
                  </div>
                  <Button type="primary" style={{ background: '#0B1F3A' }}>
                    {ev.free ? 'Đăng ký tham gia' : 'Xem chi tiết'}
                  </Button>
                </article>
              ))}
            </div>
          )}
        </section>
      ) : null}

      <div
        style={{
          marginTop: 20,
          padding: 18,
          borderRadius: 14,
          background: '#eff6ff',
          border: '1px solid #bfdbfe',
          display: 'flex',
          flexWrap: 'wrap',
          gap: 12,
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <div>
          <strong style={{ color: '#0b1f3a' }}>
            <ReadOutlined /> Cần hỗ trợ đăng ký workshop?
          </strong>
          <div style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>
            Liên hệ quầy thủ thư hoặc hotline (028) 3835 4409 trong giờ hành chính.
          </div>
        </div>
        <Link to="/books">
          <Button>Quay lại tra cứu sách</Button>
        </Link>
      </div>
    </div>
  );
}
