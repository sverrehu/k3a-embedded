# Changelog

## [0.4.0](https://github.com/sverrehu/k3a-embedded/compare/v0.3.3...v0.4.0) (2023-10-24)


### Features

* add test helper methods ([#54](https://github.com/sverrehu/k3a-embedded/issues/54)) ([8e408e3](https://github.com/sverrehu/k3a-embedded/commit/8e408e395cb8b41952aec2e14ac5cb959e0d90a0))

## [0.3.3](https://github.com/sverrehu/k3a-embedded/compare/v0.3.2...v0.3.3) (2023-10-11)


### Documentation

* minor update to README ([c2a05e1](https://github.com/sverrehu/k3a-embedded/commit/c2a05e14b6ce0a01839dac8e3338bf06f77448bf))

## [0.3.2](https://github.com/sverrehu/k3a-embedded/compare/v0.3.1...v0.3.2) (2023-10-07)


### Bug Fixes

* Don't deploy a version without Kafka version metadata tag ([#47](https://github.com/sverrehu/k3a-embedded/issues/47)) ([0ce94fb](https://github.com/sverrehu/k3a-embedded/commit/0ce94fbee99360543c2a19db2fce88dca590735b))

## [0.3.1](https://github.com/sverrehu/k3a-embedded/compare/v0.3.0...v0.3.1) (2023-10-07)


### Bug Fixes

* Downgrade to Java 11 ([#44](https://github.com/sverrehu/k3a-embedded/issues/44)) ([77a89d2](https://github.com/sverrehu/k3a-embedded/commit/77a89d249c6cecca2329fa8229b2b98f3c29331f))

## [0.3.0](https://github.com/sverrehu/k3a-embedded/compare/v0.2.2...v0.3.0) (2023-10-07)


### ⚠ BREAKING CHANGES

* Remove deprecated constructor. Builder only now. ([#42](https://github.com/sverrehu/k3a-embedded/issues/42))

### Features

* Allow specifying additional listeners ([#41](https://github.com/sverrehu/k3a-embedded/issues/41)) ([a23392d](https://github.com/sverrehu/k3a-embedded/commit/a23392dd6cc4c3a3bdf5d0d851de42f1ae2bc715))
* Introduce Builder, deprecating the default constructor ([#38](https://github.com/sverrehu/k3a-embedded/issues/38)) ([af83daa](https://github.com/sverrehu/k3a-embedded/commit/af83daab68a5304bad3935da8c20fd40f3766ea6))


### Bug Fixes

* Remove deprecated constructor. Builder only now. ([#42](https://github.com/sverrehu/k3a-embedded/issues/42)) ([688ecef](https://github.com/sverrehu/k3a-embedded/commit/688ecef9348b6212200530e937de3eb5132dd137))

## [0.2.2](https://github.com/sverrehu/k3a-embedded/compare/v0.2.1...v0.2.2) (2023-10-07)


### Bug Fixes

* correct scope on slf4j backend ([#34](https://github.com/sverrehu/k3a-embedded/issues/34)) ([b3dbee1](https://github.com/sverrehu/k3a-embedded/commit/b3dbee1537aa107ad052fced1e4cfd9eb7b7033a))


### Documentation

* Update README with pom.xml instructions ([#32](https://github.com/sverrehu/k3a-embedded/issues/32)) ([2f3cdbc](https://github.com/sverrehu/k3a-embedded/commit/2f3cdbcbca85211a5782cde9d7fc7a8f3c293565))

## [0.2.1](https://github.com/sverrehu/k3a-embedded/compare/v0.2.0...v0.2.1) (2023-10-07)


### Bug Fixes

* Clean to avoid uploading duplicates ([6331284](https://github.com/sverrehu/k3a-embedded/commit/63312844fd8d82463c4e4d9a0e267d68196e5fcd))

## [0.2.0](https://github.com/sverrehu/k3a-embedded/compare/v0.1.3...v0.2.0) (2023-10-07)


### Features

* New home at Maven Central ([#27](https://github.com/sverrehu/k3a-embedded/issues/27)) ([b9a3b22](https://github.com/sverrehu/k3a-embedded/commit/b9a3b222755d6a10bbbd67ba13933fa37ce5998d))

## [0.1.3](https://github.com/sverrehu/k3a-embedded/compare/v0.1.2...v0.1.3) (2023-10-07)


### Bug Fixes

* Maven Central ([#24](https://github.com/sverrehu/k3a-embedded/issues/24)) ([95bbe34](https://github.com/sverrehu/k3a-embedded/commit/95bbe349e5b277d51498332217ea540951af83ae))

## [0.1.2](https://github.com/sverrehu/k3a-embedded/compare/v0.1.1...v0.1.2) (2023-10-07)


### Bug Fixes

* Add signing ([#21](https://github.com/sverrehu/k3a-embedded/issues/21)) ([eb63be2](https://github.com/sverrehu/k3a-embedded/commit/eb63be23e461ccecd251a9af9bcb4d1dcce8a71b))

## [0.1.1](https://github.com/sverrehu/k3a-embedded/compare/v0.1.0...v0.1.1) (2023-10-06)


### Bug Fixes

* Deploy ordering ([#18](https://github.com/sverrehu/k3a-embedded/issues/18)) ([47384da](https://github.com/sverrehu/k3a-embedded/commit/47384da9559296afd6c19c850716bb2dd0466465))

## [0.1.0](https://github.com/sverrehu/k3a-embedded/compare/v0.0.3...v0.1.0) (2023-10-06)


### Features

* build for multiple Kafka versions ([#15](https://github.com/sverrehu/k3a-embedded/issues/15)) ([4224477](https://github.com/sverrehu/k3a-embedded/commit/42244778d2e2f2d74884275c0d740a8c36a39015))

## [0.0.3](https://github.com/sverrehu/k3a-embedded/compare/v0.0.2...v0.0.3) (2023-10-06)


### Bug Fixes

* Use RELEASE_TOKEN ([#12](https://github.com/sverrehu/k3a-embedded/issues/12)) ([7a9d364](https://github.com/sverrehu/k3a-embedded/commit/7a9d36496cf4d137e14288772b1c48340c2af0c6))

## [0.0.2](https://github.com/sverrehu/k3a-embedded/compare/v0.0.1...v0.0.2) (2023-10-06)


### Bug Fixes

* Make workflow files consistent ([#10](https://github.com/sverrehu/k3a-embedded/issues/10)) ([71a68c7](https://github.com/sverrehu/k3a-embedded/commit/71a68c7ddd9b6ca4b0747f12392a48a6e8cff9f6))


### Documentation

* Add simple explanation ([#8](https://github.com/sverrehu/k3a-embedded/issues/8)) ([9e278e0](https://github.com/sverrehu/k3a-embedded/commit/9e278e0aa47bb0ab64825d7793107e563e79d483))
