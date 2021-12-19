## Requirements

[Forex](forex-mtl) is a simple application that acts as a local proxy for getting exchange rates. It's a service that can be consumed by other internal services to get the exchange rate between a set of currencies, so they don't have to care about the specifics of third-party providers.

We provide you with an initial scaffold for the application with some dummy interpretations/implementations. For starters we would like you to try and understand the structure of the application, so you can use this as the base to address the following use case:

> An internal user of the application should be able to ask for an exchange rate between 2 given currencies, and get back a rate that is not older than 5 minutes. The application should at least support 10.000 requests per day.

> For this problem I have added a cache service for the API call, then when we call one-frame server we will cache the response for each 5 mins, then we can resolve the issue with 1000 request per day. We can enable and disable the cache base on config app.cache.enable=true|false  

Please note the following drawback of the [One-Frame service](https://hub.docker.com/r/paidyinc/one-frame):

> The One-Frame service supports a maximum of 1000 requests per day for any given authentication token.

## How to run the project

```shell
docker-compose -f docker/docker-compose.yml up oneframe memcached -d
sbt run
```

```shell
ab -n 100 -c 10 'http://localhost:8080/rates?from=AUD&to=CAD'
```
