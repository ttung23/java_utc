import { ShoppingCartOutlined } from '@ant-design/icons';
import { Badge, Button } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import { useBorrowCart } from '@/cart/BorrowCartContext';

export function FloatingCartBar() {
  const { count } = useBorrowCart();
  const navigate = useNavigate();
  const location = useLocation();

  if (count <= 0) return null;
  if (location.pathname.startsWith('/borrow-cart')) return null;

  return (
    <div className="float-cart">
      <div className="float-cart-left">
        <Badge count={count} color="#2563eb">
          <span className="float-cart-icon">
            <ShoppingCartOutlined />
          </span>
        </Badge>
        <div>
          <div className="t">Giỏ mượn: {count} tài liệu</div>
          <div className="s">Hạn mượn dự kiến: 14 ngày</div>
        </div>
      </div>
      <Button type="primary" onClick={() => navigate('/borrow-cart')}>
        Xem Giỏ &amp; Gửi Yêu Cầu →
      </Button>
    </div>
  );
}
