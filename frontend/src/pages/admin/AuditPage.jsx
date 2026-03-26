import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Shield, LogIn } from 'lucide-react';
import { adminApi } from '../../api/client';
import LoadingSpinner from '../../components/LoadingSpinner';
import EmptyState from '../../components/EmptyState';
import { useToast } from '../../components/Toast';

const TABS = [
  { key: 'activity', label: 'Log Aktivitas', icon: Shield },
  { key: 'login', label: 'Log Login', icon: LogIn },
];

export default function AuditPage() {
  const toast = useToast();
  const [activeTab, setActiveTab] = useState('activity');
  const [activityLogs, setActivityLogs] = useState([]);
  const [loginLogs, setLoginLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchLogs = async () => {
      setLoading(true);
      try {
        if (activeTab === 'activity') {
          const data = await adminApi.getAuditLogs();
          setActivityLogs(data || []);
        } else {
          const data = await adminApi.getLoginAudit();
          setLoginLogs(data || []);
        }
      } catch (err) {
        toast.error('Gagal memuat log');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchLogs();
  }, [activeTab]);

  const formatDateTime = (dateStr) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('id-ID', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: 'easeOut' }}
    >
      <h1 className="mb-6 text-2xl font-bold text-neutral-900">Audit Log</h1>

      {/* Tabs */}
      <div className="mb-6 flex gap-2">
        {TABS.map((tab) => {
          const isActive = activeTab === tab.key;
          return (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`inline-flex items-center gap-2 rounded-xl px-5 py-2.5 text-sm font-semibold transition-all duration-200 active:scale-[0.97] ${
                isActive
                  ? 'bg-primary-500 text-white shadow-button'
                  : 'bg-white text-neutral-500 ring-1 ring-neutral-200 hover:bg-neutral-50 hover:text-neutral-700'
              }`}
            >
              <tab.icon className="h-4 w-4" />
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Content */}
      {loading ? (
        <LoadingSpinner />
      ) : activeTab === 'activity' ? (
        <ActivityTable logs={activityLogs} formatDateTime={formatDateTime} />
      ) : (
        <LoginTable logs={loginLogs} formatDateTime={formatDateTime} />
      )}
    </motion.div>
  );
}

function ActivityTable({ logs, formatDateTime }) {
  if (logs.length === 0) {
    return (
      <EmptyState
        icon={Shield}
        title="Tidak ada log aktivitas"
        description="Log aktivitas akan muncul saat ada perubahan data."
      />
    );
  }

  return (
    <div className="rounded-2xl bg-white shadow-card ring-1 ring-neutral-100 overflow-hidden">
      <div className="overflow-x-auto max-h-[600px] overflow-y-auto">
        <table className="w-full text-left text-sm">
          <thead className="sticky top-0 z-10">
            <tr className="border-b border-neutral-100 bg-neutral-50">
              <th className="px-6 py-3 font-semibold text-neutral-500">Waktu</th>
              <th className="px-6 py-3 font-semibold text-neutral-500">User</th>
              <th className="px-6 py-3 font-semibold text-neutral-500">Aksi</th>
              <th className="px-6 py-3 font-semibold text-neutral-500">Entity</th>
              <th className="px-6 py-3 font-semibold text-neutral-500">Detail</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((log, idx) => (
              <tr
                key={log.id || idx}
                className={`border-b border-neutral-50 ${
                  idx % 2 === 0 ? 'bg-white' : 'bg-neutral-50/40'
                }`}
              >
                <td className="whitespace-nowrap px-6 py-3 text-neutral-500">
                  {formatDateTime(log.createdAt || log.timestamp)}
                </td>
                <td className="px-6 py-3 font-medium text-neutral-700">
                  {log.username || log.performedBy || '-'}
                </td>
                <td className="px-6 py-3">
                  <span className="inline-flex rounded-md bg-primary-50 px-2 py-0.5 text-xs font-semibold text-primary-700">
                    {log.action || '-'}
                  </span>
                </td>
                <td className="px-6 py-3 text-neutral-600">{log.entityType || log.entity || '-'}</td>
                <td className="max-w-xs truncate px-6 py-3 text-neutral-500">
                  {log.detail || log.description || '-'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function LoginTable({ logs, formatDateTime }) {
  if (logs.length === 0) {
    return (
      <EmptyState
        icon={LogIn}
        title="Tidak ada log login"
        description="Log login akan muncul saat ada aktivitas login."
      />
    );
  }

  return (
    <div className="rounded-2xl bg-white shadow-card ring-1 ring-neutral-100 overflow-hidden">
      <div className="overflow-x-auto max-h-[600px] overflow-y-auto">
        <table className="w-full text-left text-sm">
          <thead className="sticky top-0 z-10">
            <tr className="border-b border-neutral-100 bg-neutral-50">
              <th className="px-6 py-3 font-semibold text-neutral-500">Waktu</th>
              <th className="px-6 py-3 font-semibold text-neutral-500">Username</th>
              <th className="px-6 py-3 font-semibold text-neutral-500">Role</th>
              <th className="px-6 py-3 font-semibold text-neutral-500">IP Address</th>
              <th className="px-6 py-3 font-semibold text-neutral-500">User Agent</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((log, idx) => (
              <tr
                key={log.id || idx}
                className={`border-b border-neutral-50 ${
                  idx % 2 === 0 ? 'bg-white' : 'bg-neutral-50/40'
                }`}
              >
                <td className="whitespace-nowrap px-6 py-3 text-neutral-500">
                  {formatDateTime(log.createdAt || log.timestamp || log.loginTime)}
                </td>
                <td className="px-6 py-3 font-medium text-neutral-700">{log.username || '-'}</td>
                <td className="px-6 py-3">
                  <span
                    className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                      log.role === 'ADMIN'
                        ? 'bg-purple-50 text-purple-700'
                        : 'bg-blue-50 text-blue-700'
                    }`}
                  >
                    {log.role || '-'}
                  </span>
                </td>
                <td className="px-6 py-3 font-mono text-xs text-neutral-500">
                  {log.ipAddress || log.ip || '-'}
                </td>
                <td className="max-w-xs truncate px-6 py-3 text-xs text-neutral-400">
                  {log.userAgent || '-'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
