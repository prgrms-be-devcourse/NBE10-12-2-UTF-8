# 빌드 스테이지
FROM node:22-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .

# 1. Railway 빌드 아규먼트를 받도록 ARG 정의
ARG NEXT_PUBLIC_API_URL
# 2. 빌드 환경변수(ENV)로 등록하여 build 명령어에 주입되도록 함
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL

RUN npm run build

# 실행 스테이지
FROM node:22-alpine
WORKDIR /app
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
COPY --from=builder /app/public ./public
EXPOSE 3000
ENV PORT=3000
CMD ["node", "server.js"]
