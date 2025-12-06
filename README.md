# PaaS Playground

## Quick Start

1. Copy `.env.example` to `.env`
2. Start docker
   ```bash
   docker-compose up -d
   ```
3. Run backend
   ```bash
   ./gradlew :foundation:run
   ```
   
4. Build client
   ```bash
   npm install
   ```

5. Run client
   ```bash
   npm run dev
   ```