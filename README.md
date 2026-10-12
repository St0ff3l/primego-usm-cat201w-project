# PrimeGo

PrimeGo is a USM CAT201 e-commerce project. The customer-facing Vue app is a page-by-page migration of the existing PrimeGo JSP UI. The original JSP pages remain the reference for layout and behavior, and the existing Servlet/DAO backend continues to handle business data and actions during the migration.

## Application layout

- `backend/` is the RuoYi-Vue-Plus 6.x backend (Spring Boot, Sa-Token, MyBatis-Plus, MySQL, Redis/Redisson). Its system module provides administrator and staff user, role, department, menu, and permission management.
- `admin-ui/` is the matching RuoYi-Vue-Plus Vue 3 + TypeScript administration frontend.
- `frontend/` is the PrimeGo customer-facing Vue 3 app. Migrated pages keep their original JSP paths; pages not migrated yet open the original JSP/Servlet app.
- `src/main/` is the original JSP/Servlet UI and business backend. Its pages and DAOs remain in place while Vue takes over each page.

The framework's `sys_user` accounts are for system operators. PrimeGo customer and merchant accounts are separate business identities; their legacy registration/profile flows have not been migrated yet.

## Server database and Redis

MySQL and Redis are supplied by the test/deployment server; this project does not start local database or Redis containers. Copy `.env.example` to `.env` and set the server connection values. Import `backend/script/sql/ry_vue.sql` and then `backend/script/sql/primego_catalog.sql` into the target MySQL database before the first run. The first script seeds the RuoYi administration data; change the seeded administrator password after first login. The catalog script creates new category, product, and image tables; the legacy project did not include a tracked database schema or data export, so its data still needs a separate migration.

## Start the Vue app and legacy backend

Use Node.js 20.19 or newer. From the repository root, install the storefront dependencies once, then start the Vue app and the legacy Servlet app:

```sh
npm --prefix frontend ci
npm run dev
```

The root `npm run dev` starts Vue on `http://localhost:5173` and, when the ignored local `DBUtil.java` is present, starts the existing WAR on `http://localhost:8081`. If the root `.env` is complete, it also starts the separate RuoYi API on `http://localhost:8080`. The Vue API proxy uses the legacy app; unconverted page routes open that app directly so its forms and redirects keep working. Press Ctrl+C to stop the servers. The legacy app requires its local database configuration and schema; neither is included in a clean checkout.

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

The Vue app listens on `http://localhost:5173` and proxies data and cart requests to the legacy app on port `8081`. Set `VITE_LEGACY_API_TARGET` and `VITE_LEGACY_APP_URL` in `frontend/.env` if the legacy app runs elsewhere.

## Migration boundary

The Vue migration currently covers the home page, login, signup, catalog search, product detail, and cart. Customer profiles, order confirmation/history, wallet, merchant, and admin pages remain on the original JSP UI until their Vue replacements are ready. The old WAR relies on a local `DBUtil.java` excluded by `.gitignore`, and its database DDL is not present in the repository, so it cannot be rebuilt from a clean checkout yet. The RuoYi administration frontend and backend remain separate from this customer UI migration.
