import { useCallback, useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ReactFlow,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
  BackgroundVariant,
  type Node,
  type Edge,
  type Connection,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { functionsApi } from '../api/functions';
import { FunctionNode } from './FunctionNode';
import type { Function as FunctionType } from '../types';

interface FunctionGraphProps {
  projectId: string;
}

const nodeTypes = {
  functionNode: FunctionNode,
};

export function FunctionGraph({ projectId }: FunctionGraphProps) {
  const [isCreating, setIsCreating] = useState(false);
  const [newFunctionName, setNewFunctionName] = useState('');
  const [newFunctionCode, setNewFunctionCode] = useState('');
  const [contextMenu, setContextMenu] = useState<{
    x: number;
    y: number;
    nodeId: string;
  } | null>(null);
  const queryClient = useQueryClient();

  const { data: functions = [], isLoading } = useQuery({
    queryKey: ['functions', projectId],
    queryFn: () => functionsApi.getByProjectId(projectId),
    enabled: !!projectId,
  });

  const createMutation = useMutation({
    mutationFn: (data: { name?: string; sourceCode: string }) =>
      functionsApi.create(projectId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['functions', projectId] });
      setIsCreating(false);
      setNewFunctionName('');
      setNewFunctionCode('');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (functionId: string) => functionsApi.delete(projectId, functionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['functions', projectId] });
      setContextMenu(null);
    },
  });

  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);

  // Update nodes when functions change
  useEffect(() => {
    const newNodes: Node[] = functions.map((func: FunctionType, index: number) => ({
      id: func.id,
      type: 'functionNode',
      position: { x: 100 + (index % 3) * 300, y: 100 + Math.floor(index / 3) * 200 },
      data: { function: func },
    }));
    setNodes(newNodes);
  }, [functions, setNodes]);

  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  const handleCreate = () => {
    if (!newFunctionCode.trim()) return;
    createMutation.mutate({
      name: newFunctionName || undefined,
      sourceCode: newFunctionCode,
    });
  };

  const handleNodeContextMenu = useCallback(
    (event: React.MouseEvent, node: Node) => {
      event.preventDefault();
      setContextMenu({
        x: event.clientX,
        y: event.clientY,
        nodeId: node.id,
      });
    },
    []
  );

  const handleDelete = () => {
    if (contextMenu) {
      deleteMutation.mutate(contextMenu.nodeId);
    }
  };

  // Close context menu on click outside
  useEffect(() => {
    const handleClick = () => setContextMenu(null);
    if (contextMenu) {
      document.addEventListener('click', handleClick);
      return () => document.removeEventListener('click', handleClick);
    }
  }, [contextMenu]);

  if (isLoading) {
    return (
      <div className="h-full flex items-center justify-center text-gray-500">
        Loading functions...
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col bg-white">
      <div className="flex items-center justify-between p-4 border-b border-gray-200">
        <h2 className="text-lg font-semibold text-gray-800">Function Graph</h2>
        <button
          onClick={() => setIsCreating(true)}
          className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 transition-colors"
        >
          + Add Function
        </button>
      </div>

      {isCreating && (
        <div className="p-4 border-b border-gray-200 bg-gray-50">
          <div className="space-y-2">
            <input
              type="text"
              placeholder="Function name (optional, will be extracted from code)"
              value={newFunctionName}
              onChange={(e) => setNewFunctionName(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
            />
            <textarea
              placeholder="Source code (e.g., fun add(a: Int, b: Int): Int = a + b)"
              value={newFunctionCode}
              onChange={(e) => setNewFunctionCode(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 font-mono text-sm"
              rows={4}
              autoFocus
            />
            <div className="flex gap-2">
              <button
                onClick={handleCreate}
                disabled={!newFunctionCode.trim() || createMutation.isPending}
                className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
              >
                {createMutation.isPending ? 'Creating...' : 'Create'}
              </button>
              <button
                onClick={() => {
                  setIsCreating(false);
                  setNewFunctionName('');
                  setNewFunctionCode('');
                }}
                className="px-4 py-2 bg-gray-300 text-gray-700 rounded-md hover:bg-gray-400 transition-colors"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="flex-1">
        {functions.length === 0 ? (
          <div className="h-full flex items-center justify-center text-gray-500">
            No functions yet. Click "Add Function" to create one.
          </div>
        ) : (
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onNodeContextMenu={handleNodeContextMenu}
            nodeTypes={nodeTypes}
            fitView
          >
            <Background variant={BackgroundVariant.Dots} />
            <Controls />
          </ReactFlow>
        )}
      </div>

      {/* Context Menu */}
      {contextMenu && (
        <div
          className="fixed bg-white rounded-md shadow-lg border border-gray-200 py-1 z-50"
          style={{ left: contextMenu.x, top: contextMenu.y }}
          onClick={(e) => e.stopPropagation()}
        >
          <button
            onClick={handleDelete}
            disabled={deleteMutation.isPending}
            className="w-full px-4 py-2 text-left text-red-600 hover:bg-red-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
          </button>
        </div>
      )}
    </div>
  );
}
