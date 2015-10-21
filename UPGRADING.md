# datomic-toolbox Upgrade Process

## Background

datomic-toolbox 2.0.0 has been released. It has breaking
changes. There was a reorganization of the functions and there is new,
better functionality to support better transactions.

This document will guide you through upgrading the `*-works` projects
to datomic-toolbox 2.0.0.

## Reorganization

The easiest part of this process will be using the new namespaces. In
the 1.x.x version, there was just `datomic-toolbox.core`. Now there
are multiple namespaces:

Process:

* Change the dependency in `project.clj`

    It should read `[democracyworks/datomic-toolbox "2.0.0"]`.

* Audit each call to a datomic-toolbox functions to make sure it's
  still there.

    Since they've moved around, you'll likely need to require
    different namespaces. I recommend doing `lein do clean, compile`
    to see errors. Also, run the tests (`lein test`).

* For each datomic-toolbox function that has moved, require the new
  namespace where it exists and change the function call.

    I've included the namespaces you should need to know about below
    with an example `:require` statement. Please stick to these
    aliases, if possible. If it's impossible, please let Eric know and
    we'll work it out.
    
* add configuration to `datomic.core/initialize`

    Its signature has changed. datomic-toolbox no longer gets its
    configuration from resource-config. You have to pass it in, so you
    should grab it like `(config [:datomic])`. `initialize` is
    ususally called in `core/-main`.



* Run tests and compile again!

```
lein do clean, compile, test
```

* Might as well commit that now to a new branch.

### `datomic-toolbox.core`

```
:require [datomic-toolbox.core :as db]
```

Functions and other variables for getting datomic initialized. These
are largely unchanged and won't require a new `:require`.

This is the home of these commonly called functions:

* `connection`
* `transact`
* `initialize`

### `datomic-toolbox.query`

```
:require [datomic-toolbox.query :as query]
```

Usually functions in this namespace take a db and some other
params. These are functions that query the database but don't change
it.

This is the new home of these commonly called functions:

* `find-one`
* `match-query`
* `match-entities`
* `timestamps`

### `datomic-toolbox.transaction`

```
:require [datomic-toolbox.transaction :as tx]
```

Functions for modifying the database. They often take a connection.

This is the new home of these functions:

* `dissoc-optional-nil-values`
* `deftx-data-fn`
* `swap-tx!`

### Other namespaces

The others are not intended to be used directly. If you need to refer
to the others, tell Eric.


## Atomic Transactions

The second change to datomic-toolbox is to support an atomic
transaction model.

### Problem statement

Very often, our functions follow this pattern:

1. Read something from the database (using a query, find-one, etc.).
1. Construct some datomic transaction data based on the value read.
1. Transact the data to datomic.

Unfortunately, this can create data consistency errors. Imagine a
simple example where we want to increment a counter. This is how it
might go:

1. Read the current value of the counter from the database
1. Construct transaction data setting the counter to counter + 1.
1. Transact the new value to datomic.

This would work fine in a setting with just one machine with one ever
doing this. But we have a cluster where usually there are 3 servers
running this same process. What can (and will) happen is that two
machines read the value at the same time, let's say it's 100. They
each add 1 to it, getting 101. Then they each transact 101 as the new
value of the counter. They both counted but the current value only
incremented by 1. **This pattern is inherently unsafe.**

### Solution

The solution is to do something more like `clojure.core/swap!`. You
never read the current value of the database directly. Instead, you
write a function that is called on the current value of the
database. That function *must* be pure, because it can be called
multiple times.

The function you pass will take the current version of the database
and return a new transaction data. That transaction data will be
transacted to datomic. If it throws a
`ConcurrentModificationException`, the whole thing will start
over. Otherwise, it passes and the transcation is stored in
datomic. (Other exceptions will fail the entire swap and will be
thrown as normal).

But why would something throw Concurrent Modification Exceptions?
Well, there are three new datomic database functions installed as part
of the normal initialization/migration process. They assert that
changes have not been made to specific values since the last database
read. They support a compare-and-set style of transaction.

So the counter above would work like this:

1. Read the current value of the database (call it `c`).
1. Add 1 to `c` and construct datomic transaction data from it that
   which asserts that the current count is still equal to `c`.
