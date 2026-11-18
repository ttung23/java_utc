import { Button, Card, Result } from 'antd';
import { Link } from 'react-router-dom';

type Props = {
  title: string;
  description?: string;
};

export function PlaceholderPage({ title, description }: Props) {
  return (
    <Card>
      <Result
        status="info"
        title={title}
        subTitle={description || 'Màn hình đang phát triển — API/module backend tương ứng chưa gắn đầy đủ.'}
        extra={
          <Button type="primary">
            <Link to="/">Về bảng điều khiển</Link>
          </Button>
        }
      />
    </Card>
  );
}
