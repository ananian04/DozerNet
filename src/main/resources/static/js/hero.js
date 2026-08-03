// DozerNet landing hero - Three.js.
// Tries to load a real JCB model from /models/excavator.glb; if that is not
// present it builds a stylized low-poly excavator in code so the hero always
// renders. Kept deliberately lightweight and lazy-friendly for performance.

import * as THREE from 'https://cdn.jsdelivr.net/npm/three@0.160.0/build/three.module.js';
import { GLTFLoader } from 'https://cdn.jsdelivr.net/npm/three@0.160.0/examples/jsm/loaders/GLTFLoader.js';

const canvas = document.getElementById('hero-canvas');
if (canvas) {
    initHero(canvas);
}

function initHero(canvas) {
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(38, canvas.clientWidth / canvas.clientHeight, 0.1, 100);
    camera.position.set(6, 3.6, 8);
    camera.lookAt(0, 0.8, 0);

    const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setSize(canvas.clientWidth, canvas.clientHeight, false);

    // Lighting
    scene.add(new THREE.HemisphereLight(0xffffff, 0x202024, 1.1));
    const key = new THREE.DirectionalLight(0xffffff, 1.6);
    key.position.set(5, 8, 6);
    scene.add(key);
    const rim = new THREE.DirectionalLight(0xffcb05, 0.6);
    rim.position.set(-6, 3, -4);
    scene.add(rim);

    const pivot = new THREE.Group();
    scene.add(pivot);

    // Try the real model first, fall back to procedural.
    const loader = new GLTFLoader();
    loader.load(
        '/models/excavator.glb',
        (gltf) => {
            const model = gltf.scene;
            fitAndCenter(model, 3.4);
            pivot.add(model);
        },
        undefined,
        () => pivot.add(buildProceduralExcavator())
    );

    function onResize() {
        const w = canvas.clientWidth, h = canvas.clientHeight;
        camera.aspect = w / h;
        camera.updateProjectionMatrix();
        renderer.setSize(w, h, false);
    }
    window.addEventListener('resize', onResize);

    // Subtle pointer parallax
    let targetY = 0;
    window.addEventListener('pointermove', (e) => {
        targetY = ((e.clientX / window.innerWidth) - 0.5) * 0.6;
    });

    const clock = new THREE.Clock();
    (function animate() {
        requestAnimationFrame(animate);
        const t = clock.getElapsedTime();
        if (!reduceMotion) {
            pivot.rotation.y += 0.004;
        }
        pivot.rotation.y += (targetY - (pivot.rotation.y % (Math.PI * 2))) * 0.0;
        pivot.position.y = Math.sin(t * 0.8) * 0.08;
        renderer.render(scene, camera);
    })();
}

function fitAndCenter(object, targetSize) {
    const box = new THREE.Box3().setFromObject(object);
    const size = box.getSize(new THREE.Vector3());
    const center = box.getCenter(new THREE.Vector3());
    const scale = targetSize / Math.max(size.x, size.y, size.z);
    object.scale.setScalar(scale);
    object.position.sub(center.multiplyScalar(scale));
}

function buildProceduralExcavator() {
    const g = new THREE.Group();

    const yellow = new THREE.MeshStandardMaterial({ color: 0xffcb05, roughness: 0.45, metalness: 0.2 });
    const dark = new THREE.MeshStandardMaterial({ color: 0x1d1d1f, roughness: 0.6, metalness: 0.3 });
    const glass = new THREE.MeshStandardMaterial({ color: 0x8fd3ff, roughness: 0.1, metalness: 0.1, transparent: true, opacity: 0.7 });
    const steel = new THREE.MeshStandardMaterial({ color: 0x3a3a3d, roughness: 0.5, metalness: 0.5 });

    const box = (w, h, d, mat, x = 0, y = 0, z = 0) => {
        const m = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), mat);
        m.position.set(x, y, z);
        return m;
    };

    // Tracks (crawler)
    const trackL = box(3.4, 0.6, 0.7, dark, 0, 0.3, -0.9);
    const trackR = box(3.4, 0.6, 0.7, dark, 0, 0.3, 0.9);
    g.add(trackL, trackR);
    // Track rollers
    for (let i = -1; i <= 1; i++) {
        g.add(box(0.35, 0.35, 0.75, steel, i * 1.1, 0.25, -0.9));
        g.add(box(0.35, 0.35, 0.75, steel, i * 1.1, 0.25, 0.9));
    }

    // Rotating house / body
    const body = box(2.4, 1.0, 2.0, yellow, -0.2, 1.15, 0);
    g.add(body);
    // Counterweight
    g.add(box(0.7, 1.0, 2.0, dark, -1.5, 1.15, 0));
    // Cabin
    g.add(box(1.0, 1.0, 1.0, yellow, 0.7, 1.75, -0.5));
    g.add(box(0.85, 0.7, 0.85, glass, 0.75, 1.8, -0.5));
    // Exhaust
    g.add(box(0.12, 0.5, 0.12, steel, 0.2, 2.0, 0.6));

    // Boom + arm + bucket
    const arm = new THREE.Group();
    arm.position.set(1.1, 1.3, 0.35);
    const boom = box(2.2, 0.4, 0.4, yellow, 0.9, 0.6, 0);
    boom.rotation.z = 0.5;
    arm.add(boom);
    const stick = box(1.8, 0.32, 0.32, yellow, 2.1, 0.2, 0);
    stick.rotation.z = -0.6;
    arm.add(stick);
    // Bucket
    const bucket = new THREE.Group();
    bucket.position.set(2.7, -0.7, 0);
    bucket.add(box(0.7, 0.7, 0.9, dark));
    bucket.add(box(0.2, 0.5, 0.9, dark, 0.35, -0.35, 0));
    arm.add(bucket);
    g.add(arm);

    g.position.y = -1.0;
    return g;
}
