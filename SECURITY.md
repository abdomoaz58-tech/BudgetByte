# Security Review

## Scope

This repository is an Android application. It has no HTTP server, public API,
password authentication, web cookies, CORS policy, or SQL backend. The
database is local Room storage, so server-side controls must be implemented in
the separate backend repository when one is introduced.

## Controls implemented here

- `.env`, signing keys, certificates, secret properties, and generated release
  artifacts are ignored by Git.
- Secure preferences fail closed instead of falling back to plaintext storage.
- App databases, files, external files, and shared preferences are excluded
  from Android cloud backup and device transfer.
- Room is used for local persistence; database access must continue through DAO
  methods and never through string-built SQL.
- `FLAG_SECURE` is enabled for sensitive screens and user input is validated
  before persistence.

## Backend requirements before exposing an API

Use these controls in the backend service, not in Android resources:

```js
import bcrypt from "bcrypt";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import cors from "cors";
import { z } from "zod";

app.set("trust proxy", 1);
app.disable("x-powered-by");
app.use(
  helmet({
    contentSecurityPolicy: {
      directives: {
        defaultSrc: ["'self'"],
        objectSrc: ["'none'"],
        frameAncestors: ["'none'"],
        baseUri: ["'self'"],
      },
    },
    referrerPolicy: { policy: "no-referrer" },
    hsts: { maxAge: 31536000, includeSubDomains: true, preload: true },
  }),
);
app.use(cors({ origin: ["https://app.example.com"], credentials: true }));
app.use(express.json({ limit: "32kb" }));
app.use(rateLimit({ windowMs: 15 * 60 * 1000, limit: 100 }));
app.use(
  "/auth",
  rateLimit({
    windowMs: 15 * 60 * 1000,
    limit: 5,
    skipSuccessfulRequests: true,
  }),
);

const passwordSchema = z
  .string()
  .min(12)
  .max(128)
  .regex(/[a-z]/)
  .regex(/[A-Z]/)
  .regex(/[0-9]/)
  .regex(/[^A-Za-z0-9]/);

const passwordHash = await bcrypt.hash(password, 12);
res.cookie("session", token, {
  httpOnly: true,
  secure: true,
  sameSite: "lax",
  path: "/",
});
```

Use parameterized queries or a safe ORM for every database operation. Encode
output using the framework's default HTML escaping and never render raw user
content without a narrowly scoped sanitizer. Add CAPTCHA after repeated
failed attempts and on public signup/login forms, with server-side verification
and a timeout.

Terminate TLS at a trusted reverse proxy, redirect HTTP to HTTPS, disable
directory listing (`autoindex off;` in Nginx or `Options -Indexes` in Apache),
and do not commit certificates or private keys.

Run dependency checks in CI and before release:

```bash
./gradlew dependencyUpdates
./gradlew dependencyCheckAnalyze
npm audit --audit-level=high
```

The first two commands require the corresponding Gradle version/dependency
check plugins in the backend or Android build. Keep lockfiles committed and
fail CI on high or critical advisories after reviewing false positives.
