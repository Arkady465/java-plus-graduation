eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
  instance:
    preferIpAddress: true
    hostname: ${spring.application.name:localhost}
#    nonSecurePort: ${server.port}
#    nonSecurePortEnabled: true
#    lease-renewal-interval-in-seconds: 10
#    lease-expiration-duration-in-seconds: 30
    instance-id: "${spring.application.name}:${random.value}"

spring:
  application:
    name: main-service
  config:
    import: "configserver:"
  cloud:
    config:
      label: core
      discovery:
        enabled: true
        serviceId: config-server
      request-connect-timeout: 30000
      request-read-timeout: 30000
      fail-fast: true
      retry:
        useRandomPolicy: true
        max-attempts: 5
        initial-interval: 5000
        max-interval: 20000

management:
  endpoints:
    web:
      exposure:
        include: health