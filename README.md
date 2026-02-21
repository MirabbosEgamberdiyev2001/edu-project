# EduPlatform

Full-stack educational platform with Spring Boot backend and React frontend.

## Project Structure

```
EduPlatform/
├── eduPlatformBackend/     # Spring Boot 3.2.2, Java 21, PostgreSQL, Redis
└── eduPlatformFrontend/    # React 18, TypeScript, Vite, Material-UI
```

## Backend

- **Tech**: Spring Boot 3.2.2, Java 21, PostgreSQL 16, Redis 7
- **Auth**: JWT with JTI-based blacklisting, Refresh Token Rotation, Google OAuth, Telegram
- **Modules**: Auth, Content (Subject/Topic/Question), Test Generation & Taking, Assessment, Groups, Parent Portal, Subscriptions (Payme/Click/Uzum), Analytics, Admin, Notifications (SMS/Email)
- **i18n**: JSONB multilingual fields (uz_latn, uz_cyrl, en, ru)
- **Export**: PDF, DOCX, CSV, Excel

### Run Backend
```bash
cd eduPlatformBackend
mvn clean compile
mvn spring-boot:run
```

## Frontend

- **Tech**: React 18, TypeScript, Vite, Material-UI, Zustand, TanStack Query
- **Structure**: Monorepo with pnpm workspaces (main, admin, shared packages)
- **Features**: Auth flows, Dashboard, Subject/Topic/Question management, Test generation & taking, Admin panel, Analytics, Parent portal, Subscriptions
- **i18n**: 4 languages (uz_latn, uz_cyrl, en, ru)

### Run Frontend
```bash
cd eduPlatformFrontend
pnpm install
pnpm dev
```

## API

- Backend runs on port `8082`
- Frontend dev server proxies `/api` to backend
- Swagger UI: `http://localhost:8082/swagger-ui.html`
