# Hero Section Redesign Brief — DozerNet

**Audience of this document:** the model/engineer building the new landing-page hero.
**Your job:** replace the current landing-page hero with a clean, confident, engaging one that
looks like it belongs to the rest of DozerNet and does **not** look like generic AI-generated
"slop". Read this whole file before touching code.

---

## 0. TL;DR

- Redesign **only the hero** (`<header class="hero">` in `landing.html`, its CSS block in `app.css`, and `hero.js`).
- Keep the site's existing look: Apple-clean neutrals + **JCB yellow `#ffcb05`**, **Space Grotesk** headings, **Manrope** body, pill buttons, dark cinematic sections.
- The current hero feels **lumpy and unattractive** (diagnosis in §4). Fix the composition, not just the colours.
- **Take Chrome screenshots throughout the build** (mandatory process, §8). Do not design blind.
- No new frameworks, no build step, no new libraries, no stock gimmicks. Plain Thymeleaf + CSS + a little vanilla JS.
- Only use **real data** the app already has. No fabricated stats, testimonials, logos or ratings.

---

## 1. Project context

**DozerNet** is a Spring Boot 3 / Thymeleaf web app (Java 21) for renting **JCB / construction
machinery in Sri Lanka**. It replaces phone-call-based rental coordination with one verified platform
for **customers, private owners, operators and admins** (university project SE2030, group MLB-B4G2-09).

What the product actually offers (use this for honest copy — never promise more):

- Search a verified fleet and **book machines online** for chosen dates (same-day up to 180 days ahead), no double-bookings.
- Verified machines (company-owned + admin-approved private owners) and **licensed operators** assigned to jobs.
- Machines that are unsafe/overdue for maintenance are kept out of the booking pool.
- Invoices generated on completion, clear itemised pricing: daily rate + district transport + 18% VAT, in **Sri Lankan Rupees (Rs.)**.
- Target visitors on the home page: **customers who need a machine for a job** (builders, contractors, homeowners), plus owners/operators who may register.

Brand tone: **practical, trustworthy, no-nonsense, a little bold**. Construction people, not startup people. Plain words beat buzzwords.

### Stack / how to run

- Maven, Spring Boot, Thymeleaf templates, hand-written CSS (no framework). Static files live in `src/main/resources/static`.
- Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run` (H2 in-memory DB, seeded on first run). Open `http://localhost:8080`.
  If port 8080 is already serving the app, reuse it. Thymeleaf cache is off, so template edits show on refresh; **CSS/JS are static files — hard-refresh** (or the app may need `mvn compile`/restart if resources are served from `target/classes`; check by editing and reloading).
- Demo accounts (password `Password123` for all): `customer@dozernet.lk`, `owner@dozernet.lk`, `operator@dozernet.lk`, `admin@dozernet.lk`. You do not need to log in to see the landing page (`/`).

---

## 2. Files you will work in

| File | What it is | You may… |
|---|---|---|
| `src/main/resources/templates/public/landing.html` | Landing page. Hero is the `<header class="hero">…</header>` block at the top. | Rewrite the hero block. Leave every section **below** it unchanged. |
| `src/main/resources/static/css/app.css` | The single global stylesheet. Hero CSS is the block headed `/* ---------- Hero (cinematic, photo-first) ---------- */` plus the hero rules in the `@media (max-width: 900px)`, `(max-width: 620px)` and `prefers-reduced-motion` blocks. | Rewrite/replace hero rules. Add new `.hero-*` rules in that same area. |
| `src/main/resources/static/js/hero.js` | Hero entrance + parallax/"breathing" photo animation. | Rewrite or simplify. |
| `src/main/resources/static/images/machines/*.jpg` | `excavator, backhoe-loader, bulldozer, compactor, skid-steer, telehandler, wheel-loader` — all **1200×800** JPEGs. | Use them. Do **not** add or download new images unless the user says so. |
| `src/main/resources/templates/fragments/common.html` | Shared `<head>`, navbar, footer, reveal script. | **Read only** (see "do not touch"). |
| `src/main/java/com/dozernet/common/web/HomeController.java` | Supplies `availableMachines` (int) and `jobCategories` (list) to `landing.html`. | Read. Only change if truly needed (it usually is not). |

### Do NOT touch

- `fragments/common.html` (navbar/footer are shared by every page and owned by other team members).
- Anything below the hero on the landing page: `#highlights`, features grid, `#job-uses`, CTA band.
  Your hero must **hand off cleanly** into `#highlights` (a light `--bg` section directly after it).
