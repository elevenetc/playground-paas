interface SourceModalProps {
  isOpen: boolean;
  onClose: () => void;
  source: string | null;
  loading: boolean;
  title?: string;
}

export function SourceModal({ isOpen, onClose, source, loading, title = "Generated Source" }: SourceModalProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div
        className="absolute inset-0 bg-black/50"
        onClick={onClose}
      />
      <div className="relative bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[80vh] flex flex-col">
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <h2 className="text-xl font-semibold">{title}</h2>
          <button
            onClick={onClose}
            className="text-gray-500 hover:text-gray-700 text-2xl leading-none"
          >
            ×
          </button>
        </div>
        <div className="flex-1 overflow-auto p-6">
          {loading ? (
            <div className="flex items-center justify-center h-full">
              <div className="text-gray-500">Loading source code...</div>
            </div>
          ) : source ? (
            <pre className="text-sm font-mono bg-gray-50 p-4 rounded border border-gray-200 overflow-auto">
              {source}
            </pre>
          ) : (
            <div className="flex items-center justify-center h-full">
              <div className="text-red-500">Failed to load source code</div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
