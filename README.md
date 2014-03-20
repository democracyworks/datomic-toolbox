# datomic-toolbox

Datomic utility library

Requires a config.edn file in the classpath with 
`{:datomic {:uri XXX :partition :XXX}}`

Can load one or more schema files a schemas directory on the classpath, in lexigraphic order. Easiest convention is to name schema files with ascending timestamps as in Rails migrations. Call the core/initialize function in setup or test setup code to utilize this feature.

## License

Copyright © 2014 Democracy Works, Inc.