- `.job-uses*`, `.cta-*`, `.card*`, `.btn*` base rules, design tokens in `:root`. You may *use* them; don't edit them.
  (If you genuinely need a new button variant, add it as a new class.)
- Java code outside `HomeController`, templates of other pages, `pom.xml`, tests.
- `git` state: **do not commit, push or create branches** unless the user asks.

### ⚠ Work-in-progress already in the tree (uncommitted)

`git status` currently shows uncommitted edits made by the user before this brief:

- `app.css`: a `.grid > .reveal:nth-child(n)` stagger-delay block + its reduced-motion override (near the bottom, "Cascade grid cards…"). **Keep it.**
- `hero.js`: a requestAnimationFrame "idle breathing" zoom on the hero photo (this is part of what you are replacing — fine to remove/rework).
- `landing.html`: one added line, `<script th:src="@{/js/stat-counter.js}">`. **Keep it.**
- `static/js/stat-counter.js` (untracked): counts up the numbers in `#highlights`. **Keep it, don't edit it.**

Do not `git checkout`/`reset` any of these. Edit around them.

---

## 3. Design system you must match

Tokens already defined in `:root` of `app.css` — **use the variables, don't hard-code new colours**:

```
--bg #f5f5f7        page background (light grey)
--surface #ffffff   cards
--ink #1d1d1f       text          --ink-soft #6e6e73   secondary text
--line #e3e3e6      hairlines
--accent #ffcb05    JCB yellow    --accent-dark #e0a800   --accent-ink #1d1d1f (text on yellow)
--dark #0a0a0a      near-black used for dark sections
--radius 14px  --radius-lg 22px   --shadow-sm/md/lg
--space-1..7        4 8 16 24 40 64 96 px
--font-display "Space Grotesk"    --font "Manrope"
--maxw 1120px       content width (use `.container`)
```

Existing visual language to stay consistent with:

- **Light, airy, Apple-like** base (white cards, light-grey page, hairline borders) with **dark cinematic photo sections** for drama. The `#job-uses` section is full-bleed near-black with large Space Grotesk type and a photo — the hero should feel like a sibling of that, not a different site.
- **Yellow is the only accent.** Use it sparingly: primary button, one small marker, maybe a thin line. Never large yellow fills behind body copy, never yellow gradients.
- **Pill buttons** (`.btn`, `.btn-primary`, `.btn-ghost-light`, `.btn-lg`). Reuse them as-is.
- Headings: Space Grotesk 600–700, tight tracking (`-0.03em` to `-0.04em`), tight line-height. Body: Manrope.
- Motion: calm, ease-out, short. Existing easing is `cubic-bezier(.22, 1, .36, 1)`.
- Navbar is **sticky, translucent white, 72px tall**, sits directly above the hero. The hero must look right directly under it (no hairline gap, no double border).
- Respect `prefers-reduced-motion` (the codebase already does everywhere — keep that standard).

---

## 4. What's wrong with the current hero (diagnose it yourself, don't trust this blindly)

**Step one is to screenshot the existing hero** at desktop and mobile (§8) and form your own view.
My read of the code, to be verified against the screenshot:

1. **Lumpy composition.** Everything is a narrow left column (`max-width: 36rem`), bottom-aligned, with the entire right half essentially just a darkened photo. Headline is forced to `max-width: 11ch`, creating a ragged, tall, awkward word-stack.
2. **Too many tiny stacked elements** before the message: brand label "DOZERNET" + yellow rule + H1 + lede + two buttons + a scroll indicator. The brand label is redundant — the logo is in the navbar directly above. It reads as clutter.
3. **Photo is buried.** A heavy two-layer black veil (up to ~92% opacity on the left) plus film grain plus a slow "breathing" zoom makes the image muddy, and the zoom/parallax adds motion for its own sake. The product is *machines*; the machine should be clear and good-looking.
4. **Dead space + weak hierarchy.** Content hugs the bottom with large empty top area; the scroll cue competes with the CTAs.
5. **No functional value.** The hero is only decoration. A visitor can't *start* doing anything from it except click two buttons.

You are free to disagree with any of this after seeing it, as long as you can say why.

---

## 5. Design direction (what to build)

**Concept: "Confident dark photo hero with one clear action."**
A full-width dark hero (same family as `#job-uses`) with a clearly visible machine photo, a strong
two-line headline, one sentence of support, one obvious primary action, and one *useful* real-data element.
Quiet, precise, editorial. The beauty comes from **spacing, scale, alignment and a good crop** — not effects.

