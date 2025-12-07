import { Handle, Position } from '@xyflow/react';

interface ResultNodeData {
  result?: string | null;
  error?: string | null;
}

interface ResultNodeProps {
  data: ResultNodeData;
}

export function ResultNode({ data }: ResultNodeProps) {
  const hasResult = data.result || data.error;
  const isError = !!data.error;

  return (
    <div
      className={`
        px-3 py-2 rounded-lg border-2 shadow-md min-w-[120px]
        ${isError ? 'bg-red-50 border-red-500 text-red-800' :
          hasResult ? 'bg-green-50 border-green-500 text-green-800' :
          'bg-gray-50 border-gray-300 text-gray-500'}
      `}
    >
      <Handle type="target" position={Position.Left} />

      <div className="text-xs font-semibold mb-1">
        {isError ? 'Error' : 'Result'}
      </div>

      {hasResult ? (
        <div className="text-xs font-mono break-all">
          {data.error || data.result}
        </div>
      ) : (
        <div className="text-xs italic">
          Not computed
        </div>
      )}
    </div>
  );
}
