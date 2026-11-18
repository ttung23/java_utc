import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { ConfigProvider, App as AntApp } from 'antd';
import viVN from 'antd/locale/vi_VN';
import { AuthProvider } from '@/auth/AuthContext';
import { RequireAuth } from '@/auth/RequireAuth';
import { BorrowCartProvider } from '@/cart/BorrowCartContext';
import { ClientLayout } from '@/layout/ClientLayout';
import { HomePage } from '@/pages/HomePage';
import { LoginPage } from '@/pages/LoginPage';
import { RegisterPage } from '@/pages/RegisterPage';
import { BooksPage } from '@/pages/BooksPage';
import { BookDetailPage } from '@/pages/BookDetailPage';
import { NewBooksPage } from '@/pages/NewBooksPage';
import { BorrowCartPage } from '@/pages/BorrowCartPage';
import { MyBorrowsPage } from '@/pages/MyBorrowsPage';
import { MyFinesPage } from '@/pages/MyFinesPage';
import { MyReservationsPage } from '@/pages/MyReservationsPage';
import { FavoritesPage } from '@/pages/FavoritesPage';
import { NotificationsPage } from '@/pages/NotificationsPage';
import { NewsPage } from '@/pages/NewsPage';
import { NewsDetailPage } from '@/pages/NewsDetailPage';
import { RulesPage } from '@/pages/RulesPage';
import { AboutPage } from '@/pages/AboutPage';
import { ContactPage } from '@/pages/ContactPage';

export default function App() {
  return (
    <ConfigProvider
      locale={viVN}
      theme={{
        token: {
          colorPrimary: '#0b1f3a',
          colorInfo: '#1d4ed8',
          borderRadius: 10,
          fontFamily: '"Be Vietnam Pro", "Segoe UI", Roboto, Arial, sans-serif',
        },
      }}
    >
      <AntApp>
        <AuthProvider>
          <BorrowCartProvider>
            <BrowserRouter>
              <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route element={<ClientLayout />}>
                  <Route index element={<HomePage />} />
                  <Route path="books" element={<BooksPage />} />
                  <Route path="books/:id" element={<BookDetailPage />} />
                  <Route path="sach-moi" element={<NewBooksPage />} />
                  <Route path="news" element={<NewsPage />} />
                  <Route path="news/:slug" element={<NewsDetailPage />} />
                  <Route path="rules" element={<RulesPage />} />
                  <Route path="about" element={<AboutPage />} />
                  <Route path="contact" element={<ContactPage />} />
                  <Route element={<RequireAuth />}>
                    <Route path="borrow-cart" element={<BorrowCartPage />} />
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
          </BorrowCartProvider>
        </AuthProvider>
      </AntApp>
    </ConfigProvider>
  );
}
