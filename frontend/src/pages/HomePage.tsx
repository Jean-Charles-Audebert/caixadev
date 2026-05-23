import { useEffect, useState } from 'react';
import { getPublicProjects } from '../services/projectService';
import type { ProjectInfo } from '../types';

export default function HomePage() {
  const [publicProjects, setPublicProjects] = useState<ProjectInfo[]>([]);

  useEffect(() => {
    getPublicProjects().then(setPublicProjects).catch(() => null);
  }, []);

  return (
    <div style={{ paddingTop: '3.5rem' }}>
      {/* ── Hero ── */}
      <header className="hero" style={{ position: 'relative', maxHeight: '400px', overflow: 'hidden', color: '#fff' }}>
        <img
          src="/images/robynne-o-HOrhCnQsxnQ-unsplash.jpg"
          alt="hero"
          style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover' }}
        />
        <div style={{
          position: 'absolute', inset: 0,
          background: 'linear-gradient(to bottom, rgba(0,0,0,0.55), rgba(0,0,0,0.75))',
        }} />
        <div className="container" style={{ position: 'relative', zIndex: 1, textAlign: 'center', paddingTop: '3rem', paddingBottom: '2rem' }}>
          <h1>Caixadev — Développement d'applications web</h1>
          <p style={{ fontSize: '1rem', color: '#e5e7eb' }}>
            Java, Spring Boot, React, Node.js, Angular, HTML, CSS, JavaScript
          </p>
          <br />
          Basé à La Rochelle
        </div>
        <div style={{ position: 'relative', zIndex: 1, display: 'flex', justifyContent: 'center', gap: '0.8rem', paddingBottom: '2rem' }}>
          <a href="#projects" className="btn">Projets</a>
          <a href="#contact" className="btn secondary">Contact</a>
        </div>
      </header>

      <main className="container">
        {/* ── À propos ── */}
        <section className="about">
          <h2>À propos</h2>
          <p>
            Conception et développement d'applications web orientées métier.
            Approche pragmatique : compréhension du besoin, architecture claire, code maintenable, tests automatisés, déploiement continu.
          </p>
          <p>Expérience sur des contextes concrets : outils internes, applications de gestion, API exposées à des fronts modernes.</p>
          <p>Expériences métier : Bâtiments (ingénierie), Assurance, Associations…</p>
        </section>

        {/* ── Projets dynamiques (publics) ── */}
        {publicProjects.length > 0 && (
          <section id="projects" style={{ marginTop: '3rem' }}>
            <h2>Projets en ligne</h2>
            {publicProjects.map((p) => (
              <article key={p.id} className="project-card">
                <h3>{p.name}</h3>
                {p.description && <p>{p.description}</p>}
              </article>
            ))}
          </section>
        )}

        {/* ── Sélection de projets statiques ── */}
        <section id="projects">
          <h2>Sélection de projets</h2>

          <article className="project-card">
            <h3>Site et micro CMS</h3>
            <p>Site vitrine association Batala La Rochelle — Micro CMS de gestion des contenus (textes, photos, événements)</p>
            <ul>
              <li>Authentification</li>
              <li>Gestion des rôles</li>
              <li>Interface d'administration sécurisée (JWT, refresh tokens, HttpOnly cookies, tests, CI/CD)</li>
            </ul>
            <p>Technologies : Node.js, React, HTML, CSS, JavaScript, PostgreSQL, Docker, Git</p>
            <a href="https://batala-lr.com" target="_blank" rel="noopener noreferrer">batala-lr.com</a>
          </article>

          <article className="project-card">
            <h3>Application métier — RFI — MAIF</h3>
            <p>Modernisation et refonte complète d'une application métier :</p>
            <ul>
              <li>Passage d'architecture en couches à une architecture hexagonale</li>
              <li>Migration Java 11 vers Java 21</li>
              <li>Tests unitaires et d'intégration, CI/CD</li>
              <li>Front client React 18</li>
            </ul>
            <p>Technologies : Java, Spring Boot, React, Oracle, Docker, Git, Jenkins…</p>
          </article>

          <article className="project-card">
            <h3>Application Initiative Deux-Sèvres</h3>
            <p>Mise en relation entre porteurs de projets et parrains.</p>
            <ul>
              <li>Authentification, gestion des rôles</li>
              <li>Interfaces selon les rôles (plateforme, parrain, porteur, admin)</li>
              <li>Bibliothèque de documents</li>
            </ul>
            <p>Technologies : Java, Spring Boot, React, PostgreSQL, Docker, Git, Figma…</p>
          </article>

          <article className="project-card">
            <h3>Application Association — Gestion</h3>
            <p>Application complète pour la gestion d'une association musicale :</p>
            <ul>
              <li>Gestion des adhérents, instruments, événements</li>
              <li>Messagerie interne (WebSocket)</li>
              <li>PWA et gestion hors ligne</li>
              <li>Interface admin sécurisée (JWT, CI/CD)</li>
            </ul>
            <p>Java, Spring Boot, React, PostgreSQL, Docker, Git…</p>
          </article>
        </section>

        {/* ── Contact ── */}
        <section id="contact">
          <h3>Contact</h3>
          <ul className="contact-list" style={{ listStyle: 'none', marginLeft: 0 }}>
            <li>
              Email :{' '}
              <a href="mailto:contact@caixadev.dev">contact@caixadev.dev</a>
            </li>
            <li>
              <a href="https://www.linkedin.com/in/jean-charles-audebert" target="_blank" rel="noopener noreferrer">
                LinkedIn
              </a>
            </li>
          </ul>
        </section>
      </main>
    </div>
  );
}