1. Attempt to transact the data.
1. 
  * If it fails, start back at 1.
  * Otherwise, you're done.

These three functions have proven sufficient in a test upgrade of a
works component.

Database functions:

#### `:assert-equal`

You often need to assert that something is equal directly. This is not
always needed, but sometimes **you need to check that something hasn't
changed since you read the database value**.

Example:

```
;; assert that the current count of 100004342342 (entity id) = 10.
[:assert-equal 10 100004342342 :current/count]
```

If the values stored in datomic is not 10, try again.

#### `:assert-empty`

You often need to say something like "add this electorate only if
there isn't already an electorate for this user+election". This
function takes a query and its arguments and asserts that the query
finds no answer.

Example:

```
;; assert that no subscription exists for that user
[:assert-empty '{:find [?eid]
                 :in [$ ?userid]
                 :where [[?eid :user/id ?userid]]} user-id]
```

#### `:transact`

The previous two simply asserted properties of the current
database. This one can actually change it. You pass `:transact` the
entity-id, the relation, and the old and new values. If the old value
is not the current value, it will throw
ConcurrentModificationException. Otherwise, it will transact the new
value.

Note that datomic transactions are atomic in that they are all or
nothing. If anything throws an exception inside, nothing gets changed
in the database.

`:transact` is what we should be using for most changes to the
database. It handles nils, components, and refs all in reasonable ways
that are otherwise a pain to handle.

Example:

```
;; change :user/id from nil to user-id
[:transact ?id :user/id nil user-id]
;; change :notification/medium from nothing (empty set) to mediums
[:transact ?id :notification/medium #{} mediums]
```

### Upgrade process

This is by far more difficult than the function migration process. It
requires thinking about operations in this compare-and-swap style
instead of how we've been doing it.

* Locate any functions (in handlers or entities) that read from the
  database and create a transaction from that.

    The telltale sign is that they require both a database connection
    (explicitly or implicitly using `db/transact`) and a database
    value (`(db/db)`).
    
    Example (see inline comments):
    
```
    (defn create-subscription
      [subscription]
      (if-let [old-subscription (read-subscription-entity
                                 ;; here we're reading from the db
                                 (db/db) (:user-id subscription))]
        ;; does this function transact? probably, you need to check
        (update-subscription old-subscription subscription)
        (let [{:keys [db-after]} @(db/transact ;; transact!!!
                                   [(subscription->tx-data subscription)])]
          (read-subscription db-after (:user-id subscription))))))
```
    
    Actually, in our test upgrade, this code was replaced by something
    much cleaner:
    
```
    (defn create-subscription
      [subscription]
      (let [userid (:user-id subscription)
            {:keys [db-after db-before]}
            @(db/swap-tx!
               (fn [db]
                 ;; notice read-subscription-entity is moved inside the fn
                 (if-let [old-sub (read-subscription-entity db userid)]
                   (update-subscription-tx old-sub subscription)
                   (create-subscription-tx subscription))))]
        ;; use db-before to get the value before transaction applied
        {:before (read-subscription db-before userid)
         :after  (read-subscription db-after  userid)}))
```

* Convert them to use `datomic-toolbox.transaction/swap-tx!`.
    * Use `:transact` to change data in the database. Use values taken
      from the current database in the old-value position.
    * Use `:assert-equal` and `:assert-empty` to replicate any other
      logic you are querying on the database in your function. For
      instance, if you check if the user has no subscription, you
      should assert that the users *still* has no subscription in the
      transaction.
    
* Make creative use of db-before and db-after.

    `swap-tx!` returns the same map as datomic `transact`. `db-before`
    is useful to know just what the value you replaced *was*.

* Optional (but recommended): Update all other functions that create
  transaction data to use `:transact`.

    It handles `nil`, many cardinality, and components very well.

* The tests should still pass.

Note: you may need fewer `dissoc-optional-nil-values` because passing
nil to `:transact` will take care of nils for you!! It's
smart. Passing nil for the new value will retract the existing value.

Here are some examples:
https://github.com/democracyworks/election-notification-works/pull/17

Also, please don't hesitate to ask Eric. This stuff is hard to explain
and hard to get right. You can lean on him for help. It gets better
with practice.
