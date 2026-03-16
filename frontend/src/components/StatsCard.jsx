import React from 'react';
import { motion } from 'framer-motion';

export default function StatsCard({ icon: Icon, label, value, trend }) {
  const trendIsPositive = trend != null && trend >= 0;

  return (
    <motion.div
      className="interactive-card rounded-xl bg-white p-5 shadow-sm ring-1 ring-neutral-100"
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.3, ease: 'easeOut' }}
    >
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-medium text-neutral-500">{label}</p>
          <p className="mt-1 text-2xl font-bold text-neutral-900">{value}</p>
        </div>
        {Icon && (
          <div className="rounded-lg bg-primary-50 p-2.5">
            <Icon className="h-5 w-5 text-primary-600" />
          </div>
        )}
      </div>

      {trend != null && (
        <p
          className={`mt-3 text-xs font-medium ${
            trendIsPositive ? 'text-green-600' : 'text-red-500'
          }`}
        >
          {trendIsPositive ? '+' : ''}
          {trend}%{' '}
          <span className="text-neutral-400">dari periode sebelumnya</span>
        </p>
      )}
    </motion.div>
  );
}
