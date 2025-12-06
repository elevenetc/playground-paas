# Kotlin FaaS Platform - Frontend Client

React + TypeScript frontend for the Kotlin Function-as-a-Service platform.

## Features

- **Project Management**: Create and view Kotlin function projects
- **Function Graph**: Visual representation of functions using ReactFlow
- **Real-time Updates**: React Query for efficient data fetching and caching
- **Type Safety**: Full TypeScript types matching backend Kotlin DTOs

## Tech Stack

- **React 18** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool and dev server
- **ReactFlow** - Interactive node-based graph visualization
- **React Query** - Server state management
- **Axios** - HTTP client

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- Backend server running on `http://localhost:8080`

### Installation

```bash
npm install
```

### Development

```bash
npm run dev
```

The app will be available at `http://localhost:5173`

### Build for Production

```bash
npm run build
```

## Project Structure

```
src/
├── api/              # API client and endpoint definitions
│   ├── client.ts     # Axios instance with interceptors
│   ├── projects.ts   # Project API calls
│   └── functions.ts  # Function API calls
├── components/       # React components
│   ├── ProjectList.tsx      # Sidebar project list
│   ├── FunctionGraph.tsx    # ReactFlow graph view
│   └── FunctionNode.tsx     # Custom node component
├── types/           # TypeScript type definitions
│   └── index.ts     # Types matching Kotlin DTOs
├── App.tsx          # Main application component
└── main.tsx         # Application entry point
```

## Configuration

Create a `.env` file:

```
VITE_API_URL=http://localhost:8080
```

## Features to Implement

- [ ] Function code editor with Monaco
- [ ] Function execution and logs viewer
- [ ] Visual function chaining (connections between nodes)
- [ ] Real-time status updates
- [ ] Function metrics dashboard
- [ ] Delete projects and functions
- [ ] Edit existing functions
