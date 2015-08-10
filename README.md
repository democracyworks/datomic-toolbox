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

## License

Copyright © 2014-2015 Democracy Works, Inc.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
