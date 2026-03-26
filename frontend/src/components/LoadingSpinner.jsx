import React from 'react';

export default function LoadingSpinner() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4">
      <div className="h-12 w-12 animate-spin rounded-full border-4 border-primary-200 border-t-primary-600" />
      <p className="text-sm font-medium text-neutral-500">Memuat...</p>
    </div>
  );
}
