# datomic-toolbox

Datomic utility library

Takes a config map like:
`{:uri XXX :partition :XXX}`

Pass it in like this `(datomic-toolbox.core/initialize config-map)` or
like this `(datomic-toolbox.core/configure! config-map)` before using
any of the zero-arity stateful fns.

## Migrations

Datomic Toolbox has support for migrations. The database needs to be initialized by datomic-toolbox to get this support (older databases will need to be migrated). Running `(initialize config-map)` installs the partition and a transaction attribute called :datomic-toolbox/migration, which is used to keep track of which migration files have been run.

Files in resources/schema that end in .edn are considered migration files. datomic-toolbox orders schema files lexigraphically, so the convention is to name them with a date and time followed by some text describing what they do, e.g. "20140616105400-initial-schema.edn".

When `(run-migrations)` is called, it compares the schema files and the :datomic-toolbox/migration transaction attribute to figure out which schema files have not been run, and then runs them in ascending lexigraphical order. When a migration file is run, the transaction is tagged with the migration attribute that contains the name of that schema file. You can use `(applied-migrations)` to get the migration filenames that have been run, and `(unapplied-migrations)` to get the migration files that have not been run.

There is currently no support for automatic migration running other than when `(initialize config-map)` is first called, at which time it will run all available migrations. Outside of that, you'll need to make a call to `(run-migrations)` at the appropriate point in your application.

## Database functions

This library adds database functions to the database upon
initialization. These functions aid it making atomic operations
possible. They do so by asserting properties on the current
database. If the assertion fails, the transaction fails with a
`ConcurrentModificationException`.

**`:transact`:**

Generally useful for setting values of all types and cardinalities
using compare-and-swap semantics.

Example:

```
[:transact eid :some/prop old-value new-value]
```

This means "set `:some/prop` on entity with id `eid` to `new-value`
given that it is currently set to `old-value`". Throw an exception
otherwise.

The semantics are dependent on the schema of the property being

* A `nil` (or empty collection) `old-value` indicates that the
property should not be set for that entity.

* A `nil` (or empty collection) `new-value` indicates that the
property should be retracted for that entity.

* Cardinality `:many` properties are treated as a whole
collection. `old-value` and `new-value` should be the old and new sets
of values, respectively.

* Non-component refs expect the entity ids for the values. Component
refs expect nested maps.

**`:assert-empty`:**

Generally useful for asserting that something *still* doesn't
exist. For instance, assuring the uniqueness of a particular
entity.

Example:

```
[:assert-empty
 {:find [?eid]
  :in [$ ?userid]
  :where [[?eid :user/id ?userid]]}
 123]
```

This asserts that there is no entity with `:user/id` 123. This is
useful for enforcing a uniqueness constraint.

**`:assert-not-empty`:**

Similarly useful for assuring that something *still* exists.

Example:

```
[:assert-not-empty
 {:find [?eid]
  :in [$ ?userid]
  :where [[?eid :user/id ?userid]]}
 111 222]
```

This asserts that the entitity with id 111 still points to
userid 222. This is useful for enforcing uniqueness.

**`:assert-equal`:**

Assert that a constant value is equal to the value of an entity's
property.

Example:

```
[:assert-equal
 123 :election/authority-level #{:state :municipal}]
```

Assert that entity 123 has the given authority levels. Note that the
sets need to be equal for this assertion to succeed. This is useful
before retracting an entity for the existence/non-existence of some
property.

## Retrying failed transactions

`datomic.api/transact` works similar to `reset!` (the atom
operation). It changes the database regardless of the current
value. There is no equivalent in datomic to do a `swap!`, where the
transaction data is based on the current value. That's what
`datomic-toolbox.core/swap-tx!` is for.

`swap-tx!` takes a function of a single argument. That argument is the
current database. The function should return the transaction data
calculated from that database. It should be pure. `swap-tx!` will call
the function on the current database, transact the result, and if
there's a ConcurrentModificationException (meaning the relevant subset
of the database has changed since the function was called), it will
retry it.

The default is to use the global connection
(`datomic-toolbox.core/connection`) and 100 retries, though this can
be overridden in the 3-argument version.s

Example (atomic increment):

```
(swap-tx! (fn [db]
            (let [user (d/entity db userid)]
              [[:transact userid :page/count
                (:page/count user) (inc (:page/count user 0))]])))
```

Notice that the transaction data is based on the current value of the
database. It also uses `:transact` to assure an atomic transaction.


## License

Copyright © 2014-2015 Democracy Works, Inc.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