### 5.1 Composition

- Use a **two-zone layout inside `.container`** (max 1120px) on desktop:
  - **Left (≈ 55%)**: headline, supporting line, actions.
  - **Right / background**: the machine photo carries the right side and is genuinely *visible* (veil is a gentle left-weighted gradient for legibility, not a blackout).
- Vertically **centre** the copy block in the hero (or optical-centre slightly above middle). Do not bottom-pin it.
- Hero height ≈ `min(88vh, 820px)` with a sensible `min-height` (~560px). On a typical 1440×900 window the hero plus a **peek of the next section** should be visible, signalling there's more below. (That peek replaces the scroll indicator → **delete the "Explore" scroll cue**.)
- **Remove**: the "DozerNet" brand eyebrow, the yellow rule above the H1 (or fold one small yellow marker into something purposeful), the film-grain overlay, the idle breathing zoom.
- Let the headline use the width it needs. Aim for **2 lines at desktop** (e.g. "Heavy machines." / "Booked in minutes." — keep the existing copy unless you have a clearly better line; see §6). No `11ch` stack.
- Type scale (guidance, tune by eye): H1 `clamp(2.6rem, 6vw, 4.8rem)`, 700, `line-height: 1.0`, `letter-spacing: -0.04em`; lede `clamp(1.05rem, 1.5vw, 1.2rem)` at ~70–78% white, `max-width ≈ 38–42ch`.

### 5.2 The one useful element (pick **one**, build it well)

The hero should do something. Choose **one** of these (recommended first), implement with real data only:

**A. Quick-find strip (recommended).** A compact search row under the CTAs or anchored near the bottom of the hero:
a single text input + submit button that does a plain `GET /machines` with `name="keyword"`
(the catalog controller already supports `keyword`; verify in `MachineCatalogController.browse`).
Placeholder like "Search excavators, loaders…". Beneath/next to it, up to **4–5 chips** linking to
`/machines?job=<slug>` built from `${jobCategories}` (`cat.name`, `cat.slug`, `cat.machineCount`).
Style chips like the existing `.job-filter-chip` but dark-on-photo (translucent white border, white text). Must be a real `<form>` and real links (works without JS).

