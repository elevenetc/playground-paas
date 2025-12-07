import { useState } from 'react';
import { Handle, Position } from '@xyflow/react';
import type { Function as FunctionType, FunctionStatus } from '../types';

interface FunctionNodeData {
  function: FunctionType;
  onRun: (functionId: string) => void;
  onDelete: (functionId: string) => void;
  onGetSource: (functionId: string) => void;
  onGetFunctionSource: (functionId: string) => void;
}

interface FunctionNodeProps {
  data: FunctionNodeData;
}

const STATUS_COLORS: Record<FunctionStatus, string> = {
  PENDING: 'bg-yellow-100 border-yellow-500 text-yellow-800',
  COMPILING: 'bg-blue-100 border-blue-500 text-blue-800',
  READY: 'bg-green-100 border-green-500 text-green-800',
  FAILED: 'bg-red-100 border-red-500 text-red-800',
  STOPPED: 'bg-gray-100 border-gray-500 text-gray-800',
  STOPPING: 'bg-orange-100 border-orange-500 text-orange-800',
  DELETING: 'bg-purple-100 border-purple-500 text-purple-800',
};

export function FunctionNode({ data }: FunctionNodeProps) {
  const func = data.function;
  const statusColor = STATUS_COLORS[func.status];
  const [menuOpen, setMenuOpen] = useState(false);

  const handleRun = (e: React.MouseEvent) => {
    e.stopPropagation();
    data.onRun(func.id);
  };

  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation();
    setMenuOpen(false);
    data.onDelete(func.id);
  };

  const handleGetSource = (e: React.MouseEvent) => {
    e.stopPropagation();
    setMenuOpen(false);
    data.onGetSource(func.id);
  };

  const handleGetFunctionSource = (e: React.MouseEvent) => {
    e.stopPropagation();
    setMenuOpen(false);
    data.onGetFunctionSource(func.id);
  };

  const toggleMenu = (e: React.MouseEvent) => {
    e.stopPropagation();
    setMenuOpen(!menuOpen);
  };

  return (
    <div
      className={`
        px-3 py-2 rounded-lg border-2 shadow-md
        ${statusColor}
      `}
    >
      <Handle type="target" position={Position.Top} />

      <pre className="text-xs font-mono whitespace-pre-wrap max-w-[400px] mb-2">
        {func.sourceCode}
      </pre>

      <div className="flex gap-2 items-center border-t border-current/20 pt-2">
        <button
          onClick={handleRun}
          disabled={func.status !== 'READY'}
          className="px-2 py-1 rounded hover:bg-black/10 disabled:opacity-50 disabled:cursor-not-allowed transition-colors text-lg"
          title="Run function"
        >
          ▶️
        </button>

        <div className="relative">
          <button
            onClick={toggleMenu}
            className="px-2 py-1 rounded hover:bg-black/10 transition-colors text-lg"
            title="Menu"
          >
            ⋮
          </button>

          {menuOpen && (
            <>
              <div
                className="fixed inset-0 z-10"
                onClick={() => setMenuOpen(false)}
              />
              <div className="absolute left-0 top-full mt-1 bg-white rounded-md shadow-lg border border-gray-200 py-1 z-20 min-w-[180px]">
                <button
                  onClick={handleGetSource}
                  className="w-full px-4 py-2 text-left hover:bg-gray-100 transition-colors text-sm"
                >
                  Get execution source
                </button>
                <button
                  onClick={handleGetFunctionSource}
                  className="w-full px-4 py-2 text-left hover:bg-gray-100 transition-colors text-sm"
                >
                  Get function source
                </button>
                <button
                  onClick={handleDelete}
                  className="w-full px-4 py-2 text-left text-red-600 hover:bg-red-50 transition-colors text-sm"
                >
                  Delete
                </button>
              </div>
            </>
          )}
        </div>
      </div>

      <Handle type="source" position={Position.Right} id="result" />
      <Handle type="source" position={Position.Bottom} />
    </div>
  );
}
