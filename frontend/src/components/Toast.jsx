import React, { createContext, useCallback, useContext, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { CheckCircle, XCircle, Info, X } from 'lucide-react';

const ToastContext = createContext(null);

const ICONS = {
  success: CheckCircle,
  error: XCircle,
  info: Info,
};

const STYLES = {
  success: {
    bg: 'bg-green-50 ring-green-200',
    icon: 'text-green-600',
    text: 'text-green-800',
  },
  error: {
    bg: 'bg-red-50 ring-red-200',
    icon: 'text-red-600',
    text: 'text-red-800',
  },
  info: {
    bg: 'bg-blue-50 ring-blue-200',
    icon: 'text-blue-600',
    text: 'text-blue-800',
  },
};

const AUTO_DISMISS_MS = 4000;

function ToastItem({ id, type, message, onDismiss }) {
  const style = STYLES[type] || STYLES.info;
  const Icon = ICONS[type] || Info;

  React.useEffect(() => {
    const timer = setTimeout(() => onDismiss(id), AUTO_DISMISS_MS);
    return () => clearTimeout(timer);
  }, [id, onDismiss]);

  return (
    <motion.div
      layout
      initial={{ opacity: 0, x: 80 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: 80 }}
      transition={{ duration: 0.25, ease: 'easeOut' }}
      className={`pointer-events-auto flex w-80 items-start gap-3 rounded-lg p-4 shadow-lg ring-1 ${style.bg}`}
    >
      <Icon className={`mt-0.5 h-5 w-5 flex-shrink-0 ${style.icon}`} />
      <p className={`flex-1 text-sm font-medium ${style.text}`}>{message}</p>
      <button
        onClick={() => onDismiss(id)}
        className="rounded p-0.5 text-neutral-400 transition-colors hover:text-neutral-600"
      >
        <X className="h-4 w-4" />
      </button>
    </motion.div>
  );
}

let nextId = 0;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const dismiss = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const addToast = useCallback((message, type = 'info') => {
    const id = ++nextId;
    setToasts((prev) => [...prev, { id, message, type }]);
  }, []);

  const toast = React.useMemo(
    () => ({
      success: (msg) => addToast(msg, 'success'),
      error: (msg) => addToast(msg, 'error'),
      info: (msg) => addToast(msg, 'info'),
    }),
    [addToast],
  );

  return (
    <ToastContext.Provider value={toast}>
      {children}

      {/* Toast container */}
      <div className="pointer-events-none fixed right-4 top-4 z-[100] flex flex-col gap-2">
        <AnimatePresence mode="popLayout">
          {toasts.map((t) => (
            <ToastItem
              key={t.id}
              id={t.id}
              type={t.type}
              message={t.message}
              onDismiss={dismiss}
            />
          ))}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return ctx;
}
