import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { ConfigProvider, App as AntApp } from 'antd';
import viVN from 'antd/locale/vi_VN';
import dayjs from 'dayjs';
import 'dayjs/locale/vi';
import { AuthProvider } from '@/auth/AuthContext';
import { RequireAdmin, RequireAuth } from '@/auth/RequireAuth';
import { AppLayout } from '@/layout/AppLayout';
import { LoginPage } from '@/pages/LoginPage';
import { DashboardPage } from '@/pages/DashboardPage';
import { PlaceholderPage } from '@/pages/PlaceholderPage';
import { CatalogPage } from '@/pages/CatalogPage';
import { BooksPage } from '@/pages/BooksPage';
import { CopiesPage } from '@/pages/CopiesPage';
import { ReadersPage } from '@/pages/ReadersPage';
import { ReaderDetailPage } from '@/pages/ReaderDetailPage';
import { tokens } from '@/styles/tokens';

dayjs.locale('vi');

export default function App() {
  return (
    <ConfigProvider
      locale={viVN}
      theme={{
        token: {
          colorPrimary: tokens.color.primary,
          colorSuccess: tokens.color.success,
          colorWarning: tokens.color.warning,
          colorError: tokens.color.danger,
          borderRadius: tokens.radius.md,
          fontFamily:
            '"Be Vietnam Pro", "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
        },
      }}
    >
      <AntApp>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route
                path="/403"
                element={
                  <PlaceholderPage title="403 — Không có quyền" description="Bạn không được phép truy cập trang này." />
                }
              />
              <Route element={<RequireAuth />}>
                <Route element={<AppLayout />}>
                  <Route index element={<DashboardPage />} />
                  <Route path="catalog" element={<CatalogPage />} />
                  <Route path="borrows/create" element={<PlaceholderPage title="Tạo phiếu mượn (quầy)" />} />
                  <Route path="returns" element={<PlaceholderPage title="Trả sách" />} />
                  <Route path="borrows/online" element={<PlaceholderPage title="Yêu cầu mượn online" />} />
                  <Route path="borrows" element={<PlaceholderPage title="Danh sách phiếu mượn" />} />
                  <Route path="borrows/overdue" element={<PlaceholderPage title="Sách quá hạn" />} />
                  <Route path="reservations" element={<PlaceholderPage title="Đặt trước" />} />
                  <Route path="fines" element={<PlaceholderPage title="Khoản phạt" />} />
                  <Route path="payments" element={<PlaceholderPage title="Biên lai thanh toán" />} />
                  <Route path="readers" element={<ReadersPage />} />
                  <Route path="readers/:id" element={<ReaderDetailPage />} />
                  <Route path="cards" element={<PlaceholderPage title="Thẻ thư viện" />} />
                  <Route path="books" element={<BooksPage />} />
                  <Route path="copies" element={<CopiesPage />} />
                  <Route path="locations" element={<PlaceholderPage title="Vị trí kệ" />} />
                  <Route path="news" element={<PlaceholderPage title="Tin tức" />} />
                  <Route path="banners" element={<PlaceholderPage title="Banner" />} />
                  <Route path="reports" element={<PlaceholderPage title="Báo cáo & Thống kê" />} />
                  <Route path="profile" element={<PlaceholderPage title="Hồ sơ" />} />
                  <Route element={<RequireAdmin />}>
                    <Route path="reader-types" element={<PlaceholderPage title="Loại độc giả" />} />
                    <Route path="staff" element={<PlaceholderPage title="Nhân viên" />} />
                    <Route path="settings" element={<PlaceholderPage title="Cấu hình" />} />
                    <Route path="holidays" element={<PlaceholderPage title="Ngày nghỉ" />} />
                    <Route path="audit-logs" element={<PlaceholderPage title="Nhật ký hoạt động" />} />
                  </Route>
                </Route>
              </Route>
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </AntApp>
    </ConfigProvider>
  );
}
