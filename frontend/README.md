# Wallet and Withdrawal System — Frontend

React frontend for the Wallet and Withdrawal System. It communicates with the Spring Boot backend and displays wallet, payout, and administrative workflows. The backend remains the source of truth for balances, payout configuration, authorization, and withdrawal state.

## Technology

- React 19
- JavaScript (ES modules)
- Vite
- Tailwind CSS 4
- Axios
- React Router
- Oxlint

## Prerequisites

- Node.js compatible with the Vite version in `package.json`
- npm
- The backend API running and reachable from the browser

## Setup

From the repository root:

```bash
cd frontend
npm ci
```

Create a local environment file:

```bash
cp .env.example .env
```

Set the API base URL in `frontend/.env`:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

Use the actual backend URL for your environment. Vite exposes variables prefixed with `VITE_` to client-side code, so never place secrets in this file.

## Run locally

```bash
npm run dev
```

Open the local URL printed by Vite. The backend must be running, and its CORS configuration must allow the frontend origin.

## Build and lint

```bash
npm run build
npm run lint
```

To preview the production build locally:

```bash
npm run preview
```

## Application areas

The source includes these screens and flows:

- Login
- Wallet balances and transaction information
- Payout and withdrawal flow
- Administrative withdrawal management
- Administrative wallet reconciliation

Routes and available actions are defined in `src/App.jsx` and the page components under `src/pages/`.

## Configuration and security

- `VITE_API_BASE_URL` must point to the backend API base URL.
- Do not commit `.env` files or place passwords, JWT secrets, database credentials, or private API keys in frontend environment variables.
- The frontend is not a security boundary. Every sensitive action and every ownership/role check must be enforced by the backend.
- Do not trust balances, payout values, withdrawal eligibility, or withdrawal status supplied by the browser.
