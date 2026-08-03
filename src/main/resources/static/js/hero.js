// DozerNet landing hero — cinematic entrance + subtle parallax.
// No heavy WebGL: photo-first for reliability and personality.
(function () {
    const hero = document.querySelector('.hero');
    if (!hero) return;

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const photo = hero.querySelector('.hero-photo');

    // Staggered entrance
    requestAnimationFrame(() => hero.classList.add('is-ready'));

    if (reduceMotion || !photo) return;

    let ticking = false;
    const onScroll = () => {
        if (ticking) return;
        ticking = true;
        requestAnimationFrame(() => {
            const rect = hero.getBoundingClientRect();
            const progress = Math.min(1, Math.max(0, -rect.top / Math.max(rect.height, 1)));
            photo.style.transform = 'scale(' + (1.08 - progress * 0.06) + ') translate3d(0,' + (progress * 36) + 'px,0)';
            ticking = false;
        });
    };

    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
})();
