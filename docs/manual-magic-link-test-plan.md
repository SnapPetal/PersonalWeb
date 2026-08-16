# Manual Magic-Link Authentication Test Plan

## Purpose

Verify the deployed magic-link authentication flow after the service-layer unit test was removed.
This plan covers email delivery, token exchange, session authorization, logout, and invalid-token
behavior.

## Test setup

- Use the deployed staging or production URL.
- Use a dedicated test Gmail address, never a customer’s address.
- Use the deployed email provider. If using Mailtrap, configure the deployment to send to Mailtrap’s
  SMTP/API sandbox instead of AWS SES.
- Use a fresh browser context or Playwright browser context for each test.
- Record the deployment URL, test email address, timestamp, and commit under test.

The session cookie is `PERSONALWEB_AUTH_SESSION`. It should be `HttpOnly`, scoped to `/`, and valid
for 24 hours.

## Test cases

### 1. Login page and protected-page redirect

1. Open `/landscape` in a fresh browser context.
2. Confirm the application redirects to `/auth/login`.
3. Confirm the login page renders without an error.

Expected: unauthenticated users cannot access the landscape planner.

### 2. Request a magic link

1. Open `/auth/login?redirect=/landscape`.
2. Submit the dedicated test Gmail address.
3. Confirm the browser returns to the login page with the sent state.
4. Open Gmail and locate the newest magic-link message from the configured sender.

Expected: one email arrives and contains a link with `/auth/confirm?token=` and the requested
`redirect=/landscape`.

### 3. Complete login with the magic link

1. Open the link from the email in the same browser context.
2. Confirm the response redirects to `/landscape`.
3. Confirm the browser receives `PERSONALWEB_AUTH_SESSION`.
4. Confirm the cookie is `HttpOnly`, has path `/`, and is marked `Secure` over HTTPS.
5. Confirm the landscape planner loads instead of redirecting to login.
6. Optionally call `/auth/me` and confirm it returns the authenticated user ID.

Expected: the token is accepted once and the session grants access to protected landscape pages.

### 4. Token reuse is rejected

1. Open the same magic link again in a fresh browser context.

Expected: the browser redirects to `/auth/login?invalid`; no session cookie is issued.

### 5. Logout revokes the session

1. From the authenticated browser context, submit the application logout action or send
   `POST /auth/logout`.
2. Confirm the response redirects to the login page.
3. Confirm `PERSONALWEB_AUTH_SESSION` is cleared or expires immediately.
4. Reload `/landscape`.

Expected: the user is redirected to login and can no longer access protected pages.

### 6. Invalid and expired tokens

1. Open `/auth/confirm?token=not-a-real-token`.
2. Confirm the invalid-token redirect.
3. If an expired-token fixture or administrative test record is available, open its link.

Expected: invalid and expired tokens are rejected without creating a session.

### 7. Rate-limit behavior

1. Request magic links more than five times in one hour for the same email/IP combination.
2. Confirm no additional login token is issued after the limit is reached.
3. Confirm the application remains usable and does not expose internal errors.

Expected: the rate limit is enforced without revealing whether an account exists.

## Gmail retrieval options

For a one-off test, open Gmail manually and copy the link. For repeatable Playwright runs, use a
dedicated Gmail test account with the Gmail API and OAuth credentials:

1. Request a link using the dedicated account.
2. Poll Gmail for a recent message matching the sender and subject.
3. Read the message body and extract the confirmation URL.
4. Open the URL in Playwright.
5. Delete or label the test message after the run.

Do not automate a personal Gmail login or store a Gmail password in the repository.

## Evidence to record

- Test timestamp and deployment commit
- Browser context and URL
- Login email received, with token values redacted
- Redirect results
- Cookie attributes, with the cookie value redacted
- `/auth/me` response, if used
- Logout result
- Any application logs or screenshots for failed cases

## Pass criteria

All seven test cases pass, no token or session value appears in logs or screenshots, and the test
account receives no unexpected messages after cleanup.
