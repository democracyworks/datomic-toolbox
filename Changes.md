# Changelog

## UNRELEASED

* Added support for a `:migration-tx-instant` optional config param that can be set to a java.util.Date value.
  This will be used to set the `:db/txInstant` of the schema migrations when present. Higher arities of the relevant
  migration functions were also added to facilitate this (but the old ones were kept too so this should _not_ break
  existing code).

## Changes between 2.0.1 and 2.0.2

* Updated dependencies, which shouldn't affect users much since you should be
  using your own Clojure, Datomic peer lib, etc.

## Changes between 2.0.0 and 2.0.1

* Moved default Datomic connection into an atom because we discovered that
  `datomic.api/connect` sometimes has long (5+ second) pauses even though its
  docs say that it caches connections and is safe to call repeatedly. This was
  true in Datomic 0.9.5327, anyway. We're working with Cognitect to figure out
  if this is a bug, but this seems like an OK way to do things either way.

## Changes between 1.1.0 and 2.0.0

* Removed `turbovote/resource-config` dependency. Now you should pass in your
  config to `datomic-toolbox.core/initialize` or `datomic-toolbox.core/configure!`
  before using any of the zero-arity stateful fns (see list below).
  **This is a breaking change.**
* Made the following zero-arity functions optionally take connections and/or
  database args in addition to working with the defaults setup by `initialize`
  or `configure`:
    * `applied-migrations`
        * zero-arity uses `(db)`
        * single-arity takes Datomic db value arg
    * `unapplied-migrations`
        * zero-arity uses `(db)`
        * single-arity takes Datomic db value arg
    * `run-migration`
        * single-arity takes migration EDN file arg
        * double-artiy takes Datomic connection and migration EDN file args
    * `run-migrations`
        * zero-arity uses `(connection)`
        * (there is no single-arity version)
        * double-arity takes Datomic connection and Datomic db value as args
    * `install-migration-schema`
        * zero-arity uses `(connection)`
        * single-arity takes Datomic connection arg
    * All other functions without higher arities for connections and db values
      were intentionally left alone because `datomic.api` already provides the
      equivalent function (e.g. `datomic-toolbox.core/db` versus `datomic.api/db`).

## Changes between 1.0.0 and 1.1.0

* Added a core namespace. Code using this library will need to update
  require and use statements to refer to `datomic-toolbox.core`.
  **This is a breaking change.**
* Updated to Clojure 1.7.0.
* Updated to turbovote.resource-config 0.2.0. If you use both this and
  resource-config, **then this is a breaking change for you.** You
  will need to update to resource-config 0.2.0+ as well.
