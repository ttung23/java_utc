import { Navigate } from 'react-router-dom';

/** Giữ route /register — chuyển sang tab đăng ký trên trang login. */
export function RegisterPage() {
  return <Navigate to="/login?tab=register" replace />;
}