**B. Availability marker.** A small, quiet line with a pulsing-free status dot: "`${availableMachines}` machines available to book now" using the real `availableMachines` model value (don't fake it; hide it if the value is 0). Pair it with the CTAs. Simpler, lower risk.

Do **not** do both A and B plus more. Whichever you choose, it must be visually subordinate to the headline and the primary CTA.

### 5.3 Actions

- Primary: `Browse machines` → `@{/machines}` (`.btn .btn-primary .btn-lg`).
- Secondary: `See how it works` → `@{/how-it-works}`. Prefer the existing `.btn-ghost-light`; if option A is used, a lighter text-link style is fine to reduce button clutter.
- All links via Thymeleaf `@{...}`. Don't hard-code `/` URLs.

### 5.4 Image treatment

- Use `excavator.jpg` (current hero image) unless, after viewing the other photos, another is clearly better. **Open the images and look at them before choosing.** All are 1200×800, so avoid upscaling past ~1.15× or they go soft — don't use `scale(1.08)` + parallax zoom on a 1200px source stretched across 1920px without checking sharpness.
- Crop with `object-fit: cover` and a deliberate `object-position` so the **machine is the focal point and is not hidden behind text**. Verify the crop at 1440, 1920, 1024, 768 and 390px wide.
- Veil: simple, one left→right gradient (e.g. ~`rgba(10,10,10,.85)` → `~.25`) plus a subtle bottom fade into the hero edge. Text contrast must meet **WCAG AA (4.5:1)** over the real pixels behind it — check, don't assume.
- Optional, tasteful: a very slight fixed-position parallax *or* none. If kept, ≤ 24px translate, no scale pulse. It must be off for reduced-motion. "No motion" is an acceptable answer.
- Add `fetchpriority="high"` and explicit `width`/`height` (1200/800) on the `<img>` to avoid layout shift; keep `alt=""` + `aria-hidden` on the container since it's decorative, **or** give it a real `alt` and drop `aria-hidden` — pick one consistently.

### 5.5 Handoff to the next section

The next section (`#highlights`) is light grey (`--bg`). The hero's bottom edge should be a **clean straight edge** (no wavy dividers, no diagonal clip-paths). A subtle bottom gradient into near-black is fine. Check there's no awkward double-spacing between hero and the stats row.

### 5.6 Motion (restrained)

- One entrance: headline, lede, actions fade/translate-up (≤ 20px) with a short stagger (≤ 80–100ms steps, total < 900ms). Reuse the existing `.hero-anim` / `--d` / `.is-ready` mechanism or simplify it — but if JS fails to load, **content must still be visible** (progressive enhancement: don't leave `opacity: 0` stuck without JS; e.g. gate the hidden state on a `.js` class set by an inline snippet, or keep the current approach only if you verify `hero.js` always runs).
- No looping animations, no particles, no cursor effects, no marquee, no typewriter text, no WebGL/Three.js (the README mentions a Three.js hero; it's **not** in the codebase — don't add it).
- `prefers-reduced-motion`: everything static and fully visible.

---

## 6. Copy rules

- Keep the current headline/lede unless you have a clearly better one:
  - H1: "Heavy machines. Booked in minutes."
  - Lede: "Search verified JCBs, reserve your dates, and track the job — without the phone chase."
- Plain, concrete, short. No "revolutionize", "seamless", "cutting-edge", "unlock", "empower", "next-gen", no emoji, no exclamation marks.
- Any number shown must come from the model (`availableMachines`, `cat.machineCount`). **No invented stats** ("10,000+ jobs", "4.9★", "trusted by…").
- Currency, if mentioned, is "Rs." (Sri Lankan Rupees).

---

## 7. Anti-slop checklist (reject your own work if any box is true)

- [ ] Purple/blue/neon gradient, glow orbs, blurred blobs, "aurora" backgrounds
- [ ] Glassmorphism cards floating over the photo for no reason
- [ ] Fake dashboard / fake booking widget mock-ups, fake stats, fake logos, fake testimonials
- [ ] More than one decorative element competing with the headline
- [ ] Gradient text, text shadows/glow on the headline
- [ ] Emoji or generic stock icons used as decoration
- [ ] Three tiny "feature" badges in a row under the buttons (that's what `#highlights` is for)
- [ ] Yellow used on large areas, or more than ~3 yellow touches in the viewport
- [ ] Centered-everything "SaaS template" layout that ignores the site's left-aligned industrial feel
- [ ] Tiny, low-contrast text over the photo
- [ ] Animation that you'd call "cool" rather than "useful"
- [ ] New fonts, new colours outside the tokens, new JS/CSS libraries or CDNs
- [ ] Anything that looks different from the rest of the site's style when scrolling from hero → highlights → features → job-uses

The target feeling: **a well-made site for a real equipment-hire company**, calm and sure of itself.

---

## 8. Mandatory process — screenshots in Chrome throughout the build

You have Chrome automation tools (`mcp__claude-in-chrome__*`). **Load them with one `ToolSearch` call**
(`select:` the tools you need, comma-separated), invoke the `claude-in-chrome` skill first if available, and start every
browser session with `tabs_context_mcp`. Open a **new tab** for `http://localhost:8080/`
(don't reuse stale tab IDs). Avoid triggering JS `alert/confirm/prompt` dialogs. If browser calls fail 2–3 times, stop and tell the user rather than looping.

Work in this order and **save/inspect a screenshot at each numbered point**, looking at it critically before moving on:

1. **Baseline.** Start the app. Screenshot the *existing* hero at **1440×900**, **1024×768**, and **390×844** (use `resize_window`). Write down in 3–5 bullets what looks wrong. Also scroll down once and screenshot the hero→`#highlights` transition and the `#job-uses` section so you know the site's visual rhythm.
2. **Skeleton.** After laying out the new markup with minimal CSS: screenshot desktop. Check alignment, spacing, hierarchy only (ignore polish).
3. **Image & veil.** After the photo crop and gradient: screenshot at 1440, 1920 (if the window allows) and 390. Check the machine is visible, nothing important is behind the text, contrast is OK, image isn't blurry.
4. **Typography & actions.** After type scale/buttons/quick-find: screenshot; zoom into the text region to check line breaks and contrast.
5. **Motion.** Reload and screenshot ~300ms and ~1200ms after load (or record with `gif_creator`, named e.g. `hero-entrance.gif`, with extra frames before/after) to confirm the entrance finishes with everything visible. Then confirm the end state is correct.
6. **Responsive pass.** Screenshot at **1920, 1440, 1024, 768, 390** widths. Check: no horizontal scroll, no overlapping elements, tap targets ≥ 44px on mobile, the headline wraps nicely, search row doesn't overflow.
7. **Transition & whole page.** Scroll through hero → highlights → features → job-uses → CTA. Make sure the hero feels like the same site. Check the sticky navbar over the hero when scrolled.
8. **Final QA.** Check console for errors (`read_console_messages`, use a `pattern` filter) and the network panel for 404s on images/JS/CSS. Test keyboard: Tab order and visible focus rings on every interactive element in the hero.

After each screenshot, **say what you see and what you'll change**, then change it. Iterate until it passes. Don't stop at the first version that "works". If you find yourself making the same fix twice, step back and rethink the layout rather than nudging pixels.

---

## 9. Technical requirements

- **Thymeleaf-valid HTML** (the file declares `xmlns:th`). Use `th:href="@{...}"`, `th:src="@{...}"`, `th:each`, `th:text`. Escape `&` as `&amp;` in static text. Keep the document structure and `<header class="hero">` landmark.
- Semantics & a11y: one `<h1>`; real `<form>`/`<label>` (visually hidden label is OK, using a utility class you add) for any input; `aria-label` where there's no visible text; visible `:focus-visible` rings (use the yellow `rgba(255,203,5,.35)` glow pattern the site already uses); decorative media hidden from assistive tech.
- Performance: no new network requests besides what's there (fonts + one hero image). No layout shift. Don't animate `top/left/width/height`; use `transform`/`opacity` only. Remove any `will-change` you don't need.
- CSS: keep it in `app.css`, scoped under `.hero` / `.hero-*`. **Delete** the old hero rules that you replace — don't leave dead CSS or contradictory duplicates (including the hero bits inside the `@media (max-width: 900px)` and `620px` blocks and the reduced-motion block). Don't add `!important`.
- JS: vanilla, ≤ ~40 lines, wrapped in an IIFE like the existing files, no dependencies, safe if elements are missing. If you no longer need `hero.js` beyond adding `is-ready`, shrink it accordingly; don't remove the `<script>` tag from `landing.html` unless the file is truly redundant.
- Browser support: current Chrome, Safari, Firefox, mobile Safari/Chrome. Avoid cutting-edge CSS without a fallback (e.g. use `min-height: 88vh` first and `88svh` as an enhancement).
- Don't break other pages: `app.css` is global. After editing, spot-check `/machines`, `/how-it-works`, `/login` in Chrome to confirm nothing else shifted. Buttons and `.hero-actions` are reused by the CTA band at the bottom of the landing page (`.cta-band .hero-actions`) — **keep `.hero-actions` working** there.
- Responsive breakpoints already used by the site: `900px` and `620px`. Reuse them.

---

## 10. Acceptance criteria (definition of done)

1. At 1440×900 the hero is balanced: copy block vertically centred on the left, machine clearly visible on the right, no big dead zones, a peek of `#highlights` below.
2. H1 wraps to two clean lines on desktop; hierarchy reads H1 → lede → primary action → (quick-find or availability).
3. Brand eyebrow, grain overlay, breathing zoom and scroll cue are gone.
4. Colours/fonts/radii all come from the existing tokens; yellow appears only as accent; looks consistent with `#job-uses` and the buttons elsewhere.
5. Text contrast ≥ 4.5:1 over the real photo at all tested widths.
6. Works at 1920 / 1440 / 1024 / 768 / 390 with no horizontal scroll, overlap, or clipped text; mobile tap targets ≥ 44px.
7. Works with JS disabled (content visible, links/search work) and with `prefers-reduced-motion: reduce` (no motion).
8. No console errors, no 404s; no new dependencies.
9. Everything below the hero on `/` is visually unchanged; other pages are unaffected; `.cta-band .hero-actions` still looks right.
10. `stat-counter.js`, the stagger CSS, and the `stat-counter.js` script tag still present and working.
11. Old hero CSS removed; no dead code.
12. `mvn test` is **not** required to change, but if you edit `HomeController` run the tests with JDK 21 (`JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test`) and keep them green.

## 11. When you finish

Reply with:

- A short summary of what you changed and **why** (3–6 bullets), and the final files touched.
- The key screenshots you took (desktop, tablet, mobile) with a one-line critique of each.
- Anything you chose **not** to do and anything you'd flag for the user (e.g. the 1200×800 photos being low-res for large screens, wanting a better image).
- Do **not** commit. Leave changes in the working tree for the user to review.

If something in this brief conflicts with what you see in the running app, trust the running app, make the best decision, and note the deviation in your summary rather than silently ignoring it.
