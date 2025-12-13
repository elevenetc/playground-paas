import {useCallback, useEffect, useState} from 'react';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {
  addEdge,
  Background,
  BackgroundVariant,
  type Connection,
  Controls,
  type Edge,
  type Node,
  ReactFlow,
  useEdgesState,
  useNodesState,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import {functionsApi} from '../api/functions';
import {FunctionNode} from './FunctionNode';
import {ResultNode} from './ResultNode';
import {SourceModal} from './SourceModal';
import {TextModal} from './TextModal';
import type {Function as FunctionType} from '../types';

interface FunctionGraphProps {
  projectId: string;
}

const nodeTypes = {
  functionNode: FunctionNode,
  resultNode: ResultNode,
};

export function FunctionGraph({ projectId }: FunctionGraphProps) {
  const [isCreating, setIsCreating] = useState(false);
  const [newFunctionName, setNewFunctionName] = useState('');
  const [newFunctionCode, setNewFunctionCode] = useState('');
  const [sourceModalOpen, setSourceModalOpen] = useState(false);
  const [debugSource, setDebugSource] = useState<string | null>(null);
  const [sourceModalTitle, setSourceModalTitle] = useState('Generated Source');
  const [stateModalOpen, setStateModalOpen] = useState(false);
  const [stateContent, setStateContent] = useState<string | null>(null);
  const [stateModalTitle, setStateModalTitle] = useState('Function State');
  const [executionResults, setExecutionResults] = useState<Record<string, string>>({});
  const [executionErrors, setExecutionErrors] = useState<Record<string, string>>({});
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
    },
  });

  const executeMutation = useMutation({
    mutationFn: (functionId: string) => functionsApi.execute(projectId, functionId),
    onSuccess: (data, functionId) => {
      if (data.result) {
        // Clear any previous error and set result
        setExecutionErrors(prev => {
          const newErrors = { ...prev };
          delete newErrors[functionId];
          return newErrors;
        });
        setExecutionResults(prev => ({ ...prev, [functionId]: data.result! }));
      } else if (data.error) {
        // Clear any previous result and set error
        setExecutionResults(prev => {
          const newResults = { ...prev };
          delete newResults[functionId];
          return newResults;
        });
        setExecutionErrors(prev => ({ ...prev, [functionId]: data.error! }));
      }
    },
    onError: (error: any, functionId) => {
      // Handle network/API errors
      setExecutionResults(prev => {
        const newResults = { ...prev };
        delete newResults[functionId];
        return newResults;
      });
      setExecutionErrors(prev => ({ ...prev, [functionId]: error.message || 'Execution failed' }));
    },
  });

  const getSourceMutation = useMutation({
    mutationFn: (functionId: string) => functionsApi.getDebugSource(projectId, functionId),
    onSuccess: (data) => {
      setDebugSource(data);
      setSourceModalTitle('Generated Application Source');
      setSourceModalOpen(true);
    },
  });

  const getFunctionSourceMutation = useMutation({
    mutationFn: (functionId: string) => functionsApi.getFunctionSource(projectId, functionId),
    onSuccess: (data) => {
      setDebugSource(data);
      setSourceModalTitle('Generated User Function Source');
      setSourceModalOpen(true);
    },
  });

  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [nodePositions, setNodePositions] = useState<Record<string, { x: number; y: number }>>({});

  const handleRun = useCallback((functionId: string) => {
    executeMutation.mutate(functionId);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleDelete = useCallback((functionId: string) => {
    deleteMutation.mutate(functionId);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleGetSource = useCallback((functionId: string) => {
    getSourceMutation.mutate(functionId);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleGetFunctionSource = useCallback((functionId: string) => {
    getFunctionSourceMutation.mutate(functionId);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleGetState = useCallback((functionId: string) => {
    const func = functions.find((f: FunctionType) => f.id === functionId);
    if (func) {
      const statusInfo = `Status: ${func.status}${func.errorMessage ? '\n\nError:\n' + func.errorMessage : ''}`;
      setStateContent(statusInfo);
      setStateModalTitle(`Function State: ${func.name}`);
      setStateModalOpen(true);
    }
  }, [functions]);

  // Update nodes and edges when functions change
  useEffect(() => {
    const newNodes: Node[] = [];
    const newEdges: Edge[] = [];

    functions.forEach((func: FunctionType, index: number) => {
      const functionNodeId = func.id;
      const resultNodeId = `${func.id}-result`;
      const baseX = 100 + (index % 3) * 450;
      const baseY = 100 + Math.floor(index / 3) * 200;

      // Create function node
      newNodes.push({
        id: functionNodeId,
        type: 'functionNode',
        position: { x: baseX, y: baseY },
        data: {
          function: func,
          onRun: handleRun,
          onDelete: handleDelete,
          onGetSource: handleGetSource,
          onGetFunctionSource: handleGetFunctionSource,
          onGetState: handleGetState,
        },
      });

      // Create result node (positioned to the right of function)
      newNodes.push({
        id: resultNodeId,
        type: 'resultNode',
        position: { x: baseX + 280, y: baseY },
        data: {
          result: executionResults[func.id] || null,
          error: executionErrors[func.id] || null,
        },
        draggable: true,
      });

      // Create edge from function to result
      newEdges.push({
        id: `${functionNodeId}-to-${resultNodeId}`,
        source: functionNodeId,
        target: resultNodeId,
        sourceHandle: 'result',
        type: 'straight',
        animated: false,
        style: { stroke: '#94a3b8', strokeWidth: 2 },
      });
    });

    setNodes(newNodes);
    setEdges(newEdges);
  }, [functions, setNodes, setEdges, handleRun, handleDelete, handleGetSource, handleGetFunctionSource, handleGetState, executionResults, executionErrors]);

  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  // Handle node drag to move result nodes with function nodes
  const onNodeDragStop = useCallback(
    (_event: any, node: Node) => {
      // If a function node was dragged, update its result node position
      if (node.type === 'functionNode') {
        const resultNodeId = `${node.id}-result`;
        setNodes((nds) =>
          nds.map((n) => {
            if (n.id === resultNodeId) {
              return {
                ...n,
                position: {
                  x: node.position.x + 280,
                  y: node.position.y,
                },
              };
            }
            return n;
          })
        );
      }
    },
    [setNodes]
  );

  const handleCreate = () => {
    if (!newFunctionCode.trim()) return;
    createMutation.mutate({
      name: newFunctionName || undefined,
      sourceCode: newFunctionCode,
    });
  };


  if (isLoading) {
    return (
      <div className="h-full flex items-center justify-center text-gray-500">
        Loading functions...
      </div>
    );
  }

  return (
    <>
      <SourceModal
        isOpen={sourceModalOpen}
        onClose={() => {
          setSourceModalOpen(false);
          setDebugSource(null);
        }}
        source={debugSource}
        loading={getSourceMutation.isPending || getFunctionSourceMutation.isPending}
        title={sourceModalTitle}
      />

      <TextModal
          isOpen={stateModalOpen}
          onClose={() => {
            setStateModalOpen(false);
            setStateContent(null);
          }}
          content={stateContent}
          title={stateModalTitle}
      />

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
            onNodeDragStop={onNodeDragStop}
            nodeTypes={nodeTypes}
            fitView
          >
            <Background variant={BackgroundVariant.Dots} />
            <Controls />
          </ReactFlow>
        )}
      </div>
    </div>
    </>
  );
}
