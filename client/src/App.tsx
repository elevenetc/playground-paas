import { useState } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ProjectList } from './components/ProjectList';
import { FunctionGraph } from './components/FunctionGraph';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

function App() {
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null);

  return (
    <QueryClientProvider client={queryClient}>
      <div className="h-screen flex flex-col">
        <header className="bg-blue-600 text-white px-6 py-4 shadow-md">
          <h1 className="text-2xl font-bold">Kotlin Function-as-a-Service Platform</h1>
          <p className="text-sm text-blue-100 mt-1">
            Create and manage Kotlin functions in containerized environments
          </p>
        </header>

        <div className="flex-1 flex overflow-hidden">
          <div className="w-80">
            <ProjectList
              selectedProjectId={selectedProjectId}
              onSelectProject={setSelectedProjectId}
            />
          </div>

          <div className="flex-1">
            {selectedProjectId ? (
              <FunctionGraph projectId={selectedProjectId} />
            ) : (
              <div className="h-full flex items-center justify-center text-gray-400 text-lg">
                Select a project to view its functions
              </div>
            )}
          </div>
        </div>
      </div>
    </QueryClientProvider>
  );
}

export default App;
