## Requirements

[Forex](forex-mtl) is a simple application that acts as a local proxy for getting exchange rates. It's a service that can be consumed by other internal services to get the exchange rate between a set of currencies, so they don't have to care about the specifics of third-party providers.

We provide you with an initial scaffold for the application with some dummy interpretations/implementations. For starters we would like you to try and understand the structure of the application, so you can use this as the base to address the following use case:

> An internal user of the application should be able to ask for an exchange rate between 2 given currencies, and get back a rate that is not older than 5 minutes. The application should at least support 10.000 requests per day.

Please note the following drawback of the [One-Frame service](https://hub.docker.com/r/paidyinc/one-frame):

> The One-Frame service supports a maximum of 1000 requests per day for any given authentication token.

## Solution
> For this problem I have added a cache service for the API call, then when we call one-frame server we will cache the response for each 5 mins, then we can resolve the issue with 1000 request per day. We can enable and disable the cache base on config app.cache.enable=true|false

> I have added a scheduler job that will fetch all currency pairs for every 5 mins and store them in cache (memcached or redis), then the API will get values in cache instead of calling one-frame, then the number of API call will 288 times/day.
> Fetching URI http://localhost:8081/rates?pair=AUDCAD&pair=AUDCHF&pair=AUDEUR&pair=AUDGBP&pair=AUDNZD&pair=AUDJPY&pair=AUDSGD&pair=AUDUSD&pair=CADAUD&pair=CADCHF&pair=CADEUR&pair=CADGBP&pair=CADNZD&pair=CADJPY&pair=CADSGD&pair=CADUSD&pair=CHFAUD&pair=CHFCAD&pair=CHFEUR&pair=CHFGBP&pair=CHFNZD&pair=CHFJPY&pair=CHFSGD&pair=CHFUSD&pair=EURAUD&pair=EURCAD&pair=EURCHF&pair=EURGBP&pair=EURNZD&pair=EURJPY&pair=EURSGD&pair=EURUSD&pair=GBPAUD&pair=GBPCAD&pair=GBPCHF&pair=GBPEUR&pair=GBPNZD&pair=GBPJPY&pair=GBPSGD&pair=GBPUSD&pair=NZDAUD&pair=NZDCAD&pair=NZDCHF&pair=NZDEUR&pair=NZDGBP&pair=NZDJPY&pair=NZDSGD&pair=NZDUSD&pair=JPYAUD&pair=JPYCAD&pair=JPYCHF&pair=JPYEUR&pair=JPYGBP&pair=JPYNZD&pair=JPYSGD&pair=JPYUSD&pair=SGDAUD&pair=SGDCAD&pair=SGDCHF&pair=SGDEUR&pair=SGDGBP&pair=SGDNZD&pair=SGDJPY&pair=SGDUSD&pair=USDAUD&pair=USDCAD&pair=USDCHF&pair=USDEUR&pair=USDGBP&pair=USDNZD&pair=USDJPY&pair=USDSGD

## Known Issue / Defect
 - I have not finished the error handling for the components, need to update that part.
 - The number of test cases are not covered all components of the project.
 - I have not finished the dockerization for the application. Have just finished the docker-compose for One-Frame, memcached and redis
 - I have not finished the github action for CI-CD process

## How to run the project
### Run the application with docker containers
```shell
docker-compose -f docker/docker-compose.yml up oneframe memcached redis -d
sbt run
```
### Testing the API responses
#### Enable cache
```hocon
//  application.conf
  ...
  cache {
    ...
    enable: true
  }
```
```shell
ab -n 1000 -c 10 'http://localhost:8080/rates?from=AUD&to=CAD'
```

#### Unable cache
```hocon
//  application.conf
  ...
  cache {
    ...
    enable: false
  }
```
```shell
ab -n 1000 -c 10 'http://localhost:8080/rates?from=AUD&to=CAD'
```

## Run test
```shell
sbt test
```
