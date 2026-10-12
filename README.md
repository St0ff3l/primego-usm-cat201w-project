# PrimeGo

PrimeGo is a USM CAT201 e-commerce project. The PrimeGo storefront is implemented in Vue 3, preserving the original JSP screens' layouts and interactions. The Servlet/DAO application remains as the storefront's business API; the old JSP templates have been removed.

## Application layout

- `backend/` is the RuoYi-Vue-Plus 6.x backend (Spring Boot, Sa-Token, MyBatis-Plus, MySQL, Redis/Redisson). Its system module provides administrator and staff user, role, department, menu, and permission management.
- `admin-ui/` is the matching RuoYi-Vue-Plus Vue 3 + TypeScript administration frontend.
- `frontend/` is the PrimeGo customer-facing Vue 3 app. It owns the storefront routes and UI.
- `src/main/` contains the Servlet/DAO business API and static storefront assets. It no longer contains JSP views.

The framework's `sys_user` accounts are for system operators. PrimeGo customer and merchant accounts are separate business identities, handled by the storefront API.

## Server database and Redis

MySQL and Redis are supplied by the test/deployment server; this project does not start local database or Redis containers. Copy `.env.example` to `.env` and set the server connection values. Import `backend/script/sql/ry_vue.sql` and then `backend/script/sql/primego_catalog.sql` into the target MySQL database before the first run. The first script seeds the RuoYi administration data; change the seeded administrator password after first login. The catalog script creates new category, product, and image tables; the legacy project did not include a tracked database schema or data export, so its data still needs a separate migration.

## Start the Vue app and storefront API

Use Node.js 20.19 or newer. From the repository root, install the storefront dependencies once, then start the Vue app and the legacy Servlet app:

```sh
npm --prefix frontend ci
npm run dev
```

The root `npm run dev` starts Vue on `http://localhost:5173` and, when the ignored local `DBUtil.java` is present, starts the Servlet API WAR on `http://localhost:8081`. If the root `.env` is complete, it also starts the separate RuoYi API on `http://localhost:8080`. Press Ctrl+C to stop the servers. The Servlet API requires its local database configuration and schema; neither is included in a clean checkout.

## Run the backend manually

Use JDK 21 or newer and Maven. The development profile reads MySQL and Redis settings from the environment variables listed in `.env.example`.

```sh
cd backend
set -a
. ../.env
set +a
mvn -pl ruoyi-admin -am -DskipTests package
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

The API listens on port `8080`; it requires the configured server database and Redis to be reachable.

## Run the administration frontend

Use Node.js 20.19 or newer and pnpm 10 or newer:

```sh
cd admin-ui
pnpm install --frozen-lockfile
pnpm dev
```

The admin UI listens on `http://localhost:5174` and proxies API requests to `VITE_APP_PROXY_TARGET` (defaults to `http://localhost:8080`). Set it to the backend server URL when the UI and API run on different hosts. The RuoYi system user, role, and permission screens are available after database initialization and login.

## Run the PrimeGo storefront

```sh
cd frontend
npm install
npm run dev
```

The Vue app listens on `http://localhost:5173` and proxies data and action requests to the Servlet API on port `8081`. Set `VITE_LEGACY_API_TARGET` and `VITE_LEGACY_APP_URL` in `frontend/.env` if the API or its static assets run elsewhere.

## Storefront UI

All storefront screens, including customer, merchant, and store-admin flows, now render in Vue. Former JSP page URLs remain Vue route aliases for existing links. The JSP templates and JSP/JSTL dependencies are removed; the Servlet API and static assets remain. The WAR relies on a local `DBUtil.java` excluded by `.gitignore`, and its database DDL is not present in the repository, so it cannot be rebuilt from a clean checkout yet. The RuoYi administration frontend and backend remain separate from this storefront.
