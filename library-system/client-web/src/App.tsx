import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { ConfigProvider, App as AntApp } from 'antd';
import viVN from 'antd/locale/vi_VN';
import { AuthProvider } from '@/auth/AuthContext';
import { RequireAuth } from '@/auth/RequireAuth';
import { ClientLayout } from '@/layout/ClientLayout';
import { HomePage } from '@/pages/HomePage';
import { LoginPage } from '@/pages/LoginPage';
import { RegisterPage } from '@/pages/RegisterPage';
import { BooksPage } from '@/pages/BooksPage';
import { BookDetailPage } from '@/pages/BookDetailPage';
import { MyBorrowsPage } from '@/pages/MyBorrowsPage';
import { MyFinesPage } from '@/pages/MyFinesPage';
import { MyReservationsPage } from '@/pages/MyReservationsPage';
import { FavoritesPage } from '@/pages/FavoritesPage';
import { NotificationsPage } from '@/pages/NotificationsPage';
import { NewsDetailPage } from '@/pages/NewsDetailPage';

export default function App() {
  return (
    <ConfigProvider
      locale={viVN}
      theme={{
        token: {
          colorPrimary: '#0b1f3a',
          borderRadius: 8,
          fontFamily: '"Be Vietnam Pro", "Segoe UI", Roboto, Arial, sans-serif',
        },
      }}
    >
      <AntApp>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              <Route element={<ClientLayout />}>
                <Route index element={<HomePage />} />
                <Route path="books" element={<BooksPage />} />
                <Route path="books/:id" element={<BookDetailPage />} />
                <Route path="news/:slug" element={<NewsDetailPage />} />
                <Route path="login" element={<LoginPage />} />
                <Route path="register" element={<RegisterPage />} />
                <Route element={<RequireAuth />}>
                  <Route path="me/borrows" element={<MyBorrowsPage />} />
                  <Route path="me/fines" element={<MyFinesPage />} />
                  <Route path="me/reservations" element={<MyReservationsPage />} />
                  <Route path="me/favorites" element={<FavoritesPage />} />
                  <Route path="me/notifications" element={<NotificationsPage />} />
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
