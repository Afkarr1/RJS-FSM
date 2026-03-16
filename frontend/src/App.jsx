import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import LoginPage from './pages/LoginPage';
import DashboardLayout from './components/DashboardLayout';

// Admin pages
import AdminDashboardPage from './pages/admin/DashboardPage';
import AdminJobListPage from './pages/admin/JobListPage';
import AdminJobDetailPage from './pages/admin/JobDetailPage';
import AdminCreateJobPage from './pages/admin/CreateJobPage';
import AdminUserListPage from './pages/admin/UserListPage';
import AdminAuditPage from './pages/admin/AuditPage';

// Tech pages
import TechDashboardPage from './pages/tech/TechDashboardPage';
import TechJobListPage from './pages/tech/TechJobListPage';
import TechJobDetailPage from './pages/tech/TechJobDetailPage';

// Public pages
import ReviewPage from './pages/public/ReviewPage';

function RequireAuth({ children, role }) {
  const { isAuthenticated, isAdmin, isTech } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (role === 'ADMIN' && !isAdmin) return <Navigate to="/tech/dashboard" replace />;
  if (role === 'TECHNICIAN' && !isTech) return <Navigate to="/admin/dashboard" replace />;
  return children;
}

function RootRedirect() {
  const { isAuthenticated, isAdmin } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <Navigate to={isAdmin ? '/admin/dashboard' : '/tech/dashboard'} replace />;
}

export default function App() {
  return (
    <Routes>
      {/* Root */}
      <Route path="/" element={<RootRedirect />} />

      {/* Login */}
      <Route path="/login" element={<LoginPage />} />

      {/* Public routes (no auth required) */}
      <Route path="/public/reviews/:token" element={<ReviewPage />} />

      {/* Admin routes */}
      <Route
        path="/admin"
        element={
          <RequireAuth role="ADMIN">
            <DashboardLayout />
          </RequireAuth>
        }
      >
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard" element={<AdminDashboardPage />} />
        <Route path="jobs" element={<AdminJobListPage />} />
        <Route path="jobs/new" element={<AdminCreateJobPage />} />
        <Route path="jobs/:id" element={<AdminJobDetailPage />} />
        <Route path="users" element={<AdminUserListPage />} />
        <Route path="audit" element={<AdminAuditPage />} />
      </Route>

      {/* Tech routes */}
      <Route
        path="/tech"
        element={
          <RequireAuth role="TECHNICIAN">
            <DashboardLayout />
          </RequireAuth>
        }
      >
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard" element={<TechDashboardPage />} />
        <Route path="jobs" element={<TechJobListPage />} />
        <Route path="jobs/:id" element={<TechJobDetailPage />} />
      </Route>

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
