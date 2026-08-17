FROM golang:1.22-alpine AS build
WORKDIR /app
COPY go.mod ./
COPY cmd/ ./cmd/
RUN go build -o /app/server ./cmd/

FROM alpine:3.21
WORKDIR /app
COPY --from=build /app/server .
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --retries=3 \
    CMD wget -qO- http://localhost:8080/health || exit 1
ENTRYPOINT ["./server"]
