# V1 Web UI

V1 keeps the dashboard intentionally small. The live UI is served as static assets by the Spring Boot control plane from `apps/control-plane/src/main/resources/static`.

This directory exists to reserve `apps/web` for a future standalone frontend when the passive observability UI outgrows the embedded static approach.

