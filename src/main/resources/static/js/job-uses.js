// DozerNet job-use categories — hover swaps the full-bleed photo,
// sharpens the active label, and blurs the rest (matches the design intent).
(function () {
    const root = document.querySelector('.job-uses');
    if (!root) return;

    const items = Array.from(root.querySelectorAll('.job-uses-item'));
    const activePhoto = root.querySelector('.job-uses-photo[data-role="active"]');
    const nextPhoto = root.querySelector('.job-uses-photo[data-role="next"]');
    if (!items.length || !activePhoto || !nextPhoto) return;

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    let currentSrc = activePhoto.getAttribute('src') || '';
    let swapTimer = 0;

    const setActive = (item) => {
        items.forEach((el) => el.classList.toggle('is-active', el === item));
        const src = item.getAttribute('data-image');
        if (!src || src === currentSrc) return;

        if (reduceMotion) {
            activePhoto.src = src;
            currentSrc = src;
            return;
        }

        window.clearTimeout(swapTimer);
        nextPhoto.src = src;
        nextPhoto.classList.add('is-incoming');
        swapTimer = window.setTimeout(() => {
            activePhoto.src = src;
            nextPhoto.classList.remove('is-incoming');
            currentSrc = src;
        }, 420);
    };

    items.forEach((item) => {
        item.addEventListener('mouseenter', () => setActive(item));
        item.addEventListener('focusin', () => setActive(item));
    });

    // Touch / click without leaving for machines page: keep selection visible.
    root.addEventListener('mouseleave', () => {
        const first = items[0];
        if (first && !root.matches(':focus-within')) setActive(first);
    });
})();
