import React, { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  LayoutDashboard,
  Briefcase,
  Users,
  Shield,
  LogOut,
  Wrench,
  Menu,
  X,
  ChevronRight,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const ADMIN_NAV = [
  { label: 'Dashboard', icon: LayoutDashboard, path: '/admin/dashboard' },
  { label: 'Pekerjaan', icon: Briefcase, path: '/admin/jobs' },
  { label: 'Pengguna', icon: Users, path: '/admin/users' },
  { label: 'Audit Log', icon: Shield, path: '/admin/audit' },
];

const TECH_NAV = [
  { label: 'Dashboard', icon: LayoutDashboard, path: '/tech/dashboard' },
  { label: 'Pekerjaan Saya', icon: Wrench, path: '/tech/jobs' },
];

export default function DashboardLayout() {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const navItems = isAdmin ? ADMIN_NAV : TECH_NAV;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleNav = (path) => {
    navigate(path);
    setSidebarOpen(false);
  };

  const isActive = (path) => location.pathname === path || location.pathname.startsWith(path + '/');

  const sidebarContent = (
    <div className="flex h-full flex-col">
      {/* Logo */}
      <div className="flex items-center gap-3 px-6 py-6">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-500 shadow-button">
          <Wrench className="h-5 w-5 text-white" />
        </div>
        <div>
          <h1 className="text-lg font-bold text-neutral-900 tracking-tight">RJS FSM</h1>
          <span className="inline-flex items-center rounded-md bg-primary-50 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-primary-600">
            {user?.role || 'User'}
          </span>
        </div>
      </div>

      {/* Divider */}
      <div className="mx-5 border-t border-neutral-100" />

      {/* Navigation */}
      <nav className="flex-1 space-y-1 px-4 py-4">
        {navItems.map((item) => {
          const active = isActive(item.path);
          return (
            <button
              key={item.path}
              onClick={() => handleNav(item.path)}
              className={`nav-item w-full text-left ${active ? 'active' : ''}`}
            >
              <item.icon className="h-5 w-5 flex-shrink-0" />
              <span className="flex-1">{item.label}</span>
              {active && <ChevronRight className="h-4 w-4 opacity-60" />}
            </button>
          );
        })}
      </nav>

      {/* User info + logout */}
      <div className="border-t border-neutral-100 px-4 py-4">
        <div className="mb-3 flex items-center gap-3 px-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary-100 text-sm font-bold text-primary-600">
            {user?.username?.charAt(0).toUpperCase() || 'U'}
          </div>
          <div className="flex-1 min-w-0">
            <p className="truncate text-sm font-semibold text-neutral-800">{user?.username}</p>
            <p className="text-xs text-neutral-400">{isAdmin ? 'Administrator' : 'Teknisi'}</p>
          </div>
        </div>
        <button
          onClick={handleLogout}
          className="flex w-full items-center gap-3 rounded-xl px-4 py-2.5 text-sm font-medium text-red-500 transition-all duration-200 hover:bg-red-50 active:scale-[0.98]"
        >
          <LogOut className="h-4 w-4" />
          Keluar
        </button>
      </div>
    </div>
  );

  return (
    <div className="flex h-screen bg-neutral-50">
      {/* Desktop sidebar */}
      <aside className="hidden lg:flex lg:w-[280px] lg:flex-shrink-0 lg:flex-col border-r border-neutral-150 bg-white">
        {sidebarContent}
      </aside>

      {/* Mobile overlay */}
      <AnimatePresence>
        {sidebarOpen && (
          <>
            <motion.div
              className="fixed inset-0 z-40 bg-neutral-900/40 backdrop-blur-sm lg:hidden"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setSidebarOpen(false)}
            />
            <motion.aside
              className="fixed inset-y-0 left-0 z-50 w-[280px] bg-white shadow-xl lg:hidden"
              initial={{ x: -280 }}
              animate={{ x: 0 }}
              exit={{ x: -280 }}
              transition={{ type: 'spring', damping: 25, stiffness: 300 }}
            >
              {sidebarContent}
            </motion.aside>
          </>
        )}
      </AnimatePresence>

      {/* Main content */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Mobile header */}
        <header className="flex items-center gap-3 border-b border-neutral-150 bg-white px-4 py-3 lg:hidden">
          <button
            onClick={() => setSidebarOpen(true)}
            className="rounded-lg p-2 text-neutral-500 transition-colors hover:bg-neutral-100"
          >
            <Menu className="h-5 w-5" />
          </button>
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary-500">
              <Wrench className="h-4 w-4 text-white" />
            </div>
            <span className="text-base font-bold text-neutral-900">RJS FSM</span>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
