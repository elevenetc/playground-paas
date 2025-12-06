import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { projectsApi } from '../api/projects';
import type { Project } from '../types';

interface ProjectListProps {
  selectedProjectId: string | null;
  onSelectProject: (projectId: string) => void;
}

export function ProjectList({ selectedProjectId, onSelectProject }: ProjectListProps) {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newProjectName, setNewProjectName] = useState('');
  const [newProjectDescription, setNewProjectDescription] = useState('');
  const queryClient = useQueryClient();

  const { data: projects = [], isLoading } = useQuery({
    queryKey: ['projects'],
    queryFn: projectsApi.getAll,
  });

  const createMutation = useMutation({
    mutationFn: projectsApi.create,
    onSuccess: (newProject) => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      setIsModalOpen(false);
      setNewProjectName('');
      setNewProjectDescription('');
      onSelectProject(newProject.id);
    },
  });

  const handleCreate = () => {
    if (!newProjectName.trim()) return;
    createMutation.mutate({
      name: newProjectName,
      description: newProjectDescription || null,
    });
  };

  const handleClose = () => {
    setIsModalOpen(false);
    setNewProjectName('');
    setNewProjectDescription('');
  };

  if (isLoading) {
    return <div className="p-4 text-gray-500">Loading projects...</div>;
  }

  return (
    <>
      <div className="h-full flex flex-col bg-gray-50 border-r border-gray-200">
        <div className="p-4 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-800">Projects</h2>
        </div>

        <div className="flex-1 overflow-y-auto">
          {projects.map((project: Project) => (
            <div
              key={project.id}
              onClick={() => onSelectProject(project.id)}
              className={`
                px-4 py-3 cursor-pointer border-b border-gray-100 hover:bg-gray-100 transition-colors
                ${selectedProjectId === project.id ? 'bg-blue-50 border-l-4 border-l-blue-500' : ''}
              `}
            >
              <div className="font-medium text-gray-800">{project.name}</div>
              {project.description && (
                <div className="text-sm text-gray-500 mt-1 truncate">{project.description}</div>
              )}
            </div>
          ))}

          {projects.length === 0 && (
            <div className="p-4 text-center text-gray-500">No projects yet</div>
          )}
        </div>

        <div className="p-4 border-t border-gray-200">
          <button
            onClick={() => setIsModalOpen(true)}
            className="w-full px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
          >
            + Add Project
          </button>
        </div>
      </div>

      {/* Modal */}
      {isModalOpen && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
          onClick={handleClose}
        >
          <div
            className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-xl font-semibold text-gray-800 mb-4">Create New Project</h3>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Project Name *
                </label>
                <input
                  type="text"
                  placeholder="Enter project name"
                  value={newProjectName}
                  onChange={(e) => setNewProjectName(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && handleCreate()}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  autoFocus
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Description (optional)
                </label>
                <textarea
                  placeholder="Enter project description"
                  value={newProjectDescription}
                  onChange={(e) => setNewProjectDescription(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  rows={3}
                />
              </div>

              <div className="flex gap-3 pt-2">
                <button
                  onClick={handleCreate}
                  disabled={!newProjectName.trim() || createMutation.isPending}
                  className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
                >
                  {createMutation.isPending ? 'Creating...' : 'Create Project'}
                </button>
                <button
                  onClick={handleClose}
                  disabled={createMutation.isPending}
                  className="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300 disabled:opacity-50 transition-colors"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
