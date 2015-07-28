# Changelog

## Changes between 1.0.0 and 1.1.0

* Added a core namespace. Code using this library will need to update
  require and use statements to refer to `datomic-toolbox.core`.
  **This is a breaking change.**
* Updated to Clojure 1.7.0.
* Updated to turbovote.resource-config 0.2.0. If you use both this and
  resource-config, **then this is a breaking change for you.** You
  will need to update to resource-config 0.2.0+ as well.
