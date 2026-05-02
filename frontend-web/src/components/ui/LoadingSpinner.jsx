/*
 * This file provides reusable UI behavior for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
export function LoadingSpinner({ size = 'md', className = '' }) {
  const sizes = { sm: 'h-4 w-4', md: 'h-6 w-6', lg: 'h-10 w-10' };
  return (
    <div className={`flex items-center justify-center ${className}`}>
      <div className={`${sizes[size]} animate-spin rounded-full border-2 border-slate-200 border-t-brand dark:border-slate-700 dark:border-t-brand-400`} />
    </div>
  );
}

// Performs the page loader workflow so callers do not duplicate this logic.
export function PageLoader() {
  return (
    <div className="flex min-h-[40vh] items-center justify-center">
      <div className="text-center">
        <LoadingSpinner size="lg" />
        <p className="mt-4 text-sm text-slate-500 dark:text-slate-400">Loading...</p>
      </div>
    </div>
  );
}

// Defines empty state so related behavior stays grouped in one place.
export function EmptyState({ icon, title, description }) {
  return (
    <div className="flex min-h-[30vh] items-center justify-center">
      <div className="text-center">
        {icon && <div className="mx-auto mb-4 text-slate-300 dark:text-slate-600">{icon}</div>}
        <h3 className="font-display text-lg font-semibold text-slate-700 dark:text-slate-300">{title}</h3>
        {description && <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">{description}</p>}
      </div>
    </div>
  );
}
