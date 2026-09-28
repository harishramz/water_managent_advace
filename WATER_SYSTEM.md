# WellSpring Water Management

## Existing application retained

The application keeps its React 19/Vite frontend, Spring Boot 4/JPA backend, and the existing MySQL `java_fullstack` database. Existing `users` and `products` tables are extended in place; no existing tables are dropped. New persistent tables cover addresses, cart items, orders, order items, payments, deliveries, notifications, and support requests.

## Local setup

Set secrets in the PowerShell session used to start the API. Do not put real values in source control:

```powershell
$env:DATABASE_URL = "jdbc:mysql://localhost:3306/java_fullstack"
$env:DATABASE_USERNAME = "root"
$env:DATABASE_PASSWORD = "<your-local-MySQL-password>"
$env:ADMIN_EMAIL = "admin@example.com"
$env:ADMIN_PASSWORD = "<at-least-12-characters>"
Set-Location H:\project_one\product-api
.\mvnw.cmd spring-boot:run
```

On startup, Hibernate's development `ddl-auto=update` adds the missing columns and tables without dropping current data. New installations also need the existing `java_fullstack` MySQL database created first. For production, replace automatic schema updates with reviewed Flyway/Liquibase migrations before deployment.

If the product table is empty, `CatalogBootstrap` inserts a starter catalog of packaged-water listings across Bisleri, Kinley, Aquafina, Bailley, Himalayan, Tata Copper+, Rail Neer, and Vedica. It inserts records into the existing `products` table, never returns fake API data, and skips seeding whenever products already exist. New local demo listings have 24 opening units so the customer purchase flow can be tested immediately; admins should replace the demo stock and suggested prices with verified business inventory/pricing before real use. Brand listings are not affiliated with the named manufacturers.

Set `ADMIN_EMAIL` and a 12-character minimum `ADMIN_PASSWORD` only in the backend process environment. If that email already belongs to a user, startup grants that account the `ADMIN` role; otherwise, startup creates the admin. There is no public admin registration path. Admin password changes for an existing account must be performed through a password-management process; the bootstrap variable does not overwrite it.

In a second terminal:

```powershell
Set-Location H:\project_one\java-fullstack-frontend
npm run dev -- --host 127.0.0.1
```

Open `http://localhost:5173/`. Vite proxies `/api` to the backend at `http://localhost:8080`.

## Vercel + public API deployment

The frontend and API must both be deployed. Vercel only hosts the React frontend; the local `localhost:8080` API is not reachable from public browsers. See the Railway example below to create the API and database, then set the Vercel environment variable to its public URL. Vercel reads `VITE_API_BASE_URL` at build time, so changing it requires a redeploy.

In Vercel, keep **Root Directory** at the repository root (the directory containing `vercel.json`). The checked-in Vercel config builds `java-fullstack-frontend` and rewrites client routes such as `/brands/Bisleri` to `index.html`.

The frontend no longer assumes the development-only Vite proxy exists in production. In local development with no `VITE_API_BASE_URL`, relative `/api` paths still use the Vite proxy to `localhost:8080`.

### Create the backend service (Railway example)

The API code is included in this repository but needs a public host and public database before Vercel can call it. The backend has `product-api/Dockerfile`; Railway detects and builds it when the service root directory is `/product-api`. You must be signed into Railway to create the cloud resources; never put provider credentials or database passwords in GitHub.

1. In Railway, create a project and add a MySQL database service. Wait for it to become available.
2. Add a service from the GitHub repository. Set its root directory to `/product-api`, select the Dockerfile builder, and deploy. In the service's Health Check settings, use `/api/products/brands` as the health-check path.
3. In the API service's Variables page, add the required database values by referencing Railway's MySQL service variables: `DATABASE_URL` as `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?serverTimezone=UTC`, `DATABASE_USERNAME` as `${{MySQL.MYSQLUSER}}`, and `DATABASE_PASSWORD` as `${{MySQL.MYSQLPASSWORD}}`. If Railway gives the database a different service name than `MySQL`, use that exact name in each reference. Enter database/admin passwords in Railway's secret fields, not in chat or source files.
4. Also set API service variables: `APP_CORS_ALLOWED_ORIGINS` to the exact Vercel origin (e.g. `https://your-site.vercel.app`), `SESSION_COOKIE_SAME_SITE=None`, `SESSION_COOKIE_SECURE=true`, `ADMIN_EMAIL` to the chosen admin email, and `ADMIN_PASSWORD` to a unique value at least 12 characters long.
5. In Railway service settings, generate a public domain for the API. The application reads Railway's `PORT` automatically.
6. Verify `https://<railway-api-domain>/api/products/brands` returns JSON. If it does not, inspect the Railway deploy/build logs and verify the MySQL variable references.
7. In Vercel Project Settings → Environment Variables, set `VITE_API_BASE_URL` to the API origin only (e.g. `https://<railway-api-domain>`, no `/api` suffix). Apply it to Production, then trigger a new Vercel deployment because Vite embeds this value at build time.
8. Test `/products`, `/brands`, registration, login, cart, and checkout. Confirm the browser accepts session cookies. Since Vercel and Railway default domains are different sites, some browser privacy settings can block cross-site cookies; a custom API domain under the same registrable domain as the frontend is more reliable.

