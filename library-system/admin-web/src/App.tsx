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
import { BorrowCreatePage } from '@/pages/BorrowCreatePage';
import { BorrowListPage } from '@/pages/BorrowListPage';
import { BorrowOnlinePage } from '@/pages/BorrowOnlinePage';
import { ReturnsPage } from '@/pages/ReturnsPage';
import { OverduePage } from '@/pages/OverduePage';
import { ReservationsPage } from '@/pages/ReservationsPage';
import { FinesPage } from '@/pages/FinesPage';
import { PaymentsPage } from '@/pages/PaymentsPage';
import { ReportsPage } from '@/pages/ReportsPage';
import { CardsPage } from '@/pages/CardsPage';
import { LocationsPage } from '@/pages/LocationsPage';
import { ReaderTypesPage } from '@/pages/ReaderTypesPage';
import { NewsPage } from '@/pages/NewsPage';
import { BannersPage } from '@/pages/BannersPage';
import { ProfilePage } from '@/pages/ProfilePage';
import { StaffPage } from '@/pages/StaffPage';
import { SettingsPage } from '@/pages/SettingsPage';
import { HolidaysPage } from '@/pages/HolidaysPage';
import { AuditLogsPage } from '@/pages/AuditLogsPage';
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
                  <Route path="borrows/create" element={<BorrowCreatePage />} />
                  <Route path="returns" element={<ReturnsPage />} />
                  <Route path="borrows/online" element={<BorrowOnlinePage />} />
                  <Route path="borrows" element={<BorrowListPage />} />
                  <Route path="borrows/overdue" element={<OverduePage />} />
                  <Route path="reservations" element={<ReservationsPage />} />
                  <Route path="fines" element={<FinesPage />} />
                  <Route path="payments" element={<PaymentsPage />} />
                  <Route path="readers" element={<ReadersPage />} />
                  <Route path="readers/:id" element={<ReaderDetailPage />} />
                  <Route path="cards" element={<CardsPage />} />
                  <Route path="books" element={<BooksPage />} />
                  <Route path="copies" element={<CopiesPage />} />
                  <Route path="locations" element={<LocationsPage />} />
                  <Route
                    path="inventory/receipts"
                    element={
                      <PlaceholderPage
                        title="Nhập sách"
                        description="Màn hình phiếu nhập kho sẽ hoàn thiện ở bước tiếp theo."
                      />
                    }
                  />
                  <Route
                    path="inventory/stocktake"
                    element={
                      <PlaceholderPage
                        title="Kiểm kê kho"
                        description="Màn hình kiểm kê sẽ hoàn thiện ở bước tiếp theo."
                      />
                    }
                  />
                  <Route path="news" element={<NewsPage />} />
                  <Route path="banners" element={<BannersPage />} />
                  <Route path="reports" element={<ReportsPage />} />
                  <Route path="profile" element={<ProfilePage />} />
                  <Route element={<RequireAdmin />}>
                    <Route path="reader-types" element={<ReaderTypesPage />} />
                    <Route path="staff" element={<StaffPage />} />
                    <Route path="settings" element={<SettingsPage />} />
                    <Route path="holidays" element={<HolidaysPage />} />
                    <Route path="audit-logs" element={<AuditLogsPage />} />
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
