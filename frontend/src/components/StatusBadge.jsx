import React from 'react';

const STATUS_MAP = {
  OPEN: { label: 'Baru', className: 'badge-open' },
  ASSIGNED: { label: 'Ditugaskan', className: 'badge-assigned' },
  IN_PROGRESS: { label: 'Dikerjakan', className: 'badge-in-progress' },
  DONE: { label: 'Selesai', className: 'badge-done' },
  NEED_FOLLOWUP: { label: 'Follow Up', className: 'badge-need-followup' },
  CLOSED: { label: 'Ditutup', className: 'badge-closed' },
};

export default function StatusBadge({ status }) {
  const config = STATUS_MAP[status] || { label: status, className: 'badge' };

  return (
    <span className={`badge ${config.className} inline-flex items-center gap-1.5`}>
      <span className="inline-block h-2 w-2 rounded-full bg-current opacity-80" />
      {config.label}
    </span>
  );
}
