import { Handle, Position } from '@xyflow/react';
import type { Function as FunctionType, FunctionStatus } from '../types';

interface FunctionNodeData {
  function: FunctionType;
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

  return (
    <div
      className={`
        px-3 py-2 rounded-lg border-2 shadow-md
        ${statusColor}
      `}
    >
      <Handle type="target" position={Position.Top} />

      <pre className="text-xs font-mono whitespace-pre-wrap max-w-[400px]">
        {func.sourceCode}
      </pre>

      <Handle type="source" position={Position.Bottom} />
    </div>
  );
}