Never put database/admin secrets in GitHub, Vercel frontend variables, or `VITE_*` variables. `VITE_API_BASE_URL` is public and is only the backend address. Keep schema auto-update for development only; review and migrate the database safely before production use.

## Customer journey

1. Open **Water**, choose a product from the database catalog, and add it to the cart.
2. Create a customer account with an email and a password of at least eight characters, then sign in.
3. In checkout, create or choose a delivery address and select cash on delivery or a test-mode payment method.
4. Review and place the order. The backend recalculates current product prices, checks stock, locks cart/product rows, records the order/payment/delivery, decrements stock, and clears the cart in one transaction.
5. Track order status under **Orders**; use the customer dashboard, profile, payments, notifications, and support request views as needed.

## Roles and payment limitations

- New public registrations are always `CUSTOMER`; passwords are BCrypt-hashed.
- A legacy plaintext password is upgraded to BCrypt when its owner next signs in successfully. Existing users are retained.
- Login uses an HTTP-only, same-site server session. Product creation, order administration, inventory, customer management, support administration, and delivery assignment are backend role-checked.
- Admins create delivery staff credentials in the Operations view. Delivery staff sign in through the same login page and receive only assigned deliveries.
- UPI, card, and online selections create a `PENDING` payment record only. No real payment is taken and no card data is collected. Cash-on-delivery records become paid when delivery is marked complete.
- The database password is intentionally not in the project configuration anymore. `DATABASE_PASSWORD` must be set before launching the MySQL-backed API.

## API surface

- `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout`
- `GET /api/products`, `GET /api/products/brands`, `GET /api/products/{id}`, `POST /api/products`, `PUT /api/products/{id}`, `DELETE /api/products/{id}`
- `GET /api/cart`, `POST /api/cart/items`, `PUT /api/cart/items/{id}`, `DELETE /api/cart/items/{id}`, `DELETE /api/cart`
- `GET|POST /api/addresses`, `PUT|DELETE /api/addresses/{id}`
- `POST /api/orders`, `GET /api/orders`, `GET /api/orders/{id}`, `POST /api/orders/{id}/cancel`, `POST /api/orders/{id}/reorder`
- `GET /api/payments`, `GET /api/dashboard/customer`, `PUT /api/users/me`
- `GET /api/notifications`, `PUT /api/notifications/{id}/read`, `GET|POST /api/support`
- Admin: `GET /api/admin/dashboard`, `/reports`, `/orders`, `/customers`, `/deliveries`, `/delivery-staff`, `/inventory`; related `PUT` and `POST` routes manage those records.
- Delivery staff: `GET /api/deliveries/assigned`, `PUT /api/deliveries/{id}/status`

The storefront routes `/brands` and `/brands/{brand}` browse active brands and their database-backed product counts. `/products` shows the current filtered product count and in-stock count. Customer cart lines, quantity totals, prices, stock and subtotal/delivery/total are returned from the persisted `/api/cart` endpoints.

## Verification

Backend schema/context test (uses isolated H2, not the local MySQL database):

```powershell
Set-Location H:\project_one\product-api
.\mvnw.cmd test
```

Frontend production build and lint:

```powershell
Set-Location H:\project_one\java-fullstack-frontend
npm run build
npm run lint
```

For an end-to-end customer checkout against MySQL, set the database/admin environment variables, run both services, create an admin, add at least one catalog product with stock, register a customer, then complete the customer journey above. Verify the resulting rows in `users`, `products` (the existing table now storing water products), `addresses`, `cart_items`, `orders`, `order_items`, `payments`, `deliveries`, and `notifications`.

## Known gaps

- A payment gateway is not configured; online methods are test-mode pending records.
- Schema updates use Hibernate for local development; production migrations have not yet been introduced.
- Delivery failure is recorded on the delivery record; automated customer rescheduling/refund workflows are not included.
- Analytics show database-derived daily order counts, monthly paid revenue, product demand, and delivery status; downloadable report export is not included.
- No persistent push/email/SMS notification provider is configured; order notifications are stored and shown in-app.