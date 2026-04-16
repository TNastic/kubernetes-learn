# Personal Task Manager Kubernetes Lab

This repository is a learning project for building and deploying a small personal task management system with Docker, K3s, and Kubernetes YAML.

The goal is not to build a complex task product. The goal is to practice the full path from local development to server deployment, including configuration, container images, Kubernetes resources, HTTPS, logs, rolling updates, and rollback.

## Current Progress

Current stage: stage 2, server database preparation.

Stage 1 is complete:

- Project plan document created.
- Basic repository directory structure created.
- `README.md` created.
- `.gitignore` created.

The detailed roadmap is in `docs/00-project-plan.md`.

## Tech Stack

- Frontend: React, Vite, Nginx.
- Backend: Java 8, Spring Boot 2.x.
- Database: MySQL 8.
- Cache: Redis 7.
- Container: Docker.
- Kubernetes: K3s, Deployment, Service, ConfigMap, Secret, PVC, Ingress.
- HTTPS: Ingress Controller, cert-manager or cloud provider certificate, Let's Encrypt.

## Repository Structure

```text
kubernetes-learn/
  docs/
    00-project-plan.md

  frontend/
    src/

  backend/
    src/

  deploy/
    k8s/
      mysql/
      redis/
      backend/
      frontend/
      ingress/
    scripts/

  .gitignore
  README.md
```

## Security Rules

- Do not commit real `.env` files.
- Do not commit real Kubernetes `secret.yaml` files.
- Do not commit database passwords, Redis passwords, tokens, certificates, or registry credentials.
- Commit only example files such as `.env.example` and `secret.example.yaml`.

## Learning Principle

- Make it work first, then optimize.
- Understand each Kubernetes object before adding automation.
- Keep the business code simple so the project stays focused on deployment and troubleshooting.
- Record reproducible commands, configuration, problems, and fixes in `docs/`.
