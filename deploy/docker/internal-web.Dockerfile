FROM node:22-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json tsconfig.base.json ./
COPY frontend ./frontend
RUN npm ci --ignore-scripts
RUN npm run build --workspace=@srm/internal-web

FROM nginx:1.27-alpine
COPY deploy/nginx/internal/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/frontend/apps/internal-web/dist /usr/share/nginx/html
EXPOSE 80
