# PrimeGo

PrimeGo is a USM CAT201 e-commerce project. The repository is moving from the original JSP/Servlet application to a separated Vue and API architecture, using RuoYi-Vue-Plus as the backend administration foundation.

## Application layout

- `backend/` is the RuoYi-Vue-Plus 6.x backend (Spring Boot, Sa-Token, MyBatis-Plus, MySQL, Redis/Redisson). Its system module provides administrator and staff user, role, department, menu, and permission management.
- `admin-ui/` is the matching RuoYi-Vue-Plus Vue 3 + TypeScript administration frontend.
- `frontend/` is the PrimeGo customer-facing Vue 3 storefront, connected to the public product catalog API.
- `src/main/` is the original JSP/Servlet application, retained while its business flows are migrated in stages.

The framework's `sys_user` accounts are for system operators. PrimeGo customer and merchant accounts are separate business identities; their legacy registration/profile flows have not been migrated yet.

## Server database and Redis

MySQL and Redis are supplied by the test/deployment server; this project does not start local database or Redis containers. Copy `.env.example` to `.env` and set the server connection values. Import `backend/script/sql/ry_vue.sql` and then `backend/script/sql/primego_catalog.sql` into the target MySQL database before the first run. The first script seeds the RuoYi administration data; change the seeded administrator password after first login. The catalog script creates new category, product, and image tables; the legacy project did not include a tracked database schema or data export, so its data still needs a separate migration.

## Start the storefront and API together

Use Node.js 20.19 or newer. From the repository root, install the storefront dependencies once, then start the storefront without connecting to any database:

```sh
npm --prefix frontend ci
npm run dev
```

The root `npm run dev` starts the storefront on `http://localhost:5173`; press Ctrl+C to stop it. Without complete server settings, the page runs without an API, so product and category data will not load. To start the API too, use JDK 21 or newer and Maven, copy `.env.example` to `.env`, fill in the test server's MySQL and Redis connection values, then run `npm run dev` again. It will build the backend and start the API on `http://localhost:8080`. Import `backend/script/sql/ry_vue.sql` and `backend/script/sql/primego_catalog.sql` into the server database before starting the API. Do not commit `.env`.

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

The storefront listens on `http://localhost:5173`. Its category, product search/filter, and product detail requests use the public Plus API; Vite proxies those requests to `VITE_API_TARGET` (copy `frontend/.env.example` to `frontend/.env` and set the backend address if needed). Category and product reads are cached in the configured server Redis instance.

## Migration boundary

The new Plus administration and catalog API run independently from the legacy WAR. Login, customer/merchant profiles, cart, orders, wallet, and the rest of the JSP/Servlet flows remain to be migrated. The old WAR also relies on a local `DBUtil.java` excluded by `.gitignore`, and its database DDL is not present in the repository, so it cannot be rebuilt from a clean checkout yet.
